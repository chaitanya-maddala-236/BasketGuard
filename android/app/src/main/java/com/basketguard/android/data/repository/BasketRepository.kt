package com.basketguard.android.data.repository

import android.content.Context
import com.basketguard.android.data.local.BasketGuardDatabase
import com.basketguard.android.data.local.CartEntity
import com.basketguard.android.data.local.SessionStore
import com.basketguard.android.data.local.toCartEntity
import com.basketguard.android.data.sync.scheduleCartSync
import com.basketguard.android.data.network.AuthRequest
import com.basketguard.android.data.network.BasketGuardApi
import com.basketguard.android.data.network.CartLineDto
import com.basketguard.android.data.network.CartSyncRequest
import com.basketguard.android.data.network.CartValidationRequest
import com.basketguard.android.data.network.OrderDto
import com.basketguard.android.data.network.OrderRequest
import com.basketguard.android.data.network.ProductDto
import com.basketguard.android.data.network.SubstitutionDto
import com.basketguard.android.data.network.SubstitutionCreateRequest
import com.basketguard.android.domain.Category
import com.basketguard.android.domain.Product
import com.basketguard.android.domain.demoCategories
import com.basketguard.android.domain.demoProducts
import kotlinx.coroutines.flow.Flow

data class CatalogSnapshot(val products: List<Product>, val categories: List<Category>, val connected: Boolean)

class BasketRepository(
    context: Context,
    private val database: BasketGuardDatabase,
    private val api: BasketGuardApi,
) {
    private val cartDao = database.cartDao()
    private val session = SessionStore(context.applicationContext)
    val cart: Flow<List<CartEntity>> = cartDao.observeCart()

    suspend fun catalog(): CatalogSnapshot = try {
        val categories = api.categories()
        val categoryNames = categories.associate { it.id to it.name }
        val products = api.products().map { it.toDomain(categoryNames[it.category_id].orEmpty()) }
        CatalogSnapshot(products.ifEmpty { demoProducts },
            listOf(Category(0, "All", "✦")) + categories.map { Category(it.id, it.name, it.icon) },
            connected = true)
    } catch (_: Exception) {
        CatalogSnapshot(demoProducts, demoCategories, connected = false)
    }

    suspend fun setQuantity(product: Product, quantity: Int) {
        if (quantity <= 0) cartDao.remove(product.id) else cartDao.upsert(product.toCartEntity(quantity))
        scheduleCartSync(context)
    }

    suspend fun accountName(): String? = session.name()
    suspend fun hasAccount(): Boolean = !session.token().isNullOrBlank()

    suspend fun authenticate(name: String, email: String, password: String, register: Boolean): String {
        val result = if (register) {
            api.register(AuthRequest(name = name.trim(), email = email.trim(), password = password))
        } else {
            api.login(AuthRequest(email = email.trim(), password = password))
        }
        session.save(result.access_token, result.name)
        return result.name
    }

    suspend fun signOut() = session.clear()

    suspend fun validate(items: List<CartEntity>) = api.validateCart(
        CartValidationRequest(items = items.map { CartLineDto(it.productId, it.quantity) }),
    )

    suspend fun placeOrder(address: String, items: List<CartEntity>): OrderDto {
        val token = session.token() ?: error("Sign in from Profile before placing your order.")
        return api.createOrder(
            bearer = "Bearer $token",
            idempotencyKey = session.checkoutKey(),
            request = OrderRequest(
                address = address,
                items = items.map { CartLineDto(it.productId, it.quantity) },
            ),
        ).also {
            cartDao.clear()
            session.clearCheckoutKey()
        }
    }

    suspend fun orders(): List<OrderDto> {
        val token = session.token() ?: return emptyList()
        return api.orders("Bearer $token")
    }

    suspend fun substitutions(orderId: String): List<SubstitutionDto> {
        val token = session.token() ?: return emptyList()
        return api.substitutions("Bearer $token", orderId)
    }

    suspend fun requestSubstitution(orderItemId: Int, productId: Int): SubstitutionDto {
        val token = session.token() ?: error("Sign in to request a replacement.")
        return api.createSubstitution(
            "Bearer $token",
            SubstitutionCreateRequest(order_item_id = orderItemId, replacement_product_id = productId),
        )
    }

    suspend fun decideSubstitution(id: String, decision: String): SubstitutionDto {
        val token = session.token() ?: error("Sign in to manage substitutions.")
        return api.decideSubstitution("Bearer $token", id, mapOf("decision" to decision))
    }

    suspend fun syncCart() {
        val token = session.token() ?: return
        val items = cartDao.getCart()
        api.syncCart("Bearer $token", CartSyncRequest(items.map { CartLineDto(it.productId, it.quantity) }))
        if (items.isEmpty()) return
        val validation = api.validateCart(CartValidationRequest(
            items = items.map { CartLineDto(it.productId, it.quantity) },
        ))
        val freshProducts = api.products().associateBy { it.id }
        val availability = validation.items.associate { it.product_id to it.available }
        items.forEach { old ->
            val fresh = freshProducts[old.productId]
            if (fresh != null) {
                cartDao.upsert(old.copy(name = fresh.name, price = fresh.price, unit = fresh.unit,
                    emoji = fresh.emoji, available = availability[old.productId] ?: fresh.available_stock))
            } else {
                cartDao.upsert(old.copy(available = 0))
            }
        }
    }

    private fun ProductDto.toDomain(category: String) = Product(
        id = id,
        name = name,
        description = description,
        price = price,
        unit = unit,
        emoji = emoji,
        categoryId = category_id,
        category = category,
        available = available_stock,
    )
}

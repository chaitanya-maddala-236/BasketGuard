package com.basketguard.android.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.basketguard.android.data.local.CartEntity
import com.basketguard.android.data.network.OrderDto
import com.basketguard.android.data.network.SubstitutionDto
import com.basketguard.android.data.repository.BasketRepository
import com.basketguard.android.analytics.BasketAnalytics
import com.basketguard.shared.domain.BasketPlanRequest
import com.basketguard.shared.domain.BasketRescueItem
import com.basketguard.shared.domain.Category
import com.basketguard.shared.domain.GenerateBasketPlanUseCase
import com.basketguard.shared.domain.PlannedProduct
import com.basketguard.shared.domain.Product
import com.basketguard.shared.domain.ProductAlternative
import com.basketguard.shared.domain.SmartBasketPlan
import com.basketguard.shared.domain.demoCategories
import com.basketguard.shared.domain.demoProducts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppUiState(
    val products: List<Product> = demoProducts,
    val categories: List<Category> = demoCategories,
    val cart: List<CartEntity> = emptyList(),
    val accountName: String? = null,
    val connected: Boolean = false,
    val busy: Boolean = false,
    val message: String? = null,
    val orders: List<OrderDto> = emptyList(),
    val substitutions: Map<String, List<SubstitutionDto>> = emptyMap(),
    val rescueBudgetPercent: Int = 15,
    val rescueItems: List<BasketRescueItem> = emptyList(),
    val alternatives: Map<Int, List<ProductAlternative>> = emptyMap(),
    val alternativesLoading: Set<Int> = emptySet(),
    val smartBasketPlan: SmartBasketPlan? = null,
    val planning: Boolean = false,
)

@HiltViewModel
class AppViewModel @Inject constructor(
    private val repository: BasketRepository,
    private val generateBasketPlan: GenerateBasketPlanUseCase,
    private val analytics: BasketAnalytics,
) : ViewModel() {
    private val mutableState = MutableStateFlow(AppUiState())
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.catalog().also { catalog ->
                mutableState.update {
                    it.copy(products = catalog.products, categories = catalog.categories,
                        connected = catalog.connected)
                }
            }
        }
        viewModelScope.launch {
            repository.cart.collect { items -> mutableState.update { it.copy(cart = items) } }
        }
        viewModelScope.launch {
            mutableState.update { it.copy(accountName = repository.accountName(),
                rescueBudgetPercent = repository.rescueBudgetPercent()) }
        }
    }

    fun add(product: Product) = changeQuantity(product, (state.value.cart.firstOrNull { it.productId == product.id }?.quantity ?: 0) + 1)

    fun changeQuantity(productId: Int, quantity: Int) {
        viewModelScope.launch {
            val item = state.value.cart.firstOrNull { it.productId == productId }
            val product = state.value.products.firstOrNull { it.id == productId }
            if (product != null) repository.setQuantity(product, quantity)
            else if (item != null) {
                if (quantity <= 0) repository.setQuantity(
                    Product(item.productId, item.name, "", item.price, item.unit, item.emoji,
                        0, "", 0), 0,
                ) else repository.setQuantity(
                    Product(item.productId, item.name, "", item.price, item.unit, item.emoji,
                        0, "", 0), quantity,
                )
            }
            mutableState.update { it.copy(rescueItems = it.rescueItems.filterNot { rescue -> rescue.source.id == productId }) }
        }
    }

    fun authenticate(name: String, email: String, password: String, register: Boolean) {
        if (email.isBlank() || password.isBlank() || (register && name.isBlank())) {
            showMessage("Add your details to continue.")
            return
        }
        viewModelScope.launch {
            mutableState.update { it.copy(busy = true, message = null) }
            try {
                val accountName = repository.authenticate(name, email, password, register)
                mutableState.update { it.copy(accountName = accountName, busy = false,
                    message = "You’re signed in, $accountName.") }
                try { repository.syncCart() } catch (_: Exception) { }
                loadOrders()
            } catch (error: Exception) {
                mutableState.update { it.copy(busy = false,
                    message = error.message?.substringAfter("\n")?.take(140)
                        ?: "Couldn’t sign in. Check your details and try again.") }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
            mutableState.update { it.copy(accountName = null, orders = emptyList(), substitutions = emptyMap(),
                message = "You’re signed out.") }
        }
    }

    fun placeOrder(address: String) {
        val current = state.value
        if (current.accountName == null) {
            showMessage("Sign in from Profile before checking out.")
            return
        }
        if (current.cart.isEmpty()) {
            showMessage("Your basket is empty.")
            return
        }
        if (address.trim().length < 8) {
            showMessage("Enter a complete delivery address.")
            return
        }
        viewModelScope.launch {
            mutableState.update { it.copy(busy = true, message = null) }
            try {
                val validation = repository.validate(current.cart)
                if (!validation.valid) {
                    val rescueItems = prepareRescue(validation, current.cart, current.products,
                        current.rescueBudgetPercent)
                    analytics.event("stockout_rescue_shown", mapOf(
                        "issue_count" to rescueItems.size.toLong(),
                        "budget_percent" to current.rescueBudgetPercent.toLong(),
                    ))
                    mutableState.update { it.copy(busy = false, rescueItems = rescueItems, message = null) }
                    return@launch
                }
                val order = repository.placeOrder(address.trim(), current.cart)
                mutableState.update { it.copy(busy = false, rescueItems = emptyList(),
                    message = "Order placed · ${order.id.take(8)}") }
                loadOrders()
            } catch (error: Exception) {
                val refreshed = try { repository.validate(state.value.cart) } catch (_: Exception) { null }
                if (refreshed != null && !refreshed.valid) {
                    val rescues = prepareRescue(refreshed, state.value.cart, state.value.products,
                        state.value.rescueBudgetPercent)
                    mutableState.update { it.copy(busy = false, rescueItems = rescues, message = null) }
                } else {
                    mutableState.update { it.copy(busy = false,
                        message = if (error.message?.contains("409") == true)
                            "Stock changed while you checked out. Review your basket and retry."
                        else error.message?.take(140) ?: "Couldn’t place your order. Try again.") }
                }
            }
        }
    }

    fun loadAlternatives(product: Product, quantity: Int = 1) {
        viewModelScope.launch {
            val budget = state.value.rescueBudgetPercent
            mutableState.update { it.copy(alternativesLoading = it.alternativesLoading + product.id) }
            val alternatives = repository.alternatives(product, quantity, budget)
            mutableState.update { it.copy(
                alternatives = it.alternatives + (product.id to alternatives),
                alternativesLoading = it.alternativesLoading - product.id,
            ) }
        }
    }

    fun requestSmartBasketPlan(prompt: String, budget: Double) {
        viewModelScope.launch {
            mutableState.update { it.copy(planning = true, smartBasketPlan = null, message = null) }
            try {
                val plan = generateBasketPlan(BasketPlanRequest(prompt = prompt, budget = budget))
                analytics.event("ai_basket_plan_created", mapOf(
                    "item_count" to plan.items.size.toLong(),
                    "budget_bucket" to when {
                        budget < 250 -> "under_250"
                        budget < 500 -> "250_to_499"
                        else -> "500_plus"
                    },
                ))
                mutableState.update { it.copy(planning = false, smartBasketPlan = plan) }
            } catch (error: Exception) {
                mutableState.update { it.copy(planning = false,
                    message = error.message?.take(180) ?: "Couldn’t make a basket plan. Try again.") }
            }
        }
    }

    fun addSmartPlanToBasket() {
        val plan = state.value.smartBasketPlan ?: return
        viewModelScope.launch {
            mutableState.update { it.copy(busy = true) }
            try {
                repository.addPlannedProducts(plan.items)
                analytics.event("ai_basket_plan_added", mapOf("item_count" to plan.items.size.toLong()))
                mutableState.update { it.copy(busy = false, smartBasketPlan = null,
                    message = "${plan.items.size} planned items added to your basket.") }
            } catch (error: Exception) {
                mutableState.update { it.copy(busy = false,
                    message = error.message ?: "Some planned items are no longer available.") }
            }
        }
    }

    fun setRescueBudget(percent: Int) {
        if (percent !in listOf(0, 15, 30)) return
        viewModelScope.launch {
            try {
                val current = state.value.rescueItems
                mutableState.update { it.copy(rescueBudgetPercent = percent,
                    alternatives = emptyMap(),
                    rescueItems = current.map { row -> row.copy(loading = true, alternatives = emptyList()) }) }
                repository.saveRescueBudgetPercent(percent)
                val updated = current.map { row ->
                    row.copy(loading = false, alternatives = repository.alternatives(row.source, row.quantity, percent))
                }
                mutableState.update { latest ->
                    if (latest.rescueBudgetPercent != percent) latest
                    else latest.copy(rescueItems = latest.rescueItems.map { row ->
                        updated.firstOrNull { replacement -> replacement.source.id == row.source.id } ?: row.copy(loading = false)
                    }, alternatives = emptyMap(), alternativesLoading = emptySet())
                }
            } catch (_: Exception) {
                mutableState.update { latest ->
                    latest.copy(rescueItems = latest.rescueItems.map { it.copy(loading = false) },
                        message = "Couldn’t update your swap budget. Try again.")
                }
            }
        }
    }

    fun applyRescue(sourceProductId: Int, alternative: ProductAlternative, quantity: Int) {
        viewModelScope.launch {
            mutableState.update { it.copy(busy = true) }
            try {
                repository.replaceCartItem(sourceProductId, alternative, quantity)
                analytics.event("stockout_substitute_selected", mapOf(
                    "source_product_id" to sourceProductId.toLong(),
                    "replacement_product_id" to alternative.product.id.toLong(),
                    "quantity" to quantity.toLong(),
                ))
                mutableState.update { it.copy(busy = false,
                    rescueItems = it.rescueItems.filterNot { row -> row.source.id == sourceProductId },
                    message = "${alternative.product.name} added within your swap budget.") }
            } catch (error: Exception) {
                mutableState.update { it.copy(busy = false,
                    message = error.message ?: "That replacement is no longer available.") }
            }
        }
    }

    fun keepAvailable(sourceProductId: Int, available: Int) {
        val source = state.value.rescueItems.firstOrNull { it.source.id == sourceProductId } ?: return
        viewModelScope.launch {
            mutableState.update { it.copy(busy = true) }
            try {
                repository.setQuantity(source.source.copy(available = available), available)
                mutableState.update { it.copy(busy = false,
                    rescueItems = it.rescueItems.filterNot { row -> row.source.id == sourceProductId },
                    message = "Basket updated to the $available currently available.") }
            } catch (error: Exception) {
                mutableState.update { it.copy(busy = false, message = error.message ?: "Couldn’t update this basket item.") }
            }
        }
    }

    fun removeUnavailable(sourceProductId: Int) {
        val source = state.value.rescueItems.firstOrNull { it.source.id == sourceProductId } ?: return
        viewModelScope.launch {
            mutableState.update { it.copy(busy = true) }
            try {
                repository.setQuantity(source.source, 0)
                analytics.event("stockout_item_removed", mapOf("product_id" to sourceProductId.toLong()))
                mutableState.update { it.copy(busy = false,
                    rescueItems = it.rescueItems.filterNot { row -> row.source.id == sourceProductId },
                    message = "${source.source.name} removed from your basket.") }
            } catch (error: Exception) {
                mutableState.update { it.copy(busy = false, message = error.message ?: "Couldn’t remove this item.") }
            }
        }
    }

    private suspend fun prepareRescue(
        validation: com.basketguard.android.data.network.CartValidationDto,
        cart: List<CartEntity>,
        products: List<Product>,
        budget: Int,
    ): List<BasketRescueItem> {
        val lines = cart.associateBy { it.productId }
        return validation.items.filterNot { it.valid }.mapNotNull { item ->
            val line = lines[item.product_id] ?: return@mapNotNull null
            val source = products.firstOrNull { it.id == line.productId } ?: Product(
                line.productId, line.name, "", line.price, line.unit, line.emoji, 0, "", item.available,
            )
            BasketRescueItem(
                source = source,
                quantity = item.requested,
                available = item.available,
                alternatives = repository.alternatives(source, item.requested, budget),
            )
        }
    }

    fun loadOrders() {
        viewModelScope.launch {
            try {
                val orders = repository.orders()
                mutableState.update { it.copy(orders = orders) }
                orders.forEach { refreshSubstitutions(it.id) }
            } catch (_: Exception) {
                mutableState.update { it.copy(orders = emptyList()) }
            }
        }
    }

    fun refreshSubstitutions(orderId: String) {
        viewModelScope.launch {
            try {
                val rows = repository.substitutions(orderId)
                mutableState.update { it.copy(substitutions = it.substitutions + (orderId to rows)) }
            } catch (_: Exception) { }
        }
    }

    fun requestSubstitution(orderId: String, orderItemId: Int, productId: Int) {
        viewModelScope.launch {
            try {
                repository.requestSubstitution(orderItemId, productId)
                refreshSubstitutions(orderId)
                showMessage("Replacement sent for your approval.")
            } catch (error: Exception) {
                showMessage(error.message?.take(120) ?: "Couldn’t request that replacement.")
            }
        }
    }

    fun decideSubstitution(orderId: String, substitutionId: String, decision: String) {
        viewModelScope.launch {
            try {
                repository.decideSubstitution(substitutionId, decision)
                loadOrders()
                showMessage(if (decision == "approve") "Replacement approved." else "Replacement declined.")
            } catch (error: Exception) {
                showMessage(error.message?.take(120) ?: "Couldn’t update the replacement.")
            }
        }
    }

    fun showMessage(message: String) = mutableState.update { it.copy(message = message) }
    fun clearMessage() = mutableState.update { it.copy(message = null) }
}

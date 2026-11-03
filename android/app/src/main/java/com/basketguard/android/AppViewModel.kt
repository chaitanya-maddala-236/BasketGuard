package com.basketguard.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.basketguard.android.data.local.CartEntity
import com.basketguard.android.data.network.OrderDto
import com.basketguard.android.data.network.SubstitutionDto
import com.basketguard.android.data.repository.BasketRepository
import com.basketguard.android.domain.Category
import com.basketguard.android.domain.Product
import com.basketguard.android.domain.demoCategories
import com.basketguard.android.domain.demoProducts
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
)

@HiltViewModel
class AppViewModel @Inject constructor(private val repository: BasketRepository) : ViewModel() {
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
            mutableState.update { it.copy(accountName = repository.accountName()) }
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
                    val names = validation.unavailable.mapNotNull { id -> current.products.firstOrNull { it.id == id }?.name }
                    mutableState.update { it.copy(busy = false,
                        message = if (names.isEmpty()) "Stock changed. Review your basket and retry."
                        else "Stock changed for ${names.joinToString()}. Choose a replacement or update quantity.") }
                    return@launch
                }
                val order = repository.placeOrder(address.trim(), current.cart)
                mutableState.update { it.copy(busy = false, message = "Order placed · ${order.id.take(8)}") }
                loadOrders()
            } catch (error: Exception) {
                mutableState.update { it.copy(busy = false,
                    message = if (error.message?.contains("409") == true)
                        "A product just sold out. Refresh your basket before retrying."
                    else error.message?.take(140) ?: "Couldn’t place your order. Try again.") }
            }
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

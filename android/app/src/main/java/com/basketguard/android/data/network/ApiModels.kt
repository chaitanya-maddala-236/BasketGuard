package com.basketguard.android.data.network

data class ProductDto(
    val id: Int,
    val name: String,
    val description: String,
    val price: Double,
    val unit: String,
    val emoji: String,
    val category_id: Int,
    val image_url: String? = null,
    val available_stock: Int = 0,
)

data class AlternativeDto(
    val id: Int,
    val name: String,
    val category_id: Int,
    val description: String,
    val price: Double,
    val unit: String,
    val emoji: String,
    val image_url: String? = null,
    val available_stock: Int = 0,
    val price_difference: Double,
    val reason: String,
)

data class BasketRescueDto(
    val product_id: Int,
    val requested_quantity: Int,
    val max_price: Double,
    val alternatives: List<AlternativeDto>,
)

data class CategoryDto(val id: Int, val name: String, val icon: String)
data class CartLineDto(val product_id: Int, val quantity: Int)
data class CartValidationRequest(val store_id: Int = 1, val items: List<CartLineDto>)
data class CartValidationItemDto(
    val product_id: Int,
    val requested: Int,
    val available: Int,
    val valid: Boolean,
)
data class CartValidationDto(
    val valid: Boolean,
    val items: List<CartValidationItemDto>,
    val unavailable: List<Int>,
)
data class CartSyncRequest(val items: List<CartLineDto>)
data class ReservationRequest(val store_id: Int = 1, val items: List<CartLineDto>)
data class ReservationDto(
    val reservation_id: String,
    val store_id: Int,
    val expires_at: String,
    val items: List<CartLineDto>,
    val status: String,
)
data class OrderRequest(
    val store_id: Int = 1,
    val address: String,
    val payment_method: String = "mock_card",
    val reservation_id: String? = null,
    val items: List<CartLineDto>,
)
data class OrderItemDto(
    val id: Int,
    val product_id: Int,
    val product_name: String,
    val quantity: Int,
    val final_price: Double,
)
data class OrderDto(
    val id: String,
    val status: String,
    val total_amount: Double,
    val address: String,
    val created_at: String,
    val items: List<OrderItemDto>,
)
data class TokenDto(
    val access_token: String,
    val token_type: String,
    val user_id: String,
    val name: String,
)
data class AuthRequest(val name: String? = null, val email: String, val password: String)
data class SubstitutionDto(
    val id: String,
    val order_item_id: Int,
    val replacement_product_id: Int,
    val replacement_product_name: String,
    val status: String,
    val price_difference: Double,
)
data class SubstitutionCreateRequest(val order_item_id: Int, val replacement_product_id: Int)

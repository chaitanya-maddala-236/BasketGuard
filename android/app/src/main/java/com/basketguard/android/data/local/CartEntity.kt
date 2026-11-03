package com.basketguard.android.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.basketguard.android.domain.Product

@Entity(tableName = "cart_items")
data class CartEntity(
    @PrimaryKey val productId: Int,
    val name: String,
    val price: Double,
    val unit: String,
    val emoji: String,
    val quantity: Int,
    val available: Int = 0,
)

fun Product.toCartEntity(quantity: Int) = CartEntity(
    productId = id,
    name = name,
    price = price,
    unit = unit,
    emoji = emoji,
    quantity = quantity,
    available = available,
)

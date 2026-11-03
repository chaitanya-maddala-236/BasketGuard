package com.basketguard.android.domain

data class Product(
    val id: Int,
    val name: String,
    val description: String,
    val price: Double,
    val unit: String,
    val emoji: String,
    val categoryId: Int,
    val category: String,
    val available: Int,
)

data class Category(val id: Int, val name: String, val icon: String)

val demoCategories = listOf(
    Category(0, "All", "✦"),
    Category(1, "Produce", "🥑"),
    Category(2, "Dairy", "🥛"),
    Category(3, "Bakery", "🥖"),
    Category(4, "Pantry", "🥫"),
    Category(5, "Snacks", "🍿"),
    Category(6, "Drinks", "🧃"),
)

val demoProducts = listOf(
    Product(1, "Avocado", "Creamy, ripe Hass avocado", 79.0, "1 piece", "🥑", 1, "Produce", 24),
    Product(2, "Farm eggs", "Free-range, protein-rich eggs", 99.0, "6 pieces", "🥚", 2, "Dairy", 18),
    Product(3, "Sourdough loaf", "Slow-fermented, baked this morning", 149.0, "400 g", "🍞", 3, "Bakery", 8),
    Product(4, "Oat milk", "Smooth, unsweetened oat drink", 129.0, "1 litre", "🥛", 2, "Dairy", 5),
    Product(5, "Almond butter", "Roasted almonds, no added sugar", 249.0, "200 g", "🥜", 4, "Pantry", 11),
    Product(6, "Baby spinach", "Tender leaves, washed and ready", 59.0, "100 g", "🥬", 1, "Produce", 16),
    Product(7, "Sea salt chips", "Crispy kettle-cooked potato chips", 49.0, "120 g", "🥔", 5, "Snacks", 20),
    Product(8, "Cold brew coffee", "Slow-steeped, ready to pour", 119.0, "250 ml", "☕", 6, "Drinks", 3),
    Product(9, "Greek yogurt", "Thick, creamy, high protein", 89.0, "400 g", "🍶", 2, "Dairy", 14),
    Product(10, "Wild blueberries", "Sweet-tart berries, frozen fresh", 169.0, "250 g", "🫐", 1, "Produce", 0),
)

package com.basketguard.shared.domain

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
    val substitutionGroup: String = "general",
)

data class Category(val id: Int, val name: String, val icon: String)

data class ProductAlternative(
    val product: Product,
    val priceDifference: Double,
    val reason: String,
)

data class BasketRescueItem(
    val source: Product,
    val quantity: Int,
    val available: Int,
    val alternatives: List<ProductAlternative> = emptyList(),
    val loading: Boolean = false,
)

data class PlannedProduct(
    val product: Product,
    val quantity: Int,
    val reason: String,
)

data class SmartBasketPlan(
    val title: String,
    val summary: String,
    val budget: Double,
    val estimatedTotal: Double,
    val items: List<PlannedProduct>,
)

data class BasketPlanRequest(
    val prompt: String,
    val budget: Double,
    val storeId: Int = 1,
)

fun interface BasketPlanGateway {
    suspend fun generate(request: BasketPlanRequest): SmartBasketPlan
}

class GenerateBasketPlanUseCase(private val gateway: BasketPlanGateway) {
    suspend operator fun invoke(request: BasketPlanRequest): SmartBasketPlan {
        require(request.prompt.trim().length in 8..240) { "Describe what you need in 8–240 characters." }
        require(request.budget.isFinite() && request.budget in 1.0..20_000.0) {
            "Set a budget between ₹1 and ₹20,000."
        }
        val plan = gateway.generate(request.copy(prompt = request.prompt.trim()))
        require(plan.items.isNotEmpty()) { "No available items fit this request." }
        require(plan.items.all { it.quantity in 1..99 && it.quantity <= it.product.available }) {
            "The plan includes an item quantity that is not currently available."
        }
        val verifiedTotal = plan.items.sumOf { it.product.price * it.quantity }
        require(verifiedTotal <= request.budget + 0.01) { "The plan exceeds your budget." }
        return plan.copy(budget = request.budget, estimatedTotal = verifiedTotal)
    }
}

object BasketRescuePolicy {
    fun recommend(
        source: Product,
        candidates: List<Product>,
        quantity: Int,
        maxPriceIncreasePercent: Int,
        limit: Int = 5,
    ): List<ProductAlternative> {
        require(quantity in 1..99)
        require(maxPriceIncreasePercent in listOf(0, 15, 30))
        if (limit <= 0) return emptyList()
        val ceiling = source.price * (1.0 + maxPriceIncreasePercent / 100.0)
        return candidates.asSequence()
            .filter { it.id != source.id && it.substitutionGroup == source.substitutionGroup }
            .filter { it.available >= quantity && it.price <= ceiling }
            .sortedWith(compareBy<Product>({ it.price >= source.price }, { kotlin.math.abs(it.price - source.price) }, { -it.available }, { it.name }))
            .take(limit)
            .map { product ->
                val difference = product.price - source.price
                ProductAlternative(product, difference, when {
                    difference < 0 -> "Same use · lower price · ${product.available} in stock"
                    difference == 0.0 -> "Same use · same price · ${product.available} in stock"
                    else -> "Same use · within your price cap · ${product.available} in stock"
                })
            }.toList()
    }
}

val demoCategories = listOf(
    Category(0, "All", "✦"),
    Category(1, "Produce", "🥑"),
    Category(2, "Dairy", "🥛"),
    Category(3, "Bakery", "🥖"),
    Category(4, "Pantry", "🥫"),
    Category(5, "Snacks", "🍿"),
    Category(6, "Drinks", "🧃"),
    Category(7, "Berries", "🫐"),
)

val demoProducts = listOf(
    Product(1, "Avocado", "Creamy, ripe Hass avocado", 79.0, "1 piece", "🥑", 1, "Produce", 24, "avocado"),
    Product(2, "Farm eggs", "Free-range, protein-rich eggs", 99.0, "6 pieces", "🥚", 2, "Dairy", 18, "eggs"),
    Product(3, "Sourdough loaf", "Slow-fermented, baked this morning", 149.0, "400 g", "🍞", 3, "Bakery", 8, "bread"),
    Product(4, "Oat milk", "Smooth, unsweetened oat drink", 129.0, "1 litre", "🥛", 2, "Dairy", 5, "plant-milk"),
    Product(5, "Almond butter", "Roasted almonds, no added sugar", 249.0, "200 g", "🥜", 4, "Pantry", 11, "nut-butter"),
    Product(6, "Baby spinach", "Tender leaves, washed and ready", 59.0, "100 g", "🥬", 1, "Produce", 16, "leafy-greens"),
    Product(7, "Sea salt chips", "Crispy kettle-cooked potato chips", 49.0, "120 g", "🥔", 5, "Snacks", 20, "chips"),
    Product(8, "Cold brew coffee", "Slow-steeped, ready to pour", 119.0, "250 ml", "☕", 6, "Drinks", 3, "cold-brew"),
    Product(9, "Greek yogurt", "Thick, creamy, high protein", 89.0, "400 g", "🍶", 2, "Dairy", 14, "yogurt"),
    Product(10, "Wild blueberries", "Sweet-tart berries, frozen fresh", 169.0, "250 g", "🫐", 7, "Berries", 0, "berries"),
    Product(11, "Sweet strawberries", "Ripe berries from a nearby farm", 139.0, "250 g", "🍓", 7, "Berries", 8, "berries"),
    Product(12, "Fresh raspberries", "Delicate, sweet-tart berry punnet", 189.0, "125 g", "🍇", 7, "Berries", 5, "berries"),
    Product(13, "Barista soy milk", "Creamy plant milk, locally made", 109.0, "1 litre", "🥛", 2, "Dairy", 9, "plant-milk"),
    Product(14, "Multigrain loaf", "Soft loaf with oats and seeds", 119.0, "400 g", "🥖", 3, "Bakery", 7, "bread"),
)

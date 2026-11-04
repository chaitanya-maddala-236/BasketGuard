package com.basketguard.android

import com.basketguard.shared.domain.BasketRescuePolicy
import com.basketguard.shared.domain.Product
import org.junit.Assert.assertEquals
import org.junit.Test

class BasketRescuePolicyTest {
    private val source = product(1, "Wild blueberries", 169.0, 0)

    @Test
    fun recommendsSameUseItemsThatCoverTheWholeQuantityAndCap() {
        val candidates = listOf(
            product(2, "Strawberries", 139.0, 8),
            product(3, "Raspberries", 189.0, 5),
            product(4, "Low-stock berries", 99.0, 2),
            product(5, "Over-cap berries", 199.0, 12),
            product(6, "Different use", 89.0, 10, group = "chips"),
            source,
        )

        val actual = BasketRescuePolicy.recommend(source, candidates, quantity = 3, maxPriceIncreasePercent = 15)

        assertEquals(listOf(2, 3), actual.map { it.product.id })
    }

    @Test
    fun strictBudgetOnlyAllowsSameOrCheaperAlternatives() {
        val actual = BasketRescuePolicy.recommend(
            source,
            listOf(product(2, "Strawberries", 139.0, 8), product(3, "Raspberries", 169.0, 5), product(4, "Premium berries", 170.0, 8)),
            quantity = 1,
            maxPriceIncreasePercent = 0,
        )

        assertEquals(listOf(2, 3), actual.map { it.product.id })
    }

    @Test
    fun recommendationCountIsBounded() {
        val candidates = (2..10).map { product(it, "Berry $it", 140.0 + it, 10) }

        val actual = BasketRescuePolicy.recommend(source, candidates, quantity = 1, maxPriceIncreasePercent = 30, limit = 3)

        assertEquals(3, actual.size)
    }

    private fun product(id: Int, name: String, price: Double, stock: Int, group: String = "berries") = Product(
        id = id,
        name = name,
        description = "Fresh item",
        price = price,
        unit = "punnet",
        emoji = "🫐",
        categoryId = 7,
        category = "Berries",
        available = stock,
        substitutionGroup = group,
    )
}

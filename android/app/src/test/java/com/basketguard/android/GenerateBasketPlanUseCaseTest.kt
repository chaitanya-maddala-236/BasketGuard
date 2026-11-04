package com.basketguard.android

import com.basketguard.shared.domain.BasketPlanGateway
import com.basketguard.shared.domain.BasketPlanRequest
import com.basketguard.shared.domain.GenerateBasketPlanUseCase
import com.basketguard.shared.domain.PlannedProduct
import com.basketguard.shared.domain.Product
import com.basketguard.shared.domain.SmartBasketPlan
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GenerateBasketPlanUseCaseTest {
    @Test
    fun trimsPromptAndRecalculatesTotalFromCatalogPrices() = runBlocking {
        var observedPrompt: String? = null
        val useCase = GenerateBasketPlanUseCase(BasketPlanGateway { request ->
            observedPrompt = request.prompt
            plan(quantity = 2, available = 4, reportedTotal = 999.0)
        })

        val result = useCase(BasketPlanRequest("  Quick dinner for two  ", budget = 500.0))

        assertEquals("Quick dinner for two", observedPrompt)
        assertEquals(200.0, result.estimatedTotal, 0.001)
    }

    @Test
    fun rejectsAPlanThatExceedsBudget() = runBlocking {
        var called = false
        val useCase = GenerateBasketPlanUseCase(BasketPlanGateway {
            called = true
            plan(quantity = 3, available = 4, reportedTotal = 300.0)
        })

        val error = runCatching { useCase(BasketPlanRequest("Dinner for two", budget = 250.0)) }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertTrue(called)
    }

    @Test
    fun rejectsBadBudgetBeforeCallingGateway() = runBlocking {
        var called = false
        val useCase = GenerateBasketPlanUseCase(BasketPlanGateway {
            called = true
            plan(quantity = 1, available = 4, reportedTotal = 100.0)
        })

        val error = runCatching { useCase(BasketPlanRequest("Dinner for two", budget = 0.0)) }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertFalse(called)
    }

    @Test
    fun rejectsRecommendationsAboveCurrentStock() = runBlocking {
        val useCase = GenerateBasketPlanUseCase(BasketPlanGateway {
            plan(quantity = 3, available = 2, reportedTotal = 300.0)
        })

        val error = runCatching { useCase(BasketPlanRequest("Dinner for two", budget = 500.0)) }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
    }

    private fun plan(quantity: Int, available: Int, reportedTotal: Double) = SmartBasketPlan(
        title = "Dinner",
        summary = "A quick dinner basket.",
        budget = 500.0,
        estimatedTotal = reportedTotal,
        items = listOf(PlannedProduct(
            product = Product(1, "Oat milk", "Milk", 100.0, "litre", "🥛", 2, "Dairy", available, "plant-milk"),
            quantity = quantity,
            reason = "Useful for the recipe",
        )),
    )
}

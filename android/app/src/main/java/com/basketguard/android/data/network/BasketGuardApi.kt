package com.basketguard.android.data.network

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface BasketGuardApi {
    @GET("products")
    suspend fun products(
        @Query("q") query: String? = null,
        @Query("category_id") categoryId: Int? = null,
        @Query("store_id") storeId: Int = 1,
    ): List<ProductDto>

    @GET("categories")
    suspend fun categories(): List<CategoryDto>

    @POST("cart/validate")
    suspend fun validateCart(@Body request: CartValidationRequest): CartValidationDto

    @PUT("cart")
    suspend fun syncCart(
        @Header("Authorization") bearer: String,
        @Body request: CartSyncRequest,
    )

    @POST("auth/register")
    suspend fun register(@Body request: AuthRequest): TokenDto

    @POST("auth/login")
    suspend fun login(@Body request: AuthRequest): TokenDto

    @POST("orders")
    suspend fun createOrder(
        @Header("Authorization") bearer: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: OrderRequest,
    ): OrderDto

    @GET("orders")
    suspend fun orders(@Header("Authorization") bearer: String): List<OrderDto>

    @GET("orders/{orderId}/substitutions")
    suspend fun substitutions(
        @Header("Authorization") bearer: String,
        @Path("orderId") orderId: String,
    ): List<SubstitutionDto>

    @POST("substitutions")
    suspend fun createSubstitution(
        @Header("Authorization") bearer: String,
        @Body request: SubstitutionCreateRequest,
    ): SubstitutionDto

    @POST("substitutions/{substitutionId}/decision")
    suspend fun decideSubstitution(
        @Header("Authorization") bearer: String,
        @Path("substitutionId") substitutionId: String,
        @Body decision: Map<String, String>,
    ): SubstitutionDto

    @DELETE("reservations/{reservationId}")
    suspend fun releaseReservation(
        @Header("Authorization") bearer: String,
        @Path("reservationId") reservationId: String,
    )
}

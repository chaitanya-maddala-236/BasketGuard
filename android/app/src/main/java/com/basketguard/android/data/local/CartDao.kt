package com.basketguard.android.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CartDao {
    @Query("SELECT * FROM cart_items ORDER BY name")
    fun observeCart(): Flow<List<CartEntity>>

    @Query("SELECT * FROM cart_items ORDER BY name")
    suspend fun getCart(): List<CartEntity>

    @Upsert
    suspend fun upsert(item: CartEntity)

    @Query("DELETE FROM cart_items WHERE productId = :productId")
    suspend fun remove(productId: Int)

    @Query("DELETE FROM cart_items")
    suspend fun clear()

    @Transaction
    suspend fun replaceItem(sourceProductId: Int, replacement: CartEntity) {
        if (sourceProductId != replacement.productId) remove(sourceProductId)
        upsert(replacement)
    }
}

package com.basketguard.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [CartEntity::class], version = 1, exportSchema = false)
abstract class BasketGuardDatabase : RoomDatabase() {
    abstract fun cartDao(): CartDao
}

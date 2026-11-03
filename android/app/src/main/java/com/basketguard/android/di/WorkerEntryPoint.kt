package com.basketguard.android.di

import com.basketguard.android.data.repository.BasketRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WorkerEntryPoint {
    fun basketRepository(): BasketRepository
}

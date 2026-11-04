package com.basketguard.android.di

import android.content.Context
import androidx.room.Room
import com.basketguard.android.BuildConfig
import com.basketguard.android.data.local.BasketGuardDatabase
import com.basketguard.android.analytics.BasketAnalytics
import com.basketguard.android.data.analytics.FirebaseBasketAnalytics
import com.basketguard.android.data.network.BasketGuardApi
import com.basketguard.android.data.repository.BasketRepository
import com.basketguard.shared.domain.BasketPlanGateway
import com.basketguard.shared.domain.GenerateBasketPlanUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): BasketGuardDatabase =
        Room.databaseBuilder(context, BasketGuardDatabase::class.java, "basketguard.db").build()

    @Provides
    @Singleton
    fun provideApi(): BasketGuardApi = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(BasketGuardApi::class.java)

    @Provides
    @Singleton
    fun provideRepository(
        @ApplicationContext context: Context,
        database: BasketGuardDatabase,
        api: BasketGuardApi,
    ) = BasketRepository(context, database, api)

    @Provides
    @Singleton
    fun provideBasketPlanGateway(repository: BasketRepository): BasketPlanGateway = repository

    @Provides
    @Singleton
    fun provideGenerateBasketPlan(gateway: BasketPlanGateway) = GenerateBasketPlanUseCase(gateway)

    @Provides
    @Singleton
    fun provideBasketAnalytics(@ApplicationContext context: Context): BasketAnalytics =
        FirebaseBasketAnalytics(context)
}

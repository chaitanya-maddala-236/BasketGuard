package com.basketguard.android.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.basketguard.android.BasketGuardApplication
import com.basketguard.android.di.WorkerEntryPoint
import dagger.hilt.android.EntryPointAccessors
import java.io.IOException
import retrofit2.HttpException

class CartSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        val dependencies = EntryPointAccessors.fromApplication(
            applicationContext as BasketGuardApplication,
            WorkerEntryPoint::class.java,
        )
        dependencies.basketRepository().syncCart()
        Result.success()
    } catch (error: IOException) {
        Result.retry()
    } catch (error: HttpException) {
        if (error.code() >= 500) Result.retry() else Result.failure()
    } catch (_: Exception) {
        Result.failure()
    }
}

fun scheduleCartSync(context: Context) {
    val request = OneTimeWorkRequestBuilder<CartSyncWorker>()
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .build()
    WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
        "basketguard-cart-sync", ExistingWorkPolicy.REPLACE, request,
    )
}

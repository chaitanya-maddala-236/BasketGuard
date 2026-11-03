package com.basketguard.android

import android.app.Application
import com.basketguard.android.data.sync.scheduleCartSync
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BasketGuardApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        scheduleCartSync(this)
    }
}

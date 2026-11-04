package com.basketguard.android.data.analytics

import android.content.Context
import android.os.Bundle
import com.basketguard.android.analytics.BasketAnalytics
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics

class FirebaseBasketAnalytics(context: Context) : BasketAnalytics {
    private val analytics = if (FirebaseApp.getApps(context).isNotEmpty()) {
        FirebaseAnalytics.getInstance(context)
    } else {
        null
    }

    override fun event(name: String, parameters: Map<String, Any>) {
        val client = analytics ?: return
        val bundle = Bundle().apply {
            parameters.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Long -> putLong(key, value)
                    is Int -> putLong(key, value.toLong())
                    is Double -> putDouble(key, value)
                    is Boolean -> putBoolean(key, value)
                }
            }
        }
        client.logEvent(name, bundle)
    }
}

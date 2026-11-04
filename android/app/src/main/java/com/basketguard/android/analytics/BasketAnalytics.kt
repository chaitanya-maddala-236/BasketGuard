package com.basketguard.android.analytics

interface BasketAnalytics {
    fun event(name: String, parameters: Map<String, Any> = emptyMap())
}

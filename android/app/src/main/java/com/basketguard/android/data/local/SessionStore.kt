package com.basketguard.android.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.sessionDataStore by preferencesDataStore(name = "basketguard_session")

class SessionStore(private val context: Context) {
    private val tokenKey = stringPreferencesKey("access_token")
    private val nameKey = stringPreferencesKey("account_name")
    private val checkoutKey = stringPreferencesKey("checkout_idempotency_key")

    suspend fun save(token: String, name: String) {
        context.sessionDataStore.edit { preferences ->
            preferences[tokenKey] = token
            preferences[nameKey] = name
        }
    }

    suspend fun token(): String? = context.sessionDataStore.data.first()[tokenKey]
    suspend fun name(): String? = context.sessionDataStore.data.first()[nameKey]

    suspend fun checkoutKey(): String {
        val existing = context.sessionDataStore.data.first()[checkoutKey]
        if (existing != null) return existing
        val created = java.util.UUID.randomUUID().toString()
        context.sessionDataStore.edit { preferences ->
            if (preferences[checkoutKey] == null) preferences[checkoutKey] = created
        }
        return context.sessionDataStore.data.first()[checkoutKey] ?: created
    }

    suspend fun clearCheckoutKey() {
        context.sessionDataStore.edit { it.remove(checkoutKey) }
    }

    suspend fun clear() {
        context.sessionDataStore.edit { preferences ->
            preferences.remove(tokenKey)
            preferences.remove(nameKey)
            preferences.remove(checkoutKey)
        }
    }
}

package com.arverse.arbattery.companion

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

val Context.companionDataStore by preferencesDataStore(name = "arbattery_companion_prefs")

data class CompanionConfig(
    val deviceId: String,
    val hubIp: String,
    val hubPort: Int,
    val authToken: String?,
    val isPaired: Boolean,
    val lastSyncTimestamp: Long?
)

class CompanionPreferences(private val context: Context) {

    private val KEY_DEVICE_ID = stringPreferencesKey("device_id")
    private val KEY_HUB_IP = stringPreferencesKey("hub_ip")
    private val KEY_HUB_PORT = intPreferencesKey("hub_port")
    private val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
    private val KEY_IS_PAIRED = booleanPreferencesKey("is_paired")
    private val KEY_LAST_SYNC = longPreferencesKey("last_sync_timestamp")

    val configFlow: Flow<CompanionConfig> = context.companionDataStore.data.map { prefs ->
        val id = prefs[KEY_DEVICE_ID] ?: run {
            "tab-${UUID.randomUUID().toString().take(8)}"
        }
        CompanionConfig(
            deviceId = id,
            hubIp = prefs[KEY_HUB_IP] ?: "192.168.1.100",
            hubPort = prefs[KEY_HUB_PORT] ?: 8443,
            authToken = prefs[KEY_AUTH_TOKEN],
            isPaired = prefs[KEY_IS_PAIRED] ?: false,
            lastSyncTimestamp = prefs[KEY_LAST_SYNC]
        )
    }

    suspend fun getOrGenerateDeviceId(): String {
        val prefs = context.companionDataStore.data.first()
        val existing = prefs[KEY_DEVICE_ID]
        if (existing != null) return existing

        val newId = "tab-${UUID.randomUUID().toString().take(8)}"
        context.companionDataStore.edit { it[KEY_DEVICE_ID] = newId }
        return newId
    }

    suspend fun savePairing(hubIp: String, hubPort: Int, authToken: String) {
        context.companionDataStore.edit { prefs ->
            prefs[KEY_HUB_IP] = hubIp
            prefs[KEY_HUB_PORT] = hubPort
            prefs[KEY_AUTH_TOKEN] = authToken
            prefs[KEY_IS_PAIRED] = true
        }
    }

    suspend fun updateLastSync(timestamp: Long = System.currentTimeMillis()) {
        context.companionDataStore.edit { prefs ->
            prefs[KEY_LAST_SYNC] = timestamp
        }
    }

    suspend fun clearPairing() {
        context.companionDataStore.edit { prefs ->
            prefs.remove(KEY_AUTH_TOKEN)
            prefs[KEY_IS_PAIRED] = false
            prefs.remove(KEY_LAST_SYNC)
        }
    }
}

package com.arverse.arbattery.feature.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

val Context.dataStore by preferencesDataStore(name = "arbattery_settings")

data class SettingsUiState(
    val lowBatteryThreshold: Int = 20,
    val criticalBatteryThreshold: Int = 10,
    val isNotificationsEnabled: Boolean = true,
    val isVibrationEnabled: Boolean = true,
    val hubPort: Int = 8443,
    val dataRetentionDays: Int = 14,
    val periodicSyncIntervalMinutes: Int = 15,
    val infoMessage: String? = null
)

class SettingsViewModel(
    private val appContext: Context
) : ViewModel() {

    private val KEY_LOW_THRESHOLD = intPreferencesKey("low_battery_threshold")
    private val KEY_CRITICAL_THRESHOLD = intPreferencesKey("critical_battery_threshold")
    private val KEY_NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
    private val KEY_VIBRATION = booleanPreferencesKey("vibration_enabled")
    private val KEY_HUB_PORT = intPreferencesKey("hub_port")
    private val KEY_RETENTION_DAYS = intPreferencesKey("retention_days")
    private val KEY_SYNC_INTERVAL = intPreferencesKey("sync_interval_minutes")

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val prefs = appContext.dataStore.data.first()
            _uiState.update {
                it.copy(
                    lowBatteryThreshold = prefs[KEY_LOW_THRESHOLD] ?: 20,
                    criticalBatteryThreshold = prefs[KEY_CRITICAL_THRESHOLD] ?: 10,
                    isNotificationsEnabled = prefs[KEY_NOTIFICATIONS] ?: true,
                    isVibrationEnabled = prefs[KEY_VIBRATION] ?: true,
                    hubPort = prefs[KEY_HUB_PORT] ?: 8443,
                    dataRetentionDays = prefs[KEY_RETENTION_DAYS] ?: 14,
                    periodicSyncIntervalMinutes = prefs[KEY_SYNC_INTERVAL] ?: 15
                )
            }
        }
    }

    fun setLowBatteryThreshold(threshold: Int) {
        val safeVal = threshold.coerceIn(5, 40)
        _uiState.update { it.copy(lowBatteryThreshold = safeVal) }
        savePreference(KEY_LOW_THRESHOLD, safeVal)
    }

    fun setCriticalBatteryThreshold(threshold: Int) {
        val safeVal = threshold.coerceIn(1, 20)
        _uiState.update { it.copy(criticalBatteryThreshold = safeVal) }
        savePreference(KEY_CRITICAL_THRESHOLD, safeVal)
    }

    fun toggleNotifications(enabled: Boolean) {
        _uiState.update { it.copy(isNotificationsEnabled = enabled) }
        savePreference(KEY_NOTIFICATIONS, enabled)
    }

    fun toggleVibration(enabled: Boolean) {
        _uiState.update { it.copy(isVibrationEnabled = enabled) }
        savePreference(KEY_VIBRATION, enabled)
    }

    fun setHubPort(port: Int) {
        val safePort = port.coerceIn(1024, 65535)
        _uiState.update { it.copy(hubPort = safePort) }
        savePreference(KEY_HUB_PORT, safePort)
    }

    fun setDataRetentionDays(days: Int) {
        _uiState.update { it.copy(dataRetentionDays = days) }
        savePreference(KEY_RETENTION_DAYS, days)
    }

    fun setPeriodicSyncInterval(minutes: Int) {
        val safeMin = minutes.coerceIn(15, 360)
        _uiState.update { it.copy(periodicSyncIntervalMinutes = safeMin) }
        savePreference(KEY_SYNC_INTERVAL, safeMin)
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            appContext.dataStore.edit { it.clear() }
            _uiState.value = SettingsUiState(infoMessage = "Settings restored to defaults.")
        }
    }

    fun clearInfoMessage() {
        _uiState.update { it.copy(infoMessage = null) }
    }

    private fun <T> savePreference(key: androidx.datastore.preferences.core.Preferences.Key<T>, value: T) {
        viewModelScope.launch {
            appContext.dataStore.edit { prefs ->
                prefs[key] = value
            }
        }
    }
}

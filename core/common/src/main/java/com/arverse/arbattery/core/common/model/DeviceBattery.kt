package com.arverse.arbattery.core.common.model

import kotlinx.serialization.Serializable

@Serializable
data class DeviceBattery(
    val deviceId: String,
    val deviceName: String,
    val deviceType: DeviceType = DeviceType.UNKNOWN,
    val batteryPercentage: Int? = null,
    val chargingState: ChargingState = ChargingState.UNKNOWN,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val connectionMethod: ConnectionMethod = ConnectionMethod.BLE,
    val lastUpdated: Long = System.currentTimeMillis(),
    val lastSuccessfulReading: Long? = null,
    val capabilityStatus: ReadingQuality = ReadingQuality.UNAVAILABLE,
    val errorState: String? = null,
    val manufacturer: String? = null,
    val model: String? = null
) {
    init {
        batteryPercentage?.let {
            require(it in 0..100) { "Battery percentage must be between 0 and 100 or null, got: $it" }
        }
    }

    val isAvailable: Boolean
        get() = batteryPercentage != null && capabilityStatus != ReadingQuality.UNAVAILABLE && capabilityStatus != ReadingQuality.UNSUPPORTED

    val displayPercentage: String
        get() = batteryPercentage?.let { "$it%" } ?: "--"

    fun isLow(threshold: Int = 20): Boolean {
        val pct = batteryPercentage ?: return false
        return pct <= threshold && pct > 10
    }

    fun isCritical(threshold: Int = 10): Boolean {
        val pct = batteryPercentage ?: return false
        return pct <= threshold
    }
}

package com.arverse.arbattery.core.bluetooth.model

import com.arverse.arbattery.core.common.model.DeviceType
import kotlinx.serialization.Serializable

@Serializable
data class BleScanDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val advertisedServiceUuids: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val deviceType: DeviceType = DeviceType.UNKNOWN,
    val isConnectable: Boolean = true
) {
    val displayName: String
        get() = name.ifBlank { "Unknown Peripheral ($address)" }

    val signalStrengthPercentage: Int
        get() = when {
            rssi >= -50 -> 100
            rssi <= -100 -> 0
            else -> 2 * (rssi + 100)
        }
}

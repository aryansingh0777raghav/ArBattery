package com.arverse.arbattery.core.common.model

import kotlinx.serialization.Serializable

@Serializable
enum class ConnectionMethod(val label: String) {
    BLE("Bluetooth LE"),
    WIFI("Local Wi-Fi"),
    COMPANION("Companion App"),
    BLUETOOTH_CLASSIC("Bluetooth Classic"),
    MANUAL("Manual")
}

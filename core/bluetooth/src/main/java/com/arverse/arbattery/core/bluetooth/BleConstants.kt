package com.arverse.arbattery.core.bluetooth

import java.util.UUID

object BleConstants {
    // Bluetooth SIG Standard Battery Service
    val BATTERY_SERVICE_UUID: UUID = UUID.fromString("0000180F-0000-1000-8000-00805F9B34FB")
    val BATTERY_LEVEL_CHARACTERISTIC_UUID: UUID = UUID.fromString("00002A19-0000-1000-8000-00805F9B34FB")

    // Client Characteristic Configuration Descriptor (CCCD) for Notifications & Indications
    val CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")

    // Device Information Service
    val DEVICE_INFORMATION_SERVICE_UUID: UUID = UUID.fromString("0000180A-0000-1000-8000-00805F9B34FB")
    val MANUFACTURER_NAME_STRING_UUID: UUID = UUID.fromString("00002A29-0000-1000-8000-00805F9B34FB")
    val MODEL_NUMBER_STRING_UUID: UUID = UUID.fromString("00002A24-0000-1000-8000-00805F9B34FB")
    val FIRMWARE_REVISION_STRING_UUID: UUID = UUID.fromString("00002A26-0000-1000-8000-00805F9B34FB")
    val HARDWARE_REVISION_STRING_UUID: UUID = UUID.fromString("00002A27-0000-1000-8000-00805F9B34FB")

    // Scan settings
    const val DEFAULT_SCAN_TIMEOUT_MS = 15_000L
    const val GATT_CONNECT_TIMEOUT_MS = 10_000L
    const val GATT_OPERATION_TIMEOUT_MS = 5_000L

    fun getServiceName(uuid: UUID): String {
        return when (uuid) {
            BATTERY_SERVICE_UUID -> "Battery Service (0x180F)"
            DEVICE_INFORMATION_SERVICE_UUID -> "Device Information Service (0x180A)"
            UUID.fromString("00001800-0000-1000-8000-00805F9B34FB") -> "Generic Access (0x1800)"
            UUID.fromString("00001801-0000-1000-8000-00805F9B34FB") -> "Generic Attribute (0x1801)"
            UUID.fromString("0000180D-0000-1000-8000-00805F9B34FB") -> "Heart Rate (0x180D)"
            else -> "Vendor Specific (${uuid.toString().substring(0, 8)})"
        }
    }
}

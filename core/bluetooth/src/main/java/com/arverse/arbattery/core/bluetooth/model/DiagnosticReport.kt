package com.arverse.arbattery.core.bluetooth.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class DiagnosticReport(
    val deviceName: String,
    val deviceAddress: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isStandardBatteryServiceFound: Boolean,
    val isBatteryLevelCharacteristicFound: Boolean,
    val batteryCharacteristicCanNotify: Boolean,
    val batteryCharacteristicCanRead: Boolean,
    val batteryValueRead: Int? = null,
    val totalServicesCount: Int,
    val totalCharacteristicsCount: Int,
    val services: List<GattServiceInfo>,
    val diagnosticSummary: String,
    val compatibilityVerdict: CompatibilityVerdict
) {
    @Serializable
    enum class CompatibilityVerdict {
        COMPATIBLE_STANDARD_BLE,
        UNSUPPORTED_STANDARD_SERVICE_MISSING,
        UNSUPPORTED_BATTERY_CHAR_MISSING,
        CONNECTION_FAILED,
        READ_FAILED
    }

    fun exportAsJson(): String {
        val json = Json { prettyPrint = true }
        return json.encodeToString(this)
    }

    fun exportAsText(): String {
        return buildString {
            appendLine("=== ARBATTERY BLE DIAGNOSTIC REPORT ===")
            appendLine("Device Name: $deviceName")
            appendLine("Device Address: $deviceAddress")
            appendLine("Timestamp: $timestamp")
            appendLine("Verdict: $compatibilityVerdict")
            appendLine("Standard Battery Service (0x180F): ${if (isStandardBatteryServiceFound) "PRESENT" else "NOT FOUND"}")
            appendLine("Battery Level Char (0x2A19): ${if (isBatteryLevelCharacteristicFound) "PRESENT" else "NOT FOUND"}")
            appendLine("Battery Level Value Read: ${batteryValueRead?.let { "$it%" } ?: "NONE"}")
            appendLine("Total Services Discovered: $totalServicesCount")
            appendLine("Total Characteristics Discovered: $totalCharacteristicsCount")
            appendLine("Summary: $diagnosticSummary")
            appendLine("----------------------------------------")
            appendLine("Discovered GATT Profile:")
            services.forEach { svc ->
                appendLine("  [Service] ${svc.serviceName} (${svc.uuid})")
                svc.characteristics.forEach { ch ->
                    appendLine("    [Char] ${ch.uuid} | Props: ${ch.propertyNames.joinToString(", ")}")
                }
            }
            appendLine("========================================")
        }
    }
}

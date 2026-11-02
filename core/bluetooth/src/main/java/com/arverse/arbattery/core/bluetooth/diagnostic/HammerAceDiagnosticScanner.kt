package com.arverse.arbattery.core.bluetooth.diagnostic

import com.arverse.arbattery.core.bluetooth.BleConstants
import com.arverse.arbattery.core.bluetooth.gatt.BluetoothConnectionManager
import com.arverse.arbattery.core.bluetooth.model.DiagnosticReport
import com.arverse.arbattery.core.bluetooth.model.GattCharacteristicInfo
import com.arverse.arbattery.core.bluetooth.model.GattProfile
import com.arverse.arbattery.core.common.result.AppResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

sealed interface DiagnosticProgress {
    data object Idle : DiagnosticProgress
    data class Connecting(val address: String) : DiagnosticProgress
    data class DiscoveringServices(val address: String) : DiagnosticProgress
    data class ProbingBatteryService(val address: String) : DiagnosticProgress
    data class ReadingBatteryLevel(val address: String) : DiagnosticProgress
    data class Completed(val report: DiagnosticReport) : DiagnosticProgress
    data class Failed(val reason: String, val partialReport: DiagnosticReport? = null) : DiagnosticProgress
}

class HammerAceDiagnosticScanner(
    private val connectionManager: BluetoothConnectionManager
) {
    private val _progress = MutableStateFlow<DiagnosticProgress>(DiagnosticProgress.Idle)
    val progress: StateFlow<DiagnosticProgress> = _progress.asStateFlow()

    suspend fun runDiagnostic(
        address: String,
        deviceName: String = "Hammer Ace"
    ): DiagnosticReport {
        _progress.value = DiagnosticProgress.Connecting(address)

        val connectResult = connectionManager.connect(address)
        if (connectResult is AppResult.Error) {
            val report = DiagnosticReport(
                deviceName = deviceName,
                deviceAddress = address,
                isStandardBatteryServiceFound = false,
                isBatteryLevelCharacteristicFound = false,
                batteryCharacteristicCanNotify = false,
                batteryCharacteristicCanRead = false,
                batteryValueRead = null,
                totalServicesCount = 0,
                totalCharacteristicsCount = 0,
                services = emptyList(),
                diagnosticSummary = "GATT connection to $address failed: ${connectResult.message}",
                compatibilityVerdict = DiagnosticReport.CompatibilityVerdict.CONNECTION_FAILED
            )
            _progress.value = DiagnosticProgress.Failed(connectResult.message ?: "Connection failed", report)
            return report
        }

        _progress.value = DiagnosticProgress.DiscoveringServices(address)
        val servicesResult = connectionManager.discoverServices()
        if (servicesResult is AppResult.Error) {
            val report = DiagnosticReport(
                deviceName = deviceName,
                deviceAddress = address,
                isStandardBatteryServiceFound = false,
                isBatteryLevelCharacteristicFound = false,
                batteryCharacteristicCanNotify = false,
                batteryCharacteristicCanRead = false,
                batteryValueRead = null,
                totalServicesCount = 0,
                totalCharacteristicsCount = 0,
                services = emptyList(),
                diagnosticSummary = "GATT service discovery failed: ${servicesResult.message}",
                compatibilityVerdict = DiagnosticReport.CompatibilityVerdict.CONNECTION_FAILED
            )
            _progress.value = DiagnosticProgress.Failed(servicesResult.message ?: "Service discovery failed", report)
            connectionManager.disconnect()
            return report
        }

        val profile = (servicesResult as AppResult.Success).data
        _progress.value = DiagnosticProgress.ProbingBatteryService(address)

        val batteryService = profile.services.firstOrNull {
            it.uuid.equals(BleConstants.BATTERY_SERVICE_UUID.toString(), ignoreCase = true)
        }

        val totalChars = profile.services.sumOf { it.characteristics.size }

        if (batteryService == null) {
            val report = DiagnosticReport(
                deviceName = deviceName,
                deviceAddress = address,
                isStandardBatteryServiceFound = false,
                isBatteryLevelCharacteristicFound = false,
                batteryCharacteristicCanNotify = false,
                batteryCharacteristicCanRead = false,
                batteryValueRead = null,
                totalServicesCount = profile.services.size,
                totalCharacteristicsCount = totalChars,
                services = profile.services,
                diagnosticSummary = "Standard BLE Battery Service (0x180F) NOT exposed by $deviceName. Device uses proprietary communication services.",
                compatibilityVerdict = DiagnosticReport.CompatibilityVerdict.UNSUPPORTED_STANDARD_SERVICE_MISSING
            )
            _progress.value = DiagnosticProgress.Completed(report)
            connectionManager.disconnect()
            return report
        }

        val batteryChar = batteryService.characteristics.firstOrNull {
            it.uuid.equals(BleConstants.BATTERY_LEVEL_CHARACTERISTIC_UUID.toString(), ignoreCase = true)
        }

        if (batteryChar == null) {
            val report = DiagnosticReport(
                deviceName = deviceName,
                deviceAddress = address,
                isStandardBatteryServiceFound = true,
                isBatteryLevelCharacteristicFound = false,
                batteryCharacteristicCanNotify = false,
                batteryCharacteristicCanRead = false,
                batteryValueRead = null,
                totalServicesCount = profile.services.size,
                totalCharacteristicsCount = totalChars,
                services = profile.services,
                diagnosticSummary = "Battery Service (0x180F) present, but Battery Level characteristic (0x2A19) is missing.",
                compatibilityVerdict = DiagnosticReport.CompatibilityVerdict.UNSUPPORTED_BATTERY_CHAR_MISSING
            )
            _progress.value = DiagnosticProgress.Completed(report)
            connectionManager.disconnect()
            return report
        }

        _progress.value = DiagnosticProgress.ReadingBatteryLevel(address)
        val readResult = connectionManager.readCharacteristic(
            BleConstants.BATTERY_SERVICE_UUID,
            BleConstants.BATTERY_LEVEL_CHARACTERISTIC_UUID
        )

        val batteryLevel = when (readResult) {
            is AppResult.Success -> {
                val bytes = readResult.data
                if (bytes.isNotEmpty()) bytes[0].toInt() and 0xFF else null
            }
            else -> null
        }

        val verdict = if (batteryLevel != null && batteryLevel in 0..100) {
            DiagnosticReport.CompatibilityVerdict.COMPATIBLE_STANDARD_BLE
        } else {
            DiagnosticReport.CompatibilityVerdict.READ_FAILED
        }

        val summary = if (verdict == DiagnosticReport.CompatibilityVerdict.COMPATIBLE_STANDARD_BLE) {
            "Fully compatible! Standard Battery Service (0x180F) and Level characteristic (0x2A19) verified. Current reading: $batteryLevel%."
        } else {
            "Battery service found, but reading characteristic returned invalid or null value."
        }

        val finalReport = DiagnosticReport(
            deviceName = deviceName,
            deviceAddress = address,
            isStandardBatteryServiceFound = true,
            isBatteryLevelCharacteristicFound = true,
            batteryCharacteristicCanNotify = batteryChar.canNotify,
            batteryCharacteristicCanRead = batteryChar.canRead,
            batteryValueRead = batteryLevel,
            totalServicesCount = profile.services.size,
            totalCharacteristicsCount = totalChars,
            services = profile.services,
            diagnosticSummary = summary,
            compatibilityVerdict = verdict
        )

        _progress.value = DiagnosticProgress.Completed(finalReport)
        connectionManager.disconnect()
        return finalReport
    }
}

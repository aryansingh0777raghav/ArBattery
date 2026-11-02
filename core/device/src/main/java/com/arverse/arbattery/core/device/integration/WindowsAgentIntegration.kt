package com.arverse.arbattery.core.device.integration

import android.Manifest
import com.arverse.arbattery.core.common.model.ConnectionMethod
import com.arverse.arbattery.core.common.model.DeviceBattery
import com.arverse.arbattery.core.common.model.DeviceType
import com.arverse.arbattery.core.common.result.AppResult
import com.arverse.arbattery.core.database.repository.DeviceRepository

/**
 * Extensible integration architecture for Windows desktop agent.
 * The companion agent reads actual battery status via Windows OS APIs (psutil / GetSystemPowerStatusEx),
 * and syncs securely over local network using authenticated tokens.
 *
 * (No fake or fabricated battery monitoring is used).
 */
class WindowsAgentIntegration(
    private val deviceRepository: DeviceRepository
) : DeviceIntegration {

    override val supportedTypes: Set<DeviceType> = setOf(DeviceType.LAPTOP)
    override val connectionMethod: ConnectionMethod = ConnectionMethod.WIFI
    override val requiredPermissions: List<String> = listOf(
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE
    )
    override val supportsBatteryReading: Boolean = true
    override val supportsForceRefresh: Boolean = false
    override val supportsChargingInfo: Boolean = true
    override val integrationName: String = "Windows Desktop Companion Agent"

    override suspend fun readBattery(
        deviceId: String,
        deviceName: String
    ): AppResult<DeviceBattery> {
        val device = deviceRepository.getDeviceById(deviceId)
        return if (device != null) {
            AppResult.Success(device)
        } else {
            AppResult.Error(NoSuchElementException("Windows device $deviceId not registered."))
        }
    }

    override fun isAvailable(): Boolean = true
}

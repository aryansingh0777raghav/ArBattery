package com.arverse.arbattery.core.device.integration

import android.Manifest
import com.arverse.arbattery.core.common.model.ConnectionMethod
import com.arverse.arbattery.core.common.model.DeviceBattery
import com.arverse.arbattery.core.common.model.DeviceType
import com.arverse.arbattery.core.common.model.ReadingQuality
import com.arverse.arbattery.core.common.result.AppResult
import com.arverse.arbattery.core.database.repository.DeviceRepository

class AndroidCompanionIntegration(
    private val deviceRepository: DeviceRepository
) : DeviceIntegration {

    override val supportedTypes: Set<DeviceType> = setOf(
        DeviceType.TABLET,
        DeviceType.SMARTPHONE
    )

    override val connectionMethod: ConnectionMethod = ConnectionMethod.COMPANION
    override val requiredPermissions: List<String> = listOf(
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE
    )
    override val supportsBatteryReading: Boolean = true
    override val supportsForceRefresh: Boolean = false // Push-based from companion
    override val supportsChargingInfo: Boolean = true
    override val integrationName: String = "ArBattery Android Companion Synchronization"

    override suspend fun readBattery(
        deviceId: String,
        deviceName: String
    ): AppResult<DeviceBattery> {
        val device = deviceRepository.getDeviceById(deviceId)
        return if (device != null) {
            AppResult.Success(device)
        } else {
            AppResult.Error(NoSuchElementException("Companion device $deviceId not registered."))
        }
    }

    override fun isAvailable(): Boolean = true
}

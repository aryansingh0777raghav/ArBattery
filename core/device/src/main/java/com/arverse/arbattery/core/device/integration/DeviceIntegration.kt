package com.arverse.arbattery.core.device.integration

import com.arverse.arbattery.core.common.model.ConnectionMethod
import com.arverse.arbattery.core.common.model.DeviceBattery
import com.arverse.arbattery.core.common.model.DeviceType
import com.arverse.arbattery.core.common.result.AppResult

interface DeviceIntegration {
    val supportedTypes: Set<DeviceType>
    val connectionMethod: ConnectionMethod
    val requiredPermissions: List<String>
    val supportsBatteryReading: Boolean
    val supportsForceRefresh: Boolean
    val supportsChargingInfo: Boolean
    val integrationName: String

    suspend fun readBattery(deviceId: String, deviceName: String = "Device"): AppResult<DeviceBattery>
    suspend fun refresh(deviceId: String): AppResult<DeviceBattery> = readBattery(deviceId)
    fun isAvailable(): Boolean
}

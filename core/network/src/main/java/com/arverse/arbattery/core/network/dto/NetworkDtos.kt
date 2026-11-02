package com.arverse.arbattery.core.network.dto

import com.arverse.arbattery.core.common.model.ChargingState
import com.arverse.arbattery.core.common.model.ConnectionMethod
import com.arverse.arbattery.core.common.model.ConnectionState
import com.arverse.arbattery.core.common.model.DeviceBattery
import com.arverse.arbattery.core.common.model.DeviceType
import com.arverse.arbattery.core.common.model.ReadingQuality
import kotlinx.serialization.Serializable

@Serializable
data class PairingRequestDto(
    val deviceId: String,
    val deviceName: String,
    val deviceType: String,
    val timestamp: Long,
    val clientNonce: String
)

@Serializable
data class PairingConfirmDto(
    val deviceId: String,
    val authToken: String,
    val timestamp: Long
)

@Serializable
data class PairingResponseDto(
    val status: String,
    val hubName: String,
    val authToken: String? = null,
    val message: String? = null
)

@Serializable
data class BatteryReportDto(
    val deviceId: String,
    val deviceName: String,
    val deviceType: String,
    val batteryPercentage: Int,
    val charging: Boolean,
    val chargingState: String = if (charging) "CHARGING" else "DISCHARGING",
    val connectionMethod: String = "WIFI",
    val timestamp: Long = System.currentTimeMillis(),
    val manufacturer: String? = null,
    val model: String? = null
) {
    fun toDomainModel(): DeviceBattery {
        val type = try { DeviceType.valueOf(deviceType) } catch (_: Exception) { DeviceType.TABLET }
        val chState = try { ChargingState.valueOf(chargingState) } catch (_: Exception) {
            if (charging) ChargingState.CHARGING else ChargingState.DISCHARGING
        }
        val connMethod = try { ConnectionMethod.valueOf(connectionMethod) } catch (_: Exception) { ConnectionMethod.WIFI }

        return DeviceBattery(
            deviceId = deviceId,
            deviceName = deviceName,
            deviceType = type,
            batteryPercentage = batteryPercentage.coerceIn(0, 100),
            chargingState = chState,
            connectionState = ConnectionState.CONNECTED,
            connectionMethod = connMethod,
            lastUpdated = timestamp,
            lastSuccessfulReading = timestamp,
            capabilityStatus = ReadingQuality.LIVE,
            manufacturer = manufacturer,
            model = model
        )
    }
}

@Serializable
data class SyncRequestDto(
    val deviceId: String,
    val reports: List<BatteryReportDto>
)

@Serializable
data class SyncResponseDto(
    val status: String,
    val processedCount: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class DeviceDto(
    val deviceId: String,
    val deviceName: String,
    val deviceType: String,
    val batteryPercentage: Int?,
    val chargingState: String,
    val connectionState: String,
    val lastUpdated: Long
)

@Serializable
data class ApiResponseDto<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

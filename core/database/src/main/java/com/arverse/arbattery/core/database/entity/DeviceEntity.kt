package com.arverse.arbattery.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.arverse.arbattery.core.common.model.ChargingState
import com.arverse.arbattery.core.common.model.ConnectionMethod
import com.arverse.arbattery.core.common.model.ConnectionState
import com.arverse.arbattery.core.common.model.DeviceBattery
import com.arverse.arbattery.core.common.model.DeviceType
import com.arverse.arbattery.core.common.model.ReadingQuality

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: DeviceType,
    val manufacturer: String? = null,
    val model: String? = null,
    val connectionMethod: ConnectionMethod,
    val connectionStatus: ConnectionState,
    val batteryPercentage: Int?,
    val chargingState: ChargingState,
    val lastUpdated: Long,
    val lastSuccessfulReading: Long?,
    val capabilityStatus: ReadingQuality,
    val errorState: String? = null
) {
    fun toDomainModel(): DeviceBattery = DeviceBattery(
        deviceId = id,
        deviceName = name,
        deviceType = type,
        batteryPercentage = batteryPercentage,
        chargingState = chargingState,
        connectionState = connectionStatus,
        connectionMethod = connectionMethod,
        lastUpdated = lastUpdated,
        lastSuccessfulReading = lastSuccessfulReading,
        capabilityStatus = capabilityStatus,
        errorState = errorState,
        manufacturer = manufacturer,
        model = model
    )

    companion object {
        fun fromDomainModel(battery: DeviceBattery): DeviceEntity = DeviceEntity(
            id = battery.deviceId,
            name = battery.deviceName,
            type = battery.deviceType,
            manufacturer = battery.manufacturer,
            model = battery.model,
            connectionMethod = battery.connectionMethod,
            connectionStatus = battery.connectionState,
            batteryPercentage = battery.batteryPercentage,
            chargingState = battery.chargingState,
            lastUpdated = battery.lastUpdated,
            lastSuccessfulReading = battery.lastSuccessfulReading,
            capabilityStatus = battery.capabilityStatus,
            errorState = battery.errorState
        )
    }
}

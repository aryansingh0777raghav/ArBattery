package com.arverse.arbattery.core.database.converter

import androidx.room.TypeConverter
import com.arverse.arbattery.core.common.model.ChargingState
import com.arverse.arbattery.core.common.model.ConnectionMethod
import com.arverse.arbattery.core.common.model.ConnectionState
import com.arverse.arbattery.core.common.model.DeviceType
import com.arverse.arbattery.core.common.model.ReadingQuality

class DatabaseConverters {

    @TypeConverter
    fun fromDeviceType(value: DeviceType?): String = value?.name ?: DeviceType.UNKNOWN.name

    @TypeConverter
    fun toDeviceType(value: String?): DeviceType = try {
        value?.let { DeviceType.valueOf(it) } ?: DeviceType.UNKNOWN
    } catch (_: Exception) {
        DeviceType.UNKNOWN
    }

    @TypeConverter
    fun fromConnectionMethod(value: ConnectionMethod?): String = value?.name ?: ConnectionMethod.BLE.name

    @TypeConverter
    fun toConnectionMethod(value: String?): ConnectionMethod = try {
        value?.let { ConnectionMethod.valueOf(it) } ?: ConnectionMethod.BLE
    } catch (_: Exception) {
        ConnectionMethod.BLE
    }

    @TypeConverter
    fun fromConnectionState(value: ConnectionState?): String = value?.name ?: ConnectionState.DISCONNECTED.name

    @TypeConverter
    fun toConnectionState(value: String?): ConnectionState = try {
        value?.let { ConnectionState.valueOf(it) } ?: ConnectionState.DISCONNECTED
    } catch (_: Exception) {
        ConnectionState.DISCONNECTED
    }

    @TypeConverter
    fun fromChargingState(value: ChargingState?): String = value?.name ?: ChargingState.UNKNOWN.name

    @TypeConverter
    fun toChargingState(value: String?): ChargingState = try {
        value?.let { ChargingState.valueOf(it) } ?: ChargingState.UNKNOWN
    } catch (_: Exception) {
        ChargingState.UNKNOWN
    }

    @TypeConverter
    fun fromReadingQuality(value: ReadingQuality?): String = value?.name ?: ReadingQuality.UNAVAILABLE.name

    @TypeConverter
    fun toReadingQuality(value: String?): ReadingQuality = try {
        value?.let { ReadingQuality.valueOf(it) } ?: ReadingQuality.UNAVAILABLE
    } catch (_: Exception) {
        ReadingQuality.UNAVAILABLE
    }
}

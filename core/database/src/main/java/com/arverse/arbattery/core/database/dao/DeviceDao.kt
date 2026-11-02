package com.arverse.arbattery.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.arverse.arbattery.core.common.model.ChargingState
import com.arverse.arbattery.core.common.model.ConnectionState
import com.arverse.arbattery.core.common.model.ReadingQuality
import com.arverse.arbattery.core.database.entity.DeviceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {

    @Query("SELECT * FROM devices ORDER BY name ASC")
    fun getAllDevicesFlow(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM devices ORDER BY name ASC")
    suspend fun getAllDevices(): List<DeviceEntity>

    @Query("SELECT * FROM devices WHERE id = :deviceId LIMIT 1")
    fun getDeviceByIdFlow(deviceId: String): Flow<DeviceEntity?>

    @Query("SELECT * FROM devices WHERE id = :deviceId LIMIT 1")
    suspend fun getDeviceById(deviceId: String): DeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDevice(device: DeviceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDevices(devices: List<DeviceEntity>)

    @Update
    suspend fun updateDevice(device: DeviceEntity)

    @Query("""
        UPDATE devices 
        SET batteryPercentage = :percentage,
            chargingState = :chargingState,
            lastUpdated = :lastUpdated,
            lastSuccessfulReading = CASE WHEN :percentage IS NOT NULL THEN :lastUpdated ELSE lastSuccessfulReading END,
            capabilityStatus = :quality,
            errorState = :errorState,
            connectionStatus = :connectionState
        WHERE id = :deviceId
    """)
    suspend fun updateBatteryReading(
        deviceId: String,
        percentage: Int?,
        chargingState: ChargingState,
        lastUpdated: Long,
        quality: ReadingQuality,
        errorState: String?,
        connectionState: ConnectionState
    )

    @Query("UPDATE devices SET connectionStatus = :state WHERE id = :deviceId")
    suspend fun updateConnectionState(deviceId: String, state: ConnectionState)

    @Delete
    suspend fun deleteDevice(device: DeviceEntity)

    @Query("DELETE FROM devices WHERE id = :deviceId")
    suspend fun deleteDeviceById(deviceId: String)

    @Query("DELETE FROM devices")
    suspend fun clearAllDevices()

    @Query("SELECT COUNT(*) FROM devices")
    suspend fun getDeviceCount(): Int

    @Query("SELECT COUNT(*) FROM devices WHERE connectionStatus = 'CONNECTED'")
    suspend fun getConnectedDeviceCount(): Int
}

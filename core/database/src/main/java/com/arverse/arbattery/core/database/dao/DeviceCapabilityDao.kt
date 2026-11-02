package com.arverse.arbattery.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.arverse.arbattery.core.database.entity.DeviceCapabilityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceCapabilityDao {

    @Query("SELECT * FROM device_capabilities WHERE deviceId = :deviceId LIMIT 1")
    suspend fun getCapability(deviceId: String): DeviceCapabilityEntity?

    @Query("SELECT * FROM device_capabilities WHERE deviceId = :deviceId LIMIT 1")
    fun getCapabilityFlow(deviceId: String): Flow<DeviceCapabilityEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCapability(capability: DeviceCapabilityEntity)

    @Query("DELETE FROM device_capabilities WHERE deviceId = :deviceId")
    suspend fun deleteCapability(deviceId: String)
}

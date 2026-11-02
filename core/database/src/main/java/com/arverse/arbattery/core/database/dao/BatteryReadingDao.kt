package com.arverse.arbattery.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.arverse.arbattery.core.database.entity.BatteryReadingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BatteryReadingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(reading: BatteryReadingEntity): Long

    @Query("SELECT * FROM battery_readings WHERE deviceId = :deviceId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentReadingsFlow(deviceId: String, limit: Int = 100): Flow<List<BatteryReadingEntity>>

    @Query("SELECT * FROM battery_readings WHERE deviceId = :deviceId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentReadings(deviceId: String, limit: Int = 100): List<BatteryReadingEntity>

    @Query("SELECT * FROM battery_readings WHERE deviceId = :deviceId AND timestamp >= :sinceTimestamp ORDER BY timestamp ASC")
    fun getReadingsSinceFlow(deviceId: String, sinceTimestamp: Long): Flow<List<BatteryReadingEntity>>

    @Query("SELECT * FROM battery_readings WHERE deviceId = :deviceId AND timestamp >= :sinceTimestamp ORDER BY timestamp ASC")
    suspend fun getReadingsSince(deviceId: String, sinceTimestamp: Long): List<BatteryReadingEntity>

    @Query("DELETE FROM battery_readings WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteReadingsOlderThan(cutoffTimestamp: Long): Int

    @Query("DELETE FROM battery_readings WHERE deviceId = :deviceId")
    suspend fun deleteReadingsForDevice(deviceId: String)

    @Query("SELECT COUNT(*) FROM battery_readings WHERE deviceId = :deviceId")
    suspend fun getReadingCountForDevice(deviceId: String): Int
}

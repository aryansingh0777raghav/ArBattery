package com.arverse.arbattery.core.database.repository

import com.arverse.arbattery.core.common.dispatcher.DefaultDispatcherProvider
import com.arverse.arbattery.core.common.dispatcher.DispatcherProvider
import com.arverse.arbattery.core.common.model.ChargingState
import com.arverse.arbattery.core.common.model.ReadingQuality
import com.arverse.arbattery.core.common.result.AppResult
import com.arverse.arbattery.core.database.dao.BatteryReadingDao
import com.arverse.arbattery.core.database.entity.BatteryReadingEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class BatteryHistoryItem(
    val id: Long,
    val deviceId: String,
    val percentage: Int,
    val chargingState: ChargingState,
    val timestamp: Long,
    val readingSource: String,
    val qualityStatus: ReadingQuality
)

interface BatteryHistoryRepository {
    fun getRecentReadingsFlow(deviceId: String, limit: Int = 100): Flow<List<BatteryHistoryItem>>
    suspend fun getRecentReadings(deviceId: String, limit: Int = 100): List<BatteryHistoryItem>
    fun getReadingsSinceFlow(deviceId: String, sinceTimestamp: Long): Flow<List<BatteryHistoryItem>>
    suspend fun recordReading(
        deviceId: String,
        percentage: Int,
        chargingState: ChargingState = ChargingState.UNKNOWN,
        source: String = "BLE",
        quality: ReadingQuality = ReadingQuality.LIVE,
        timestamp: Long = System.currentTimeMillis()
    ): AppResult<Long>
    suspend fun pruneOldReadings(daysToKeep: Int): Int
}

class DefaultBatteryHistoryRepository(
    private val batteryReadingDao: BatteryReadingDao,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) : BatteryHistoryRepository {

    override fun getRecentReadingsFlow(
        deviceId: String,
        limit: Int
    ): Flow<List<BatteryHistoryItem>> =
        batteryReadingDao.getRecentReadingsFlow(deviceId, limit)
            .map { list -> list.map { it.toModel() } }
            .flowOn(dispatchers.io)

    override suspend fun getRecentReadings(
        deviceId: String,
        limit: Int
    ): List<BatteryHistoryItem> = withContext(dispatchers.io) {
        batteryReadingDao.getRecentReadings(deviceId, limit).map { it.toModel() }
    }

    override fun getReadingsSinceFlow(
        deviceId: String,
        sinceTimestamp: Long
    ): Flow<List<BatteryHistoryItem>> =
        batteryReadingDao.getReadingsSinceFlow(deviceId, sinceTimestamp)
            .map { list -> list.map { it.toModel() } }
            .flowOn(dispatchers.io)

    override suspend fun recordReading(
        deviceId: String,
        percentage: Int,
        chargingState: ChargingState,
        source: String,
        quality: ReadingQuality,
        timestamp: Long
    ): AppResult<Long> = withContext(dispatchers.io) {
        try {
            val entity = BatteryReadingEntity(
                deviceId = deviceId,
                percentage = percentage,
                chargingState = chargingState,
                timestamp = timestamp,
                readingSource = source,
                qualityStatus = quality
            )
            val id = batteryReadingDao.insertReading(entity)
            AppResult.Success(id)
        } catch (e: Exception) {
            AppResult.Error(e, "Failed to record reading: ${e.message}")
        }
    }

    override suspend fun pruneOldReadings(daysToKeep: Int): Int = withContext(dispatchers.io) {
        val cutoffMs = System.currentTimeMillis() - (daysToKeep * 24L * 3600L * 1000L)
        batteryReadingDao.deleteReadingsOlderThan(cutoffMs)
    }

    private fun BatteryReadingEntity.toModel(): BatteryHistoryItem = BatteryHistoryItem(
        id = id,
        deviceId = deviceId,
        percentage = percentage,
        chargingState = chargingState,
        timestamp = timestamp,
        readingSource = readingSource,
        qualityStatus = qualityStatus
    )
}

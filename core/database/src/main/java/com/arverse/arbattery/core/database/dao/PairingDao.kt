package com.arverse.arbattery.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.arverse.arbattery.core.database.entity.PairingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PairingDao {

    @Query("SELECT * FROM pairings WHERE deviceId = :deviceId LIMIT 1")
    suspend fun getPairing(deviceId: String): PairingEntity?

    @Query("SELECT * FROM pairings WHERE deviceId = :deviceId LIMIT 1")
    fun getPairingFlow(deviceId: String): Flow<PairingEntity?>

    @Query("SELECT * FROM pairings WHERE status = 'ACTIVE'")
    suspend fun getActivePairings(): List<PairingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPairing(pairing: PairingEntity)

    @Update
    suspend fun updatePairing(pairing: PairingEntity)

    @Query("UPDATE pairings SET lastSync = :timestamp WHERE deviceId = :deviceId")
    suspend fun updateLastSync(deviceId: String, timestamp: Long)

    @Query("DELETE FROM pairings WHERE deviceId = :deviceId")
    suspend fun deletePairing(deviceId: String)

    @Query("SELECT COUNT(*) FROM pairings WHERE status = 'ACTIVE'")
    suspend fun getActivePairingCount(): Int
}

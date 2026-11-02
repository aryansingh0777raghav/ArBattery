package com.arverse.arbattery.core.database.repository

import com.arverse.arbattery.core.common.dispatcher.DefaultDispatcherProvider
import com.arverse.arbattery.core.common.dispatcher.DispatcherProvider
import com.arverse.arbattery.core.common.result.AppResult
import com.arverse.arbattery.core.database.dao.PairingDao
import com.arverse.arbattery.core.database.entity.PairingEntity
import kotlinx.coroutines.withContext

interface PairingRepository {
    suspend fun getPairing(deviceId: String): PairingEntity?
    suspend fun savePairing(deviceId: String, tokenHash: String): AppResult<Unit>
    suspend fun updateLastSync(deviceId: String, timestamp: Long = System.currentTimeMillis()): AppResult<Unit>
    suspend fun validateToken(deviceId: String, tokenHash: String): Boolean
    suspend fun deletePairing(deviceId: String): AppResult<Unit>
    suspend fun getActivePairings(): List<PairingEntity>
}

class DefaultPairingRepository(
    private val pairingDao: PairingDao,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) : PairingRepository {

    override suspend fun getPairing(deviceId: String): PairingEntity? = withContext(dispatchers.io) {
        pairingDao.getPairing(deviceId)
    }

    override suspend fun savePairing(deviceId: String, tokenHash: String): AppResult<Unit> = withContext(dispatchers.io) {
        try {
            val entity = PairingEntity(
                deviceId = deviceId,
                tokenHash = tokenHash,
                pairedAt = System.currentTimeMillis(),
                lastSync = System.currentTimeMillis(),
                status = "ACTIVE"
            )
            pairingDao.insertPairing(entity)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(e, "Failed to save pairing: ${e.message}")
        }
    }

    override suspend fun updateLastSync(deviceId: String, timestamp: Long): AppResult<Unit> = withContext(dispatchers.io) {
        try {
            pairingDao.updateLastSync(deviceId, timestamp)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(e, "Failed to update last sync: ${e.message}")
        }
    }

    override suspend fun validateToken(deviceId: String, tokenHash: String): Boolean = withContext(dispatchers.io) {
        val pairing = pairingDao.getPairing(deviceId) ?: return@withContext false
        pairing.status == "ACTIVE" && pairing.tokenHash == tokenHash
    }

    override suspend fun deletePairing(deviceId: String): AppResult<Unit> = withContext(dispatchers.io) {
        try {
            pairingDao.deletePairing(deviceId)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(e, "Failed to delete pairing: ${e.message}")
        }
    }

    override suspend fun getActivePairings(): List<PairingEntity> = withContext(dispatchers.io) {
        pairingDao.getActivePairings()
    }
}

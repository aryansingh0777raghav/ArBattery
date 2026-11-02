package com.arverse.arbattery.core.database.repository

import com.arverse.arbattery.core.common.dispatcher.DefaultDispatcherProvider
import com.arverse.arbattery.core.common.dispatcher.DispatcherProvider
import com.arverse.arbattery.core.common.model.ConnectionState
import com.arverse.arbattery.core.common.model.DeviceBattery
import com.arverse.arbattery.core.common.result.AppResult
import com.arverse.arbattery.core.database.dao.DeviceDao
import com.arverse.arbattery.core.database.entity.DeviceEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface DeviceRepository {
    fun getAllDevicesFlow(): Flow<List<DeviceBattery>>
    suspend fun getAllDevices(): List<DeviceBattery>
    fun getDeviceByIdFlow(deviceId: String): Flow<DeviceBattery?>
    suspend fun getDeviceById(deviceId: String): DeviceBattery?
    suspend fun saveDevice(device: DeviceBattery): AppResult<Unit>
    suspend fun updateBatteryReading(device: DeviceBattery): AppResult<Unit>
    suspend fun updateConnectionState(deviceId: String, state: ConnectionState): AppResult<Unit>
    suspend fun deleteDevice(deviceId: String): AppResult<Unit>
    suspend fun getConnectedCount(): Int
    suspend fun getTotalCount(): Int
}

class DefaultDeviceRepository(
    private val deviceDao: DeviceDao,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) : DeviceRepository {

    override fun getAllDevicesFlow(): Flow<List<DeviceBattery>> =
        deviceDao.getAllDevicesFlow()
            .map { list -> list.map { it.toDomainModel() } }
            .flowOn(dispatchers.io)

    override suspend fun getAllDevices(): List<DeviceBattery> = withContext(dispatchers.io) {
        deviceDao.getAllDevices().map { it.toDomainModel() }
    }

    override fun getDeviceByIdFlow(deviceId: String): Flow<DeviceBattery?> =
        deviceDao.getDeviceByIdFlow(deviceId)
            .map { it?.toDomainModel() }
            .flowOn(dispatchers.io)

    override suspend fun getDeviceById(deviceId: String): DeviceBattery? = withContext(dispatchers.io) {
        deviceDao.getDeviceById(deviceId)?.toDomainModel()
    }

    override suspend fun saveDevice(device: DeviceBattery): AppResult<Unit> = withContext(dispatchers.io) {
        try {
            deviceDao.insertOrUpdateDevice(DeviceEntity.fromDomainModel(device))
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(e, "Failed to save device: ${e.message}")
        }
    }

    override suspend fun updateBatteryReading(device: DeviceBattery): AppResult<Unit> = withContext(dispatchers.io) {
        try {
            val existing = deviceDao.getDeviceById(device.deviceId)
            if (existing == null) {
                deviceDao.insertOrUpdateDevice(DeviceEntity.fromDomainModel(device))
            } else {
                deviceDao.updateBatteryReading(
                    deviceId = device.deviceId,
                    percentage = device.batteryPercentage,
                    chargingState = device.chargingState,
                    lastUpdated = device.lastUpdated,
                    quality = device.capabilityStatus,
                    errorState = device.errorState,
                    connectionState = device.connectionState
                )
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(e, "Failed to update battery reading: ${e.message}")
        }
    }

    override suspend fun updateConnectionState(
        deviceId: String,
        state: ConnectionState
    ): AppResult<Unit> = withContext(dispatchers.io) {
        try {
            deviceDao.updateConnectionState(deviceId, state)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(e, "Failed to update connection state: ${e.message}")
        }
    }

    override suspend fun deleteDevice(deviceId: String): AppResult<Unit> = withContext(dispatchers.io) {
        try {
            deviceDao.deleteDeviceById(deviceId)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(e, "Failed to delete device: ${e.message}")
        }
    }

    override suspend fun getConnectedCount(): Int = withContext(dispatchers.io) {
        deviceDao.getConnectedDeviceCount()
    }

    override suspend fun getTotalCount(): Int = withContext(dispatchers.io) {
        deviceDao.getDeviceCount()
    }
}

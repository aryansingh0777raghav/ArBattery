package com.arverse.arbattery.core.device.manager

import com.arverse.arbattery.core.common.dispatcher.DefaultDispatcherProvider
import com.arverse.arbattery.core.common.dispatcher.DispatcherProvider
import com.arverse.arbattery.core.common.model.ConnectionMethod
import com.arverse.arbattery.core.common.model.ConnectionState
import com.arverse.arbattery.core.common.model.DeviceBattery
import com.arverse.arbattery.core.common.result.AppResult
import com.arverse.arbattery.core.database.repository.BatteryHistoryRepository
import com.arverse.arbattery.core.database.repository.DeviceRepository
import com.arverse.arbattery.core.device.evaluator.BatteryFreshnessEvaluator
import com.arverse.arbattery.core.device.integration.DeviceIntegration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface BatteryAlertListener {
    fun onLowBattery(device: DeviceBattery, percentage: Int)
    fun onCriticalBattery(device: DeviceBattery, percentage: Int)
}

class UniversalBatteryManager(
    private val deviceRepository: DeviceRepository,
    private val historyRepository: BatteryHistoryRepository,
    private val integrations: Map<ConnectionMethod, DeviceIntegration>,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) {
    private var alertListener: BatteryAlertListener? = null
    private val previouslyAlertedDevices = mutableMapOf<String, Int>()

    fun setAlertListener(listener: BatteryAlertListener?) {
        this.alertListener = listener
    }

    fun getMonitoredDevicesFlow(): Flow<List<DeviceBattery>> =
        deviceRepository.getAllDevicesFlow()
            .map { list ->
                list.map { battery ->
                    BatteryFreshnessEvaluator.evaluateFreshness(battery)
                }
            }
            .flowOn(dispatchers.io)

    suspend fun refreshDevice(deviceId: String): AppResult<DeviceBattery> = withContext(dispatchers.io) {
        val device = deviceRepository.getDeviceById(deviceId)
            ?: return@withContext AppResult.Error(NoSuchElementException("Device $deviceId not found."))

        val integration = integrations[device.connectionMethod]
            ?: return@withContext AppResult.Error(
                UnsupportedOperationException("No integration for connection method ${device.connectionMethod}")
            )

        if (!integration.supportsForceRefresh) {
            return@withContext AppResult.Success(device)
        }

        when (val result = integration.readBattery(deviceId, device.deviceName)) {
            is AppResult.Success -> {
                val updatedDevice = result.data
                deviceRepository.updateBatteryReading(updatedDevice)

                updatedDevice.batteryPercentage?.let { pct ->
                    historyRepository.recordReading(
                        deviceId = deviceId,
                        percentage = pct,
                        chargingState = updatedDevice.chargingState,
                        source = updatedDevice.connectionMethod.name
                    )
                    checkThresholds(updatedDevice, pct)
                }

                AppResult.Success(updatedDevice)
            }
            is AppResult.Error -> {
                // Preserve last known battery, but mark connection as disconnected/error
                deviceRepository.updateConnectionState(deviceId, ConnectionState.DISCONNECTED)
                AppResult.Error(result.exception, result.message)
            }
            is AppResult.Loading -> AppResult.Loading
        }
    }

    suspend fun refreshAllConnectedDevices(): Map<String, AppResult<DeviceBattery>> = withContext(dispatchers.io) {
        val devices = deviceRepository.getAllDevices()
        val results = mutableMapOf<String, AppResult<DeviceBattery>>()

        for (device in devices) {
            if (device.connectionMethod == ConnectionMethod.BLE) {
                results[device.deviceId] = refreshDevice(device.deviceId)
            }
        }
        results
    }

    private fun checkThresholds(device: DeviceBattery, percentage: Int) {
        val lastAlerted = previouslyAlertedDevices[device.deviceId]
        val isCrossingCritical = percentage <= 10 && (lastAlerted == null || lastAlerted > 10)
        val isCrossingLow = percentage <= 20 && percentage > 10 && (lastAlerted == null || lastAlerted > 20)

        if (isCrossingCritical) {
            previouslyAlertedDevices[device.deviceId] = percentage
            alertListener?.onCriticalBattery(device, percentage)
        } else if (isCrossingLow) {
            previouslyAlertedDevices[device.deviceId] = percentage
            alertListener?.onLowBattery(device, percentage)
        } else if (percentage > 20) {
            // Reset alert state when charged back above 20%
            previouslyAlertedDevices.remove(device.deviceId)
        }
    }
}

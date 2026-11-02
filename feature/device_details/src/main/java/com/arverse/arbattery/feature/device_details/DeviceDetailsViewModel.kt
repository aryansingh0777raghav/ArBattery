package com.arverse.arbattery.feature.device_details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arverse.arbattery.core.bluetooth.gatt.BluetoothConnectionManager
import com.arverse.arbattery.core.common.model.ConnectionMethod
import com.arverse.arbattery.core.common.model.ConnectionState
import com.arverse.arbattery.core.common.model.DeviceBattery
import com.arverse.arbattery.core.common.result.AppResult
import com.arverse.arbattery.core.database.repository.BatteryHistoryItem
import com.arverse.arbattery.core.database.repository.BatteryHistoryRepository
import com.arverse.arbattery.core.database.repository.DeviceRepository
import com.arverse.arbattery.core.device.manager.UniversalBatteryManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DeviceDetailsUiState(
    val device: DeviceBattery? = null,
    val history: List<BatteryHistoryItem> = emptyList(),
    val isRefreshing: Boolean = false,
    val isDeleting: Boolean = false,
    val infoMessage: String? = null,
    val errorMessage: String? = null
)

class DeviceDetailsViewModel(
    private val deviceId: String,
    private val deviceRepository: DeviceRepository,
    private val historyRepository: BatteryHistoryRepository,
    private val batteryManager: UniversalBatteryManager,
    private val connectionManager: BluetoothConnectionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceDetailsUiState())
    val uiState: StateFlow<DeviceDetailsUiState> = _uiState.asStateFlow()

    init {
        observeDevice()
        observeHistory()
    }

    private fun observeDevice() {
        viewModelScope.launch {
            deviceRepository.getDeviceByIdFlow(deviceId).collect { dev ->
                _uiState.update { it.copy(device = dev) }
            }
        }
    }

    private fun observeHistory() {
        viewModelScope.launch {
            val readings = historyRepository.getRecentReadings(deviceId, 100)
            _uiState.update { it.copy(history = readings) }
        }
    }

    fun refreshBattery() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            val result = batteryManager.refreshDevice(deviceId)
            when (result) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            device = result.data,
                            isRefreshing = false,
                            infoMessage = "Battery status updated."
                        )
                    }
                    observeHistory()
                }
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            errorMessage = result.message ?: "Failed to refresh battery"
                        )
                    }
                }
                is AppResult.Loading -> {}
            }
        }
    }

    fun toggleConnection() {
        val dev = _uiState.value.device ?: return
        viewModelScope.launch {
            if (dev.connectionState == ConnectionState.CONNECTED) {
                if (dev.connectionMethod == ConnectionMethod.BLE) {
                    connectionManager.disconnect()
                }
                deviceRepository.updateConnectionState(deviceId, ConnectionState.DISCONNECTED)
                _uiState.update { it.copy(infoMessage = "Device disconnected.") }
            } else {
                deviceRepository.updateConnectionState(deviceId, ConnectionState.CONNECTING)
                refreshBattery()
            }
        }
    }

    fun deleteDevice(onComplete: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }
            if (_uiState.value.device?.connectionMethod == ConnectionMethod.BLE) {
                connectionManager.disconnect()
            }
            deviceRepository.deleteDevice(deviceId)
            onComplete()
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(infoMessage = null, errorMessage = null) }
    }
}

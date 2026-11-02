package com.arverse.arbattery.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arverse.arbattery.core.common.model.DeviceBattery
import com.arverse.arbattery.core.database.repository.BatteryHistoryItem
import com.arverse.arbattery.core.database.repository.BatteryHistoryRepository
import com.arverse.arbattery.core.database.repository.DeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HistoryUiState(
    val devices: List<DeviceBattery> = emptyList(),
    val selectedDeviceId: String? = null,
    val readings: List<BatteryHistoryItem> = emptyList(),
    val totalCount: Int = 0,
    val lowBatteryCount: Int = 0,
    val averagePercentage: Int = 0,
    val isLoading: Boolean = false,
    val message: String? = null
)

class HistoryViewModel(
    private val deviceRepository: DeviceRepository,
    private val historyRepository: BatteryHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadDevices()
    }

    private fun loadDevices() {
        viewModelScope.launch {
            deviceRepository.getAllDevicesFlow().collect { devs ->
                _uiState.update { it.copy(devices = devs) }
                loadReadings()
            }
        }
    }

    fun selectDevice(deviceId: String?) {
        _uiState.update { it.copy(selectedDeviceId = deviceId) }
        loadReadings()
    }

    fun loadReadings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val selId = _uiState.value.selectedDeviceId
            val readings = if (selId != null) {
                historyRepository.getRecentReadings(selId, 50)
            } else {
                val allList = mutableListOf<BatteryHistoryItem>()
                _uiState.value.devices.forEach { dev ->
                    allList.addAll(historyRepository.getRecentReadings(dev.deviceId, 20))
                }
                allList.sortedByDescending { it.timestamp }
            }

            val total = readings.size
            val lowCount = readings.count { it.percentage <= 20 }
            val avg = if (readings.isNotEmpty()) {
                readings.map { it.percentage }.average().toInt()
            } else 0

            _uiState.update {
                it.copy(
                    readings = readings,
                    totalCount = total,
                    lowBatteryCount = lowCount,
                    averagePercentage = avg,
                    isLoading = false
                )
            }
        }
    }

    fun exportAsCsv(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        return buildString {
            appendLine("Timestamp,DeviceId,BatteryPercentage,ChargingState,ReadingSource")
            _uiState.value.readings.forEach { r ->
                appendLine("${dateFormat.format(Date(r.timestamp))},${r.deviceId},${r.percentage},${r.chargingState.name},${r.readingSource}")
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            // Prune all readings older than 0 days (i.e. all readings)
            historyRepository.pruneOldReadings(0)
            loadReadings()
            _uiState.update { it.copy(message = "History cleared.") }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}

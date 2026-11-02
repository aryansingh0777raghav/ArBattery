package com.arverse.arbattery.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arverse.arbattery.core.common.dispatcher.DefaultDispatcherProvider
import com.arverse.arbattery.core.common.dispatcher.DispatcherProvider
import com.arverse.arbattery.core.common.model.ConnectionState
import com.arverse.arbattery.core.common.model.DeviceBattery
import com.arverse.arbattery.core.device.manager.UniversalBatteryManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DashboardFilter {
    ALL,
    CONNECTED,
    LOW_BATTERY,
    DISCONNECTED
}

data class DashboardUiState(
    val devices: List<DeviceBattery> = emptyList(),
    val totalCount: Int = 0,
    val connectedCount: Int = 0,
    val lowBatteryCount: Int = 0,
    val isRefreshing: Boolean = false,
    val selectedFilter: DashboardFilter = DashboardFilter.ALL,
    val error: String? = null
)

class DashboardViewModel(
    private val batteryManager: UniversalBatteryManager,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    private val _selectedFilter = MutableStateFlow(DashboardFilter.ALL)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<DashboardUiState> = combine(
        batteryManager.getMonitoredDevicesFlow(),
        _selectedFilter,
        _isRefreshing,
        _errorMessage
    ) { allDevices, filter, refreshing, err ->
        val total = allDevices.size
        val connected = allDevices.count { it.connectionState == ConnectionState.CONNECTED }
        val low = allDevices.count { it.isLow() || it.isCritical() }

        val filtered = when (filter) {
            DashboardFilter.ALL -> allDevices
            DashboardFilter.CONNECTED -> allDevices.filter { it.connectionState == ConnectionState.CONNECTED }
            DashboardFilter.LOW_BATTERY -> allDevices.filter { it.isLow() || it.isCritical() }
            DashboardFilter.DISCONNECTED -> allDevices.filter { it.connectionState != ConnectionState.CONNECTED }
        }

        DashboardUiState(
            devices = filtered,
            totalCount = total,
            connectedCount = connected,
            lowBatteryCount = low,
            isRefreshing = refreshing,
            selectedFilter = filter,
            error = err
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun setFilter(filter: DashboardFilter) {
        _selectedFilter.value = filter
    }

    fun refreshAll() {
        viewModelScope.launch(dispatchers.io) {
            _isRefreshing.value = true
            try {
                batteryManager.refreshAllConnectedDevices()
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

package com.arverse.arbattery.feature.discovery

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arverse.arbattery.core.bluetooth.BluetoothPermissionHelper
import com.arverse.arbattery.core.bluetooth.diagnostic.DiagnosticProgress
import com.arverse.arbattery.core.bluetooth.diagnostic.HammerAceDiagnosticScanner
import com.arverse.arbattery.core.bluetooth.gatt.BluetoothConnectionManager
import com.arverse.arbattery.core.bluetooth.model.BleScanDevice
import com.arverse.arbattery.core.bluetooth.model.DiagnosticReport
import com.arverse.arbattery.core.bluetooth.scanner.BleScanState
import com.arverse.arbattery.core.bluetooth.scanner.BluetoothScanner
import com.arverse.arbattery.core.common.model.ConnectionMethod
import com.arverse.arbattery.core.common.model.ConnectionState
import com.arverse.arbattery.core.common.model.DeviceBattery
import com.arverse.arbattery.core.common.model.DeviceType
import com.arverse.arbattery.core.database.repository.DeviceRepository
import com.arverse.arbattery.core.device.manager.UniversalBatteryManager
import com.arverse.arbattery.core.network.security.CryptoTokenManager
import com.arverse.arbattery.core.network.util.NetworkUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiscoveryUiState(
    val selectedTab: DiscoveryTab = DiscoveryTab.BLUETOOTH,
    val isScanning: Boolean = false,
    val scanErrorMessage: String? = null,
    val searchQuery: String = "",
    val hideUnnamedDevices: Boolean = true,
    val isBluetoothEnabled: Boolean = true,
    val hasBluetoothPermissions: Boolean = true,
    val hubLocalIp: String = "127.0.0.1",
    val hubPort: Int = 8443,
    val hubPairingToken: String = "",
    val hubPairingQrPayload: String = "",
    val pairingSuccessMessage: String? = null,
    val diagnosticProgress: DiagnosticProgress = DiagnosticProgress.Idle,
    val lastDiagnosticReport: DiagnosticReport? = null,
    val diagnosticTargetAddress: String = "",
    val isAddingDevice: Boolean = false
)

enum class DiscoveryTab {
    BLUETOOTH,
    COMPANION_WIFI,
    HAMMER_ACE_DIAGNOSTIC
}

class DiscoveryViewModel(
    private val bluetoothScanner: BluetoothScanner,
    private val connectionManager: BluetoothConnectionManager,
    private val hammerAceDiagnosticScanner: HammerAceDiagnosticScanner,
    private val deviceRepository: DeviceRepository,
    private val batteryManager: UniversalBatteryManager,
    private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoveryUiState())
    val uiState: StateFlow<DiscoveryUiState> = _uiState.asStateFlow()

    val discoveredDevices: StateFlow<List<BleScanDevice>> = combine(
        bluetoothScanner.discoveredDevices,
        _uiState
    ) { devices, state ->
        devices.filter { device ->
            val matchesFilter = if (state.hideUnnamedDevices) {
                !device.name.isNullOrBlank() && device.name != "Unknown Device"
            } else true
            val matchesQuery = if (state.searchQuery.isBlank()) true else {
                (device.name?.contains(state.searchQuery, ignoreCase = true) == true) ||
                        device.address.contains(state.searchQuery, ignoreCase = true)
            }
            matchesFilter && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        checkPermissionsAndBluetooth()
        initializeHubCompanionInfo()
        observeScanState()
        observeDiagnosticProgress()
    }

    fun selectTab(tab: DiscoveryTab) {
        _uiState.update { it.copy(selectedTab = tab) }
        if (tab == DiscoveryTab.COMPANION_WIFI) {
            refreshHubCompanionInfo()
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleHideUnnamedDevices(hide: Boolean) {
        _uiState.update { it.copy(hideUnnamedDevices = hide) }
    }

    fun checkPermissionsAndBluetooth() {
        val hasPerms = BluetoothPermissionHelper.hasBluetoothPermissions(appContext)
        val isBtOn = BluetoothPermissionHelper.isBluetoothEnabled(appContext)
        _uiState.update {
            it.copy(
                hasBluetoothPermissions = hasPerms,
                isBluetoothEnabled = isBtOn
            )
        }
    }

    fun startBleScan() {
        checkPermissionsAndBluetooth()
        if (!_uiState.value.hasBluetoothPermissions) {
            _uiState.update { it.copy(scanErrorMessage = "Bluetooth permissions required to scan.") }
            return
        }
        if (!_uiState.value.isBluetoothEnabled) {
            _uiState.update { it.copy(scanErrorMessage = "Bluetooth is currently turned off.") }
            return
        }
        _uiState.update { it.copy(scanErrorMessage = null) }
        bluetoothScanner.startScan()
    }

    fun stopBleScan() {
        bluetoothScanner.stopScan()
    }

    fun addDiscoveredDevice(device: BleScanDevice, deviceType: DeviceType) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAddingDevice = true) }
            val displayName = device.name?.takeIf { it.isNotBlank() } ?: "BLE Device (${device.address.takeLast(5)})"
            val initialBattery = DeviceBattery(
                deviceId = device.address,
                deviceName = displayName,
                deviceType = deviceType,
                connectionMethod = ConnectionMethod.BLE,
                connectionState = ConnectionState.CONNECTED,
                batteryPercentage = null,
                lastUpdated = System.currentTimeMillis()
            )
            deviceRepository.saveDevice(initialBattery)
            batteryManager.refreshDevice(device.address)
            _uiState.update {
                it.copy(
                    isAddingDevice = false,
                    pairingSuccessMessage = "Device \"$displayName\" added successfully!"
                )
            }
        }
    }

    fun clearPairingSuccessMessage() {
        _uiState.update { it.copy(pairingSuccessMessage = null) }
    }

    private fun initializeHubCompanionInfo() {
        refreshHubCompanionInfo()
    }

    fun refreshHubCompanionInfo() {
        val lanList = NetworkUtils.getAllLanInterfaces()
        val availableIps = lanList.map { it.ipAddress }.distinct()
        val ip = NetworkUtils.getLocalIpAddress(appContext)
            ?: availableIps.firstOrNull()
            ?: "127.0.0.1"

        val port = 8443
        val token = CryptoTokenManager.generateSecureToken()
        val qrPayload = "arbattery://pair?ip=$ip&port=$port&token=$token"
        _uiState.update {
            it.copy(
                hubLocalIp = ip,
                hubPort = port,
                hubPairingToken = token,
                hubPairingQrPayload = qrPayload
            )
        }
    }

    fun setDiagnosticTargetAddress(address: String) {
        _uiState.update { it.copy(diagnosticTargetAddress = address) }
    }

    fun runHammerAceDiagnostic(address: String? = null, deviceName: String = "Hammer Ace") {
        val targetAddr = address ?: _uiState.value.diagnosticTargetAddress
        if (targetAddr.isBlank()) {
            _uiState.update { it.copy(scanErrorMessage = "Please select or enter a device MAC address.") }
            return
        }
        viewModelScope.launch {
            bluetoothScanner.stopScan()
            val report = hammerAceDiagnosticScanner.runDiagnostic(targetAddr, deviceName)
            _uiState.update {
                it.copy(
                    lastDiagnosticReport = report,
                    diagnosticTargetAddress = targetAddr
                )
            }
        }
    }

    private fun observeScanState() {
        viewModelScope.launch {
            bluetoothScanner.scanState.collect { state ->
                when (state) {
                    is BleScanState.Scanning -> _uiState.update { it.copy(isScanning = true, scanErrorMessage = null) }
                    is BleScanState.Idle -> _uiState.update { it.copy(isScanning = false) }
                    is BleScanState.Error -> _uiState.update { it.copy(isScanning = false, scanErrorMessage = state.message) }
                }
            }
        }
    }

    private fun observeDiagnosticProgress() {
        viewModelScope.launch {
            hammerAceDiagnosticScanner.progress.collect { progress ->
                _uiState.update { it.copy(diagnosticProgress = progress) }
                if (progress is DiagnosticProgress.Completed) {
                    _uiState.update { it.copy(lastDiagnosticReport = progress.report) }
                } else if (progress is DiagnosticProgress.Failed && progress.partialReport != null) {
                    _uiState.update { it.copy(lastDiagnosticReport = progress.partialReport) }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothScanner.stopScan()
    }
}

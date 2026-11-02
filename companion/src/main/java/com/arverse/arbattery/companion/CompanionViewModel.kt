package com.arverse.arbattery.companion

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arverse.arbattery.core.common.model.DeviceType
import com.arverse.arbattery.core.common.result.AppResult
import com.arverse.arbattery.core.network.client.CompanionApiClient
import com.arverse.arbattery.core.network.dto.BatteryReportDto
import com.arverse.arbattery.core.network.dto.PairingConfirmDto
import com.arverse.arbattery.core.network.dto.PairingRequestDto
import com.arverse.arbattery.core.network.util.NetworkUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class CompanionUiState(
    val batteryInfo: DetailedBatteryInfo? = null,
    val deviceId: String = "",
    val hubIp: String = "192.168.1.100",
    val hubPort: Int = 8443,
    val authToken: String = "",
    val isPaired: Boolean = false,
    val isSyncing: Boolean = false,
    val isPairing: Boolean = false,
    val lastSyncTime: Long? = null,
    val infoMessage: String? = null,
    val errorMessage: String? = null
)

class CompanionViewModel(
    private val appContext: Context,
    private val batteryCollector: BatteryCollector = BatteryCollector(appContext),
    private val preferences: CompanionPreferences = CompanionPreferences(appContext),
    private val apiClient: CompanionApiClient = CompanionApiClient()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompanionUiState())
    val uiState: StateFlow<CompanionUiState> = _uiState.asStateFlow()

    init {
        observePreferences()
        startLiveBatteryMonitoring()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferences.configFlow.collect { config ->
                _uiState.update {
                    it.copy(
                        deviceId = config.deviceId,
                        hubIp = config.hubIp,
                        hubPort = config.hubPort,
                        authToken = config.authToken ?: "",
                        isPaired = config.isPaired,
                        lastSyncTime = config.lastSyncTimestamp
                    )
                }
            }
        }
    }

    private fun startLiveBatteryMonitoring() {
        viewModelScope.launch {
            while (isActive) {
                val info = batteryCollector.collectBatteryInfo()
                _uiState.update { it.copy(batteryInfo = info) }
                delay(3000)
            }
        }
    }

    fun updateHubIp(ip: String) {
        _uiState.update { it.copy(hubIp = ip) }
    }

    fun updateHubPort(portStr: String) {
        val port = portStr.toIntOrNull() ?: 8443
        _uiState.update { it.copy(hubPort = port) }
    }

    fun updateAuthToken(token: String) {
        _uiState.update { it.copy(authToken = token) }
    }

    fun parseQrPayload(payload: String) {
        try {
            if (payload.startsWith("arbattery://pair")) {
                val uri = android.net.Uri.parse(payload)
                val ip = uri.getQueryParameter("ip")
                val port = uri.getQueryParameter("port")?.toIntOrNull()
                val token = uri.getQueryParameter("token")

                if (!ip.isNullOrBlank() && port != null && !token.isNullOrBlank()) {
                    _uiState.update {
                        it.copy(
                            hubIp = ip,
                            hubPort = port,
                            authToken = token,
                            infoMessage = "QR payload detected. Tap 'Connect to Hub'."
                        )
                    }
                }
            }
        } catch (_: Exception) {
            _uiState.update { it.copy(errorMessage = "Invalid QR code payload.") }
        }
    }

    fun autoDetectHub() {
        viewModelScope.launch {
            _uiState.update { it.copy(isPairing = true, infoMessage = "Scanning local Wi-Fi for ArBattery Hub...", errorMessage = null) }
            val myWifiIp = NetworkUtils.getLocalIpAddress(appContext)
            if (myWifiIp == null || !NetworkUtils.isPrivateLanIp(myWifiIp)) {
                _uiState.update {
                    it.copy(
                        isPairing = false,
                        errorMessage = "Tablet is not connected to a local Wi-Fi network. Please connect to Wi-Fi."
                    )
                }
                return@launch
            }

            val foundIp = apiClient.discoverHubOnSubnet(myWifiIp, _uiState.value.hubPort)
            if (foundIp != null) {
                _uiState.update {
                    it.copy(
                        hubIp = foundIp,
                        isPairing = false,
                        infoMessage = "Found ArBattery Hub at $foundIp! Ready to pair."
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isPairing = false,
                        errorMessage = "Could not detect Hub on subnet $myWifiIp/24. Ensure phone & tablet share the same Wi-Fi network and ArBattery is open on your phone."
                    )
                }
            }
        }
    }

    fun pairWithHub() {
        val state = _uiState.value
        if (state.hubIp.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter Hub IP address.") }
            return
        }

        if (NetworkUtils.isCellularOrSpecialIp(state.hubIp)) {
            _uiState.update {
                it.copy(
                    errorMessage = "IP ${state.hubIp} is an internal mobile carrier IP (464XLAT), not a local Wi-Fi address. Please check your phone for its Wi-Fi IP (usually 192.168.x.x) or tap 'Auto-Detect Hub'."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isPairing = true, errorMessage = null) }
            val devId = preferences.getOrGenerateDeviceId()
            val battery = batteryCollector.collectBatteryInfo()

            if (state.authToken.isNotBlank()) {
                // Confirm existing pairing token (e.g. from QR code or manual entry)
                val confirmResult = apiClient.confirmPairing(state.hubIp, state.hubPort, devId, state.authToken)
                when (confirmResult) {
                    is AppResult.Success<*> -> {
                        preferences.savePairing(state.hubIp, state.hubPort, state.authToken)
                        CompanionSyncManager.schedulePeriodicSync(appContext)
                        _uiState.update {
                            it.copy(
                                isPairing = false,
                                isPaired = true,
                                infoMessage = "Successfully paired with Hub!"
                            )
                        }
                        syncNow()
                    }
                    is AppResult.Error -> {
                        val err = confirmResult.message.orEmpty()
                        val msg = if (err.contains("timeout", ignoreCase = true) || err.contains("Connect", ignoreCase = true)) {
                            "Cannot reach Hub at ${state.hubIp}:${state.hubPort}. Verify both devices are on the same Wi-Fi network, or tap 'Auto-Detect Hub'."
                        } else {
                            "Pairing confirmation failed: $err"
                        }
                        _uiState.update {
                            it.copy(
                                isPairing = false,
                                errorMessage = msg
                            )
                        }
                    }
                    is AppResult.Loading -> {}
                }
            } else {
                // Request new pairing token from Hub
                val pairResult = apiClient.requestPairing(
                    hubHost = state.hubIp,
                    hubPort = state.hubPort,
                    deviceId = devId,
                    deviceName = battery.deviceDisplayName,
                    deviceType = DeviceType.TABLET.name
                )
                when (pairResult) {
                    is AppResult.Success<*> -> {
                        val token = (pairResult as AppResult.Success).data.authToken ?: ""
                        preferences.savePairing(state.hubIp, state.hubPort, token)
                        CompanionSyncManager.schedulePeriodicSync(appContext)
                        _uiState.update {
                            it.copy(
                                isPairing = false,
                                isPaired = true,
                                authToken = token,
                                infoMessage = "Successfully registered and paired with Hub!"
                            )
                        }
                        syncNow()
                    }
                    is AppResult.Error -> {
                        val err = pairResult.message.orEmpty()
                        val msg = if (err.contains("timeout", ignoreCase = true) || err.contains("Connect", ignoreCase = true)) {
                            "Cannot reach Hub at ${state.hubIp}:${state.hubPort}. Verify both devices are on the same Wi-Fi network, or tap 'Auto-Detect Hub'."
                        } else {
                            "Cannot reach Hub at ${state.hubIp}:${state.hubPort}: $err"
                        }
                        _uiState.update {
                            it.copy(
                                isPairing = false,
                                errorMessage = msg
                            )
                        }
                    }
                    is AppResult.Loading -> {}
                }
            }
        }
    }

    fun syncNow() {
        val state = _uiState.value
        if (!state.isPaired || state.authToken.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Companion is not paired with a Hub.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, errorMessage = null) }
            val battery = batteryCollector.collectBatteryInfo()
            val report = batteryCollector.toBatteryReportDto(state.deviceId, battery)

            val result = apiClient.reportBattery(
                host = state.hubIp,
                port = state.hubPort,
                token = state.authToken,
                report = report
            )

            when (result) {
                is AppResult.Success<*> -> {
                    val now = System.currentTimeMillis()
                    preferences.updateLastSync(now)
                    _uiState.update {
                        it.copy(
                            isSyncing = false,
                            lastSyncTime = now,
                            infoMessage = "Telemetry synced to Hub."
                        )
                    }
                }
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSyncing = false,
                            errorMessage = "Sync failed: ${result.message}"
                        )
                    }
                }
                is AppResult.Loading -> {}
            }
        }
    }

    fun unpair() {
        viewModelScope.launch {
            preferences.clearPairing()
            CompanionSyncManager.cancelPeriodicSync(appContext)
            _uiState.update {
                it.copy(
                    isPaired = false,
                    authToken = "",
                    infoMessage = "Companion uncoupled from Hub."
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(infoMessage = null, errorMessage = null) }
    }
}

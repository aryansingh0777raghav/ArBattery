package com.arverse.arbattery.feature.pairing

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arverse.arbattery.core.database.entity.PairingEntity
import com.arverse.arbattery.core.database.repository.PairingRepository
import com.arverse.arbattery.core.network.security.CryptoTokenManager
import com.arverse.arbattery.core.network.util.NetworkUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

data class PairingUiState(
    val hubIp: String = "127.0.0.1",
    val hubPort: Int = 8443,
    val availableIps: List<String> = emptyList(),
    val sessionToken: String = "",
    val numericPin: String = "",
    val qrPayload: String = "",
    val qrBitmap: ImageBitmap? = null,
    val activePairings: List<PairingEntity> = emptyList(),
    val isGenerating: Boolean = false,
    val infoMessage: String? = null,
    val errorMessage: String? = null
)

class PairingViewModel(
    private val pairingRepository: PairingRepository,
    private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(PairingUiState())
    val uiState: StateFlow<PairingUiState> = _uiState.asStateFlow()

    init {
        generateNewSession()
        loadActivePairings()
    }

    fun generateNewSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true) }
            val lanList = NetworkUtils.getAllLanInterfaces()
            val availableIps = lanList.map { it.ipAddress }.distinct()
            val ip = NetworkUtils.getLocalIpAddress(appContext)
                ?: availableIps.firstOrNull()
                ?: "127.0.0.1"

            val port = 8443
            val token = CryptoTokenManager.generateSecureToken()
            val pin = String.format("%06d", Random.nextInt(100000, 999999))
            val payload = "arbattery://pair?ip=$ip&port=$port&token=$token&pin=$pin"
            val qrBitmap = QrCodeGenerator.generateQrImageBitmap(payload, 512, 512)

            _uiState.update {
                it.copy(
                    hubIp = ip,
                    hubPort = port,
                    availableIps = availableIps,
                    sessionToken = token,
                    numericPin = pin,
                    qrPayload = payload,
                    qrBitmap = qrBitmap,
                    isGenerating = false
                )
            }
        }
    }

    fun selectHubIp(newIp: String) {
        viewModelScope.launch {
            val state = _uiState.value
            val payload = "arbattery://pair?ip=$newIp&port=${state.hubPort}&token=${state.sessionToken}&pin=${state.numericPin}"
            val qrBitmap = QrCodeGenerator.generateQrImageBitmap(payload, 512, 512)
            _uiState.update {
                it.copy(
                    hubIp = newIp,
                    qrPayload = payload,
                    qrBitmap = qrBitmap
                )
            }
        }
    }

    fun loadActivePairings() {
        viewModelScope.launch {
            val pairings = pairingRepository.getActivePairings()
            _uiState.update { it.copy(activePairings = pairings) }
        }
    }

    fun unpairDevice(deviceId: String) {
        viewModelScope.launch {
            pairingRepository.deletePairing(deviceId)
            loadActivePairings()
            _uiState.update { it.copy(infoMessage = "Device uncoupled successfully.") }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(infoMessage = null, errorMessage = null) }
    }
}

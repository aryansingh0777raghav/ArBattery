package com.arverse.arbattery.core.bluetooth.scanner

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import com.arverse.arbattery.core.bluetooth.BleConstants
import com.arverse.arbattery.core.bluetooth.BluetoothPermissionHelper
import com.arverse.arbattery.core.bluetooth.model.BleScanDevice
import com.arverse.arbattery.core.common.dispatcher.DefaultDispatcherProvider
import com.arverse.arbattery.core.common.dispatcher.DispatcherProvider
import com.arverse.arbattery.core.common.model.DeviceType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

sealed interface BleScanState {
    data object Idle : BleScanState
    data object Scanning : BleScanState
    data class Error(val message: String) : BleScanState
}

interface BluetoothScanner {
    val scanState: StateFlow<BleScanState>
    val discoveredDevices: StateFlow<List<BleScanDevice>>

    fun startScan(
        timeoutMs: Long = BleConstants.DEFAULT_SCAN_TIMEOUT_MS,
        serviceFilterUuids: List<UUID>? = null
    ): Boolean

    fun stopScan()
    fun clearDiscoveredDevices()
}

class AndroidBluetoothScanner(
    private val context: Context,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) : BluetoothScanner {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? get() = bluetoothManager?.adapter
    private val bleScanner: BluetoothLeScanner? get() = bluetoothAdapter?.bluetoothLeScanner

    private val _scanState = MutableStateFlow<BleScanState>(BleScanState.Idle)
    override val scanState: StateFlow<BleScanState> = _scanState.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BleScanDevice>>(emptyList())
    override val discoveredDevices: StateFlow<List<BleScanDevice>> = _discoveredDevices.asStateFlow()

    private var scanCallback: ScanCallback? = null
    private var scanTimeoutJob: Job? = null
    private val scope = CoroutineScope(dispatchers.default)
    private val scanMutex = Mutex()

    @SuppressLint("MissingPermission")
    override fun startScan(
        timeoutMs: Long,
        serviceFilterUuids: List<UUID>?
    ): Boolean {
        if (!BluetoothPermissionHelper.hasBluetoothPermissions(context)) {
            _scanState.value = BleScanState.Error("Bluetooth permissions not granted.")
            return false
        }

        if (!BluetoothPermissionHelper.isBluetoothEnabled(context)) {
            _scanState.value = BleScanState.Error("Bluetooth is turned off.")
            return false
        }

        val scanner = bleScanner
        if (scanner == null) {
            _scanState.value = BleScanState.Error("Bluetooth LE scanner is unavailable.")
            return false
        }

        stopScan()

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result?.let { handleScanResult(it) }
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                results?.forEach { handleScanResult(it) }
            }

            override fun onScanFailed(errorCode: Int) {
                val errorMsg = when (errorCode) {
                    SCAN_FAILED_ALREADY_STARTED -> "Scan already started"
                    SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "App registration failed"
                    SCAN_FAILED_INTERNAL_ERROR -> "Internal Bluetooth error"
                    SCAN_FAILED_FEATURE_UNSUPPORTED -> "BLE scan feature unsupported"
                    else -> "BLE scan failed with error code $errorCode"
                }
                _scanState.value = BleScanState.Error(errorMsg)
            }
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
            .build()

        val filters = serviceFilterUuids?.map { uuid ->
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(uuid))
                .build()
        } ?: emptyList()

        try {
            scanner.startScan(filters, settings, callback)
            scanCallback = callback
            _scanState.value = BleScanState.Scanning

            scanTimeoutJob?.cancel()
            scanTimeoutJob = scope.launch {
                delay(timeoutMs)
                stopScan()
            }
            return true
        } catch (e: Exception) {
            _scanState.value = BleScanState.Error("Failed to initiate BLE scan: ${e.message}")
            return false
        }
    }

    @SuppressLint("MissingPermission")
    override fun stopScan() {
        scanTimeoutJob?.cancel()
        scanTimeoutJob = null

        val callback = scanCallback
        if (callback != null && bleScanner != null && BluetoothPermissionHelper.hasBluetoothPermissions(context)) {
            try {
                bleScanner?.stopScan(callback)
            } catch (_: Exception) {
                // Ignore cleanup errors
            }
        }
        scanCallback = null
        if (_scanState.value is BleScanState.Scanning) {
            _scanState.value = BleScanState.Idle
        }
    }

    override fun clearDiscoveredDevices() {
        _discoveredDevices.value = emptyList()
    }

    @SuppressLint("MissingPermission")
    private fun handleScanResult(result: ScanResult) {
        val device = result.device ?: return
        val rawName = try {
            result.scanRecord?.deviceName ?: device.name ?: ""
        } catch (_: SecurityException) {
            ""
        }
        val address = device.address ?: return
        val rssi = result.rssi
        val serviceUuids = result.scanRecord?.serviceUuids?.map { it.uuid.toString() } ?: emptyList()

        val inferredType = inferDeviceType(rawName, serviceUuids)

        _discoveredDevices.update { currentList ->
            val existingIndex = currentList.indexOfFirst { it.address.equals(address, ignoreCase = true) }
            if (existingIndex >= 0) {
                val updatedList = currentList.toMutableList()
                val existing = updatedList[existingIndex]
                updatedList[existingIndex] = existing.copy(
                    rssi = rssi,
                    advertisedServiceUuids = if (serviceUuids.isNotEmpty()) serviceUuids else existing.advertisedServiceUuids,
                    timestamp = System.currentTimeMillis()
                )
                updatedList
            } else {
                currentList + BleScanDevice(
                    name = rawName,
                    address = address,
                    rssi = rssi,
                    advertisedServiceUuids = serviceUuids,
                    deviceType = inferredType
                )
            }
        }
    }

    private fun inferDeviceType(name: String, serviceUuids: List<String>): DeviceType {
        val lower = name.lowercase()
        return when {
            serviceUuids.any { it.contains("180D", ignoreCase = true) } -> DeviceType.SMARTWATCH
            lower.contains("watch") || lower.contains("ace") || lower.contains("band") || lower.contains("fit") -> DeviceType.SMARTWATCH
            lower.contains("bud") || lower.contains("airpod") || lower.contains("ear") || lower.contains("tws") -> DeviceType.EARBUDS
            lower.contains("headset") || lower.contains("headphone") || lower.contains("wh-") -> DeviceType.HEADPHONES
            lower.contains("tab") || lower.contains("pad") -> DeviceType.TABLET
            lower.contains("phone") || lower.contains("galaxy") -> DeviceType.SMARTPHONE
            else -> DeviceType.UNKNOWN
        }
    }
}

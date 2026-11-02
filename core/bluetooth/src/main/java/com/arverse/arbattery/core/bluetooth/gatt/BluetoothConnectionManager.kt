package com.arverse.arbattery.core.bluetooth.gatt

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.os.Build
import com.arverse.arbattery.core.bluetooth.BleConstants
import com.arverse.arbattery.core.bluetooth.BluetoothPermissionHelper
import com.arverse.arbattery.core.bluetooth.model.GattCharacteristicInfo
import com.arverse.arbattery.core.bluetooth.model.GattDescriptorInfo
import com.arverse.arbattery.core.bluetooth.model.GattProfile
import com.arverse.arbattery.core.bluetooth.model.GattServiceInfo
import com.arverse.arbattery.core.common.dispatcher.DefaultDispatcherProvider
import com.arverse.arbattery.core.common.dispatcher.DispatcherProvider
import com.arverse.arbattery.core.common.model.ConnectionState
import com.arverse.arbattery.core.common.result.AppResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

interface BluetoothConnectionManager {
    val connectionState: StateFlow<ConnectionState>
    val connectedDeviceAddress: StateFlow<String?>

    suspend fun connect(address: String): AppResult<Unit>
    suspend fun disconnect()
    suspend fun discoverServices(): AppResult<GattProfile>
    suspend fun readCharacteristic(serviceUuid: UUID, characteristicUuid: UUID): AppResult<ByteArray>
    suspend fun enableNotifications(
        serviceUuid: UUID,
        characteristicUuid: UUID,
        onNotification: (ByteArray) -> Unit
    ): AppResult<Unit>
}

class AndroidBluetoothConnectionManager(
    private val context: Context,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) : BluetoothConnectionManager {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? get() = bluetoothManager?.adapter

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _connectedDeviceAddress = MutableStateFlow<String?>(null)
    override val connectedDeviceAddress: StateFlow<String?> = _connectedDeviceAddress.asStateFlow()

    private var activeGatt: BluetoothGatt? = null
    private val gattMutex = Mutex()
    private val scope = CoroutineScope(dispatchers.io)

    private var connectDeferred: CompletableDeferred<Boolean>? = null
    private var serviceDiscoveryDeferred: CompletableDeferred<Boolean>? = null
    private var characteristicReadDeferred: CompletableDeferred<ByteArray?>? = null
    private var descriptorWriteDeferred: CompletableDeferred<Boolean>? = null

    private var notificationCallback: ((ByteArray) -> Unit)? = null

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _connectionState.value = ConnectionState.CONNECTED
                    connectDeferred?.complete(true)
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    _connectionState.value = ConnectionState.CONNECTING
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _connectionState.value = ConnectionState.DISCONNECTED
                    _connectedDeviceAddress.value = null
                    connectDeferred?.complete(false)
                }
                else -> {
                    _connectionState.value = ConnectionState.ERROR
                    connectDeferred?.complete(false)
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                serviceDiscoveryDeferred?.complete(true)
            } else {
                serviceDiscoveryDeferred?.complete(false)
            }
        }

        @Deprecated("Used on older Android versions")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS && characteristic != null) {
                characteristicReadDeferred?.complete(characteristic.value)
            } else {
                characteristicReadDeferred?.complete(null)
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                characteristicReadDeferred?.complete(value)
            } else {
                characteristicReadDeferred?.complete(null)
            }
        }

        @Deprecated("Used on older Android versions")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            characteristic?.value?.let { bytes ->
                notificationCallback?.invoke(bytes)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            notificationCallback?.invoke(value)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            descriptorWriteDeferred?.complete(status == BluetoothGatt.GATT_SUCCESS)
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun connect(address: String): AppResult<Unit> = gattMutex.withLock {
        if (!BluetoothPermissionHelper.hasBluetoothPermissions(context)) {
            return AppResult.Error(SecurityException("Bluetooth permission denied"))
        }

        val adapter = bluetoothAdapter ?: return AppResult.Error(IllegalStateException("Bluetooth adapter unavailable"))
        val device: BluetoothDevice = try {
            adapter.getRemoteDevice(address)
        } catch (e: Exception) {
            return AppResult.Error(IllegalArgumentException("Invalid Bluetooth address: $address", e))
        }

        disconnectInternal()

        _connectionState.value = ConnectionState.CONNECTING
        val deferred = CompletableDeferred<Boolean>()
        connectDeferred = deferred

        activeGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            device.connectGatt(context, false, gattCallback)
        }

        val success = withTimeoutOrNull(BleConstants.GATT_CONNECT_TIMEOUT_MS) {
            deferred.await()
        } ?: false

        if (success) {
            _connectedDeviceAddress.value = address
            AppResult.Success(Unit)
        } else {
            disconnectInternal()
            AppResult.Error(IllegalStateException("Connection to $address timed out or failed."))
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun disconnect() = gattMutex.withLock {
        disconnectInternal()
    }

    @SuppressLint("MissingPermission")
    private fun disconnectInternal() {
        try {
            activeGatt?.disconnect()
            activeGatt?.close()
        } catch (_: Exception) {
        }
        activeGatt = null
        _connectionState.value = ConnectionState.DISCONNECTED
        _connectedDeviceAddress.value = null
        connectDeferred = null
        serviceDiscoveryDeferred = null
        characteristicReadDeferred = null
        descriptorWriteDeferred = null
        notificationCallback = null
    }

    @SuppressLint("MissingPermission")
    override suspend fun discoverServices(): AppResult<GattProfile> = gattMutex.withLock {
        val gatt = activeGatt ?: return AppResult.Error(IllegalStateException("No active GATT connection."))

        val deferred = CompletableDeferred<Boolean>()
        serviceDiscoveryDeferred = deferred

        val started = gatt.discoverServices()
        if (!started) {
            return AppResult.Error(IllegalStateException("Failed to initiate GATT service discovery."))
        }

        val success = withTimeoutOrNull(BleConstants.GATT_OPERATION_TIMEOUT_MS) {
            deferred.await()
        } ?: false

        if (!success) {
            return AppResult.Error(IllegalStateException("Service discovery timed out or failed."))
        }

        val services = gatt.services ?: emptyList()
        val gattProfile = buildGattProfile(gatt.device, services)
        AppResult.Success(gattProfile)
    }

    @SuppressLint("MissingPermission")
    override suspend fun readCharacteristic(
        serviceUuid: UUID,
        characteristicUuid: UUID
    ): AppResult<ByteArray> = gattMutex.withLock {
        val gatt = activeGatt ?: return AppResult.Error(IllegalStateException("No active GATT connection."))
        val service = gatt.getService(serviceUuid)
            ?: return AppResult.Error(NoSuchElementException("Service $serviceUuid not found."))
        val characteristic = service.getCharacteristic(characteristicUuid)
            ?: return AppResult.Error(NoSuchElementException("Characteristic $characteristicUuid not found in service $serviceUuid."))

        val deferred = CompletableDeferred<ByteArray?>()
        characteristicReadDeferred = deferred

        val readInitiated = gatt.readCharacteristic(characteristic)
        if (!readInitiated) {
            return AppResult.Error(IllegalStateException("Failed to initiate characteristic read for $characteristicUuid."))
        }

        val resultBytes = withTimeoutOrNull(BleConstants.GATT_OPERATION_TIMEOUT_MS) {
            deferred.await()
        }

        return if (resultBytes != null) {
            AppResult.Success(resultBytes)
        } else {
            AppResult.Error(IllegalStateException("Timed out reading characteristic $characteristicUuid."))
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun enableNotifications(
        serviceUuid: UUID,
        characteristicUuid: UUID,
        onNotification: (ByteArray) -> Unit
    ): AppResult<Unit> = gattMutex.withLock {
        val gatt = activeGatt ?: return AppResult.Error(IllegalStateException("No active GATT connection."))
        val service = gatt.getService(serviceUuid)
            ?: return AppResult.Error(NoSuchElementException("Service $serviceUuid not found."))
        val characteristic = service.getCharacteristic(characteristicUuid)
            ?: return AppResult.Error(NoSuchElementException("Characteristic $characteristicUuid not found."))

        // 1. Enable local notification
        val registered = gatt.setCharacteristicNotification(characteristic, true)
        if (!registered) {
            return AppResult.Error(IllegalStateException("setCharacteristicNotification returned false for $characteristicUuid."))
        }

        // 2. Write CCCD descriptor (0x2902)
        val descriptor = characteristic.getDescriptor(BleConstants.CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID)
            ?: return AppResult.Error(NoSuchElementException("Descriptor 0x2902 not found for characteristic $characteristicUuid."))

        val deferred = CompletableDeferred<Boolean>()
        descriptorWriteDeferred = deferred
        notificationCallback = onNotification

        val valueToSet = if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        } else {
            BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
        }

        val writeInitiated = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeDescriptor(descriptor, valueToSet) == BluetoothStatusCodes.SUCCESS
        } else {
            @Suppress("DEPRECATION")
            descriptor.value = valueToSet
            @Suppress("DEPRECATION")
            gatt.writeDescriptor(descriptor)
        }

        if (!writeInitiated) {
            return AppResult.Error(IllegalStateException("Failed to initiate descriptor write for CCCD on $characteristicUuid."))
        }

        val writeSuccess = withTimeoutOrNull(BleConstants.GATT_OPERATION_TIMEOUT_MS) {
            deferred.await()
        } ?: false

        return if (writeSuccess) {
            AppResult.Success(Unit)
        } else {
            AppResult.Error(IllegalStateException("Timed out writing descriptor for $characteristicUuid."))
        }
    }

    @SuppressLint("MissingPermission")
    private fun buildGattProfile(device: BluetoothDevice, services: List<BluetoothGattService>): GattProfile {
        var hasBatteryService = false
        var hasBatteryChar = false

        val serviceInfos = services.map { service ->
            val isBatterySvc = service.uuid == BleConstants.BATTERY_SERVICE_UUID
            if (isBatterySvc) hasBatteryService = true

            val charInfos = service.characteristics.map { char ->
                if (isBatterySvc && char.uuid == BleConstants.BATTERY_LEVEL_CHARACTERISTIC_UUID) {
                    hasBatteryChar = true
                }
                val props = char.properties
                val propNames = mutableListOf<String>()
                val canRead = props and BluetoothGattCharacteristic.PROPERTY_READ != 0
                val canWrite = props and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0
                val canNotify = props and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0
                val canIndicate = props and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0

                if (canRead) propNames.add("READ")
                if (canWrite) propNames.add("WRITE")
                if (canNotify) propNames.add("NOTIFY")
                if (canIndicate) propNames.add("INDICATE")

                val descInfos = char.descriptors.map { desc ->
                    GattDescriptorInfo(desc.uuid.toString(), desc.permissions)
                }

                GattCharacteristicInfo(
                    uuid = char.uuid.toString(),
                    properties = props,
                    propertyNames = propNames,
                    canRead = canRead,
                    canNotify = canNotify,
                    canIndicate = canIndicate,
                    canWrite = canWrite,
                    descriptors = descInfos
                )
            }

            GattServiceInfo(
                uuid = service.uuid.toString(),
                serviceName = BleConstants.getServiceName(service.uuid),
                isPrimary = service.type == BluetoothGattService.SERVICE_TYPE_PRIMARY,
                characteristics = charInfos
            )
        }

        val deviceName = try {
            device.name ?: device.address
        } catch (_: SecurityException) {
            device.address
        }

        return GattProfile(
            deviceAddress = device.address,
            deviceName = deviceName,
            services = serviceInfos,
            hasBatteryService = hasBatteryService,
            hasBatteryLevelCharacteristic = hasBatteryChar
        )
    }
}

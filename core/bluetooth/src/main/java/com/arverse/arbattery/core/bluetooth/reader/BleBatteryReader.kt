package com.arverse.arbattery.core.bluetooth.reader

import com.arverse.arbattery.core.bluetooth.BleConstants
import com.arverse.arbattery.core.bluetooth.gatt.BluetoothConnectionManager
import com.arverse.arbattery.core.common.model.ChargingState
import com.arverse.arbattery.core.common.model.ConnectionMethod
import com.arverse.arbattery.core.common.model.ConnectionState
import com.arverse.arbattery.core.common.model.DeviceBattery
import com.arverse.arbattery.core.common.model.ReadingQuality
import com.arverse.arbattery.core.common.result.AppResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID

interface BleBatteryReader {
    suspend fun readBattery(
        address: String,
        deviceName: String = "Bluetooth Device"
    ): AppResult<DeviceBattery>

    fun observeBatteryUpdates(
        address: String
    ): Flow<Int>
}

class StandardBleBatteryReader(
    private val connectionManager: BluetoothConnectionManager
) : BleBatteryReader {

    override suspend fun readBattery(
        address: String,
        deviceName: String
    ): AppResult<DeviceBattery> {
        val connectResult = connectionManager.connect(address)
        if (connectResult is AppResult.Error) {
            return AppResult.Error(
                connectResult.exception,
                "Failed to connect to $deviceName ($address): ${connectResult.message}"
            )
        }

        val servicesResult = connectionManager.discoverServices()
        if (servicesResult is AppResult.Error) {
            return AppResult.Error(
                servicesResult.exception,
                "GATT service discovery failed for $deviceName: ${servicesResult.message}"
            )
        }

        val profile = (servicesResult as AppResult.Success).data
        if (!profile.hasBatteryService) {
            return AppResult.Success(
                DeviceBattery(
                    deviceId = address,
                    deviceName = deviceName,
                    batteryPercentage = null,
                    chargingState = ChargingState.UNKNOWN,
                    connectionState = ConnectionState.CONNECTED,
                    connectionMethod = ConnectionMethod.BLE,
                    lastUpdated = System.currentTimeMillis(),
                    lastSuccessfulReading = null,
                    capabilityStatus = ReadingQuality.UNSUPPORTED,
                    errorState = "Standard Bluetooth Battery Service (0x180F) not found on device."
                )
            )
        }

        if (!profile.hasBatteryLevelCharacteristic) {
            return AppResult.Success(
                DeviceBattery(
                    deviceId = address,
                    deviceName = deviceName,
                    batteryPercentage = null,
                    chargingState = ChargingState.UNKNOWN,
                    connectionState = ConnectionState.CONNECTED,
                    connectionMethod = ConnectionMethod.BLE,
                    lastUpdated = System.currentTimeMillis(),
                    lastSuccessfulReading = null,
                    capabilityStatus = ReadingQuality.UNSUPPORTED,
                    errorState = "Battery Level characteristic (0x2A19) not found."
                )
            )
        }

        val readResult = connectionManager.readCharacteristic(
            BleConstants.BATTERY_SERVICE_UUID,
            BleConstants.BATTERY_LEVEL_CHARACTERISTIC_UUID
        )

        return when (readResult) {
            is AppResult.Success -> {
                val bytes = readResult.data
                if (bytes.isEmpty()) {
                    return AppResult.Error(IllegalStateException("Empty characteristic data received from $address"))
                }
                val rawPercentage = bytes[0].toInt() and 0xFF
                if (rawPercentage !in 0..100) {
                    return AppResult.Error(
                        IllegalArgumentException("Invalid battery percentage received: $rawPercentage (must be 0-100)")
                    )
                }

                val now = System.currentTimeMillis()
                AppResult.Success(
                    DeviceBattery(
                        deviceId = address,
                        deviceName = deviceName,
                        batteryPercentage = rawPercentage,
                        chargingState = ChargingState.UNKNOWN,
                        connectionState = ConnectionState.CONNECTED,
                        connectionMethod = ConnectionMethod.BLE,
                        lastUpdated = now,
                        lastSuccessfulReading = now,
                        capabilityStatus = ReadingQuality.LIVE,
                        errorState = null
                    )
                )
            }
            is AppResult.Error -> {
                AppResult.Error(readResult.exception, "Failed to read battery characteristic: ${readResult.message}")
            }
            is AppResult.Loading -> AppResult.Loading
        }
    }

    override fun observeBatteryUpdates(address: String): Flow<Int> = callbackFlow {
        val result = connectionManager.enableNotifications(
            BleConstants.BATTERY_SERVICE_UUID,
            BleConstants.BATTERY_LEVEL_CHARACTERISTIC_UUID
        ) { rawBytes ->
            if (rawBytes.isNotEmpty()) {
                val pct = rawBytes[0].toInt() and 0xFF
                if (pct in 0..100) {
                    trySend(pct)
                }
            }
        }

        if (result is AppResult.Error) {
            close(result.exception)
        }

        awaitClose {
            // Channel cleanup
        }
    }
}

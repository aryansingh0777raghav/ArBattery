package com.arverse.arbattery.core.device.integration

import android.content.Context
import com.arverse.arbattery.core.bluetooth.BluetoothPermissionHelper
import com.arverse.arbattery.core.bluetooth.reader.BleBatteryReader
import com.arverse.arbattery.core.common.model.ConnectionMethod
import com.arverse.arbattery.core.common.model.DeviceBattery
import com.arverse.arbattery.core.common.model.DeviceType
import com.arverse.arbattery.core.common.result.AppResult

class StandardBleBatteryIntegration(
    private val context: Context,
    private val bleBatteryReader: BleBatteryReader
) : DeviceIntegration {

    override val supportedTypes: Set<DeviceType> = setOf(
        DeviceType.SMARTWATCH,
        DeviceType.EARBUDS,
        DeviceType.HEADPHONES,
        DeviceType.ACCESSORY,
        DeviceType.UNKNOWN
    )

    override val connectionMethod: ConnectionMethod = ConnectionMethod.BLE
    override val requiredPermissions: List<String> = BluetoothPermissionHelper.getRequiredBluetoothPermissions().toList()
    override val supportsBatteryReading: Boolean = true
    override val supportsForceRefresh: Boolean = true
    override val supportsChargingInfo: Boolean = false // Standard BLE 0x180F characteristic only exposes level
    override val integrationName: String = "Standard Bluetooth LE Battery Service (0x180F)"

    override suspend fun readBattery(
        deviceId: String,
        deviceName: String
    ): AppResult<DeviceBattery> {
        return bleBatteryReader.readBattery(deviceId, deviceName)
    }

    override fun isAvailable(): Boolean {
        return BluetoothPermissionHelper.isBluetoothSupported(context) &&
                BluetoothPermissionHelper.isBluetoothEnabled(context) &&
                BluetoothPermissionHelper.hasBluetoothPermissions(context)
    }
}

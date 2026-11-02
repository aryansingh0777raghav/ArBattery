package com.arverse.arbattery.core.bluetooth.reader

import com.arverse.arbattery.core.bluetooth.BleConstants
import com.arverse.arbattery.core.bluetooth.gatt.BluetoothConnectionManager
import com.arverse.arbattery.core.bluetooth.model.GattCharacteristicInfo
import com.arverse.arbattery.core.bluetooth.model.GattProfile
import com.arverse.arbattery.core.bluetooth.model.GattServiceInfo
import com.arverse.arbattery.core.common.model.ConnectionState
import com.arverse.arbattery.core.common.model.ReadingQuality
import com.arverse.arbattery.core.common.result.AppResult
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StandardBleBatteryReaderTest {

    private val connectionManager: BluetoothConnectionManager = mockk(relaxed = true)
    private lateinit var batteryReader: StandardBleBatteryReader

    private val testAddress = "AA:BB:CC:DD:EE:FF"
    private val testDeviceName = "Hammer Ace"

    @Before
    fun setup() {
        coEvery { connectionManager.connectionState } returns MutableStateFlow(ConnectionState.CONNECTED)
        batteryReader = StandardBleBatteryReader(connectionManager)
    }

    @Test
    fun `readBattery successfully parses valid percentage when standard battery service exists`() = runTest {
        coEvery { connectionManager.connect(testAddress) } returns AppResult.Success(Unit)

        val profile = GattProfile(
            deviceAddress = testAddress,
            deviceName = testDeviceName,
            services = listOf(
                GattServiceInfo(
                    uuid = BleConstants.BATTERY_SERVICE_UUID.toString(),
                    serviceName = "Battery Service",
                    isPrimary = true,
                    characteristics = listOf(
                        GattCharacteristicInfo(
                            uuid = BleConstants.BATTERY_LEVEL_CHARACTERISTIC_UUID.toString(),
                            properties = 0x12,
                            propertyNames = listOf("READ", "NOTIFY"),
                            canRead = true,
                            canNotify = true,
                            canIndicate = false,
                            canWrite = false
                        )
                    )
                )
            ),
            hasBatteryService = true,
            hasBatteryLevelCharacteristic = true
        )
        coEvery { connectionManager.discoverServices() } returns AppResult.Success(profile)
        // 85% as single byte (0x55)
        coEvery {
            connectionManager.readCharacteristic(
                BleConstants.BATTERY_SERVICE_UUID,
                BleConstants.BATTERY_LEVEL_CHARACTERISTIC_UUID
            )
        } returns AppResult.Success(byteArrayOf(85.toByte()))

        val result = batteryReader.readBattery(testAddress, testDeviceName)

        assertTrue(result is AppResult.Success)
        val battery = (result as AppResult.Success).data
        assertEquals(85, battery.batteryPercentage)
        assertEquals(ReadingQuality.LIVE, battery.capabilityStatus)
        assertEquals(testDeviceName, battery.deviceName)
        assertNull(battery.errorState)
    }

    @Test
    fun `readBattery returns unsupported state when battery service is missing`() = runTest {
        coEvery { connectionManager.connect(testAddress) } returns AppResult.Success(Unit)

        val profileWithoutBattery = GattProfile(
            deviceAddress = testAddress,
            deviceName = testDeviceName,
            services = listOf(
                GattServiceInfo(
                    uuid = "0000fee7-0000-1000-8000-00805f9b34fb",
                    serviceName = "Vendor Service",
                    isPrimary = true,
                    characteristics = emptyList()
                )
            ),
            hasBatteryService = false,
            hasBatteryLevelCharacteristic = false
        )
        coEvery { connectionManager.discoverServices() } returns AppResult.Success(profileWithoutBattery)

        val result = batteryReader.readBattery(testAddress, testDeviceName)

        assertTrue(result is AppResult.Success)
        val battery = (result as AppResult.Success).data
        assertNull(battery.batteryPercentage)
        assertEquals(ReadingQuality.UNSUPPORTED, battery.capabilityStatus)
        assertTrue(battery.errorState?.contains("not found") == true)
    }

    @Test
    fun `readBattery fails when characteristic read times out or errors`() = runTest {
        coEvery { connectionManager.connect(testAddress) } returns AppResult.Success(Unit)

        val profile = GattProfile(
            deviceAddress = testAddress,
            deviceName = testDeviceName,
            services = emptyList(),
            hasBatteryService = true,
            hasBatteryLevelCharacteristic = true
        )
        coEvery { connectionManager.discoverServices() } returns AppResult.Success(profile)
        coEvery {
            connectionManager.readCharacteristic(any(), any())
        } returns AppResult.Error(IllegalStateException("Read timed out"))

        val result = batteryReader.readBattery(testAddress, testDeviceName)

        assertTrue(result is AppResult.Error)
        assertEquals("Failed to read battery characteristic: Read timed out", (result as AppResult.Error).message)
    }

    @Test
    fun `readBattery rejects byte values outside 0 to 100`() = runTest {
        coEvery { connectionManager.connect(testAddress) } returns AppResult.Success(Unit)

        val profile = GattProfile(
            deviceAddress = testAddress,
            deviceName = testDeviceName,
            services = emptyList(),
            hasBatteryService = true,
            hasBatteryLevelCharacteristic = true
        )
        coEvery { connectionManager.discoverServices() } returns AppResult.Success(profile)
        // 150 (0x96) is invalid percentage
        coEvery {
            connectionManager.readCharacteristic(any(), any())
        } returns AppResult.Success(byteArrayOf(150.toByte()))

        val result = batteryReader.readBattery(testAddress, testDeviceName)

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).message?.contains("Invalid battery percentage") == true)
    }
}

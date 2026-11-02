package com.arverse.arbattery.core.bluetooth.diagnostic

import com.arverse.arbattery.core.bluetooth.BleConstants
import com.arverse.arbattery.core.bluetooth.gatt.BluetoothConnectionManager
import com.arverse.arbattery.core.bluetooth.model.DiagnosticReport
import com.arverse.arbattery.core.bluetooth.model.GattCharacteristicInfo
import com.arverse.arbattery.core.bluetooth.model.GattProfile
import com.arverse.arbattery.core.bluetooth.model.GattServiceInfo
import com.arverse.arbattery.core.common.result.AppResult
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HammerAceDiagnosticScannerTest {

    private val connectionManager: BluetoothConnectionManager = mockk(relaxed = true)
    private lateinit var diagnosticScanner: HammerAceDiagnosticScanner

    private val testAddress = "FF:EE:DD:CC:BB:AA"
    private val testDeviceName = "Hammer Ace"

    @Before
    fun setup() {
        diagnosticScanner = HammerAceDiagnosticScanner(connectionManager)
    }

    @Test
    fun `runDiagnostic flags unsupported when standard 0x180F service is missing`() = runTest {
        coEvery { connectionManager.connect(testAddress) } returns AppResult.Success(Unit)

        val proprietaryProfile = GattProfile(
            deviceAddress = testAddress,
            deviceName = testDeviceName,
            services = listOf(
                GattServiceInfo(
                    uuid = "0000fee7-0000-1000-8000-00805f9b34fb",
                    serviceName = "Tencent Sports / Proprietary",
                    isPrimary = true,
                    characteristics = listOf(
                        GattCharacteristicInfo(
                            uuid = "0000fea1-0000-1000-8000-00805f9b34fb",
                            properties = 0x08,
                            propertyNames = listOf("WRITE"),
                            canRead = false,
                            canNotify = false,
                            canIndicate = false,
                            canWrite = true
                        )
                    )
                )
            ),
            hasBatteryService = false,
            hasBatteryLevelCharacteristic = false
        )
        coEvery { connectionManager.discoverServices() } returns AppResult.Success(proprietaryProfile)

        val report = diagnosticScanner.runDiagnostic(testAddress, testDeviceName)

        assertEquals(DiagnosticReport.CompatibilityVerdict.UNSUPPORTED_STANDARD_SERVICE_MISSING, report.compatibilityVerdict)
        assertFalse(report.isStandardBatteryServiceFound)
        assertNotNull(report.exportAsJson())
        assertTrue(report.exportAsText().contains("Standard Battery Service (0x180F): NOT FOUND"))
    }

    @Test
    fun `runDiagnostic marks compatible when standard battery service and characteristic read successfully`() = runTest {
        coEvery { connectionManager.connect(testAddress) } returns AppResult.Success(Unit)

        val compatibleProfile = GattProfile(
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
        coEvery { connectionManager.discoverServices() } returns AppResult.Success(compatibleProfile)
        coEvery {
            connectionManager.readCharacteristic(BleConstants.BATTERY_SERVICE_UUID, BleConstants.BATTERY_LEVEL_CHARACTERISTIC_UUID)
        } returns AppResult.Success(byteArrayOf(92.toByte()))

        val report = diagnosticScanner.runDiagnostic(testAddress, testDeviceName)

        assertEquals(DiagnosticReport.CompatibilityVerdict.COMPATIBLE_STANDARD_BLE, report.compatibilityVerdict)
        assertTrue(report.isStandardBatteryServiceFound)
        assertTrue(report.isBatteryLevelCharacteristicFound)
        assertEquals(92, report.batteryValueRead)
        assertTrue(report.exportAsText().contains("92%"))
    }
}

package com.arverse.arbattery.core.database.repository

import com.arverse.arbattery.core.common.model.ChargingState
import com.arverse.arbattery.core.common.model.ConnectionMethod
import com.arverse.arbattery.core.common.model.ConnectionState
import com.arverse.arbattery.core.common.model.DeviceBattery
import com.arverse.arbattery.core.common.model.DeviceType
import com.arverse.arbattery.core.common.model.ReadingQuality
import com.arverse.arbattery.core.common.result.AppResult
import com.arverse.arbattery.core.database.dao.DeviceDao
import com.arverse.arbattery.core.database.entity.DeviceEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeviceRepositoryTest {

    private val deviceDao: DeviceDao = mockk(relaxed = true)
    private lateinit var repository: DefaultDeviceRepository

    @Before
    fun setup() {
        repository = DefaultDeviceRepository(deviceDao)
    }

    @Test
    fun `getAllDevicesFlow maps entities to domain models`() = runTest {
        val entities = listOf(
            DeviceEntity(
                id = "dev-1",
                name = "Galaxy Tab A",
                type = DeviceType.TABLET,
                connectionMethod = ConnectionMethod.WIFI,
                connectionStatus = ConnectionState.CONNECTED,
                batteryPercentage = 75,
                chargingState = ChargingState.CHARGING,
                lastUpdated = 1000L,
                lastSuccessfulReading = 1000L,
                capabilityStatus = ReadingQuality.LIVE
            )
        )
        coEvery { deviceDao.getAllDevicesFlow() } returns flowOf(entities)

        val devices = repository.getAllDevicesFlow().first()

        assertEquals(1, devices.size)
        val device = devices.first()
        assertEquals("dev-1", device.deviceId)
        assertEquals("Galaxy Tab A", device.deviceName)
        assertEquals(75, device.batteryPercentage)
        assertEquals(ChargingState.CHARGING, device.chargingState)
        assertEquals(ConnectionState.CONNECTED, device.connectionState)
    }

    @Test
    fun `saveDevice calls insertOrUpdate on Dao`() = runTest {
        val model = DeviceBattery(
            deviceId = "dev-2",
            deviceName = "Hammer Ace",
            deviceType = DeviceType.SMARTWATCH,
            batteryPercentage = 50,
            chargingState = ChargingState.DISCHARGING,
            connectionState = ConnectionState.CONNECTED,
            connectionMethod = ConnectionMethod.BLE
        )

        val result = repository.saveDevice(model)

        assertTrue(result is AppResult.Success)
        coVerify(exactly = 1) { deviceDao.insertOrUpdateDevice(any()) }
    }

    @Test
    fun `deleteDevice calls deleteDeviceById on Dao`() = runTest {
        val result = repository.deleteDevice("dev-1")

        assertTrue(result is AppResult.Success)
        coVerify(exactly = 1) { deviceDao.deleteDeviceById("dev-1") }
    }
}

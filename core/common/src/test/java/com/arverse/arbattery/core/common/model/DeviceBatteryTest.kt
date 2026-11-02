package com.arverse.arbattery.core.common.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceBatteryTest {

    @Test
    fun `DeviceBattery thresholds work correctly`() {
        val normalDevice = DeviceBattery(
            deviceId = "dev-1",
            deviceName = "Hammer Ace",
            batteryPercentage = 80
        )
        assertFalse(normalDevice.isLow(20))
        assertFalse(normalDevice.isCritical(10))

        val lowDevice = DeviceBattery(
            deviceId = "dev-2",
            deviceName = "Hammer Ace",
            batteryPercentage = 15
        )
        assertTrue(lowDevice.isLow(20))
        assertFalse(lowDevice.isCritical(10))

        val criticalDevice = DeviceBattery(
            deviceId = "dev-3",
            deviceName = "Hammer Ace",
            batteryPercentage = 8
        )
        assertFalse(criticalDevice.isLow(20)) // critical is <= 10
        assertTrue(criticalDevice.isCritical(10))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `DeviceBattery rejects percentage greater than 100`() {
        DeviceBattery(
            deviceId = "dev-invalid",
            deviceName = "Invalid Device",
            batteryPercentage = 101
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `DeviceBattery rejects negative percentage`() {
        DeviceBattery(
            deviceId = "dev-invalid",
            deviceName = "Invalid Device",
            batteryPercentage = -5
        )
    }

    @Test
    fun `DeviceBattery display percentage shows placeholder when null`() {
        val unreadDevice = DeviceBattery(
            deviceId = "dev-unknown",
            deviceName = "Unread Device",
            batteryPercentage = null
        )
        assertEquals("--", unreadDevice.displayPercentage)
        assertFalse(unreadDevice.isAvailable)
    }
}

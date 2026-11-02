package com.arverse.arbattery.core.device.evaluator

import com.arverse.arbattery.core.common.model.DeviceBattery
import com.arverse.arbattery.core.common.model.ReadingQuality
import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryFreshnessEvaluatorTest {

    private val now = 1_700_000_000_000L

    @Test
    fun `evaluateFreshness marks recent reading as LIVE`() {
        val device = DeviceBattery(
            deviceId = "dev-1",
            deviceName = "Hammer Ace",
            batteryPercentage = 80,
            lastUpdated = now - 30_000L, // 30 sec ago
            capabilityStatus = ReadingQuality.LIVE
        )

        val evaluated = BatteryFreshnessEvaluator.evaluateFreshness(device, now)
        assertEquals(ReadingQuality.LIVE, evaluated.capabilityStatus)
    }

    @Test
    fun `evaluateFreshness marks 5 min old reading as RECENT`() {
        val device = DeviceBattery(
            deviceId = "dev-1",
            deviceName = "Hammer Ace",
            batteryPercentage = 80,
            lastUpdated = now - 5 * 60_000L, // 5 min ago
            capabilityStatus = ReadingQuality.LIVE
        )

        val evaluated = BatteryFreshnessEvaluator.evaluateFreshness(device, now)
        assertEquals(ReadingQuality.RECENT, evaluated.capabilityStatus)
    }

    @Test
    fun `evaluateFreshness marks 30 min old reading as STALE`() {
        val device = DeviceBattery(
            deviceId = "dev-1",
            deviceName = "Hammer Ace",
            batteryPercentage = 80,
            lastUpdated = now - 30 * 60_000L, // 30 min ago
            capabilityStatus = ReadingQuality.LIVE
        )

        val evaluated = BatteryFreshnessEvaluator.evaluateFreshness(device, now)
        assertEquals(ReadingQuality.STALE, evaluated.capabilityStatus)
    }

    @Test
    fun `evaluateFreshness preserves UNSUPPORTED and null readings`() {
        val unsupportedDevice = DeviceBattery(
            deviceId = "dev-unsupported",
            deviceName = "Unsupported Watch",
            batteryPercentage = null,
            capabilityStatus = ReadingQuality.UNSUPPORTED
        )

        val evaluated = BatteryFreshnessEvaluator.evaluateFreshness(unsupportedDevice, now)
        assertEquals(ReadingQuality.UNSUPPORTED, evaluated.capabilityStatus)
    }
}

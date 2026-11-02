package com.arverse.arbattery.core.device.evaluator

import com.arverse.arbattery.core.common.model.DeviceBattery
import com.arverse.arbattery.core.common.model.ReadingQuality
import com.arverse.arbattery.core.common.util.BatteryFormatting

object BatteryFreshnessEvaluator {

    fun evaluateFreshness(
        battery: DeviceBattery,
        currentTime: Long = System.currentTimeMillis()
    ): DeviceBattery {
        // If device has no reading or is unsupported/error, preserve that state
        if (battery.capabilityStatus == ReadingQuality.UNSUPPORTED ||
            battery.capabilityStatus == ReadingQuality.ERROR ||
            battery.batteryPercentage == null
        ) {
            return battery
        }

        val quality = BatteryFormatting.computeFreshness(battery.lastUpdated, currentTime)
        return battery.copy(capabilityStatus = quality)
    }
}

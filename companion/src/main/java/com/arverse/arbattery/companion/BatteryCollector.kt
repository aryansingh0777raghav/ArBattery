package com.arverse.arbattery.companion

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import com.arverse.arbattery.core.common.model.ChargingState
import com.arverse.arbattery.core.common.model.ConnectionMethod
import com.arverse.arbattery.core.common.model.DeviceType
import com.arverse.arbattery.core.network.dto.BatteryReportDto

data class DetailedBatteryInfo(
    val percentage: Int,
    val chargingState: ChargingState,
    val powerSource: String,
    val health: String,
    val voltageMv: Int,
    val temperatureCelsius: Float,
    val technology: String?,
    val deviceModel: String,
    val deviceManufacturer: String,
    val deviceDisplayName: String,
    val timestamp: Long = System.currentTimeMillis()
)

class BatteryCollector(private val context: Context) {

    fun collectBatteryInfo(): DetailedBatteryInfo {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, filter)

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val percentage = if (level >= 0 && scale > 0) {
            (level * 100 / scale.toFloat()).toInt().coerceIn(0, 100)
        } else {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 0
        }

        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val chargingState = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> ChargingState.CHARGING
            BatteryManager.BATTERY_STATUS_FULL -> ChargingState.FULL
            BatteryManager.BATTERY_STATUS_DISCHARGING -> ChargingState.DISCHARGING
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> ChargingState.NOT_CHARGING
            else -> ChargingState.UNKNOWN
        }

        val plugged = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val powerSource = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Charger"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB Cable"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless Dock"
            else -> "Battery Discharging"
        }

        val healthCode = batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
        val health = when (healthCode) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
            else -> "Normal"
        }

        val voltageMv = batteryStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: 0
        val tempRaw = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: 0
        val temperatureCelsius = tempRaw / 10f

        val technology = batteryStatus?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)

        val model = Build.MODEL ?: "Galaxy Tab A 8.0"
        val manufacturer = Build.MANUFACTURER ?: "Samsung"
        val displayName = "$manufacturer $model"

        return DetailedBatteryInfo(
            percentage = percentage,
            chargingState = chargingState,
            powerSource = powerSource,
            health = health,
            voltageMv = voltageMv,
            temperatureCelsius = temperatureCelsius,
            technology = technology,
            deviceModel = model,
            deviceManufacturer = manufacturer,
            deviceDisplayName = displayName
        )
    }

    fun toBatteryReportDto(deviceId: String, info: DetailedBatteryInfo): BatteryReportDto {
        return BatteryReportDto(
            deviceId = deviceId,
            deviceName = info.deviceDisplayName,
            deviceType = DeviceType.TABLET.name,
            batteryPercentage = info.percentage,
            charging = info.chargingState == ChargingState.CHARGING || info.chargingState == ChargingState.FULL,
            chargingState = info.chargingState.name,
            connectionMethod = ConnectionMethod.WIFI.name,
            timestamp = info.timestamp,
            manufacturer = info.deviceManufacturer,
            model = info.deviceModel
        )
    }
}

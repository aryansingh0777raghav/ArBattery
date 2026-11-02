package com.arverse.arbattery.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.arverse.arbattery.core.common.model.ChargingState
import com.arverse.arbattery.core.common.model.DeviceBattery
import com.arverse.arbattery.core.database.ArBatteryDatabase

class ArBatteryMediumWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = ArBatteryDatabase.getInstance(context)
        val devices = database.deviceDao().getAllDevices().map { it.toDomainModel() }

        val displayDevices = devices.take(3)

        provideContent {
            MediumWidgetContent(
                context = context,
                devicesCount = devices.size,
                displayDevices = displayDevices
            )
        }
    }
}

@Composable
private fun MediumWidgetContent(
    context: Context,
    devicesCount: Int,
    displayDevices: List<DeviceBattery>
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color(0xFF1B1C24)))
            .cornerRadius(18.dp)
            .padding(14.dp)
            .clickable(actionStartActivity(context.packageManager.getLaunchIntentForPackage(context.packageName)?.component?.className?.let {
                android.content.ComponentName(context.packageName, it)
            } ?: android.content.ComponentName(context.packageName, "com.arverse.arbattery.MainActivity"))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Header Row
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ArBattery",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF0072DE)),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = "$devicesCount Devices",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFA0A6B2)),
                        fontSize = 11.sp
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            if (displayDevices.isEmpty()) {
                Box(
                    modifier = GlanceModifier.fillMaxWidth().height(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No devices paired yet. Open ArBattery to connect.",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFA0A6B2)),
                            fontSize = 12.sp
                        )
                    )
                }
            } else {
                displayDevices.forEach { dev ->
                    DeviceWidgetRow(device = dev)
                    Spacer(modifier = GlanceModifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun DeviceWidgetRow(device: DeviceBattery) {
    val pct = device.batteryPercentage
    val pctText = pct?.let { "$it%" } ?: "--"
    val color = when {
        pct == null -> Color(0xFF8E99A4)
        pct >= 50 -> Color(0xFF2ECC71)
        pct >= 20 -> Color(0xFFF39C12)
        else -> Color(0xFFE74C3C)
    }

    val isCharging = device.chargingState == ChargingState.CHARGING

    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = device.deviceName,
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(
                color = ColorProvider(Color(0xFFF5F7FA)),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        )

        if (isCharging) {
            Text(
                text = "⚡",
                style = TextStyle(
                    color = ColorProvider(Color(0xFF2ECC71)),
                    fontSize = 12.sp
                )
            )
            Spacer(modifier = GlanceModifier.width(4.dp))
        }

        Text(
            text = pctText,
            style = TextStyle(
                color = ColorProvider(color),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

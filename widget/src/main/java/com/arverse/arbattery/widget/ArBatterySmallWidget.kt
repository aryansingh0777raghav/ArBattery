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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.arverse.arbattery.core.common.model.DeviceBattery
import com.arverse.arbattery.core.database.ArBatteryDatabase

class ArBatterySmallWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = ArBatteryDatabase.getInstance(context)
        val devices = database.deviceDao().getAllDevices().map { it.toDomainModel() }

        val lowestDevice: DeviceBattery? = devices.filter { it.batteryPercentage != null }
            .minByOrNull { it.batteryPercentage!! }

        provideContent {
            SmallWidgetContent(
                context = context,
                devicesCount = devices.size,
                lowestDevice = lowestDevice
            )
        }
    }
}

@Composable
private fun SmallWidgetContent(
    context: Context,
    devicesCount: Int,
    lowestDevice: DeviceBattery?
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
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
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            if (lowestDevice != null) {
                val pct = lowestDevice.batteryPercentage ?: 0
                val color = when {
                    pct >= 50 -> Color(0xFF2ECC71)
                    pct >= 20 -> Color(0xFFF39C12)
                    else -> Color(0xFFE74C3C)
                }

                Text(
                    text = "$pct%",
                    style = TextStyle(
                        color = ColorProvider(color),
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = GlanceModifier.height(2.dp))

                Text(
                    text = lowestDevice.deviceName,
                    maxLines = 1,
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFA0A6B2)),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            } else {
                Text(
                    text = "--",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF8E99A4)),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "No devices",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFA0A6B2)),
                        fontSize = 12.sp
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            Text(
                text = "$devicesCount device${if (devicesCount == 1) "" else "s"}",
                style = TextStyle(
                    color = ColorProvider(Color(0xFF6B7280)),
                    fontSize = 11.sp
                )
            )
        }
    }
}

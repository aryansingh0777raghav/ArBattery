package com.arverse.arbattery.core.designsystem.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arverse.arbattery.core.common.model.ChargingState
import com.arverse.arbattery.core.designsystem.theme.BatteryAmber
import com.arverse.arbattery.core.designsystem.theme.BatteryGreen
import com.arverse.arbattery.core.designsystem.theme.BatteryGrey
import com.arverse.arbattery.core.designsystem.theme.BatteryRed

fun getBatteryColor(percentage: Int?, isConnected: Boolean = true): Color {
    if (!isConnected || percentage == null) return BatteryGrey
    return when {
        percentage > 20 -> BatteryGreen
        percentage > 10 -> BatteryAmber
        else -> BatteryRed
    }
}

@Composable
fun BatteryBar(
    percentage: Int?,
    chargingState: ChargingState = ChargingState.UNKNOWN,
    isConnected: Boolean = true,
    height: Dp = 8.dp,
    modifier: Modifier = Modifier
) {
    val batteryColor = getBatteryColor(percentage, isConnected)
    val fraction = ((percentage ?: 0).coerceIn(0, 100)) / 100f

    val isCharging = chargingState == ChargingState.CHARGING

    val alpha = if (isCharging) {
        val transition = rememberInfiniteTransition(label = "charging_pulse")
        val animatedAlpha by transition.animateFloat(
            initialValue = 0.6f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )
        animatedAlpha
    } else {
        1.0f
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .fillMaxHeight()
                .clip(RoundedCornerShape(height / 2))
                .background(batteryColor.copy(alpha = alpha))
        )
    }
}

@Composable
fun BatteryCircularGauge(
    percentage: Int?,
    chargingState: ChargingState = ChargingState.UNKNOWN,
    isConnected: Boolean = true,
    size: Dp = 120.dp,
    strokeWidth: Dp = 10.dp,
    modifier: Modifier = Modifier
) {
    val batteryColor = getBatteryColor(percentage, isConnected)
    val sweepAngle = ((percentage ?: 0).coerceIn(0, 100) / 100f) * 360f
    val isCharging = chargingState == ChargingState.CHARGING

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        val trackColor = MaterialTheme.colorScheme.surfaceVariant
        Canvas(modifier = Modifier.size(size - strokeWidth)) {
            // Track
            drawCircle(
                color = trackColor,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
            // Progress
            if (sweepAngle > 0f) {
                drawArc(
                    color = batteryColor,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = percentage?.let { "$it%" } ?: "--",
                fontSize = (size.value * 0.22).sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (isCharging) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = "Charging",
                    tint = BatteryGreen,
                    modifier = Modifier.size((size.value * 0.20).dp)
                )
            }
        }
    }
}

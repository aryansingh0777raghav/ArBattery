package com.arverse.arbattery.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arverse.arbattery.core.common.model.ConnectionState
import com.arverse.arbattery.core.common.model.ReadingQuality
import com.arverse.arbattery.core.designsystem.theme.BatteryAmber
import com.arverse.arbattery.core.designsystem.theme.BatteryGreen
import com.arverse.arbattery.core.designsystem.theme.BatteryGrey
import com.arverse.arbattery.core.designsystem.theme.BatteryRed

@Composable
fun ReadingQualityBadge(
    quality: ReadingQuality,
    modifier: Modifier = Modifier
) {
    val (label, bg, fg) = when (quality) {
        ReadingQuality.LIVE -> Triple("LIVE", BatteryGreen.copy(alpha = 0.15f), BatteryGreen)
        ReadingQuality.RECENT -> Triple("RECENT", BatteryAmber.copy(alpha = 0.15f), BatteryAmber)
        ReadingQuality.STALE -> Triple("STALE", BatteryGrey.copy(alpha = 0.2f), BatteryGrey)
        ReadingQuality.UNAVAILABLE -> Triple("UNAVAILABLE", BatteryGrey.copy(alpha = 0.2f), BatteryGrey)
        ReadingQuality.UNSUPPORTED -> Triple("UNSUPPORTED", BatteryRed.copy(alpha = 0.15f), BatteryRed)
        ReadingQuality.ERROR -> Triple("ERROR", BatteryRed.copy(alpha = 0.15f), BatteryRed)
    }

    Box(
        modifier = modifier
            .background(color = bg, shape = RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = fg,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun ConnectionStateBadge(
    state: ConnectionState,
    modifier: Modifier = Modifier
) {
    val (label, bg, fg) = when (state) {
        ConnectionState.CONNECTED -> Triple("CONNECTED", BatteryGreen.copy(alpha = 0.15f), BatteryGreen)
        ConnectionState.CONNECTING -> Triple("CONNECTING...", BatteryAmber.copy(alpha = 0.15f), BatteryAmber)
        ConnectionState.DISCONNECTED -> Triple("DISCONNECTED", BatteryGrey.copy(alpha = 0.2f), BatteryGrey)
        ConnectionState.ERROR -> Triple("ERROR", BatteryRed.copy(alpha = 0.15f), BatteryRed)
    }

    Box(
        modifier = modifier
            .background(color = bg, shape = RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = fg,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

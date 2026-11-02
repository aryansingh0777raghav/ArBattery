package com.arverse.arbattery.feature.device_details

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arverse.arbattery.core.common.model.ChargingState
import com.arverse.arbattery.core.common.model.ConnectionState
import com.arverse.arbattery.core.common.model.DeviceBattery
import com.arverse.arbattery.core.common.util.BatteryFormatting
import com.arverse.arbattery.core.database.repository.BatteryHistoryItem
import com.arverse.arbattery.core.designsystem.component.BatteryCircularGauge
import com.arverse.arbattery.core.designsystem.component.ConnectionStateBadge
import com.arverse.arbattery.core.designsystem.component.DeviceTypeIcon
import com.arverse.arbattery.core.designsystem.component.OneUiCard
import com.arverse.arbattery.core.designsystem.component.OneUiLargeHeader
import com.arverse.arbattery.core.designsystem.component.ReadingQualityBadge
import com.arverse.arbattery.core.designsystem.theme.BatteryAmber
import com.arverse.arbattery.core.designsystem.theme.BatteryGreen
import com.arverse.arbattery.core.designsystem.theme.BatteryRed
import com.arverse.arbattery.core.designsystem.theme.OneUiBlue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DeviceDetailsScreen(
    viewModel: DeviceDetailsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.infoMessage) {
        uiState.infoMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            OneUiLargeHeader(
                title = uiState.device?.deviceName ?: "Device Details",
                subtitle = uiState.device?.deviceType?.displayName ?: "Battery Telemetry",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        val dev = uiState.device
        if (dev == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = OneUiBlue)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hero Battery Gauge Card
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    HeroGaugeCard(device = dev)
                }

                // Controls & Actions Card
                item {
                    ControlsCard(
                        device = dev,
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = { viewModel.refreshBattery() },
                        onToggleConnection = { viewModel.toggleConnection() },
                        onDeleteClick = { showDeleteConfirmDialog = true }
                    )
                }

                // Device Specifications Card
                item {
                    SpecificationsCard(device = dev)
                }

                // Battery History Chart
                item {
                    BatteryHistoryChartCard(history = uiState.history)
                }

                // Recent Readings List
                item {
                    Text(
                        "Recorded Telemetry (${uiState.history.size} points)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                if (uiState.history.isEmpty()) {
                    item {
                        OneUiCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "No history readings recorded yet. Historical trends are built as periodic sync occurs.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(uiState.history.take(10), key = { it.id }) { reading ->
                        ReadingHistoryItem(reading = reading)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Remove Device?") },
            text = { Text("Are you sure you want to remove \"${uiState.device?.deviceName}\"? All historical battery logs for this device will be deleted.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteDevice { onNavigateBack() }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BatteryRed)
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun HeroGaugeCard(device: DeviceBattery) {
    OneUiCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DeviceTypeIcon(deviceType = device.deviceType, size = 42.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ConnectionStateBadge(state = device.connectionState)
                    ReadingQualityBadge(quality = device.capabilityStatus)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Large Circular Gauge
            BatteryCircularGauge(
                percentage = device.batteryPercentage,
                chargingState = device.chargingState,
                size = 180.dp,
                strokeWidth = 14.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Charging State label
            if (device.chargingState == ChargingState.CHARGING) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(BatteryGreen.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = BatteryGreen, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Currently Charging", color = BatteryGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Relative Freshness text
            Text(
                text = BatteryFormatting.formatRelativeTime(device.lastUpdated),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ControlsCard(
    device: DeviceBattery,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onToggleConnection: () -> Unit,
    onDeleteClick: () -> Unit
) {
    OneUiCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("Device Controls", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onRefresh,
                    enabled = !isRefreshing,
                    colors = ButtonDefaults.buttonColors(containerColor = OneUiBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Refresh")
                    }
                }

                OutlinedButton(
                    onClick = onToggleConnection,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    val isConnected = device.connectionState == ConnectionState.CONNECTED
                    Icon(
                        if (isConnected) Icons.Default.PowerOff else Icons.Default.Power,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isConnected) "Disconnect" else "Connect")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onDeleteClick,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = BatteryRed),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Remove Device")
            }
        }
    }
}

@Composable
private fun SpecificationsCard(device: DeviceBattery) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    OneUiCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("Specifications & Telemetry", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            SpecRow(label = "Device ID / MAC", value = device.deviceId, isMono = true)
            SpecRow(label = "Device Category", value = device.deviceType.displayName)
            SpecRow(label = "Connection Method", value = device.connectionMethod.name)
            SpecRow(label = "Charging State", value = device.chargingState.name)
            SpecRow(label = "Telemetry Quality", value = device.capabilityStatus.name)
            SpecRow(
                label = "Last Sync",
                value = dateFormat.format(Date(device.lastUpdated)),
                isMono = true
            )
            device.lastSuccessfulReading?.let {
                SpecRow(
                    label = "Last Successful Read",
                    value = dateFormat.format(Date(it)),
                    isMono = true
                )
            }
        }
    }
}

@Composable
private fun SpecRow(label: String, value: String, isMono: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            fontFamily = if (isMono) FontFamily.Monospace else FontFamily.Default,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun BatteryHistoryChartCard(history: List<BatteryHistoryItem>) {
    OneUiCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timeline, contentDescription = null, tint = OneUiBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("24-Hour Battery Trend", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }

                if (history.isNotEmpty()) {
                    Text(
                        "${history.minOfOrNull { it.percentage } ?: 0}% - ${history.maxOfOrNull { it.percentage } ?: 0}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (history.size < 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Not enough points to draw curve (requires at least 2 readings)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Line Graph using Compose Canvas
                val sortedReadings = remember(history) { history.sortedBy { it.timestamp } }
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    val width = size.width
                    val height = size.height

                    // Draw reference grid lines at 25%, 50%, 75%, 100%
                    listOf(0.25f, 0.5f, 0.75f).forEach { fraction ->
                        val y = height * (1f - fraction)
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.2f),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    val minTime = sortedReadings.first().timestamp
                    val maxTime = sortedReadings.last().timestamp
                    val timeSpan = (maxTime - minTime).coerceAtLeast(1L).toFloat()

                    val points = sortedReadings.map { reading ->
                        val x = ((reading.timestamp - minTime).toFloat() / timeSpan) * width
                        val y = height * (1f - (reading.percentage.toFloat() / 100f))
                        Offset(x, y)
                    }

                    // Stroke curve
                    val path = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            val prev = points[i - 1]
                            val curr = points[i]
                            val cx = (prev.x + curr.x) / 2
                            cubicTo(cx, prev.y, cx, curr.y, curr.x, curr.y)
                        }
                    }

                    // Gradient fill underneath
                    val fillPath = Path().apply {
                        addPath(path)
                        lineTo(points.last().x, height)
                        lineTo(points.first().x, height)
                        close()
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(OneUiBlue.copy(alpha = 0.35f), Color.Transparent),
                            startY = 0f,
                            endY = height
                        )
                    )

                    drawPath(
                        path = path,
                        color = OneUiBlue,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw dots on readings
                    points.forEach { pt ->
                        drawCircle(color = Color.White, radius = 4.dp.toPx(), center = pt)
                        drawCircle(color = OneUiBlue, radius = 2.5.dp.toPx(), center = pt)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadingHistoryItem(reading: BatteryHistoryItem) {
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                dateFormat.format(Date(reading.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )
            Text(
                reading.chargingState.name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val pct = reading.percentage
        val color = when {
            pct >= 50 -> BatteryGreen
            pct >= 20 -> BatteryAmber
            else -> BatteryRed
        }

        Text(
            text = "$pct%",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = color
        )
    }
}

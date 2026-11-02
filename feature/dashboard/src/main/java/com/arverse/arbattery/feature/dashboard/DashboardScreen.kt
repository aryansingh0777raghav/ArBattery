package com.arverse.arbattery.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arverse.arbattery.core.common.model.ChargingState
import com.arverse.arbattery.core.common.model.DeviceBattery
import com.arverse.arbattery.core.common.util.BatteryFormatting
import com.arverse.arbattery.core.designsystem.component.BatteryBar
import com.arverse.arbattery.core.designsystem.component.ConnectionStateBadge
import com.arverse.arbattery.core.designsystem.component.CraftedByPill
import com.arverse.arbattery.core.designsystem.component.CreatorProfileModal
import com.arverse.arbattery.core.designsystem.component.DeviceTypeIcon
import com.arverse.arbattery.core.designsystem.component.OneUiCard
import com.arverse.arbattery.core.designsystem.component.OneUiLargeHeader
import com.arverse.arbattery.core.designsystem.component.ReadingQualityBadge
import com.arverse.arbattery.core.designsystem.theme.BatteryAmber
import com.arverse.arbattery.core.designsystem.theme.BatteryGreen
import com.arverse.arbattery.core.designsystem.theme.BatteryGrey
import com.arverse.arbattery.core.designsystem.theme.OneUiBlue
import com.arverse.arbattery.core.designsystem.theme.PillShape

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onDeviceClick: (String) -> Unit,
    onAddDeviceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreatorModal by remember { mutableStateOf(false) }

    if (showCreatorModal) {
        CreatorProfileModal(onDismissRequest = { showCreatorModal = false })
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // One UI Header
        OneUiLargeHeader(
            title = "ArBattery",
            subtitle = "${uiState.connectedCount} of ${uiState.totalCount} devices connected",
            actions = {
                IconButton(onClick = { viewModel.refreshAll() }) {
                    if (uiState.isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                IconButton(onClick = onAddDeviceClick) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Device",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )

        // Summary Badges Row
        SummaryCardRow(
            total = uiState.totalCount,
            connected = uiState.connectedCount,
            lowBattery = uiState.lowBatteryCount
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Filter Pills
        FilterPillsRow(
            selectedFilter = uiState.selectedFilter,
            onFilterSelect = { viewModel.setFilter(it) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Devices List
        if (uiState.devices.isEmpty()) {
            EmptyDashboardState(
                onAddDeviceClick = onAddDeviceClick,
                onCreatorClick = { showCreatorModal = true }
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(uiState.devices, key = { it.deviceId }) { device ->
                    DeviceCard(
                        device = device,
                        onClick = { onDeviceClick(device.deviceId) }
                    )
                }

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CraftedByPill(
                            onClick = { showCreatorModal = true }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryCardRow(
    total: Int,
    connected: Int,
    lowBattery: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryTile(
            title = "Monitored",
            count = total,
            indicatorColor = OneUiBlue,
            modifier = Modifier.weight(1f)
        )
        SummaryTile(
            title = "Connected",
            count = connected,
            indicatorColor = BatteryGreen,
            modifier = Modifier.weight(1f)
        )
        SummaryTile(
            title = "Low Battery",
            count = lowBattery,
            indicatorColor = if (lowBattery > 0) BatteryAmber else BatteryGrey,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun SummaryTile(
    title: String,
    count: Int,
    indicatorColor: Color,
    modifier: Modifier = Modifier
) {
    OneUiCard(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(indicatorColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = count.toString(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun FilterPillsRow(
    selectedFilter: DashboardFilter,
    onFilterSelect: (DashboardFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(DashboardFilter.entries) { filter ->
            val isSelected = filter == selectedFilter
            val label = when (filter) {
                DashboardFilter.ALL -> "All"
                DashboardFilter.CONNECTED -> "Connected"
                DashboardFilter.LOW_BATTERY -> "Needs Attention"
                DashboardFilter.DISCONNECTED -> "Disconnected"
            }

            Surface(
                modifier = Modifier
                    .clip(PillShape)
                    .clickable { onFilterSelect(filter) },
                shape = PillShape,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DeviceCard(
    device: DeviceBattery,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = device.connectionState == com.arverse.arbattery.core.common.model.ConnectionState.CONNECTED
    val isCharging = device.chargingState == ChargingState.CHARGING

    OneUiCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DeviceTypeIcon(deviceType = device.deviceType, size = 48.dp)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.deviceName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${device.deviceType.displayName} • ${device.connectionMethod.label}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = device.displayPercentage,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isCharging) {
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = "Charging",
                                tint = BatteryGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Text(
                        text = BatteryFormatting.formatRelativeTime(device.lastUpdated),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Battery Bar
            BatteryBar(
                percentage = device.batteryPercentage,
                chargingState = device.chargingState,
                isConnected = isConnected,
                height = 7.dp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ConnectionStateBadge(state = device.connectionState)
                ReadingQualityBadge(quality = device.capabilityStatus)
            }
        }
    }
}

@Composable
fun EmptyDashboardState(
    onAddDeviceClick: () -> Unit,
    onCreatorClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "No Devices Connected",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Add your Hammer Ace smartwatch, tablet companion, or Bluetooth headphones to start monitoring battery levels.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Surface(
                modifier = Modifier
                    .clip(PillShape)
                    .clickable(onClick = onAddDeviceClick),
                shape = PillShape,
                color = MaterialTheme.colorScheme.primary
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add Device",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            CraftedByPill(
                onClick = onCreatorClick
            )
        }
    }
}

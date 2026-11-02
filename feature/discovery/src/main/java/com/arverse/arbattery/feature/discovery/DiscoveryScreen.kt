package com.arverse.arbattery.feature.discovery

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TabletAndroid
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arverse.arbattery.core.bluetooth.diagnostic.DiagnosticProgress
import com.arverse.arbattery.core.bluetooth.model.BleScanDevice
import com.arverse.arbattery.core.bluetooth.model.DiagnosticReport
import com.arverse.arbattery.core.common.model.DeviceType
import com.arverse.arbattery.core.designsystem.component.DeviceTypeIcon
import com.arverse.arbattery.core.designsystem.component.OneUiCard
import com.arverse.arbattery.core.designsystem.component.OneUiLargeHeader
import com.arverse.arbattery.core.designsystem.theme.BatteryAmber
import com.arverse.arbattery.core.designsystem.theme.BatteryGreen
import com.arverse.arbattery.core.designsystem.theme.BatteryRed
import com.arverse.arbattery.core.designsystem.theme.OneUiBlue

private data class VerdictStyle(val color: Color, val text: String)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryScreen(
    viewModel: DiscoveryViewModel,
    onNavigateBack: () -> Unit,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.pairingSuccessMessage) {
        uiState.pairingSuccessMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearPairingSuccessMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            OneUiLargeHeader(
                title = "Add Device",
                subtitle = "Discover Bluetooth, Tablet Companion, and Smartwatches",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            PrimaryTabRow(
                selectedTabIndex = uiState.selectedTab.ordinal,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = uiState.selectedTab == DiscoveryTab.BLUETOOTH,
                    onClick = { viewModel.selectTab(DiscoveryTab.BLUETOOTH) },
                    text = { Text("Bluetooth") },
                    icon = { Icon(Icons.Default.Bluetooth, contentDescription = null) }
                )
                Tab(
                    selected = uiState.selectedTab == DiscoveryTab.COMPANION_WIFI,
                    onClick = { viewModel.selectTab(DiscoveryTab.COMPANION_WIFI) },
                    text = { Text("Companion") },
                    icon = { Icon(Icons.Default.Wifi, contentDescription = null) }
                )
                Tab(
                    selected = uiState.selectedTab == DiscoveryTab.HAMMER_ACE_DIAGNOSTIC,
                    onClick = { viewModel.selectTab(DiscoveryTab.HAMMER_ACE_DIAGNOSTIC) },
                    text = { Text("Hammer Ace") },
                    icon = { Icon(Icons.Default.Watch, contentDescription = null) }
                )
            }

            when (uiState.selectedTab) {
                DiscoveryTab.BLUETOOTH -> {
                    BluetoothScanTab(
                        uiState = uiState,
                        discoveredDevices = discoveredDevices,
                        onStartScan = { viewModel.startBleScan() },
                        onStopScan = { viewModel.stopBleScan() },
                        onSearchChange = { viewModel.updateSearchQuery(it) },
                        onToggleHideUnnamed = { viewModel.toggleHideUnnamedDevices(it) },
                        onRequestPermissions = onRequestPermissions,
                        onAddDevice = { device, type -> viewModel.addDiscoveredDevice(device, type) },
                        onRunDiagnostic = { address, name ->
                            viewModel.selectTab(DiscoveryTab.HAMMER_ACE_DIAGNOSTIC)
                            viewModel.runHammerAceDiagnostic(address, name)
                        }
                    )
                }
                DiscoveryTab.COMPANION_WIFI -> {
                    CompanionWifiTab(
                        uiState = uiState,
                        onRefresh = { viewModel.refreshHubCompanionInfo() }
                    )
                }
                DiscoveryTab.HAMMER_ACE_DIAGNOSTIC -> {
                    HammerAceDiagnosticTab(
                        uiState = uiState,
                        onAddressChange = { viewModel.setDiagnosticTargetAddress(it) },
                        onRunTest = { viewModel.runHammerAceDiagnostic() }
                    )
                }
            }
        }
    }
}

@Composable
private fun BluetoothScanTab(
    uiState: DiscoveryUiState,
    discoveredDevices: List<BleScanDevice>,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onSearchChange: (String) -> Unit,
    onToggleHideUnnamed: (Boolean) -> Unit,
    onRequestPermissions: () -> Unit,
    onAddDevice: (BleScanDevice, DeviceType) -> Unit,
    onRunDiagnostic: (String, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            if (!uiState.hasBluetoothPermissions) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = BatteryAmber.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = BatteryAmber)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Bluetooth Permissions Required",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Android requires Bluetooth Scan & Connect permissions to discover nearby battery devices.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onRequestPermissions,
                            colors = ButtonDefaults.buttonColors(containerColor = OneUiBlue)
                        ) {
                            Text("Grant Permissions")
                        }
                    }
                }
            }

            if (!uiState.isBluetoothEnabled) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = BatteryRed.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = BatteryRed)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Bluetooth is turned off. Please turn on Bluetooth in device Settings to scan.",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchChange,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    placeholder = { Text("Filter devices or MAC...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (uiState.isScanning) onStopScan() else onStartScan()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isScanning) MaterialTheme.colorScheme.error else OneUiBlue
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (uiState.isScanning) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stop")
                    } else {
                        Icon(Icons.Default.BluetoothSearching, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scan")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FilterChip(
                    selected = uiState.hideUnnamedDevices,
                    onClick = { onToggleHideUnnamed(!uiState.hideUnnamedDevices) },
                    label = { Text(if (uiState.hideUnnamedDevices) "Hiding Unnamed" else "Showing All Devices") },
                    leadingIcon = {
                        Icon(
                            if (uiState.hideUnnamedDevices) Icons.Default.FilterList else Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )

                Text(
                    text = "${discoveredDevices.size} found",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (discoveredDevices.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.BluetoothSearching,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (uiState.isScanning) "Scanning for nearby BLE devices..." else "No devices discovered yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (uiState.isScanning) "Ensure your smartwatch, earbuds, or tablet are in pairing mode" else "Tap 'Scan' above to start searching",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        } else {
            items(discoveredDevices, key = { it.address }) { device ->
                BleDeviceItemCard(
                    device = device,
                    onAdd = { type -> onAddDevice(device, type) },
                    onDiagnostic = { onRunDiagnostic(device.address, device.name ?: "Unknown Device") }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BleDeviceItemCard(
    device: BleScanDevice,
    onAdd: (DeviceType) -> Unit,
    onDiagnostic: () -> Unit
) {
    var showTypeMenu by remember { mutableStateOf(false) }

    OneUiCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DeviceTypeIcon(
                    deviceType = device.deviceType,
                    size = 46.dp
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.name.ifBlank { "Unnamed Device" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = device.address,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                RssiBadge(rssi = device.rssi)
            }

            Spacer(modifier = Modifier.height(10.dp))

            val hasBatteryService = device.advertisedServiceUuids.any { it.contains("180f", ignoreCase = true) }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (hasBatteryService) {
                    Box(
                        modifier = Modifier
                            .background(BatteryGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Standard Battery Service (0x180F)",
                            color = BatteryGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (device.advertisedServiceUuids.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${device.advertisedServiceUuids.size} Advertised UUIDs",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isSmartwatchCandidate = device.deviceType == DeviceType.SMARTWATCH ||
                        device.name.contains("Ace", ignoreCase = true) ||
                        device.name.contains("Hammer", ignoreCase = true)

                if (isSmartwatchCandidate) {
                    OutlinedButton(
                        onClick = onDiagnostic,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Watch, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Test 0x180F")
                    }
                }

                Box {
                    Button(
                        onClick = { showTypeMenu = true },
                        colors = ButtonDefaults.buttonColors(containerColor = OneUiBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Connect")
                    }

                    DropdownMenu(
                        expanded = showTypeMenu,
                        onDismissRequest = { showTypeMenu = false }
                    ) {
                        Text(
                            "Select Device Type:",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        DropdownMenuItem(
                            text = { Text("Smartwatch (Hammer Ace, etc.)") },
                            onClick = {
                                showTypeMenu = false
                                onAdd(DeviceType.SMARTWATCH)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Bluetooth Earbuds") },
                            onClick = {
                                showTypeMenu = false
                                onAdd(DeviceType.EARBUDS)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Bluetooth Headphones") },
                            onClick = {
                                showTypeMenu = false
                                onAdd(DeviceType.HEADPHONES)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Other Bluetooth Accessory") },
                            onClick = {
                                showTypeMenu = false
                                onAdd(DeviceType.ACCESSORY)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RssiBadge(rssi: Int) {
    val (label, color) = when {
        rssi >= -65 -> "Strong" to BatteryGreen
        rssi >= -80 -> "Good" to OneUiBlue
        rssi >= -90 -> "Fair" to BatteryAmber
        else -> "Weak" to BatteryRed
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = "$rssi dBm ($label)",
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CompanionWifiTab(
    uiState: DiscoveryUiState,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            OneUiCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DeviceTypeIcon(deviceType = DeviceType.TABLET, size = 48.dp)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                "Samsung Galaxy Tab A 8.0",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Companion Synchronization Setup",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        "The ArBattery Companion app runs directly on your Galaxy Tab A 8.0 (Android 9-11) and securely pushes live battery metrics to this phone over local Wi-Fi.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            OneUiCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Local Hub Connection Info",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Hub Info")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    DetailRow(
                        label = "Hub IP Address",
                        value = uiState.hubLocalIp,
                        onCopy = {
                            clipboard.setPrimaryClip(ClipData.newPlainText("Hub IP", uiState.hubLocalIp))
                            Toast.makeText(context, "Copied IP to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    DetailRow(
                        label = "Listening Port",
                        value = uiState.hubPort.toString(),
                        onCopy = {
                            clipboard.setPrimaryClip(ClipData.newPlainText("Hub Port", uiState.hubPort.toString()))
                            Toast.makeText(context, "Copied Port to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    DetailRow(
                        label = "Pairing Auth Token",
                        value = if (uiState.hubPairingToken.length > 12) uiState.hubPairingToken.take(12) + "..." else uiState.hubPairingToken,
                        onCopy = {
                            clipboard.setPrimaryClip(ClipData.newPlainText("Pairing Token", uiState.hubPairingToken))
                            Toast.makeText(context, "Copied Token to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }

        item {
            OneUiCard(modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "QR Code Pairing Payload",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Scan this from the Companion app or copy the payload string to connect:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = uiState.hubPairingQrPayload,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                clipboard.setPrimaryClip(ClipData.newPlainText("Pairing URL", uiState.hubPairingQrPayload))
                                Toast.makeText(context, "Pairing URL copied", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy URL")
                        }

                        Button(
                            onClick = {
                                val sendIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, uiState.hubPairingQrPayload)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Pairing Info"))
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = OneUiBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share")
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
        }
        IconButton(onClick = onCopy) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy $label", modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun HammerAceDiagnosticTab(
    uiState: DiscoveryUiState,
    onAddressChange: (String) -> Unit,
    onRunTest: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            OneUiCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DeviceTypeIcon(deviceType = DeviceType.SMARTWATCH, size = 48.dp)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                "Hammer Ace Smartwatch",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "BLE Battery Profile Diagnostic Suite",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "The Hammer Ace smartwatch is an unverified BLE device. This diagnostic probes its GATT table for standard Battery Service (0x180F) and Battery Level characteristic (0x2A19). If proprietary, a full GATT inspection report is generated for protocol adapter integration.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            OneUiCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        "Target Device Address",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = uiState.diagnosticTargetAddress,
                            onValueChange = onAddressChange,
                            placeholder = { Text("e.g. AA:BB:CC:11:22:33") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        val isRunning = uiState.diagnosticProgress !is DiagnosticProgress.Idle &&
                                uiState.diagnosticProgress !is DiagnosticProgress.Completed &&
                                uiState.diagnosticProgress !is DiagnosticProgress.Failed

                        Button(
                            onClick = onRunTest,
                            enabled = !isRunning && uiState.diagnosticTargetAddress.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = OneUiBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isRunning) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                            } else {
                                Text("Run Test")
                            }
                        }
                    }
                }
            }
        }

        if (uiState.diagnosticProgress !is DiagnosticProgress.Idle) {
            item {
                DiagnosticProgressCard(progress = uiState.diagnosticProgress)
            }
        }

        uiState.lastDiagnosticReport?.let { report ->
            item {
                DiagnosticReportCard(
                    report = report,
                    onExportJson = {
                        val json = report.exportAsJson()
                        clipboard.setPrimaryClip(ClipData.newPlainText("Diagnostic JSON", json))
                        Toast.makeText(context, "Copied JSON report to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    onExportText = {
                        val text = report.exportAsText()
                        clipboard.setPrimaryClip(ClipData.newPlainText("Diagnostic Text", text))
                        Toast.makeText(context, "Copied text report to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    onShare = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, report.exportAsText())
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Diagnostic Report"))
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DiagnosticProgressCard(progress: DiagnosticProgress) {
    OneUiCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (progress) {
                is DiagnosticProgress.Connecting -> {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp, color = OneUiBlue)
                    Spacer(modifier = Modifier.width(14.dp))
                    Text("Connecting to ${progress.address} via GATT...", style = MaterialTheme.typography.bodyMedium)
                }
                is DiagnosticProgress.DiscoveringServices -> {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp, color = OneUiBlue)
                    Spacer(modifier = Modifier.width(14.dp))
                    Text("Discovering GATT primary services...", style = MaterialTheme.typography.bodyMedium)
                }
                is DiagnosticProgress.ProbingBatteryService -> {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp, color = OneUiBlue)
                    Spacer(modifier = Modifier.width(14.dp))
                    Text("Inspecting Battery Service (0x180F)...", style = MaterialTheme.typography.bodyMedium)
                }
                is DiagnosticProgress.ReadingBatteryLevel -> {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp, color = OneUiBlue)
                    Spacer(modifier = Modifier.width(14.dp))
                    Text("Reading characteristic 0x2A19 byte value...", style = MaterialTheme.typography.bodyMedium)
                }
                is DiagnosticProgress.Completed -> {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BatteryGreen, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(14.dp))
                    Text("Diagnostic completed successfully", fontWeight = FontWeight.Bold, color = BatteryGreen)
                }
                is DiagnosticProgress.Failed -> {
                    Icon(Icons.Default.Error, contentDescription = null, tint = BatteryRed, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(14.dp))
                    Text("Diagnostic halted: ${progress.reason}", fontWeight = FontWeight.Bold, color = BatteryRed)
                }
                DiagnosticProgress.Idle -> {}
            }
        }
    }
}

@Composable
private fun DiagnosticReportCard(
    report: DiagnosticReport,
    onExportJson: () -> Unit,
    onExportText: () -> Unit,
    onShare: () -> Unit
) {
    OneUiCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Diagnostic Inspection Result",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                val verdictStyle = when (report.compatibilityVerdict) {
                    DiagnosticReport.CompatibilityVerdict.COMPATIBLE_STANDARD_BLE ->
                        VerdictStyle(BatteryGreen, "COMPATIBLE")
                    DiagnosticReport.CompatibilityVerdict.UNSUPPORTED_STANDARD_SERVICE_MISSING ->
                        VerdictStyle(BatteryAmber, "CUSTOM PROFILE")
                    DiagnosticReport.CompatibilityVerdict.UNSUPPORTED_BATTERY_CHAR_MISSING ->
                        VerdictStyle(BatteryAmber, "CHAR MISSING")
                    DiagnosticReport.CompatibilityVerdict.CONNECTION_FAILED ->
                        VerdictStyle(BatteryRed, "FAILED")
                    DiagnosticReport.CompatibilityVerdict.READ_FAILED ->
                        VerdictStyle(BatteryRed, "READ FAIL")
                }

                Box(
                    modifier = Modifier
                        .background(verdictStyle.color.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(verdictStyle.text, color = verdictStyle.color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                report.diagnosticSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

            DiagnosticCheckRow(
                label = "Standard Battery Service (0x180F)",
                passed = report.isStandardBatteryServiceFound
            )
            DiagnosticCheckRow(
                label = "Battery Level Characteristic (0x2A19)",
                passed = report.isBatteryLevelCharacteristicFound
            )
            DiagnosticCheckRow(
                label = "Readable (PROPERTY_READ)",
                passed = report.batteryCharacteristicCanRead
            )
            DiagnosticCheckRow(
                label = "Notification Support (CCCD 0x2902)",
                passed = report.batteryCharacteristicCanNotify
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Battery Reading Value:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = report.batteryValueRead?.let { "$it%" } ?: "None (--)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (report.batteryValueRead != null) BatteryGreen else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Total GATT Services Discovered:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "${report.totalServicesCount} services (${report.totalCharacteristicsCount} chars)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Discovered Services & Characteristics:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (report.services.isEmpty()) {
                    Text("No services retrieved", style = MaterialTheme.typography.bodySmall)
                } else {
                    report.services.forEach { svc ->
                        Text(
                            text = "[Service] ${svc.serviceName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = OneUiBlue
                        )
                        Text(
                            text = "  UUID: ${svc.uuid}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        svc.characteristics.forEach { ch ->
                            Text(
                                text = "    • Char: ${ch.uuid.take(8)}... Props: [${ch.propertyNames.joinToString()}]",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onExportJson,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("JSON")
                }
                OutlinedButton(
                    onClick = onExportText,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Text")
                }
                Button(
                    onClick = onShare,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = OneUiBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share")
                }
            }
        }
    }
}

@Composable
private fun DiagnosticCheckRow(label: String, passed: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (passed) Icons.Default.CheckCircle else Icons.Default.Error,
            contentDescription = null,
            tint = if (passed) BatteryGreen else BatteryAmber,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = if (passed) "PRESENT" else "NOT FOUND",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (passed) BatteryGreen else BatteryAmber
        )
    }
}

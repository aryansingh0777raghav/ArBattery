package com.arverse.arbattery.companion

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TabletAndroid
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arverse.arbattery.core.common.model.ChargingState
import com.arverse.arbattery.core.designsystem.component.BatteryCircularGauge
import com.arverse.arbattery.core.designsystem.component.CraftedByPill
import com.arverse.arbattery.core.designsystem.component.CreatorProfileModal
import com.arverse.arbattery.core.designsystem.component.OneUiCard
import com.arverse.arbattery.core.designsystem.component.OneUiLargeHeader
import com.arverse.arbattery.core.designsystem.theme.BatteryAmber
import com.arverse.arbattery.core.designsystem.theme.BatteryGreen
import com.arverse.arbattery.core.designsystem.theme.BatteryRed
import com.arverse.arbattery.core.designsystem.theme.OneUiBlue
import com.arverse.arbattery.core.network.util.NetworkUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CompanionScreen(
    viewModel: CompanionViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val clipboard = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
    var showCreatorModal by remember { mutableStateOf(false) }

    if (showCreatorModal) {
        CreatorProfileModal(onDismissRequest = { showCreatorModal = false })
    }

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
                title = "ArBattery Companion",
                subtitle = "Samsung Galaxy Tab A 8.0 Telemetry Node"
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Battery Gauge Hero
            item {
                Spacer(modifier = Modifier.height(4.dp))
                uiState.batteryInfo?.let { battery ->
                    HeroBatteryCard(battery = battery)
                }
            }

            // Connection & Sync Section
            item {
                if (uiState.isPaired) {
                    PairedStatusCard(
                        uiState = uiState,
                        onSyncNow = { viewModel.syncNow() },
                        onUnpair = { viewModel.unpair() }
                    )
                } else {
                    PairingSetupCard(
                        uiState = uiState,
                        onIpChange = { viewModel.updateHubIp(it) },
                        onPortChange = { viewModel.updateHubPort(it) },
                        onTokenChange = { viewModel.updateAuthToken(it) },
                        onAutoDetect = { viewModel.autoDetectHub() },
                        onPasteFromClipboard = {
                            val clip = clipboard.primaryClip
                            if (clip != null && clip.itemCount > 0) {
                                val text = clip.getItemAt(0).text?.toString() ?: ""
                                viewModel.parseQrPayload(text)
                            }
                        },
                        onConnect = { viewModel.pairWithHub() }
                    )
                }
            }

            // Tablet Hardware & Battery Specs Card
            item {
                uiState.batteryInfo?.let { battery ->
                    TelemetryDetailsCard(battery = battery, deviceId = uiState.deviceId)
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CraftedByPill(
                        onClick = { showCreatorModal = true }
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun HeroBatteryCard(battery: DetailedBatteryInfo) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(OneUiBlue.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.TabletAndroid, contentDescription = null, tint = OneUiBlue, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = battery.deviceDisplayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Charging Badge
                if (battery.chargingState == ChargingState.CHARGING || battery.chargingState == ChargingState.FULL) {
                    Box(
                        modifier = Modifier
                            .background(BatteryGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = BatteryGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (battery.chargingState == ChargingState.FULL) "Full" else "Charging",
                                color = BatteryGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Gauge
            BatteryCircularGauge(
                percentage = battery.percentage,
                chargingState = battery.chargingState,
                size = 170.dp,
                strokeWidth = 14.dp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "${battery.powerSource} • ${battery.health} Health",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PairedStatusCard(
    uiState: CompanionUiState,
    onSyncNow: () -> Unit,
    onUnpair: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    OneUiCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BatteryGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Paired with Phone Hub", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }

                Box(
                    modifier = Modifier
                        .background(BatteryGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("ACTIVE", color = BatteryGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Hub IP Endpoint:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${uiState.hubIp}:${uiState.hubPort}", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Last Telemetry Push:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = uiState.lastSyncTime?.let { dateFormat.format(Date(it)) } ?: "Pending first sync",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onSyncNow,
                    enabled = !uiState.isSyncing,
                    colors = ButtonDefaults.buttonColors(containerColor = OneUiBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (uiState.isSyncing) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Syncing...")
                    } else {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sync Now")
                    }
                }

                OutlinedButton(
                    onClick = onUnpair,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BatteryRed)
                ) {
                    Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Unpair")
                }
            }
        }
    }
}

@Composable
private fun PairingSetupCard(
    uiState: CompanionUiState,
    onIpChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onTokenChange: (String) -> Unit,
    onAutoDetect: () -> Unit,
    onPasteFromClipboard: () -> Unit,
    onConnect: () -> Unit
) {
    OneUiCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Wifi, contentDescription = null, tint = OneUiBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pair with ArBattery Phone Hub", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Enter the connection IP and Auth Token shown on your Samsung smartphone in ArBattery > Add Device > Companion:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = uiState.hubIp,
                onValueChange = onIpChange,
                label = { Text("Hub IP Address") },
                placeholder = { Text("e.g. 192.168.1.105") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            if (NetworkUtils.isCellularOrSpecialIp(uiState.hubIp)) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BatteryAmber.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = BatteryAmber, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Notice: ${uiState.hubIp} is a mobile carrier IP (464XLAT). Connect to the same Wi-Fi and use your phone's Wi-Fi IP (e.g. 192.168.x.x) or tap 'Auto-Detect'.",
                        color = BatteryAmber,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = uiState.hubPort.toString(),
                    onValueChange = onPortChange,
                    label = { Text("Port") },
                    modifier = Modifier.width(110.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = uiState.authToken,
                    onValueChange = onTokenChange,
                    label = { Text("Auth Token / PIN") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onAutoDetect,
                    enabled = !uiState.isPairing,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Auto-Detect Hub")
                }

                OutlinedButton(
                    onClick = onPasteFromClipboard,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Paste Link")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onConnect,
                enabled = !uiState.isPairing && uiState.hubIp.isNotBlank() && uiState.authToken.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = OneUiBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isPairing) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Connecting...")
                } else {
                    Text("Connect to Hub")
                }
            }
        }
    }
}

@Composable
private fun TelemetryDetailsCard(battery: DetailedBatteryInfo, deviceId: String) {
    OneUiCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("Hardware & Battery Diagnostics", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))

            DetailItem("Device Identity", deviceId, isMono = true)
            DetailItem("Hardware Model", "${battery.deviceManufacturer} ${battery.deviceModel}")
            DetailItem("Power Source", battery.powerSource)
            DetailItem("Battery Health", battery.health)
            DetailItem("Voltage", "${battery.voltageMv} mV", isMono = true)
            DetailItem("Temperature", "${String.format("%.1f", battery.temperatureCelsius)} °C", isMono = true)
            battery.technology?.let {
                DetailItem("Battery Chemistry", it)
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String, isMono: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
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

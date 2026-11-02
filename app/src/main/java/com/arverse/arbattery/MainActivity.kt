package com.arverse.arbattery

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.arverse.arbattery.core.bluetooth.BluetoothPermissionHelper
import com.arverse.arbattery.core.designsystem.theme.ArBatteryTheme
import com.arverse.arbattery.feature.dashboard.DashboardScreen
import com.arverse.arbattery.feature.dashboard.DashboardViewModel
import com.arverse.arbattery.feature.device_details.DeviceDetailsScreen
import com.arverse.arbattery.feature.device_details.DeviceDetailsViewModel
import com.arverse.arbattery.feature.discovery.DiscoveryScreen
import com.arverse.arbattery.feature.discovery.DiscoveryViewModel
import com.arverse.arbattery.feature.history.HistoryScreen
import com.arverse.arbattery.feature.history.HistoryViewModel
import com.arverse.arbattery.feature.pairing.PairingScreen
import com.arverse.arbattery.feature.pairing.PairingViewModel
import com.arverse.arbattery.feature.settings.SettingsScreen
import com.arverse.arbattery.feature.settings.SettingsViewModel

sealed class AppDestination(val route: String, val title: String, val icon: ImageVector) {
    data object Dashboard : AppDestination("dashboard", "Dashboard", Icons.Default.Dashboard)
    data object Discovery : AppDestination("discovery", "Add Device", Icons.Default.AddCircleOutline)
    data object History : AppDestination("history", "History", Icons.Default.History)
    data object Settings : AppDestination("settings", "Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = ArBatteryApp.instance

        setContent {
            ArBatteryTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { _ -> }

                fun requestRequiredPermissions() {
                    val permissions = BluetoothPermissionHelper.getRequiredBluetoothPermissions().toMutableList()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                    permissionLauncher.launch(permissions.toTypedArray())
                }

                LaunchedEffect(Unit) {
                    if (!BluetoothPermissionHelper.hasBluetoothPermissions(this@MainActivity)) {
                        requestRequiredPermissions()
                    }
                }

                val bottomDestinations = listOf(
                    AppDestination.Dashboard,
                    AppDestination.Discovery,
                    AppDestination.History,
                    AppDestination.Settings
                )

                val showBottomBar = currentRoute in bottomDestinations.map { it.route }

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar {
                                bottomDestinations.forEach { dest ->
                                    val isSelected = currentRoute == dest.route
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = {
                                            navController.navigate(dest.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = { Icon(dest.icon, contentDescription = dest.title) },
                                        label = { Text(dest.title) }
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = AppDestination.Dashboard.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(AppDestination.Dashboard.route) {
                            val viewModel = remember {
                                DashboardViewModel(
                                    batteryManager = app.batteryManager
                                )
                            }
                            DashboardScreen(
                                viewModel = viewModel,
                                onDeviceClick = { deviceId ->
                                    navController.navigate("device_details/$deviceId")
                                },
                                onAddDeviceClick = {
                                    navController.navigate(AppDestination.Discovery.route)
                                }
                            )
                        }

                        composable(AppDestination.Discovery.route) {
                            val viewModel = remember {
                                DiscoveryViewModel(
                                    bluetoothScanner = app.bluetoothScanner,
                                    connectionManager = app.connectionManager,
                                    hammerAceDiagnosticScanner = app.hammerAceDiagnosticScanner,
                                    deviceRepository = app.deviceRepository,
                                    batteryManager = app.batteryManager,
                                    appContext = applicationContext
                                )
                            }
                            DiscoveryScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onRequestPermissions = { requestRequiredPermissions() }
                            )
                        }

                        composable(
                            route = "device_details/{deviceId}",
                            arguments = listOf(navArgument("deviceId") { type = NavType.StringType })
                        ) { backStack ->
                            val deviceId = backStack.arguments?.getString("deviceId") ?: ""
                            val viewModel = remember(deviceId) {
                                DeviceDetailsViewModel(
                                    deviceId = deviceId,
                                    deviceRepository = app.deviceRepository,
                                    historyRepository = app.historyRepository,
                                    batteryManager = app.batteryManager,
                                    connectionManager = app.connectionManager
                                )
                            }
                            DeviceDetailsScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("pairing") {
                            val viewModel = remember {
                                PairingViewModel(
                                    pairingRepository = app.pairingRepository,
                                    appContext = applicationContext
                                )
                            }
                            PairingScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(AppDestination.History.route) {
                            val viewModel = remember {
                                HistoryViewModel(
                                    deviceRepository = app.deviceRepository,
                                    historyRepository = app.historyRepository
                                )
                            }
                            HistoryScreen(
                                viewModel = viewModel,
                                onNavigateBack = null
                            )
                        }

                        composable(AppDestination.Settings.route) {
                            val viewModel = remember {
                                SettingsViewModel(
                                    appContext = applicationContext
                                )
                            }
                            SettingsScreen(
                                viewModel = viewModel,
                                onNavigateBack = null
                            )
                        }
                    }
                }
            }
        }
    }
}

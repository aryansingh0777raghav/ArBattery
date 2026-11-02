package com.arverse.arbattery

import android.app.Application
import com.arverse.arbattery.core.bluetooth.diagnostic.HammerAceDiagnosticScanner
import com.arverse.arbattery.core.bluetooth.gatt.AndroidBluetoothConnectionManager
import com.arverse.arbattery.core.bluetooth.gatt.BluetoothConnectionManager
import com.arverse.arbattery.core.bluetooth.reader.StandardBleBatteryReader
import com.arverse.arbattery.core.bluetooth.scanner.AndroidBluetoothScanner
import com.arverse.arbattery.core.bluetooth.scanner.BluetoothScanner
import com.arverse.arbattery.core.common.dispatcher.DefaultDispatcherProvider
import com.arverse.arbattery.core.common.dispatcher.DispatcherProvider
import com.arverse.arbattery.core.common.model.ConnectionMethod
import com.arverse.arbattery.core.common.model.DeviceBattery
import com.arverse.arbattery.core.database.ArBatteryDatabase
import com.arverse.arbattery.core.database.repository.BatteryHistoryRepository
import com.arverse.arbattery.core.database.repository.DefaultBatteryHistoryRepository
import com.arverse.arbattery.core.database.repository.DefaultDeviceRepository
import com.arverse.arbattery.core.database.repository.DefaultPairingRepository
import com.arverse.arbattery.core.database.repository.DeviceRepository
import com.arverse.arbattery.core.database.repository.PairingRepository
import com.arverse.arbattery.core.device.integration.AndroidCompanionIntegration
import com.arverse.arbattery.core.device.integration.StandardBleBatteryIntegration
import com.arverse.arbattery.core.device.integration.WindowsAgentIntegration
import com.arverse.arbattery.core.device.manager.BatteryAlertListener
import com.arverse.arbattery.core.device.manager.UniversalBatteryManager
import com.arverse.arbattery.core.network.client.CompanionApiClient
import com.arverse.arbattery.core.network.server.HubEmbeddedServer
import com.arverse.arbattery.notification.BatteryNotificationManager
import com.arverse.arbattery.widget.WidgetUpdateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class ArBatteryApp : Application() {

    lateinit var dispatchers: DispatcherProvider
        private set
    lateinit var database: ArBatteryDatabase
        private set
    lateinit var deviceRepository: DeviceRepository
        private set
    lateinit var historyRepository: BatteryHistoryRepository
        private set
    lateinit var pairingRepository: PairingRepository
        private set

    lateinit var connectionManager: BluetoothConnectionManager
        private set
    lateinit var bluetoothScanner: BluetoothScanner
        private set
    lateinit var bleBatteryReader: StandardBleBatteryReader
        private set
    lateinit var hammerAceDiagnosticScanner: HammerAceDiagnosticScanner
        private set

    lateinit var batteryManager: UniversalBatteryManager
        private set
    lateinit var notificationManager: BatteryNotificationManager
        private set
    lateinit var hubServer: HubEmbeddedServer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        instance = this

        dispatchers = DefaultDispatcherProvider()
        database = ArBatteryDatabase.getInstance(this)

        deviceRepository = DefaultDeviceRepository(database.deviceDao(), dispatchers)
        historyRepository = DefaultBatteryHistoryRepository(database.batteryReadingDao(), dispatchers)
        pairingRepository = DefaultPairingRepository(database.pairingDao(), dispatchers)

        connectionManager = AndroidBluetoothConnectionManager(this, dispatchers)
        bluetoothScanner = AndroidBluetoothScanner(this, dispatchers)
        bleBatteryReader = StandardBleBatteryReader(connectionManager)
        hammerAceDiagnosticScanner = HammerAceDiagnosticScanner(connectionManager)

        val integrations = mapOf<ConnectionMethod, com.arverse.arbattery.core.device.integration.DeviceIntegration>(
            ConnectionMethod.BLE to StandardBleBatteryIntegration(this, bleBatteryReader),
            ConnectionMethod.COMPANION to AndroidCompanionIntegration(deviceRepository),
            ConnectionMethod.WIFI to WindowsAgentIntegration(deviceRepository)
        )

        batteryManager = UniversalBatteryManager(
            deviceRepository = deviceRepository,
            historyRepository = historyRepository,
            integrations = integrations,
            dispatchers = dispatchers
        )

        notificationManager = BatteryNotificationManager(this)

        // Setup alert threshold notifications
        batteryManager.setAlertListener(object : BatteryAlertListener {
            override fun onLowBattery(device: DeviceBattery, percentage: Int) {
                notificationManager.handleBatteryUpdate(device, lowThreshold = 20, criticalThreshold = 10)
            }

            override fun onCriticalBattery(device: DeviceBattery, percentage: Int) {
                notificationManager.handleBatteryUpdate(device, lowThreshold = 20, criticalThreshold = 10)
            }
        })

        // Start Ktor Hub Embedded Server
        hubServer = HubEmbeddedServer(
            deviceRepository = deviceRepository,
            pairingRepository = pairingRepository,
            historyRepository = historyRepository
        )
        applicationScope.launch(dispatchers.io) {
            try {
                hubServer.start(port = 8443)
            } catch (_: Exception) {
            }
        }

        // Schedule periodic widget updates & synchronize on DB updates
        WidgetUpdateManager.schedulePeriodicUpdates(this)
        applicationScope.launch(dispatchers.io) {
            deviceRepository.getAllDevicesFlow()
                .distinctUntilChanged()
                .collect {
                    WidgetUpdateManager.updateAllWidgets(this@ArBatteryApp)
                }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        applicationScope.launch(dispatchers.io) {
            hubServer.stop()
        }
    }

    companion object {
        lateinit var instance: ArBatteryApp
            private set
    }
}

# ArBattery Testing Strategy & Verification Guide

## 1. Overview & Testing Pyramid

ArBattery utilizes a multi-tiered verification methodology designed to validate low-level BLE hardware interactions, embedded HTTP networking, local persistence, Compose UI states, and Glance home-screen widgets.

```
       ▲
      / \     E2E Hardware Verification (Galaxy A36 + Tab A 8.0 + Hammer Ace)
     /───\    ─────────────────────────────────────────────────────────────
    /     \   Android Instrumentation & Compose UI Tests (Robolectric / Espresso)
   /───────\  ─────────────────────────────────────────────────────────────
  /         \ Unit Tests (JUnit 4, MockK, Coroutines Test, Room In-Memory)
 ─────────────
```

---

## 2. Automated Unit Tests

Automated tests are distributed across modules, validating core business logic in isolation without requiring physical hardware:

### Key Test Suites

| Module | Test Class | Validated Behaviors |
| :--- | :--- | :--- |
| **`:core:common`** | `DeviceBatteryTest` | Validates percentage boundaries (`0..100`), freshness computation, and formatted time elapsed strings. |
| **`:core:bluetooth`** | `StandardBleBatteryReaderTest` | Validates unsigned byte parsing from characteristic `0x2A19`, out-of-bounds rejection, and CCCD descriptor configuration. |
| **`:core:bluetooth`** | `HammerAceDiagnosticScannerTest`| Verifies GATT service walk, standard Battery Service detection, and diagnostic report generation. |
| **`:core:database`** | `DeviceDaoTest` | Verifies Room SQLite transactions, unique constraints, and cascade deletion of historical battery readings. |
| **`:core:network`** | `CryptoTokenManagerTest` | Tests cryptographic token generation, SHA-256 hashing, and constant-time token comparison. |
| **`:core:network`** | `NetworkUtilsTest` | Validates IPv4 subnet detection and local IP formatting. |
| **`:core:device`** | `BatteryFreshnessEvaluatorTest` | Tests transitions between `LIVE`, `RECENT`, `STALE`, and `UNAVAILABLE`. |

### Executing Unit Tests
Run the entire test suite via the Gradle wrapper:
```bash
# Run all unit tests across all 15 modules
./gradlew test

# Run tests for a specific module
./gradlew :core:common:testDebugUnitTest
./gradlew :core:bluetooth:testDebugUnitTest
./gradlew :core:database:testDebugUnitTest
./gradlew :core:network:testDebugUnitTest
```

---

## 3. Real Hardware Verification Matrix

### Device 1: Samsung Galaxy A36 5G (Hub Central Smartphone)
*Target: Android 14 / 15 (One UI 6.x / 7.0)*

| Test Case | Procedure | Expected Result | Pass/Fail Criteria |
| :--- | :--- | :--- | :--- |
| **TC-HUB-01** | Install and launch `app-debug.apk`. | Hub launches into Dashboard with One UI 7 typography and styling. | Clean startup; no crashes. |
| **TC-HUB-02** | Grant Bluetooth permissions on Android 14+. | System displays "Nearby devices" prompt with `neverForLocation` flag. | Prompt does not request precise location. |
| **TC-HUB-03** | Embedded Ktor CIO Server startup. | Navigate to Pairing tab. Check displayed IP/port. | Server binds to `0.0.0.0:8443`; IP matches local Wi-Fi. |
| **TC-HUB-04** | Home Screen Widgets. | Long press home screen -> Add ArBattery Widgets (2x2, 4x2, 4x4). | Widgets render high-contrast gauges and respond to system theme changes. |
| **TC-HUB-05** | Threshold Notifications. | In Settings, set Low Battery threshold to 25%. Ingest simulated or real reading of 20%. | System notification fires with device name and percentage. |

---

### Device 2: Hammer Ace Smartwatch (BLE Diagnostics)
*Target: Bluetooth Low Energy peripheral (Unverified SIG compatibility)*

| Test Case | Procedure | Expected Result | Pass/Fail Criteria |
| :--- | :--- | :--- | :--- |
| **TC-BLE-01** | Open Discovery -> Tap Hammer Ace Diagnostic tab -> Start Probing. | BLE scanner targets the Hammer Ace and establishes direct GATT connection. | Device is connected without timeout. |
| **TC-BLE-02** | Service Enumeration. | Engine walks GATT table and outputs all service UUIDs. | Discovered services displayed in real-time list. |
| **TC-BLE-03** | Battery Service Verification. | Engine checks for SIG Battery Service `0x180F` and characteristic `0x2A19`. | If present: reads byte and displays live %. If missing: explicitly marks as `UNSUPPORTED_STANDARD_SERVICE`. Zero fake data. |
| **TC-BLE-04** | Diagnostic Report Export. | Tap **Export Diagnostic Report**. | Generates formatted JSON and human-readable text file and opens Android share sheet. |

---

### Device 3: Samsung Galaxy Tab A 8.0 (2019 Companion Tablet)
*Target: Android 9 / 10 / 11 (`minSdk = 24`)*

| Test Case | Procedure | Expected Result | Pass/Fail Criteria |
| :--- | :--- | :--- | :--- |
| **TC-TAB-01** | Install `companion-debug.apk` on Galaxy Tab A 8.0. | App installs and launches cleanly on older Android APIs. | No dex or compatibility crashes on API 28-30. |
| **TC-TAB-02** | Hardware Battery Telemetry Reading. | Verify displayed percentage, voltage (mV), and temperature (°C). | Readings match actual Android system battery tray. |
| **TC-TAB-03** | Pairing Handshake. | Point camera at Hub's QR code (or enter IP/token manually) -> Tap **Connect to Hub**. | Handshake succeeds; token saved to DataStore. Status displays "Paired". |
| **TC-TAB-04** | Manual Sync ("Sync Now"). | Tap **Sync Now**. | Telemetry transmitted; Hub updates within < 1 second. |
| **TC-TAB-05** | WorkManager Background Sync. | Disconnect app from foreground. Wait 15 minutes. | Hub displays fresh reading with updated timestamp. |

---

## 4. Edge Cases & Resilience Testing

1. **Bluetooth Radio Toggled Off Mid-Session**:
   - The application traps `BluetoothAdapter.STATE_TURNING_OFF` and gracefully sets active BLE device statuses to `UNAVAILABLE` without freezing the UI thread.
2. **Wi-Fi Network Interruption / Subnet Change**:
   - When the Companion loses Wi-Fi connection during sync, WorkManager applies `NetworkType.CONNECTED` constraints and backs off exponentially.
3. **Database Integrity on Force Stop**:
   - SQLite Room database is configured with WAL (Write-Ahead Logging) mode, preventing corruption during sudden battery power-offs.

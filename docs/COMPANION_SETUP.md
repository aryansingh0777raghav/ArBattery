# Samsung Galaxy Tab A 8.0 & Android Companion Setup Guide

## 1. Overview

The **ArBattery Companion** (`:companion`) is a specialized, lightweight Android application engineered to report hardware battery metrics from secondary Android devices—such as the **Samsung Galaxy Tab A 8.0 (2019)**—directly to the primary ArBattery Hub smartphone (e.g., Samsung Galaxy A36 5G).

```
┌─────────────────────────────────┐               ┌─────────────────────────────────┐
│     Samsung Galaxy Tab A 8.0    │  Wi-Fi / LAN  │     Samsung Galaxy A36 5G       │
│      [ArBattery Companion]      ├──────────────►│         [ArBattery Hub]         │
│  - Android BatteryManager       │  HTTP / 8443  │  - Ktor Embedded CIO Server     │
│  - WorkManager Periodic Sync    │  Bearer Auth  │  - Room Persistence             │
│  - Live Telemetry Gauges        │               │  - Jetpack Glance Home Widgets  │
└─────────────────────────────────┘               └─────────────────────────────────┘
```

---

## 2. Hardware & Operating System Compatibility

The Galaxy Tab A 8.0 (2019, model SM-T290 / SM-T295) originally shipped with Android 9.0 (Pie, API 28) and received updates to Android 10 (API 29) and Android 11 (API 30).

- **Minimum SDK**: `minSdk = 24` (Android 7.0 Nougat).
- **Target SDK**: `targetSdk = 35` (Android 15).
- **Compatibility Matrix**:
  - Fully compatible with 32-bit and 64-bit ARM architectures (`armeabi-v7a`, `arm64-v8a`).
  - Gracefully handles battery intent changes between legacy and modern Android power subsystems.

---

## 3. Battery Telemetry Collected

ArBattery Companion uses Android's sticky `Intent.ACTION_BATTERY_CHANGED` broadcast combined with the `BatteryManager` system service to read physical hardware metrics without synthetic rounding:

| Metric | API Source | Description |
| :--- | :--- | :--- |
| **Battery Percentage** | `BatteryManager.EXTRA_LEVEL` / `EXTRA_SCALE` | Calculated percentage `(level * 100 / scale)`. Falls back to `BATTERY_PROPERTY_CAPACITY`. |
| **Charging State** | `BatteryManager.EXTRA_STATUS` | `CHARGING`, `FULL`, `DISCHARGING`, `NOT_CHARGING`, or `UNKNOWN`. |
| **Power Source** | `BatteryManager.EXTRA_PLUGGED` | Distinguishes between `AC Charger`, `USB Cable`, and `Wireless Dock`. |
| **Battery Health** | `BatteryManager.EXTRA_HEALTH` | Hardware diagnostic: `Good`, `Overheat`, `Dead`, `Over Voltage`, `Cold`, or `Failure`. |
| **Terminal Voltage** | `BatteryManager.EXTRA_VOLTAGE` | Precise millivolt (mV) reading from the battery protection circuit. |
| **Cell Temperature** | `BatteryManager.EXTRA_TEMPERATURE` | Internal battery thermistor reading in tenths of a degree Celsius (e.g., `285` = 28.5 °C). |
| **Cell Chemistry** | `BatteryManager.EXTRA_TECHNOLOGY` | Raw chemistry string (e.g., `Li-ion`, `Li-poly`). |
| **Hardware Identity** | `android.os.Build` | Device manufacturer (`Samsung`) and hardware model (`SM-T290`). |

---

## 4. Pairing & Connection Workflow

### Step 1: Initialize Pairing on the Hub (Samsung Galaxy A36 5G)
1. Open **ArBattery** on the central smartphone.
2. Tap the **Pairing** tab (or the QR icon on the Dashboard).
3. The Hub launches its embedded Ktor CIO server on port `8443` and displays:
   - Dynamic QR Code containing `arbattery://pair?ip=<HUB_IP>&port=8443&token=<RANDOM_TOKEN>`
   - 6-digit human-readable pairing PIN
   - Current LAN IP address (e.g., `192.168.1.105:8443`)

### Step 2: Connect the Tablet Companion (Galaxy Tab A 8.0)
1. Open **ArBattery Companion** on the tablet.
2. Ensure both the tablet and hub phone are connected to the **same local Wi-Fi network**.
3. Point your camera at the Hub's QR code or manually enter the Hub's IP and Pairing Token / PIN.
4. Tap **Connect to Hub**.
5. The Companion transmits an authenticated handshake request to `/api/v1/pairing/confirm`.
6. Upon validation, the Hub authorizes the device and registers its unique `deviceId` in its database.

---

## 5. Synchronization Mechanics

### A. Periodic Synchronization (WorkManager)
- Enqueued via `CompanionSyncManager.schedulePeriodicSync()`.
- Standard interval: **15 minutes** (Android WorkManager minimum periodic threshold).
- Constraints: Requires an active network connection (`NetworkType.CONNECTED`).
- Power Efficiency: Respects Android Doze mode and App Standby Buckets to ensure minimal background drain on the tablet.

### B. On-Demand Synchronization ("Sync Now")
- Immediate background dispatch triggered by tapping the **Sync Now** button in the Companion UI.
- Direct HTTP POST to `http://<HUB_IP>:8443/api/v1/devices/battery` with HMAC Bearer token.
- Returns instant confirmation and updates the Hub's database and UI widgets.

---

## 6. Troubleshooting & Best Practices

### Cellular 464XLAT IP (`192.0.0.2`) vs Wi-Fi LAN IP
- On modern Samsung smartphones (e.g. Galaxy A36 5G) with mobile data active, Android assigns internal IP addresses in the `192.0.0.x` range (RFC 7335 / 464XLAT translation for cellular carriers).
- These addresses are internal to the phone's cellular modem and cannot be reached over Wi-Fi.
- ArBattery automatically filters out carrier interfaces, prioritizes `wlan0` / `192.168.x.x` addresses, and provides an **"Auto-Detect Hub"** button in ArBattery Companion to automatically locate the phone on the local Wi-Fi subnet.

### Wi-Fi IP Address Changes
- If the Hub smartphone's router assigns a new local IP address via DHCP:
  1. Simply tap **Auto-Detect Hub** on the tablet to automatically scan the local subnet and re-acquire the Hub's IP.
  2. Alternatively, check the Hub's current IP in ArBattery -> Pairing tab and update the **Hub IP** field.
  3. Or configure a DHCP reservation on your home router for the Hub smartphone.

### Background Synchronization on Samsung One UI
Samsung's One UI battery optimization may put background applications into deep sleep:
1. On the Galaxy Tab A 8.0, navigate to **Settings -> Apps -> ArBattery Companion**.
2. Tap **Battery**.
3. Select **Unrestricted** (or add ArBattery Companion to "Never sleeping apps" under Device Care).

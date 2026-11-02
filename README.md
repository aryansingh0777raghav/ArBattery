# ArBattery 🔋

> **All your devices. One battery dashboard.**

**ArBattery** is an enterprise-grade, privacy-first, native Android multi-module application ecosystem designed to aggregate, monitor, and visualize real-time battery telemetry from all your personal devices—smartwatches, tablets, smartphones, and desktop PCs—on a single Android smartphone.

---

## 🌟 Key Highlights

- **🔒 100% Privacy-First & Local-First**: No mandatory accounts, no cloud relays, no tracking. Communication occurs strictly over direct Bluetooth Low Energy (BLE) or authenticated Local Area Network (LAN) sockets.
- **🎯 Absolute Telemetry Integrity (Zero Fabrication)**: Battery percentages and charging states are never simulated or guessed. If hardware telemetry is missing or stale, explicit diagnostic states (`--`, `STALE`, `UNSUPPORTED`) are rendered alongside exact timestamps.
- **📱 Native Jetpack Glance Widgets**: Modern, responsive home-screen widgets (Small 2x2, Medium 4x2, Large 4x4) matching system Material You / One UI dynamic palettes.
- **⌚ Hammer Ace Smartwatch Diagnostic Suite**: Dedicated hardware inspection workflow that probes the standard Bluetooth SIG Battery Service (`0x180F` / `0x2A19`), dumps full GATT service trees, and exports audit reports.
- **📲 Dedicated Tablet Companion App**: Separate lightweight APK specifically designed for tablets like the Samsung Galaxy Tab A 8.0 (`minSdk = 24`), capturing exact `BatteryManager` metrics (percentage, charging source, cell health, voltage in mV, thermistor temperature in °C).
- **🎨 Samsung One UI 7 Inspired Design**: Custom-crafted design system with high-contrast circular gauges, animated charge bars, device-specific glyphs, and responsive dark/light theme support.
- **⚡ Embedded Ktor CIO Server**: Central Hub smartphone hosts a local authenticated HTTP/WebSocket server on port `8443` with HMAC-SHA256 bearer token validation.
- **🔔 Threshold Notifications**: Configurable Low (default: 20%) and Critical (default: 10%) battery alerts with custom vibration patterns.

---

## 🏗 Modular Architecture

The repository is organized into 15 focused Gradle modules adhering to Clean Architecture and MVVM:

```
ArBattery/
├── app/                  # Central Hub Android application entry point & DI container
├── companion/            # Dedicated Tablet/Secondary Android Companion application
├── widget/               # Jetpack Glance Home Screen Widgets (2x2, 4x2, 4x4)
├── core/
│   ├── common/           # Pure domain models, AppResult, battery utilities
│   ├── database/         # Room 2.6.1 persistence (Devices, History, Pairings)
│   ├── bluetooth/        # BLE Scanner, GATT Manager, Standard SIG Battery Reader
│   ├── network/          # Ktor CIO embedded server & client, crypto auth tokens
│   ├── designsystem/     # One UI 7 colors, typography, battery gauges & components
│   └── device/           # Universal battery coordination engine & freshness rules
├── feature/
│   ├── dashboard/        # Main dashboard screen with hero cards & device filters
│   ├── discovery/        # BLE scan, Companion LAN wizard & Hammer Ace diagnostic
│   ├── pairing/          # QR code generator (ZXing) & 6-digit PIN pairing
│   ├── device_details/   # Detailed device view with 24h Canvas curve graph
│   ├── history/          # Battery analytics, discharge KPIs & CSV export
│   └── settings/         # Threshold sliders, server port & retention settings
└── docs/                 # Architectural specifications and hardware setup guides
```

---

## 🎯 Target Hardware Compatibility

| Device | Role | Platform / OS | Communication Method |
| :--- | :--- | :--- | :--- |
| **Samsung Galaxy A36 5G** | Primary Monitoring Hub | Android 14 / 15 (One UI 6.x / 7.0) | Embedded Ktor Server, BLE Central |
| **Hammer Ace Smartwatch** | BLE Peripheral (Diagnostic Target) | Proprietary BLE Firmware | Standard SIG BLE `0x180F` / GATT Dump |
| **Samsung Galaxy Tab A 8.0** | Secondary Companion Device | Android 9 / 10 / 11 (`minSdk = 24`) | Wi-Fi / Local REST API (Port 8443) |
| **Windows 10/11 Laptop** | Future Desktop Workstation | Windows 10/11 (x64 / ARM64) | Local Wi-Fi HTTP/WebSocket Daemon |

---

## 🚀 Building & Installing

### Prerequisites
- **JDK**: Microsoft OpenJDK 21 LTS or compatible JDK 21.
- **Android SDK**: API Level 35 build tools installed.

### Build via Gradle Wrapper

```bash
# 1. Build Central Hub Application APK
./gradlew :app:assembleDebug

# 2. Build Tablet Companion Application APK
./gradlew :companion:assembleDebug

# 3. Run Automated Unit Tests across all modules
./gradlew test
```

### Generated APK Locations

- **ArBattery Hub**: `app/build/outputs/apk/debug/app-debug.apk`
- **ArBattery Companion**: `companion/build/outputs/apk/debug/companion-debug.apk`

### Installation via ADB

```bash
# Install Hub on Primary Phone (e.g., Galaxy A36 5G)
adb -s <PHONE_SERIAL> install -r app/build/outputs/apk/debug/app-debug.apk

# Install Companion on Tablet (e.g., Galaxy Tab A 8.0)
adb -s <TABLET_SERIAL> install -r companion/build/outputs/apk/debug/companion-debug.apk
```

---

## 📖 In-Depth Documentation

Detailed technical documentation is available in the [`docs/`](file:///c:/Users/aryan/Downloads/ArBattery/docs/) directory:

- [System Architecture](file:///c:/Users/aryan/Downloads/ArBattery/docs/ARCHITECTURE.md): Multi-module design, Clean Architecture data flows, and security model.
- [BLE Compatibility & Hammer Ace Diagnostics](file:///c:/Users/aryan/Downloads/ArBattery/docs/BLE_COMPATIBILITY.md): Standard Bluetooth SIG Battery Service `0x180F`/`0x2A19`, CCCD notifications, and diagnostic inspection suite.
- [Companion Setup Guide](file:///c:/Users/aryan/Downloads/ArBattery/docs/COMPANION_SETUP.md): Galaxy Tab A 8.0 installation, pairing handshake, and WorkManager sync.
- [Permissions Architecture](file:///c:/Users/aryan/Downloads/ArBattery/docs/PERMISSIONS.md): Complete permissions matrix and graceful degradation across Android 7 to 15.
- [Testing & Verification Guide](file:///c:/Users/aryan/Downloads/ArBattery/docs/TESTING.md): Unit tests, automated suites, and real hardware testing procedures.

---

## 👤 Creator & Lead Platform Architect

<div align="center">

### **Aryan Singh**
*Creator & Lead Platform Architect*  
**Software Architect & Platform Engineer | Independent Filmmaker**  
📍 *Gorakhpur, Uttar Pradesh, India*

[🌐 Portfolio](https://aryan-cine-dev-portfolio.vercel.app/) • [💻 GitHub](https://github.com/aryansingh0777raghav) • [💼 LinkedIn](https://www.linkedin.com/in/iamaryan07) • [🎬 IMDb](https://www.imdb.com/name/nm18214429/) • [📸 Instagram](https://www.instagram.com/iam_aryannnn07) • [✉️ Email](mailto:aryansingh979211@gmail.com)

</div>

> *"Software Architect and Independent Filmmaker specializing in zero-latency systems, local-first architectures, and high-craft digital products."*

#### 🏆 Notable Flagships & Ecosystems
- **ArKTest Beta**
- **ArCV**
- **CarHari**
- **VnishAr**
- **CineOn Studio 7**

---

## 📄 License
ArBattery is distributed under the Apache License 2.0.

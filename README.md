# ReefBrite Controller – Android App

A Bluetooth Low Energy (BLE) Android application for wirelessly controlling ReefBrite aquarium LED lighting systems.

## Features

- **BLE Device Discovery & Connection** – Scan for and connect to ReefBrite hardware controllers via Bluetooth LE
- **Dual-Channel Brightness Control** – Independent real-time control of blue and white LED channels (0–100%)
- **24-Hour Lighting Schedules** – Create up to 10 time-based schedule points with custom blue/white intensity levels
- **Schedule Visualization** – Interactive line chart displaying the full 24-hour lighting profile
- **Simulation Mode** – Time-compressed preview of a full day's lighting cycle on the connected hardware
- **Custom Device Naming** – Rename connected controllers (persisted across sessions)

## Recent Update – v6.0

- **Upgraded to Android SDK 35** (Android 15) with `minSdk 23`
- **Android 12+ Bluetooth permissions** – Added `BLUETOOTH_SCAN` and `BLUETOOTH_CONNECT` runtime permissions with proper `neverForLocation` flag; legacy Bluetooth/location permissions capped at `maxSdkVersion="30"`
- **AndroidX migration** – Replaced legacy support libraries with `androidx.appcompat` and `androidx.legacy`
- **Modern build tooling** – Migrated to current Gradle plugin with `namespace` declaration and updated dependencies (`MPAndroidChart v3.1.0`, `Gson 2.10.1`, `ConstraintLayout 2.1.4`)
- **Signing credentials externalized** – Release signing now reads passwords from environment variables (`REEFBRITE_STORE_PASSWORD`, `REEFBRITE_KEY_PASSWORD`) instead of hardcoded values

## Build & Run

1. Open the `ReefBrite Controller` directory in **Android Studio**
2. Set environment variables for release signing (optional):
   ```bash
   export REEFBRITE_STORE_PASSWORD=your_store_password
   export REEFBRITE_KEY_PASSWORD=your_key_password
   ```
3. Build and run on a device with BLE support (API 23+)

## Communication Protocol

The app communicates with the ReefBrite hardware controller over a Nordic Semiconductor UART BLE service using simple byte-array commands:

| Command | Bytes | Description |
|---------|-------|-------------|
| Set White | `[2, value]` | Set white LED brightness (0–255) |
| Set Blue  | `[3, value]` | Set blue LED brightness (0–255)  |
| Sim Time  | `[4, hour, minute]` | Jump simulation to specified time |
| Disconnect | `[5]` | Disconnect from device |
| Cancel | `[7]` | Cancel current operation |
| Suspend | `[8]` | Suspend for mode change |
| Rename | `[9, ...name]` | Rename device (max 7 chars) |

## Project Structure

```
app/src/main/java/com/nordicsemi/reefBrite/
├── MainActivity.java        # Main UI – connect, schedule list, navigation
├── UartService.java          # BLE GATT service for device communication
├── DeviceListActivity.java   # BLE device scanner/picker
├── BrightnessActivity.java   # Real-time blue/white LED control
├── PointActivity.java        # Schedule point editor (time + intensity)
├── ChartActivity.java        # 24-hour schedule chart + simulation
├── PointModel.java           # Data model for schedule points
└── PointAdapter.java         # ListView adapter for schedule points
```

## Requirements

- Android 6.0 (API 23) or later
- Device with Bluetooth Low Energy support
- ReefBrite hardware controller

## License

Based on Nordic Semiconductor nRF UART – see [LICENSE](LICENSE) for details.

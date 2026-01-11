# 🖨️ Plan: Hardware Integration - Bluetooth Printer

**Feature Name**: Bluetooth Thermal Printer Support
**Status**: 🟡 In Progress
**Related Roadmap Item**: Phase 7
**Created**: 2026-01-11

---

## 1. Context & Objective

### Why
- Kasir membutuhkan struk fisik untuk pelanggan
- Thermal printer adalah standar industri untuk POS
- Harus support printer 58mm dan 80mm (paling umum)

### Goal
- Discover dan pair dengan Bluetooth thermal printer
- Format receipt menggunakan ESC/POS protocol
- Print struk dari ReceiptScreen dengan satu klik
- Support printer populer: EPSON, GOOJPRT, Xprinter, dll

### User Stories
- "Sebagai Kasir, saya ingin mencetak struk setelah transaksi selesai"
- "Sebagai Owner, saya ingin setup printer sekali dan selalu tersambung"
- "Sebagai Kasir, saya ingin melihat status printer (connected/disconnected)"

---

## 2. Technical Approach

### Architecture

```
feature/printer/
├── data/
│   ├── bluetooth/
│   │   ├── BluetoothPrinterManager.kt    # Discovery, connect, disconnect
│   │   └── BluetoothPermissionHelper.kt  # Runtime permissions
│   ├── escpos/
│   │   ├── EscPosCommands.kt             # ESC/POS byte commands
│   │   └── ReceiptFormatter.kt           # Format transaction to ESC/POS
│   └── repository/
│       └── PrinterRepository.kt          # Printer operations interface
├── domain/
│   ├── model/
│   │   ├── PrinterDevice.kt              # Bluetooth device wrapper
│   │   └── PrinterStatus.kt              # Connected, Disconnected, Printing
│   └── usecase/
│       ├── DiscoverPrintersUseCase.kt
│       ├── ConnectPrinterUseCase.kt
│       └── PrintReceiptUseCase.kt
├── presentation/
│   ├── PrinterSettingsScreen.kt          # Discover & manage printers
│   ├── PrinterSettingsViewModel.kt
│   └── components/
│       └── PrinterListItem.kt
├── di/
│   └── PrinterModule.kt
└── navigation/
    └── PrinterNavigation.kt
```

### Bluetooth Permissions (Android 12+)

```xml
<!-- For Android 12+ (API 31+) -->
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />

<!-- For older Android -->
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

### ESC/POS Protocol Basics

Common commands for thermal printers:
- `0x1B 0x40` - Initialize printer
- `0x1B 0x61 0x01` - Center align
- `0x1B 0x45 0x01` - Bold on
- `0x1D 0x56 0x00` - Cut paper
- `0x0A` - Line feed

### Data Flow

```
ReceiptScreen
    ↓ (onClick Print)
PrintReceiptUseCase
    ↓ (format transaction)
ReceiptFormatter (ESC/POS bytes)
    ↓ (send to printer)
BluetoothPrinterManager
    ↓ (Bluetooth socket)
Printer Hardware
```

---

## 3. Implementation Steps

### Sprint 12: Printer Setup ✅ Complete

1. [x] Create `:feature:printer` module
2. [x] Add Bluetooth permissions to AndroidManifest
3. [x] Create BluetoothPrinterManager for discovery/connect
4. [x] Create PrinterDevice domain model
5. [x] Create PrinterSettingsScreen UI
6. [x] Save last connected printer to DataStore
7. [x] Add "Printer" menu in Settings

### Sprint 13: ESC/POS & Printing ✅ Complete

8. [x] Create EscPosCommands constants
9. [x] Create ReceiptFormatter for transaction to bytes
10. [x] Implement PrinterService for receipt printing
11. [x] Integrate print button in ReceiptScreen
12. [x] Add print status indicator (in ReceiptScreen)
13. [x] Error handling (printer offline, navigation to settings)

---

## 4. Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| **Different printer brands** | Incompatibility | Use standard ESC/POS, test with multiple brands |
| **Bluetooth pairing fails** | User frustration | Clear error messages, retry logic |
| **Permission denied** | Feature broken | Graceful degradation, explain why needed |
| **Paper width varies** | Bad formatting | Detect 58mm vs 80mm, adjust columns |
| **Slow printing** | UX issue | Progress indicator, async printing |

---

## 5. Dependencies

```kotlin
// No external library needed - using Android Bluetooth API
// ESC/POS commands are just byte arrays
```

---

## 6. Success Criteria

- [ ] Can discover nearby Bluetooth printers
- [ ] Can connect and remember printer
- [ ] Receipt prints correctly with logo placeholder
- [ ] Works with 58mm and 80mm printers
- [ ] Graceful error handling
- [ ] Print takes < 3 seconds

---

## 7. Testing Checklist

- [ ] Discover printers (with/without permissions)
- [ ] Connect to printer
- [ ] Disconnect from printer
- [ ] Print sample receipt
- [ ] Print after transaction complete
- [ ] Handle printer offline
- [ ] Handle paper out (if detectable)
- [ ] App restart remembers last printer

---

# Multi-Role Access Control Implementation Plan

> **Version**: 1.0.0
> **Status**: 🟡 In Progress
> **Last Updated**: 2026-01-13

---

## Overview

Implementasi sistem kontrol akses multi-role untuk ZatiarasPOS yang memungkinkan:
- **Pemilik (Owner)**: Akses penuh ke semua fitur
- **Kasir (Cashier)**: Akses terbatas, beberapa screen memerlukan PIN pemilik

---

## Architecture

### Core Components

| Component | Location | Purpose |
|-----------|----------|---------|
| `UserRole` | `core/data/access/` | Enum untuk role user (PEMILIK, KASIR) |
| `LockableRoute` | `core/data/access/` | Enum untuk route yang bisa dikunci |
| `AccessControlPreferences` | `core/data/access/` | DataStore untuk PIN owner & locked routes |
| `AccessControlManager` | `core/data/access/` | Manager untuk cek akses |
| `OwnerPinDialog` | `core/ui/components/` | Dialog input PIN owner |
| `AccessControlGate` | `core/ui/components/` | Composable wrapper untuk protected screens |

### Flow Diagram

```
User navigates to protected screen
           │
           ▼
    AccessControlGate
           │
           ▼
   ┌───────────────────┐
   │ Check user role   │
   └───────────────────┘
           │
    ┌──────┴──────┐
    │             │
 PEMILIK       KASIR
    │             │
    ▼             ▼
 GRANTED    Check if route locked
                  │
           ┌──────┴──────┐
           │             │
       Not Locked     Locked
           │             │
           ▼             ▼
        GRANTED    Show OwnerPinDialog
                         │
                  ┌──────┴──────┐
                  │             │
              Correct PIN   Wrong PIN
                  │             │
                  ▼             ▼
              GRANTED      Navigate back
```

---

## Lockable Routes

| Route | Display Name | Recommended Lock |
|-------|--------------|------------------|
| `settings` | Pengaturan | ✅ Yes |
| `inventory` | Inventaris | ✅ Yes |
| `pnl_report` | Laporan Laba Rugi | ✅ Yes |
| `printer_settings` | Pengaturan Printer | Optional |
| `reports` | Tab Laporan | Optional |
| `cash_record` | Tab Buku Kas | Optional |

---

## Implementation Status

### ✅ Completed

1. **UserRole.kt** - Enum untuk role user
2. **LockableRoute.kt** - Enum untuk route yang bisa dikunci
3. **AccessControlPreferences.kt** - DataStore untuk PIN owner & locked routes
4. **AccessControlManager.kt** - Manager untuk cek akses
5. **OwnerPinDialog.kt** - Dialog input PIN owner
6. **AccessControlGate.kt** - Composable wrapper untuk protected screens
7. **SettingsScreen.kt** - Updated dengan "Kunci Menu" section
8. **SettingsViewModel.kt** - Updated dengan access control logic

### 🟡 Pending Integration

1. **Wrap Protected Routes** - Integrate AccessControlGate in navigation
2. **Tab Lock Indicators** - Show lock icon on locked tabs in bottom nav
3. **Sync to Supabase** - Sync locked routes & owner PIN to cloud

---

## Usage Examples

### 1. Using AccessControlGate in Navigation

```kotlin
// In your NavGraphBuilder extension
fun NavGraphBuilder.pnlReportScreen(
    accessControlManager: AccessControlManager,
    onNavigateBack: () -> Unit
) {
    composable(route = PNL_REPORT_ROUTE) {
        AccessControlGate(
            accessControlManager = accessControlManager,
            route = LockableRoute.PNL_REPORT.route,
            screenName = "Laporan Laba Rugi",
            onAccessDenied = onNavigateBack
        ) {
            PnlReportRoute(onNavigateBack = onNavigateBack)
        }
    }
}
```

### 2. Checking Access in ViewModel

```kotlin
class SomeViewModel @Inject constructor(
    private val accessControlManager: AccessControlManager
) : ViewModel() {
    
    suspend fun checkCanAccessReports(): Boolean {
        return !accessControlManager.requiresPin(LockableRoute.REPORTS_TAB.route)
    }
}
```

### 3. Owner PIN Verification

```kotlin
// In a composable
val scope = rememberCoroutineScope()

OwnerPinDialog(
    onDismiss = { /* handle dismiss */ },
    onPinVerified = { /* grant access */ },
    verifyPin = { pin -> 
        accessControlManager.verifyOwnerPin(pin)
    },
    screenName = "Pengaturan"
)
```

---

## Testing Checklist

- [ ] Login as Pemilik, verify all menus accessible
- [ ] Login as Kasir, verify locked menus require PIN
- [ ] Set owner PIN, verify it persists
- [ ] Lock a route, verify kasir needs PIN
- [ ] Unlock a route, verify kasir can access freely
- [ ] Wrong PIN shows error and allows retry
- [ ] Cancel PIN dialog navigates back
- [ ] Owner can set/change PIN from Settings

---

## Notes

1. **Security**: Owner PIN is hashed with SHA-256 before storage
2. **Offline**: All access control works offline (DataStore-based)
3. **Future**: Consider syncing locked routes to Supabase for multi-device

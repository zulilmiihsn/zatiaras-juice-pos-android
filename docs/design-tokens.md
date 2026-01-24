# 🎨 Design Tokens - ZatiarasPOS

## Overview

Design Tokens adalah konstanta yang mendefinisikan nilai-nilai styling konsisten di seluruh aplikasi. File ini terletak di:

```
core/ui/src/main/java/com/zatiaras/pos/core/ui/theme/Dimensions.kt
```

## Cara Penggunaan

### 1. Import dan Akses

```kotlin
import com.zatiaras.pos.core.ui.theme.LocalDimensions
import com.zatiaras.pos.core.ui.theme.AppShapes

@Composable
fun MyScreen() {
    val dimensions = LocalDimensions.current
    
    Box(
        modifier = Modifier.padding(dimensions.paddingM)
    ) {
        // Content
    }
}
```

### 2. Harus disediakan di Theme (jika ingin customizable)

```kotlin
// Di Theme.kt
CompositionLocalProvider(
    LocalDimensions provides Dimensions()
) {
    MaterialTheme(...) {
        content()
    }
}
```

---

## Referensi Nilai

### Padding Values

| Token | Nilai | Penggunaan |
|-------|-------|------------|
| `paddingXXS` | 4.dp | Micro padding untuk icons, badges |
| `paddingXS` | 8.dp | Tight padding untuk chips, compact elements |
| `paddingS` | 12.dp | Secondary padding untuk compact cards, list items |
| `paddingM` | 16.dp | **Standard content padding (paling umum)** |
| `paddingL` | 20.dp | Hero/Featured card padding |
| `paddingXL` | 24.dp | Modal/Sheet/Auth screen padding |
| `paddingXXL` | 32.dp | Large section dividers |

### Spacing Values

| Token | Nilai | Penggunaan |
|-------|-------|------------|
| `spacingXXS` | 4.dp | Micro spacing |
| `spacingXS` | 8.dp | Tight spacing untuk compact lists |
| `spacingS` | 12.dp | **Standard item spacing dalam lists** |
| `spacingM` | 16.dp | Section spacing |
| `spacingL` | 24.dp | Large section divider |

### Corner Radius (via AppShapes)

| Shape | Nilai | Penggunaan |
|-------|-------|------------|
| `AppShapes.XS` | 4.dp | Progress bars, small indicators |
| `AppShapes.S` | 8.dp | Thumbnails, avatars |
| `AppShapes.M` | 12.dp | **Buttons, TextFields, small cards** |
| `AppShapes.L` | 16.dp | **Major cards, containers** |
| `AppShapes.XL` | 20.dp | Chips, pills |
| `AppShapes.Full` | 50% | Fully rounded (circles) |

### Icon Sizes

| Token | Nilai | Penggunaan |
|-------|-------|------------|
| `iconSizeXS` | 16.dp | Small inline icons |
| `iconSizeS` | 18.dp | Button icons |
| `iconSizeM` | 20.dp | List item icons |
| `iconSizeL` | 24.dp | Standard icons (default) |
| `iconSizeXL` | 48.dp | Large icons (avatars) |
| `iconSizeXXL` | 72.dp | Empty state illustrations |
| `iconSizeHero` | 80.dp | Extra large icons |

---

## Hierarki Visual

```
┌─────────────────────────────────────────┐
│  Modal/Sheet (paddingXL = 24.dp)        │
│  ┌─────────────────────────────────┐    │
│  │  Hero Card (paddingL = 20.dp)   │    │
│  └─────────────────────────────────┘    │
│  ┌─────────────────────────────────┐    │
│  │  Standard Card (paddingM = 16dp)│    │
│  │  ┌───────────────────────────┐  │    │
│  │  │ List Item (paddingS = 12) │  │    │
│  │  └───────────────────────────┘  │    │
│  └─────────────────────────────────┘    │
└─────────────────────────────────────────┘
```

---

## Contoh Implementasi

### Card dengan Standard Padding
```kotlin
Card(
    shape = AppShapes.L, // 16.dp
    modifier = Modifier.padding(dimensions.paddingM) // 16.dp
) {
    Column(modifier = Modifier.padding(dimensions.paddingM)) {
        // Content
    }
}
```

### Hero Card dengan Emphasis
```kotlin
Card(
    shape = AppShapes.L,
    modifier = Modifier.padding(dimensions.paddingM)
) {
    Column(modifier = Modifier.padding(dimensions.paddingL)) { // 20.dp
        // Featured content
    }
}
```

### Compact List Item
```kotlin
Card(
    shape = AppShapes.M // 12.dp
) {
    Row(modifier = Modifier.padding(dimensions.paddingS)) { // 12.dp
        // Compact content
    }
}
```

### Button/TextField
```kotlin
Button(
    shape = AppShapes.M, // 12.dp
    modifier = Modifier.height(dimensions.buttonHeight) // 48.dp
) { ... }

OutlinedTextField(
    shape = AppShapes.M // 12.dp
)
```

---

## Best Practices

1. **Selalu gunakan Design Tokens** - Jangan hardcode nilai seperti `padding(16.dp)`, gunakan `padding(dimensions.paddingM)`

2. **Konsisten dengan hierarki** - Gunakan padding yang tepat sesuai konteks:
   - Screen content → `paddingM` (16.dp)
   - Hero/Featured → `paddingL` (20.dp)
   - Modal/Sheet → `paddingXL` (24.dp)

3. **Corner radius yang benar**:
   - Cards → `AppShapes.L` (16.dp)
   - Buttons/TextField → `AppShapes.M` (12.dp)
   - Chips → `AppShapes.XL` (20.dp)

4. **Jangan campur** - Jika satu card menggunakan radius 16.dp, card yang setara juga harus 16.dp

---

## Migration Guide

Jika ingin mengadopsi Design Tokens ke file yang sudah ada:

```kotlin
// Sebelum
.padding(16.dp)

// Sesudah
val dimensions = LocalDimensions.current
.padding(dimensions.paddingM)

// Sebelum
RoundedCornerShape(12.dp)

// Sesudah
AppShapes.M
```

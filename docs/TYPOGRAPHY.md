# Typography Guidelines - ZatiarasPOS

## Overview

This app uses **Material Design 3 (M3)** typography scale with **Plus Jakarta Sans** font family for a modern, professional appearance optimized for retail/POS contexts.

## Typography Scale

### Display Styles
*Use for large, impactful statements on large screens. Rare in this app.*

| Style | Size | Weight | Usage |
|-------|------|--------|-------|
| `displayLarge` | 57sp | Bold | Hero numbers (dashboard total) |
| `displayMedium` | 45sp | Bold | Large highlighted amounts |
| `displaySmall` | 36sp | Bold | Prominent figures |

### Headline Styles
*Use for short, high-emphasis text. Great for page titles and dialogs.*

| Style | Size | Weight | Usage |
|-------|------|--------|-------|
| `headlineLarge` | 32sp | SemiBold | Main page headers |
| `headlineMedium` | 28sp | SemiBold | Dialog titles, app name |
| `headlineSmall` | 24sp | SemiBold | Modal headers, PIN keypad digits |

### Title Styles
*Use for medium-emphasis text that identifies sections and components.*

| Style | Size | Weight | Usage |
|-------|------|--------|-------|
| `titleLarge` | 22sp | Bold | Card headers, receipt title |
| `titleMedium` | 16sp | Bold | Section headers, list item titles |
| `titleSmall` | 14sp | Bold | Sub-section titles, filter labels |

### Body Styles
*Use for long-form content and descriptions. Optimized for readability.*

| Style | Size | Weight | Usage |
|-------|------|--------|-------|
| `bodyLarge` | 16sp | Medium | Primary content, form input text |
| `bodyMedium` | 14sp | Medium | Secondary content, descriptions |
| `bodySmall` | 12sp | Medium | Captions, timestamps, hints |

### Label Styles
*Use for small text like buttons, badges, and helper text.*

| Style | Size | Weight | Usage |
|-------|------|--------|-------|
| `labelLarge` | 14sp | Bold | Button text, tabs |
| `labelMedium` | 12sp | Bold | Chip text, form labels |
| `labelSmall` | 11sp | Bold | Badges, very small captions |

---

## Usage Examples

### ✅ Correct Usage
```kotlin
// Section header
Text(
    text = "Produk Terlaris",
    style = MaterialTheme.typography.titleMedium
)

// Description text
Text(
    text = "Lihat statistik penjualan",
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant
)

// Button text (automatic in Button composable)
Button(onClick = { }) {
    Text("Simpan") // Uses labelLarge by default
}
```

### ❌ Avoid
```kotlin
// Don't hardcode font sizes
Text(
    text = "Header",
    fontSize = 16.sp  // ❌ Bad
)

// Use typography instead
Text(
    text = "Header",
    style = MaterialTheme.typography.titleMedium  // ✅ Good
)
```

---

## Quick Reference by Context

| Context | Recommended Style |
|---------|------------------|
| TopAppBar title | `titleLarge` (auto) or `headlineSmall` |
| Section header in list | `titleMedium` |
| Card title | `titleMedium` or `titleLarge` |
| Product name | `bodyLarge` or `titleSmall` |
| Product price | `titleMedium` or `headlineSmall` |
| Description/subtitle | `bodyMedium` |
| Caption/timestamp | `bodySmall` or `labelSmall` |
| Button text | `labelLarge` (automatic) |
| Chip/tag | `labelMedium` |
| Badge counter | `labelSmall` |
| Empty state message | `bodyLarge` (title) + `bodyMedium` (desc) |
| Error message | `bodyMedium` |
| Form input hint | `bodyMedium` |

---

## Font Weight Guidelines

Our typography uses bolder weights than M3 defaults for better visibility in retail environments:

- **Bold** - Titles, labels, emphasis
- **SemiBold** - Headlines
- **Medium** - Body text

This ensures text is easily scannable in bright store lighting and on tablets held at various angles.

---

## Accessibility Notes

1. All font sizes use `sp` units for user font scaling support
2. Minimum text size is 11sp (labelSmall) - used sparingly
3. High contrast font weights improve readability
4. Line heights follow M3 recommendations for comfortable reading

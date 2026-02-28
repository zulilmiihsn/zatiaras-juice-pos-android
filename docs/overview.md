# **ZatiarasPOS (Android) - Project Overview**

**Version: 2026 — 1.0.0**
**Status: 🟡 Active Development (Phase 2)**

---

## 1. What is ZatiarasPOS?

ZatiarasPOS adalah aplikasi **Point of Sale Native Android** untuk operasional bisnis retail Zatiaras. Rebuild dari versi web (SvelteKit) dengan fokus pada:

- 📴 **Offline-First**: 100% berfungsi tanpa internet (kecuali login)
- ⚡ **Native Performance**: Kecepatan dan UX superior
- 🔒 **Enhanced Security**: Encrypted storage + Biometric lock
- 🤖 **AI-Powered**: Smart input dan analisis bisnis

---

## 2. Target Users

| Role | Access |
|------|--------|
| **Kasir** | POS, Manual record, Riwayat sendiri |
| **Pemilik** | Semua + Laporan + Manajemen Menu + Settings |

---

## 3. Core Features

| Module | Key Features |
|--------|--------------|
| **Auth** | Login, Biometric, PIN protection |
| **POS** | Catalog grid, Cart, Checkout, Receipt printing |
| **Inventory** | Product CRUD, Categories, Image upload |
| **Buku Kas** | Manual income/expense recording |
| **Reports** | Dashboard, Charts, P&L analysis |
| **AI Assistant** | Natural language queries, Smart input |

---

## 4. Technology Stack (Summary)

| Component | Choice |
|-----------|--------|
| Language | Kotlin 1.9.22 |
| UI | Jetpack Compose (MD3) |
| Local DB | Room + FTS4 |
| Network | Ktor Client |
| DI | Hilt |
| Sync | WorkManager |
| Backend | Supabase |

> 📖 **Detail lengkap**: Lihat [ARCHITECTURE_MASTER_PLAN.md](./ARCHITECTURE_MASTER_PLAN.md)

---

## 5. Module Structure

```
com.zatiaras.pos/
├── app/              # DI, Navigation
├── core/
│   ├── data/         # Database, Network
│   ├── ui/           # Theme, Components
│   └── domain/       # Entities, UseCases
└── feature/
    ├── auth/         # Login, PIN
    ├── pos/          # Catalog, Cart, Checkout
    ├── inventory/    # Product CRUD, Categories
    ├── reports/      # Dashboard, P&L, AI Chat
    └── printer/      # ESC/POS Receipt Printing
```

---

## 6. Multi-Branch Support

Satu APK untuk semua cabang:
- Samarinda
- Berau
- Balikpapan
- Samarinda 2

Config dipilih saat login, diinjeksi via Hilt.

---

## 7. Key Architecture Decisions

| Decision | Rationale |
|----------|-----------|
| **Room as Single Source of Truth** | UI always reads from local DB, never API directly |
| **Delta Sync** | Only fetch changed rows to save bandwidth |
| **BFF for AI** | AI API keys routed via Supabase Edge Function, never stored in APK |

> 📖 **Detail teknis lengkap**: Lihat [ARCHITECTURE_MASTER_PLAN.md](./ARCHITECTURE_MASTER_PLAN.md)

---

## 8. Screen Mapping (Web → Android)

| Web Route | Android Screen | Status |
|-----------|----------------|--------|
| `/` | `HomeScreen` | ✅ |
| `/login` | `LoginScreen` | ✅ |
| `/pos` | `POSScreen` | 🔲 |
| `/catat` | `ManualRecordScreen` | 🔲 |
| `/laporan` | `ReportsScreen` | 🔲 |

> 📖 **Progress lengkap**: Lihat [roadmap.md](./roadmap.md)

---

## 9. Quick Links

| Document | Purpose |
|----------|---------|
| [roadmap.md](./roadmap.md) | Progress & phases |
| [rules.md](./rules.md) | Coding standards |
| [api.md](./api.md) | Supabase data contract |
| [ARCHITECTURE_MASTER_PLAN.md](./ARCHITECTURE_MASTER_PLAN.md) | Technical architecture |

---

## 10. Summary

ZatiarasPOS Android = **Offline-first POS app** dengan Clean Architecture, di-backup oleh Supabase, enhanced dengan AI capabilities.

---

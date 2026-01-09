# 🗺️ Product Roadmap: Zatiaras POS (Android Native)

> **Status**: 🟢 Active Development
> **Phase**: Phase 3 - Inventory Management
> **Last Updated**: 2026-01-09

---

## **Progress Summary**

| Layer | Progress | Notes |
|-------|----------|-------|
| **Project Scaffolding** | 100% | Multi-module, Gradle, Compose setup complete |
| **Documentation** | 90% | All specs, plans, templates ready |
| **Core Modules** | 100% | Room Database + DAOs + Supabase fully working |
| **Authentication** | 60% | Login + Home done, PIN/Biometric pending |
| **Inventory** | 100% | ✅ COMPLETE - CRUD, Image Upload, Sync |
| **POS Feature** | 0% | Not started |
| **Reports** | 0% | Not started |
| **Sync Engine** | 50% | Basic sync implemented in Inventory |
| **Overall** | **~45%** | Phase 3 COMPLETE! Ready for Phase 4 |

---

## 🏁 Phase 1: Foundation & Architecture (Sprints 1-2) ✅ COMPLETE

- [x] **Project Scaffolding**: Setup Gradle, Multi-module structure, basic Compose.
- [x] **Documentation Setup**: Setup `.ai` context, `rules.md`, and docs folder structure.
- [x] **Core Modules**: Setup `:core:data` (Database/Network) and `:core:ui` (Theme/Components).
- [x] **Dependency Injection**: Global Hilt setup.

---

## 🔐 Phase 2: Authentication & Settings (Sprints 3-4) 🟡 IN PROGRESS

- [x] **Supabase Auth**: Login Screen with Email/Password.
- [x] **Home Dashboard**: Main menu with navigation grid.
- [x] **Session Management**: `EncryptedDataStore` for secure token storage.
- [ ] **Account Settings**: Separate screens for `Cashier` vs `Owner` profiles.
- [ ] **Biometric Lock**: Fingerprint/FaceID integration for app unlock.
- [ ] **PIN System**: Fallback PIN for sensitive actions.

---

## 📦 Phase 3: Inventory Management - "The Backoffice" (Sprints 5-6) ✅ COMPLETE

- [x] **Room Database**: ZatiarasDatabase setup with CategoryDao, ProductDao
- [x] **Product Entity**: Room Entity with FTS4 for search
- [x] **Category Entity**: Room Entity for product categories
- [x] **Repository**: ProductRepository interface + implementation (offline-first)
- [x] **InventoryScreen**: Product list with grid, search, category filter
- [x] **ProductCard**: Card component with Coil image loading
- [x] **ProductDetailScreen**: Add/Edit product form with validation
- [x] **Image Upload**: Gallery picker → Supabase Storage (with compression)
- [x] **Remote Sync**: Delta sync with Supabase (pull/push)
- [x] **ImageUploader**: Service for compress & upload to Supabase Storage
- [x] **InventoryRemoteDataSource**: Supabase Postgrest operations

---

## 🛒 Phase 4: Point of Sales (POS) - "The Cashier" (Sprints 7-9)

- [ ] **Catalog Grid**: Performance-optimized LazyVerticalGrid for products.
- [ ] **Cart Logic**: Local state management for transaction checking.
- [ ] **Add-Ons & Variants**: Topping, sugar level, ice level.
- [ ] **Checkout UI**: Bottom Sheet implementation (replacing `/pos/bayar` page).
- [ ] **Manual Record**: "Buku Kas" feature for non-POS income/expense (`/catat`).
- [ ] **Transaction Engine**: Calculation logic (Tax, Discount) - *Offline Safe*.
- [ ] **Checkout & Payment**: Cash, QRIS (Placeholder), Print Receipt (Bluetooth).

---

## 🔄 Phase 5: Sync Engine & Cloud (Sprint 10)

- [ ] **WorkManager Setup**: Background workers for upload.
- [ ] **Delta Sync**: Logic to fetch only *changed* data.
- [ ] **Conflict Resolution**: "Last Write Wins" logic.
- [ ] **Offline Queue**: Pending transactions management.

---

## 📊 Phase 6: Reports & AI (Sprint 11+)

- [ ] **Dashboard Stats**: Omzet, Transaksi, Item Terjual widgets.
- [ ] **Weekly Chart**: Compose Canvas/Charts for 7-day revenue.
- [ ] **Best Sellers**: Top products list.
- [ ] **P&L Report**: Profit/Loss analysis.
- [ ] **AI Assistant**: BFF Integration (Android → Supabase Edge Function → OpenAI).
- [ ] **Smart Input**: Natural language transaction parsing.

---

## 🖨️ Phase 7: Hardware Integration (Sprint 12)

- [ ] **Bluetooth Printer**: Thermal printer discovery & pairing.
- [ ] **ESC/POS Protocol**: Receipt formatting and printing.
- [ ] **Barcode Scanner**: USB/Bluetooth scanner support (Optional).

---

## 🚀 Phase 8: Production Polish (Sprint 13+)

- [ ] **Error Tracking**: Firebase Crashlytics integration.
- [ ] **Analytics**: Basic usage analytics.
- [ ] **Performance**: ProGuard/R8 optimization.
- [ ] **Testing**: Unit tests for UseCases, Integration tests for Repositories.
- [ ] **Play Store**: Release build, signing, listing.

---

## Legend

| Symbol | Meaning |
|--------|---------|
| `[x]` | Completed |
| `[ ]` | Not started / Pending |
| `🟢` | Active phase |
| `🟡` | In progress |
| `✅` | Phase complete |

---

## Notes

This roadmap follows the **"Foundation First"** approach:

1. ✅ Build solid architecture and documentation
2. 🔄 Implement core features with offline-first mindset
3. ⏳ Add cloud sync and AI features
4. ⏳ Polish and release

Each feature should have corresponding `/plans` and `/specs` documentation before implementation.

---

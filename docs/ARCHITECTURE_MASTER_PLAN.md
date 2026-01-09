# 🏗️ Zatiaras POS - Android Master Architecture

> **Version**: 2.1 (Production Ready)
> **Status**: APPROVED
> **Persona**: Senior Lead Architect

## 1. Executive Summary
This document is the **Single Source of Truth** for the technical implementation of Zatiaras POS on Native Android. It supercedes all previous web-based architectures. The core philosophy is **"Offline-First, AI-Powered, Cloud-Syned"**.

## 2. Technology Stack (Strict)
All developers must adhere to this stack. No deviations without Senior Architect approval.

| Component | Choice | Rationale |
| :--- | :--- | :--- |
| **Language** | Kotlin 2.0+ | Modern, type-safe, null-safe standard. |
| **UI Toolkit** | Jetpack Compose | Declarative UI, essential for complex "Optimistic UI" states. |
| **Local DB** | Room + FTS4 | Robust SQLite abstraction with Typo-tolerant search. |
| **Networking** | Ktor Client | Kotlin-native, multiplatform-ready, Coroutine-first. |
| **DI** | Hilt (Dagger) | Google's standard for modular dependency injection. |
| **Async** | Coroutines & Flow | Managing background streams without callback hell. |
| **Sync** | WorkManager | Guaranteed delivery of offline transactions. |
| **Image** | Coil | Lightweight image loading backed by Coroutines. |
| **Logs** | Timber | Clean logging abstraction. |

## 3. Modular Architecture (MVVM + Clean)
The project is structured as a Multi-Module Monolith to enforce separation of concerns.

```text
com.zatiaras.pos
├── app                 (Dependency Injection, Navigation Graph)
├── core
│   ├── data            (Database, Network Clients, Shared Repos)
│   ├── ui              (Design System, Themes, Common Composables)
│   └── domain          (Shared UseCases, Entities)
└── feature
    ├── auth            (Login, PIN, Biometric)
    ├── pos             (Cart, Catalog, Payment)
    ├── inventory       (Product CRUD, Stock, Image Upload)
    ├── transactions    (History, Offline Sync Status)
    ├── reports         (Dashboard, Charts)
    └── ai_assistant    (BFF Integration, Smart Input)
```

## 4. Key Implementation Strategies

### A. Offline-First Data Strategy (Single Source of Truth)
The **Room Database** is the master.
1.  **Read**: UI *always* observes Room. Never API directly.
2.  **Write**: UI writes to Room -> UI updates immediately (Optimistic).
3.  **Sync**: `WorkManager` detects changes (`is_synced=false`) and pushes to Supabase in background.

### B. Supabase Efficiency (The "Anti-Exceeded-Limit" Protocol)
To solve Free Tier limits:
1.  **Delta Sync**: Client sends `last_sync_timestamp` to server. Server returns ONLY rows changed after that time.
2.  **No Polling**: Use Supabase Realtime (WebSockets) only for critical "Stock Updates".
3.  **Batch Uploads**: Offline transactions are grouped into single batch requests.

### C. Security & AI (BFF Pattern)
1.  **AI Gateway**: Android App -> Supabase Edge Function (`/api/ai-process`) -> OpenAI.
    *   *Never* store OpenAI Keys in the APK.
2.  **Auth Storage**: Tokens stored in `EncryptedSharedPreferences` (via DataStore).
3.  **Biometrics**: `BiometricPrompt` required for "Manager-level" actions (Reports/Refunds).

## 5. Migration Map (Web -> Android)

| Feature | Old Web Implementation | New Android Implementation |
| :--- | :--- | :--- |
| **State** | Svelte Stores | Kotlin `StateFlow` / `ViewModel` |
| **Cache** | LocalStorage / IDB | Room Database (Type-Safe SQLite) |
| **Network** | Fetch API | Ktor Client |
| **Sync** | Service Worker | WorkManager |
| **Config** | Hardcoded Client | Dynamic Hilt Injection (`BranchConfig`) |

## 6. Screen Mapping (Web Parity Guarantee)
We must implement ALL screens present in the SvelteKit project:
1.  **POS**: Catalog (`/pos`), Checkout (`/pos/bayar` -> *BottomSheet*).
2.  **Reports**: Revenue Analysis (`/laporan` -> *Decomposed Components*).
3.  **Backoffice**: Product CRUD (`/pengaturan/pemilik/manajemenmenu`).
4.  **Settings**: Printer (`/pengaturan/printer`), Account (`/pengaturan/kasir`).
5.  **Manual Record**: "Buku Kas" (`/catat`) -> *Standalone Feature*.

# ZatiarasPOS - UI/UX Gap Analysis (Web vs Android)

**Objective**: Achieve 1:1 Feature & UX Parity between the reference Web App and the Native Android App. The goal is "Zero Learning Curve" for existing users migrating to the Android app.

**Status**: 🔴 Critical Gaps Found
**Date**: January 15, 2026

---

## 🚨 Executive Summary

The current Android application implements the core technical architecture (Clean Arch, MVVM, Room, Supabase) but **deviates significantly** from the Web App's User Experience (UX) and User Journey.

Key missing business flows:
1.  **Store Session Management**: The critical "Open/Close Store" financial control flow is entirely missing.
2.  **Dashboard First**: The Android app acts as a menu launcher, whereas the Web App is an analytical dashboard.
3.  **Customer Identity**: Checkout flow lacks customer name attribution.

---

## 📊 Gap Matrix & Priorities

| Priority | Feature / Screen | Web App Behavior (Reference) | Android App Current State | Gap / Action Item |
| :--- | :--- | :--- | :--- | :--- |
| 🔴 **P0** | **Home / Dashboard** | **Business Dashboard**. Shows Omzet, Profit, Charts immediately. "Buka Toko" button dominant. | **Menu Launcher**. Grid of buttons (POS, Inventory, etc). No metrics. | **Rewrite HomeScreen**. Move metrics from Reports to Home. Add "Open Store" logic. |
| 🔴 **P0** | **Store Session** | **Mandatory Flow**. Cannot use POS if store is closed. Requires Initial Cash input. | **Missing**. No session logic. POS is always accessible. | **Implement Session Logic**. Block POS access if session inactive. Add Modal for Initial Cash. |
| 🟡 **P1** | **Checkout** | **Identity First**. "Nama Pelanggan" is a mandatory/prominent input field. | **Anonymous**. Only "Catatan" (Notes) exists. | **Add `customerName` Field**. Make it prominent in Checkout screen. |
| 🟡 **P1** | **Login** | **Multi-Branch**. User selects "Cabang" (Branch) *before* login. | **Single Flow**. Username & Password only. | **Add Branch Selector**. Dropdown in Login Screen before submit. |
| 🟢 **P2** | **POS** | **Flexible Input**. Button for "Custom Item" (Manual Input). Toggle Grid/List view. | **Rigid Input**. Only catalog items. Fixed Grid view. | **Add Custom Item Feature**. Add View Toggle button. |
| 🟢 **P2** | **Reports** | **Interactive**. Filter by Date (Daily/Weekly/Monthly). "Ask AI" chat bubble. | **Static**. Pre-defined sections (Today/Weekly). No AI. | **Add Date Filters**. Implement AI Chat Interface. |

---

## 🛠 Detailed Screen Analysis

### 1. Login Screen (`feature:auth`)
*   **Gap:** Missing Branch Selection.
*   **Web Context:** Users typically work across multiple branches (Samarinda, Berau, etc.). Selecting the incorrect branch breaks data integrity.
*   **Android State:** Hardcoded or missing selection logic.
*   **Recommendation:**
    *   Add `ExposedDropdownMenu Box` for Branch Selection.
    *   Store selected branch in `UserPreferences`.

### 2. Home / Dashboard (`feature:home`)
*   **Gap:** Complete Layout Mismatch.
*   **Web Context:** The landing page is the "Pulse" of the business. It answers "How much money did we make so far?". It also controls the physical store state (Open/Close).
*   **Android State:** A simple navigation hub.
*   **Recommendation:**
    *   **UI:** Replace Menu Grid with **Dashboard Metrics Cards** (Omzet, Transaksi, Profit).
    *   **UI:** Move Navigation to a **Bottom Navigation Bar** (persistent) or keep Grid but push it down.
    *   **Logic:** Implement `StoreSessionManager`. If `isStoreOpen == false`, show big "BUKA TOKO" button. If true, show Dashboard.

### 3. POS Screen (`feature:pos`)
*   **Gap:** Flexibility features missing.
*   **Web Context:** Sometimes cashiers need to sell items not in the system (e.g. "Ongkir", "Jasa", special order). They also like List View for speed.
*   **Android State:** Good catalog performance (Paging 3), but rigid.
*   **Recommendation:**
    *   **UI:** Add "Custom Item" button (floated or first grid item).
    *   **UI:** Add `IconButton` in TopBar to toggle `isGridView` state.
    *   **Logic:** Handle non-catalog items in Cart Logic.

### 4. Checkout Screen (`feature:checkout`)
*   **Gap:** Customer tracking.
*   **Web Context:** "Pesanan atas nama siapa?" is the standard QSR question.
*   **Android State:** Logic exists in backend (likely), but UI field is missing.
*   **Recommendation:**
    *   Add `OutlinedTextField` for `customerName` at the top of Checkout form.
    *   Validate input (optional or mandatory depending on config).

### 5. Reports (`feature:reports`)
*   **Gap:** Granularity and AI.
*   **Web Context:** Owner wants to see "Last Month" or "Custom Range". They also use AI to query "Kenapa omzet turun?".
*   **Android State:** Fixed "Today" and "Weekly" views.
*   **Recommendation:**
    *   Add `FilterChipRow` for time ranges.
    *   Add `FloatingActionButton` "Tanya AI" that opens a bottom sheet chat interface.

---

## 📅 Implementation Roadmap

### Phase 1: Core Flow Fixes (Immediate)
1.  **Refactor `HomeScreen`**: Transform into Dashboard.
2.  **Implement `StoreSession`**: Create data models and UI for Open/Close Store.
3.  **Update `LoginScreen`**: Add Branch ID handling.

### Phase 2: Transaction Enrichment
1.  **Update `CheckoutScreen`**: Add Customer Name.
2.  **Update `PosScreen`**: Add Custom Item & View Toggles.

### Phase 3: Advanced Features
1.  **Update `ReportScreen`**: Add Filtering & AI Integration.

---

**Sign-off Required**:
Please confirm this specification matches your expectation of "1:1 Parity".

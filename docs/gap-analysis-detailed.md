# ZatiarasPOS - Detailed UI/UX Parity Analysis

**Goal**: Complete 1:1 feature matches between Web App (Reference) and Android App (Target).
**Strategy**: Correcting the Android App screen-by-screen to mimic the established Web App flow.

---

## 1. Login Page (`/login`)
**Target Android Screen**: `LoginScreen.kt`

| Component | Web App Implementation | Android App Current State | Gap / Action Required |
| :--- | :--- | :--- | :--- |
| **Branch Selection** | **Dropdown** "Pilih Cabang" (Samarinda, Berau, etc.) visible *before* login. | **Missing**. Only Username/Password fields. | **Add Dropdown**. User must select Branch ID before authenticating. |
| **Validation** | Rate limiting, visual shake on error. | Basic Snackbar error. | **Add Visual Feedback**. Improve error states. |
| **Loading State** | Lottie Animation. | Circular Progress Indicator. | **(Optional)** Add Lottie for premium feel. |

---

## 2. Dashboard / Home (`/`)
**Target Android Screen**: `HomeScreen.kt`

| Component | Web App Implementation | Android App Current State | Gap / Action Required |
| :--- | :--- | :--- | :--- |
| **Store Control** | **Big FAB/Button** "Buka Toko". Shows "Tutup Toko" if open. | **Missing**. No session control. | **Implement Logic**. `StoreSession` must gate features. Show Status Card. |
| **Metrics (Top)** | **Cards**: Omzet, Transaksi, Profit, Item Terjual (Realtime). | **Missing**. Currently only a menu grid. | **Add Metric Cards**. Move data fetch from Reports to Home. |
| **Chart** | **Bar Chart**: Pendapatan 7 Hari Terakhir. | **Missing**. | **Add Chart**. Use Vico/MPAndroidChart to show weekly trend. |
| **Navigation** | **Bottom Nav** + Quick Actions. | **Menu Grid** (Launcher Style). | **Redesign Layout**. Switch to Dashboard layout (Metrics up top, Actions below). |

---

## 3. POS Flow (`/pos`)
**Target Android Screen**: `PosScreen.kt`

| Component | Web App Implementation | Android App Current State | Gap / Action Required |
| :--- | :--- | :--- | :--- |
| **Session Gate** | **Blocked**. Cannot add items if "Toko Tutup". | **Open**. Always actionable. | **Add Gate**. Check `StoreSession` before allowing `AddToCart`. |
| **View Toggle** | **Grid / List** toggle button. | **Grid Only**. | **Add Toggle**. Allow textual list for faster scanning. |
| **Custom Item** | **"Tambah Manual"** button for ad-hoc items. | **Missing**. | **Add Feature**. Dialog to input Name + Price manually. |
| **Search** | Debounced search + Category Chips. | Search + Category Chips. | ✅ **Match** (Already implemented). |
| **Cart Interaction** | **Floating Bottom Bar** (Mobile) / Sidebar (Desktop). | **Sidebar/Button**. | **Refine Mobile UX**. Ensure Cart summary is persistent at bottom. |

---

## 4. Checkout Flow (`/pos/bayar`)
**Target Android Screen**: `CheckoutScreen.kt`

| Component | Web App Implementation | Android App Current State | Gap / Action Required |
| :--- | :--- | :--- | :--- |
| **Customer Info** | **Input "Nama Pelanggan"** (Mandatory/Optional). | **Missing**. Only "Catatan". | **Add Input**. Top of form: "Nama Pelanggan". |
| **Money Input** | **Numpad + Quick Templates** (10k, 20k, 50k, Uang Pas). | **Basic Input** + Simple Templates. | **Enhance UI**. Replicate the "Calculator" style numpad if possible. |
| **QRIS Flow** | Warning Modal "Pastikan masuk!". | Standard selection. | **Add Warning**. Confirmation dialog for QRIS. |
| **Receipt** | Intent to generic printer app. | Native Bluetooth Manager. | ✅ **Better**. Keep Android native implementation (faster). |
| **Success State** | Transaction Summary Modal. | Success Screen. | ✅ **Match**. |

---

## 5. Cash Record / Buku Kas (`/catat`)
**Target Android Screen**: `CashRecordScreen.kt`

| Component | Web App Implementation | Android App Current State | Gap / Action Required |
| :--- | :--- | :--- | :--- |
| **Mode Switch** | **Big Toggle**: Pemasukan (Green/Pink) vs Pengeluaran (Red). | Standard Tab/Selector. | **Style Match**. Make the toggle prominent and color-coded. |
| **Cash Drawer** | Checkbox "Masuk Laci Kasir?" (Affects closing balance). | **Missing**. | **Add Checkbox**. Logic to link with Store Session balance. |
| **Category** | Dropdown/Chips (Pendapatan Usaha, Beban, Lainnya). | Text/Dropdown. | **Enhance UI**. Use Chips for quick selection. |

---

## 6. Reports (`/laporan`)
**Target Android Screen**: `ReportDashboardScreen.kt`

| Component | Web App Implementation | Android App Current State | Gap / Action Required |
| :--- | :--- | :--- | :--- |
| **Filters** | **Chips**: Harian, Mingguan, Bulanan, Tahunan. | **Hardcoded** sections. | **Add Filters**. Dynamic date range query. |
| **PnL View** | Top Card: Pendapatan - Beban = Profit. | Sectioned Cards. | **Refine Layout**. mimic the "Statement" look of PnL. |
| **AI Integration** | **Floating Action Button** -> Chat Interface. | **Missing**. | **Add Feature**. Integration with OpenAI/Gemini wrapper. |

---

## 7. Settings (`/pengaturan`)
**Target Android Screen**: `SettingsScreen.kt`

| Component | Web App Implementation | Android App Current State | Gap / Action Required |
| :--- | :--- | :--- | :--- |
| **Layout** | **Grid Menu** (Large Icons). | **Vertical List**. | **Redesign**. Switch to Grid for consistency, or keep list (Android preference). *Decision: Keep List is better for Android, but Grid matches Web.* |
| **Stored Items** | "Manajemen Menu" (CRUD Produk). | "Inventory" is separate menu. | ✅ **Acceptable**. Inventory is a top-level feature in Android. |
| **Printer** | "Draft Struk" preview. | Bluetooth Printer Settings. | ✅ **Better**. Android has real printer settings. |

---

## 📝 Execution Plan (Sequential)

1.  **Phase 1: Session & Home (The Foundation)**
    *   Create `StoreSession` functional requirements.
    *   Remodel `HomeScreen` to include Dashboard Metrics & "Buka Toko".
    *   Gate `PosScreen` based on session.

2.  **Phase 2: Authentication & Multi-Branch**
    *   Update `LoginScreen` to fetch and select specific Branch ID.

3.  **Phase 3: Transaction Completeness**
    *   Add Customer Name to `CheckoutScreen`.
    *   Add Custom Item & View Toggle to `PosScreen`.
    *   Enhance `CashRecordScreen`.

4.  **Phase 4: Analytics & AI**
    *   Implement Date Filters in `ReportScreen`.
    *   Add AI Chat UI.

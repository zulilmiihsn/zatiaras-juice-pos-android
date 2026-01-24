# ZatiarasPOS - API & Data Contract Specification

> **Version**: 1.0.0
> **Backend**: Supabase (PostgreSQL)
> **Last Updated**: 2026-01-08

---

## 1. Standard Response Format

All network responses from Supabase should be handled with this pattern:

### Success Response (Kotlin Representation)

```kotlin
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val code: String, val message: String, val details: Any? = null) : ApiResult<Nothing>()
    object Loading : ApiResult<Nothing>()
}
```

### Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `AUTH_REQUIRED` | 401 | User not authenticated |
| `FORBIDDEN` | 403 | User lacks permission |
| `NOT_FOUND` | 404 | Resource not found |
| `VALIDATION_ERROR` | 400 | Invalid input data |
| `CONFLICT` | 409 | Resource already exists |
| `SERVER_ERROR` | 500 | Internal server error |
| `NETWORK_ERROR` | - | No internet connection |
| `TIMEOUT` | - | Request timed out |

---

## 2. Authentication (Supabase Auth)

### Sign In

- **Supabase SDK**: `supabase.auth.signInWithPassword(email, password)`
- **Response**: `UserSession` with access token and refresh token
- **Local Storage**: Tokens stored in `EncryptedDataStore`

### Sign Out

- **Supabase SDK**: `supabase.auth.signOut()`
- **Side Effect**: Clear all local tokens

### Get Current User

- **Supabase SDK**: `supabase.auth.currentUserOrNull()`
- **Response**: `User?` object

---

## 3. Database Schema (Supabase Tables)

### 3.1 `pengguna` (User Accounts & Profiles) ✅ IMPLEMENTED

> **Note**: This table replaces the standard `auth.users` + `profil` pattern to support **Offline Authentication**. It stores hashed passwords directly in the public schema to allow syncing to local devices.

| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID (PK) | User ID |
| `username` | TEXT (Unique) | Username for login |
| `password_hash` | TEXT | **PBKDF2** hash (`salt:hash` format) or legacy SHA-256 |
| `display_name` | TEXT | Full name of the user |
| `role` | TEXT | `kasir` \| `pemilik` |
| `is_active` | BOOLEAN | Account status |
| `created_at` | TIMESTAMP | Account creation date |
| `updated_at` | TIMESTAMP | Last update |

### 3.2 `kategori` (Categories)

| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID (PK) | Category ID |
| `name` | TEXT | Category name |
| `icon` | TEXT | Icon identifier (optional) |
| `sort_order` | INT | Display order |
| `created_at` | TIMESTAMP | Creation date |
| `updated_at` | TIMESTAMP | Last update |

### 3.3 `produk` (Products)

| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID (PK) | Product ID |
| `name` | TEXT | Product name |
| `price` | BIGINT | Price in IDR (no decimals) |
| `kategori_id` | UUID (FK) | Category reference |
| `gambar` | TEXT | Image URL (Supabase Storage) |
| `deskripsi` | TEXT | Description (optional) |
| `is_active` | BOOLEAN | Is product available |
| `created_at` | TIMESTAMP | Creation date |
| `updated_at` | TIMESTAMP | Last update |

### 3.4 `tambahan` (Add-Ons/Toppings)

| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID (PK) | Add-on ID |
| `name` | TEXT | Add-on name |
| `price` | BIGINT | Additional price |
| `is_active` | BOOLEAN | Is add-on available |
| `created_at` | TIMESTAMP | Creation date |

### 3.5 `buku_kas` (Cash Book / Transactions)

| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID (PK) | Transaction ID |
| `transaction_id` | TEXT | Grouping ID for POS transactions |
| `tipe` | TEXT | `in` (income) \| `out` (expense) |
| `jenis` | TEXT | `pendapatan_usaha` \| `beban_usaha` \| `lainnya` |
| `sumber` | TEXT | `pos` \| `catat` (manual entry) |
| `description` | TEXT | Transaction description |
| `amount` | BIGINT | Amount in IDR |
| `payment_method` | TEXT | `tunai` \| `qris` \| `transfer` |
| `waktu` | TIMESTAMP | Transaction time |
| `created_at` | TIMESTAMP | Record creation time |

### 3.6 `transaksi_item` (POS Transaction Items) ✅ IMPLEMENTED

| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID (PK) | Item ID |
| `transaksi_id` | UUID (FK) | Reference to transaksi |
| `produk_id` | UUID (FK) | Product reference |
| `produk_name` | TEXT | Product name (snapshot) |
| `produk_price` | BIGINT | Product price (snapshot) |
| `quantity` | INT | Quantity |
| `subtotal` | BIGINT | Line item total |
| `notes` | TEXT | Special instructions |
| `created_at` | TIMESTAMP | Creation time |

### 3.7 `pengaturan` (Settings) ✅ IMPLEMENTED

> **Note**: This table syncs across all devices for consistent settings.

| Column | Type | Description |
|--------|------|-------------|
| `id` | TEXT (PK) | Branch ID or "default" (singleton) |
| `owner_pin` | TEXT | Owner PIN hash (SHA-256) for kasir access control |
| `locked_routes` | TEXT[] | Routes that require owner PIN for kasir |
| `store_name` | TEXT | Store name for receipt header |
| `store_address` | TEXT | Store address for receipt |
| `store_phone` | TEXT | Store phone for receipt |
| `default_paper_width` | INT | Default thermal paper width (58 or 80 mm) |
| `receipt_footer` | TEXT | Custom receipt footer message |
| `show_logo_on_receipt` | BOOLEAN | Whether to show logo on receipt |
| `updated_at` | BIGINT | Unix timestamp for sync |

### 3.8 `tambahan` (Add-Ons/Toppings) ✅ IMPLEMENTED

| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID (PK) | Add-on ID |
| `nama` | TEXT | Add-on name (e.g., "Extra Cheese") |
| `harga` | BIGINT | Additional price in IDR |
| `kategori` | TEXT | Category (e.g., "Topping", "Size") |
| `sort_order` | INT | Display order |
| `icon` | TEXT | Icon identifier (optional) |
| `is_active` | BOOLEAN | Is add-on available |
| `created_at` | TIMESTAMP | Creation date |
| `updated_at` | BIGINT | Unix timestamp for sync |

---

## 4. API Endpoints (Ktor Client → Supabase)

### 4.1 Products

#### Get All Products
```kotlin
// Supabase Query
supabase.from("produk")
    .select("*, kategori(*)")
    .eq("is_active", true)
    .order("created_at", Order.DESCENDING)
```

#### Get Product by ID
```kotlin
supabase.from("produk")
    .select("*, kategori(*), tambahan(*)")
    .eq("id", productId)
    .single()
```

#### Create Product
```kotlin
supabase.from("produk")
    .insert(ProductInsertDto)
```

#### Update Product
```kotlin
supabase.from("produk")
    .update(ProductUpdateDto)
    .eq("id", productId)
```

#### Delete Product (Soft Delete)
```kotlin
supabase.from("produk")
    .update(mapOf("is_active" to false))
    .eq("id", productId)
```

---

### 4.2 Categories

#### Get All Categories
```kotlin
supabase.from("kategori")
    .select("*")
    .order("sort_order", Order.ASCENDING)
```

---

### 4.3 Transactions

#### Create POS Transaction
```kotlin
// 1. Insert to buku_kas
val bukuKas = supabase.from("buku_kas")
    .insert(BukuKasInsertDto)
    .select()
    .single()

// 2. Insert line items to transaksi_kasir
supabase.from("transaksi_kasir")
    .insert(transactionItems.map { it.copy(buku_kas_id = bukuKas.id) })
```

#### Get Daily Transactions
```kotlin
supabase.from("buku_kas")
    .select("*, transaksi_kasir(*)")
    .gte("waktu", startOfDayUtc)
    .lte("waktu", endOfDayUtc)
    .eq("sumber", "pos")
```

---

### 4.4 Reports

#### Get Dashboard Stats (Today)
```kotlin
// Get today's transactions
supabase.from("buku_kas")
    .select("amount, tipe, transaction_id")
    .gte("waktu", startOfDayUtc)
    .lte("waktu", endOfDayUtc)
    .eq("sumber", "pos")
```

#### Get Best Sellers (7 Days)
```kotlin
supabase.from("transaksi_kasir")
    .select("produk_id, qty, produk(name, gambar)")
    .gte("created_at", sevenDaysAgoUtc)
```
*Aggregation done client-side or via RPC function*

---

## 4.5 Error Codes per Endpoint

### Authentication Errors

| Error Code | Cause | Handling |
|------------|-------|----------|
| `AUTH_INVALID_CREDENTIALS` | Wrong email/password | Show "Email atau password salah" |
| `AUTH_USER_NOT_FOUND` | Email not registered | Show "Akun tidak ditemukan" |
| `AUTH_SESSION_EXPIRED` | Token expired | Auto-refresh or redirect to login |
| `AUTH_NETWORK_ERROR` | No internet during login | Show "Tidak ada koneksi internet" |

### Product Operations Errors

| Error Code | Cause | Handling |
|------------|-------|----------|
| `PRODUCT_NOT_FOUND` | Product ID doesn't exist | Remove from local cache |
| `PRODUCT_DUPLICATE_NAME` | Name already exists | Show "Nama produk sudah ada" |
| `PRODUCT_INVALID_PRICE` | Price <= 0 | Validate before submit |
| `PRODUCT_IMAGE_TOO_LARGE` | Image > 5MB | Compress or reject |
| `PRODUCT_CATEGORY_INVALID` | Category ID not found | Refresh categories |

### Transaction Errors

| Error Code | Cause | Handling |
|------------|-------|----------|
| `TXN_EMPTY_CART` | No items in cart | Validate before checkout |
| `TXN_INVALID_AMOUNT` | Amount mismatch | Recalculate total |
| `TXN_PRODUCT_UNAVAILABLE` | Product deactivated | Remove from cart, show warning |
| `TXN_SYNC_CONFLICT` | Server has newer version | Use server version (last-write-wins) |
| `TXN_OFFLINE_QUEUED` | No internet, saved locally | Show "Transaksi disimpan offline" |

### Sync Errors

| Error Code | Cause | Handling |
|------------|-------|----------|
| `SYNC_NETWORK_ERROR` | No internet | Retry with exponential backoff |
| `SYNC_QUOTA_EXCEEDED` | Supabase limit hit | Queue for later, notify user |
| `SYNC_PARSE_ERROR` | Invalid response format | Log error, skip item |
| `SYNC_CONFLICT` | Data conflict | Apply conflict resolution |
| `SYNC_PARTIAL_FAILURE` | Some items failed | Retry failed items only |

### Image Upload Errors

| Error Code | Cause | Handling |
|------------|-------|----------|
| `IMG_TOO_LARGE` | File > 5MB | Compress to 80% quality |
| `IMG_INVALID_FORMAT` | Not JPEG/PNG/WebP | Show "Format tidak didukung" |
| `IMG_UPLOAD_FAILED` | Storage error | Retry 3x, then fail gracefully |
| `IMG_BUCKET_FULL` | Storage quota exceeded | Alert admin |

---

## 5. Delta Sync Protocol

### Purpose
Minimize bandwidth and Supabase quota usage by only fetching changed data.

### Implementation

#### Request
```kotlin
// Android sends last sync timestamp
supabase.from("produk")
    .select("*")
    .gt("updated_at", lastSyncTimestamp)
```

#### Response
Only rows where `updated_at > lastSyncTimestamp` are returned.

#### Update Local State
```kotlin
// After successful sync
dataStore.setLastSyncTimestamp(Clock.System.now().toString())
```

### Sync Flow Diagram
```
┌─────────────────────────────────────────────────────┐
│  App Start / Pull-to-Refresh                        │
│                     ↓                               │
│  Read lastSyncTimestamp from DataStore              │
│                     ↓                               │
│  Query: SELECT * FROM x WHERE updated_at > ?        │
│                     ↓                               │
│  Upsert returned rows to Room Database              │
│                     ↓                               │
│  Update lastSyncTimestamp = now()                   │
└─────────────────────────────────────────────────────┘
```

---

## 6. Offline Transaction Upload

### Queue Structure (Room Entity)

```kotlin
@Entity(tableName = "pending_sync")
data class PendingSyncEntity(
    @PrimaryKey val id: String,
    val tableName: String,        // "buku_kas", "transaksi_kasir"
    val operation: String,        // "INSERT", "UPDATE", "DELETE"
    val payload: String,          // JSON serialized data
    val createdAt: Long,
    val retryCount: Int = 0,
    val lastError: String? = null
)
```

### Upload Flow
```
┌─────────────────────────────────────────────────────┐
│  WorkManager Trigger (Network Available)            │
│                     ↓                               │
│  Query: SELECT * FROM pending_sync ORDER BY createdAt│
│                     ↓                               │
│  For each item:                                     │
│    - Execute Supabase mutation                      │
│    - On success: DELETE from pending_sync           │
│    - On failure: INCREMENT retryCount               │
│                     ↓                               │
│  Notify UI of sync status                          │
└─────────────────────────────────────────────────────┘
```

---

## 7. Image Upload (Supabase Storage)

### Upload Flow
```kotlin
// 1. Compress image
val compressed = imageCompressor.compress(bitmap, quality = 80)

// 2. Upload to Storage
val path = "products/${UUID.randomUUID()}.webp"
supabase.storage.from("images").upload(path, compressed)

// 3. Get public URL
val publicUrl = supabase.storage.from("images").publicUrl(path)

// 4. Save URL to product record
```

### Storage Bucket: `images`
- **Public**: Yes (for product images)
- **Max Size**: 5MB
- **Allowed Types**: image/jpeg, image/png, image/webp

---

## 8. AI Gateway (BFF Pattern)

### Endpoint
Supabase Edge Function: `/functions/v1/ai-process`

### Request
```json
{
  "action": "smart_input",
  "payload": {
    "text": "Beli gas 20rb"
  }
}
```

### Response
```json
{
  "success": true,
  "data": {
    "type": "expense",
    "category": "beban_usaha",
    "description": "Beli gas",
    "amount": 20000
  }
}
```

### Security
- OpenAI API Key stored in Edge Function environment (never in APK)
- Request authenticated via Supabase Auth token

---

## 9. Multi-Branch Configuration

| Branch | Environment Variable | Description |
|--------|---------------------|-------------|
| Samarinda | `SUPABASE_URL_SAMARINDA` | Main branch |
| Berau | `SUPABASE_URL_BERAU` | Secondary |
| Balikpapan | `SUPABASE_URL_BALIKPAPAN` | Secondary |
| Samarinda 2 | `SUPABASE_URL_SAMARINDA2` | New branch |

### Implementation
Branch config injected via Hilt at app startup based on user selection during login.

---

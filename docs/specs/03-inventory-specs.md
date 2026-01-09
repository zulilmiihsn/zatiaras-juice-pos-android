# 📋 Specs: Inventory Management Module

> **Status**: 🟡 Active
> **Last Updated**: 2026-01-09

---

## 1. Database Schema (Room Entities)

### 1.1 CategoryEntity

```kotlin
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val icon: String? = null,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
```

### 1.2 ProductEntity

```kotlin
@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["categoryId"])]
)
data class ProductEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val price: Long,                    // IDR, no decimals
    val categoryId: String? = null,
    val imageUrl: String? = null,
    val description: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
```

### 1.3 ProductFtsEntity (Full-Text Search)

```kotlin
@Fts4(contentEntity = ProductEntity::class)
@Entity(tableName = "products_fts")
data class ProductFtsEntity(
    val name: String,
    val description: String?
)
```

---

## 2. DAO Interfaces

### 2.1 CategoryDao

```kotlin
@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC")
    fun getAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}
```

### 2.2 ProductDao

```kotlin
@Dao
interface ProductDao {
    @Query("""
        SELECT * FROM products 
        WHERE isActive = 1 
        ORDER BY createdAt DESC
    """)
    fun getAllActive(): Flow<List<ProductEntity>>

    @Query("""
        SELECT * FROM products 
        WHERE categoryId = :categoryId AND isActive = 1
        ORDER BY createdAt DESC
    """)
    fun getByCategory(categoryId: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)

    @Update
    suspend fun update(product: ProductEntity)

    @Query("UPDATE products SET isActive = 0, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE products SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)

    @Query("SELECT * FROM products WHERE isSynced = 0")
    suspend fun getUnsynced(): List<ProductEntity>

    // FTS Search
    @Query("""
        SELECT products.* FROM products
        JOIN products_fts ON products.rowid = products_fts.rowid
        WHERE products_fts MATCH :query AND products.isActive = 1
        ORDER BY products.createdAt DESC
    """)
    fun search(query: String): Flow<List<ProductEntity>>
}
```

---

## 3. Domain Models

### 3.1 Category

```kotlin
data class Category(
    val id: String,
    val name: String,
    val icon: String? = null,
    val sortOrder: Int = 0
)
```

### 3.2 Product

```kotlin
data class Product(
    val id: String,
    val name: String,
    val price: Long,
    val category: Category? = null,
    val imageUrl: String? = null,
    val description: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long
)
```

---

## 4. UI States

### 4.1 InventoryUiState (List)

```kotlin
sealed interface InventoryUiState {
    data object Loading : InventoryUiState
    
    data class Success(
        val products: List<Product>,
        val categories: List<Category>,
        val selectedCategoryId: String? = null,
        val searchQuery: String = ""
    ) : InventoryUiState
    
    data class Error(val message: String) : InventoryUiState
    
    data object Empty : InventoryUiState
}
```

### 4.2 ProductDetailUiState (CRUD Form)

```kotlin
sealed interface ProductDetailUiState {
    data object Loading : ProductDetailUiState
    
    data class Form(
        val id: String? = null,           // null = create mode
        val name: String = "",
        val price: String = "",
        val categoryId: String? = null,
        val imageUri: Uri? = null,
        val description: String = "",
        val categories: List<Category> = emptyList(),
        val isSubmitting: Boolean = false,
        val nameError: String? = null,
        val priceError: String? = null
    ) : ProductDetailUiState
    
    data class Success(val message: String) : ProductDetailUiState
    
    data class Error(val message: String) : ProductDetailUiState
}
```

---

## 5. Supabase Queries

### 5.1 Get Products (Delta Sync)

```kotlin
supabase.from("produk")
    .select("*, kategori(*)")
    .gt("updated_at", lastSyncTimestamp)
    .order("created_at", Order.DESCENDING)
```

### 5.2 Create Product

```kotlin
supabase.from("produk")
    .insert(ProductInsertDto(
        id = UUID.randomUUID().toString(),
        name = name,
        price = price,
        kategori_id = categoryId,
        gambar = imageUrl,
        deskripsi = description,
        is_active = true
    ))
```

### 5.3 Update Product

```kotlin
supabase.from("produk")
    .update(ProductUpdateDto(
        name = name,
        price = price,
        kategori_id = categoryId,
        gambar = imageUrl,
        deskripsi = description,
        updated_at = Clock.System.now().toString()
    ))
    .eq("id", productId)
```

### 5.4 Delete Product (Soft)

```kotlin
supabase.from("produk")
    .update(mapOf("is_active" to false))
    .eq("id", productId)
```

---

## 6. Image Upload Flow

```
┌─────────────────────────────────────────────────────────────────┐
│  User selects image (Camera/Gallery)                            │
│                         ↓                                        │
│  Compress to WebP (max 1MB, quality 80%)                        │
│                         ↓                                        │
│  Generate filename: products/{uuid}.webp                        │
│                         ↓                                        │
│  Upload to Supabase Storage bucket "images"                     │
│                         ↓                                        │
│  Get public URL                                                  │
│                         ↓                                        │
│  Save URL to ProductEntity.imageUrl                             │
└─────────────────────────────────────────────────────────────────┘
```

---

## 7. Validation Rules

| Field | Rule | Error Message |
|-------|------|---------------|
| `name` | Required, min 2 chars | "Nama produk minimal 2 karakter" |
| `price` | Required, > 0 | "Harga harus lebih dari 0" |
| `categoryId` | Optional | - |
| `image` | Optional, max 5MB | "Ukuran gambar maksimal 5MB" |
| `description` | Optional, max 500 chars | "Deskripsi maksimal 500 karakter" |

---

## 8. Acceptance Criteria

### AC-1: Product List
- [ ] Products display in grid (2 columns)
- [ ] Each card shows: image, name, price, category badge
- [ ] Loading shimmer while fetching
- [ ] Empty state when no products
- [ ] Pull-to-refresh triggers delta sync

### AC-2: Category Filter
- [ ] Horizontal scrollable category chips
- [ ] "Semua" chip shows all products
- [ ] Selecting category filters list immediately

### AC-3: Search
- [ ] Search bar at top
- [ ] Results update as user types (debounced 300ms)
- [ ] Typo-tolerant via FTS4
- [ ] Empty search = show all

### AC-4: Create Product
- [ ] FAB opens create form
- [ ] Form validates on submit
- [ ] Shows loading during save
- [ ] Success navigates back with snackbar
- [ ] Works offline (saves to Room, queues sync)

### AC-5: Edit Product
- [ ] Tap card opens detail form
- [ ] Pre-fills existing data
- [ ] Image preview with change option
- [ ] Delete button with confirmation dialog

### AC-6: Offline Behavior
- [ ] All CRUD works without internet
- [ ] Sync status indicator (optional)
- [ ] Auto-sync when connection restored

---

## 9. Screen Wireframes

### Inventory List Screen
```
┌─────────────────────────────────────┐
│ ← Inventory                    🔍   │
├─────────────────────────────────────┤
│ [🔍 Cari produk...              ]   │
├─────────────────────────────────────┤
│ [Semua] [Kopi] [Teh] [Snack] ►      │
├─────────────────────────────────────┤
│ ┌──────────┐  ┌──────────┐         │
│ │  [IMG]   │  │  [IMG]   │         │
│ │ Es Kopi  │  │ Teh Manis│         │
│ │ Rp15.000 │  │ Rp10.000 │         │
│ └──────────┘  └──────────┘         │
│ ┌──────────┐  ┌──────────┐         │
│ │  [IMG]   │  │  [IMG]   │         │
│ │ Roti     │  │ Kentang  │         │
│ │ Rp8.000  │  │ Rp12.000 │         │
│ └──────────┘  └──────────┘         │
│                                     │
│                              [+]    │
└─────────────────────────────────────┘
```

### Product Detail Screen
```
┌─────────────────────────────────────┐
│ ← Tambah Produk              [🗑️]  │
├─────────────────────────────────────┤
│                                     │
│   ┌───────────────────────────┐     │
│   │                           │     │
│   │      [📷 Foto Produk]     │     │
│   │                           │     │
│   └───────────────────────────┘     │
│                                     │
│   Nama Produk *                     │
│   ┌───────────────────────────┐     │
│   │ Es Kopi Susu              │     │
│   └───────────────────────────┘     │
│                                     │
│   Harga (Rp) *                      │
│   ┌───────────────────────────┐     │
│   │ 15000                     │     │
│   └───────────────────────────┘     │
│                                     │
│   Kategori                          │
│   ┌───────────────────────────┐     │
│   │ Kopi                    ▼ │     │
│   └───────────────────────────┘     │
│                                     │
│   Deskripsi                         │
│   ┌───────────────────────────┐     │
│   │ Kopi susu dengan es...    │     │
│   └───────────────────────────┘     │
│                                     │
│   ┌───────────────────────────┐     │
│   │        SIMPAN              │     │
│   └───────────────────────────┘     │
└─────────────────────────────────────┘
```

---

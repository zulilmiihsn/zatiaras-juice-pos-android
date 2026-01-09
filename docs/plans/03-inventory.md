# 📦 Plan: Inventory Management Module

> **Status**: 🟡 In Progress
> **Priority**: High
> **Phase**: 3 of 8
> **Sprint**: 5-6

---

## 1. Why Are We Building This?

Inventory Management adalah **core feature** untuk:
- Owner dapat mengelola menu (CRUD products)
- Kasir dapat melihat catalog products
- Support offline-first product search
- Foundation untuk POS feature (Phase 4)

---

## 2. High-Level Goals

| Goal | Description |
|------|-------------|
| **Product CRUD** | Create, Read, Update, Delete products |
| **Categories** | Organize products by category |
| **Offline Search** | Room FTS4 untuk fast typo-tolerant search |
| **Image Upload** | Camera/gallery → Supabase Storage |
| **Delta Sync** | Only fetch changed products |

---

## 3. Architecture Approach

### Data Flow (Offline-First)
```
UI ──┬──> ViewModel ──> Repository ──┬──> LocalDataSource (Room) ──> SQLite
     │                               │
     │                               └──> RemoteDataSource (Supabase) ──> PostgreSQL
     │
     └── ALWAYS reads from Room (Single Source of Truth)
```

### Sync Strategy
1. On app start: Delta sync (fetch `updated_at > lastSync`)
2. On create/update: Save to Room → Queue for Supabase sync
3. On delete: Soft delete (set `is_active = false`)

---

## 4. Module Structure

```
:feature:inventory/
├── data/
│   ├── local/
│   │   ├── entity/
│   │   │   ├── ProductEntity.kt
│   │   │   └── CategoryEntity.kt
│   │   ├── dao/
│   │   │   ├── ProductDao.kt
│   │   │   └── CategoryDao.kt
│   │   └── InventoryLocalDataSource.kt
│   ├── remote/
│   │   └── InventoryRemoteDataSource.kt
│   ├── mapper/
│   │   └── ProductMapper.kt
│   └── repository/
│       └── ProductRepositoryImpl.kt
├── domain/
│   ├── model/
│   │   ├── Product.kt
│   │   └── Category.kt
│   ├── repository/
│   │   └── ProductRepository.kt
│   └── usecase/
│       ├── GetProductsUseCase.kt
│       ├── CreateProductUseCase.kt
│       ├── UpdateProductUseCase.kt
│       └── DeleteProductUseCase.kt
├── presentation/
│   ├── list/
│   │   ├── InventoryScreen.kt
│   │   ├── InventoryViewModel.kt
│   │   └── InventoryUiState.kt
│   ├── detail/
│   │   ├── ProductDetailScreen.kt
│   │   ├── ProductDetailViewModel.kt
│   │   └── ProductDetailUiState.kt
│   └── components/
│       ├── ProductCard.kt
│       ├── CategoryChip.kt
│       └── SearchBar.kt
└── di/
    └── InventoryModule.kt
```

---

## 5. Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| **Large image uploads fail** | User frustration | Compress to WebP, max 1MB, show progress |
| **Sync conflicts** | Data inconsistency | Last-write-wins with `updated_at` timestamp |
| **FTS4 slow on large datasets** | Bad UX | Index only name + category, paginate results |
| **Out of memory (images)** | App crash | Use Coil with memory caching, no Bitmaps |

---

## 6. Dependencies (Blockers)

- [x] Room dependencies in version catalog
- [ ] Room Database setup in `:core:data`
- [ ] Supabase Storage integration
- [ ] Coil for image loading

---

## 7. Implementation Order

1. **Setup Room Database** (`:core:data`) ← Start here
2. **Create domain models** (Product, Category)
3. **Create Room entities & DAOs**
4. **Implement repository** (offline-first)
5. **Build InventoryScreen** (product list)
6. **Build ProductDetailScreen** (CRUD form)
7. **Add image upload**
8. **Implement FTS4 search**

---

## 8. Success Criteria

- [ ] Products load from Room even when offline
- [ ] CRUD operations work without internet
- [ ] Changes sync to Supabase when online
- [ ] Search is fast (<200ms) with typo tolerance
- [ ] Images display correctly with Coil
- [ ] Empty state, loading state, error state handled

---

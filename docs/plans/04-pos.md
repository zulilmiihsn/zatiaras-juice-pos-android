# 📝 Feature Plan: Point of Sales (POS) Module

**Feature Name**: Point of Sales (POS)
**Status**: 🟢 Core Complete (Sync Pending)
**Related Roadmap Item**: Phase 4

---

## 1. Context & Objective

*   **Why**: This is the core feature of the application - where cashiers process customer transactions. The POS module must be fast, reliable, and work seamlessly offline.

*   **Goal**: 
    - Cashier can browse products, add to cart, and complete transactions in under 30 seconds.
    - All transactions are saved locally first (offline-first) and synced to Supabase when online.
    - Support multiple payment methods (Cash, QRIS placeholder).

*   **User Stories**:
    - "As a Cashier, I want to quickly find products by category or search."
    - "As a Cashier, I want to add/remove items from cart easily."
    - "As a Cashier, I want to process payment and print receipt."
    - "As an Owner, I want all transactions recorded even when offline."

---

## 2. Technical Approach (The "How")

### Architecture (Clean Architecture)

```
feature/pos/
├── data/
│   ├── mapper/           # Entity <-> Domain mappers
│   └── repository/       # TransactionRepositoryImpl
├── di/
│   └── PosModule.kt      # Hilt DI bindings
├── domain/
│   ├── model/            # Cart, CartItem, Transaction models
│   └── repository/       # TransactionRepository interface
├── navigation/
│   └── PosNavigation.kt  # Compose Navigation routes
└── presentation/
    ├── catalog/          # Product grid for selection
    ├── cart/             # Cart sidebar/bottom sheet
    ├── checkout/         # Payment & completion screen
    └── components/       # Shared POS components
```

### Data Strategy

**Local (Room) - Primary Source of Truth**:
- `TransactionEntity` - completed transactions
- `TransactionItemEntity` - line items per transaction

**Cart State - In-Memory Only**:
- Cart is NOT persisted to database (intentional)
- Cart lives in ViewModel StateFlow
- If app closes, cart is cleared (normal POS behavior)

**Remote (Supabase)**:
- Sync completed transactions to cloud
- Pull transaction history for reports

### Key Dependencies
- Existing: Room, Hilt, Compose, Coil
- No new dependencies needed

---

## 3. Potential Risks & Edge Cases

- [ ] **Cart cleared unexpectedly**: ViewModel scoped to navigation graph to survive config changes
- [ ] **Offline transaction**: Must save locally first, sync later
- [ ] **Price mismatch**: Use product price at time of transaction (snapshot, not reference)
- [ ] **Large cart performance**: Lazy rendering for cart items
- [ ] **Concurrent modifications**: Single cashier device assumption (no multi-device cart)

---

## 4. Implementation Steps

### Sprint 7: Core POS UI ✅ Complete

1. [x] Create `feature/pos` module structure
2. [x] Create Cart domain models (`Cart`, `CartItem`, `CartHolder`)
3. [x] Create Transaction entities (Room) + mappers
4. [x] Create POS Catalog Screen (product grid)
5. [x] Create Cart UI component (bottom sheet)
6. [x] Implement PosViewModel with state management

### Sprint 8: Transaction Flow ✅ Complete

7. [x] Create Checkout UI (payment method selection: Cash, QRIS, Transfer)
8. [x] Create TransactionRepository (save to Room)
9. [x] Implement payment confirmation flow with validation
10. [x] Create Transaction receipt preview

### Sprint 9: Polish & Sync ✅ Complete

11. [x] Create TransactionRemoteDataSource (Supabase) - in `:core:data`
12. [x] Implement transaction sync via SyncManager (push completed transactions)
13. [x] Add "Manual Record" feature (Buku Kas) - Integrated with CashRecord
14. [ ] Performance optimization for catalog

---

## 5. Screen Flow

```
[Home] --> [POS Catalog] --> [Cart Sidebar] --> [Checkout] --> [Receipt] --> [POS Catalog]
                │                                    │
                └── [Product Detail Modal] ──────────┘
                    (variants, add-ons - future)
```

---

## 6. Success Metrics

- Transaction completion time < 30 seconds
- Zero data loss in offline mode
- Cart operations (add/remove) < 100ms response
- Catalog scroll at 60fps

---

*Created: 2026-01-09*
*Last Updated: 2026-01-09*

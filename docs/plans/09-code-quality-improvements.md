# 🔧 Code Quality Improvements Plan

> **Status**: ✅ Completed
> **Created**: 2026-01-14
> **Priority**: High (Pre-Production Polish)

---

## Overview

This plan addresses technical debt and code quality improvements identified during the deep analysis review. These changes will improve performance, maintainability, and prepare the codebase for production.

---

## Task List

| # | Task | Priority | Status | Estimated Effort |
|---|------|----------|--------|------------------|
| 1 | Fix N+1 queries in TransactionRepository | 🔴 High | ✅ Done | 1-2 hours |
| 2 | Extract DateUtils (DRY) | 🟡 Medium | ✅ Done | 30 mins |
| 3 | Split SyncManager responsibilities | 🟡 Medium | ✅ Done | 1-2 hours |
| 4 | Add pagination for product list | 🟡 Medium | ✅ Done | 1-2 hours |
| 5 | Add Compose animations | 🟢 Low | ✅ Done | 1-2 hours |
| 6 | Implement string resources | 🟢 Low | ✅ Done | 2-3 hours |

---

## Task 1: Fix N+1 Queries in TransactionRepository

### Problem
Current implementation queries transaction items inside a `map` loop, causing N+1 queries:

```kotlin
// ❌ Current (N+1 problem)
entities.map { entity ->
    val items = transactionDao.getTransactionItems(entity.id) // Called N times!
    entity.toDomain(items)
}
```

### Solution
Use Room's `@Relation` annotation with `@Embedded` to fetch transactions with items in a single query.

### Changes Required

1. **Create `TransactionWithItems` data class** (if not exists properly)
   - Location: `core/data/src/main/java/com/zatiaras/pos/core/data/local/entity/TransactionWithItems.kt`

2. **Update TransactionDao**
   - Add `@Transaction` annotated method that returns `TransactionWithItems`
   - Location: `core/data/src/main/java/com/zatiaras/pos/core/data/local/dao/TransactionDao.kt`

3. **Update TransactionRepositoryImpl**
   - Use new single-query methods
   - Location: `feature/pos/src/main/java/com/zatiaras/pos/feature/pos/data/repository/TransactionRepositoryImpl.kt`

4. **Update ReportRepositoryImpl**
   - Same fix for report queries
   - Location: `feature/reports/src/main/java/com/zatiaras/pos/feature/reports/data/repository/ReportRepositoryImpl.kt`

### Acceptance Criteria
- [ ] `TransactionWithItems` uses `@Relation` properly
- [ ] DAO methods use `@Transaction` annotation
- [ ] Repository methods no longer loop for items
- [ ] All existing tests pass

---

## Task 2: Extract DateUtils (DRY)

### Problem
Date calculation functions are duplicated in multiple repositories:
- `TransactionRepositoryImpl.getTodayRange()`
- `ReportRepositoryImpl.getStartOfDay()` / `getEndOfDay()`

### Solution
Create a `DateUtils` utility object in `core/domain` with reusable date functions.

### Changes Required

1. **Create DateUtils**
   - Location: `core/domain/src/main/java/com/zatiaras/pos/core/domain/util/DateUtils.kt`
   - Functions: `getStartOfDay()`, `getEndOfDay()`, `getTodayRange()`, `getWeekRange()`, `getMonthRange()`

2. **Update TransactionRepositoryImpl**
   - Replace local functions with DateUtils calls

3. **Update ReportRepositoryImpl**
   - Replace local functions with DateUtils calls

### Acceptance Criteria
- [ ] DateUtils contains all common date functions
- [ ] No duplicate date logic in repositories
- [ ] All existing functionality preserved

---

## Task 3: Split SyncManager Responsibilities

### Problem
`SyncManager` (252 lines) has too many responsibilities:
- Sync orchestration
- Transaction sync logic
- CashRecord sync logic
- WorkManager scheduling
- Status management

### Solution
Apply Single Responsibility Principle by extracting sync logic into dedicated syncer classes.

### Changes Required

1. **Create ISyncer interface**
   - Location: `core/data/src/main/java/com/zatiaras/pos/core/data/sync/ISyncer.kt`

2. **Create TransactionSyncer**
   - Location: `core/data/src/main/java/com/zatiaras/pos/core/data/sync/TransactionSyncer.kt`
   - Move: `syncTransactions()` logic

3. **Create CashRecordSyncer**
   - Location: `core/data/src/main/java/com/zatiaras/pos/core/data/sync/CashRecordSyncer.kt`
   - Move: `syncCashRecords()` logic

4. **Refactor SyncManager**
   - Keep only orchestration and status
   - Inject syncer instances

### Acceptance Criteria
- [ ] SyncManager under 150 lines
- [ ] Each syncer handles one entity type
- [ ] Sync functionality unchanged
- [ ] DI properly configured

---

## Task 4: Add Pagination for Product List

### Problem
Product list loads all products at once, which could cause performance issues with large catalogs.

### Solution
Implement Paging 3 library for efficient pagination.

### Changes Required

1. **Add Paging dependencies**
   - Location: `gradle/libs.versions.toml`

2. **Update ProductDao**
   - Add `PagingSource` returning method
   - Location: `core/data/src/main/java/com/zatiaras/pos/core/data/local/dao/ProductDao.kt`

3. **Update ProductRepository**
   - Add paginated method
   - Location: `feature/inventory/src/main/java/com/zatiaras/pos/feature/inventory/domain/repository/ProductRepository.kt`

4. **Update InventoryViewModel**
   - Use `cachedIn(viewModelScope)`

5. **Update InventoryScreen**
   - Use `collectAsLazyPagingItems()`

### Acceptance Criteria
- [ ] Products load in pages of 20
- [ ] Smooth scrolling with no jank
- [ ] Loading indicators for pagination
- [ ] Error handling for failed pages

---

## Task 5: Add Compose Animations

### Problem
UI lacks micro-interactions that make the app feel polished and premium.

### Solution
Add subtle animations using Compose Animation APIs.

### Target Areas

1. **Cart Item Add/Remove**
   - AnimatedVisibility with scale/fade
   - Location: `feature/pos`

2. **Loading States**
   - Shimmer effect for skeleton loading
   - Location: `core/ui`

3. **Screen Transitions**
   - Slide animations between screens
   - Location: Navigation composables

4. **Button Press Feedback**
   - Scale animation on tap
   - Location: Common button components

5. **Counter Animations**
   - AnimatedContent for quantity changes
   - Location: Cart item row

### Acceptance Criteria
- [ ] Cart items animate on add/remove
- [ ] Loading states have shimmer
- [ ] Navigation has smooth transitions
- [ ] All animations under 300ms

---

## Task 6: Implement String Resources

### Problem
Hardcoded Indonesian strings throughout the codebase make localization impossible.

### Solution
Extract all user-facing strings to `strings.xml` files.

### Changes Required

1. **Create/Update strings.xml**
   - Location: `app/src/main/res/values/strings.xml`
   - Location: `core/ui/src/main/res/values/strings.xml`
   - Location: Each feature module's `strings.xml`

2. **Update Composables**
   - Replace hardcoded strings with `stringResource(R.string.x)`

3. **Update ViewModels**
   - Return string resource IDs or sealed class instead of hardcoded messages

### Priority Strings
- Error messages
- Button labels
- Screen titles
- Empty state messages
- Validation messages

### Acceptance Criteria
- [ ] All user-facing strings in strings.xml
- [ ] Composables use stringResource()
- [ ] ViewModels return resource IDs for messages
- [ ] App functions identically

---

## Execution Order

1. **Task 2: DateUtils** (dependency for others, quick win)
2. **Task 1: N+1 Fix** (performance critical)
3. **Task 3: Split SyncManager** (architecture improvement)
4. **Task 4: Pagination** (performance for scale)
5. **Task 5: Animations** (polish)
6. **Task 6: String Resources** (localization prep)

---

## Notes

- Each task should be committed separately
- Run existing tests after each change
- Update roadmap.md after completion

# Sync Audit Report

**Date**: 2026-05-29  
**Scope**: core data syncers, remote data sources, and repository sync entry points.  
**Verdict**: operational sync is in good shape, but remote delta pull is still real tech debt for several entities.

## Current Sync Model

The app uses offline-first local writes with background/manual sync:

- Local writes happen in Room first.
- Push uploads unsynced local rows.
- Pull fetches remote rows and applies Last-Write-Wins using `updatedAt`.
- `SyncManager` runs syncers in dependency order: categories, products, transactions, cash records.

## Verified Behaviors

### Transaction Sync

Status: good, regression-tested.

Evidence:

- `TransactionSyncer` now pulls remote transactions even when there are no local unsynced transactions.
- This matters because another device or server-side process can create/update remote transactions while the current device has nothing to upload.
- Regression test: `TransactionSyncerTest.sync pulls remote transactions when no local transactions are pending`.

Remaining debt:

- `TransactionRemoteDataSource.fetchTransactionsExtended()` accepts `lastSyncTimestamp`, but the Supabase query still does a paged full pull ordered by `created_at`.
- It should eventually filter by `updated_at > lastSyncTimestamp`.

### Cash Record Sync

Status: good, regression-tested.

Evidence:

- `CashRecordRepositoryImpl.syncToRemote()` delegates to `CashRecordSyncer`.
- It no longer returns success without performing sync.
- Regression tests cover successful delegation and failed sync result propagation.

Remaining debt:

- `CashRecordRemoteDataSource.fetchCashRecords()` accepts `lastSyncTimestamp`, but still performs a full pull.
- The code comment correctly documents this as a bandwidth tradeoff.

### Auth Branch Session

Status: good, regression-tested.

Evidence:

- `AuthViewModel.login()` persists selected branch ID through `SessionPreferences.saveBranchId()`.
- Regression tests cover successful branch persistence and no branch persistence when login fails.

Remaining debt:

- Backend-backed user-role-to-branch authorization is not implemented in the login contract yet.
- The persisted branch is local session context, not authorization proof.

## Real Delta-Sync Tech Debt

These are confirmed debt items because a `lastSyncTimestamp` parameter exists or sync timestamp is tracked, but the remote query does not yet apply a remote `updated_at` filter.

| Entity | Current State | Evidence | Risk |
| --- | --- | --- | --- |
| Products | Full pull | `ProductSyncer` passes product timestamp to `InventoryRemoteDataSource.fetchProducts()`, but fetch uses plain `select()` | More bandwidth and slower sync as catalog grows |
| Transactions | Paged full pull | `TransactionSyncer` passes transaction timestamp to `fetchTransactionsExtended()`, but fetch ranges by page and orders by `created_at` | More bandwidth/CPU as transaction history grows |
| Cash records | Full pull | `CashRecordSyncer` passes cash-record timestamp to `fetchCashRecords()`, but fetch uses plain `select()` | More bandwidth as cash ledger grows |
| Add-ons | Full pull | `AddOnRepositoryImpl` passes add-on timestamp to `fetchAddOns()`, but fetch uses plain `select()` | Low to medium; add-on tables are usually small |
| Categories | Full pull | `CategorySyncer` uses `fetchCategories()` with no timestamp parameter | Low; category tables are usually small reference data |

## Recommended Fix Order

1. Transactions: highest growth risk and already paged, so add `updated_at` filtering first.
2. Products: medium/high growth risk for large catalogs.
3. Cash records: medium growth risk for long-running stores.
4. Add-ons: lower risk.
5. Categories: lowest risk, but still worth making consistent.

## Acceptance Criteria For Delta Pull

For each entity:

- Convert local millisecond timestamp to the remote timestamp format expected by Supabase/PostgREST.
- Add `updated_at > lastSyncTimestamp` filtering only when timestamp is greater than zero.
- Keep full pull when timestamp is zero.
- Keep soft-delete rows in pull results so deletes propagate.
- Add regression tests proving the remote datasource applies the expected filter path.
- Keep Last-Write-Wins merge tests at the syncer layer.


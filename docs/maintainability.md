# Maintainability Guide

Last updated: 2026-05-31

This guide is the working standard for making the whole codebase readable for senior engineers, junior engineers, and low-cost AI agents. It does not replace `rules.md`; it turns the rules into concrete review checks.

## Target State

A file is considered GREAT only when a new reader can answer these questions in under two minutes:

1. What responsibility does this file own?
2. Which layer does it belong to?
3. What inputs can change its behavior?
4. What failure paths are handled?
5. Where should a related change go next?

If those answers require scrolling through unrelated UI, data access, mapping, and business logic, the file needs refactoring.

## File Shape

Use these limits as ratchets. Files under 500 lines are acceptable when they remain cohesive and pass the quality gates. The smaller targets below are preferred edit targets, not release blockers.

| Code Shape | GREAT Target | Action When Larger |
| --- | ---: | --- |
| Route or screen shell | 150 lines | Move content sections to sibling files. |
| Compose section/component | 200 lines | Split repeated rows, headers, dialogs, and controls. |
| ViewModel | 250 lines | Move pure decisions to use cases or private reducers. |
| Repository implementation | 300 lines | Extract mapping, sync, and transaction helpers. |
| Pure utility/use case | 120 lines | Split by behavior, not by arbitrary helper groups. |

Hard cap: production Kotlin/KTS files should stay below 500 lines unless a short note explains why the file must remain larger.

## Comment Policy

Comments should explain contracts and risk, not narrate the line below them.

Good comments:

- Why a fallback exists.
- Which backend, Room, or sync invariant must stay true.
- Why a value is intentionally duplicated or cached.
- What a future TODO needs before it can be completed.

Weak comments:

- `// Header`
- `// Button`
- `// Save row`
- `// New feature`

When a comment only repeats the name of the function or component, rename the code or extract a smaller component.

## Compose Refactor Pattern

Keep a Compose screen in three layers:

1. `FeatureRoute`: connects ViewModel state and events.
2. `FeatureScreen`: owns Scaffold and top-level layout only.
3. `FeatureContent` plus small components: owns sections, cards, dialogs, and controls.

Example from settings:

- `SettingsRoute` collects state and forwards callbacks.
- `SettingsScreen` owns Scaffold and top bar.
- `SettingsContent` orders the visible sections.
- `SettingsComponents` owns reusable cards and controls.

This pattern gives AI agents small, named edit targets and reduces accidental edits in unrelated UI.

## Refactor Checklist

Before editing:

- Identify the file's single responsibility.
- Check nearby files for the local naming and state pattern.
- Keep behavior-preserving refactors separate from feature changes when possible.

During editing:

- Extract around domain concepts, not around arbitrary line counts.
- Prefer pure helper functions for parsing, formatting, and decision logic.
- Keep UI callbacks explicit; do not hide navigation or persistence inside visual components.

After editing:

- Run `spotlessCheck`, `detekt`, `test`, `lintDebug`, and `:app:assembleRelease` for broad changes.
- Re-check top file sizes.
- Search for `TODO`, `FIXME`, `HACK`, and hardcoded user-facing text in changed files.

## Current Ratchet Backlog

These are maintainability targets found during the audits:

| Area | Status | Description |
| --- | --- | --- |
| Top 7 Large Files | **COMPLETED** | Refactored `TransactionHistoryScreen.kt`, `CheckoutScreen.kt`, `ProductDetailScreen.kt`, `PrinterSettingsScreen.kt`, `LoginScreen.kt`, `SyncSettingsScreen.kt`, and `DateFilterRow.kt` following the Route -> Screen -> Content -> Components pattern. |
| Detekt Thresholds | **COMPLETED** | Cleaned up all baseline files and tightened thresholds (`LongMethod: 220`, `CyclomaticComplexMethod: 20`, `NestedBlockDepth: 6`, `TooManyFunctions` limits). |
| TooManyFunctions Suppressions | **COMPLETED** | Removed the remaining `@Suppress("TooManyFunctions")` debt from add-on/product/transaction DAOs and settings/add-on repositories by splitting DAO contracts and moving private helpers out of large classes. |
| Hardcoded UI Text Sweep | **COMPLETED** | Localized the remaining sampled hardcoded permission, custom-item, and product-option labels found during the maintainability audit. |
| No >500 production Kotlin files | **COMPLETED** | `ReceiptPreviewSection.kt` was split into a shell plus `ReceiptPreviewComponents.kt`; the current production scan has no Kotlin/KTS file above 500 lines. |
| UI & Service Hotspots | **ONGOING** | Current scan has 26 production files above the preferred 300-line target and 0 above the 500-line hard cap. Keep shrinking 300-500 line files opportunistically. |
| Settings & Chat Components | **ONGOING** | Shrink settings card files and chat composer components toward 200-line targets when editing. |

## Remaining >300-Line Production Backlog

These areas still include files above the repository's preferred size targets. They are not release blockers while they stay below 500 lines and the verification gates pass, but they are good next edit targets:

- `feature/auth/.../settings/SettingsViewModel.kt`
- `feature/inventory/.../list/AddOnComponents.kt`
- `feature/pos/.../cashrecord/AddCashRecordSheet.kt`
- `feature/inventory/.../list/InventoryScreen.kt`
- `feature/reports/.../export/PdfExportService.kt`
- `feature/reports/.../pnl/PnlReportScreen.kt`
- `feature/pos/.../cashrecord/CashRecordScreen.kt`
- `feature/reports/.../home/DashboardSections.kt`
- `feature/reports/.../home/CloseStoreDialog.kt`
- `feature/pos/.../receipt/ReceiptScreen.kt`
- `feature/pos/.../PosScreen.kt`
- `feature/inventory/.../list/EditCategoryDialog.kt`

Re-run the file-size audit before marking this backlog smaller; do not mark a hotspot completed only because it was touched.

## Detekt Exceptions

There are currently no `TooManyFunctions` suppressions in production code. Keep it that way: split contracts or extract helpers before adding a suppression.

Do not loosen the ratchet to make checks pass. If a stricter rule fails, either refactor the code or add a short documented exception with a removal plan.

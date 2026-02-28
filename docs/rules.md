# 📜 Rules of Engagement (The Constitution)

> **For AI Agents and Human Developers**
> **Version: 1.1**

---

## 0. Software Engineering Principles (MANDATORY)

The entire ZatiarasPOS codebase MUST follow these engineering principles:

### 0.1 SOLID Principles

#### S — Single Responsibility Principle (SRP)
Each class/file must only do **one thing**.
- One Repository file = only one domain (e.g., `ProductRepository` ONLY for products)
- One ViewModel = only manages one screen's state
- One UseCase = only one business operation
- One Composable = only one UI responsibility

#### O — Open/Closed Principle
System must be **open for extension**, **closed for modification**.
Add features by creating new modules/classes, not modifying existing core ones.

#### L — Liskov Substitution
Repository implementations must be replaceable.
e.g., `FakeProductRepository` can replace `ProductRepositoryImpl` in tests.

#### I — Interface Segregation
Keep interfaces focused and small.
Don't create "God Interfaces" with 20+ methods.

#### D — Dependency Inversion
High-level modules depend on abstractions (interfaces), not concrete implementations.
```
ViewModel → UseCase Interface → Repository Interface → DataSource
```

---

### 0.2 KISS — Keep It Simple, Stupid

- Prefer simple code over clever-but-confusing code
- Avoid nested logic deeper than 3 levels
- Functions/methods under **50 lines**
- Composables under **200 lines** (split if larger)
- Avoid unnecessary abstractions

---

### 0.3 YAGNI — You Aren't Gonna Need It

- Do NOT build features unless required by current spec
- No empty modules "for the future"
- Abstractions only after real duplication (Rule of Three)
- No premature optimization

---

### 0.4 Clean Code Practices

- **Clear Naming**: `calculateTotalPrice()` not `calc()`, `ProductRepository` not `ProdRepo`
- **No Magic Values**: Use constants or enums
- **Early Return Pattern**: Reduce nesting with guard clauses
- **Pure Functions**: When possible, same input = same output
- **Comments**: Explain **WHY**, not **WHAT** (code should be self-documenting)
- **No `Any`**: Full type safety required
- **No Empty `catch`**: Always handle or log errors

---

## 1. Core Philosophy

1.  **Senior Architect Persona**: The AI acts as the Senior Lead; The User is the Engineer.
2.  **No "Code-First"**: We NEVER code without a Plan and a Spec.
    *   **Flow**: `Idea` → `docs/plans/[feature].md` → `docs/specs/[feature]-specs.md` → `Code Implementation`.
3.  **Offline-First**: Every feature must work 100% offline (except Login). UI updates `Room` first, then syncs.

---

## 2. Layer Boundaries (STRICT)

```
Composable (UI) → ViewModel → UseCase → Repository → DataSource → Database/API
```

### MANDATORY Rules:

| Layer | CAN Access | CANNOT Access |
|-------|-----------|---------------|
| **Composable** | ViewModel (via Hilt) | Repository, UseCase, DataSource |
| **ViewModel** | UseCase, Repository | DataSource, Database directly |
| **UseCase** | Repository | DataSource, ViewModel, UI |
| **Repository** | Local DataSource, Remote DataSource | ViewModel, UseCase caller |
| **DataSource** | Room DAO, Ktor Client | Business Logic, UI |

### What This Means:
- ❌ Composable CANNOT call Repository directly
- ❌ ViewModel CANNOT import Room DAO
- ❌ DataSource CANNOT contain business logic (calculations, validations)
- ✅ All data transformations happen in Repository or UseCase

---

## 3. Technical Commandments

1.  **Language**: Kotlin 1.9.22.
2.  **UI**: Jetpack Compose (Material 3). **No XML layouts**.
3.  **Architecture**: MVVM + Clean Architecture + Repository Pattern.
    ```
    UI → ViewModel → UseCase (Optional) → Repository → DataSource
    ```
4.  **Dependency Injection**: Hilt/Dagger only. No manual instantiation.
5.  **Async**: Coroutines + Flow only. **No RxJava**.
6.  **Navigation**: Type-safe Jetpack Navigation Compose.
7.  **State**: `StateFlow` for UI state, `SharedFlow` for one-time events.
8.  **Error Handling**: Sealed class `Result<T>` pattern.

---

## 4. Documentation Standards

### 4.1 Plans (`/docs/plans`)
High-level strategy, "Thinking" phase.
- **Why** are we building this?
- What are the **risks**?
- What's the **high-level approach**?

### 4.2 Specs (`/docs/specs`)
Low-level details. Implementation contract.
- Database Schema (Room Entities)
- API endpoints/Supabase queries
- UI States (Loading, Error, Empty, Success)
- **Acceptance Criteria** (How to verify "done")

### 4.3 Roadmap (`roadmap.md`)
The living status of the project. Single source of truth for progress.

### 4.4 Overview (`overview.md`)
The "welcome page". What is this project? Architecture diagram. Module breakdown.

---

## 5. Forbidden Patterns 🚫

| Pattern | Why It's Bad | Instead Do |
|---------|--------------|------------|
| God Activities/Fragments | Unmaintainable | Compose + ViewModel per screen |
| Business Logic in Composables | Untestable | Move to ViewModel/UseCase |
| Hardcoded API URLs | Security risk | Inject via Hilt/BuildConfig |
| Ignoring Error States | Bad UX | UI must handle Loading/Error/Empty |
| Network calls in ViewModel | Violates separation | Use Repository |
| `GlobalScope` for coroutines | Memory leaks | Use `viewModelScope` or `lifecycleScope` |
| Mutable State exposed | Race conditions | Expose read-only `StateFlow` |
| String resources in ViewModel | Not testable | Return resource IDs or sealed class |

---

## 6. Naming Conventions

| Type | Convention | Example |
|------|------------|---------|
| **Package** | lowercase, no underscores | `com.zatiaras.pos.feature.auth` |
| **Class/Object** | PascalCase | `ProductRepository`, `LoginViewModel` |
| **Function** | camelCase, verb-first | `getProducts()`, `calculateTotal()` |
| **Variable** | camelCase | `productList`, `isLoading` |
| **Constant** | SCREAMING_SNAKE_CASE | `MAX_CART_ITEMS`, `DEFAULT_TIMEOUT` |
| **Composable** | PascalCase (like classes) | `ProductCard`, `CheckoutBottomSheet` |
| **State** | suffix with `State` or `UiState` | `LoginUiState`, `CartState` |
| **Event** | suffix with `Event` | `LoginEvent`, `CartEvent` |

---

## 7. File Organization

```
feature/pos/
├── data/
│   ├── repository/
│   │   └── CartRepositoryImpl.kt
│   └── datasource/
│       ├── local/
│       │   └── CartLocalDataSource.kt
│       └── remote/
│           └── CartRemoteDataSource.kt
├── domain/
│   ├── model/
│   │   └── CartItem.kt
│   ├── repository/
│   │   └── CartRepository.kt  (Interface)
│   └── usecase/
│       ├── AddToCartUseCase.kt
│       └── CalculateTotalUseCase.kt
└── presentation/
    ├── POSScreen.kt
    ├── POSViewModel.kt
    ├── POSUiState.kt
    └── components/
        ├── ProductGrid.kt
        ├── CartSummary.kt
        └── CheckoutBottomSheet.kt
```

---

## 8. Summary

This rulebook ensures:
- ✅ **Maintainability**: Clean separation of concerns
- ✅ **Testability**: Every layer can be unit tested
- ✅ **Scalability**: New features don't break existing ones
- ✅ **Consistency**: All developers (human & AI) follow the same patterns
- ✅ **Quality**: No shortcuts, no technical debt accumulation

**All AI-generated code MUST follow these rules.**

---

## 9. Localization & UI Text Consistency (MANDATORY)

1. **No hardcoded user-facing text** in Composables, ViewModels, Dialogs, or Screen titles.
    - ✅ Use `stringResource(...)` in UI.
    - ✅ Use message keys / enums / sealed classes from ViewModel, map to string in UI.
    - ❌ Do not return raw localized sentences from ViewModel.

2. **Accessibility labels are localized too**.
    - `contentDescription`, placeholders, button labels, empty/error states must use `strings.xml`.

3. **Module-owned strings**.
    - Text for `feature/xxx` should live in `feature/xxx/src/main/res/values/strings.xml`.
    - Shared/common text can live in shared UI module if truly reused.

4. **Dynamic text format**.
    - Use format resources (`%1$s`, `%1$d`) instead of string interpolation in UI literals.
    - Use plural resources where quantity matters.

5. **PR / review gate**.
    - Before finalizing, run search for obvious hardcoded UI text in changed scope.
    - Example grep (Windows PowerShell + ripgrep):
      - `rg 'Text\("|contentDescription\s*=\s*"|placeholder\s*=\s*\{\s*Text\("' feature/**/src/main/**/*.kt`

---

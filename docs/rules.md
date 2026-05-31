# Rules of Engagement (The Constitution)

> **For AI Agents and Human Developers**
> **Version: 1.2**

---

## 0. Software Engineering Principles (MANDATORY)

The entire ZatiarasPOS codebase MUST follow these engineering principles.

### 0.1 SOLID Principles

#### S - Single Responsibility Principle (SRP)

Each class/file must only do one thing.

- One repository file = one domain, for example `ProductRepository` only owns product behavior.
- One ViewModel = one screen or closely related screen group state.
- One UseCase = one business operation.
- One Composable = one UI responsibility.

#### O - Open/Closed Principle

System must be open for extension and closed for unsafe modification. Add features by creating focused modules/classes or small extensions to existing contracts.

#### L - Liskov Substitution

Repository implementations must be replaceable. For example, `FakeProductRepository` can replace `ProductRepositoryImpl` in tests.

#### I - Interface Segregation

Keep interfaces focused and small. Split large DAO/repository contracts by read, write, sync, report, or paging concern before adding suppressions.

#### D - Dependency Inversion

High-level modules depend on abstractions, not concrete implementations.

```text
ViewModel -> UseCase Interface -> Repository Interface -> DataSource
```

---

### 0.2 KISS - Keep It Simple

- Prefer simple code over clever-but-confusing code.
- Avoid nested logic deeper than 3 levels when possible.
- Prefer functions/methods under 50 lines.
- Prefer Composables under 200 lines.
- Production Kotlin/KTS files under 500 lines are acceptable when cohesive and quality gates pass.
- Avoid unnecessary abstractions.

---

### 0.3 YAGNI - You Aren't Gonna Need It

- Do not build features unless required by the current spec.
- No empty modules "for the future".
- Add abstractions after real duplication or clear boundary pressure.
- No premature optimization.

---

### 0.4 Clean Code Practices

- **Clear Naming**: `calculateTotalPrice()` not `calc()`, `ProductRepository` not `ProdRepo`.
- **No Magic Values**: Use constants, enums, or resource values.
- **Early Return Pattern**: Reduce nesting with guard clauses.
- **Pure Functions**: When possible, same input = same output.
- **Comments**: Explain why, not what.
- **No broad `Any` usage**: Keep domain and DTO types explicit.
- **No Empty `catch`**: Always handle or log errors.

---

## 1. Core Philosophy

1. **Senior Architect Persona**: The AI acts as the Senior Lead; the user is the engineer.
2. **Plan Before Broad Changes**: For feature work, follow `Idea -> docs/plans/[feature].md -> docs/specs/[feature]-specs.md -> Code Implementation`.
3. **Offline-First**: Every feature must work offline where possible. UI writes to Room first, then syncs.

---

## 2. Layer Boundaries (STRICT)

```text
Composable (UI) -> ViewModel -> UseCase -> Repository -> DataSource -> Database/API
```

| Layer | CAN Access | CANNOT Access |
| --- | --- | --- |
| Composable | ViewModel | Repository, UseCase, DataSource |
| ViewModel | UseCase, Repository | DataSource, Database directly |
| UseCase | Repository | DataSource, ViewModel, UI |
| Repository | Local/Remote DataSource | ViewModel, caller-specific UI |
| DataSource | Room DAO, Ktor/Supabase client | Business UI logic |

What this means:

- Composables cannot call repositories directly.
- ViewModels cannot import Room DAOs.
- DataSources cannot contain business decisions that belong in repositories/use cases.
- Data transformations happen in repositories or use cases.

---

## 3. Technical Commandments

1. **Language**: Kotlin 1.9.22.
2. **UI**: Jetpack Compose (Material 3). No XML layouts.
3. **Architecture**: MVVM + Clean Architecture + Repository Pattern.
4. **Dependency Injection**: Hilt/Dagger only. No manual graph construction.
5. **Async**: Coroutines + Flow only. No RxJava.
6. **Navigation**: Type-safe Jetpack Navigation Compose.
7. **State**: `StateFlow` for UI state, `SharedFlow` for one-time events.
8. **Error Handling**: Use the sealed/domain `Result<T>` pattern where established.

---

## 4. Documentation Standards

### 4.1 Plans (`/docs/plans`)

High-level strategy:

- Why are we building this?
- What are the risks?
- What is the high-level approach?

### 4.2 Specs (`/docs/specs`)

Implementation contract:

- Database schema and Room entities.
- API endpoints/Supabase queries.
- UI states: loading, error, empty, success.
- Acceptance criteria and verification.

### 4.3 Roadmap (`roadmap.md`)

The living status of the project.

### 4.4 Overview (`overview.md`)

The welcome page: project summary, architecture, and module breakdown.

---

## 5. Forbidden Patterns

| Pattern | Why It Is Bad | Instead Do |
| --- | --- | --- |
| God Activities/Fragments | Unmaintainable | Compose + ViewModel per screen |
| Business logic in Composables | Untestable | Move to ViewModel/UseCase |
| Hardcoded API URLs | Security risk | Inject via Hilt/BuildConfig |
| Ignoring error states | Bad UX | UI handles loading/error/empty |
| Network calls in ViewModel | Violates separation | Use Repository |
| `GlobalScope` | Memory leak risk | Use `viewModelScope` or `lifecycleScope` |
| Mutable state exposed | Race conditions | Expose read-only `StateFlow` |
| Raw localized strings in ViewModel | Not testable/localizable | Return message keys, IDs, or `UiText` |

---

## 6. Naming Conventions

| Type | Convention | Example |
| --- | --- | --- |
| Package | lowercase, no underscores | `com.zatiaras.pos.feature.auth` |
| Class/Object | PascalCase | `ProductRepository`, `LoginViewModel` |
| Function | camelCase, verb-first | `getProducts()`, `calculateTotal()` |
| Variable | camelCase | `productList`, `isLoading` |
| Constant | SCREAMING_SNAKE_CASE | `MAX_CART_ITEMS`, `DEFAULT_TIMEOUT` |
| Composable | PascalCase | `ProductCard`, `CheckoutBottomSheet` |
| State | suffix with `State` or `UiState` | `LoginUiState`, `CartState` |
| Event | suffix with `Event` | `LoginEvent`, `CartEvent` |

---

## 7. File Organization

```text
feature/pos/
  data/
    repository/
      CartRepositoryImpl.kt
    datasource/
      local/
        CartLocalDataSource.kt
      remote/
        CartRemoteDataSource.kt
  domain/
    model/
      CartItem.kt
    repository/
      CartRepository.kt
    usecase/
      AddToCartUseCase.kt
      CalculateTotalUseCase.kt
  presentation/
    POSScreen.kt
    POSViewModel.kt
    POSUiState.kt
    components/
      ProductGrid.kt
      CartSummary.kt
      CheckoutBottomSheet.kt
```

---

## 8. Summary

This rulebook protects:

- **Maintainability**: Clean separation of concerns.
- **Testability**: Each layer can be unit tested.
- **Scalability**: New features do not break existing behavior.
- **Consistency**: Humans and AI agents follow the same patterns.
- **Quality**: No hidden technical debt accumulation.

All AI-generated code must follow these rules.

---

## 9. Localization & UI Text Consistency (MANDATORY)

1. **No hardcoded user-facing text** in Composables, ViewModels, dialogs, or screen titles.
    - Use `stringResource(...)` in UI.
    - Use message keys, enums, sealed classes, or `UiText` from ViewModel and map to strings in UI.
    - Do not return raw localized sentences from ViewModel.

2. **Accessibility labels are localized too**.
    - `contentDescription`, placeholders, button labels, empty states, and error states must use string resources unless the value is user/domain data.

3. **Module-owned strings**.
    - Text for `feature/xxx` should live in `feature/xxx/src/main/res/values/strings.xml`.
    - Shared/common text can live in shared UI module if truly reused.

4. **Dynamic text format**.
    - Use format resources (`%1$s`, `%1$d`) instead of string interpolation in UI literals.
    - Use plural resources where quantity matters.

5. **Review gate**.
    - Before finalizing, search changed scope for obvious hardcoded UI text.
    - Example:

```powershell
rg 'Text\("|contentDescription\s*=\s*"|placeholder\s*=\s*\{\s*Text\("' feature/**/src/main/**/*.kt
```

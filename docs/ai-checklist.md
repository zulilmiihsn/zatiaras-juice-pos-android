# AI Agent Pre-Flight Checklist

> **Purpose**: Verification steps AI must complete before and after generating code.
> **Usage**: Run this checklist for every code generation task.

---

## Before Writing Code

### 1. Understand the Context

- [ ] Read the relevant spec in `/docs/specs/[feature]-specs.md`.
- [ ] Check the relevant plan in `/docs/plans/[feature].md`.
- [ ] Review current roadmap status in `/docs/roadmap.md`.
- [ ] Identify which phase this work belongs to.

### 2. Check Existing Patterns

- [ ] Look at similar existing code.
- [ ] Identify naming, structure, error handling, and state patterns already used.
- [ ] Check `/docs/code-templates.md` for boilerplate.

### 3. Verify Layer Boundaries

```text
Composable -> ViewModel -> UseCase -> Repository -> DataSource
```

- [ ] Confirm which layer(s) this change affects.
- [ ] Verify no boundary violations per `/docs/rules.md`.

### 4. Dependencies

- [ ] Check if new dependencies are needed.
- [ ] If yes, verify they are in the approved tech stack.
- [ ] Do not add new dependencies without explicit approval.

---

## While Writing Code

### 5. Follow Templates

- [ ] Use boilerplate from `/docs/code-templates.md` where helpful.
- [ ] Follow naming conventions from `/docs/rules.md`.
- [ ] Match the existing package/module structure.

### 6. State Handling (For UI Code)

- [ ] Represent loading, success, error, and empty states when applicable.
- [ ] Keep state immutable from the UI side.
- [ ] Expose read-only `StateFlow` from ViewModels.

### 7. Error Handling

- [ ] No empty `catch` blocks.
- [ ] Errors logged with Timber where appropriate.
- [ ] Errors surfaced to UI appropriately.
- [ ] User-facing error messages come from strings/resources or `UiText`.

### 8. Offline-First (For Data Code)

- [ ] Write goes to Room first.
- [ ] Sync to remote is secondary.
- [ ] UI observes Room, never API directly.
- [ ] `isSynced` flag is managed properly.

---

## After Writing Code

### 9. Code Quality Checks

- [ ] No avoidable `Any` types.
- [ ] No hardcoded user-facing strings.
- [ ] No unexplained magic values.
- [ ] No hardcoded URLs/keys.
- [ ] All imports resolve.
- [ ] No unused imports.
- [ ] No `TODO` left without context.
- [ ] No `@Suppress` added without a documented removal plan.

### 10. Architecture Compliance

- [ ] Composables do not call repositories.
- [ ] ViewModels do not import Room DAOs.
- [ ] DataSources do not contain business UI logic.
- [ ] Repository methods use the established Result/Flow patterns.

### 11. Naming Compliance

| Type | Expected Pattern | Check |
| --- | --- | --- |
| Screen | `[Feature]Screen.kt` | [ ] |
| ViewModel | `[Feature]ViewModel.kt` | [ ] |
| UiState | `[Feature]UiState.kt` | [ ] |
| Repository | `[Feature]Repository.kt` | [ ] |
| Impl | `[Feature]RepositoryImpl.kt` | [ ] |
| Entity | `[Feature]Entity.kt` | [ ] |
| DAO | `[Feature]Dao.kt` | [ ] |
| UseCase | `[Verb][Feature]UseCase.kt` | [ ] |

### 12. Documentation Updates

- [ ] Update `/docs/roadmap.md` if completing a tracked task.
- [ ] Add/update spec if behavior changed.
- [ ] Add comments for non-obvious logic, especially invariants and failure paths.

---

## Red Flags to Avoid

| Red Flag | Why It Is Wrong |
| --- | --- |
| Writing >200 lines in a Composable without a clear reason | Split into smaller components |
| Writing >500 production Kotlin lines in one file | Split or document why it must stay together |
| Calling API in ViewModel directly | Use Repository |
| Creating new broad interface for one caller | Premature abstraction |
| Ignoring error state in UI | Bad UX and violates rules |
| Using `GlobalScope` | Memory leak risk |
| Adding dependency not in tech stack | Requires approval |
| Skipping specs for broad feature changes | Creates hidden debt |

---

## Quick Self-Assessment

Before submitting, answer:

1. Would this code work offline? If not, why?
2. What happens if the API fails?
3. Can this be unit tested?
4. Does this follow existing patterns?
5. Did I update docs/roadmap when behavior changed?

---

## Completion Criteria

Code is done when:

- [ ] Relevant checklist items above are verified.
- [ ] Code compiles without errors.
- [ ] Lint/static analysis passes or exceptions are documented.
- [ ] It follows `/docs/rules.md`.
- [ ] Roadmap/specs are updated when applicable.

For broad maintainability or release-readiness work, prefer this gate:

```powershell
.\gradlew.bat spotlessCheck detekt test lintDebug assembleRelease koverXmlReport
```

---

## Example: Adding a New Feature

Task: Add "Manual Record" (Buku Kas) feature.

Before:

```text
1. Read specs/buku-kas-specs.md
2. Check existing pattern: feature/auth/
3. Verify affected layers: presentation, domain, data
```

During:

```text
1. Create POSScreen.kt using code-templates.md where useful
2. Create ManualRecordViewModel.kt with complete state handling
3. Create repository interface + implementation
4. Create entity + DAO
5. Handle offline insert
```

After:

```text
1. Verify naming conventions
2. Check no hardcoded user-facing strings
3. Update roadmap/spec if behavior changed
4. Verify offline behavior and error handling
```

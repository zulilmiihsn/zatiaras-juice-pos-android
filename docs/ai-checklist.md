# ✅ AI Agent Pre-Flight Checklist

> **Purpose**: Verification steps AI must complete before and after generating code
> **Usage**: Run through this checklist for every code generation task

---

## 🔍 BEFORE Writing Code

### 1. Understand the Context
- [ ] Read the relevant **spec** in `/docs/specs/[feature]-specs.md`
- [ ] Check the relevant **plan** in `/docs/plans/[feature].md`
- [ ] Review **current roadmap status** in `/docs/roadmap.md`
- [ ] Identify which **phase** this work belongs to

### 2. Check Existing Patterns
- [ ] Look at existing similar code in the codebase
- [ ] Identify patterns already used (naming, structure, error handling)
- [ ] Check `/docs/code-templates.md` for boilerplates

### 3. Verify Layer Boundaries
```
Composable → ViewModel → UseCase → Repository → DataSource
```
- [ ] Confirm which layer(s) this change affects
- [ ] Verify no boundary violations per `/docs/rules.md`

### 4. Dependencies
- [ ] Check if new dependencies are needed
- [ ] If yes, verify they're in the approved tech stack
- [ ] No new dependencies without explicit approval

---

## ✍️ WHILE Writing Code

### 5. Follow Templates
- [ ] Use boilerplate from `/docs/code-templates.md`
- [ ] Follow naming conventions from `/docs/rules.md`
- [ ] Package structure matches existing patterns

### 6. State Handling (For UI Code)
- [ ] Sealed class for UI State with ALL states:
  - [ ] `Idle`
  - [ ] `Loading`
  - [ ] `Success(data)`
  - [ ] `Error(message)`
  - [ ] `Empty` (if applicable)

### 7. Error Handling
- [ ] No empty `catch` blocks
- [ ] Errors logged with Timber
- [ ] Errors surfaced to UI appropriately
- [ ] User-friendly error messages (from strings.xml)

### 8. Offline-First (For Data Code)
- [ ] Write goes to Room FIRST
- [ ] Sync to remote is secondary
- [ ] UI observes Room, never API directly
- [ ] `isSynced` flag managed properly

---

## 🔬 AFTER Writing Code

### 9. Code Quality Checks
- [ ] No `Any` types (full type safety)
- [ ] No hardcoded strings (use `R.string.*`)
- [ ] No magic numbers (use constants)
- [ ] No hardcoded URLs/keys (use BuildConfig or Hilt injection)
- [ ] All imports resolve (no red squiggles)
- [ ] No unused imports
- [ ] No `TODO` left without context

### 10. Architecture Compliance
- [ ] Composables don't call Repository
- [ ] ViewModel doesn't import Room DAO
- [ ] DataSource doesn't contain business logic
- [ ] Repository uses Result/Flow patterns

### 11. Naming Compliance
| Type | Expected Pattern | ✓ |
|------|-----------------|---|
| Screen | `[Feature]Screen.kt` | [ ] |
| ViewModel | `[Feature]ViewModel.kt` | [ ] |
| UiState | `[Feature]UiState.kt` | [ ] |
| Repository | `[Feature]Repository.kt` (interface) | [ ] |
| Impl | `[Feature]RepositoryImpl.kt` | [ ] |
| Entity | `[Feature]Entity.kt` | [ ] |
| DAO | `[Feature]Dao.kt` | [ ] |
| UseCase | `[Verb][Feature]UseCase.kt` | [ ] |

### 12. Documentation Updates
- [ ] Update `/docs/roadmap.md` if completing a task
- [ ] Add/update spec if behavior changed
- [ ] Add comments for non-obvious logic (WHY, not WHAT)

---

## 🚨 Red Flags to Avoid

If you find yourself doing any of these, STOP and reconsider:

| Red Flag | Why It's Wrong |
|----------|---------------|
| Writing >200 lines in a Composable | Split into smaller components |
| Calling API in ViewModel directly | Use Repository |
| Creating new class without interface | Violates Dependency Inversion |
| Ignoring error state in UI | Bad UX, violates rules |
| Using `GlobalScope` | Memory leak risk |
| Adding dependency not in tech stack | Requires approval |
| Skipping spec for "small" changes | Tech debt accumulation |

---

## 📋 Quick Self-Assessment

Before submitting, answer these questions:

1. **Would this code work offline?** (If not, why?)
2. **What happens if the API fails?** (Is error shown to user?)
3. **Can this be unit tested?** (Are dependencies injected?)
4. **Does this follow existing patterns?** (Is it consistent?)
5. **Did I update the roadmap?** (If task completed)

---

## 🏁 Completion Criteria

Code is "DONE" when:

- [ ] All checklist items above are verified
- [ ] Code compiles without errors
- [ ] No lint warnings (or documented exceptions)
- [ ] Follows all rules in `/docs/rules.md`
- [ ] Roadmap updated (if applicable)
- [ ] Spec updated (if behavior changed)

---

## Example: Adding a New Feature

**Task**: Add "Manual Record" (Buku Kas) feature

**Before:**
```
1. Read specs/buku-kas-specs.md
2. Check existing pattern: feature/auth/
3. Verify: affects presentation, domain, data layers
```

**During:**
```
1. Create POSScreen.kt using code-templates.md
2. Create ManualRecordViewModel.kt with all states
3. Create BukuKasRepository interface + impl
4. Create BukuKasEntity + DAO
5. Handle offline insert
```

**After:**
```
1. Verify all naming conventions
2. Check no hardcoded strings
3. Update roadmap.md: mark "Manual Record" as [x]
4. Self-assessment: offline works, error handling exists
```

---

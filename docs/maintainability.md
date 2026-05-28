# Maintainability Guide

Last updated: 2026-05-23

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

Use these limits as ratchets. Existing files can be improved gradually, but new code should meet them immediately.

| Code Shape | GREAT Target | Action When Larger |
| --- | ---: | --- |
| Route or screen shell | 150 lines | Move content sections to sibling files. |
| Compose section/component | 200 lines | Split repeated rows, headers, dialogs, and controls. |
| ViewModel | 250 lines | Move pure decisions to use cases or private reducers. |
| Repository implementation | 300 lines | Extract mapping, sync, and transaction helpers. |
| Pure utility/use case | 120 lines | Split by behavior, not by arbitrary helper groups. |

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

These are maintainability targets found during the 2026-05-23 audit:

| Area | Reason |
| --- | --- |
| Settings components | `SettingsScreen.kt` is now a shell, but remaining card files should keep shrinking toward the 200-line target. |
| Chat report screen | Screen, content, input bar, composer row, message bubble, markdown parser, and typing indicator are split; keep chat edits in the smallest matching file. |
| Security settings screen | Screen shell, content, status header, dialog, and shared components are split; keep new edits inside the smallest matching file. |
| POS catalog components | Paged catalog shell, filters, paged product body, list item, and custom-item dialog are split; keep each new edit in the smallest matching component file. |
| POS checkout/history screens | Large UI files should follow the Route/Screen/Content/component split. |
| Detekt thresholds | Current thresholds pass, but they are still permissive for long methods and nested UI. |

Do not loosen the ratchet to make checks pass. If a stricter rule fails, either refactor the code or add a short documented exception with a removal plan.

# ZatiarasPOS Android Documentation

Last updated: 2026-05-22

This folder contains the product, architecture, API, and engineering guidance for the ZatiarasPOS Android application.

## Core Documents

| Document | Purpose |
| --- | --- |
| [overview.md](./overview.md) | Project summary and business scope. |
| [roadmap.md](./roadmap.md) | Delivery phases and current progress. |
| [rules.md](./rules.md) | Coding standards and layer boundaries. |
| [maintainability.md](./maintainability.md) | Readability ratchets for humans and AI agents. |
| [api.md](./api.md) | Supabase data contracts and schema notes. |
| [ARCHITECTURE_MASTER_PLAN.md](./ARCHITECTURE_MASTER_PLAN.md) | Technical architecture blueprint. |
| [SECURITY_RLS_CHECKLIST.md](./SECURITY_RLS_CHECKLIST.md) | Supabase anon-key and RLS production checklist. |

## AI Agent Resources

| Document | Purpose |
| --- | --- |
| [code-templates.md](./code-templates.md) | Boilerplate patterns for new files. |
| [do-dont.md](./do-dont.md) | Correct and incorrect implementation examples. |
| [ai-checklist.md](./ai-checklist.md) | Pre-flight verification checklist. |

## Feature Planning

- `plans/` contains high-level feature plans.
- `specs/` contains lower-level technical specifications and acceptance criteria.

Recommended workflow for new features:

1. Create or update a plan under `docs/plans/`.
2. Create or update the matching spec under `docs/specs/`.
3. Implement following `docs/rules.md`.
4. Run the project verification commands from the root README.

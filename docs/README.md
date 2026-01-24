# 📚 ZatiarasPOS Android - Documentation Index

> **Last Updated**: 2026-01-08

Selamat datang di dokumentasi ZatiarasPOS Android! Folder ini berisi semua dokumen yang mengatur development aplikasi.

---

## 📖 Core Documents

| Document | Purpose | Read This If... |
|----------|---------|-----------------|
| [**overview.md**](./overview.md) | What is this project? Quick summary | You're new to the project |
| [**roadmap.md**](./roadmap.md) | Progress tracking & phases | You want to know current status |
| [**rules.md**](./rules.md) | Coding standards & layer boundaries | You're about to write code |
| [**api.md**](./api.md) | Supabase data contract & schema | You're working on data layer |
| [**ARCHITECTURE_MASTER_PLAN.md**](./ARCHITECTURE_MASTER_PLAN.md) | Technical architecture deep-dive | You need implementation details |

## 🤖 AI Agent Resources

| Document | Purpose | Read This If... |
|----------|---------|-----------------|
| [**code-templates.md**](./code-templates.md) | Boilerplate code for all file types | You're creating new files |
| [**do-dont.md**](./do-dont.md) | Correct vs incorrect code examples | You want to avoid common mistakes |
| [**ai-checklist.md**](./ai-checklist.md) | Pre-flight verification checklist | You're about to submit code |

---

## 📁 Subdirectories

### `/plans`
High-level feature planning documents. **"Why"** and **"How"** of each feature.

| File | Description |
|------|-------------|
| [_TEMPLATE.md](./plans/_TEMPLATE.md) | Template for new plans |
| [01-core-architecture.md](./plans/01-core-architecture.md) | Core modules setup plan |
| [02-authentication.md](./plans/02-authentication.md) | Auth feature plan |

### `/specs`
Low-level technical specifications. Database schema, UI states, acceptance criteria.

| File | Description |
|------|-------------|
| [_TEMPLATE.md](./specs/_TEMPLATE.md) | Template for new specs |
| [01-core-modules-setup.md](./specs/01-core-modules-setup.md) | Core modules spec |
| [02-authentication-specs.md](./specs/02-authentication-specs.md) | Auth feature spec |

---

## 🔄 Document Relationships

```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│  overview.md ──────┬──────> "What is this?"                 │
│       │            │                                        │
│       ▼            │                                        │
│  roadmap.md ───────┼──────> "Where are we?"                 │
│       │            │                                        │
│       ▼            │                                        │
│  rules.md ─────────┼──────> "How do we code?"               │
│       │            │                                        │
│       ▼            │                                        │
│  ARCHITECTURE_     │                                        │
│  MASTER_PLAN.md ───┼──────> "Technical deep-dive"           │
│       │            │                                        │
│       ▼            │                                        │
│  api.md ───────────┴──────> "Data contracts"                │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  For each feature:                                   │   │
│  │                                                      │   │
│  │  plans/[feature].md ──> specs/[feature]-specs.md     │   │
│  │       │                        │                     │   │
│  │       └────────────────────────┼──> Implementation   │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 📝 Workflow: Adding New Features

1. **Create Plan**: `docs/plans/[feature].md` (use template)
2. **Create Spec**: `docs/specs/[feature]-specs.md` (use template)
3. **Update Roadmap**: Add items to `roadmap.md`
4. **Implement**: Follow `rules.md` strictly
5. **Update Roadmap**: Mark items as complete

---

## 🏷️ Document Roles (Clarification)

| Document | Role | Audience |
|----------|------|----------|
| `overview.md` | **Quick intro** - Non-technical summary, project scope | Anyone (PM, New devs) |
| `ARCHITECTURE_MASTER_PLAN.md` | **Technical blueprint** - Implementation details, strategies | Developers, AI agents |

These two documents are **complementary**, not duplicative:
- `overview.md` → "What and Why" (5-minute read)
- `ARCHITECTURE` → "How exactly" (15-minute read)

---

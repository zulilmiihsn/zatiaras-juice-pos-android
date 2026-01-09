# 📅 Plan: Phase 1 - Core Architecture & Modules Setup

> **Status**: 📝 Proposed
> **Parent**: [Project Roadmap](../roadmap.md)
> **Sprint**: 1-2

## 🎯 Objective
Establish the **Multi-Module Monolith** foundation as defined in the [Architecture Master Plan](../ARCHITECTURE_MASTER_PLAN.md). This separation of concerns is critical for the "Offline-First" capability and Clean Architecture enforcement.

## 🏗️ Modules to Create

We will create the `core` layer which serves as the foundation for all feature modules.

### 1. `:core:domain` (The "Pure" Layer)
*   **Purpose**: Contains high-level business logic, generic entities, and Repository interfaces.
*   **Dependencies**: None (Pure Kotlin).
*   **Contents**:
    *   `Result<T>` sealed class (for handling Success/Error).
    *   Base `UseCase` abstraction.
    *   Shared Entities (e.g., potentially shared data models if strictly necessary here, though often in `model` or feature-specific domains). *Correction*: Accessing `Result` wrapper pattern.

### 2. `:core:data` (The "Implementation" Layer)
*   **Purpose**: Handles data sourcing (Local DB vs Network) and synchronization logic.
*   **Dependencies**: `:core:domain`.
*   **Contents**:
    *   **Room**: Database setup, TypeConverters.
    *   **Network**: Ktor Client setup / Retrofit (Master Plan mentions Ktor, but we need to confirm strict instruction. Plan says Ktor. Rules says "Hardcoded API implementations" forbidden).
    *   **DI**: Hilt Modules for providing Database and Network clients.

### 3. `:core:ui` (The "Visual" Layer)
*   **Purpose**: The Design System. Ensures pixel-perfect consistency.
*   **Dependencies**: `androidx.compose.*`.
*   **Contents**:
    *   **Theme**: Colors, Typography (`Type.kt`), Shapes.
    *   **Components**: Reusable `ZatiarasButton`, `ZatiarasTextField`, `LoadingState`, `ErrorState`.
    *   **Assets**: Shared Drawables/Vectors.

## 📝 Step-by-Step Implementation

1.  **Module Creation**:
    *   Create directories: `core/domain`, `core/data`, `core/ui`.
    *   Create `build.gradle.kts` for each.
    *   Update `settings.gradle.kts` to include them.

2.  **Dependency Management**:
    *   Move common dependencies (Hilt, Coroutines, etc.) to a logic valid place (or duplication for now until we have a `build-logic` plugin or `libs.versions.toml`). *Strategy*: Use `libs.versions.toml` (Version Catalog) if available, or standard gradle files. *Check*: The project seems to be using standard gradle. We will verify if `libs.versions.toml` exists.

3.  **Basic Scaffolding**:
    *   **`core:ui`**: Move existing `ui.theme` from `:app` to `:core:ui`.
    *   **`:app`**: Add dependency on `:core:data` and `:core:ui`.

## 🧪 Verification Strategy
*   Build must pass (`./gradlew assembleDebug`).
*   `:app` must be able to use a component from `:core:ui` (e.g., apply the Theme).

## ❓ Open Questions
*   **Version Catalog**: Should we migrate to `libs.versions.toml` for better dependency management across these new modules? (Recommended: Yes).
*   **Networking**: Master Plan says **Ktor Client**. Confirming this choice over Retrofit. (Will stick to Plan: Ktor).


# 📅 Plan: Phase 2 - Authentication & Session

> **Status**: 📝 Proposed
> **Parent**: [Project Roadmap](../roadmap.md)
> **Sprint**: 3-4

## 🎯 Objective
Implement a secure, Offline-First authentication system using Supabase.
The user must be able to Login, store the session securely, and subsequent app launches should check this session (Auto-Login).

## 🏗️ Architecture

We will create a specific Feature Module: `:feature:auth`.

### 1. Data Layer (`:core:data`, `:feature:auth`)
*   **Supabase Client**: Initialized in `:core:data`.
*   **AuthRepository**: Interface in `:core:domain`, Implementation in `:core:data`.
    *   `login(email, password): Result<User>`
    *   `logout()`
    *   `getCurrentUser()`
*   **Session Storage**: Use `DataStore` (Proto/Preferences) to store the Access Token & Refresh Token encrypted.

### 2. Domain Layer (`:core:domain`)
*   **UseCases**:
    *   `LoginUseCase`
    *   `CheckSessionUseCase`

### 3. UI Layer (`:feature:auth`)
*   **LoginScreen**:
    *   UI: Logo, Email Input, Password Input, "Masuk" Button.
    *   State: `Idle`, `Loading`, `Success`, `Error`.
*   **Navigation**:
    *   `:app` will host the NavHost.
    *   Routes: `Login`, `Dashboard`.

## 📝 Step-by-Step Implementation

1.  **Supabase Setup**:
    *   Add Supabase BOM to `libs.versions.toml`.
    *   Initialize Supabase in `di/AppModule.kt` (`:core:data`).

2.  **Module Creation**:
    *   Create `:feature:auth`.
    *   Add dependencies.

3.  **Repository Implementation**:
    *   Implement `SupabaseAuthRepository`.

4.  **UI Construction**:
    *   Build `LoginScreen` composable using `:core:ui` components.
    *   Connect to `AuthViewModel`.

5.  **Integration**:
    *   Wire up Navigation in `MainActivity`.

## 🧪 Verification Strategy
*   User can type email (`owner@zatiaras.com`) and password.
*   Click login -> Show Loading spinner.
*   On success -> Navigate to Dashboard (Placeholder).
*   On error -> Show Snackbar.

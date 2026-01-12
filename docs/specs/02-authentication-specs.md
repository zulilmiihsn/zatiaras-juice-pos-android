# ⚙️ Spec: Phase 2 - Authentication Implementation

> **Plan**: [02-authentication.md](../plans/02-authentication.md)
> **Status**: ✅ Completed

## 1. Libraries & Dependencies

Add to `libs.versions.toml`:

```toml
[versions]
supabase = "2.1.3" # Check latest valid version
ktor = "2.3.7"
datastore = "1.0.0"
securityCrypto = "1.1.0-alpha06"

[libraries]
# Supabase
supabase-gotrue = { group = "io.github.jan-tennert.supabase", name = "gotrue-kt", version.ref = "supabase" }
supabase-postgrest = { group = "io.github.jan-tennert.supabase", name = "postgrest-kt", version.ref = "supabase" }
ktor-client-cio = { group = "io.ktor", name = "ktor-client-cio", version.ref = "ktor" }
ktor-client-core = { group = "io.ktor", name = "ktor-client-core", version.ref = "ktor" }

# Security (Encrypted Session Storage)
androidx-security-crypto = { group = "androidx.security", name = "security-crypto", version.ref = "securityCrypto" }
```

## 2. Module Structure

### `:core:domain`
*   `interface AuthRepository`
    *   `suspend fun login(email: String, password: String): Result<Unit>`
    *   `fun isUserLoggedIn(): Flow<Boolean>`

### `:core:data`
*   **DI**: `DatabaseModule` (Provides DAOs), `NetworkModule` (Provides Postgrest).
*   `LocalAuthRepository`: Implements `AuthRepository`.
    *   **Login**: Verifies password against local `users` table (PBKDF2/SHA-256).
    *   **Sync**: `UserRemoteDataSource` fetches users from remote `users` table.
*   **Data Sources**:
    *   `UserDao`: Local Room access.
    *   `UserRemoteDataSource`: Supabase Postgrest access.

### `:feature:auth`
*   `LoginScreen.kt`
    *   TextFields for username/pass.
    *   Sync status indicator.
    *   `AuthViewModel`.

## 3. Data Flow
1.  **User** enters credentials.
2.  **ViewModel** calls `loginUseCase(email, pass)`.
3.  **Repository** checks credentials against local `users` table.
4.  **Verification** uses `PasswordHasher` (PBKDF2/SHA-256).
5.  **Session** is saved via `EncryptedSessionManager` (AES256-GCM encrypted at rest).
    *   **Implementation**: Custom `SessionManager` injected into Supabase client using `AndroidX Security Crypto`.

## 4. UI States
*   `LoginUiState`:
    *   `isLoading: Boolean`
    *   `error: String?`
    *   `isSuccess: Boolean`

## 5. Acceptance Criteria
1.  App launches to Login Screen.
2.  Valid login -> Logs "Success" in Timber (Navigation to Dashboard is next step).
3.  Invalid login -> Shows error message on UI.

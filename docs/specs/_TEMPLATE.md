# ⚙️ Feature Specifications Template

**Feature**: [Name]
**Plan Reference**: [Link to Plan]

## 1. Database Schema (Room)
```kotlin
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String
)
```

## 2. Repository Interface
```kotlin
interface AuthRepository {
    fun login(u: String, p: String): Flow<Resource<User>>
}
```

## 3. UI States (Compose)
```kotlin
sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class Success(val user: User) : LoginUiState
    data class Error(val msg: String) : LoginUiState
}
```

## 4. Acceptance Criteria (Checklist)
- [ ] User can login with valid creds.
- [ ] Error message shown for invalid creds.
- [ ] App works offline (if applicable).
- [ ] Token saved encrypted.

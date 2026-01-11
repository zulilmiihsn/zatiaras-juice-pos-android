package com.zatiaras.pos.core.data.repository

import com.zatiaras.pos.core.data.local.dao.UserDao
import com.zatiaras.pos.core.data.local.entity.UserEntity
import com.zatiaras.pos.core.domain.AuthRepository
import com.zatiaras.pos.core.domain.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local implementation of AuthRepository using Room database.
 * 
 * Supports offline-first authentication:
 * - Login with username/password
 * - Credentials stored locally in Room
 * - No internet required for authentication
 */
@Singleton
class LocalAuthRepository @Inject constructor(
    private val userDao: UserDao
) : AuthRepository {

    // Session state - tracks if user is logged in
    private val _isLoggedIn = MutableStateFlow(false)
    private var _currentUser: UserEntity? = null

    override suspend fun login(email: String, password: String): Result<Unit> {
        // Note: 'email' parameter is actually username for backward compatibility
        val username = email
        
        return try {
            Timber.d("Attempting local login with username: $username")
            
            val user = userDao.getUserByUsername(username)
            
            if (user == null) {
                Timber.w("User not found: $username")
                return Result.Error(Exception("Username tidak ditemukan"))
            }
            
            if (!UserEntity.verifyPassword(password, user.passwordHash)) {
                Timber.w("Invalid password for user: $username")
                return Result.Error(Exception("Password salah"))
            }
            
            // Login successful
            _currentUser = user
            _isLoggedIn.value = true
            Timber.d("Login successful for: $username (${user.displayName})")
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Login failed: ${e.message}")
            Result.Error(Exception("Login gagal: ${e.message}"))
        }
    }

    override fun isUserLoggedIn(): Flow<Boolean> = _isLoggedIn

    override suspend fun logout() {
        Timber.d("User logged out: ${_currentUser?.username}")
        _currentUser = null
        _isLoggedIn.value = false
    }

    /**
     * Get current logged in user.
     */
    fun getCurrentUser(): UserEntity? = _currentUser

    /**
     * Create a new user account.
     * Used for initial setup or adding new staff.
     */
    suspend fun createUser(
        username: String,
        password: String,
        displayName: String,
        role: String = "kasir"
    ): Result<UserEntity> {
        return try {
            // Check if username already exists
            val existing = userDao.getUserByUsername(username)
            if (existing != null) {
                return Result.Error(Exception("Username sudah digunakan"))
            }

            val user = UserEntity(
                id = UUID.randomUUID().toString(),
                username = username,
                passwordHash = UserEntity.hashPassword(password),
                displayName = displayName,
                role = role
            )

            userDao.insertUser(user)
            Timber.d("User created: $username ($displayName)")
            
            Result.Success(user)
        } catch (e: Exception) {
            Timber.e(e, "Failed to create user: ${e.message}")
            Result.Error(Exception("Gagal membuat akun: ${e.message}"))
        }
    }

    /**
     * Check if this is a first-run setup (no users exist).
     */
    suspend fun isFirstRun(): Boolean {
        return userDao.getUserCount() == 0
    }

    /**
     * Setup default admin user for first run.
     */
    suspend fun setupDefaultAdmin(
        username: String = "admin",
        password: String = "admin123",
        displayName: String = "Administrator"
    ): Result<Unit> {
        if (!isFirstRun()) {
            return Result.Error(Exception("Users already exist"))
        }
        
        return when (val result = createUser(username, password, displayName, "pemilik")) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> result
        }
    }
}

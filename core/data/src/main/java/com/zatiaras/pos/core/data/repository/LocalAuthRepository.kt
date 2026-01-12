package com.zatiaras.pos.core.data.repository

import com.zatiaras.pos.core.data.local.dao.UserDao
import com.zatiaras.pos.core.data.local.entity.UserEntity
import com.zatiaras.pos.core.data.remote.UserRemoteDataSource
import com.zatiaras.pos.core.data.remote.dto.UserDto
import com.zatiaras.pos.core.data.session.SessionPreferences
import com.zatiaras.pos.core.domain.AuthRepository
import com.zatiaras.pos.core.domain.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local implementation of AuthRepository using Room database.
 * 
 * Supports offline-first authentication:
 * - Login with username/password
 * - Credentials stored locally in Room
 * - Users synced from Supabase when online
 * - Session persists across app restarts
 * - No internet required for authentication (after initial sync)
 */
@Singleton
class LocalAuthRepository @Inject constructor(
    private val userDao: UserDao,
    private val userRemoteDataSource: UserRemoteDataSource,
    private val sessionPreferences: SessionPreferences
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
            
            if (!user.isActive) {
                Timber.w("User is inactive: $username")
                return Result.Error(Exception("Akun tidak aktif"))
            }
            
            if (!UserEntity.verifyPassword(password, user.passwordHash)) {
                Timber.w("Invalid password for user: $username")
                return Result.Error(Exception("Password salah"))
            }
            
            // Login successful - save session
            _currentUser = user
            _isLoggedIn.value = true
            sessionPreferences.saveSession(
                userId = user.id,
                username = user.username,
                displayName = user.displayName,
                role = user.role
            )
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
        sessionPreferences.clearSession()
    }

    /**
     * Restore session from saved preferences.
     * Call this on app startup before showing login screen.
     * 
     * @return true if session was restored, false if user needs to login
     */
    suspend fun restoreSession(): Boolean {
        if (!sessionPreferences.isLoggedIn()) {
            Timber.d("No saved session found")
            return false
        }
        
        val userId = sessionPreferences.getUserId() ?: return false
        val user = userDao.getUserById(userId)
        
        if (user == null || !user.isActive) {
            Timber.w("Saved session user not found or inactive, clearing session")
            sessionPreferences.clearSession()
            return false
        }
        
        // Restore session
        _currentUser = user
        _isLoggedIn.value = true
        Timber.d("Session restored for: ${user.username} (${user.displayName})")
        return true
    }

    /**
     * Check if there's a saved session (without fully restoring).
     */
    fun hasSavedSession(): Boolean {
        return sessionPreferences.isLoggedIn()
    }

    /**
     * Get current logged in user.
     */
    fun getCurrentUser(): UserEntity? = _currentUser

    /**
     * Sync users from Supabase to local Room database.
     * Call this on app start when online.
     * 
     * @return Number of users synced, or -1 if failed
     */
    suspend fun syncUsersFromRemote(): Int {
        return try {
            Timber.d("Starting user sync from Supabase...")
            
            when (val result = userRemoteDataSource.fetchActiveUsers()) {
                is Result.Success -> {
                    val remoteUsers = result.data
                    Timber.d("Fetched ${remoteUsers.size} users from Supabase")
                    
                    // Sync each user to local database
                    var syncedCount = 0
                    for (dto in remoteUsers) {
                        syncUserToLocal(dto)
                        syncedCount++
                    }
                    
                    Timber.d("User sync completed: $syncedCount users")
                    syncedCount
                }
                is Result.Error -> {
                    Timber.e(result.exception, "Failed to sync users: ${result.exception?.message}")
                    -1
                }
                is Result.Loading -> -1
            }
        } catch (e: Exception) {
            Timber.e(e, "User sync failed: ${e.message}")
            -1
        }
    }
    
    /**
     * Sync a single user from Supabase DTO to local Room.
     * If user exists, update; otherwise insert.
     */
    private suspend fun syncUserToLocal(dto: UserDto) {
        val existing = userDao.getUserByUsername(dto.username)
        
        val user = UserEntity(
            id = dto.id,
            username = dto.username,
            passwordHash = dto.passwordHash, // Already hashed from Supabase
            displayName = dto.displayName,
            role = dto.role,
            isActive = dto.isActive,
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        if (existing != null) {
            // Update existing user
            userDao.insertUser(user) // REPLACE strategy
            Timber.d("Updated user: ${user.username}")
        } else {
            // Insert new user
            userDao.insertUser(user)
            Timber.d("Inserted new user: ${user.username}")
        }
    }

    /**
     * Check if this is a first-run setup (no users exist locally).
     */
    suspend fun isFirstRun(): Boolean {
        return userDao.getUserCount() == 0
    }

    /**
     * Get all local users.
     */
    suspend fun getAllUsers(): List<UserEntity> {
        return userDao.getAllUsersList()
    }
}

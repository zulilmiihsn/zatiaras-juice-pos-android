package com.zatiaras.pos.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zatiaras.pos.core.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for User operations.
 * 
 * Handles local authentication and user management.
 */
@Dao
interface UserDao {

    /**
     * Insert or update a user (upsert).
     * Uses REPLACE strategy for sync operations.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    /**
     * Get user by username for login.
     */
    @Query("SELECT * FROM users WHERE username = :username AND isActive = 1 LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    /**
     * Get user by ID.
     */
    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: String): UserEntity?

    /**
     * Get all active users.
     */
    @Query("SELECT * FROM users WHERE isActive = 1 ORDER BY displayName ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    /**
     * Check if any user exists (for first-run setup).
     */
    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    /**
     * Update password.
     */
    @Query("UPDATE users SET passwordHash = :newHash, updatedAt = :timestamp WHERE id = :userId")
    suspend fun updatePassword(userId: String, newHash: String, timestamp: Long = System.currentTimeMillis())

    /**
     * Deactivate user (soft delete).
     */
    @Query("UPDATE users SET isActive = 0, updatedAt = :timestamp WHERE id = :userId")
    suspend fun deactivateUser(userId: String, timestamp: Long = System.currentTimeMillis())

    /**
     * Get all users as a list (for sync operations).
     */
    @Query("SELECT * FROM users ORDER BY displayName ASC")
    suspend fun getAllUsersList(): List<UserEntity>
}

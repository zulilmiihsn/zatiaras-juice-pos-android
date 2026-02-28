package com.zatiaras.pos.core.data.remote

import com.zatiaras.pos.core.data.remote.dto.UserDto
import com.zatiaras.pos.core.domain.Result
import io.github.jan.supabase.postgrest.Postgrest
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remote data source for user operations via Supabase.
 * 
 * Handles fetching users from the remote 'pengguna' table for sync.
 */
@Singleton
class UserRemoteDataSource @Inject constructor(
    private val postgrest: Postgrest
) {
    
    companion object {
        private const val TABLE_PENGGUNA = "pengguna"
    }
    
    /**
     * Fetch all active users from Supabase.
     * Used to sync users to local Room database.
     */
    suspend fun fetchAllUsers(): Result<List<UserDto>> {
        return try {
            val users = postgrest.from(TABLE_PENGGUNA)
                .select()
                .decodeList<UserDto>()
            
            Timber.d("Fetched ${users.size} users from Supabase (pengguna)")
            Result.Success(users)
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch users from Supabase")
            Result.Error(e)
        }
    }
    
    /**
     * Fetch only active users from Supabase.
     */
    suspend fun fetchActiveUsers(): Result<List<UserDto>> {
        return try {
            val users = postgrest.from(TABLE_PENGGUNA)
                .select {
                    filter {
                        eq("is_active", true)
                    }
                }
                .decodeList<UserDto>()
            
            Timber.d("Fetched ${users.size} active users from Supabase (pengguna)")
            Result.Success(users)
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch active users from Supabase")
            Result.Error(e)
        }
    }

    /**
     * Update user password hash on Supabase.
     */
    suspend fun updatePasswordHash(userId: String, newPasswordHash: String): Result<Unit> {
        return try {
            postgrest.from(TABLE_PENGGUNA).update(
                mapOf(
                    "password_hash" to newPasswordHash,
                    "updated_at" to System.currentTimeMillis()
                )
            ) {
                filter { eq("id", userId) }
            }

            Timber.d("Updated password hash on remote for user: $userId")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update password hash on Supabase for user: $userId")
            Result.Error(e)
        }
    }
}

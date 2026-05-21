package com.zatiaras.pos.core.data.remote

import com.zatiaras.pos.core.data.remote.dto.UserDto
import com.zatiaras.pos.core.domain.Result
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
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
        private val USER_PROFILE_COLUMNS = Columns.list(
            "id",
            "username",
            "display_name",
            "role",
            "is_active",
            "created_at",
            "updated_at"
        )
        private val USER_CREDENTIAL_COLUMNS = Columns.list(
            "id",
            "username",
            "password_hash",
            "display_name",
            "role",
            "is_active",
            "created_at",
            "updated_at"
        )
    }
    
    /**
     * Fetch all active users from Supabase.
     * Used to sync users to local Room database.
     */
    suspend fun fetchAllUsers(): Result<List<UserDto>> {
        return try {
            val users = postgrest.from(TABLE_PENGGUNA)
                .select(USER_PROFILE_COLUMNS)
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
                .select(USER_PROFILE_COLUMNS) {
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
     * Fetch credential material for one login attempt only.
     *
     * Bulk user sync intentionally excludes password_hash so one compromised
     * device cannot expose every account's verifier.
     */
    suspend fun fetchActiveUserWithPassword(username: String): Result<UserDto?> {
        return try {
            val user = postgrest.from(TABLE_PENGGUNA)
                .select(USER_CREDENTIAL_COLUMNS) {
                    filter {
                        eq("username", username)
                        eq("is_active", true)
                    }
                }
                .decodeSingleOrNull<UserDto>()

            if (user == null) {
                Timber.w("No active remote user found for username: $username")
            } else {
                Timber.d("Fetched active remote credential for username: $username")
            }
            Result.Success(user)
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch remote credential for username: $username")
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

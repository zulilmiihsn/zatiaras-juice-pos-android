package com.zatiaras.pos.core.data.repository

import com.zatiaras.pos.core.domain.AuthRepository
import com.zatiaras.pos.core.domain.Result
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

class SupabaseAuthRepository @Inject constructor(
    private val auth: Auth
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            Timber.d("Attempting login with email: $email")
            auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            Timber.d("Login successful for: $email")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Login failed for $email: ${e.message}")
            // Extract more detailed error message
            val errorMessage = when {
                e.message?.contains("Invalid login credentials") == true -> 
                    "Email atau password salah"
                e.message?.contains("Email not confirmed") == true -> 
                    "Email belum diverifikasi. Cek inbox email."
                e.message?.contains("Network") == true || e.message?.contains("Unable to resolve host") == true -> 
                    "Tidak ada koneksi internet"
                else -> e.message ?: "Login gagal"
            }
            Result.Error(Exception(errorMessage, e))
        }
    }

    override fun isUserLoggedIn(): Flow<Boolean> {
        return auth.sessionStatus.map { status ->
            status is io.github.jan.supabase.gotrue.SessionStatus.Authenticated
        }
    }

    override suspend fun changeCurrentUserPassword(currentPassword: String, newPassword: String): Result<Unit> {
        return Result.Error(UnsupportedOperationException("Change password is not supported in SupabaseAuthRepository"))
    }

    override suspend fun logout() {
        auth.signOut()
    }
}

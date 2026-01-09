package com.zatiaras.pos.core.domain

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<Unit>
    fun isUserLoggedIn(): Flow<Boolean>
    suspend fun logout()
}

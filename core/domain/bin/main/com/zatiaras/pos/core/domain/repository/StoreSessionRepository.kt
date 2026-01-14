package com.zatiaras.pos.core.domain.repository

import com.zatiaras.pos.core.domain.model.StoreSession
import kotlinx.coroutines.flow.Flow

interface StoreSessionRepository {
    fun getActiveSession(): Flow<StoreSession?>
    suspend fun getActiveSessionOneShot(): StoreSession?
    suspend fun openSession(openingCash: Long, branchId: String? = null): Result<StoreSession>
    suspend fun closeSession(sessionId: String): Result<Boolean>
    fun getLastSessions(limit: Int): Flow<List<StoreSession>>
}

package com.zatiaras.pos.core.data.repository

import com.zatiaras.pos.core.data.local.dao.StoreSessionDao
import com.zatiaras.pos.core.data.local.entity.StoreSessionEntity
import com.zatiaras.pos.core.domain.model.StoreSession
import com.zatiaras.pos.core.domain.repository.StoreSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class StoreSessionRepositoryImpl @Inject constructor(
    private val storeSessionDao: StoreSessionDao
) : StoreSessionRepository {

    override fun getActiveSession(): Flow<StoreSession?> {
        return storeSessionDao.getActiveSession().map { it?.toDomain() }
    }

    override suspend fun getActiveSessionOneShot(): StoreSession? {
        return storeSessionDao.getActiveSessionOneShot()?.toDomain()
    }

    override suspend fun openSession(openingCash: Long, branchId: String?): Result<StoreSession> {
        return try {
            val existing = storeSessionDao.getActiveSessionOneShot()
            if (existing != null) {
                return Result.failure(IllegalStateException("Store is already open"))
            }

            val newSession = StoreSessionEntity(
                id = UUID.randomUUID().toString(),
                openingCash = openingCash,
                openingTime = System.currentTimeMillis(),
                isActive = true,
                branchId = branchId,
                isSynced = false
            )
            storeSessionDao.insertSession(newSession)
            Result.success(newSession.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun closeSession(sessionId: String): Result<Boolean> {
        return try {
            val session = storeSessionDao.getActiveSessionOneShot()
            if (session == null || session.id != sessionId) {
                return Result.failure(IllegalStateException("Session not found or already closed"))
            }

            val closedSession = session.copy(
                closingTime = System.currentTimeMillis(),
                isActive = false,
                isSynced = false,
                updatedAt = System.currentTimeMillis()
            )
            storeSessionDao.updateSession(closedSession)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getLastSessions(limit: Int): Flow<List<StoreSession>> {
        return storeSessionDao.getLastSessions(limit).map { list ->
            list.map { it.toDomain() }
        }
    }

    private fun StoreSessionEntity.toDomain(): StoreSession {
        return StoreSession(
            id = id,
            openingCash = openingCash,
            openingTime = openingTime,
            closingTime = closingTime,
            isActive = isActive,
            branchId = branchId
        )
    }
}

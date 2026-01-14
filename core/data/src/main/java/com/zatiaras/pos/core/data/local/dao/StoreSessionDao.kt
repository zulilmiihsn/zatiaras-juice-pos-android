package com.zatiaras.pos.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.zatiaras.pos.core.data.local.entity.StoreSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StoreSessionDao {

    @Query("SELECT * FROM store_sessions WHERE isActive = 1 ORDER BY openingTime DESC LIMIT 1")
    fun getActiveSession(): Flow<StoreSessionEntity?>

    @Query("SELECT * FROM store_sessions WHERE isActive = 1 ORDER BY openingTime DESC LIMIT 1")
    suspend fun getActiveSessionOneShot(): StoreSessionEntity?

    @Query("SELECT * FROM store_sessions ORDER BY openingTime DESC LIMIT :limit")
    fun getLastSessions(limit: Int): Flow<List<StoreSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StoreSessionEntity)

    @Update
    suspend fun updateSession(session: StoreSessionEntity)

    @Query("SELECT * FROM store_sessions WHERE isSynced = 0")
    suspend fun getUnsyncedSessions(): List<StoreSessionEntity>
    
    @Query("DELETE FROM store_sessions")
    suspend fun deleteAll()
}

package com.zatiaras.pos.core.data.sync

/**
 * Interface for entity-specific sync operations.
 * 
 * Each entity type (Transaction, CashRecord, Product, etc.) should have
 * its own implementation of this interface, following Single Responsibility Principle.
 */
interface EntitySyncer {
    
    /**
     * The type of entity this syncer handles.
     */
    val syncType: SyncType
    
    /**
     * Sync unsynced entities to remote server.
     * 
     * @return SyncResult with upload/fail counts
     */
    suspend fun sync(): SyncResult
    
    /**
     * Get count of pending (unsynced) items.
     */
    suspend fun getPendingCount(): Int
}

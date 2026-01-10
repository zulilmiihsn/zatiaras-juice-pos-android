package com.zatiaras.pos.core.data.sync

/**
 * Represents the current status of a sync operation.
 * 
 * Used by SyncManager to expose sync state to UI layer.
 */
sealed class SyncStatus {
    /**
     * No sync is currently running.
     */
    data object Idle : SyncStatus()
    
    /**
     * Sync is in progress.
     * 
     * @param progress Progress percentage (0-100), or null if indeterminate
     * @param message Human-readable status message
     */
    data class Syncing(
        val progress: Int? = null,
        val message: String = "Syncing..."
    ) : SyncStatus()
    
    /**
     * Sync completed successfully.
     * 
     * @param itemsSynced Number of items synced
     * @param timestamp Completion timestamp
     */
    data class Success(
        val itemsSynced: Int,
        val timestamp: Long = System.currentTimeMillis()
    ) : SyncStatus()
    
    /**
     * Sync failed with an error.
     * 
     * @param error The exception that caused the failure
     * @param message Human-readable error message
     * @param isRetryable Whether the sync can be retried
     */
    data class Error(
        val error: Throwable? = null,
        val message: String,
        val isRetryable: Boolean = true
    ) : SyncStatus()
}

/**
 * Represents the type of data being synced.
 */
enum class SyncType {
    PRODUCTS,
    CATEGORIES,
    TRANSACTIONS,
    CASH_RECORDS,
    ALL
}

/**
 * Result of a sync operation for a specific data type.
 */
data class SyncResult(
    val type: SyncType,
    val uploaded: Int = 0,
    val downloaded: Int = 0,
    val failed: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
) {
    val isSuccess: Boolean get() = failed == 0
    val totalSynced: Int get() = uploaded + downloaded
}

package com.zatiaras.pos.core.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.syncDataStore: DataStore<Preferences> by preferencesDataStore(name = "sync_prefs")

/**
 * Stores sync-related preferences using DataStore.
 * 
 * Tracks:
 * - Last sync timestamp for delta sync (per entity type)
 * - Sync in progress flag
 * - Last successful sync timestamp
 */
@Singleton
class SyncPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_LAST_PRODUCTS_SYNC = longPreferencesKey("last_products_sync")
        private val KEY_LAST_CATEGORIES_SYNC = longPreferencesKey("last_categories_sync")
        private val KEY_LAST_TRANSACTIONS_SYNC = longPreferencesKey("last_transactions_sync")
        private val KEY_LAST_CASH_RECORDS_SYNC = longPreferencesKey("last_cash_records_sync")
        private val KEY_LAST_ADD_ONS_SYNC = longPreferencesKey("last_add_ons_sync")
        private val KEY_LAST_SETTINGS_SYNC = longPreferencesKey("last_settings_sync")
        private val KEY_LAST_FULL_SYNC = longPreferencesKey("last_full_sync")
        private val KEY_SYNC_IN_PROGRESS = booleanPreferencesKey("sync_in_progress")
    }

    // ==================== PRODUCTS ====================

    /**
     * Get last products sync timestamp.
     */
    suspend fun getLastProductsSyncTimestamp(): Long {
        return context.syncDataStore.data.map { prefs ->
            prefs[KEY_LAST_PRODUCTS_SYNC] ?: 0L
        }.first()
    }

    /**
     * Update last products sync timestamp to now.
     */
    suspend fun updateLastProductsSyncTimestamp() {
        context.syncDataStore.edit { prefs ->
            prefs[KEY_LAST_PRODUCTS_SYNC] = System.currentTimeMillis()
        }
    }

    // ==================== CATEGORIES ====================

    /**
     * Get last categories sync timestamp.
     */
    suspend fun getLastCategoriesSyncTimestamp(): Long {
        return context.syncDataStore.data.map { prefs ->
            prefs[KEY_LAST_CATEGORIES_SYNC] ?: 0L
        }.first()
    }

    /**
     * Update last categories sync timestamp to now.
     */
    suspend fun updateLastCategoriesSyncTimestamp() {
        context.syncDataStore.edit { prefs ->
            prefs[KEY_LAST_CATEGORIES_SYNC] = System.currentTimeMillis()
        }
    }

    // ==================== TRANSACTIONS ====================

    /**
     * Get last transactions sync timestamp.
     */
    suspend fun getLastTransactionsSyncTimestamp(): Long {
        return context.syncDataStore.data.map { prefs ->
            prefs[KEY_LAST_TRANSACTIONS_SYNC] ?: 0L
        }.first()
    }

    /**
     * Update last transactions sync timestamp to now.
     */
    suspend fun updateLastTransactionsSyncTimestamp() {
        context.syncDataStore.edit { prefs ->
            prefs[KEY_LAST_TRANSACTIONS_SYNC] = System.currentTimeMillis()
        }
    }

    // ==================== CASH RECORDS ====================

    /**
     * Get last cash records sync timestamp.
     */
    suspend fun getLastCashRecordsSyncTimestamp(): Long {
        return context.syncDataStore.data.map { prefs ->
            prefs[KEY_LAST_CASH_RECORDS_SYNC] ?: 0L
        }.first()
    }

    /**
     * Update last cash records sync timestamp to now.
     */
    suspend fun updateLastCashRecordsSyncTimestamp() {
        context.syncDataStore.edit { prefs ->
            prefs[KEY_LAST_CASH_RECORDS_SYNC] = System.currentTimeMillis()
        }
    }

    // ==================== ADD-ONS ====================

    /**
     * Get last add-ons sync timestamp.
     */
    suspend fun getLastAddOnsSyncTimestamp(): Long {
        return context.syncDataStore.data.map { prefs ->
            prefs[KEY_LAST_ADD_ONS_SYNC] ?: 0L
        }.first()
    }

    /**
     * Update last add-ons sync timestamp to now.
     */
    suspend fun updateLastAddOnsSyncTimestamp() {
        context.syncDataStore.edit { prefs ->
            prefs[KEY_LAST_ADD_ONS_SYNC] = System.currentTimeMillis()
        }
    }

    // ==================== SETTINGS ====================

    /**
     * Get last settings sync timestamp.
     */
    suspend fun getLastSettingsSyncTimestamp(): Long {
        return context.syncDataStore.data.map { prefs ->
            prefs[KEY_LAST_SETTINGS_SYNC] ?: 0L
        }.first()
    }

    /**
     * Update last settings sync timestamp to now.
     */
    suspend fun updateLastSettingsSyncTimestamp() {
        context.syncDataStore.edit { prefs ->
            prefs[KEY_LAST_SETTINGS_SYNC] = System.currentTimeMillis()
        }
    }

    // ==================== OVERALL SYNC ====================

    /**
     * Get last full sync timestamp.
     */
    suspend fun getLastFullSyncTimestamp(): Long {
        return context.syncDataStore.data.map { prefs ->
            prefs[KEY_LAST_FULL_SYNC] ?: 0L
        }.first()
    }

    /**
     * Update last full sync timestamp to now.
     */
    suspend fun updateLastFullSyncTimestamp() {
        context.syncDataStore.edit { prefs ->
            prefs[KEY_LAST_FULL_SYNC] = System.currentTimeMillis()
        }
    }

    /**
     * Check if sync is in progress.
     */
    fun isSyncInProgress(): Flow<Boolean> {
        return context.syncDataStore.data.map { prefs ->
            prefs[KEY_SYNC_IN_PROGRESS] ?: false
        }
    }

    /**
     * Set sync in progress flag.
     */
    suspend fun setSyncInProgress(inProgress: Boolean) {
        context.syncDataStore.edit { prefs ->
            prefs[KEY_SYNC_IN_PROGRESS] = inProgress
        }
    }

    /**
     * Reset all sync timestamps (force full sync).
     */
    suspend fun resetSyncTimestamps() {
        context.syncDataStore.edit { prefs ->
            prefs[KEY_LAST_PRODUCTS_SYNC] = 0L
            prefs[KEY_LAST_CATEGORIES_SYNC] = 0L
            prefs[KEY_LAST_TRANSACTIONS_SYNC] = 0L
            prefs[KEY_LAST_CASH_RECORDS_SYNC] = 0L
            prefs[KEY_LAST_ADD_ONS_SYNC] = 0L
            prefs[KEY_LAST_SETTINGS_SYNC] = 0L
            prefs[KEY_LAST_FULL_SYNC] = 0L
        }
    }
}


package com.zatiaras.pos.core.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.syncDataStore: DataStore<Preferences> by preferencesDataStore(name = "sync_prefs")

/**
 * Stores sync-related preferences using DataStore.
 * 
 * Tracks:
 * - Last sync timestamp for delta sync
 */
@Singleton
class SyncPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_LAST_PRODUCTS_SYNC = longPreferencesKey("last_products_sync")
        private val KEY_LAST_CATEGORIES_SYNC = longPreferencesKey("last_categories_sync")
    }

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

    /**
     * Reset all sync timestamps (force full sync).
     */
    suspend fun resetSyncTimestamps() {
        context.syncDataStore.edit { prefs ->
            prefs[KEY_LAST_PRODUCTS_SYNC] = 0L
            prefs[KEY_LAST_CATEGORIES_SYNC] = 0L
        }
    }
}

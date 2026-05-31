package com.zatiaras.pos.core.data.local.dao

import androidx.room.Dao

/**
 * Room entrypoint for product persistence.
 *
 * Read, mutation, sync, search, and paging methods live in focused inherited
 * contracts to keep each edit target small.
 */
@Dao
interface ProductDao :
    ProductReadDao,
    ProductWriteDao,
    ProductSyncDao,
    ProductSearchDao,
    ProductPagingDao

package com.zatiaras.pos.core.data.local.dao

import androidx.room.Dao

/**
 * Room entrypoint for add-on persistence.
 *
 * The contract is split by responsibility so readers can jump directly to
 * read, write, or sync operations without scanning one large DAO surface.
 */
@Dao
interface AddOnDao :
    AddOnReadDao,
    AddOnWriteDao,
    AddOnSyncDao

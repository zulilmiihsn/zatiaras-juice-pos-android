package com.zatiaras.pos.core.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Data class representing a Transaction with its Items.
 * 
 * Uses Room @Relation to fetch related items in a single query,
 * avoiding N+1 query problem where each transaction would trigger
 * a separate query for its items.
 */
data class TransactionWithItems(
    @Embedded
    val transaction: TransactionEntity,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "transactionId"
    )
    val items: List<TransactionItemEntity>
)

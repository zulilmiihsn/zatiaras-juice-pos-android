package com.zatiaras.pos.feature.pos.domain.model

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton holder for passing Transaction to Receipt screen.
 * 
 * This is a simple approach to pass complex objects between screens
 * without serialization. The transaction is cleared after being consumed.
 */
@Singleton
class TransactionHolder @Inject constructor() {
    
    private var pendingTransaction: Transaction? = null
    
    /**
     * Set a transaction to be displayed on receipt screen.
     */
    fun setTransaction(transaction: Transaction) {
        pendingTransaction = transaction
    }
    
    /**
     * Get and consume the pending transaction.
     * Returns null if no transaction is pending.
     */
    fun consumeTransaction(): Transaction? {
        val transaction = pendingTransaction
        pendingTransaction = null
        return transaction
    }
    
    /**
     * Peek at the pending transaction without consuming it.
     */
    fun peekTransaction(): Transaction? = pendingTransaction
    
    /**
     * Check if there's a pending transaction.
     */
    fun hasPendingTransaction(): Boolean = pendingTransaction != null
}

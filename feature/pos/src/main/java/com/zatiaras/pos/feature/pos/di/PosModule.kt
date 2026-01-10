package com.zatiaras.pos.feature.pos.di

import com.zatiaras.pos.core.data.local.dao.CashRecordDao
import com.zatiaras.pos.core.data.local.dao.TransactionDao
import com.zatiaras.pos.feature.pos.data.repository.CashRecordRepositoryImpl
import com.zatiaras.pos.feature.pos.data.repository.TransactionRepositoryImpl
import com.zatiaras.pos.feature.pos.domain.repository.CashRecordRepository
import com.zatiaras.pos.feature.pos.domain.repository.TransactionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for POS feature dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object PosModule {

    @Provides
    @Singleton
    fun provideTransactionRepository(
        transactionDao: TransactionDao
    ): TransactionRepository {
        return TransactionRepositoryImpl(transactionDao)
    }

    @Provides
    @Singleton
    fun provideCashRecordRepository(
        cashRecordDao: CashRecordDao
    ): CashRecordRepository {
        return CashRecordRepositoryImpl(cashRecordDao)
    }
}

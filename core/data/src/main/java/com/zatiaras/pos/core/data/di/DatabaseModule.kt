package com.zatiaras.pos.core.data.di

import android.content.Context
import androidx.room.Room
import com.zatiaras.pos.core.data.local.ZatiarasDatabase
import com.zatiaras.pos.core.data.local.Migrations
import com.zatiaras.pos.core.data.local.dao.CashRecordDao
import com.zatiaras.pos.core.data.local.dao.CategoryDao
import com.zatiaras.pos.core.data.local.dao.ProductDao
import com.zatiaras.pos.core.data.local.dao.TransactionDao
import com.zatiaras.pos.core.data.local.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for Room Database and DAOs.
 * 
 * Provides singleton instances to ensure consistent data access
 * across the entire application lifecycle.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): ZatiarasDatabase {
        return Room.databaseBuilder(
            context,
            ZatiarasDatabase::class.java,
            ZatiarasDatabase.DATABASE_NAME
        )
            // Use proper migrations to preserve user data
            .addMigrations(*Migrations.ALL_MIGRATIONS)
            .build()
    }

    @Provides
    @Singleton
    fun provideCategoryDao(database: ZatiarasDatabase): CategoryDao {
        return database.categoryDao()
    }

    @Provides
    @Singleton
    fun provideProductDao(database: ZatiarasDatabase): ProductDao {
        return database.productDao()
    }

    @Provides
    @Singleton
    fun provideTransactionDao(database: ZatiarasDatabase): TransactionDao {
        return database.transactionDao()
    }

    @Provides
    @Singleton
    fun provideCashRecordDao(database: ZatiarasDatabase): CashRecordDao {
        return database.cashRecordDao()
    }

    @Provides
    @Singleton
    fun provideUserDao(database: ZatiarasDatabase): UserDao {
        return database.userDao()
    }
}

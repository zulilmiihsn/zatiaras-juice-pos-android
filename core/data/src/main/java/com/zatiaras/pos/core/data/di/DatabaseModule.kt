package com.zatiaras.pos.core.data.di

import android.content.Context
import androidx.room.Room
import com.zatiaras.pos.core.data.local.ZatiarasDatabase
import com.zatiaras.pos.core.data.local.Migrations
import com.zatiaras.pos.core.data.local.dao.AddOnDao
import com.zatiaras.pos.core.data.local.dao.AppSettingsDao
import com.zatiaras.pos.core.data.local.dao.CashRecordDao
import com.zatiaras.pos.core.data.local.dao.CategoryDao
import com.zatiaras.pos.core.data.local.dao.ProductDao
import com.zatiaras.pos.core.data.local.dao.TransactionDao
import com.zatiaras.pos.core.data.local.dao.UserDao
import com.zatiaras.pos.core.data.local.dao.StoreSessionDao
import com.zatiaras.pos.core.data.session.SessionPreferences
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
            // IMPORTANT: Never use fallbackToDestructiveMigration() in production!
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

    @Provides
    @Singleton
    fun provideAppSettingsDao(database: ZatiarasDatabase): AppSettingsDao {
        return database.appSettingsDao()
    }

    @Provides
    @Singleton
    fun provideAddOnDao(database: ZatiarasDatabase): AddOnDao {
        return database.addOnDao()
    }

    @Provides
    @Singleton
    fun provideStoreSessionDao(database: ZatiarasDatabase): StoreSessionDao {
        return database.storeSessionDao()
    }

    @Provides
    @Singleton
    fun provideSessionPreferences(
        @ApplicationContext context: Context
    ): SessionPreferences {
        return SessionPreferences(context)
    }
}

package com.zatiaras.pos.feature.inventory.di

import com.zatiaras.pos.feature.inventory.data.repository.ProductRepositoryImpl
import com.zatiaras.pos.core.domain.repository.ProductRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for Inventory feature dependencies.
 * 
 * Binds repository interface to implementation.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class InventoryModule {

    @Binds
    @Singleton
    abstract fun bindProductRepository(
        impl: ProductRepositoryImpl
    ): ProductRepository
}

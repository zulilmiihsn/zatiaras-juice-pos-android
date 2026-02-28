package com.zatiaras.pos.core.data.di

import com.zatiaras.pos.core.data.repository.AddOnRepositoryImpl
import com.zatiaras.pos.core.data.repository.DashboardRepositoryImpl
import com.zatiaras.pos.core.data.repository.LocalAuthRepository
import com.zatiaras.pos.core.data.repository.StoreSessionRepositoryImpl
import com.zatiaras.pos.core.domain.AuthRepository
import com.zatiaras.pos.core.domain.repository.AddOnRepository
import com.zatiaras.pos.core.domain.repository.DashboardRepository
import com.zatiaras.pos.core.domain.repository.StoreSessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindAuthRepository(
        implementation: LocalAuthRepository
    ): AuthRepository

    @Binds
    abstract fun bindStoreSessionRepository(
        implementation: StoreSessionRepositoryImpl
    ): StoreSessionRepository

    @Binds
    abstract fun bindDashboardRepository(
        implementation: DashboardRepositoryImpl
    ): DashboardRepository

    @Binds
    abstract fun bindAddOnRepository(
        implementation: AddOnRepositoryImpl
    ): AddOnRepository
}

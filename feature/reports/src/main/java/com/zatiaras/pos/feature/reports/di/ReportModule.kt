package com.zatiaras.pos.feature.reports.di

import com.zatiaras.pos.feature.reports.data.repository.ReportRepositoryImpl
import com.zatiaras.pos.feature.reports.domain.repository.ReportRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ReportModule {
    
    @Binds
    @Singleton
    abstract fun bindReportRepository(
        impl: ReportRepositoryImpl
    ): ReportRepository
}

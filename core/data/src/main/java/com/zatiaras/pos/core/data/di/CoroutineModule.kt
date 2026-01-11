package com.zatiaras.pos.core.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Annotation to identify the application-level CoroutineScope.
 * Use this for background operations that should survive ViewModel destruction
 * but be cancelled when the application is destroyed.
 */
@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class ApplicationScope

/**
 * Hilt module for Coroutine dependencies.
 * 
 * Provides a single, managed CoroutineScope for background operations
 * instead of creating multiple unmanaged scopes throughout the app.
 */
@Module
@InstallIn(SingletonComponent::class)
object CoroutineModule {

    /**
     * Provides a singleton CoroutineScope for application-level operations.
     * Uses SupervisorJob so child failures don't cancel the entire scope.
     * Uses Dispatchers.Default for CPU-bound work (sync operations, parsing).
     */
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
}

package com.zatiaras.pos

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.zatiaras.BuildConfig
import com.zatiaras.pos.core.data.repository.AppSettingsRepository
import com.zatiaras.pos.core.data.sync.SyncManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.util.DebugLogger
import javax.inject.Inject

/**
 * Application class for ZatiarasPOS.
 * 
 * Implements Configuration.Provider for WorkManager Hilt integration.
 * Initializes:
 * - Timber for logging
 * - SyncManager for background sync
 * - AppSettingsRepository for settings
 */
@HiltAndroidApp
class ZatiarasApp : Application(), Configuration.Provider, ImageLoaderFactory {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncManager: SyncManager

    @Inject
    lateinit var appSettingsRepository: AppSettingsRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Initialize settings (creates default row if not exists)
        applicationScope.launch(Dispatchers.IO) {
            try {
                appSettingsRepository.initializeIfNeeded()
                Timber.d("AppSettings initialized")
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize AppSettings")
            }
        }

        // Initialize background sync
        syncManager.initialize()
        Timber.d("ZatiarasApp initialized")
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(
                if (BuildConfig.DEBUG) android.util.Log.DEBUG 
                else android.util.Log.INFO
            )
            .build()

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // Use 25% of available RAM
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50 * 1024 * 1024) // 50MB disk cache
                    .build()
            }
            .respectCacheHeaders(false) // Better performance, cache even if headers say otherwise
            .crossfade(true) // Smooth transitions
            .allowHardware(true) // Best performance
            .apply {
                if (BuildConfig.DEBUG) {
                    logger(DebugLogger())
                }
            }
            .build()
    }
}

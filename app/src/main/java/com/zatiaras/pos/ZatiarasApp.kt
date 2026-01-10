package com.zatiaras.pos

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.zatiaras.pos.core.data.sync.SyncManager
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

/**
 * Application class for Zatiaras POS.
 * 
 * Implements Configuration.Provider for WorkManager Hilt integration.
 * Initializes:
 * - Timber for logging
 * - SyncManager for background sync
 */
@HiltAndroidApp
class ZatiarasApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncManager: SyncManager

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
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
}


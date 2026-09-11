package com.sylvester.rustsensei

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.sylvester.rustsensei.llm.ModelManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class RustSenseiApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var modelManager: ModelManager

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler())

        // Partial downloads for models that are no longer offered can never be
        // resumed or deleted from the UI, so they sit in app storage forever -
        // up to ~1.2 GB each. Reap them once per process start, off the main
        // thread since it touches the filesystem.
        appScope.launch {
            try {
                modelManager.cleanupOrphanedTempFiles()
            } catch (e: Exception) {
                Log.w("RustSenseiApplication", "Temp file cleanup failed: ${e.message}")
            }
        }
    }

    // Lazy property — only accessed after Hilt injection completes in onCreate().
    // Using `by lazy` prevents UninitializedPropertyAccessException if WorkManager
    // is initialized before Hilt finishes field injection.
    override val workManagerConfiguration: Configuration by lazy {
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }
}

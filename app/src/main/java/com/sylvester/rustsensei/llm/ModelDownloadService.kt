package com.sylvester.rustsensei.llm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.sylvester.rustsensei.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * dataSync foreground service that runs the model download.
 *
 * Running the download here (instead of a viewModelScope coroutine holding a
 * PARTIAL_WAKE_LOCK) keeps the process alive across backgrounding/navigation
 * and aligns with the Play "excessive partial wake locks" battery vital.
 * Progress is published to [DownloadStateHolder] and mirrored in an ongoing,
 * non-dismissable progress notification.
 */
@AndroidEntryPoint
class ModelDownloadService : Service() {

    @Inject lateinit var modelManager: ModelManager
    @Inject lateinit var downloadState: DownloadStateHolder

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val modelInfo = intent?.getStringExtra(EXTRA_MODEL_ID)?.let { ModelManager.getModelById(it) }
        if (modelInfo == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundInternal(buildNotification(downloadedMB = 0, totalMB = 0, indeterminate = true))

        modelManager.downloadModel(modelInfo)
            .onEach { state ->
                downloadState.update(state)
                when (state) {
                    is DownloadState.Downloading -> updateNotification(state)
                    is DownloadState.Completed, is DownloadState.Error -> {
                        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                    is DownloadState.Idle -> {}
                }
            }
            .launchIn(scope)

        // Don't auto-restart with a null intent; the .tmp file lets the user resume.
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundInternal(notification: Notification) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    private fun updateNotification(state: DownloadState.Downloading) {
        val notification = buildNotification(
            downloadedMB = state.downloadedMB,
            totalMB = state.totalMB,
            progress = (state.progress * 100).toInt().coerceIn(0, 100),
            indeterminate = state.totalMB <= 0
        )
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(
        downloadedMB: Long,
        totalMB: Long,
        progress: Int = 0,
        indeterminate: Boolean = false
    ): Notification {
        val text = if (totalMB > 0) {
            getString(R.string.notification_downloading_progress, downloadedMB, totalMB)
        } else {
            getString(R.string.notification_downloading)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_downloading))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, indeterminate)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_download_name),
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "rustsensei_download_channel"
        const val NOTIFICATION_ID = 2
        const val EXTRA_MODEL_ID = "extra_model_id"

        /** Starts the download as a foreground service. Must be called while the app is foreground. */
        fun start(context: Context, modelId: String) {
            val intent = Intent(context, ModelDownloadService::class.java)
                .putExtra(EXTRA_MODEL_ID, modelId)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}

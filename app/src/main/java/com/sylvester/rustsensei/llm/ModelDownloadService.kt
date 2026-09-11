package com.sylvester.rustsensei.llm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.sylvester.rustsensei.MainActivity
import com.sylvester.rustsensei.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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

    /** Non-null while a download is in flight. Guards against a second start. */
    private var downloadJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) {
            // User tapped Cancel on the notification. The partial .tmp is kept so
            // the next start resumes instead of re-downloading 1.2 GB.
            downloadJob?.cancel()
            downloadJob = null
            downloadState.update(DownloadState.Idle)
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val modelInfo = intent?.getStringExtra(EXTRA_MODEL_ID)?.let { ModelManager.getModelById(it) }
        if (modelInfo == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        // A second start while a download runs would open a second writer on the
        // same .tmp file and interleave bytes into a corrupt model.
        if (downloadJob?.isActive == true) {
            Log.i(TAG, "Download already in flight; ignoring duplicate start")
            return START_NOT_STICKY
        }

        // startForeground throws on API 31+ if the process lost its foreground
        // eligibility between the caller's startForegroundService() and here.
        try {
            startForegroundInternal(
                buildNotification(downloadedMB = 0, totalMB = 0, indeterminate = true)
            )
        } catch (e: Exception) {
            Log.w(TAG, "Could not enter foreground: ${e.message}")
            downloadState.update(
                DownloadState.Error("Could not start the download. Reopen RustSensei and try again.")
            )
            stopSelf()
            return START_NOT_STICKY
        }

        downloadJob = modelManager.downloadModel(modelInfo)
            .onEach { state ->
                downloadState.update(state)
                when (state) {
                    is DownloadState.Downloading -> updateNotification(state)
                    is DownloadState.Completed, is DownloadState.Error -> {
                        downloadJob = null
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
        // notify() is a no-op when POST_NOTIFICATIONS is denied, and can throw if
        // the channel was removed by the user. Progress is also shown in-app.
        try {
            getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.w(TAG, "Could not update download notification: ${e.message}")
        }
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
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val cancelIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, ModelDownloadService::class.java).setAction(ACTION_CANCEL),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_downloading))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(contentIntent)
            .addAction(0, getString(R.string.download_cancel), cancelIntent)
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
        private const val TAG = "ModelDownloadService"
        const val CHANNEL_ID = "rustsensei_download_channel"
        const val NOTIFICATION_ID = 2
        const val EXTRA_MODEL_ID = "extra_model_id"
        const val ACTION_CANCEL = "com.sylvester.rustsensei.action.CANCEL_DOWNLOAD"

        /**
         * Starts the download as a foreground service. Must be called while the app
         * is foreground; returns false if the platform refused the start so the
         * caller can surface an error instead of hanging on a progress spinner.
         */
        fun start(context: Context, modelId: String): Boolean = try {
            val intent = Intent(context, ModelDownloadService::class.java)
                .putExtra(EXTRA_MODEL_ID, modelId)
            ContextCompat.startForegroundService(context, intent)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Could not start download service: ${e.message}")
            false
        }

        /** Cancels an in-flight download, keeping the partial file for resume. */
        fun cancel(context: Context) {
            try {
                context.startService(
                    Intent(context, ModelDownloadService::class.java).setAction(ACTION_CANCEL)
                )
            } catch (e: Exception) {
                Log.w(TAG, "Could not cancel download: ${e.message}")
            }
        }
    }
}

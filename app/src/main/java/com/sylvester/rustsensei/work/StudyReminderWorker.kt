package com.sylvester.rustsensei.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sylvester.rustsensei.MainActivity
import com.sylvester.rustsensei.R
import android.util.Log
import com.sylvester.rustsensei.data.FlashCardDao
import com.sylvester.rustsensei.data.PreferencesManager
import com.sylvester.rustsensei.data.ProgressRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar

@HiltWorker
class StudyReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val flashCardDao: FlashCardDao,
    private val progressRepository: ProgressRepository,
    private val preferencesManager: PreferencesManager
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "StudyReminderWorker"
        const val CHANNEL_ID = "study_reminders"
        private const val NOTIFICATION_ID_FLASHCARDS = 1001
        private const val NOTIFICATION_ID_STREAK = 1002

        private const val KIND_FLASHCARDS = "flashcards"
        private const val KIND_STREAK = "streak"

        /** Reminders are suppressed outside these hours (local time). */
        const val QUIET_HOURS_END = 9    // reminders start at 09:00
        const val QUIET_HOURS_START = 21 // reminders stop at 21:00

        /**
         * True when [hourOfDay] (0-23) falls in the do-not-disturb window. The
         * worker repeats every 4 hours, so without this a study reminder lands
         * at 01:00 and again at 05:00.
         */
        fun isQuietHour(hourOfDay: Int): Boolean =
            hourOfDay < QUIET_HOURS_END || hourOfDay >= QUIET_HOURS_START
    }

    override suspend fun doWork(): Result {
        // The user can revoke reminders while work is already enqueued.
        if (!preferencesManager.areRemindersEnabled()) return Result.success()

        // This worker repeats every 4 hours, so without a quiet-hours guard a
        // study reminder lands at 01:00 and 05:00.
        if (isQuietHours()) return Result.success()

        return try {
            ensureNotificationChannel()

            val dueCards = flashCardDao.getDueCardCountSync(System.currentTimeMillis())
            if (dueCards > 0 && shouldNotifyToday(KIND_FLASHCARDS)) {
                sendNotification(
                    id = NOTIFICATION_ID_FLASHCARDS,
                    title = applicationContext.getString(R.string.reminder_flashcards_title),
                    body = applicationContext.resources.getQuantityString(
                        R.plurals.reminder_flashcards_body, dueCards, dueCards
                    )
                )
                markNotifiedToday(KIND_FLASHCARDS)
            }

            val streak = progressRepository.calculateStreak()
            val hasStudiedToday = progressRepository.hasStudiedToday()
            if (streak > 0 && !hasStudiedToday && shouldNotifyToday(KIND_STREAK)) {
                sendNotification(
                    id = NOTIFICATION_ID_STREAK,
                    title = applicationContext.getString(R.string.reminder_streak_title),
                    body = applicationContext.getString(R.string.reminder_streak_body, streak)
                )
                markNotifiedToday(KIND_STREAK)
            }

            Result.success()
        } catch (e: Exception) {
            // A reminder is not worth a retry storm; the next periodic run covers it.
            Log.w(TAG, "Reminder run failed: ${e.message}", e)
            Result.success()
        }
    }

    private fun isQuietHours(): Boolean =
        isQuietHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))

    private fun today(): String {
        val cal = Calendar.getInstance()
        return "%04d-%02d-%02d".format(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    private fun shouldNotifyToday(kind: String): Boolean =
        preferencesManager.getLastReminderDate(kind) != today()

    private fun markNotifiedToday(kind: String) =
        preferencesManager.setLastReminderDate(kind, today())

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                applicationContext.getString(R.string.study_reminders_channel),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = applicationContext.getString(R.string.study_reminders_channel_desc)
            }
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification(id: Int, title: String, body: String) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .build()

        try {
            applicationContext.getSystemService(NotificationManager::class.java)
                .notify(id, notification)
        } catch (e: Exception) {
            // notify() throws if the channel was deleted by the user.
            Log.w(TAG, "Could not post reminder: ${e.message}")
        }
    }
}

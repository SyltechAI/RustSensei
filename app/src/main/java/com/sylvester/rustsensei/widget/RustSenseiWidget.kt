package com.sylvester.rustsensei.widget

import android.content.Context
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.sylvester.rustsensei.MainActivity
import com.sylvester.rustsensei.R
import com.sylvester.rustsensei.data.AppDatabase
import com.sylvester.rustsensei.data.ProgressRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RustSenseiWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadData(context)

        provideContent {
            GlanceTheme {
                WidgetContent(context, data)
            }
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        provideContent {
            GlanceTheme {
                WidgetContent(
                    context,
                    WidgetData(streakDays = 7, dueFlashcards = 5, completedExercises = 12)
                )
            }
        }
    }

    private suspend fun loadData(context: Context): WidgetData {
        return try {
            val db = AppDatabase.getDatabase(context)
            val progressRepo = ProgressRepository(db.progressDao())
            val dueCards = db.flashCardDao().getDueCardCountSync(System.currentTimeMillis())
            val streak = progressRepo.calculateStreak()
            WidgetData(
                streakDays = streak,
                dueFlashcards = dueCards,
                completedExercises = 0
            )
        } catch (_: Exception) {
            WidgetData(streakDays = 0, dueFlashcards = 0, completedExercises = 0)
        }
    }

    @Composable
    private fun WidgetContent(context: Context, data: WidgetData) {
        val bgColor = ColorProvider(Color(0xFF0A0E14))
        val textColor = ColorProvider(Color(0xFFE8ECF0))
        val accentColor = ColorProvider(Color(0xFFCE412B))
        val subtextColor = ColorProvider(Color(0xFF8B95A5))

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(bgColor)
                .padding(16.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.Top,
            horizontalAlignment = Alignment.Start
        ) {
            // Title
            Text(
                text = context.getString(R.string.app_name),
                style = TextStyle(
                    color = accentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = GlanceModifier.height(12.dp))

            // Streak
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "\uD83D\uDD25",
                    style = TextStyle(fontSize = 20.sp)
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                Column {
                    Text(
                        text = "${data.streakDays}",
                        style = TextStyle(
                            color = textColor,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = context.getString(R.string.widget_streak_label),
                        style = TextStyle(
                            color = subtextColor,
                            fontSize = 11.sp
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                // Due flashcards
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${data.dueFlashcards}",
                        style = TextStyle(
                            color = textColor,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = context.getString(R.string.widget_cards_due),
                        style = TextStyle(
                            color = subtextColor,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(12.dp))

            // CTA
            Text(
                text = context.getString(R.string.widget_continue),
                style = TextStyle(
                    color = accentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

class RustSenseiWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RustSenseiWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // Push a Compose-rendered preview to the Android 15+ widget picker.
        // Best-effort: on failure the picker falls back to the manifest preview.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            val pending = goAsync()
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    GlanceAppWidgetManager(context).setWidgetPreviews(RustSenseiWidgetReceiver::class)
                } catch (_: Exception) {
                    // ignore — preview is a progressive enhancement
                } finally {
                    pending.finish()
                }
            }
        }
    }
}

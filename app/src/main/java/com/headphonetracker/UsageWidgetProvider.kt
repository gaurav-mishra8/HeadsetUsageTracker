package com.headphonetracker

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.headphonetracker.di.AppEntryPoints
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UsageWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_UPDATE_WIDGET -> {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, UsageWidgetProvider::class.java)
                )
                onUpdate(context, appWidgetManager, appWidgetIds)
            }
            ACTION_TOGGLE_TRACKING -> {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext, AppEntryPoints::class.java
                )
                val isTracking = entryPoint.getSettingsRepository().isTracking()
                val serviceIntent = Intent(context, HeadphoneTrackingService::class.java).apply {
                    action = if (isTracking) "STOP_TRACKING" else "START_TRACKING"
                }
                if (!isTracking && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }

    companion object {
        const val ACTION_UPDATE_WIDGET = "com.headphonetracker.UPDATE_WIDGET"
        private const val ACTION_TOGGLE_TRACKING = "com.headphonetracker.TOGGLE_TRACKING"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            CoroutineScope(Dispatchers.Main).launch {
                val appCtx = context.applicationContext
                val entryPoint = EntryPointAccessors.fromApplication(
                    appCtx, AppEntryPoints::class.java
                )
                val dao = entryPoint.getHeadphoneUsageDao()
                val settingsRepository = entryPoint.getSettingsRepository()
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                val totalDuration = withContext(Dispatchers.IO) {
                    dao.getTotalUsageForDate(today) ?: 0L
                }
                val weightedMinutes = withContext(Dispatchers.IO) {
                    dao.getWeightedExposureMinutes(today) ?: 0f
                }
                val budgetPct = EarHealthCalculator.budgetPercent(weightedMinutes).coerceAtMost(100)
                val budgetColor = EarHealthCalculator.statusColor(budgetPct)

                val isTracking = settingsRepository.isTracking()
                val currentAppName = settingsRepository.getCurrentTrackingAppName()

                // Open app intent
                val openIntent = PendingIntent.getActivity(
                    context, 0,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Toggle tracking intent
                val toggleIntent = PendingIntent.getBroadcast(
                    context, 1,
                    Intent(context, UsageWidgetProvider::class.java).apply {
                        action = ACTION_TOGGLE_TRACKING
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val isGreen = budgetPct < 60
                val isAmber = budgetPct in 60..84
                val isRed = budgetPct >= 85
                val budgetLabel = EarHealthCalculator.scoreGrade(EarHealthCalculator.score(weightedMinutes))

                val views = RemoteViews(context.packageName, R.layout.widget_usage).apply {
                    setTextViewText(R.id.tvWidgetTime, formatDuration(totalDuration))
                    setTextViewText(R.id.tvWidgetStatus, if (isTracking) "● Tracking" else "● Inactive")
                    setTextViewText(
                        R.id.tvWidgetCurrentApp,
                        if (isTracking && currentAppName.isNotEmpty()) "▶ $currentAppName" else ""
                    )
                    setTextViewText(R.id.btnWidgetToggle, if (isTracking) "Stop" else "Start")
                    // Budget progress bar — show exactly one colored variant
                    setViewVisibility(R.id.progressBudgetGreen, if (isGreen) android.view.View.VISIBLE else android.view.View.GONE)
                    setViewVisibility(R.id.progressBudgetAmber, if (isAmber) android.view.View.VISIBLE else android.view.View.GONE)
                    setViewVisibility(R.id.progressBudgetRed, if (isRed) android.view.View.VISIBLE else android.view.View.GONE)
                    setProgressBar(R.id.progressBudgetGreen, 100, if (isGreen) budgetPct else 0, false)
                    setProgressBar(R.id.progressBudgetAmber, 100, if (isAmber) budgetPct else 0, false)
                    setProgressBar(R.id.progressBudgetRed, 100, if (isRed) budgetPct else 0, false)
                    setTextViewText(R.id.tvWidgetBudget, "Ear: $budgetPct% used · $budgetLabel")
                    setOnClickPendingIntent(R.id.tvWidgetTime, openIntent)
                    setOnClickPendingIntent(R.id.btnWidgetToggle, toggleIntent)
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }

        private fun formatDuration(ms: Long): String {
            val hours = ms / 3_600_000
            val minutes = (ms % 3_600_000) / 60_000

            return when {
                hours > 0 -> "${hours}h ${minutes}m"
                minutes > 0 -> "${minutes}m"
                else -> "0m"
            }
        }

        fun sendUpdateBroadcast(context: Context) {
            val intent = Intent(context, UsageWidgetProvider::class.java).apply {
                action = ACTION_UPDATE_WIDGET
            }
            context.sendBroadcast(intent)
        }
    }
}

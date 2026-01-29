package com.headphonetracker

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.headphonetracker.di.AppEntryPoints
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

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
        
        if (intent.action == ACTION_UPDATE_WIDGET) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, UsageWidgetProvider::class.java)
            )
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    companion object {
        const val ACTION_UPDATE_WIDGET = "com.headphonetracker.UPDATE_WIDGET"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            CoroutineScope(Dispatchers.Main).launch {
                val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, AppEntryPoints::class.java)
                val dao = entryPoint.getHeadphoneUsageDao()
                val settingsRepository = entryPoint.getSettingsRepository()
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                val totalDuration = withContext(Dispatchers.IO) {
                    dao.getTotalUsageForDate(today) ?: 0L
                }

                val isTracking = settingsRepository.isTracking()

                val views = RemoteViews(context.packageName, R.layout.widget_usage).apply {
                    setTextViewText(R.id.tvWidgetTime, formatDuration(totalDuration))
                    setTextViewText(R.id.tvWidgetStatus, if (isTracking) "● Tracking" else "● Inactive")
                    
                    // Set click to open app
                    val intent = Intent(context, MainActivity::class.java)
                    val pendingIntent = PendingIntent.getActivity(
                        context, 0, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    setOnClickPendingIntent(R.id.tvWidgetTime, pendingIntent)
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }

        private fun formatDuration(seconds: Long): String {
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60

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



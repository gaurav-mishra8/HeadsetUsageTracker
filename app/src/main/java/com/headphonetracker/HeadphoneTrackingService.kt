package com.headphonetracker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.headphonetracker.data.HeadphoneUsage
import com.headphonetracker.data.HeadphoneUsageDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import com.headphonetracker.notifications.MilestoneChecker
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class HeadphoneTrackingService : LifecycleService() {

    private val audioManager by lazy { getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    private val usageStatsManager by lazy { getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager }
    private val powerManager by lazy { getSystemService(Context.POWER_SERVICE) as PowerManager }
    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    private val pkgManager by lazy { applicationContext.packageManager }

    @Inject
    lateinit var headphoneUsageDao: HeadphoneUsageDao

    @Inject
    lateinit var settingsRepository: com.headphonetracker.data.SettingsRepository

    private var trackingJob: Job? = null
    private var isTracking = false
    private var currentSessionStart: Long = 0
    private var lastPackageName: String? = null
    private var lastAudioApp: String? = null  // Last app that was confirmed playing audio
    private var lastSaveTime: Long = 0
    private var lastBreakReminder: Long = 0
    private var lastLimitWarning: Long = 0
    private var lastMilestoneCheck: Long = 0
    private var sessionStartTime: Long = 0 // For break reminders

    private val wakeLock by lazy {
        powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HeadphoneTracker::WakeLock")
    }

    companion object {
        private const val TAG = "HeadphoneTracker"
        private const val NOTIFICATION_ID = 1
        private const val BREAK_NOTIFICATION_ID = 2
        private const val LIMIT_NOTIFICATION_ID = 3
        private const val CHANNEL_ID = "headphone_tracker_channel"
        private const val ALERTS_CHANNEL_ID = "headphone_alerts_channel"
        private const val TRACKING_INTERVAL = 1000L // 1 second
        private const val SAVE_INTERVAL = 5000L // Save every 5 seconds
        private const val WIDGET_UPDATE_INTERVAL = 60000L // Update widget every minute

        val isTrackingFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            "START_TRACKING" -> startTracking()
            "STOP_TRACKING" -> stopTracking()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Headphone Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Tracks headphone usage across apps"
                setShowBadge(false)
            }

            val alertsChannel = NotificationChannel(
                ALERTS_CHANNEL_ID,
                "Health Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Break reminders and limit warnings"
                enableVibration(true)
            }

            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
            nm.createNotificationChannel(alertsChannel)
        }
    }

    private fun createNotification(contentText: String? = null): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, HeadphoneTrackingService::class.java).apply {
            action = "STOP_TRACKING"
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val text = contentText ?: getString(R.string.service_notification_text)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.service_notification_title))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notif_headphones)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .addAction(
                android.R.drawable.ic_media_pause,
                "Stop",
                stopPendingIntent
            )
            .build()
    }

    private fun updateNotification(text: String) {
        val notification = createNotification(text)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun updateTrackingNotification() {
        val elapsed = System.currentTimeMillis() - sessionStartTime
        val hours = (elapsed / 3_600_000).toInt()
        val minutes = ((elapsed % 3_600_000) / 60_000).toInt()
        val seconds = ((elapsed % 60_000) / 1_000).toInt()

        val duration = when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }

        val currentApp = lastPackageName
        val appLabel = if (currentApp != null) {
            try {
                val appInfo = pkgManager.getApplicationInfo(currentApp, 0)
                pkgManager.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) {
                null
            }
        } else null

        val text = when {
            appLabel != null -> "▶ $appLabel · $duration"
            isAudioPlaying() -> "Listening · $duration"
            else -> "Monitoring · $duration"
        }

        updateNotification(text)
    }

    private fun startTracking() {
        if (isTracking) return

        isTracking = true
        isTrackingFlow.value = true
        sessionStartTime = System.currentTimeMillis()
        settingsRepository.setTracking(true)
        wakeLock.acquire(10 * 60 * 60 * 1000L) // 10 hours

        // Update widget
        UsageWidgetProvider.sendUpdateBroadcast(this)

        trackingJob = lifecycleScope.launch {
            var widgetUpdateTime = 0L
            var notifUpdateTime = 0L
            while (isTracking) {
                checkHeadphoneUsage()
                checkBreakReminder()
                checkDailyLimit()
                checkMilestones()

                val now = System.currentTimeMillis()

                // Update notification every 10 seconds with live info
                if (now - notifUpdateTime > 10_000L) {
                    updateTrackingNotification()
                    notifUpdateTime = now
                }

                // Update widget periodically
                if (now - widgetUpdateTime > WIDGET_UPDATE_INTERVAL) {
                    UsageWidgetProvider.sendUpdateBroadcast(this@HeadphoneTrackingService)
                    widgetUpdateTime = now
                }

                delay(TRACKING_INTERVAL)
            }
        }
    }

    private fun stopTracking() {
        if (!isTracking) return

        isTracking = false
        isTrackingFlow.value = false
        settingsRepository.setTracking(false)
        trackingJob?.cancel()

        if (wakeLock.isHeld) {
            wakeLock.release()
        }

        lifecycleScope.launch {
            // Save current session if any
            lastPackageName?.let { packageName ->
                saveSession(packageName, currentSessionStart, System.currentTimeMillis())
            }

            lastPackageName = null
            lastAudioApp = null

            // Update widget
            UsageWidgetProvider.sendUpdateBroadcast(this@HeadphoneTrackingService)
        }
        stopSelf()
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod", "NestedBlockDepth")
    private suspend fun checkHeadphoneUsage() {
        val headphoneConnected = isHeadphoneConnected()
        val currentTime = System.currentTimeMillis()

        if (!headphoneConnected) {
            // Headphones disconnected, save current session
            if (lastPackageName != null && currentSessionStart > 0) {
                Log.d(TAG, "Headphones disconnected, saving session for $lastPackageName")
                saveSession(lastPackageName!!, currentSessionStart, currentTime)
                currentSessionStart = 0
                lastPackageName = null
                lastAudioApp = null
                lastSaveTime = 0
            }
            Log.d(TAG, "Check: headphone=false, no tracking")
            return
        }

        // Headphones connected — only track if audio is actually playing
        try {
            val audioActive = isAudioPlaying()

            if (!audioActive) {
                // No audio playing — save any existing session and stop tracking
                if (lastPackageName != null && currentSessionStart > 0) {
                    Log.d(TAG, "Audio stopped, saving session for $lastPackageName")
                    saveSession(lastPackageName!!, currentSessionStart, currentTime)
                    currentSessionStart = 0
                    lastPackageName = null
                    lastSaveTime = 0
                }
                lastAudioApp = null
                Log.d(TAG, "Check: headphone=true, audioActive=false, no tracking")
                return
            }

            // Audio is playing — identify which app is producing it
            val audioPlayingApp = getAudioPlayingApp()

            // Use detected audio app, or keep the last known audio app if detection fails
            // (some apps don't expose UID but are still playing)
            val currentPackage = audioPlayingApp ?: lastAudioApp

            if (audioPlayingApp != null) {
                lastAudioApp = audioPlayingApp
            }

            // Check excluded apps
            val excludedApps = settingsRepository.getExcludedApps()

            // Skip our own app, system/utility apps, and user-excluded apps
            val shouldTrack = currentPackage != null && currentPackage != packageName &&
                !currentPackage.startsWith("com.android.systemui") &&
                !AppCategories.skipPackages.contains(currentPackage) &&
                !excludedApps.contains(currentPackage)

            Log.d(TAG, "Check: headphone=true, audioActive=true, audioApp=$audioPlayingApp, " +
                "lastAudioApp=$lastAudioApp, tracking=$currentPackage")

            if (shouldTrack && currentPackage != null) {
                if (currentPackage != lastPackageName) {
                    // App changed, save previous session first
                    if (lastPackageName != null && currentSessionStart > 0) {
                        Log.d(TAG, "App changed from $lastPackageName to $currentPackage, saving previous session")
                        saveSession(lastPackageName!!, currentSessionStart, currentTime)
                    }

                    // Start new session
                    lastPackageName = currentPackage
                    currentSessionStart = currentTime
                    lastSaveTime = currentTime
                    Log.d(TAG, "Started new session for $currentPackage")
                } else {
                    // Same app, check if we should save incrementally
                    if (currentSessionStart > 0 && (currentTime - lastSaveTime) >= SAVE_INTERVAL) {
                        val incMsg = "Saving incremental session for $currentPackage, " +
                            "duration=${currentTime - currentSessionStart}ms"
                        Log.d(TAG, incMsg)
                        saveSession(currentPackage, currentSessionStart, currentTime)
                        // Update session start for next increment
                        currentSessionStart = currentTime
                        lastSaveTime = currentTime
                    }
                }
            } else if (!shouldTrack && lastPackageName != null && currentSessionStart > 0) {
                // Save session when switching to non-tracked app
                Log.d(TAG, "Switching to non-tracked app, saving session for $lastPackageName")
                saveSession(lastPackageName!!, currentSessionStart, currentTime)
                currentSessionStart = 0
                lastPackageName = null
                lastSaveTime = 0
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: ${e.message}")
            stopTracking()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking usage: ${e.message}")
        }
    }

    /**
     * Get the package name of the app currently playing audio.
     * Uses multiple strategies:
     * 1. AudioPlaybackConfiguration client UID (most accurate, but often returns -1)
     * 2. UsageEvents + known media app list (reliable fallback)
     */
    @Suppress("DEPRECATION")
    private fun getAudioPlayingApp(): String? {
        // Strategy 1: Try AudioPlaybackConfiguration client UID
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val activePlaybacks = audioManager.activePlaybackConfigurations
            val isMusicActive = audioManager.isMusicActive

            if (activePlaybacks.isEmpty() && !isMusicActive) {
                return null
            }

            Log.d(TAG, "Active playbacks: ${activePlaybacks.size}, isMusicActive=$isMusicActive")

            for (config in activePlaybacks) {
                val usage = config.audioAttributes.usage
                Log.d(TAG, "Playback config: usage=$usage")

                if (usage == android.media.AudioAttributes.USAGE_MEDIA ||
                    usage == android.media.AudioAttributes.USAGE_GAME ||
                    usage == android.media.AudioAttributes.USAGE_VOICE_COMMUNICATION
                ) {
                    try {
                        val getClientUidMethod = config.javaClass.getMethod("getClientUid")
                        val clientUid = getClientUidMethod.invoke(config) as Int
                        Log.d(TAG, "Client UID: $clientUid")
                        if (clientUid > 0) {
                            val packages = packageManager.getPackagesForUid(clientUid)
                            if (!packages.isNullOrEmpty()) {
                                val pkg = packages[0]
                                Log.d(TAG, "Strategy 1 (UID): Audio from $pkg")
                                return pkg
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Could not get client UID: ${e.message}")
                    }
                }
            }
        }

        // Strategy 2: Use recent UsageEvents to find the most likely media app
        // Audio is confirmed playing, so find the most likely media app
        val recentApp = getMostRecentMediaApp()
        if (recentApp != null) {
            Log.d(TAG, "Strategy 2 (recent app+audio): Attributing to $recentApp")
            return recentApp
        }

        return null
    }

    private fun getCurrentForegroundApp(): String? {
        val currentTime = System.currentTimeMillis()
        // Query a larger window to find recent foreground events
        val startTime = currentTime - 60000 // Last minute

        try {
            // First try to get from usage events
            val events = usageStatsManager.queryEvents(startTime, currentTime)
            var lastForegroundPackage: String? = null
            var lastForegroundTime = 0L

            while (events.hasNextEvent()) {
                val event = UsageEvents.Event()
                events.getNextEvent(event)

                @Suppress("DEPRECATION")
                if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                    event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
                ) {
                    if (event.timeStamp > lastForegroundTime) {
                        lastForegroundPackage = event.packageName
                        lastForegroundTime = event.timeStamp
                    }
                }
            }

            if (lastForegroundPackage != null) {
                return lastForegroundPackage
            }

            // Fallback: use usage stats
            val usageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                currentTime
            )

            return usageStats?.maxByOrNull { it.lastTimeUsed }?.packageName
        } catch (e: Exception) {
            Log.e(TAG, "Error getting foreground app: ${e.message}")
            return null
        }
    }

    /**
     * Find the most recent media app from UsageEvents.
     * When audio is playing but we can't detect the source via UID or MediaSession,
     * look through recent foreground events and prefer known media apps.
     * Only falls back to a non-media app if absolutely nothing else is found,
     * and even then only if that app was foregrounded very recently (last 30s).
     */
    private fun getMostRecentMediaApp(): String? {
        val currentTime = System.currentTimeMillis()
        val startTime = currentTime - 120_000 // Last 2 minutes

        // Use shared registry — also skip our own package
        val skipPackages = AppCategories.skipPackages + packageName
        val mediaAppPrefixes = AppCategories.mediaAttributionPrefixes

        try {
            val events = usageStatsManager.queryEvents(startTime, currentTime)
            var bestMediaApp: String? = null
            var bestMediaTime = 0L

            while (events.hasNextEvent()) {
                val event = UsageEvents.Event()
                events.getNextEvent(event)

                @Suppress("DEPRECATION")
                if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                    event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
                ) {
                    val pkg = event.packageName

                    // Skip system/utility packages entirely
                    if (skipPackages.contains(pkg)) continue

                    // Only track known media apps — don't guess with random apps
                    val isMediaApp = mediaAppPrefixes.any { pkg.startsWith(it) }

                    if (isMediaApp && event.timeStamp > bestMediaTime) {
                        bestMediaApp = pkg
                        bestMediaTime = event.timeStamp
                    }
                }
            }

            if (bestMediaApp != null) {
                Log.d(TAG, "getMostRecentMediaApp: media app=$bestMediaApp")
            } else {
                Log.d(TAG, "getMostRecentMediaApp: no known media app found in last 10 min")
            }

            // ONLY return known media apps — never attribute to Gmail, Settings, etc.
            return bestMediaApp
        } catch (e: Exception) {
            Log.e(TAG, "Error finding recent media app: ${e.message}")
            return null
        }
    }

    private fun isHeadphoneConnected(): Boolean {
        // Use modern AudioDeviceInfo API for Android M+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            for (device in devices) {
                val type = device.type
                if (type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                    type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && type == AudioDeviceInfo.TYPE_BLE_BROADCAST)
                ) {
                    Log.d(TAG, "Headphone connected: type=$type, name=${device.productName}")
                    return true
                }
            }
            return false
        }

        // Fallback for older devices
        @Suppress("DEPRECATION")
        return audioManager.isWiredHeadsetOn || audioManager.isBluetoothA2dpOn
    }

    private fun isAudioPlaying(): Boolean {
        // Check multiple audio focus states
        val isMusicActive = audioManager.isMusicActive

        // Check if a phone/VoIP call is active via AudioManager mode
        val audioMode = audioManager.mode
        val isInCall = audioMode == AudioManager.MODE_IN_CALL ||
            audioMode == AudioManager.MODE_IN_COMMUNICATION

        // For Android 8+, also check if any audio playback is happening
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Check active playback configurations
            val activePlaybacks = audioManager.activePlaybackConfigurations
            val hasActivePlayback = activePlaybacks.any { config ->
                val usage = config.audioAttributes.usage
                usage == android.media.AudioAttributes.USAGE_MEDIA ||
                    usage == android.media.AudioAttributes.USAGE_GAME ||
                    usage == android.media.AudioAttributes.USAGE_VOICE_COMMUNICATION
            }

            val audioMsg = buildString {
                append("Audio check: isMusicActive=")
                append(isMusicActive)
                append(", isInCall=")
                append(isInCall)
                append(", activePlaybacks=")
                append(activePlaybacks.size)
                append(", hasActivePlayback=")
                append(hasActivePlayback)
            }
            Log.d(TAG, audioMsg)
            return isMusicActive || hasActivePlayback || isInCall
        }

        return isMusicActive || isInCall
    }

    private suspend fun saveSession(packageName: String, startTime: Long, endTime: Long) {
        if (endTime <= startTime) {
            Log.w(TAG, "saveSession: endTime <= startTime, skipping")
            return
        }

        val duration = endTime - startTime
        if (duration < 1000) {
            Log.w(TAG, "saveSession: duration < 1s ($duration ms), skipping")
            return
        }

        try {
            // Try to get app name, use package name as fallback
            val appName = try {
                val appInfo = pkgManager.getApplicationInfo(packageName, 0)
                pkgManager.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                Log.w(TAG, "Could not get app label for $packageName, using package name")
                packageName.substringAfterLast(".")
            }

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = dateFormat.format(Date(startTime))

            val usage = HeadphoneUsage(
                packageName = packageName,
                appName = appName,
                startTime = startTime,
                endTime = endTime,
                duration = duration,
                date = date
            )

            // Insert via injected DAO
            headphoneUsageDao.insertUsage(usage)
            Log.d(TAG, "SAVED to DB: $appName ($packageName), duration=${duration}ms, date=$date")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save session for $packageName: ${e.message}")
        }
    }

    private fun checkBreakReminder() {
        val breakRemindersEnabled = settingsRepository.isBreakRemindersEnabled()
        if (!breakRemindersEnabled) return

        val intervalMinutes = settingsRepository.getBreakIntervalMinutes()
        val intervalMs = intervalMinutes * 60 * 1000L
        val now = System.currentTimeMillis()

        // Check if enough time has passed since session start and last reminder
        val sessionDuration = now - sessionStartTime
        if (sessionDuration >= intervalMs && (now - lastBreakReminder) >= intervalMs) {
            showBreakReminder()
            lastBreakReminder = now
        }
    }

    private fun showBreakReminder() {
        val notification = NotificationCompat.Builder(this, ALERTS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Time for a break! 🎧")
            .setContentText("You've been listening for a while. Give your ears a rest.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(BREAK_NOTIFICATION_ID, notification)
    }

    private suspend fun checkDailyLimit() {
        val limitMinutes = settingsRepository.getDailyLimitMinutes()
        if (limitMinutes == 0) return

        val now = System.currentTimeMillis()
        // Only check every 5 minutes to avoid spam
        if ((now - lastLimitWarning) < 5 * 60 * 1000L) return

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val todayUsageMs = withContext(Dispatchers.IO) {
            headphoneUsageDao.getTotalUsageForDate(today) ?: 0L
        }

        val usageMinutes = todayUsageMs / 60_000L
        val limitMs = limitMinutes * 60_000L

        // Warn at 100%
        if (todayUsageMs >= limitMs && (now - lastLimitWarning) >= 30 * 60 * 1000L) {
            showLimitNotification("Daily limit reached!", "You've reached your $limitMinutes minute daily limit.")
            lastLimitWarning = now
        } else if (todayUsageMs >= (limitMs * 0.8).toLong() && usageMinutes < limitMinutes) {
            // Warn at 80%
            val remaining = limitMinutes - usageMinutes
            showLimitNotification("Approaching daily limit", "You have about $remaining minutes remaining.")
            lastLimitWarning = now
        }
    }

    private fun showLimitNotification(title: String, message: String) {
        val notification = NotificationCompat.Builder(this, ALERTS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(LIMIT_NOTIFICATION_ID, notification)
    }

    private suspend fun checkMilestones() {
        val now = System.currentTimeMillis()
        if ((now - lastMilestoneCheck) < 5 * 60 * 1000L) return
        lastMilestoneCheck = now
        MilestoneChecker.check(this, headphoneUsageDao, settingsRepository)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTracking()
    }
}

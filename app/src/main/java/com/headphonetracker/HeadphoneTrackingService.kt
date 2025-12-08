package com.headphonetracker

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.headphonetracker.data.AppDatabase
import com.headphonetracker.data.HeadphoneUsage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class HeadphoneTrackingService : LifecycleService() {
    
    private val audioManager by lazy { getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    private val usageStatsManager by lazy { getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager }
    private val powerManager by lazy { getSystemService(Context.POWER_SERVICE) as PowerManager }
    private val pkgManager by lazy { applicationContext.packageManager }
    private val database by lazy { AppDatabase.getDatabase(applicationContext) }
    
    private var trackingJob: Job? = null
    private var isTracking = false
    private var currentSessionStart: Long = 0
    private var lastPackageName: String? = null
    private var lastEventTime: Long = 0
    
    private val wakeLock by lazy {
        powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HeadphoneTracker::WakeLock")
    }
    
    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "headphone_tracker_channel"
        private const val TRACKING_INTERVAL = 1000L // 1 second
        private const val SESSION_TIMEOUT = 5000L // 5 seconds
        
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
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.service_notification_title))
            .setContentText(getString(R.string.service_notification_text))
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
    
    private fun startTracking() {
        if (isTracking) return
        
        isTracking = true
        isTrackingFlow.value = true
        wakeLock.acquire(10 * 60 * 60 * 1000L) // 10 hours
        
        trackingJob = lifecycleScope.launch {
            while (isTracking) {
                checkHeadphoneUsage()
                delay(TRACKING_INTERVAL)
            }
        }
    }
    
    private fun stopTracking() {
        isTracking = false
        isTrackingFlow.value = false
        trackingJob?.cancel()
        wakeLock.release()

        lifecycleScope.launch {
            // Save current session if any
            lastPackageName?.let { packageName ->
                saveSession(packageName, currentSessionStart, System.currentTimeMillis())
            }

            lastPackageName = null
        }
        stopSelf()
    }
    
    private suspend fun checkHeadphoneUsage() {
        if (!isHeadphoneConnected()) {
            // Headphones disconnected, save current session
            lastPackageName?.let { packageName ->
                if (currentSessionStart > 0) {
                    saveSession(packageName, currentSessionStart, System.currentTimeMillis())
                    currentSessionStart = 0
                }
            }
            lastPackageName = null
            return
        }
        
        val currentTime = System.currentTimeMillis()
        val endTime = currentTime
        val startTime = endTime - TRACKING_INTERVAL
        
        try {
            val events = usageStatsManager.queryEvents(startTime, endTime)
            var currentPackage: String? = null
            
            while (events.hasNextEvent()) {
                val event = UsageEvents.Event()
                events.getNextEvent(event)
                
                if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                    event.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                    currentPackage = event.packageName
                    lastEventTime = event.timeStamp
                }
            }
            
            // Check for currently running app
            if (currentPackage == null) {
                val runningApps = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_BEST,
                    startTime,
                    endTime
                )
                
                runningApps?.maxByOrNull { it.lastTimeUsed }?.let {
                    currentPackage = it.packageName
                }
            }
            
            // Check if audio is playing
            if (currentPackage != null && isAudioPlaying()) {
                if (currentPackage != lastPackageName) {
                    // App changed, save previous session
                    lastPackageName?.let { packageName ->
                        if (currentSessionStart > 0) {
                            saveSession(packageName, currentSessionStart, System.currentTimeMillis())
                        }
                    }
                    
                    // Start new session
                    lastPackageName = currentPackage
                    currentSessionStart = System.currentTimeMillis()
                } else if (currentSessionStart == 0L) {
                    // Same app, continue session
                    currentSessionStart = System.currentTimeMillis()
                }
                
                // Update session end time periodically
                if (abs(currentTime - lastEventTime) < SESSION_TIMEOUT && currentSessionStart > 0) {
                    // Session is active, will be saved when it ends
                }
            } else {
                // No audio or app changed, check if we should end session
                if (lastPackageName != null && currentSessionStart > 0) {
                    if (currentTime - lastEventTime > SESSION_TIMEOUT) {
                        // Session timeout, save it
                        saveSession(lastPackageName!!, currentSessionStart, currentTime)
                        currentSessionStart = 0
                        lastPackageName = null
                    }
                }
            }
            
        } catch (e: SecurityException) {
            // Permission not granted
            stopTracking()
        }
    }
    
    private fun isHeadphoneConnected(): Boolean {
        return audioManager.isWiredHeadsetOn || 
               audioManager.isBluetoothA2dpOn ||
               (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && 
                audioManager.isBluetoothScoOn)
    }
    
    private fun isAudioPlaying(): Boolean {
        return audioManager.isMusicActive
    }
    
    private suspend fun saveSession(packageName: String, startTime: Long, endTime: Long) {
        if (endTime <= startTime) return
        
        val duration = endTime - startTime
        if (duration < 1000) return // Ignore sessions less than 1 second
        
        try {
            val appInfo = pkgManager.getApplicationInfo(packageName, 0)
            val appName = pkgManager.getApplicationLabel(appInfo).toString()
            
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
            
            database.headphoneUsageDao().insertUsage(usage)
        } catch (e: Exception) {
            // App not found or other error, skip
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopTracking()
        wakeLock.release()
    }
}


package com.headphonetracker

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

/**
 * A minimal NotificationListenerService.
 * 
 * Its primary purpose is NOT to process notifications, but to provide
 * the permission needed for MediaSessionManager.getActiveSessions()
 * to work. Once the user grants Notification Access to this app,
 * the tracking service can query active media sessions to accurately
 * detect which app is playing audio.
 */
class MediaNotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "HeadphoneTracker"
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // No-op: we don't need to process notifications
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // No-op
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "NotificationListener connected — MediaSession access enabled")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "NotificationListener disconnected")
    }
}

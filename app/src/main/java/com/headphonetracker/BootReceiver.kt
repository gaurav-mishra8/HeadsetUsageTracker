package com.headphonetracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed received")
            
            val prefs = context.getSharedPreferences("headphone_tracker_prefs", Context.MODE_PRIVATE)
            val autoStartEnabled = prefs.getBoolean("auto_start_enabled", false)
            
            if (autoStartEnabled) {
                Log.d(TAG, "Auto-start enabled, starting tracking service...")
                
                val serviceIntent = Intent(context, HeadphoneTrackingService::class.java)
                
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                    Log.d(TAG, "Tracking service started successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start tracking service: ${e.message}")
                }
            } else {
                Log.d(TAG, "Auto-start is disabled")
            }
        }
    }
}


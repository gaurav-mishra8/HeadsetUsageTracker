package com.headphonetracker

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.headphonetracker.di.AppEntryPoints
import com.headphonetracker.notifications.DailySummaryScheduler
import com.headphonetracker.sync.DriveSyncScheduler
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.EntryPointAccessors

@HiltAndroidApp
class TrackerApplication : Application() {

	override fun onCreate() {
		super.onCreate()

		val entryPoint = EntryPointAccessors.fromApplication(
			this,
			AppEntryPoints::class.java
		)
		val settingsRepository = entryPoint.getSettingsRepository()

		when (settingsRepository.getAppTheme()) {
			"light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
			"dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
			else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
		}

		if (settingsRepository.isDriveSyncEnabled()) {
			DriveSyncScheduler.schedule(this, settingsRepository.getDriveSyncIntervalMinutes())
		} else {
			DriveSyncScheduler.cancel(this)
		}

		if (settingsRepository.isDailySummaryEnabled()) {
			DailySummaryScheduler.schedule(this)
		}
	}
}

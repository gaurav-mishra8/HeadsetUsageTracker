package com.headphonetracker

import android.app.Application
import com.headphonetracker.di.AppEntryPoints
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

		if (settingsRepository.isDriveSyncEnabled()) {
			DriveSyncScheduler.schedule(this, settingsRepository.getDriveSyncIntervalMinutes())
		} else {
			DriveSyncScheduler.cancel(this)
		}
	}
}

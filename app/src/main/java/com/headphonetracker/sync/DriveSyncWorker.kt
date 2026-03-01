package com.headphonetracker.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.headphonetracker.data.BackupJsonUtils
import com.headphonetracker.di.AppEntryPoints
import dagger.hilt.android.EntryPointAccessors

class DriveSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            AppEntryPoints::class.java
        )
        val settingsRepository = entryPoint.getSettingsRepository()
        val dao = entryPoint.getHeadphoneUsageDao()

        if (!settingsRepository.isDriveSyncEnabled()) {
            return Result.success()
        }

        val driveSyncManager = DriveSyncManager(applicationContext)
        if (driveSyncManager.getSignedInAccount() == null) {
            return Result.success()
        }

        val allData = dao.getAllUsage()
        if (allData.isEmpty()) {
            return Result.success()
        }

        return try {
            val jsonString = BackupJsonUtils.createBackupJson(allData).toString(2)
            driveSyncManager.uploadBackup(jsonString)
            settingsRepository.setDriveLastSyncTime(System.currentTimeMillis())
            settingsRepository.setDriveLastError("")
            Result.success()
        } catch (e: Exception) {
            settingsRepository.setDriveLastError(e.message ?: "Unknown error")
            Result.retry()
        }
    }
}

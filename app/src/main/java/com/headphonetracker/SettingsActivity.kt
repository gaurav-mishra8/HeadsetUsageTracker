package com.headphonetracker

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.headphonetracker.data.BackupJsonUtils
import com.headphonetracker.data.HeadphoneUsageDao
import com.headphonetracker.databinding.ActivitySettingsBinding
import com.headphonetracker.sync.DriveSyncScheduler
import com.headphonetracker.sync.DriveSyncManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    @Inject
    lateinit var headphoneUsageDao: HeadphoneUsageDao

    @Inject
    lateinit var settingsRepository: com.headphonetracker.data.SettingsRepository

    private val driveSyncManager by lazy { DriveSyncManager(this) }
    private var pendingDriveAction: DriveAction? = null

    private val restoreFilePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { restoreFromUri(it) }
    }

    private val driveSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            pendingDriveAction = null
            Toast.makeText(this, "Google Drive sign-in cancelled", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }

        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            settingsRepository.setDriveSyncEnabled(true)
            settingsRepository.setDriveAccountEmail(account.email ?: "")
            updateDriveStatus()
            Toast.makeText(this, "Google Drive connected", Toast.LENGTH_SHORT).show()

            when (pendingDriveAction) {
                DriveAction.BACKUP -> backupToDrive()
                DriveAction.RESTORE -> restoreFromDrive()
                null -> Unit
            }
        } catch (e: ApiException) {
            Toast.makeText(this, "Drive sign-in failed: ${e.statusCode}", Toast.LENGTH_SHORT).show()
        } finally {
            pendingDriveAction = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // DAO is injected by Hilt

        setupToolbar()
        setupHealthSettings()
        setupNotificationSettings()
        setupAppearanceSettings()
        setupTrackingSettings()
        setupDataSettings()
        setupAboutSettings()
        loadStorageInfo()
        loadVersion()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    // ==================== HEALTH SETTINGS ====================

    private fun setupHealthSettings() {
        // Daily Limit
        updateDailyLimitDisplay()
        binding.cardDailyLimit.setOnClickListener {
            HapticUtils.performClickFeedback(it)
            showDailyLimitPicker()
        }

        // Break Reminders
        binding.switchBreakReminders.isChecked = settingsRepository.isBreakRemindersEnabled()
        binding.switchBreakReminders.setOnCheckedChangeListener { view, isChecked ->
            HapticUtils.performSelectionFeedback(view)
            settingsRepository.setBreakRemindersEnabled(isChecked)
            binding.cardBreakInterval.alpha = if (isChecked) 1f else 0.5f
            binding.cardBreakInterval.isEnabled = isChecked
        }

        // Break Interval
        updateBreakIntervalDisplay()
        binding.cardBreakInterval.alpha = if (binding.switchBreakReminders.isChecked) 1f else 0.5f
        binding.cardBreakInterval.isEnabled = binding.switchBreakReminders.isChecked
        binding.cardBreakInterval.setOnClickListener {
            HapticUtils.performClickFeedback(it)
            showBreakIntervalPicker()
        }
    }

    private fun updateDailyLimitDisplay() {
        val limitMinutes = settingsRepository.getDailyLimitMinutes()
        binding.tvDailyLimitValue.text = if (limitMinutes == 0) {
            "No limit set"
        } else {
            val hours = limitMinutes / 60
            val mins = limitMinutes % 60
            if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
        }
    }

    private fun showDailyLimitPicker() {
        val options = arrayOf("No limit", "30 minutes", "1 hour", "1.5 hours", "2 hours", "3 hours", "4 hours", "5 hours", "6 hours")
        val values = intArrayOf(0, 30, 60, 90, 120, 180, 240, 300, 360)
        val currentLimit = settingsRepository.getDailyLimitMinutes()
        val currentIndex = values.indexOf(currentLimit).coerceAtLeast(0)

        MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme)
            .setTitle("Daily listening limit")
            .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                settingsRepository.setDailyLimitMinutes(values[which])
                updateDailyLimitDisplay()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateBreakIntervalDisplay() {
        val intervalMinutes = settingsRepository.getBreakIntervalMinutes()
        binding.tvBreakInterval.text = "Every $intervalMinutes minutes"
    }

    private fun showBreakIntervalPicker() {
        val options = arrayOf("Every 30 minutes", "Every 45 minutes", "Every 60 minutes", "Every 90 minutes", "Every 120 minutes")
        val values = intArrayOf(30, 45, 60, 90, 120)
        val currentInterval = settingsRepository.getBreakIntervalMinutes()
        val currentIndex = values.indexOf(currentInterval).coerceAtLeast(2)

        MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme)
            .setTitle("Break interval")
            .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                settingsRepository.setBreakIntervalMinutes(values[which])
                updateBreakIntervalDisplay()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ==================== NOTIFICATION SETTINGS ====================

    private fun setupNotificationSettings() {
        // Daily Summary
        binding.switchDailySummary.isChecked = settingsRepository.isDailySummaryEnabled()
        binding.switchDailySummary.setOnCheckedChangeListener { view, isChecked ->
            HapticUtils.performSelectionFeedback(view)
            settingsRepository.setDailySummaryEnabled(isChecked)
            if (isChecked) {
                scheduleDailySummary()
                Toast.makeText(this, "Daily summary enabled at 9 PM", Toast.LENGTH_SHORT).show()
            }
        }

        // Milestone Alerts
        binding.switchMilestones.isChecked = settingsRepository.isMilestonesEnabled()
        binding.switchMilestones.setOnCheckedChangeListener { view, isChecked ->
            HapticUtils.performSelectionFeedback(view)
            settingsRepository.setMilestonesEnabled(isChecked)
        }
    }

    private fun scheduleDailySummary() {
        // Would schedule using WorkManager in production
    }

    // ==================== APPEARANCE SETTINGS ====================

    private fun setupAppearanceSettings() {
        // Theme
        updateThemeDisplay()
        binding.cardTheme.setOnClickListener {
            HapticUtils.performClickFeedback(it)
            showThemePicker()
        }

        // Accent Color
        binding.cardAccentColor.setOnClickListener {
            HapticUtils.performClickFeedback(it)
            showColorPicker()
        }
    }

    private fun updateThemeDisplay() {
        val theme = settingsRepository.getAppTheme()
        binding.tvThemeValue.text = when (theme) {
            "light" -> "Light"
            "dark" -> "Dark"
            else -> "System default"
        }
    }

    private fun showThemePicker() {
        val options = arrayOf("Light", "Dark", "System default")
        val values = arrayOf("light", "dark", "system")
        val currentTheme = settingsRepository.getAppTheme()
        val currentIndex = values.indexOf(currentTheme).coerceAtLeast(1)

        MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme)
            .setTitle("Choose theme")
            .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                settingsRepository.setAppTheme(values[which])
                updateThemeDisplay()
                applyTheme(values[which])
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun applyTheme(theme: String) {
        when (theme) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    private fun showColorPicker() {
        val colors = arrayOf("Cyan (Default)", "Purple", "Teal", "Orange", "Pink", "Green")
        val colorValues = arrayOf("cyan", "purple", "teal", "orange", "pink", "green")
        val currentColor = settingsRepository.getAccentColor()
        val currentIndex = colorValues.indexOf(currentColor).coerceAtLeast(0)

        MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme)
            .setTitle("Accent color")
            .setSingleChoiceItems(colors, currentIndex) { dialog, which ->
                settingsRepository.setAccentColor(colorValues[which])
                Toast.makeText(this, "Restart app to apply color", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ==================== TRACKING SETTINGS ====================

    private fun setupTrackingSettings() {
        // Auto-start
        binding.switchAutoStart.isChecked = settingsRepository.isAutoStartEnabled()
        binding.switchAutoStart.setOnCheckedChangeListener { view, isChecked ->
            HapticUtils.performSelectionFeedback(view)
            settingsRepository.setAutoStartEnabled(isChecked)
            if (isChecked) {
                Toast.makeText(this, "Tracking will start on device boot", Toast.LENGTH_SHORT).show()
            }
        }

        // Excluded Apps
        updateExcludedAppsCount()
        binding.cardExcludedApps.setOnClickListener {
            HapticUtils.performClickFeedback(it)
            startActivity(Intent(this, ExcludedAppsActivity::class.java))
        }
    }

    private fun updateExcludedAppsCount() {
        val excludedApps = settingsRepository.getExcludedApps()
        binding.tvExcludedCount.text = if (excludedApps.isEmpty()) {
            "No apps excluded"
        } else {
            "${excludedApps.size} app${if (excludedApps.size > 1) "s" else ""} excluded"
        }
    }

    override fun onResume() {
        super.onResume()
        updateExcludedAppsCount()
        updateDriveStatus()
    }

    // ==================== DATA SETTINGS ====================

    private fun setupDataSettings() {
        binding.cardBackup.setOnClickListener {
            HapticUtils.performClickFeedback(it)
            backupData()
        }
        binding.cardRestore.setOnClickListener {
            HapticUtils.performClickFeedback(it)
            restoreFilePicker.launch("application/json")
        }
        binding.cardDriveConnect.setOnClickListener {
            HapticUtils.performClickFeedback(it)
            handleDriveConnectClick()
        }
        binding.cardDriveBackup.setOnClickListener {
            HapticUtils.performClickFeedback(it)
            handleDriveBackupClick()
        }
        binding.cardDriveRestore.setOnClickListener {
            HapticUtils.performClickFeedback(it)
            handleDriveRestoreClick()
        }
        binding.cardDriveSyncNow.setOnClickListener {
            HapticUtils.performClickFeedback(it)
            handleDriveSyncNowClick()
        }
        binding.cardDriveSyncInterval.setOnClickListener {
            HapticUtils.performClickFeedback(it)
            showDriveIntervalPicker()
        }
        binding.cardExportData.setOnClickListener {
            HapticUtils.performClickFeedback(it)
            exportAllData()
        }
        binding.cardClearData.setOnClickListener {
            HapticUtils.performClickFeedback(it)
            showClearDataConfirmation()
        }

        updateDriveStatus()
    }

    private fun backupData() {
        lifecycleScope.launch {
            try {
                val jsonString = buildBackupJsonString() ?: run {
                    Toast.makeText(this@SettingsActivity, "No data to backup", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val fileName = "headphone_tracker_backup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.json"
                val file = File(getExternalFilesDir(null), fileName)
                file.writeText(jsonString)

                val uri = FileProvider.getUriForFile(this@SettingsActivity, "$packageName.fileprovider", file)

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Headphone Tracker Backup")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                startActivity(Intent.createChooser(shareIntent, "Save backup"))
            } catch (e: Exception) {
                Toast.makeText(this@SettingsActivity, "Backup failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun restoreFromUri(uri: Uri) {
        lifecycleScope.launch {
            try {
                val jsonString = contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                    ?: throw Exception("Could not read file")
                val importedCount = restoreFromJson(jsonString, replaceExisting = false)
                Toast.makeText(this@SettingsActivity, "Restored $importedCount records", Toast.LENGTH_SHORT).show()
                loadStorageInfo()
            } catch (e: Exception) {
                Toast.makeText(this@SettingsActivity, "Restore failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleDriveConnectClick() {
        if (isDriveSignedIn()) {
            MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme)
                .setTitle("Disconnect Google Drive?")
                .setMessage("You can reconnect later to sync your backups.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Disconnect") { _, _ ->
                    driveSyncManager.signOut().addOnCompleteListener {
                        settingsRepository.setDriveSyncEnabled(false)
                        settingsRepository.setDriveAccountEmail("")
                        DriveSyncScheduler.cancel(this)
                        updateDriveStatus()
                        Toast.makeText(this, "Google Drive disconnected", Toast.LENGTH_SHORT).show()
                    }
                }
                .show()
        } else {
            startDriveSignIn(null)
        }
    }

    private fun handleDriveBackupClick() {
        if (!isDriveSignedIn()) {
            startDriveSignIn(DriveAction.BACKUP)
            return
        }
        backupToDrive()
    }

    private fun handleDriveRestoreClick() {
        if (!isDriveSignedIn()) {
            startDriveSignIn(DriveAction.RESTORE)
            return
        }
        restoreFromDrive()
    }

    private fun startDriveSignIn(action: DriveAction?) {
        pendingDriveAction = action
        driveSignInLauncher.launch(driveSyncManager.getSignInClient().signInIntent)
    }

    private fun isDriveSignedIn(): Boolean = driveSyncManager.getSignedInAccount() != null

    private fun updateDriveStatus() {
        val account = driveSyncManager.getSignedInAccount()
        val lastSyncTime = settingsRepository.getDriveLastSyncTime()
        val lastError = settingsRepository.getDriveLastError()

        if (account != null) {
            settingsRepository.setDriveSyncEnabled(true)
            settingsRepository.setDriveAccountEmail(account.email ?: "")
            val email = account.email ?: "Google account"
            binding.tvDriveStatus.text = "Connected as $email"
            DriveSyncScheduler.schedule(this, settingsRepository.getDriveSyncIntervalMinutes())
        } else {
            binding.tvDriveStatus.text = "Not connected"
            DriveSyncScheduler.cancel(this)
        }

        binding.tvDriveLastSync.text = if (lastSyncTime > 0) {
            val formatter = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
            "Last sync: ${formatter.format(Date(lastSyncTime))}"
        } else {
            "Last sync: never"
        }

        binding.tvDriveError.text = if (lastError.isNotBlank()) {
            "Last error: $lastError"
        } else {
            "Last error: none"
        }

        val intervalMinutes = settingsRepository.getDriveSyncIntervalMinutes()
        binding.tvDriveInterval.text = "Every $intervalMinutes minutes"
    }

    private fun backupToDrive() {
        lifecycleScope.launch {
            try {
                val jsonString = buildBackupJsonString() ?: run {
                    Toast.makeText(this@SettingsActivity, "No data to backup", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                driveSyncManager.uploadBackup(jsonString)
                settingsRepository.setDriveLastSyncTime(System.currentTimeMillis())
                settingsRepository.setDriveLastError("")
                Toast.makeText(this@SettingsActivity, "Backup saved to Google Drive", Toast.LENGTH_SHORT).show()
                updateDriveStatus()
            } catch (e: Exception) {
                settingsRepository.setDriveLastError(e.message ?: "Unknown error")
                Toast.makeText(this@SettingsActivity, "Drive backup failed: ${e.message}", Toast.LENGTH_SHORT).show()
                updateDriveStatus()
            }
        }
    }

    private fun restoreFromDrive() {
        MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme)
            .setTitle("Restore from Google Drive?")
            .setMessage("This will replace your local data with the Drive backup.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Restore") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val jsonString = driveSyncManager.downloadBackup()
                            ?: run {
                                Toast.makeText(this@SettingsActivity, "No Drive backup found", Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                        val importedCount = restoreFromJson(jsonString, replaceExisting = true)
                        Toast.makeText(this@SettingsActivity, "Restored $importedCount records", Toast.LENGTH_SHORT).show()
                        settingsRepository.setDriveLastSyncTime(System.currentTimeMillis())
                        settingsRepository.setDriveLastError("")
                        updateDriveStatus()
                        loadStorageInfo()
                    } catch (e: Exception) {
                        settingsRepository.setDriveLastError(e.message ?: "Unknown error")
                        Toast.makeText(this@SettingsActivity, "Drive restore failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        updateDriveStatus()
                    }
                }
            }
            .show()
    }

    private fun handleDriveSyncNowClick() {
        if (!isDriveSignedIn()) {
            startDriveSignIn(DriveAction.BACKUP)
            return
        }
        DriveSyncScheduler.runNow(this)
        Toast.makeText(this, "Sync queued", Toast.LENGTH_SHORT).show()
    }

    private fun showDriveIntervalPicker() {
        val options = arrayOf("15 minutes", "30 minutes", "1 hour", "3 hours", "6 hours")
        val values = intArrayOf(15, 30, 60, 180, 360)
        val current = settingsRepository.getDriveSyncIntervalMinutes()
        val currentIndex = values.indexOf(current).coerceAtLeast(0)

        MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme)
            .setTitle("Sync interval")
            .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                val selected = values[which]
                settingsRepository.setDriveSyncIntervalMinutes(selected)
                if (settingsRepository.isDriveSyncEnabled()) {
                    DriveSyncScheduler.schedule(this, selected)
                }
                updateDriveStatus()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private suspend fun buildBackupJsonString(): String? {
        val allData = withContext(Dispatchers.IO) {
            headphoneUsageDao.getAllUsage()
        }

        if (allData.isEmpty()) {
            return null
        }

        return BackupJsonUtils.createBackupJson(allData).toString(2)
    }

    private suspend fun restoreFromJson(jsonString: String, replaceExisting: Boolean): Int {
        val usageList = BackupJsonUtils.parseBackupJson(jsonString)
        var importedCount = 0

        withContext(Dispatchers.IO) {
            if (replaceExisting) {
                headphoneUsageDao.deleteAllUsage()
            }

            usageList.forEach { usage ->
                headphoneUsageDao.insertUsage(usage)
                importedCount++
            }
        }

        return importedCount
    }

    private enum class DriveAction {
        BACKUP,
        RESTORE
    }

    private fun exportAllData() {
        lifecycleScope.launch {
            try {
                val allData = withContext(Dispatchers.IO) {
                    headphoneUsageDao.getAllUsage()
                }

                if (allData.isEmpty()) {
                    Toast.makeText(this@SettingsActivity, "No data to export", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val csv = buildString {
                    appendLine("Date,App Name,Package Name,Duration (seconds),Duration (formatted)")
                    allData.forEach { usage ->
                        val formattedDuration = formatDuration(usage.duration)
                        appendLine("${usage.date},\"${usage.appName}\",${usage.packageName},${usage.duration},\"$formattedDuration\"")
                    }
                }

                val fileName = "headphone_tracker_export_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv"
                val file = File(getExternalFilesDir(null), fileName)
                file.writeText(csv)

                val uri = FileProvider.getUriForFile(this@SettingsActivity, "$packageName.fileprovider", file)

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Headphone Usage Export")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                startActivity(Intent.createChooser(shareIntent, "Export data"))
            } catch (e: Exception) {
                Toast.makeText(this@SettingsActivity, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showClearDataConfirmation() {
        MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme)
            .setTitle("Clear all data?")
            .setMessage("This will permanently delete all your tracking history. This action cannot be undone.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ -> clearAllData() }
            .show()
    }

    private fun clearAllData() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                headphoneUsageDao.deleteAllUsage()
            }
            Toast.makeText(this@SettingsActivity, "All data cleared", Toast.LENGTH_SHORT).show()
            loadStorageInfo()
        }
    }

    private fun loadStorageInfo() {
        lifecycleScope.launch {
            val allData = withContext(Dispatchers.IO) {
                headphoneUsageDao.getAllUsage()
            }
            val recordCount = allData.size
            val totalDuration = allData.sumOf { it.duration }

            val formattedDuration = formatDuration(totalDuration)
            binding.tvStorageInfo.text = "$recordCount records • $formattedDuration total tracked"
        }
    }

    private fun setupAboutSettings() {
        binding.cardViewOnboarding.setOnClickListener {
            HapticUtils.performClickFeedback(it)
            startActivity(Intent(this, OnboardingActivity::class.java))
        }
    }

    private fun loadVersion() {
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            binding.tvVersion.text = "Version ${packageInfo.versionName}"
        } catch (e: Exception) {
            binding.tvVersion.text = "Version 1.0"
        }
    }

    private fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m ${secs}s"
            minutes > 0 -> "${minutes}m ${secs}s"
            else -> "${secs}s"
        }
    }
}

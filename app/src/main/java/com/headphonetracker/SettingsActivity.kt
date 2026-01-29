package com.headphonetracker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.NumberPicker
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

import com.headphonetracker.data.HeadphoneUsageDao
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.headphonetracker.data.HeadphoneUsage
import com.headphonetracker.databinding.ActivitySettingsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    @Inject
    lateinit var headphoneUsageDao: HeadphoneUsageDao
    @Inject
    lateinit var settingsRepository: com.headphonetracker.data.SettingsRepository

    private val restoreFilePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { restoreFromUri(it) }
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
        binding.cardExportData.setOnClickListener {
            HapticUtils.performClickFeedback(it)
            exportAllData()
        }
        binding.cardClearData.setOnClickListener {
            HapticUtils.performClickFeedback(it)
            showClearDataConfirmation()
        }
    }

    private fun backupData() {
        lifecycleScope.launch {
            try {
                val allData = withContext(Dispatchers.IO) {
                    headphoneUsageDao.getAllUsage()
                }

                if (allData.isEmpty()) {
                    Toast.makeText(this@SettingsActivity, "No data to backup", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val json = JSONObject().apply {
                    put("version", 1)
                    put("exported_at", System.currentTimeMillis())
                    put("data", JSONArray().apply {
                        allData.forEach { usage ->
                            put(JSONObject().apply {
                                put("id", usage.id)
                                put("date", usage.date)
                                put("packageName", usage.packageName)
                                put("appName", usage.appName)
                                put("duration", usage.duration)
                                put("startTime", usage.startTime)
                                put("endTime", usage.endTime)
                            })
                        }
                    })
                }

                val fileName = "headphone_tracker_backup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.json"
                val file = File(getExternalFilesDir(null), fileName)
                file.writeText(json.toString(2))

                val uri = FileProvider.getUriForFile(this@SettingsActivity, "${packageName}.fileprovider", file)

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

                val json = JSONObject(jsonString)
                val dataArray = json.getJSONArray("data")
                
                var importedCount = 0
                withContext(Dispatchers.IO) {
                    for (i in 0 until dataArray.length()) {
                        val item = dataArray.getJSONObject(i)
                        val usage = HeadphoneUsage(
                            id = 0, // Auto-generate new ID
                            date = item.getString("date"),
                            packageName = item.getString("packageName"),
                            appName = item.getString("appName"),
                            duration = item.getLong("duration"),
                            startTime = item.getLong("startTime"),
                            endTime = item.getLong("endTime")
                        )
                        headphoneUsageDao.insertUsage(usage)
                        importedCount++
                    }
                }

                Toast.makeText(this@SettingsActivity, "Restored $importedCount records", Toast.LENGTH_SHORT).show()
                loadStorageInfo()

            } catch (e: Exception) {
                Toast.makeText(this@SettingsActivity, "Restore failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
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

                val uri = FileProvider.getUriForFile(this@SettingsActivity, "${packageName}.fileprovider", file)

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

package com.headphonetracker

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.headphonetracker.data.BackupJsonUtils
import com.headphonetracker.data.HeadphoneUsageDao
import com.headphonetracker.data.SettingsRepository
import com.headphonetracker.databinding.ActivityMainBinding
import com.headphonetracker.sync.DriveSyncManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    @Inject
    lateinit var headphoneUsageDao: HeadphoneUsageDao

    @Inject
    lateinit var settingsRepository: SettingsRepository
    private val driveSyncManager by lazy { DriveSyncManager(this) }

    private val todayFragment by lazy { TodayFragment() }
    private val statsFragment by lazy { StatsFragment() }
    private val settingsFragment by lazy { SettingsFragment() }
    private var activeFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if onboarding is completed
        val onboardingCompleted = settingsRepository.isOnboardingCompleted()

        if (!onboardingCompleted) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            setupFragments()
        } else {
            // Restore fragments after config change
            restoreFragments()
        }

        setupBottomNavigation()
        checkPermissions()
        maybePromptDriveRestore()
        pruneOldData()
    }

    /** Delete usage data older than 3 months to keep the database lean. */
    private fun pruneOldData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val cutoff = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(java.util.Calendar.getInstance().apply {
                    add(java.util.Calendar.MONTH, -3)
                }.time)
            headphoneUsageDao.deleteOldData(cutoff)
        }
    }

    private fun setupFragments() {
        supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, settingsFragment, "settings").hide(settingsFragment)
            .add(R.id.fragmentContainer, statsFragment, "stats").hide(statsFragment)
            .add(R.id.fragmentContainer, todayFragment, "today")
            .commit()
        activeFragment = todayFragment
    }

    private fun restoreFragments() {
        val today = supportFragmentManager.findFragmentByTag("today") as? TodayFragment
        val stats = supportFragmentManager.findFragmentByTag("stats") as? StatsFragment
        val settings = supportFragmentManager.findFragmentByTag("settings") as? SettingsFragment

        // Determine which fragment is currently visible
        activeFragment = when {
            today?.isVisible == true -> today
            stats?.isVisible == true -> stats
            settings?.isVisible == true -> settings
            else -> {
                // Default to today
                today?.let {
                    supportFragmentManager.beginTransaction().show(it).commit()
                    it
                }
            }
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            HapticUtils.performSelectionFeedback(binding.bottomNavigation)
            when (item.itemId) {
                R.id.nav_today -> {
                    switchFragment(getOrCreateFragment("today"))
                    val frag = getOrCreateFragment("today")
                    if (frag is TodayFragment) {
                        frag.resetToToday()
                    }
                    true
                }
                R.id.nav_stats -> {
                    switchFragment(getOrCreateFragment("stats"))
                    true
                }
                R.id.nav_settings -> {
                    switchFragment(getOrCreateFragment("settings"))
                    true
                }
                else -> false
            }
        }
    }

    private fun getOrCreateFragment(tag: String): Fragment {
        return supportFragmentManager.findFragmentByTag(tag) ?: when (tag) {
            "today" -> todayFragment
            "stats" -> statsFragment
            "settings" -> settingsFragment
            else -> todayFragment
        }
    }

    private fun switchFragment(target: Fragment) {
        if (target == activeFragment) return

        val transaction = supportFragmentManager.beginTransaction()
        activeFragment?.let { transaction.hide(it) }
        transaction.show(target)
        transaction.commit()
        activeFragment = target
    }

    private fun maybePromptDriveRestore() {
        if (settingsRepository.isDriveRestorePrompted()) {
            return
        }

        driveSyncManager.getSignedInAccount() ?: return

        lifecycleScope.launch {
            val hasLocalData = withContext(Dispatchers.IO) {
                headphoneUsageDao.getAllUsage().isNotEmpty()
            }

            if (hasLocalData) {
                settingsRepository.setDriveRestorePrompted(true)
                return@launch
            }

            val hasBackup = try {
                driveSyncManager.hasBackup()
            } catch (e: Exception) {
                return@launch
            }

            if (!hasBackup) {
                return@launch
            }

            settingsRepository.setDriveRestorePrompted(true)

            restoreDriveBackup()
        }
    }

    private fun restoreDriveBackup() {
        lifecycleScope.launch {
            try {
                val jsonString = driveSyncManager.downloadBackup()
                    ?: run {
                        Toast.makeText(this@MainActivity, "No Drive backup found", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                val usageList = BackupJsonUtils.parseBackupJson(jsonString)

                withContext(Dispatchers.IO) {
                    headphoneUsageDao.deleteAllUsage()
                    usageList.forEach { usage ->
                        headphoneUsageDao.insertUsage(usage)
                    }
                }

                Toast.makeText(this@MainActivity, "Restored ${usageList.size} records", Toast.LENGTH_SHORT).show()
                // Refresh today fragment
                val frag = supportFragmentManager.findFragmentByTag("today") as? TodayFragment
                frag?.refreshData()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Drive restore failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!hasUsageStatsPermission()) {
            startActivity(Intent(this, UsageStatsPermissionActivity::class.java))
        } else {
            // Refresh current today fragment data
            val frag = supportFragmentManager.findFragmentByTag("today") as? TodayFragment
            frag?.refreshData()
        }
    }

    private fun checkPermissions() {
        if (!hasUsageStatsPermission()) {
            startActivity(Intent(this, UsageStatsPermissionActivity::class.java))
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
}

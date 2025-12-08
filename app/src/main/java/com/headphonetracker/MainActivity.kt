package com.headphonetracker

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.headphonetracker.data.AppDatabase
import com.headphonetracker.data.AppUsageSummary
import com.headphonetracker.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var database: AppDatabase
    private lateinit var adapter: AppUsageAdapter
    private var isTracking = false
    private var refreshJob: Job? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        database = AppDatabase.getDatabase(this)
        
        setupRecyclerView()
        setupCharts()
        setupButton()
        checkPermissions()
        
        // Observe tracking state
        lifecycleScope.launch {
            HeadphoneTrackingService.isTrackingFlow.collectLatest { tracking ->
                isTracking = tracking
                updateButton()
                if (tracking) {
                    startAutoRefresh()
                } else {
                    stopAutoRefresh()
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        if (hasUsageStatsPermission()) {
            loadData()
        } else {
            // Request permission
            startActivity(Intent(this, UsageStatsPermissionActivity::class.java))
        }
        
        // Update tracking state
        isTracking = HeadphoneTrackingService.isTrackingFlow.value
        updateButton()
    }
    
    private fun setupRecyclerView() {
        adapter = AppUsageAdapter(emptyList(), 0, packageManager)
        binding.rvAppList.layoutManager = LinearLayoutManager(this)
        binding.rvAppList.adapter = adapter
    }
    
    private fun setupCharts() {
        setupPieChart()
        setupBarChart()
    }
    
    private fun setupPieChart() {
        binding.pieChart.description.isEnabled = false
        binding.pieChart.setUsePercentValues(true)
        binding.pieChart.setDrawEntryLabels(false)
        binding.pieChart.legend.isEnabled = true
        binding.pieChart.setExtraOffsets(5f, 10f, 5f, 5f)
        binding.pieChart.setEntryLabelTextSize(12f)
        binding.pieChart.setEntryLabelColor(android.graphics.Color.BLACK)
        binding.pieChart.setCenterTextSize(16f)
        binding.pieChart.animateY(1000)
    }
    
    private fun setupBarChart() {
        binding.barChart.description.isEnabled = false
        binding.barChart.setDrawGridBackground(false)
        binding.barChart.setScaleEnabled(false)
        binding.barChart.setPinchZoom(false)
        binding.barChart.legend.isEnabled = true
        
        val xAxis = binding.barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.labelRotationAngle = -45f
        
        val leftAxis = binding.barChart.axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.axisMinimum = 0f
        
        binding.barChart.axisRight.isEnabled = false
        binding.barChart.animateY(1000)
    }
    
    private fun setupButton() {
        binding.btnToggleTracking.setOnClickListener {
            toggleTracking()
        }
    }
    
    private fun toggleTracking() {
        val intent = Intent(this, HeadphoneTrackingService::class.java)
        
        if (isTracking) {
            intent.action = "STOP_TRACKING"
            stopService(intent)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent.apply { action = "START_TRACKING" })
            } else {
                startService(intent.apply { action = "START_TRACKING" })
            }
        }
        
        requestNotificationPermission()
    }
    
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                }
                startActivity(intent)
            }
        }
    }
    
    private fun updateButton() {
        if (isTracking) {
            binding.btnToggleTracking.text = getString(R.string.stop_tracking)
            binding.btnToggleTracking.setIconResource(android.R.drawable.ic_media_pause)
        } else {
            binding.btnToggleTracking.text = getString(R.string.start_tracking)
            binding.btnToggleTracking.setIconResource(android.R.drawable.ic_media_play)
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
    
    private fun loadData() {
        lifecycleScope.launch {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            
            // Load today's total
            val totalDuration = withContext(Dispatchers.IO) {
                database.headphoneUsageDao().getTotalUsageForDate(today) ?: 0L
            }
            binding.tvTotalTime.text = formatDuration(totalDuration)
            
            // Load app usage
            val appUsage = withContext(Dispatchers.IO) {
                database.headphoneUsageDao().getUsageByAppForDate(today)
            }
            
            if (appUsage.isNotEmpty()) {
                updatePieChart(appUsage, totalDuration)
                adapter.updateData(appUsage, totalDuration)
            } else {
                binding.pieChart.data = null
                binding.pieChart.invalidate()
                adapter.updateData(emptyList(), 0)
            }
            
            // Load last 7 days
            val last7Days = withContext(Dispatchers.IO) {
                database.headphoneUsageDao().getLast7DaysUsage()
            }
            updateBarChart(last7Days)
        }
    }
    
    private fun updatePieChart(usageList: List<AppUsageSummary>, totalDuration: Long) {
        if (totalDuration == 0L) {
            binding.pieChart.data = null
            binding.pieChart.invalidate()
            return
        }
        
        val entries = mutableListOf<PieEntry>()
        val colors = mutableListOf<Int>()
        
        val colorPalette = listOf(
            android.graphics.Color.parseColor("#FF6384"),
            android.graphics.Color.parseColor("#36A2EB"),
            android.graphics.Color.parseColor("#FFCE56"),
            android.graphics.Color.parseColor("#4BC0C0"),
            android.graphics.Color.parseColor("#9966FF"),
            android.graphics.Color.parseColor("#FF9F40"),
            android.graphics.Color.parseColor("#FF6384"),
            android.graphics.Color.parseColor("#C9CBCF")
        )
        
        usageList.sortedByDescending { it.totalDuration }.take(8).forEachIndexed { index, usage ->
            val hours = usage.totalDuration / (1000 * 60 * 60.0)
            entries.add(PieEntry(hours.toFloat(), usage.appName))
            colors.add(colorPalette[index % colorPalette.size])
        }
        
        val dataSet = PieDataSet(entries, "").apply {
            setColors(colors)
            valueTextSize = 12f
            valueTextColor = android.graphics.Color.WHITE
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return String.format("%.1fh", value)
                }
            }
        }
        
        val data = PieData(dataSet)
        binding.pieChart.data = data
        binding.pieChart.centerText = "Today's Usage"
        binding.pieChart.invalidate()
    }
    
    private fun updateBarChart(dailyUsage: List<com.headphonetracker.data.DailyUsageSummary>) {
        if (dailyUsage.isEmpty()) {
            binding.barChart.data = null
            binding.barChart.invalidate()
            return
        }
        
        val entries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()
        
        // Reverse to show oldest to newest (left to right)
        dailyUsage.reversed().forEachIndexed { index, daily ->
            val hours = daily.totalDuration / (1000 * 60 * 60.0)
            entries.add(BarEntry(index.toFloat(), hours.toFloat()))
            
            // Format date
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = dateFormat.parse(daily.date)
                val displayFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
                labels.add(displayFormat.format(date ?: Date()))
            } catch (e: Exception) {
                labels.add(daily.date)
            }
        }
        
        val dataSet = BarDataSet(entries, "Daily Usage (hours)").apply {
            color = android.graphics.Color.parseColor("#36A2EB")
            valueTextSize = 10f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return String.format("%.1fh", value)
                }
            }
        }
        
        val data = BarData(dataSet)
        binding.barChart.data = data
        
        binding.barChart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index >= 0 && index < labels.size) {
                    labels[index]
                } else {
                    ""
                }
            }
        }
        
        binding.barChart.invalidate()
    }
    
    private fun formatDuration(millis: Long): String {
        val seconds = millis / 1000
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        
        return when {
            hours > 0 -> "${hours}h ${minutes}m ${secs}s"
            minutes > 0 -> "${minutes}m ${secs}s"
            else -> "${secs}s"
        }
    }
    
    private fun startAutoRefresh() {
        stopAutoRefresh()
        refreshJob = lifecycleScope.launch {
            while (isTracking) {
                kotlinx.coroutines.delay(5000) // Refresh every 5 seconds
                if (isTracking && hasUsageStatsPermission()) {
                    loadData()
                }
            }
        }
    }
    
    private fun stopAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = null
    }
    
    override fun onPause() {
        super.onPause()
        stopAutoRefresh()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopAutoRefresh()
    }
}


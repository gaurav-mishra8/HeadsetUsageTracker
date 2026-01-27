package com.headphonetracker

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.AppOpsManager
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

import com.headphonetracker.data.HeadphoneUsageDao
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.headphonetracker.data.AppUsageSummary
import com.headphonetracker.data.DailyUsageSummary
import com.headphonetracker.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    @Inject
    lateinit var headphoneUsageDao: HeadphoneUsageDao
    private lateinit var adapter: AppUsageAdapter
    private var isTracking = false
    private var refreshJob: Job? = null
    private var cachedWeeklyData: List<DailyUsageSummary> = emptyList()
    
    // Date navigation
    private var selectedDate: Calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
    private val minDate: Calendar = Calendar.getInstance().apply {
        add(Calendar.MONTH, -3) // 3 months ago
    }
    
    // Modern color palette
    private val chartColors = listOf(
        Color.parseColor("#6366F1"), // Primary purple
        Color.parseColor("#EC4899"), // Pink
        Color.parseColor("#06B6D4"), // Cyan
        Color.parseColor("#10B981"), // Green
        Color.parseColor("#F59E0B"), // Amber
        Color.parseColor("#8B5CF6"), // Violet
        Color.parseColor("#14B8A6"), // Teal
        Color.parseColor("#F472B6")  // Light pink
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if onboarding is completed
        val prefs = getSharedPreferences("headphone_tracker_prefs", Context.MODE_PRIVATE)
        val onboardingCompleted = prefs.getBoolean("onboarding_completed", false)
        
        if (!onboardingCompleted) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }
        
        // Edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
    // DAO is injected by Hilt
        
        setupRecyclerView()
        setupCharts()
        setupButton()
        setupExportButton()
        setupDateNavigation()
        setupBottomNavigation()
        setupPullToRefresh()
        checkPermissions()
        
        // Initial animations
        animateCardsOnLoad()
        
        // Observe tracking state
        lifecycleScope.launch {
            HeadphoneTrackingService.isTrackingFlow.collectLatest { tracking ->
                isTracking = tracking
                updateButton()
                updateStatusIndicator(tracking)
                if (tracking) {
                    startAutoRefresh()
                } else {
                    stopAutoRefresh()
                }
            }
        }
    }
    
    private fun animateCardsOnLoad() {
        val cards = listOf(
            binding.cardTodayUsage,
            binding.cardPieChart,
            binding.cardBarChart
        )
        
        cards.forEachIndexed { index, card ->
            card.alpha = 0f
            card.translationY = 50f
            card.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay((index * 100).toLong())
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }
    
    private fun setupDateNavigation() {
        // Update date display
        updateDateDisplay()
        
        // Previous day button
        binding.btnPrevDay.setOnClickListener {
            HapticUtils.performClickFeedback(it)
            val prevDay = selectedDate.clone() as Calendar
            prevDay.add(Calendar.DAY_OF_YEAR, -1)
            
            if (prevDay.timeInMillis >= minDate.timeInMillis) {
                selectedDate = prevDay
                updateDateDisplay()
                loadDataForSelectedDate()
            } else {
                HapticUtils.performErrorFeedback(this)
                Toast.makeText(this, "Can't go back more than 3 months", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Next day button
        binding.btnNextDay.setOnClickListener {
            HapticUtils.performClickFeedback(it)
            val nextDay = selectedDate.clone() as Calendar
            nextDay.add(Calendar.DAY_OF_YEAR, 1)
            val today = Calendar.getInstance()
            
            if (nextDay.timeInMillis <= today.timeInMillis) {
                selectedDate = nextDay
                updateDateDisplay()
                loadDataForSelectedDate()
            } else {
                HapticUtils.performErrorFeedback(this)
                Toast.makeText(this, "Can't view future dates", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Date picker on tap
        binding.dateSelector.setOnClickListener {
            HapticUtils.performClickFeedback(it)
            showDatePicker()
        }
    }
    
    private fun updateDateDisplay() {
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        
        val isToday = isSameDay(selectedDate, today)
        val isYesterday = isSameDay(selectedDate, yesterday)
        
        binding.tvSelectedDateLabel.text = when {
            isToday -> "TODAY"
            isYesterday -> "YESTERDAY"
            else -> selectedDate.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault())?.uppercase() ?: ""
        }
        
        binding.tvSelectedDate.text = displayDateFormat.format(selectedDate.time)
        
        // Update next button visibility
        binding.btnNextDay.alpha = if (isToday) 0.3f else 1f
        binding.btnNextDay.isEnabled = !isToday
        
        // Update label color
        binding.tvSelectedDateLabel.setTextColor(
            if (isToday) ContextCompat.getColor(this, R.color.success)
            else ContextCompat.getColor(this, R.color.primary_light)
        )
    }
    
    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
    
    private fun showDatePicker() {
        val constraints = com.google.android.material.datepicker.CalendarConstraints.Builder()
            .setStart(minDate.timeInMillis)
            .setEnd(System.currentTimeMillis())
            .setOpenAt(selectedDate.timeInMillis)
            .setValidator(com.google.android.material.datepicker.DateValidatorPointBackward.now())
            .build()
        
        val datePicker = com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select date")
            .setSelection(selectedDate.timeInMillis)
            .setCalendarConstraints(constraints)
            .setTheme(R.style.MaterialDatePickerTheme)
            .build()
        
        datePicker.addOnPositiveButtonClickListener { selection ->
            selectedDate.timeInMillis = selection
            updateDateDisplay()
            loadDataForSelectedDate()
        }
        
        datePicker.show(supportFragmentManager, "DATE_PICKER")
    }
    
    private fun loadDataForSelectedDate() {
        lifecycleScope.launch {
            // Show loading skeleton
            binding.loadingSkeleton.visibility = View.VISIBLE
            binding.rvAppList.visibility = View.GONE
            binding.emptyState.visibility = View.GONE
            
            val dateString = dateFormat.format(selectedDate.time)
            
            // Load total for selected date
            val totalDuration = withContext(Dispatchers.IO) {
                headphoneUsageDao.getTotalUsageForDate(dateString) ?: 0L
            }
            
            animateTotalTime(totalDuration)
            
            // Load app usage for selected date
            val appUsage = withContext(Dispatchers.IO) {
                headphoneUsageDao.getUsageByAppForDate(dateString)
            }
            
            // Hide loading skeleton
            binding.loadingSkeleton.visibility = View.GONE
            
            if (appUsage.isNotEmpty()) {
                binding.emptyState.visibility = View.GONE
                binding.rvAppList.visibility = View.VISIBLE
                updatePieChart(appUsage, totalDuration)
                adapter.updateData(appUsage, totalDuration)
            } else {
                binding.emptyState.visibility = View.VISIBLE
                binding.rvAppList.visibility = View.GONE
                binding.pieChart.data = null
                binding.pieChart.invalidate()
                binding.legendScrollView.visibility = View.GONE
                adapter.updateData(emptyList(), 0)
            }
            
            // Stop refresh indicator
            binding.swipeRefresh.isRefreshing = false
        }
    }
    
    private fun setupPullToRefresh() {
        binding.swipeRefresh.setColorSchemeColors(
            ContextCompat.getColor(this, R.color.primary),
            ContextCompat.getColor(this, R.color.primary_light),
            ContextCompat.getColor(this, R.color.secondary)
        )
        
        binding.swipeRefresh.setOnRefreshListener {
            HapticUtils.performSwipeFeedback(binding.swipeRefresh)
            loadDataForSelectedDate()
        }
    }
    
    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            HapticUtils.performSelectionFeedback(binding.bottomNavigation)
            when (item.itemId) {
                R.id.nav_today -> {
                    // Reset to today
                    selectedDate = Calendar.getInstance()
                    updateDateDisplay()
                    loadDataForSelectedDate()
                    binding.scrollView.smoothScrollTo(0, 0)
                    true
                }
                R.id.nav_stats -> {
                    startActivity(Intent(this, StatsActivity::class.java))
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
    
    private fun updateStatusIndicator(tracking: Boolean) {
        if (tracking) {
            binding.statusDot.setBackgroundResource(R.drawable.status_dot_active)
            binding.tvStatus.text = "Tracking"
            binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.success))
            
            // Pulse animation for status dot
            val pulseAnimator = ObjectAnimator.ofFloat(binding.statusDot, "alpha", 1f, 0.4f, 1f)
            pulseAnimator.duration = 1500
            pulseAnimator.repeatCount = ValueAnimator.INFINITE
            pulseAnimator.start()
        } else {
            binding.statusDot.setBackgroundResource(R.drawable.status_dot_inactive)
            binding.tvStatus.text = "Inactive"
            binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            binding.statusDot.alpha = 1f
        }
    }
    
    override fun onResume() {
        super.onResume()
        if (hasUsageStatsPermission()) {
            loadData()
        } else {
            startActivity(Intent(this, UsageStatsPermissionActivity::class.java))
        }
        
        isTracking = HeadphoneTrackingService.isTrackingFlow.value
        updateButton()
        updateStatusIndicator(isTracking)
    }
    
    private fun setupRecyclerView() {
        adapter = AppUsageAdapter(emptyList(), 0, packageManager)
        binding.rvAppList.layoutManager = LinearLayoutManager(this)
        binding.rvAppList.adapter = adapter
        
        // Item animation
        binding.rvAppList.itemAnimator?.apply {
            addDuration = 300
            removeDuration = 300
            moveDuration = 300
            changeDuration = 300
        }
    }
    
    private fun setupCharts() {
        setupPieChart()
        setupBarChart()
    }
    
    private fun setupPieChart() {
        binding.pieChart.apply {
            description.isEnabled = false
            setUsePercentValues(true)
            setDrawEntryLabels(false)
            isDrawHoleEnabled = true
            holeRadius = 55f
            transparentCircleRadius = 60f
            setHoleColor(Color.parseColor("#151B23"))
            setTransparentCircleColor(Color.parseColor("#1A2028"))
            
            // Center text styling
            centerText = "Usage"
            setCenterTextSize(14f)
            setCenterTextColor(Color.parseColor("#94A3B8"))
            
            // Disable built-in legend - we'll use custom scrollable one
            legend.isEnabled = false
            
            setExtraOffsets(10f, 10f, 10f, 10f)
            setNoDataText("No usage data")
            setNoDataTextColor(Color.parseColor("#64748B"))
        }
    }
    
    private fun updateCustomLegend(usageList: List<AppUsageSummary>, totalDuration: Long) {
        binding.legendContainer.removeAllViews()
        
        if (usageList.isEmpty() || totalDuration == 0L) {
            binding.legendScrollView.visibility = View.GONE
            return
        }
        
        binding.legendScrollView.visibility = View.VISIBLE
        
        usageList.sortedByDescending { it.totalDuration }.take(8).forEachIndexed { index, usage ->
            val legendItem = layoutInflater.inflate(R.layout.item_legend, binding.legendContainer, false)
            
            // Set color dot
            val colorDot = legendItem.findViewById<View>(R.id.legendColorDot)
            val drawable = colorDot.background.mutate()
            drawable.setTint(chartColors[index % chartColors.size])
            colorDot.background = drawable
            
            // Set app name (2 lines max)
            val appNameText = legendItem.findViewById<android.widget.TextView>(R.id.legendAppName)
            appNameText.text = usage.appName
            
            // Set percentage
            val percentageText = legendItem.findViewById<android.widget.TextView>(R.id.legendPercentage)
            val percentage = (usage.totalDuration * 100f / totalDuration)
            percentageText.text = String.format("%.0f%%", percentage)
            
            binding.legendContainer.addView(legendItem)
        }
    }
    
    private fun setupBarChart() {
        binding.barChart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawValueAboveBar(true)
            
            // X-axis styling
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                setDrawAxisLine(false)
                granularity = 1f
                textColor = Color.parseColor("#94A3B8")
                textSize = 11f
                labelRotationAngle = 0f
            }
            
            // Y-axis styling
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#1E293B")
                setDrawAxisLine(false)
                textColor = Color.parseColor("#64748B")
                textSize = 10f
                axisMinimum = 0f
            }
            
            axisRight.isEnabled = false
            
            // Legend
            legend.apply {
                isEnabled = false
            }
            
            setNoDataText("No weekly data")
            setNoDataTextColor(Color.parseColor("#64748B"))
        }
    }
    
    private fun setupButton() {
        binding.btnToggleTracking.setOnClickListener {
            HapticUtils.performClickFeedback(it)
            // Button press animation
            it.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    it.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .setInterpolator(OvershootInterpolator())
                        .start()
                }
                .start()
            
            toggleTracking()
        }
    }
    
    private fun setupExportButton() {
        binding.btnExport.setOnClickListener {
            HapticUtils.performClickFeedback(it)
            // Button press animation
            it.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    it.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .setInterpolator(OvershootInterpolator())
                        .start()
                }
                .start()
            
            exportWeeklyData()
        }
    }
    
    private fun exportWeeklyData() {
        lifecycleScope.launch {
            try {
                // Calculate date range for last 7 days
                val calendar = Calendar.getInstance()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val endDate = dateFormat.format(calendar.time)
                calendar.add(Calendar.DAY_OF_YEAR, -6)
                val startDate = dateFormat.format(calendar.time)
                
                // Get detailed usage data
                val detailedUsage = withContext(Dispatchers.IO) {
                    headphoneUsageDao.getUsageForDateRange(startDate, endDate)
                }
                
                if (cachedWeeklyData.isEmpty() && detailedUsage.isEmpty()) {
                    Toast.makeText(this@MainActivity, "No data to export", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                // Export and share
                ExportUtils.exportAndShareWeeklyData(
                    this@MainActivity,
                    cachedWeeklyData,
                    detailedUsage
                )
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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
            binding.btnToggleTracking.setBackgroundColor(ContextCompat.getColor(this, R.color.error))
        } else {
            binding.btnToggleTracking.text = getString(R.string.start_tracking)
            binding.btnToggleTracking.setIconResource(android.R.drawable.ic_media_play)
            binding.btnToggleTracking.setBackgroundColor(ContextCompat.getColor(this, R.color.primary))
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
        // Load data for selected date
        loadDataForSelectedDate()
        
        // Also load weekly bar chart data
        lifecycleScope.launch {
            val last7Days = withContext(Dispatchers.IO) {
                headphoneUsageDao.getLast7DaysUsage()
            }
            cachedWeeklyData = last7Days
            updateBarChart(last7Days)
        }
    }
    
    private fun animateTotalTime(newDuration: Long) {
        binding.tvTotalTime.text = formatDuration(newDuration)
    }
    
    private fun updatePieChart(usageList: List<AppUsageSummary>, totalDuration: Long) {
        if (totalDuration == 0L) {
            binding.pieChart.data = null
            binding.pieChart.invalidate()
            binding.legendScrollView.visibility = View.GONE
            return
        }
        
        val entries = mutableListOf<PieEntry>()
        val colors = mutableListOf<Int>()
        
        val sortedList = usageList.sortedByDescending { it.totalDuration }.take(8)
        
        sortedList.forEachIndexed { index, usage ->
            val percentage = (usage.totalDuration * 100f / totalDuration)
            entries.add(PieEntry(percentage, "")) // Empty label - using custom legend
            colors.add(chartColors[index % chartColors.size])
        }
        
        val dataSet = PieDataSet(entries, "").apply {
            setColors(colors)
            valueTextSize = 0f // Hide values on slices
            sliceSpace = 3f
            selectionShift = 8f
        }
        
        val data = PieData(dataSet)
        binding.pieChart.data = data
        binding.pieChart.animateY(800, Easing.EaseInOutCubic)
        binding.pieChart.invalidate()
        
        // Update custom scrollable legend
        updateCustomLegend(usageList, totalDuration)
    }
    
    private fun updateBarChart(dailyUsage: List<com.headphonetracker.data.DailyUsageSummary>) {
        if (dailyUsage.isEmpty()) {
            binding.barChart.data = null
            binding.barChart.invalidate()
            return
        }
        
        val entries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()
        
        dailyUsage.reversed().forEachIndexed { index, daily ->
            val minutes = daily.totalDuration / (1000 * 60.0)
            entries.add(BarEntry(index.toFloat(), minutes.toFloat()))
            
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = dateFormat.parse(daily.date)
                val displayFormat = SimpleDateFormat("EEE", Locale.getDefault())
                labels.add(displayFormat.format(date ?: Date()))
            } catch (e: Exception) {
                labels.add(daily.date)
            }
        }
        
        val dataSet = BarDataSet(entries, "").apply {
            color = Color.parseColor("#6366F1")
            valueTextColor = Color.parseColor("#94A3B8")
            valueTextSize = 10f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return if (value >= 60) {
                        String.format("%.1fh", value / 60)
                    } else {
                        String.format("%.0fm", value)
                    }
                }
            }
            setGradientColor(Color.parseColor("#6366F1"), Color.parseColor("#8B5CF6"))
        }
        
        val data = BarData(dataSet).apply {
            barWidth = 0.6f
        }
        
        binding.barChart.data = data
        binding.barChart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index >= 0 && index < labels.size) labels[index] else ""
            }
        }
        
        binding.barChart.animateY(800, Easing.EaseInOutCubic)
        binding.barChart.invalidate()
    }
    
    private fun formatDuration(millis: Long): String {
        val seconds = millis / 1000
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${secs}s"
            else -> "${secs}s"
        }
    }
    
    private fun startAutoRefresh() {
        stopAutoRefresh()
        refreshJob = lifecycleScope.launch {
            while (isTracking) {
                kotlinx.coroutines.delay(5000)
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

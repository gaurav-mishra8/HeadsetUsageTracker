package com.headphonetracker

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.ComponentName
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.headphonetracker.data.AppUsageSummary
import com.headphonetracker.data.DailyUsageSummary
import com.headphonetracker.data.HeadphoneUsageDao
import com.headphonetracker.data.SettingsRepository
import com.headphonetracker.databinding.FragmentTodayBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class TodayFragment : Fragment() {

    private var _binding: FragmentTodayBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var headphoneUsageDao: HeadphoneUsageDao

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private lateinit var adapter: AppUsageAdapter
    private var isTracking = false
    private var refreshJob: Job? = null
    private var cachedWeeklyData: List<DailyUsageSummary> = emptyList()

    // Date navigation
    private var selectedDate: Calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
    private val minDate: Calendar = Calendar.getInstance().apply {
        add(Calendar.MONTH, -3)
    }

    // Modern color palette
    private val chartColors = listOf(
        Color.parseColor("#6366F1"),
        Color.parseColor("#EC4899"),
        Color.parseColor("#06B6D4"),
        Color.parseColor("#10B981"),
        Color.parseColor("#F59E0B"),
        Color.parseColor("#8B5CF6"),
        Color.parseColor("#14B8A6"),
        Color.parseColor("#F472B6")
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTodayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupCharts()
        setupButton()
        setupExportButton()
        setupDateNavigation()
        setupPullToRefresh()

        animateCardsOnLoad()

        // Observe tracking state
        viewLifecycleOwner.lifecycleScope.launch {
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

    fun resetToToday() {
        selectedDate = Calendar.getInstance()
        if (_binding != null) {
            updateDateDisplay()
            loadDataForSelectedDate()
            binding.scrollView.smoothScrollTo(0, 0)
        }
    }

    fun refreshData() {
        if (_binding != null) {
            loadData()
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
        updateDateDisplay()

        binding.btnPrevDay.setOnClickListener {
            HapticUtils.performClickFeedback(it)
            val prevDay = selectedDate.clone() as Calendar
            prevDay.add(Calendar.DAY_OF_YEAR, -1)

            if (prevDay.timeInMillis >= minDate.timeInMillis) {
                selectedDate = prevDay
                updateDateDisplay()
                loadDataForSelectedDate()
            } else {
                HapticUtils.performErrorFeedback(requireContext())
                Toast.makeText(requireContext(), "Can't go back more than 3 months", Toast.LENGTH_SHORT).show()
            }
        }

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
                HapticUtils.performErrorFeedback(requireContext())
                Toast.makeText(requireContext(), "Can't view future dates", Toast.LENGTH_SHORT).show()
            }
        }

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

        binding.btnNextDay.alpha = if (isToday) 0.3f else 1f
        binding.btnNextDay.isEnabled = !isToday

        binding.tvSelectedDateLabel.setTextColor(
            if (isToday) {
                ContextCompat.getColor(requireContext(), R.color.success)
            } else {
                ContextCompat.getColor(requireContext(), R.color.primary_light)
            }
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

        datePicker.show(childFragmentManager, "DATE_PICKER")
    }

    private fun loadDataForSelectedDate() {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.loadingSkeleton.visibility = View.VISIBLE
            binding.rvAppList.visibility = View.GONE
            binding.emptyState.visibility = View.GONE

            val dateString = dateFormat.format(selectedDate.time)

            val totalDuration = withContext(Dispatchers.IO) {
                headphoneUsageDao.getTotalUsageForDate(dateString) ?: 0L
            }

            animateTotalTime(totalDuration)

            val appUsage = withContext(Dispatchers.IO) {
                headphoneUsageDao.getUsageByAppForDate(dateString)
            }

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

            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun setupPullToRefresh() {
        binding.swipeRefresh.setColorSchemeColors(
            ContextCompat.getColor(requireContext(), R.color.primary),
            ContextCompat.getColor(requireContext(), R.color.primary_light),
            ContextCompat.getColor(requireContext(), R.color.secondary)
        )

        binding.swipeRefresh.setOnRefreshListener {
            HapticUtils.performSwipeFeedback(binding.swipeRefresh)
            loadDataForSelectedDate()
        }
    }

    private fun updateStatusIndicator(tracking: Boolean) {
        if (_binding == null) return

        if (tracking) {
            binding.statusDot.setBackgroundResource(R.drawable.status_dot_active)
            binding.tvStatus.text = "Tracking"
            binding.tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.success))

            val pulseAnimator = ObjectAnimator.ofFloat(binding.statusDot, "alpha", 1f, 0.4f, 1f)
            pulseAnimator.duration = 1500
            pulseAnimator.repeatCount = ValueAnimator.INFINITE
            pulseAnimator.start()
        } else {
            binding.statusDot.setBackgroundResource(R.drawable.status_dot_inactive)
            binding.tvStatus.text = "Inactive"
            binding.tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            binding.statusDot.alpha = 1f
        }
    }

    private fun setupRecyclerView() {
        adapter = AppUsageAdapter(emptyList(), 0, requireContext().packageManager)
        binding.rvAppList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAppList.adapter = adapter

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

            centerText = "Usage"
            setCenterTextSize(14f)
            setCenterTextColor(Color.parseColor("#94A3B8"))

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

            val colorDot = legendItem.findViewById<View>(R.id.legendColorDot)
            val drawable = colorDot.background.mutate()
            drawable.setTint(chartColors[index % chartColors.size])
            colorDot.background = drawable

            val appNameText = legendItem.findViewById<android.widget.TextView>(R.id.legendAppName)
            appNameText.text = usage.appName

            val percentageText = legendItem.findViewById<android.widget.TextView>(R.id.legendPercentage)
            val percentage = (usage.totalDuration * 100f / totalDuration)
            percentageText.text = String.format(Locale.getDefault(), "%.0f%%", percentage)

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

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                setDrawAxisLine(false)
                granularity = 1f
                textColor = Color.parseColor("#94A3B8")
                textSize = 11f
                labelRotationAngle = 0f
            }

            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#1E293B")
                setDrawAxisLine(false)
                textColor = Color.parseColor("#64748B")
                textSize = 10f
                axisMinimum = 0f
            }

            axisRight.isEnabled = false
            legend.isEnabled = false

            setNoDataText("No weekly data")
            setNoDataTextColor(Color.parseColor("#64748B"))
        }
    }

    private fun setupButton() {
        binding.btnToggleTracking.setOnClickListener {
            HapticUtils.performClickFeedback(it)
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
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val calendar = Calendar.getInstance()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val endDate = dateFormat.format(calendar.time)
                calendar.add(Calendar.DAY_OF_YEAR, -6)
                val startDate = dateFormat.format(calendar.time)

                val detailedUsage = withContext(Dispatchers.IO) {
                    headphoneUsageDao.getUsageForDateRange(startDate, endDate)
                }

                if (cachedWeeklyData.isEmpty() && detailedUsage.isEmpty()) {
                    Toast.makeText(requireContext(), "No data to export", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                ExportUtils.exportAndShareWeeklyData(
                    requireActivity(),
                    cachedWeeklyData,
                    detailedUsage
                )
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggleTracking() {
        val context = requireContext()
        val intent = Intent(context, HeadphoneTrackingService::class.java)

        if (isTracking) {
            intent.action = "STOP_TRACKING"
            context.stopService(intent)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent.apply { action = "START_TRACKING" })
            } else {
                context.startService(intent.apply { action = "START_TRACKING" })
            }
            maybePromptNotificationAccess()
        }

        requestNotificationPermission()
    }

    /**
     * Show a one-time prompt asking the user to grant Notification Access
     * so MediaSessionManager can accurately detect which app is playing audio.
     * If they decline, Strategy 3 (UsageEvents) still works as a fallback.
     */
    private fun maybePromptNotificationAccess() {
        // Only prompt once
        if (settingsRepository.isNotificationAccessPrompted()) return
        // Already granted — nothing to do
        if (isNotificationListenerEnabled()) return

        settingsRepository.setNotificationAccessPrompted(true)

        val isNight = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val dialogStyle = if (isNight) R.style.AlertDialogTheme_Dark else R.style.AlertDialogTheme_Light
        val themedContext = ContextThemeWrapper(requireContext(), dialogStyle)

        MaterialAlertDialogBuilder(themedContext)
            .setTitle("Improve App Detection")
            .setMessage(
                "For the most accurate tracking, Headphone Tracker needs " +
                "Notification Access to detect which app is playing audio.\n\n" +
                "This lets us correctly attribute listening time to apps like " +
                "YouTube, Spotify, or podcasts — even when they play in the background.\n\n" +
                "Your notifications are never read or stored."
            )
            .setPositiveButton("Grant Access") { dialog, _ ->
                dialog.dismiss()
                try {
                    startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Could not open settings", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Maybe Later", null)
            .setCancelable(true)
            .show()
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val componentName = ComponentName(
            requireContext(), MediaNotificationListener::class.java
        )
        val flat = android.provider.Settings.Secure.getString(
            requireContext().contentResolver, "enabled_notification_listeners"
        )
        return flat != null && flat.contains(componentName.flattenToString())
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()) {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
                }
                startActivity(intent)
            }
        }
    }

    private fun updateButton() {
        if (_binding == null) return

        if (isTracking) {
            binding.btnToggleTracking.text = getString(R.string.stop_tracking)
            binding.btnToggleTracking.setIconResource(android.R.drawable.ic_media_pause)
            binding.btnToggleTracking.backgroundTintList =
                android.content.res.ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.error))
        } else {
            binding.btnToggleTracking.text = getString(R.string.start_tracking)
            binding.btnToggleTracking.setIconResource(android.R.drawable.ic_media_play)
            binding.btnToggleTracking.backgroundTintList =
                android.content.res.ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary))
        }
        // Ensure text & icon are always white for contrast
        binding.btnToggleTracking.setTextColor(Color.WHITE)
        binding.btnToggleTracking.iconTint = android.content.res.ColorStateList.valueOf(Color.WHITE)
    }

    private fun loadData() {
        loadDataForSelectedDate()

        viewLifecycleOwner.lifecycleScope.launch {
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
            entries.add(PieEntry(percentage, ""))
            colors.add(chartColors[index % chartColors.size])
        }

        val dataSet = PieDataSet(entries, "").apply {
            setColors(colors)
            valueTextSize = 0f
            sliceSpace = 3f
            selectionShift = 8f
        }

        val data = PieData(dataSet)
        binding.pieChart.data = data
        binding.pieChart.animateY(800, Easing.EaseInOutCubic)
        binding.pieChart.invalidate()

        updateCustomLegend(usageList, totalDuration)
    }

    private fun updateBarChart(dailyUsage: List<DailyUsageSummary>) {
        if (_binding == null) return

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
                        String.format(Locale.getDefault(), "%.1fh", value / 60)
                    } else {
                        String.format(Locale.getDefault(), "%.0fm", value)
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
        refreshJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isTracking) {
                kotlinx.coroutines.delay(5000)
                if (isTracking && _binding != null) {
                    loadData()
                }
            }
        }
    }

    private fun stopAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = null
    }

    override fun onResume() {
        super.onResume()
        isTracking = HeadphoneTrackingService.isTrackingFlow.value
        if (_binding != null) {
            updateButton()
            updateStatusIndicator(isTracking)
        }
    }

    override fun onPause() {
        super.onPause()
        stopAutoRefresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopAutoRefresh()
        _binding = null
    }
}

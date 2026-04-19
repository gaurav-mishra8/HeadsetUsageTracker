package com.headphonetracker

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.headphonetracker.data.DailyUsageSummary
import com.headphonetracker.data.HeadphoneUsageDao
import com.headphonetracker.databinding.ActivityAppDetailBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class AppDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_APP_NAME = "app_name"
    }

    private lateinit var binding: ActivityAppDetailBinding

    @Inject
    lateinit var headphoneUsageDao: HeadphoneUsageDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: run { finish(); return }
        val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: packageName

        setupToolbar(appName)
        loadAppIcon(packageName)
        loadStats(packageName, appName)
        setupBarChart()
    }

    private fun setupToolbar(appName: String) {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = appName
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun loadAppIcon(packageName: String) {
        try {
            val icon = packageManager.getApplicationIcon(packageName)
            binding.ivAppIcon.setImageDrawable(icon)
        } catch (_: Exception) {
            binding.ivAppIcon.setImageResource(android.R.drawable.sym_def_app_icon)
        }

        binding.tvAppName.text = try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            packageName.substringAfterLast(".")
        }

        binding.tvCategory.text = AppCategories.categorize(packageName).label
    }

    private fun loadStats(packageName: String, appName: String) {
        lifecycleScope.launch {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val today = dateFormat.format(Date())
            val weekStart = dateFormat.format(
                Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }.time
            )

            val todayMs = withContext(Dispatchers.IO) {
                headphoneUsageDao.getDurationForAppSince(packageName, today) ?: 0L
            }
            val weekMs = withContext(Dispatchers.IO) {
                headphoneUsageDao.getDurationForAppSince(packageName, weekStart) ?: 0L
            }
            val allTimeMs = withContext(Dispatchers.IO) {
                headphoneUsageDao.getTotalDurationForApp(packageName) ?: 0L
            }
            val last30Days = withContext(Dispatchers.IO) {
                headphoneUsageDao.getLast30DaysUsageForApp(packageName)
            }

            binding.tvToday.text = DurationUtils.formatDuration(todayMs)
            binding.tvThisWeek.text = DurationUtils.formatDuration(weekMs)
            binding.tvAllTime.text = DurationUtils.formatDuration(allTimeMs)

            updateBarChart(last30Days)
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
                textSize = 10f
            }

            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#E2E8F0")
                setDrawAxisLine(false)
                textColor = Color.parseColor("#64748B")
                textSize = 10f
                axisMinimum = 0f
            }

            axisRight.isEnabled = false
            legend.isEnabled = false
            setNoDataText("No data yet")
            setNoDataTextColor(Color.parseColor("#64748B"))
        }
    }

    private fun updateBarChart(dailyUsage: List<DailyUsageSummary>) {
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
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(daily.date)
                labels.add(SimpleDateFormat("d MMM", Locale.getDefault()).format(date ?: Date()))
            } catch (_: Exception) {
                labels.add(daily.date)
            }
        }

        val dataSet = BarDataSet(entries, "").apply {
            color = Color.parseColor("#6366F1")
            valueTextColor = Color.parseColor("#94A3B8")
            valueTextSize = 9f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    if (value == 0f) return ""
                    return if (value >= 60) {
                        String.format(Locale.getDefault(), "%.1fh", value / 60)
                    } else {
                        String.format(Locale.getDefault(), "%.0fm", value)
                    }
                }
            }
            setGradientColor(Color.parseColor("#6366F1"), Color.parseColor("#8B5CF6"))
        }

        binding.barChart.data = BarData(dataSet).apply { barWidth = 0.6f }
        binding.barChart.xAxis.apply {
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val i = value.toInt()
                    return if (i >= 0 && i < labels.size) labels[i] else ""
                }
            }
            // Show a label every ~5 days to avoid crowding on 30-day view
            setLabelCount((labels.size.coerceAtMost(30) / 5).coerceAtLeast(2), true)
        }
        binding.barChart.animateY(800, Easing.EaseInOutCubic)
        binding.barChart.invalidate()
    }
}

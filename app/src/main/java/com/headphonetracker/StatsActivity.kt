package com.headphonetracker

import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.headphonetracker.data.HeadphoneUsageDao
import com.headphonetracker.databinding.ActivityStatsBinding
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
class StatsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatsBinding

    @Inject
    lateinit var headphoneUsageDao: HeadphoneUsageDao

    // App categories mapping
    private val musicApps = setOf(
        "com.spotify.music",
        "com.apple.android.music",
        "com.amazon.mp3",
        "com.google.android.apps.youtube.music",
        "com.soundcloud.android",
        "deezer.android.app",
        "com.pandora.android"
    )
    private val podcastApps = setOf(
        "com.google.android.apps.podcasts",
        "com.spotify.music",
        "com.apple.podcasts",
        "fm.castbox.audiobook.radio.podcast",
        "com.bambuna.podcastaddict"
    )
    private val videoApps = setOf(
        "com.google.android.youtube",
        "com.netflix.mediaclient",
        "com.amazon.avod.thirdpartyclient",
        "com.disney.disneyplus",
        "com.hbo.hbonow"
    )
    private val socialApps = setOf("com.instagram.android", "com.zhiliaoapp.musically", "com.snapchat.android", "com.facebook.katana")
    private val gamingApps = setOf("com.supercell.clashofclans", "com.kiloo.subwaysurf", "com.mojang.minecraftpe")
    private val callApps = setOf(
        "com.google.android.dialer",
        "com.whatsapp",
        "org.telegram.messenger",
        "com.discord",
        "us.zoom.videomeetings"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // DAO is injected by Hilt

        binding.toolbar.setNavigationOnClickListener { finish() }

        loadStats()
    }

    private fun loadStats() {
        lifecycleScope.launch {
            loadStreaks()
            loadWeekComparison()
            loadHourlyChart()
            loadCategories()
            loadMonthlyStats()
        }
    }

    private suspend fun loadStreaks() {
        val allDates = withContext(Dispatchers.IO) {
            headphoneUsageDao.getAllUsage()
                .map { it.date }
                .distinct()
                .sortedDescending()
        }

        if (allDates.isEmpty()) {
            binding.tvCurrentStreak.text = "0"
            binding.tvLongestStreak.text = "0"
            return
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(Date())

        // Calculate current streak
        var currentStreak = 0
        var checkDate = Calendar.getInstance()

        for (i in 0..365) {
            val dateStr = dateFormat.format(checkDate.time)
            if (allDates.contains(dateStr)) {
                currentStreak++
                checkDate.add(Calendar.DAY_OF_YEAR, -1)
            } else if (i == 0) {
                // Today might not have data yet, check yesterday
                checkDate.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }

        // Calculate longest streak
        var longestStreak = 0
        var tempStreak = 0
        val sortedDates = allDates.sorted()

        for (i in sortedDates.indices) {
            if (i == 0) {
                tempStreak = 1
            } else {
                val prevDate = dateFormat.parse(sortedDates[i - 1])
                val currDate = dateFormat.parse(sortedDates[i])
                val diffDays = ((currDate?.time ?: 0) - (prevDate?.time ?: 0)) / (1000 * 60 * 60 * 24)

                if (diffDays == 1L) {
                    tempStreak++
                } else {
                    longestStreak = maxOf(longestStreak, tempStreak)
                    tempStreak = 1
                }
            }
        }
        longestStreak = maxOf(longestStreak, tempStreak)

        binding.tvCurrentStreak.text = currentStreak.toString()
        binding.tvLongestStreak.text = longestStreak.toString()
    }

    private suspend fun loadWeekComparison() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()

        // This week
        val thisWeekEnd = dateFormat.format(calendar.time)
        calendar.add(Calendar.DAY_OF_YEAR, -6)
        val thisWeekStart = dateFormat.format(calendar.time)

        // Last week
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val lastWeekEnd = dateFormat.format(calendar.time)
        calendar.add(Calendar.DAY_OF_YEAR, -6)
        val lastWeekStart = dateFormat.format(calendar.time)

        val allUsage = withContext(Dispatchers.IO) {
            headphoneUsageDao.getAllUsage()
        }

        val thisWeekTotal = allUsage.filter { it.date in thisWeekStart..thisWeekEnd }.sumOf { it.duration }
        val lastWeekTotal = allUsage.filter { it.date in lastWeekStart..lastWeekEnd }.sumOf { it.duration }

        binding.tvThisWeekTotal.text = formatDurationShort(thisWeekTotal)
        binding.tvLastWeekTotal.text = formatDurationShort(lastWeekTotal)

        val changePercent = if (lastWeekTotal > 0) {
            ((thisWeekTotal - lastWeekTotal) * 100 / lastWeekTotal).toInt()
        } else if (thisWeekTotal > 0) {
            100
        } else {
            0
        }

        binding.tvWeekChange.text = "${if (changePercent >= 0) "+" else ""}$changePercent%"
        binding.tvWeekChange.setTextColor(
            if (changePercent >= 0) {
                ContextCompat.getColor(this, R.color.success)
            } else {
                ContextCompat.getColor(this, R.color.error)
            }
        )
    }

    private suspend fun loadHourlyChart() {
        val allUsage = withContext(Dispatchers.IO) {
            headphoneUsageDao.getAllUsage()
        }

        // Group by hour
        val hourlyUsage = LongArray(24) { 0L }
        allUsage.forEach { usage ->
            val hour = Calendar.getInstance().apply {
                timeInMillis = usage.startTime
            }.get(Calendar.HOUR_OF_DAY)
            hourlyUsage[hour] += usage.duration
        }

        val entries = hourlyUsage.mapIndexed { index, duration ->
            BarEntry(index.toFloat(), (duration / 60f)) // Convert to minutes
        }

        val dataSet = BarDataSet(entries, "").apply {
            color = ContextCompat.getColor(this@StatsActivity, R.color.primary)
            setDrawValues(false)
        }

        binding.hourlyChart.apply {
            data = BarData(dataSet).apply { barWidth = 0.8f }
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(false)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = ContextCompat.getColor(this@StatsActivity, R.color.text_tertiary)
                textSize = 10f
                labelCount = 6
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val hour = value.toInt()
                        return when {
                            hour == 0 -> "12am"
                            hour == 12 -> "12pm"
                            hour < 12 -> "${hour}am"
                            else -> "${hour - 12}pm"
                        }
                    }
                }
            }

            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = ContextCompat.getColor(this@StatsActivity, R.color.divider)
                textColor = ContextCompat.getColor(this@StatsActivity, R.color.text_tertiary)
                textSize = 10f
                axisMinimum = 0f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return "${value.toInt()}m"
                    }
                }
            }

            axisRight.isEnabled = false
            setFitBars(true)
            invalidate()
        }

        // Find peak hour
        val peakHour = hourlyUsage.indices.maxByOrNull { hourlyUsage[it] } ?: 0
        val peakHourStr = when {
            peakHour == 0 -> "12 AM - 1 AM"
            peakHour == 12 -> "12 PM - 1 PM"
            peakHour < 12 -> "$peakHour AM - ${peakHour + 1} AM"
            else -> "${peakHour - 12} PM - ${peakHour - 11} PM"
        }
        binding.tvPeakHour.text = "Peak listening: $peakHourStr"
    }

    private suspend fun loadCategories() {
        val allUsage = withContext(Dispatchers.IO) {
            headphoneUsageDao.getAllUsage()
        }

        val categoryTotals = mutableMapOf(
            "Music" to 0L,
            "Podcasts" to 0L,
            "Video" to 0L,
            "Social" to 0L,
            "Gaming" to 0L,
            "Calls" to 0L,
            "Other" to 0L
        )

        allUsage.forEach { usage ->
            val category = when {
                musicApps.contains(usage.packageName) -> "Music"
                podcastApps.contains(usage.packageName) -> "Podcasts"
                videoApps.contains(usage.packageName) -> "Video"
                socialApps.contains(usage.packageName) -> "Social"
                gamingApps.contains(usage.packageName) -> "Gaming"
                callApps.contains(usage.packageName) -> "Calls"
                else -> "Other"
            }
            categoryTotals[category] = (categoryTotals[category] ?: 0L) + usage.duration
        }

        val totalDuration = categoryTotals.values.sum().coerceAtLeast(1)
        val sortedCategories = categoryTotals.entries.sortedByDescending { it.value }

        val colors = mapOf(
            "Music" to R.color.chart_1,
            "Video" to R.color.chart_2,
            "Podcasts" to R.color.chart_3,
            "Social" to R.color.chart_4,
            "Gaming" to R.color.chart_5,
            "Calls" to R.color.primary,
            "Other" to R.color.text_tertiary
        )

        binding.categoriesContainer.removeAllViews()

        sortedCategories.filter { it.value > 0 }.forEach { (category, duration) ->
            val percentage = (duration * 100 / totalDuration).toInt()

            val itemView = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 8.dpToPx(), 0, 8.dpToPx())
            }

            val labelLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val categoryLabel = TextView(this).apply {
                text = category
                setTextColor(ContextCompat.getColor(this@StatsActivity, R.color.text_primary))
                textSize = 14f
            }

            val durationLabel = TextView(this).apply {
                text = formatDurationShort(duration)
                setTextColor(ContextCompat.getColor(this@StatsActivity, R.color.text_tertiary))
                textSize = 12f
            }

            labelLayout.addView(categoryLabel)
            labelLayout.addView(durationLabel)

            val progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
                layoutParams = LinearLayout.LayoutParams(0, 8.dpToPx(), 2f).apply {
                    marginStart = 16.dpToPx()
                    marginEnd = 8.dpToPx()
                }
                max = 100
                progress = percentage
                progressDrawable = ContextCompat.getDrawable(this@StatsActivity, R.drawable.progress_bar_category)
                progressTintList = ContextCompat.getColorStateList(this@StatsActivity, colors[category] ?: R.color.primary)
            }

            val percentLabel = TextView(this).apply {
                text = "$percentage%"
                setTextColor(ContextCompat.getColor(this@StatsActivity, R.color.text_secondary))
                textSize = 14f
                minWidth = 48.dpToPx()
                gravity = Gravity.END
            }

            itemView.addView(labelLayout)
            itemView.addView(progressBar)
            itemView.addView(percentLabel)

            binding.categoriesContainer.addView(itemView)
        }
    }

    private suspend fun loadMonthlyStats() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()

        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val monthStart = dateFormat.format(calendar.time)

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        val monthEnd = dateFormat.format(calendar.time)

        val monthUsage = withContext(Dispatchers.IO) {
            headphoneUsageDao.getAllUsage()
                .filter { it.date in monthStart..monthEnd }
        }

        val totalDuration = monthUsage.sumOf { it.duration }
        val activeDays = monthUsage.map { it.date }.distinct().size
        val avgDaily = if (activeDays > 0) totalDuration / activeDays else 0

        binding.tvMonthTotal.text = formatDurationShort(totalDuration)
        binding.tvMonthAverage.text = formatDurationShort(avgDaily)
        binding.tvMonthDays.text = activeDays.toString()
    }

    private fun formatDurationShort(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}

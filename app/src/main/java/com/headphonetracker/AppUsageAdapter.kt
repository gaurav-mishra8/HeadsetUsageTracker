package com.headphonetracker

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.recyclerview.widget.RecyclerView
import com.headphonetracker.data.AppUsageSummary
import com.headphonetracker.databinding.ItemAppUsageBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppUsageAdapter(
    private var usageList: List<AppUsageSummary>,
    private var totalDuration: Long,
    private val packageManager: android.content.pm.PackageManager
) : RecyclerView.Adapter<AppUsageAdapter.ViewHolder>() {

    private var lastAnimatedPosition = -1

    // Cache for app icons to avoid repeated loading
    private val iconCache = mutableMapOf<String, Drawable?>()

    // Track loading jobs per ViewHolder to cancel on rebind
    private val loadingJobs = mutableMapOf<Int, Job>()

    class ViewHolder(val binding: ItemAppUsageBinding) : RecyclerView.ViewHolder(binding.root) {
        var currentPackage: String? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAppUsageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val usage = usageList[position]

        holder.binding.tvAppName.text = usage.appName
        holder.binding.tvUsageTime.text = formatDuration(usage.totalDuration)

        val percentage = if (totalDuration > 0) {
            (usage.totalDuration * 100 / totalDuration).toInt()
        } else {
            0
        }
        holder.binding.tvPercentage.text = "$percentage%"

        // Track which package this holder is displaying
        holder.currentPackage = usage.packageName

        // Cancel any existing loading job for this position
        loadingJobs[holder.hashCode()]?.cancel()

        // Load app icon with caching
        loadAppIcon(holder, usage.packageName)

        // Item animation
        if (position > lastAnimatedPosition) {
            animateItem(holder, position)
            lastAnimatedPosition = position
        }
    }

    private fun loadAppIcon(holder: ViewHolder, packageName: String) {
        // Check cache first
        if (iconCache.containsKey(packageName)) {
            val cachedIcon = iconCache[packageName]
            if (cachedIcon != null) {
                holder.binding.ivAppIcon.setImageDrawable(cachedIcon)
            } else {
                holder.binding.ivAppIcon.setImageResource(android.R.drawable.sym_def_app_icon)
            }
            return
        }

        // Set placeholder while loading
        holder.binding.ivAppIcon.setImageResource(android.R.drawable.sym_def_app_icon)

        // Load icon asynchronously
        val job = CoroutineScope(Dispatchers.IO).launch {
            try {
                // Try to get app icon directly using package name
                val icon = try {
                    packageManager.getApplicationIcon(packageName)
                } catch (e: Exception) {
                    // Fallback: try getting through ApplicationInfo
                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    packageManager.getApplicationIcon(appInfo)
                }

                // Cache the icon
                iconCache[packageName] = icon

                withContext(Dispatchers.Main) {
                    // Only set if this holder is still displaying the same package
                    if (holder.currentPackage == packageName) {
                        holder.binding.ivAppIcon.setImageDrawable(icon)
                        android.util.Log.d("AppUsageAdapter", "Icon loaded for: $packageName")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AppUsageAdapter", "Failed to load icon for $packageName: ${e.message}")
                // Cache the failure too (null = use default)
                iconCache[packageName] = null

                withContext(Dispatchers.Main) {
                    if (holder.currentPackage == packageName) {
                        holder.binding.ivAppIcon.setImageResource(android.R.drawable.sym_def_app_icon)
                    }
                }
            }
        }

        loadingJobs[holder.hashCode()] = job
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        // Cancel loading when view is recycled
        loadingJobs[holder.hashCode()]?.cancel()
        holder.currentPackage = null
    }

    private fun animateItem(holder: ViewHolder, position: Int) {
        holder.itemView.alpha = 0f
        holder.itemView.translationX = -50f

        holder.itemView.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(300)
            .setStartDelay((position * 50).toLong())
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    override fun getItemCount(): Int = usageList.size

    fun updateData(newList: List<AppUsageSummary>, newTotal: Long) {
        val sortedList = newList.sortedByDescending { it.totalDuration }

        // Reset animation position when data changes significantly
        if (sortedList.size != usageList.size) {
            lastAnimatedPosition = -1
        }

        usageList = sortedList
        totalDuration = newTotal
        notifyDataSetChanged()
    }

    fun clearIconCache() {
        iconCache.clear()
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
}

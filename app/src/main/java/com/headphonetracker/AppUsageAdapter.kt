package com.headphonetracker

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.headphonetracker.data.AppUsageSummary
import com.headphonetracker.databinding.ItemAppUsageBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppUsageAdapter(
    private var usageList: List<AppUsageSummary>,
    private val totalDuration: Long,
    private val packageManager: android.content.pm.PackageManager
) : RecyclerView.Adapter<AppUsageAdapter.ViewHolder>() {
    
    class ViewHolder(val binding: ItemAppUsageBinding) : RecyclerView.ViewHolder(binding.root)
    
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
        
        // Load app icon
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appInfo = packageManager.getApplicationInfo(usage.packageName, 0)
                val icon = packageManager.getApplicationIcon(appInfo)
                withContext(Dispatchers.Main) {
                    holder.binding.ivAppIcon.setImageDrawable(icon)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    holder.binding.ivAppIcon.setImageResource(android.R.drawable.sym_def_app_icon)
                }
            }
        }
    }
    
    override fun getItemCount(): Int = usageList.size
    
    fun updateData(newList: List<AppUsageSummary>, newTotal: Long) {
        usageList = newList.sortedByDescending { it.totalDuration }
        notifyDataSetChanged()
    }
    
    private fun formatDuration(millis: Long): String {
        val seconds = millis / 1000
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "${seconds}s"
        }
    }
}


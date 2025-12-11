package com.headphonetracker

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.headphonetracker.data.AppDatabase
import com.headphonetracker.databinding.ActivityExcludedAppsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExcludedAppsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExcludedAppsBinding
    private lateinit var database: AppDatabase
    private val prefs by lazy { getSharedPreferences("headphone_tracker_prefs", Context.MODE_PRIVATE) }
    private lateinit var adapter: ExcludedAppsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExcludedAppsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getDatabase(this)

        binding.toolbar.setNavigationOnClickListener { finish() }

        setupRecyclerView()
        loadApps()
    }

    private fun setupRecyclerView() {
        adapter = ExcludedAppsAdapter(
            getExcludedApps(),
            packageManager
        ) { packageName, isExcluded ->
            updateExcludedApp(packageName, isExcluded)
        }
        binding.rvApps.layoutManager = LinearLayoutManager(this)
        binding.rvApps.adapter = adapter
    }

    private fun getExcludedApps(): MutableSet<String> {
        return prefs.getStringSet("excluded_apps", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
    }

    private fun updateExcludedApp(packageName: String, isExcluded: Boolean) {
        val excludedApps = getExcludedApps()
        if (isExcluded) {
            excludedApps.add(packageName)
        } else {
            excludedApps.remove(packageName)
        }
        prefs.edit().putStringSet("excluded_apps", excludedApps).apply()
    }

    private fun loadApps() {
        lifecycleScope.launch {
            val apps = withContext(Dispatchers.IO) {
                // Get unique apps from usage history
                database.headphoneUsageDao().getAllUsage()
                    .map { AppInfo(it.packageName, it.appName) }
                    .distinctBy { it.packageName }
                    .sortedBy { it.appName.lowercase() }
            }

            if (apps.isEmpty()) {
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.rvApps.visibility = View.GONE
            } else {
                binding.tvEmptyState.visibility = View.GONE
                binding.rvApps.visibility = View.VISIBLE
                adapter.updateApps(apps)
            }
        }
    }

    data class AppInfo(val packageName: String, val appName: String)

    class ExcludedAppsAdapter(
        private var excludedApps: MutableSet<String>,
        private val packageManager: android.content.pm.PackageManager,
        private val onExclusionChanged: (String, Boolean) -> Unit
    ) : RecyclerView.Adapter<ExcludedAppsAdapter.ViewHolder>() {

        private var apps: List<AppInfo> = emptyList()
        private val iconCache = mutableMapOf<String, Drawable?>()

        fun updateApps(newApps: List<AppInfo>) {
            apps = newApps
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_excluded_app, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val app = apps[position]
            holder.bind(app, excludedApps.contains(app.packageName))
        }

        override fun getItemCount() = apps.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val ivIcon: ImageView = itemView.findViewById(R.id.ivAppIcon)
            private val tvName: TextView = itemView.findViewById(R.id.tvAppName)
            private val tvPackage: TextView = itemView.findViewById(R.id.tvPackageName)
            private val checkbox: CheckBox = itemView.findViewById(R.id.checkExcluded)

            fun bind(app: AppInfo, isExcluded: Boolean) {
                tvName.text = app.appName
                tvPackage.text = app.packageName
                checkbox.isChecked = isExcluded

                // Load icon
                val cachedIcon = iconCache[app.packageName]
                if (cachedIcon != null) {
                    ivIcon.setImageDrawable(cachedIcon)
                } else {
                    try {
                        val icon = packageManager.getApplicationIcon(app.packageName)
                        iconCache[app.packageName] = icon
                        ivIcon.setImageDrawable(icon)
                    } catch (e: Exception) {
                        ivIcon.setImageResource(R.drawable.ic_headphones)
                    }
                }

                checkbox.setOnCheckedChangeListener { _, checked ->
                    if (checked) {
                        excludedApps.add(app.packageName)
                    } else {
                        excludedApps.remove(app.packageName)
                    }
                    onExclusionChanged(app.packageName, checked)
                }

                itemView.setOnClickListener {
                    checkbox.isChecked = !checkbox.isChecked
                }
            }
        }
    }
}


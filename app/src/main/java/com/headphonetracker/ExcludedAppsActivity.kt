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
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
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

        binding.toolbar.setNavigationOnClickListener {
            HapticUtils.performClickFeedback(it)
            finish()
        }

        setupRecyclerView()
        setupSwipeToDelete()
        loadApps()
    }

    private fun setupRecyclerView() {
        adapter = ExcludedAppsAdapter(
            getExcludedApps(),
            packageManager
        ) { packageName, isExcluded ->
            HapticUtils.performSelectionFeedback(binding.rvApps)
            updateExcludedApp(packageName, isExcluded)
        }
        binding.rvApps.layoutManager = LinearLayoutManager(this)
        binding.rvApps.adapter = adapter
    }
    
    private fun setupSwipeToDelete() {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val app = adapter.apps[position]
                
                HapticUtils.performSwipeFeedback(viewHolder.itemView)
                
                // Remove from excluded apps if it was excluded
                if (adapter.excludedApps.contains(app.packageName)) {
                    adapter.excludedApps.remove(app.packageName)
                    updateExcludedApp(app.packageName, false)
                }
                
                // Notify adapter
                adapter.notifyItemChanged(position)
            }

            override fun onChildDraw(
                c: android.graphics.Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val itemView = viewHolder.itemView
                    val background = ContextCompat.getDrawable(
                        this@ExcludedAppsActivity,
                        R.drawable.swipe_delete_background
                    )
                    background?.setBounds(
                        itemView.right + dX.toInt(),
                        itemView.top,
                        itemView.right,
                        itemView.bottom
                    )
                    background?.draw(c)
                    
                    val icon = ContextCompat.getDrawable(
                        this@ExcludedAppsActivity,
                        R.drawable.ic_delete
                    )
                    val iconMargin = (itemView.height - (icon?.intrinsicHeight ?: 0)) / 2
                    val iconTop = itemView.top + iconMargin
                    val iconBottom = iconTop + (icon?.intrinsicHeight ?: 0)
                    val iconLeft = itemView.right - iconMargin - (icon?.intrinsicWidth ?: 0)
                    val iconRight = itemView.right - iconMargin
                    
                    icon?.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    icon?.setTint(ContextCompat.getColor(this@ExcludedAppsActivity, android.R.color.white))
                    icon?.draw(c)
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        })
        
        itemTouchHelper.attachToRecyclerView(binding.rvApps)
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
                binding.cardEmptyState.visibility = View.VISIBLE
                binding.rvApps.visibility = View.GONE
            } else {
                binding.cardEmptyState.visibility = View.GONE
                binding.rvApps.visibility = View.VISIBLE
                adapter.updateApps(apps)
            }
        }
    }

    data class AppInfo(val packageName: String, val appName: String)

    class ExcludedAppsAdapter(
        val excludedApps: MutableSet<String>,
        private val packageManager: android.content.pm.PackageManager,
        private val onExclusionChanged: (String, Boolean) -> Unit
    ) : RecyclerView.Adapter<ExcludedAppsAdapter.ViewHolder>() {

        var apps: List<AppInfo> = emptyList()
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
                    HapticUtils.performSelectionFeedback(itemView)
                    if (checked) {
                        excludedApps.add(app.packageName)
                    } else {
                        excludedApps.remove(app.packageName)
                    }
                    onExclusionChanged(app.packageName, checked)
                }

                itemView.setOnClickListener {
                    HapticUtils.performClickFeedback(itemView)
                    checkbox.isChecked = !checkbox.isChecked
                }
            }
        }
    }
}


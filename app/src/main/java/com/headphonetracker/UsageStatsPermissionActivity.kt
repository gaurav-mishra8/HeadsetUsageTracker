package com.headphonetracker

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.headphonetracker.databinding.ActivityUsageStatsPermissionBinding

class UsageStatsPermissionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUsageStatsPermissionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityUsageStatsPermissionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnGrantPermission.setOnClickListener {
            HapticUtils.performClickFeedback(it)
            openUsageStatsSettings()
        }

        // Animate entrance
        playEntranceAnimation()
    }

    override fun onResume() {
        super.onResume()
        if (hasUsageStatsPermission()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        }
    }

    private fun playEntranceAnimation() {
        val card = binding.cardDialog
        val dimOverlay = binding.viewDimOverlay

        // Dim overlay fade in
        val dimAnim = ObjectAnimator.ofFloat(dimOverlay, View.ALPHA, 0f, 1f).apply {
            duration = 400
            interpolator = DecelerateInterpolator()
        }

        // Card slide up + fade in
        val cardAlpha = ObjectAnimator.ofFloat(card, View.ALPHA, 0f, 1f).apply {
            duration = 500
            interpolator = DecelerateInterpolator()
        }
        val cardTranslate = ObjectAnimator.ofFloat(card, View.TRANSLATION_Y, 60f, 0f).apply {
            duration = 600
            interpolator = OvershootInterpolator(0.8f)
        }

        AnimatorSet().apply {
            playTogether(dimAnim, cardAlpha, cardTranslate)
            startDelay = 150
            start()
        }
    }

    private fun openUsageStatsSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        startActivity(intent)
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

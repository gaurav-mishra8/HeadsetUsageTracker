package com.headphonetracker

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import com.headphonetracker.data.DailyUsageSummary
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object EarReportCard {

    fun shareWeeklyReport(
        context: Context,
        weeklyExposure: List<DailyUsageSummary>,
        totalWeekMs: Long
    ) {
        val bitmap = generateCard(context, weeklyExposure, totalWeekMs)
        val uri = saveBitmap(context, bitmap)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, buildShareText(weeklyExposure))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Ear Report"))
    }

    private fun buildShareText(weeklyExposure: List<DailyUsageSummary>): String {
        val daysWithin = weeklyExposure.count { it.totalDuration <= EarHealthCalculator.DAILY_BUDGET_MINUTES }
        return "My EarGuard weekly report: $daysWithin/${weeklyExposure.size} days within the WHO safe listening budget. #EarGuard #HearingHealth"
    }

    private fun generateCard(
        context: Context,
        weeklyExposure: List<DailyUsageSummary>,
        totalWeekMs: Long
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val W = (360 * density).toInt()
        val H = (220 * density).toInt()
        val dp = density

        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // Background gradient
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        bgPaint.shader = LinearGradient(
            0f, 0f, W.toFloat(), H.toFloat(),
            Color.parseColor("#0F172A"), Color.parseColor("#1E293B"),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(RectF(0f, 0f, W.toFloat(), H.toFloat()), 24 * dp, 24 * dp, bgPaint)

        // Top accent line
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, W.toFloat(), 0f,
                Color.parseColor("#6366F1"), Color.parseColor("#10B981"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(RectF(24 * dp, 0f, W - 24 * dp, 4 * dp), 2 * dp, 2 * dp, accentPaint)

        val textPrimary = Color.parseColor("#F1F5F9")
        val textSecondary = Color.parseColor("#94A3B8")
        val textTertiary = Color.parseColor("#64748B")

        // Title "EarGuard Weekly Report"
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textPrimary
            textSize = 15 * dp
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("EarGuard Weekly Report", 24 * dp, 36 * dp, titlePaint)

        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textSecondary
            textSize = 11 * dp
        }
        val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())
        canvas.drawText(dateStr, 24 * dp, 52 * dp, datePaint)

        // Compute weekly stats (totalDuration here is already weighted minutes as Float stored in Long)
        val avgBudgetPct = if (weeklyExposure.isEmpty()) 0
        else weeklyExposure.map { EarHealthCalculator.budgetPercent(it.totalDuration.toFloat()) }.average().toInt()
        val score = EarHealthCalculator.score(
            if (weeklyExposure.isEmpty()) 0f
            else weeklyExposure.map { it.totalDuration.toFloat() }.average().toFloat()
        )
        val daysWithin = weeklyExposure.count { it.totalDuration.toFloat() <= EarHealthCalculator.DAILY_BUDGET_MINUTES }
        val scoreColor = EarHealthCalculator.statusColor(avgBudgetPct)

        // Big score
        val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = scoreColor
            textSize = 52 * dp
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText(score.toString(), 24 * dp, 120 * dp, scorePaint)

        val scoreLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textSecondary
            textSize = 11 * dp
        }
        canvas.drawText("Avg. Ear Health Score", 24 * dp, 136 * dp, scoreLabelPaint)
        canvas.drawText(EarHealthCalculator.scoreGrade(score), 24 * dp, 152 * dp, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = scoreColor
            textSize = 13 * dp
            typeface = Typeface.DEFAULT_BOLD
        })

        // Stats column on the right
        val col2X = 200 * dp
        drawStatBlock(canvas, col2X, 72 * dp, dp,
            "$daysWithin / ${weeklyExposure.size}", "Days within budget", textPrimary, textTertiary)
        drawStatBlock(canvas, col2X, 116 * dp, dp,
            DurationUtils.formatDuration(totalWeekMs), "Total this week", textPrimary, textTertiary)
        drawStatBlock(canvas, col2X, 160 * dp, dp,
            "$avgBudgetPct%", "Avg. budget used", textPrimary, textTertiary)

        // Footer
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textTertiary
            textSize = 9 * dp
        }
        canvas.drawText("earguard.app  •  Based on WHO 80 dB / 8 h reference", 24 * dp, H.toFloat() - 14 * dp, footerPaint)

        return bmp
    }

    private fun drawStatBlock(
        canvas: Canvas, x: Float, y: Float, dp: Float,
        value: String, label: String, valueCl: Int, labelCl: Int
    ) {
        canvas.drawText(value, x, y, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = valueCl
            textSize = 16 * dp
            typeface = Typeface.DEFAULT_BOLD
        })
        canvas.drawText(label, x, y + 14 * dp, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = labelCl
            textSize = 10 * dp
        })
    }

    private fun saveBitmap(context: Context, bmp: Bitmap): Uri {
        val dir = File(context.cacheDir, "reports")
        dir.mkdirs()
        val file = File(dir, "ear_report_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 95, it) }
        return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    }
}

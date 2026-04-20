package com.headphonetracker

import android.graphics.Color

/**
 * Hearing health calculations based on the WHO/ISO 1999 equal-energy principle.
 *
 * Reference: 80 dB (≈ 80% media volume) for 480 minutes/day is the safe daily limit.
 * Every doubling of sound intensity (≈ 3 dB increase) halves the safe exposure time.
 * We approximate this with a squared volume-ratio weight: (v / 0.8)².
 */
object EarHealthCalculator {

    const val DAILY_BUDGET_MINUTES = 480f // 8 hours at reference volume (80%)

    /**
     * Weight factor for [volumePercent]. Null = treat as 70% (factor ≈ 0.766).
     * At ≤ 40% volume the exposure is negligible (factor = 0.1).
     */
    fun volumeWeight(volumePercent: Float?): Float {
        val v = volumePercent ?: 0.7f
        if (v <= 0.4f) return 0.1f
        return (v / 0.8f) * (v / 0.8f)
    }

    /** Convert raw duration + volume to weighted minutes of hearing exposure. */
    fun weightedMinutes(durationMs: Long, volumePercent: Float?): Float =
        (durationMs / 60_000f) * volumeWeight(volumePercent)

    /** Ear health score 0–100 (100 = no exposure, 0 = budget exhausted). */
    fun score(weightedMinutesToday: Float): Int =
        (100f - (weightedMinutesToday / DAILY_BUDGET_MINUTES * 100f))
            .coerceIn(0f, 100f).toInt()

    /** Budget consumed as 0–100+ percent. */
    fun budgetPercent(weightedMinutesToday: Float): Int =
        (weightedMinutesToday / DAILY_BUDGET_MINUTES * 100f).toInt()

    fun statusMessage(budgetPct: Int): String = when {
        budgetPct < 30  -> "Ears are healthy — great listening habits!"
        budgetPct < 60  -> "You're within safe limits today"
        budgetPct < 85  -> "Getting close — consider lowering volume or taking a break"
        budgetPct < 100 -> "Almost at today's safe limit — give your ears a rest"
        else            -> "Daily hearing budget exceeded — protect your ears now"
    }

    fun scoreGrade(score: Int): String = when {
        score >= 90 -> "Excellent"
        score >= 75 -> "Good"
        score >= 55 -> "Fair"
        score >= 35 -> "Poor"
        else        -> "Critical"
    }

    /** Returns a color int based on budget consumed. */
    fun statusColor(budgetPct: Int): Int = when {
        budgetPct < 60  -> Color.parseColor("#10B981") // green
        budgetPct < 85  -> Color.parseColor("#F59E0B") // amber
        else            -> Color.parseColor("#EF4444") // red
    }

    /** Volume advice string shown in the card. */
    fun volumeAdvice(volumePercent: Float?): String {
        val pct = ((volumePercent ?: 0.7f) * 100).toInt()
        return when {
            pct <= 50  -> "Volume is low — fully safe range"
            pct <= 70  -> "Moderate volume — within safe limits"
            pct <= 85  -> "High volume — budget drains faster"
            else       -> "Very loud — $pct% volume drains budget quickly"
        }
    }
}

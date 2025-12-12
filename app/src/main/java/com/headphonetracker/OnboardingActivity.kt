package com.headphonetracker

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.headphonetracker.databinding.ActivityOnboardingBinding

data class OnboardingPage(
    val iconRes: Int,
    val title: String,
    val subtitle: String,
    val content: String,
    val bulletPoints: List<String>? = null,
    val trivia: String? = null
)

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var adapter: OnboardingAdapter

    private val pages = listOf(
        OnboardingPage(
            iconRes = R.drawable.ic_headphones,
            title = "Welcome to Headphone Tracker",
            subtitle = "Track your listening habits",
            content = "Monitor how much time you spend with headphones across all your apps. Stay aware of your listening patterns and maintain healthy habits."
        ),
        OnboardingPage(
            iconRes = R.drawable.ic_warning,
            title = "Risks of Excessive Headphone Use",
            subtitle = "Protect your hearing health",
            content = "Prolonged headphone usage can lead to several health concerns:",
            bulletPoints = listOf(
                "Hearing loss and tinnitus from loud volumes",
                "Ear infections from prolonged use",
                "Headaches and ear pain",
                "Social isolation and reduced awareness",
                "Sleep disruption if used before bed",
                "Neck and jaw strain from extended use"
            )
        ),
        OnboardingPage(
            iconRes = R.drawable.ic_timer,
            title = "Recommended Listening Limits",
            subtitle = "WHO Guidelines",
            content = "Follow these guidelines to protect your hearing:",
            bulletPoints = listOf(
                "60/60 Rule: 60 minutes at 60% volume maximum",
                "Daily limit: 2-3 hours of continuous use",
                "Take breaks: 10-15 minutes every hour",
                "Volume: Keep below 85 dB (60% of max)",
                "Weekly limit: No more than 40 hours total",
                "Rest days: Take at least one day off per week"
            )
        ),
        OnboardingPage(
            iconRes = R.drawable.ic_milestone,
            title = "Fun Facts About Headphones",
            subtitle = "Did you know?",
            content = "",
            trivia = "The first headphones were invented in 1910 by Nathaniel Baldwin in his kitchen! They weighed over 5 pounds and were used by the US Navy."
        ),
        OnboardingPage(
            iconRes = R.drawable.ic_headphones,
            title = "Hearing Science",
            subtitle = "Did you know?",
            content = "",
            trivia = "The human ear can detect sounds from 20 Hz to 20,000 Hz. Most headphones can reproduce this full range, but prolonged exposure above 85 dB can cause permanent damage. Your ears need rest to recover!"
        ),
        OnboardingPage(
            iconRes = R.drawable.ic_headphones,
            title = "Headphone Evolution",
            subtitle = "Fun history",
            content = "",
            trivia = "Wireless headphones became popular in the 2000s, but the first Bluetooth headphones were released in 2004. Today, over 70% of headphone sales are wireless! Noise-cancelling technology was first developed for pilots in the 1950s."
        ),
        OnboardingPage(
            iconRes = R.drawable.ic_headphones,
            title = "Volume Facts",
            subtitle = "Stay safe",
            content = "",
            trivia = "A normal conversation is about 60 dB. Most smartphones can reach 100+ dB at maximum volume. Listening at 100 dB for just 15 minutes can cause hearing damage. Always use the 60/60 rule: 60% volume for 60 minutes max!"
        ),
        OnboardingPage(
            iconRes = R.drawable.ic_headphones,
            title = "You're All Set!",
            subtitle = "Start tracking your usage",
            content = "Now you can track your headphone usage, set daily limits, get break reminders, and maintain healthy listening habits.\n\nLet's get started!",
            bulletPoints = listOf(
                "Set your daily listening limit",
                "Enable break reminders",
                "Track usage across all apps",
                "View detailed statistics",
                "Export your data anytime"
            )
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewPager()
        setupButtons()
        setupSkipButton()
    }

    private fun setupViewPager() {
        adapter = OnboardingAdapter(pages)
        binding.viewPager.adapter = adapter

        // Connect TabLayout with ViewPager2
        TabLayoutMediator(binding.tabIndicator, binding.viewPager) { _, _ -> }.attach()

        // Update button visibility based on page
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateButtonVisibility(position)
            }
        })

        updateButtonVisibility(0)
    }

    private fun setupButtons() {
        binding.btnNext.setOnClickListener {
            val currentItem = binding.viewPager.currentItem
            if (currentItem < pages.size - 1) {
                binding.viewPager.currentItem = currentItem + 1
            } else {
                finishOnboarding()
            }
        }

        binding.btnPrevious.setOnClickListener {
            val currentItem = binding.viewPager.currentItem
            if (currentItem > 0) {
                binding.viewPager.currentItem = currentItem - 1
            }
        }
    }

    private fun setupSkipButton() {
        binding.tvSkip.setOnClickListener {
            finishOnboarding()
        }
    }

    private fun updateButtonVisibility(position: Int) {
        // Show/hide Previous button
        binding.btnPrevious.visibility = if (position > 0) View.VISIBLE else View.GONE

        // Update Next button text
        binding.btnNext.text = if (position == pages.size - 1) "Get Started" else "Next"

        // Update Next button icon
        if (position == pages.size - 1) {
            binding.btnNext.icon = null
        } else {
            binding.btnNext.setIconResource(R.drawable.ic_chevron_right)
        }
    }

    private fun finishOnboarding() {
        getSharedPreferences("headphone_tracker_prefs", MODE_PRIVATE)
            .edit()
            .putBoolean("onboarding_completed", true)
            .apply()

        // Start MainActivity
        startActivity(android.content.Intent(this, MainActivity::class.java))
        finish()
    }

    class OnboardingAdapter(private val pages: List<OnboardingPage>) :
        RecyclerView.Adapter<OnboardingAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_onboarding, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(pages[position])
        }

        override fun getItemCount() = pages.size

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val ivIcon: ImageView = itemView.findViewById(R.id.ivIcon)
            private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
            private val tvSubtitle: TextView = itemView.findViewById(R.id.tvSubtitle)
            private val tvContent: TextView = itemView.findViewById(R.id.tvContent)
            private val bulletContainer: LinearLayout = itemView.findViewById(R.id.bulletContainer)
            private val cardTrivia: com.google.android.material.card.MaterialCardView = itemView.findViewById(R.id.cardTrivia)
            private val tvTrivia: TextView = itemView.findViewById(R.id.tvTrivia)

            fun bind(page: OnboardingPage) {
                // Set icon with tint
                ivIcon.setImageResource(page.iconRes)
                
                // Apply different colors based on page type
                val iconColor = when {
                    page.trivia != null -> ContextCompat.getColor(itemView.context, R.color.accent_teal)
                    page.bulletPoints != null -> ContextCompat.getColor(itemView.context, R.color.warning)
                    else -> ContextCompat.getColor(itemView.context, R.color.primary)
                }
                
                ivIcon.setColorFilter(iconColor, android.graphics.PorterDuff.Mode.SRC_IN)

                // Set text
                tvTitle.text = page.title
                tvSubtitle.text = page.subtitle
                tvContent.text = page.content

                // Handle bullet points
                if (page.bulletPoints != null && page.bulletPoints.isNotEmpty()) {
                    bulletContainer.visibility = View.VISIBLE
                    bulletContainer.removeAllViews()
                    
                    page.bulletPoints.forEach { point ->
                        val bulletView = createBulletPoint(itemView.context, point)
                        bulletContainer.addView(bulletView)
                    }
                } else {
                    bulletContainer.visibility = View.GONE
                }

                // Handle trivia
                if (page.trivia != null) {
                    cardTrivia.visibility = View.VISIBLE
                    tvTrivia.text = page.trivia
                } else {
                    cardTrivia.visibility = View.GONE
                }
            }

            private fun createBulletPoint(context: android.content.Context, text: String): View {
                val layout = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.START
                    setPadding(0, 8.dpToPx(), 0, 8.dpToPx())
                }

                // Bullet point
                val bullet = View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(12.dpToPx(), 12.dpToPx()).apply {
                        marginEnd = 16.dpToPx()
                        topMargin = 6.dpToPx()
                    }
                    background = ContextCompat.getDrawable(context, R.drawable.legend_dot)
                    background?.setTint(ContextCompat.getColor(context, R.color.primary))
                }

                // Text
                val textView = TextView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                    this.text = text
                    setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                    textSize = 15f
                    setLineSpacing(4.dpToPx().toFloat(), 1f)
                }

                layout.addView(bullet)
                layout.addView(textView)

                return layout
            }

            private fun Int.dpToPx(): Int = (this * itemView.context.resources.displayMetrics.density).toInt()
        }
    }
}


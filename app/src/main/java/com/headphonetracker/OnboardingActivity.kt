package com.headphonetracker

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
import com.google.android.material.tabs.TabLayoutMediator
import com.headphonetracker.data.SettingsRepository
import com.headphonetracker.databinding.ActivityOnboardingBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

data class OnboardingPage(
    val iconRes: Int,
    val title: String,
    val subtitle: String,
    val content: String,
    val bulletPoints: List<String>? = null,
    val trivia: String? = null
)

@AndroidEntryPoint
class OnboardingActivity : AppCompatActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var adapter: OnboardingAdapter

    private val pages = listOf(
        OnboardingPage(
            iconRes = R.drawable.ic_shield,
            title = "Guard Your Hearing",
            subtitle = "1.1 billion people at risk",
            content = "WHO estimates 1.1 billion young people risk permanent hearing loss from unsafe listening. EarGuard watches your daily exposure so you don't have to.",
            trivia = "Noise-induced hearing loss is the only preventable kind — and it's irreversible."
        ),
        OnboardingPage(
            iconRes = R.drawable.ic_warning,
            title = "The 8-Hour Rule",
            subtitle = "Volume × time = damage",
            content = "80 dB for 8 hours is the WHO safe daily limit. Every 3 dB louder cuts that time in half.",
            bulletPoints = listOf(
                "85 dB → safe for only 4 hours/day",
                "94 dB → safe for only 1 hour/day",
                "100 dB → safe for only 15 min/day"
            )
        ),
        OnboardingPage(
            iconRes = R.drawable.ic_headphones,
            title = "Your Ear Health Score",
            subtitle = "Real-time protection",
            content = "EarGuard calculates volume-weighted exposure every session and shows you a live Ear Health score — so you know when to take a break before the damage is done."
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
        settingsRepository.setOnboardingCompleted(true)

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

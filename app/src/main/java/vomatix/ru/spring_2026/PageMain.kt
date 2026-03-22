package vomatix.ru.spring_2026

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment

class PageMain : Fragment(R.layout.fragment_page_main) {

    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView
    private lateinit var tvProgress: TextView
    private lateinit var rating_text: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressBar = view.findViewById(R.id.progressBar)
        tvStatus = view.findViewById(R.id.textView22)
        tvProgress = view.findViewById(R.id.tvProgressText)
        rating_text = view.findViewById(R.id.rating_text)
        val profileClick = view.findViewById<ConstraintLayout>(R.id.profileclick)

        profileClick.setOnClickListener {
            it.pressAnim()
            (activity as? MainActivity)?.selectBottomItem(R.id.page_profile)
        }
        val financeEffect = view.findViewById<ConstraintLayout>(R.id.financeeffect)

        financeEffect.setOnClickListener {
            it.pressAnim()
            (activity as? MainActivity)?.selectBottomItem(R.id.page_effect)
        }
        val smotr = view.findViewById<ConstraintLayout>(R.id.smotr)
        smotr.setOnClickListener {
            it.pressAnim()

            val url = "https://vkvideo.ru/@sber"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)

        }
        val gotoGIGA = view.findViewById<ConstraintLayout>(R.id.gotoGIGA)
        gotoGIGA.setOnClickListener {
            it.pressAnim()
            openSupport()
        }
        val advantage_level = view.findViewById<ConstraintLayout>(R.id.advantage_level)
        advantage_level.setOnClickListener {
            it.pressAnim()
            openAdvantages()
        }
        val finance_perehod = view.findViewById<ConstraintLayout>(R.id.finance_perehod)
        finance_perehod.setOnClickListener {
            it.pressAnim()
            openPeriods()
        }
        animateUI(view)
    }
    fun View.pressAnim() {
        this.animate().scaleX(0.96f).scaleY(0.96f).setDuration(100).withEndAction {
            this.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
        }.start()
    }
    private var isFirstLoad = true
    private fun openSupport() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, support())
            .addToBackStack(null)
            .commit()
    }

    private fun openPeriods() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, RatingMore())
            .addToBackStack(null)
            .commit()
    }
    private fun openAdvantages() {
        startActivity(Intent(context, Level::class.java))
    }

    // 🔥 ВЫЗЫВАЕТСЯ ИЗ MainActivity
    fun updateData(rating: Double, status: String) {

        // статус
        tvStatus.text = "До $status"

        // прогресс (пример: rating 0-100)
        val progress = rating.toInt().coerceIn(0, 100)
        progressBar.progress = progress

        // текст прогресса
        tvProgress.text = "$progress / 100"
        rating_text.text = "$progress баллов"
    }

    override fun onResume() {
        super.onResume()
        (activity as? MainActivity)?.runCalculation()

        if (isFirstLoad) {
            view?.post {
                animateUI(requireView())
            }
            isFirstLoad = false
        }
    }
    private fun animateUI(root: View) {

        val elements = listOf(
            progressBar,
            tvStatus,
            tvProgress,
            rating_text,
            root.findViewById<View>(R.id.profileclick),
            root.findViewById<View>(R.id.financeeffect),
            root.findViewById<View>(R.id.smotr),
            root.findViewById<View>(R.id.gotoGIGA),
            root.findViewById<View>(R.id.advantage_level),
            root.findViewById<View>(R.id.finance_perehod)
        )

        elements.forEach {
            it.clearAnimation() // 🔥 важно
            it.alpha = 0f
            it.translationY = 60f
        }

        elements.forEachIndexed { index, v ->
            v.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay((index * 70).toLong())
                .setDuration(300)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }
    }
}
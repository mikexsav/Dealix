package vomatix.ru.spring_2026

import android.animation.ValueAnimator
import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class RatingMore : Fragment(R.layout.fragment_rating_more) {

    private var scenarioMode = false

    private data class MetricScore(
        val name: String,
        val raw: Double,
        val normalized: Double,
        val weight: Double,
        val points: Int
    )

    private data class RatingResult(
        val totalPoints: Int,
        val currentStatus: String,
        val nextStatus: String,
        val pointsToNext: Int,
        val progressPercent: Int,
        val metrics: List<MetricScore>,
        val synergyBonus: Int,
        val penalty: Int,
        val forecastIncome: String,
        val forecastMortgage: String,
        val formula: String
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnBack = view.findViewById<ImageView>(R.id.btnBack)
        val tvStatusBadge = view.findViewById<TextView>(R.id.tvStatusBadge)
        val ringView = view.findViewById<RatingRingView>(R.id.ringView)
        val tvRingValue = view.findViewById<TextView>(R.id.tvRingValue)
        val tvToNextLevel = view.findViewById<TextView>(R.id.tvToNextLevel)
        val progressNext = view.findViewById<ProgressBar>(R.id.progressNext)

        val tvMetric1Points = view.findViewById<TextView>(R.id.tvMetric1Points)
        val tvMetric1Value = view.findViewById<TextView>(R.id.tvMetric1Value)
        val tvMetric2Points = view.findViewById<TextView>(R.id.tvMetric2Points)
        val tvMetric2Value = view.findViewById<TextView>(R.id.tvMetric2Value)
        val tvMetric3Points = view.findViewById<TextView>(R.id.tvMetric3Points)
        val tvMetric3Value = view.findViewById<TextView>(R.id.tvMetric3Value)

        val tvForecastIncome = view.findViewById<TextView>(R.id.tvForecastIncome)
        val tvForecastMortgage = view.findViewById<TextView>(R.id.tvForecastMortgage)

        val btnCalc = view.findViewById<View>(R.id.btnCalc)
        val btnGrowth = view.findViewById<View>(R.id.btnGrowth)

        val baseResult = calculateRating(scenarioMode = false)

        render(
            result = baseResult,
            ringView = ringView,
            tvStatusBadge = tvStatusBadge,
            tvRingValue = tvRingValue,
            tvToNextLevel = tvToNextLevel,
            progressNext = progressNext,
            tvMetric1Points = tvMetric1Points,
            tvMetric1Value = tvMetric1Value,
            tvMetric2Points = tvMetric2Points,
            tvMetric2Value = tvMetric2Value,
            tvMetric3Points = tvMetric3Points,
            tvMetric3Value = tvMetric3Value,
            tvForecastIncome = tvForecastIncome,
            tvForecastMortgage = tvForecastMortgage,
            animate = false
        )

        btnBack.setOnClickListener {
            btnBack.pressAnim()
            parentFragmentManager.popBackStack()
        }

        btnCalc.setOnClickListener {
            btnCalc.pressAnim()
            showFormulaDialog(baseResult)
        }

        btnGrowth.setOnClickListener {
            btnGrowth.pressAnim()
            scenarioMode = !scenarioMode

            val result = calculateRating(scenarioMode)
            render(
                result = result,
                ringView = ringView,
                tvStatusBadge = tvStatusBadge,
                tvRingValue = tvRingValue,
                tvToNextLevel = tvToNextLevel,
                progressNext = progressNext,
                tvMetric1Points = tvMetric1Points,
                tvMetric1Value = tvMetric1Value,
                tvMetric2Points = tvMetric2Points,
                tvMetric2Value = tvMetric2Value,
                tvMetric3Points = tvMetric3Points,
                tvMetric3Value = tvMetric3Value,
                tvForecastIncome = tvForecastIncome,
                tvForecastMortgage = tvForecastMortgage,
                animate = true
            )

            btnGrowth.animate()
                .scaleX(0.96f)
                .scaleY(0.96f)
                .setDuration(80)
                .withEndAction {
                    btnGrowth.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .setInterpolator(OvershootInterpolator(1.2f))
                        .start()
                }
                .start()
        }

        view.post {
            listOf(
                tvStatusBadge,
                ringView,
                tvRingValue,
                tvToNextLevel,
                progressNext,
                view.findViewById<View>(R.id.mainCard)
            ).forEachIndexed { index, item ->
                item.alpha = 0f
                item.translationY = 40f
                item.postDelayed({
                    item.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(350)
                        .setInterpolator(OvershootInterpolator(1.08f))
                        .start()
                }, (index * 70).toLong())
            }
        }
    }

    private fun calculateRating(scenarioMode: Boolean): RatingResult {
        val baseVolume = RatingMoreDataStore.volumeUnits.toDouble()
        val baseDeals = RatingMoreDataStore.dealsCount.toDouble()
        val baseShare = RatingMoreDataStore.bankSharePercent.toDouble()

        val volume = if (scenarioMode) baseVolume * 1.22 else baseVolume
        val deals = if (scenarioMode) baseDeals * 1.18 else baseDeals
        val share = if (scenarioMode) baseShare + 2.0 else baseShare

        val volumeNorm = saturatingNormalize(volume, target = 18.0)
        val dealsNorm = saturatingNormalize(deals, target = 12.0)
        val shareNorm = saturatingNormalize(share, target = 6.0)

        val rawVolumePoints = (volumeNorm * 40.0).roundToInt()
        val rawDealsPoints = (dealsNorm * 35.0).roundToInt()
        val rawSharePoints = (shareNorm * 25.0).roundToInt()

        val metrics = listOf(
            MetricScore(
                name = "Объем",
                raw = volume,
                normalized = volumeNorm,
                weight = 0.40,
                points = rawVolumePoints
            ),
            MetricScore(
                name = "Сделки",
                raw = deals,
                normalized = dealsNorm,
                weight = 0.35,
                points = rawDealsPoints
            ),
            MetricScore(
                name = "Доля банка",
                raw = share,
                normalized = shareNorm,
                weight = 0.25,
                points = rawSharePoints
            )
        )

        val weightedScore = metrics.sumOf { it.normalized * it.weight }

        val balance = balanceIndex(metrics)
        val synergyBonus = when {
            balance > 0.92 -> 8
            balance > 0.84 -> 6
            balance > 0.76 -> 4
            balance > 0.68 -> 2
            else -> 0
        }

        val dominationPenalty = dominationPenalty(metrics)
        val basePoints = (weightedScore * 100.0).roundToInt()

        val totalPoints = (basePoints + synergyBonus - dominationPenalty)
            .coerceIn(0, 100)

        val currentStatus = when {
            totalPoints < 55 -> "Silver"
            totalPoints < 85 -> "Gold"
            else -> "Black"
        }

        val nextStatus = when (currentStatus) {
            "Silver" -> "Gold"
            "Gold" -> "Black"
            else -> "Black"
        }

        val threshold = when (currentStatus) {
            "Silver" -> 55
            "Gold" -> 85
            else -> 100
        }

        val pointsToNext = max(0, threshold - totalPoints)
        val progressPercent = ((totalPoints * 100.0) / threshold).roundToInt().coerceIn(0, 100)

        val forecastIncome = when (currentStatus) {
            "Silver" -> "180 000 ₽"
            "Gold" -> "320 000 ₽"
            else -> "500 000 ₽"
        }

        val forecastMortgage = when (currentStatus) {
            "Silver" -> "740 000 ₽"
            "Gold" -> "1 050 000 ₽"
            else -> "1 400 000 ₽"
        }

        val formula = """
            Модель рейтинга:
            
            S = 100 × (0.40·f(V) + 0.35·f(D) + 0.25·f(SH))
                + B(balance)
                - P(domination)
            
            где:
            f(x) = 1 - e^(-x / k)
            V = объем
            D = сделки
            SH = доля банка
            
            B(balance) — бонус за сбалансированный профиль
            P(domination) — штраф за перекос одной метрики
        """.trimIndent()

        return RatingResult(
            totalPoints = totalPoints,
            currentStatus = currentStatus,
            nextStatus = nextStatus,
            pointsToNext = pointsToNext,
            progressPercent = progressPercent,
            metrics = metrics,
            synergyBonus = synergyBonus,
            penalty = dominationPenalty,
            forecastIncome = forecastIncome,
            forecastMortgage = forecastMortgage,
            formula = formula
        )
    }

    private fun saturatingNormalize(value: Double, target: Double): Double {
        return 1.0 - exp(-value / target)
    }

    private fun balanceIndex(metrics: List<MetricScore>): Double {
        val values = metrics.map { it.normalized }
        val avg = values.average()
        val variance = values.sumOf { (it - avg) * (it - avg) } / values.size
        val std = kotlin.math.sqrt(variance)
        return (1.0 - std).coerceIn(0.0, 1.0)
    }

    private fun dominationPenalty(metrics: List<MetricScore>): Int {
        val values = metrics.map { it.points }
        val max = values.maxOrNull()?.toDouble() ?: 0.0
        val min = values.minOrNull()?.toDouble() ?: 0.0
        val spread = max - min

        return when {
            spread > 28 -> 6
            spread > 20 -> 4
            spread > 12 -> 2
            else -> 0
        }
    }

    private fun render(
        result: RatingResult,
        ringView: RatingRingView,
        tvStatusBadge: TextView,
        tvRingValue: TextView,
        tvToNextLevel: TextView,
        progressNext: ProgressBar,
        tvMetric1Points: TextView,
        tvMetric1Value: TextView,
        tvMetric2Points: TextView,
        tvMetric2Value: TextView,
        tvMetric3Points: TextView,
        tvMetric3Value: TextView,
        tvForecastIncome: TextView,
        tvForecastMortgage: TextView,
        animate: Boolean
    ) {
        tvStatusBadge.text = result.currentStatus
        tvRingValue.text = "${result.totalPoints} баллов"
        tvToNextLevel.text = "До ${result.nextStatus} осталось ${result.pointsToNext} баллов"
        progressNext.progress = result.progressPercent

        tvMetric1Points.text = "${result.metrics[0].points} баллов"
        tvMetric1Value.text = "${result.metrics[0].raw.roundToInt()} ед."

        tvMetric2Points.text = "${result.metrics[1].points} баллов"
        tvMetric2Value.text = "${result.metrics[1].raw.roundToInt()} ед."

        tvMetric3Points.text = "${result.metrics[2].points} баллов"
        tvMetric3Value.text = "${result.metrics[2].raw.roundToInt()} %"

        tvForecastIncome.text =
            "При переходе на ${result.nextStatus} ваш годовой доход вырастет на ${result.forecastIncome}"

        tvForecastMortgage.text =
            "Экономия на ипотеке: ${result.forecastMortgage}"

        ringView.setData(
            volume = result.metrics[0].points.toFloat(),
            deals = result.metrics[1].points.toFloat(),
            share = result.metrics[2].points.toFloat()
        )

        if (animate) {
            animateNumber(tvRingValue, result.totalPoints, "баллов")
            animateProgress(progressNext, result.progressPercent)
        }
    }

    private fun showFormulaDialog(result: RatingResult) {
        val details = buildString {
            appendLine(result.formula)
            appendLine()
            appendLine("Бонус синергии: +${result.synergyBonus}")
            appendLine("Штраф за перекос: -${result.penalty}")
            appendLine()
            appendLine("Детализация:")
            result.metrics.forEach { metric ->
                appendLine("• ${metric.name}: ${metric.points} баллов")
                appendLine("  Нормализация: ${(metric.normalized * 100).roundToInt()}%")
            }
            appendLine()
            appendLine("Итог: ${result.totalPoints} баллов, статус ${result.currentStatus}")
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Расчет рейтинга")
            .setMessage(details)
            .setPositiveButton("Понятно", null)
            .show()
    }

    private fun animateNumber(textView: TextView, target: Int, suffix: String) {
        val start = textView.text.toString().filter { it.isDigit() }.toIntOrNull() ?: 0
        ValueAnimator.ofInt(start, target).apply {
            duration = 500
            addUpdateListener { animator ->
                val value = animator.animatedValue as Int
                textView.text = "$value $suffix"
            }
            start()
        }
    }

    private fun animateProgress(progressBar: ProgressBar, target: Int) {
        val start = progressBar.progress
        ValueAnimator.ofInt(start, target).apply {
            duration = 500
            addUpdateListener { animator ->
                progressBar.progress = animator.animatedValue as Int
            }
            start()
        }
    }
    fun View.pressAnim() {
        this.animate()
            .scaleX(0.94f)
            .scaleY(0.94f)
            .setDuration(70)
            .withEndAction {
                this.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(120)
                    .setInterpolator(OvershootInterpolator(1.2f))
                    .start()
            }
            .start()
    }
}
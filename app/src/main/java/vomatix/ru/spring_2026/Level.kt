package vomatix.ru.spring_2026

import android.animation.ValueAnimator
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sqrt

data class LevelTier(
    val name: String,
    val minScore: Int,
    val accent: Int,
    val subtitle: String,
    val perks: List<String>,
    val multiplier: Double
)

data class ScenarioResult(
    val score: Int,
    val currentLevel: LevelTier,
    val nextLevel: LevelTier?,
    val pointsToNext: Int,
    val annualBenefit: Long,
    val bonusBlock: Long,
    val mortgageBlock: Long,
    val cashbackBlock: Long,
    val dmsBlock: Long,
    val balanceBonus: Int,
    val penalty: Int,
    val volumePoints: Int,
    val dealsPoints: Int,
    val sharePoints: Int,
    val formulaText: String
)

class Level : AppCompatActivity() {

    private lateinit var root: View
    private lateinit var backBtn: ImageView
    private lateinit var recyclerView: RecyclerView

    private lateinit var dealsValueTv: TextView
    private lateinit var volumeValueTv: TextView
    private lateinit var shareValueTv: TextView
    private lateinit var volumeEdit: EditText
    private lateinit var calcButton: TextView
    private lateinit var titleText: TextView

    private lateinit var dealsPlus: View
    private lateinit var dealsMinus: View
    private lateinit var sharePlus: View
    private lateinit var shareMinus: View

    private var deals = 0
    private var sharePercent = 0
    private var volumeRub = 0L
    private var selfChange = false
    private var hasAnimated = false

    private val levels = listOf(
        LevelTier(
            name = "Silver",
            minScore = 0,
            accent = Color.parseColor("#B5B5B5"),
            subtitle = "Базовый пакет",
            perks = listOf(
                "Стартовые привилегии",
                "Базовая аналитика",
                "Стандартный сценарий эффекта"
            ),
            multiplier = 0.96
        ),
        LevelTier(
            name = "Gold",
            minScore = 55,
            accent = Color.parseColor("#FFB000"),
            subtitle = "Ускоренный пакет",
            perks = listOf(
                "Ускоренный рост эффекта",
                "Приоритетные сценарии",
                "Расширенные бонусы"
            ),
            multiplier = 1.08
        ),
        LevelTier(
            name = "Black",
            minScore = 85,
            accent = Color.parseColor("#19B34B"),
            subtitle = "Максимальный пакет",
            perks = listOf(
                "Максимальный финансовый эффект",
                "Премиальные условия",
                "Сильный мультипликатор модели"
            ),
            multiplier = 1.18
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_level)

        root = findViewById(R.id.main)

        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        bindViews()
        setupRecycler()
        setupControls()
        syncFromStore()
        renderPreview(animated = false)
        animateEntrance()
    }

    override fun onResume() {
        super.onResume()
        if (!hasAnimated) {
            root.post { animateEntrance() }
        }
    }

    private fun bindViews() {
        backBtn = findViewById(R.id.imageView8)
        titleText = findTextViewByText(root, "Сценарный калькулятор") ?: findViewById(R.id.textView14)

        recyclerView = findFirstViewOfType(root, RecyclerView::class.java)
            ?: throw IllegalStateException("RecyclerView not found in activity_level.xml")

        val dealsBlock = findRequiredConstraintLayout(root, R.id.constraintLayout9)
        val volumeBlock = findRequiredConstraintLayout(root, R.id.constraintLayout10)
        val shareBlock = findRequiredConstraintLayout(root, R.id.constraintLayout11)

        dealsValueTv = findRequiredTextView(root, R.id.textView16)
        volumeEdit = findFirstEditText(volumeBlock)
            ?: throw IllegalStateException("EditText not found inside constraintLayout10")
        shareValueTv = findRequiredTextView(root, R.id.textView19)

        dealsPlus = dealsBlock.getChildAt(2)
        dealsMinus = dealsBlock.getChildAt(1)
        sharePlus = shareBlock.getChildAt(2)
        shareMinus = shareBlock.getChildAt(1)

        calcButton = findTextViewByText(root, "Рассчитать")
            ?: throw IllegalStateException("Button 'Рассчитать' not found")

        volumeValueTv = findRequiredTextView(root, R.id.textView18)

        val btnBack = backBtn
        btnBack.setOnClickListener {
            it.pressAnim()
            finish()
        }
    }

    private fun setupRecycler() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = TierAdapter(levels)
        recyclerView.itemAnimator = null
        recyclerView.setHasFixedSize(true)
    }

    private fun setupControls() {
        dealsMinus.setOnClickListener {
            it.pressAnim()
            deals = (deals - 1).coerceAtLeast(0)
            renderPreview(animated = true)
        }

        dealsPlus.setOnClickListener {
            it.pressAnim()
            deals = (deals + 1).coerceAtMost(999)
            renderPreview(animated = true)
        }

        shareMinus.setOnClickListener {
            it.pressAnim()
            sharePercent = (sharePercent - 1).coerceAtLeast(0)
            renderPreview(animated = true)
        }

        sharePlus.setOnClickListener {
            it.pressAnim()
            sharePercent = (sharePercent + 1).coerceAtMost(100)
            renderPreview(animated = true)
        }

        volumeEdit.keyListener = DigitsKeyListener.getInstance("0123456789")
        volumeEdit.setText(formatMoney(volumeRub))
        volumeEdit.setSelection(volumeEdit.text?.length ?: 0)

        volumeEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                if (selfChange) return
                volumeRub = parseMoney(s?.toString().orEmpty())
                renderPreview(animated = false)
            }
        })

        calcButton.setOnClickListener {
            it.pressAnim()
            val result = calculate()
            showResultDialog(result)
        }

        // Если юзер меняет данные — кнопка сразу отражает текущий прогноз
        volumeValueTv.text = "Доля банка, %"
    }

    private fun syncFromStore() {
        val dashboard = DashboardStore.dashboardState
        val levelName = dashboard?.level?.name ?: "Silver"

        when (levelName.lowercase()) {
            "silver" -> {
                deals = 0
                sharePercent = 0
                volumeRub = 0L
            }
            "gold" -> {
                deals = 6
                sharePercent = 4
                volumeRub = 2_500_000L
            }
            "black" -> {
                deals = 11
                sharePercent = 7
                volumeRub = 6_000_000L
            }
        }

        if (dashboard != null) {
            val boost = dashboard.totalPoints.coerceIn(0, 100)
            deals = (deals + boost / 20).coerceAtMost(999)
            sharePercent = (sharePercent + boost / 25).coerceAtMost(100)
            volumeRub += (boost * 40_000L)
        }

        updateUiState()
    }

    private fun updateUiState() {
        dealsValueTv.text = deals.toString()
        shareValueTv.text = sharePercent.toString()
        volumeEdit.setText(formatMoney(volumeRub))
        volumeEdit.setSelection(volumeEdit.text?.length ?: 0)
        renderPreview(animated = false)
    }

    private fun calculate(): ScenarioResult {
        val currentLevelSeed = DashboardStore.dashboardState?.level?.name ?: "Silver"
        val currentTier = levels.firstOrNull { it.name.equals(currentLevelSeed, ignoreCase = true) } ?: levels[0]

        val volumeM = volumeRub / 1_000_000.0
        val dealsScore = saturatingScore(deals.toDouble(), k = 9.0, maxPoints = 40.0)
        val volumeScore = saturatingScore(volumeM, k = 8.0, maxPoints = 35.0)
        val shareScore = saturatingScore(sharePercent.toDouble(), k = 6.0, maxPoints = 25.0)

        val score = (dealsScore + volumeScore + shareScore + synergyBonus(dealsScore, volumeScore, shareScore) - penalty(dealsScore, volumeScore, shareScore))
            .roundToInt()
            .coerceIn(0, 100)

        val currentLevel = levels.lastOrNull { score >= it.minScore } ?: levels.first()
        val nextLevel = levels.firstOrNull { it.minScore > currentLevel.minScore }
        val targetThreshold = nextLevel?.minScore ?: 100
        val pointsToNext = max(0, targetThreshold - score)

        val levelMul = when (currentLevel.name.lowercase()) {
            "silver" -> 0.96
            "gold" -> 1.08
            "black" -> 1.18
            else -> 1.0
        }

        val baseAnnual = 220_000.0
        val momentum = 1.0 + score / 100.0 * 0.42
        val balance = balanceIndex(dealsScore, volumeScore, shareScore)
        val profileBoost = 1.0 + balance * 0.18

        val annualBenefit = (
                baseAnnual *
                        levelMul *
                        momentum *
                        profileBoost *
                        (0.90 + min(0.25, volumeM / 30.0)) *
                        (0.94 + min(0.18, deals / 80.0))
                ).roundToLong()

        val balanceBonus = (balance * 14.0).roundToInt().coerceAtLeast(0)
        val penaltyPoints = penalty(dealsScore, volumeScore, shareScore)

        val bonusBlock = (annualBenefit * 0.36 + score * 910 + deals * 3200).roundToLong()
        val mortgageBlock = (annualBenefit * 0.27 + sharePercent * 4400 + volumeM * 1200).roundToLong()
        val cashbackBlock = (annualBenefit * 0.08 + deals * 1150 + (if (sharePercent >= 5) 2500 else 0)).roundToLong()
        val dmsBlock = max(0L, annualBenefit - bonusBlock - mortgageBlock - cashbackBlock)

        val formulaText = buildString {
            appendLine("Модель сценарного эффекта:")
            appendLine()
            appendLine("S = 100 × (0.40·f(D) + 0.35·f(V) + 0.25·f(B)) + Bsyn - P")
            appendLine("f(x) = 1 - e^(-x / k)")
            appendLine("D — сделки")
            appendLine("V — объем")
            appendLine("B — доля банка")
            appendLine("Bsyn — бонус за баланс")
            appendLine("P — штраф за перекос")
            appendLine()
            appendLine("Текущий уровень: ${currentLevel.name}")
            appendLine("Баланс-профиль: ${"%.2f".format(balance)}")
        }.trimIndent()

        return ScenarioResult(
            score = score,
            currentLevel = currentLevel,
            nextLevel = nextLevel,
            pointsToNext = pointsToNext,
            annualBenefit = annualBenefit,
            bonusBlock = bonusBlock,
            mortgageBlock = mortgageBlock,
            cashbackBlock = cashbackBlock,
            dmsBlock = dmsBlock,
            balanceBonus = balanceBonus,
            penalty = penaltyPoints,
            volumePoints = volumeScore.roundToInt(),
            dealsPoints = dealsScore.roundToInt(),
            sharePoints = shareScore.roundToInt(),
            formulaText = formulaText
        )
    }

    private fun renderPreview(animated: Boolean) {
        val result = calculate()

        dealsValueTv.text = result.score.toString()
        shareValueTv.text = sharePercent.toString()

        calcButton.text = "Рассчитать • ${formatMoney(result.annualBenefit)} ₽"

        volumeValueTv.text = when (result.currentLevel.name.lowercase()) {
            "silver" -> "Доля банка, %"
            "gold" -> "Доля банка, %"
            "black" -> "Доля банка, %"
            else -> "Доля банка, %"
        }

        if (animated) {
            pulse(calcButton)
            pulse(dealsValueTv)
            pulse(shareValueTv)
        }
    }

    private fun showResultDialog(result: ScenarioResult) {
        val next = result.nextLevel?.name ?: "максимума"

        val msg = buildString {
            appendLine("Статус: ${result.currentLevel.name}")
            appendLine("До $next осталось: ${result.pointsToNext} баллов")
            appendLine()
            appendLine("Итоговый рейтинг: ${result.score}/100")
            appendLine("Годовой эффект: ${formatMoney(result.annualBenefit)} ₽")
            appendLine()
            appendLine("Бонусы: ${formatMoney(result.bonusBlock)} ₽")
            appendLine("Ипотека: ${formatMoney(result.mortgageBlock)} ₽")
            appendLine("Кэшбэк: ${formatMoney(result.cashbackBlock)} ₽")
            appendLine("ДМС: ${formatMoney(result.dmsBlock)} ₽")
            appendLine()
            appendLine("Бонус за баланс: +${result.balanceBonus}")
            appendLine("Штраф за перекос: -${result.penalty}")
            appendLine()
            appendLine(result.formulaText)
        }

        AlertDialog.Builder(this)
            .setTitle("Сценарный калькулятор")
            .setMessage(msg)
            .setPositiveButton("Понятно", null)
            .show()
    }

    private fun saturatingScore(value: Double, k: Double, maxPoints: Double): Double {
        return (1.0 - exp(-value / k)) * maxPoints
    }

    private fun synergyBonus(a: Double, b: Double, c: Double): Int {
        val avg = (a + b + c) / 3.0
        val variance = ((a - avg) * (a - avg) + (b - avg) * (b - avg) + (c - avg) * (c - avg)) / 3.0
        val std = sqrt(variance)
        return when {
            std < 2.5 -> 10
            std < 4.0 -> 7
            std < 6.0 -> 4
            std < 8.5 -> 2
            else -> 0
        }
    }

    private fun penalty(a: Double, b: Double, c: Double): Int {
        val values = listOf(a, b, c)
        val spread = (values.maxOrNull() ?: 0.0) - (values.minOrNull() ?: 0.0)
        return when {
            spread > 24 -> 7
            spread > 18 -> 5
            spread > 12 -> 3
            spread > 8 -> 1
            else -> 0
        }
    }

    private fun balanceIndex(a: Double, b: Double, c: Double): Double {
        val avg = (a + b + c) / 3.0
        val variance = ((a - avg) * (a - avg) + (b - avg) * (b - avg) + (c - avg) * (c - avg)) / 3.0
        val std = sqrt(variance)
        return (1.0 - std / 15.0).coerceIn(0.0, 1.0)
    }

    private fun formatMoney(value: Long): String {
        val s = value.toString()
        val sb = StringBuilder(s)
        var i = sb.length - 3
        while (i > 0) {
            sb.insert(i, ' ')
            i -= 3
        }
        return sb.toString()
    }

    private fun parseMoney(input: String): Long {
        val digits = input.filter { it.isDigit() }
        return digits.toLongOrNull() ?: 0L
    }

    private fun animateEntrance() {
        if (hasAnimated) return
        hasAnimated = true

        val items = listOf(
            titleText,
            recyclerView,
            findRequiredTextView(root, R.id.textView14),
            findRequiredTextView(root, R.id.textView15),
            findRequiredConstraintLayout(root, R.id.constraintLayout9),
            findRequiredTextView(root, R.id.textView17),
            findRequiredConstraintLayout(root, R.id.constraintLayout10),
            findRequiredTextView(root, R.id.textView18),
            findRequiredConstraintLayout(root, R.id.constraintLayout11),
            calcButton
        )

        items.forEach {
            it.alpha = 0f
            it.translationY = 42f
            it.scaleX = 0.97f
            it.scaleY = 0.97f
        }

        items.forEachIndexed { index, view ->
            view.postDelayed({
                view.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(320)
                    .setInterpolator(OvershootInterpolator(1.08f))
                    .start()
            }, (index * 75).toLong())
        }
    }

    private fun pulse(view: View) {
        view.animate()
            .scaleX(0.96f)
            .scaleY(0.96f)
            .setDuration(80)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(120)
                    .setInterpolator(OvershootInterpolator(1.15f))
                    .start()
            }
            .start()
    }

    private fun View.pressAnim() {
        animate()
            .scaleX(0.96f)
            .scaleY(0.96f)
            .setDuration(80)
            .withEndAction {
                animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(120)
                    .setInterpolator(OvershootInterpolator(1.15f))
                    .start()
            }
            .start()
    }

    private fun findRequiredTextView(root: View, id: Int): TextView {
        return root.findViewById(id)
            ?: throw IllegalStateException("TextView id=$id not found")
    }

    private fun findRequiredConstraintLayout(root: View, id: Int): ConstraintLayout {
        return root.findViewById(id)
            ?: throw IllegalStateException("ConstraintLayout id=$id not found")
    }

    private fun findTextViewByText(root: View, target: String): TextView? {
        if (root is TextView && root.text?.toString() == target) return root
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                val found = findTextViewByText(root.getChildAt(i), target)
                if (found != null) return found
            }
        }
        return null
    }

    private fun findFirstEditText(root: View): EditText? {
        if (root is EditText) return root
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                val found = findFirstEditText(root.getChildAt(i))
                if (found != null) return found
            }
        }
        return null
    }

    private fun <T : View> findFirstViewOfType(root: View, clazz: Class<T>): T? {
        if (clazz.isInstance(root)) return clazz.cast(root)
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                val found = findFirstViewOfType(root.getChildAt(i), clazz)
                if (found != null) return found
            }
        }
        return null
    }

    private class TierAdapter(
        private val items: List<LevelTier>
    ) : RecyclerView.Adapter<TierAdapter.TierHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TierHolder {
            val ctx = parent.context

            val card = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(ctx, 16), dp(ctx, 16), dp(ctx, 16), dp(ctx, 16))
                background = rounded(Color.parseColor("#1A1A1A"), dp(ctx, 24).toFloat())
                layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(dp(ctx, 10), dp(ctx, 8), dp(ctx, 10), dp(ctx, 8))
                }
            }

            val header = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val badge = TextView(ctx).apply {
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(dp(ctx, 42), dp(ctx, 42))
                background = rounded(Color.parseColor("#2C2C2C"), dp(ctx, 21).toFloat())
                textSize = 14f
            }

            val textBlock = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = dp(ctx, 12)
                }
            }

            val title = TextView(ctx).apply {
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.WHITE)
                textSize = 17f
            }

            val subtitle = TextView(ctx).apply {
                setTextColor(Color.parseColor("#A7A7A7"))
                textSize = 13f
            }

            textBlock.addView(title)
            textBlock.addView(subtitle)

            val minScore = TextView(ctx).apply {
                setTextColor(Color.parseColor("#7E7E7E"))
                setTypeface(typeface, Typeface.BOLD)
                textSize = 12f
            }

            header.addView(badge)
            header.addView(textBlock)
            header.addView(minScore)

            val perks = TextView(ctx).apply {
                setTextColor(Color.WHITE)
                textSize = 13f
                setLineSpacing(0f, 1.12f)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(ctx, 10)
                }
            }

            card.addView(header)
            card.addView(perks)

            return TierHolder(card, badge, title, subtitle, minScore, perks)
        }

        override fun onBindViewHolder(holder: TierHolder, position: Int) {
            holder.bind(items[position])

            holder.itemView.alpha = 0f
            holder.itemView.translationY = 24f
            holder.itemView.postDelayed({
                holder.itemView.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(260)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }, (position * 80).toLong())
        }

        override fun getItemCount(): Int = items.size

        class TierHolder(
            itemView: View,
            private val badge: TextView,
            private val title: TextView,
            private val subtitle: TextView,
            private val minScore: TextView,
            private val perks: TextView
        ) : RecyclerView.ViewHolder(itemView) {

            fun bind(item: LevelTier) {
                badge.text = item.name.firstOrNull()?.uppercase() ?: "?"
                title.text = item.name
                subtitle.text = item.subtitle
                minScore.text = "от ${item.minScore} баллов"

                perks.text = item.perks.joinToString(separator = "\n") { "• $it" }

                (badge.background as? GradientDrawable)?.setColor(item.accent)

                (itemView.background as? GradientDrawable)?.setColor(
                    when (item.name.lowercase()) {
                        "silver" -> Color.parseColor("#171717")
                        "gold" -> Color.parseColor("#1D1910")
                        "black" -> Color.parseColor("#111A13")
                        else -> Color.parseColor("#171717")
                    }
                )
            }
        }

        companion object {
            private fun dp(context: AppCompatActivity, value: Int): Int {
                return (value * context.resources.displayMetrics.density).roundToInt()
            }

            private fun dp(context: android.content.Context, value: Int): Int {
                return (value * context.resources.displayMetrics.density).roundToInt()
            }

            private fun rounded(color: Int, radius: Float): GradientDrawable {
                return GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = radius
                    setColor(color)
                }
            }
        }
    }

    companion object {
        private fun rounded(color: Int, radius: Float): GradientDrawable {
            return GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = radius
                setColor(color)
            }
        }
    }
}

object DashboardStore {

    data class DashboardState(
        val totalPoints: Int,
        val level: LevelInfo
    )

    data class LevelInfo(
        val name: String
    )

    var dashboardState: DashboardState? = DashboardState(
        totalPoints = 42,
        level = LevelInfo("Gold")
    )
}
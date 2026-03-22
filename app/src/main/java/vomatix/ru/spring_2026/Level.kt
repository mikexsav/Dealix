package vomatix.ru.spring_2026

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.InputType
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
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
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

    private lateinit var levelsRecycler: RecyclerView
    private lateinit var levelsAdapter: LevelAdapter

    private lateinit var dealsValueTv: TextView
    private lateinit var shareValueTv: TextView
    private lateinit var volumeEdit: EditText
    private lateinit var calcButton: TextView
    private lateinit var titleText: TextView
    private lateinit var privilegesText: TextView

    private lateinit var dealsPlus: View
    private lateinit var dealsMinus: View
    private lateinit var sharePlus: View
    private lateinit var shareMinus: View
    private lateinit var dealsContainer: ConstraintLayout
    private lateinit var volumeContainer: ConstraintLayout
    private lateinit var shareContainer: ConstraintLayout

    private var deals = 0
    private var sharePercent = 0
    private var volumeRub = 0L
    private var selectedLevelIndex = 0
    private var hasAnimated = false

    private fun showResultDialog(result: ScenarioResult) {

        val dialogLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }

        fun createText(text: String, size: Float = 16f, bold: Boolean = false): TextView {
            return TextView(this).apply {
                this.text = text
                textSize = size
                setTextColor(Color.WHITE)
                if (bold) setTypeface(null, Typeface.BOLD)
                setPadding(0, 10, 0, 10)
            }
        }

        dialogLayout.addView(createText("Ваш результат", 20f, true))

        dialogLayout.addView(createText("Скор: ${result.score}"))
        dialogLayout.addView(createText("Уровень: ${result.currentLevel.name}"))

        if (result.nextLevel != null) {
            dialogLayout.addView(createText("До ${result.nextLevel.name}: ${result.pointsToNext} баллов"))
        }

        dialogLayout.addView(createText(""))

        dialogLayout.addView(createText("Годовая выгода:", 18f, true))
        dialogLayout.addView(createText("${formatMoney(result.annualBenefit)} ₽", 22f, true))

        dialogLayout.addView(createText(""))
        dialogLayout.addView(createText("Разбивка:", 18f, true))
        dialogLayout.addView(createText("Бонусы: ${formatMoney(result.bonusBlock)} ₽"))
        dialogLayout.addView(createText("Ипотека: ${formatMoney(result.mortgageBlock)} ₽"))
        dialogLayout.addView(createText("Кэшбэк: ${formatMoney(result.cashbackBlock)} ₽"))
        dialogLayout.addView(createText("ДМС: ${formatMoney(result.dmsBlock)} ₽"))

        dialogLayout.addView(createText(""))
        dialogLayout.addView(createText("Аналитика:", 18f, true))
        dialogLayout.addView(createText("Баланс бонус: ${result.balanceBonus}"))
        dialogLayout.addView(createText("Штраф: ${result.penalty}"))

        dialogLayout.addView(createText(""))
        dialogLayout.addView(createText("Модель:", 18f, true))
        dialogLayout.addView(createText(result.formulaText))

        val scroll = android.widget.ScrollView(this)
        scroll.addView(dialogLayout)

        AlertDialog.Builder(this)
            .setView(scroll)
            .setPositiveButton("ОК") { d, _ -> d.dismiss() }
            .show()
    }
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
            name = "Platinum",
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
        setupLevelsRecycler()
        setupCalculator()
        resetDefaults()
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
        backBtn = findRequiredImageView(R.id.imageView8)

        titleText = findTextViewByText(root, "Привилегии уровней")
            ?: findRequiredTextView(R.id.textView14)

        levelsRecycler = findFirstRecyclerView(root)
            ?: throw IllegalStateException("RecyclerView not found in activity_level.xml")

        dealsContainer = findRequiredConstraintLayout(R.id.constraintLayout9)
        volumeContainer = findRequiredConstraintLayout(R.id.constraintLayout10)
        shareContainer = findRequiredConstraintLayout(R.id.constraintLayout11)

        dealsValueTv = findRequiredTextView(R.id.textView16)
        shareValueTv = findRequiredTextView(R.id.textView19)

        volumeEdit = findFirstEditText(volumeContainer)
            ?: throw IllegalStateException("EditText not found inside constraintLayout10")

        dealsMinus = dealsContainer.getChildAt(1)
        dealsPlus = dealsContainer.getChildAt(2)

        shareMinus = shareContainer.getChildAt(1)
        sharePlus = shareContainer.getChildAt(2)

        calcButton = findTextViewByText(root, "Рассчитать")
            ?: throw IllegalStateException("Button 'Рассчитать' not found")

        privilegesText = findPrivilegesTextView()
            ?: throw IllegalStateException("Privileges text block not found")

        backBtn.setOnClickListener {
            it.softTapAnim()
            finish()
        }
    }

    private fun setupLevelsRecycler() {
        levelsAdapter = LevelAdapter(
            levels = levels,
            selectedIndex = selectedLevelIndex,
            onSelect = { index ->
                selectedLevelIndex = index
                levelsAdapter.setSelectedIndex(index)
                renderPreview(animated = true)
            }
        )

        levelsRecycler.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        levelsRecycler.adapter = levelsAdapter
        levelsRecycler.itemAnimator = null
        levelsRecycler.setHasFixedSize(true)
        levelsRecycler.isNestedScrollingEnabled = false
        levelsRecycler.overScrollMode = View.OVER_SCROLL_NEVER

        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(levelsRecycler)

        levelsRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val manager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                    val snapView = snapHelper.findSnapView(manager) ?: return
                    val position = manager.getPosition(snapView)
                    if (position != selectedLevelIndex && position in levels.indices) {
                        selectedLevelIndex = position
                        levelsAdapter.setSelectedIndex(position)
                        renderPreview(animated = true)
                    }
                }
            }
        })
    }

    private fun setupCalculator() {
        dealsValueTv.text = "0"
        shareValueTv.text = "0"

        volumeEdit.keyListener = DigitsKeyListener.getInstance("0123456789")
        volumeEdit.inputType = InputType.TYPE_CLASS_NUMBER
        volumeEdit.setText("0")
        volumeEdit.setSelection(1)

        volumeEdit.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && volumeEdit.text.toString() == "0") {
                volumeEdit.setText("")
            }
            if (!hasFocus && volumeEdit.text.isNullOrBlank()) {
                volumeEdit.setText("0")
                volumeRub = 0L
                renderPreview(animated = false)
            }
        }

        volumeEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString().orEmpty()
                if (text.isBlank()) {
                    volumeRub = 0L
                } else {
                    volumeRub = parseMoney(text)
                }
                renderPreview(animated = false)
            }
        })

        dealsMinus.setOnClickListener {
            it.softTapAnim()
            deals = (deals - 1).coerceAtLeast(0)
            renderPreview(animated = true)
        }

        dealsPlus.setOnClickListener {
            it.softTapAnim()
            deals = (deals + 1).coerceAtMost(999)
            renderPreview(animated = true)
        }

        shareMinus.setOnClickListener {
            it.softTapAnim()
            sharePercent = (sharePercent - 1).coerceAtLeast(0)
            renderPreview(animated = true)
        }

        sharePlus.setOnClickListener {
            it.softTapAnim()
            sharePercent = (sharePercent + 1).coerceAtMost(100)
            renderPreview(animated = true)
        }

        volumeContainer.setOnClickListener {
            volumeEdit.requestFocus()
            volumeEdit.setSelection(volumeEdit.text?.length ?: 0)
        }

        calcButton.setOnClickListener {
            it.softTapAnim()

            val result = calculate()
            showResultDialog(result)
        }
    }

    private fun resetDefaults() {
        deals = 0
        sharePercent = 0
        volumeRub = 0L
        selectedLevelIndex = 0

        dealsValueTv.text = "0"
        shareValueTv.text = "0"
        volumeEdit.setText("0")
        volumeEdit.setSelection(1)
        levelsAdapter.setSelectedIndex(0)
        updateTitleAndPrivileges(levels[0])
    }

    private fun updateTitleAndPrivileges(level: LevelTier) {
        titleText.text = "Привилегии уровней • ${level.name}"

        privilegesText.text = buildString {
            appendLine("Текущий уровень: ${level.name}")
            appendLine(level.subtitle)
            appendLine()
            appendLine("Привилегии:")
            level.perks.forEach { appendLine("• $it") }
        }
    }

    private fun renderPreview(animated: Boolean) {
        val result = calculate()
        val selectedLevel = levels[selectedLevelIndex]

        dealsValueTv.text = deals.toString()
        shareValueTv.text = sharePercent.toString()
        updateTitleAndPrivileges(selectedLevel)

        calcButton.text = "Рассчитать • ${formatMoney(result.annualBenefit)} ₽"

        if (animated) {
            pulse(calcButton)
            pulse(dealsValueTv)
            pulse(shareValueTv)
        }
    }

    private fun calculate(): ScenarioResult {
        val currentLevel = levels[selectedLevelIndex]

        val volumeM = volumeRub / 1_000_000.0
        val dealsScore = saturatingScore(deals.toDouble(), k = 9.0, maxPoints = 40.0)
        val volumeScore = saturatingScore(volumeM, k = 8.0, maxPoints = 35.0)
        val shareScore = saturatingScore(sharePercent.toDouble(), k = 6.0, maxPoints = 25.0)

        val rawBase = dealsScore + volumeScore + shareScore
        val syncBonus = synergyBonus(dealsScore, volumeScore, shareScore)
        val penaltyPoints = penalty(dealsScore, volumeScore, shareScore)

        val score = (
                rawBase +
                        syncBonus -
                        penaltyPoints +
                        levelOffset(currentLevel)
                ).roundToInt().coerceIn(0, 100)

        val currentTier = levels.lastOrNull { score >= it.minScore } ?: levels.first()
        val nextTier = levels.firstOrNull { it.minScore > currentTier.minScore }
        val targetThreshold = nextTier?.minScore ?: 100
        val pointsToNext = max(0, targetThreshold - score)

        val baseAnnual = 220_000.0
        val momentum = 1.0 + score / 100.0 * 0.42
        val balance = balanceIndex(dealsScore, volumeScore, shareScore)
        val profileBoost = 1.0 + balance * 0.18
        val levelBoost = currentLevel.multiplier

        val annualBenefit = (
                baseAnnual *
                        levelBoost *
                        momentum *
                        profileBoost *
                        (0.90 + min(0.25, volumeM / 30.0)) *
                        (0.94 + min(0.18, deals / 80.0))
                ).roundToLong()

        val balanceBonus = (balance * 14.0).roundToInt().coerceAtLeast(0)

        val bonusBlock = (annualBenefit * 0.36 + score * 910 + deals * 3200).roundToLong()
        val mortgageBlock = (annualBenefit * 0.27 + sharePercent * 4400 + volumeM * 1200).roundToLong()
        val cashbackBlock = (annualBenefit * 0.08 + deals * 1150 + (if (sharePercent >= 5) 2500 else 0)).roundToLong()
        val dmsBlock = max(0L, annualBenefit - bonusBlock - mortgageBlock - cashbackBlock)

        val formulaText = buildString {
            appendLine("Модель сценарного эффекта:")
            appendLine()
            appendLine("S = 100 × (0.40·f(D) + 0.35·f(V) + 0.25·f(B)) + Bsyn + L - P")
            appendLine("f(x) = 1 - e^(-x / k)")
            appendLine("D — сделки")
            appendLine("V — объем")
            appendLine("B — доля банка")
            appendLine("Bsyn — бонус за баланс")
            appendLine("L — вклад выбранного уровня")
            appendLine("P — штраф за перекос")
            appendLine()
            appendLine("Выбранный уровень: ${currentLevel.name}")
            appendLine("Баланс-профиль: ${"%.2f".format(balance)}")
        }.trimIndent()

        return ScenarioResult(
            score = score,
            currentLevel = currentTier,
            nextLevel = nextTier,
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

    private fun levelOffset(level: LevelTier): Int {
        return when (level.name.lowercase()) {
            "silver" -> 0
            "gold" -> 8
            "Platinum" -> 14
            else -> 4
        }
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
            levelsRecycler,
            privilegesText,
            dealsContainer,
            volumeContainer,
            shareContainer,
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
            .alpha(0.78f)
            .setDuration(60)
            .withEndAction {
                view.animate()
                    .alpha(1f)
                    .setDuration(110)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
            .start()
    }

    private fun View.softTapAnim() {
        animate()
            .scaleX(0.98f)
            .scaleY(0.98f)
            .setDuration(70)
            .withEndAction {
                animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(90)
                    .setInterpolator(OvershootInterpolator(1.08f))
                    .start()
            }
            .start()
    }

    private fun findRequiredImageView(id: Int): ImageView {
        return findViewById(id)
            ?: throw IllegalStateException("ImageView id=$id not found")
    }

    private fun findRequiredTextView(id: Int): TextView {
        return findViewById(id)
            ?: throw IllegalStateException("TextView id=$id not found")
    }

    private fun findRequiredConstraintLayout(id: Int): ConstraintLayout {
        return findViewById(id)
            ?: throw IllegalStateException("ConstraintLayout id=$id not found")
    }

    private fun findFirstRecyclerView(root: View): RecyclerView? {
        if (root is RecyclerView) return root
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                val found = findFirstRecyclerView(root.getChildAt(i))
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

    private fun findPrivilegesTextView(): TextView? {
        val label = findTextViewByText(root, "Список привилегий") ?: return null
        val parent = label.parent as? ViewGroup ?: return null
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            if (child is TextView && child !== label) return child
        }
        return null
    }

    private inner class LevelAdapter(
        private val levels: List<LevelTier>,
        private var selectedIndex: Int,
        private val onSelect: (Int) -> Unit
    ) : RecyclerView.Adapter<LevelAdapter.Holder>() {

        fun setSelectedIndex(index: Int) {
            selectedIndex = index
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = layoutInflater.inflate(R.layout.item_level, parent, false)
            return Holder(view)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.bind(levels[position], position == selectedIndex)

            holder.itemView.alpha = 0f
            holder.itemView.translationY = 18f
            holder.itemView.postDelayed({
                holder.itemView.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(240)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }, (position * 65L).coerceAtMost(240L))

            holder.itemView.setOnClickListener {
                selectedIndex = position
                notifyDataSetChanged()
                onSelect(position)

                it.softTapAnim()
            }
        }

        override fun getItemCount(): Int = levels.size

        inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {

            private val rootCard: ViewGroup = itemView as ViewGroup
            private val container: ViewGroup = rootCard.getChildAt(0) as ViewGroup

            private val imageView: ImageView = findFirstImageView(itemView)
                ?: throw IllegalStateException("ImageView not found in item_level")

            private val texts: List<TextView> = collectTextViews(itemView)

            private val title: TextView = texts.getOrNull(0)
                ?: throw IllegalStateException("Title TextView not found in item_level")

            private val subtitle: TextView = texts.getOrNull(1)
                ?: throw IllegalStateException("Subtitle TextView not found in item_level")

            private val conditionsLabel: TextView = texts.getOrNull(2)
                ?: throw IllegalStateException("Conditions label TextView not found in item_level")

            private val conditionsText: TextView = texts.getOrNull(3)
                ?: throw IllegalStateException("Conditions text TextView not found in item_level")

            private val footer: TextView = texts.getOrNull(4)
                ?: throw IllegalStateException("Footer TextView not found in item_level")

            fun bind(item: LevelTier, selected: Boolean) {
                title.text = item.name
                subtitle.text = if (selected) item.subtitle else if (item.name == "Silver") "Не выполнены условия" else item.subtitle
                conditionsLabel.text = "Условия:"
                conditionsText.text = item.perks.joinToString(separator = "\n") { "• $it" }
                footer.text = "Подробнее"

                imageView.setImageResource(resolveLevelImage(item.name))

                setBackground(container, item, selected)
                applyTextColors(item, selected)
                applySelectionVisuals(selected)
            }

            private fun setBackground(container: ViewGroup, item: LevelTier, selected: Boolean) {
                val color = when (item.name.lowercase()) {
                    "silver" -> if (selected) Color.parseColor("#232323") else Color.parseColor("#171717")
                    "gold" -> if (selected) Color.parseColor("#2A2310") else Color.parseColor("#1D1910")
                    "Platinum" -> if (selected) Color.parseColor("#132018") else Color.parseColor("#111A13")
                    else -> Color.parseColor("#171717")
                }

                (container.background as? GradientDrawable)?.setColor(color)
                    ?: run {
                        container.background = GradientDrawable().apply {
                            cornerRadius = 24f
                            setColor(color)
                        }
                    }
            }

            private fun applyTextColors(item: LevelTier, selected: Boolean) {
                title.setTextColor(if (selected) item.accent else Color.WHITE)
                subtitle.setTextColor(if (selected) Color.WHITE else Color.parseColor("#D0D0D0"))
                conditionsLabel.setTextColor(Color.WHITE)
                conditionsText.setTextColor(Color.WHITE)
                footer.setTextColor(if (selected) item.accent else Color.WHITE)
            }

            private fun applySelectionVisuals(selected: Boolean) {
                rootCard.alpha = if (selected) 1f else 0.92f
                rootCard.scaleX = 1f
                rootCard.scaleY = 1f
                rootCard.animate().cancel()
            }
        }
    }

    private fun resolveLevelImage(name: String): Int {
        val resId = resources.getIdentifier(name.lowercase(), "drawable", packageName)
        return if (resId != 0) resId else R.drawable.ic_launcher_background
    }

    private fun collectTextViews(root: View): List<TextView> {
        val result = mutableListOf<TextView>()
        fun walk(view: View) {
            when (view) {
                is TextView -> result.add(view)
                is ViewGroup -> for (i in 0 until view.childCount) walk(view.getChildAt(i))
            }
        }
        walk(root)
        return result
    }

    private fun findFirstImageView(root: View): ImageView? {
        if (root is ImageView) return root
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                val found = findFirstImageView(root.getChildAt(i))
                if (found != null) return found
            }
        }
        return null
    }

    companion object {
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
    }
}
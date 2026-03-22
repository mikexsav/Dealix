package vomatix.ru.spring_2026

import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sqrt

data class FinancialTaskUi(
    val id: Long,
    val title: String,
    val description: String,
    val dueText: String,
    val isDone: Boolean,
    val isOverdue: Boolean,
    val impactPercent: Int
)

data class FinancialCard(
    val title: String,
    val amount: Long,
    val accent: Int,
    val subtitle: String
)

data class FinancialEffectState(
    val totalBenefit: Long,
    val bonusBlock: Long,
    val mortgageBlock: Long,
    val cashbackBlock: Long,
    val dmsBlock: Long,
    val levelName: String,
    val ratingPoints: Int,
    val levelBoost: Double,
    val taskCompletionRate: Int,
    val overdueCount: Int,
    val balanceBonus: Int,
    val penalty: Int,
    val formulaText: String,
    val cards: List<FinancialCard>,
    val tasks: List<FinancialTaskUi>
)

object EffectSnapshot {
    @Volatile var levelName: String = "Silver"
    @Volatile var ratingPoints: Int = 43
    @Volatile var tasks: List<FinancialTaskUi> = demoTasks()

    fun submit(levelName: String, ratingPoints: Int, tasks: List<FinancialTaskUi>) {
        this.levelName = levelName
        this.ratingPoints = ratingPoints
        this.tasks = tasks
    }

    private fun demoTasks(): List<FinancialTaskUi> {
        return listOf(
            FinancialTaskUi(
                id = 1,
                title = "Закрыть 5 заявок",
                description = "Довести текущие сделки до оплаты и не потерять воронку.",
                dueText = "до 31 марта",
                isDone = false,
                isOverdue = false,
                impactPercent = 18
            ),
            FinancialTaskUi(
                id = 2,
                title = "Поднять долю банка",
                description = "Сдвинуть структуру предложений в сторону банка Сбера.",
                dueText = "до 12 марта",
                isDone = true,
                isOverdue = false,
                impactPercent = 22
            ),
            FinancialTaskUi(
                id = 3,
                title = "Обработать просрочки",
                description = "Закрыть зависшие лиды и убрать просадки по KPI.",
                dueText = "вчера",
                isDone = false,
                isOverdue = true,
                impactPercent = 14
            ),
            FinancialTaskUi(
                id = 4,
                title = "Добить план по объёму",
                description = "Увеличить количество финансовых продуктов в месяце.",
                dueText = "до 28 марта",
                isDone = false,
                isOverdue = false,
                impactPercent = 20
            )
        )
    }
}

class page_effect : Fragment(R.layout.fragment_page_effect) {

    private lateinit var titleText: TextView
    private lateinit var subtitleText: TextView
    private lateinit var totalText: TextView
    private lateinit var bonusText: TextView
    private lateinit var mortgageText: TextView
    private lateinit var cashbackText: TextView
    private lateinit var dmsText: TextView
    private lateinit var tasksRecycler: RecyclerView
    private lateinit var tasksLabel: TextView
    private lateinit var tasksSummary: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var topCard: ConstraintLayout
    private lateinit var breakdownCard: ConstraintLayout
    private lateinit var tasksCard: RecyclerView
    private lateinit var formulaCard: ConstraintLayout
    private lateinit var adapter: TaskAdapter

    private var hasAnimated = false
    private var currentState: FinancialEffectState? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)
        setupRecycler()
        setupClicks()
        renderState(buildState())
        animateScreen(view)
    }

    override fun onResume() {
        super.onResume()
        val newState = buildState()
        renderState(newState)
        if (!hasAnimated) {
            view?.post { animateScreen(requireView()) }
        }
        (activity as? MainActivity)?.setBottomNavVisible(true)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && view != null) {
            view?.post {
                renderState(buildState())
                animateScreen(requireView())
            }
        }
    }

    private fun bindViews(root: View) {
        titleText = root.findViewById(R.id.textView)
        subtitleText = root.findViewById(R.id.textView2)
        totalText = root.findViewById(R.id.ammountOfLever)
        bonusText = root.findViewById(R.id.v_gifts)
        mortgageText = root.findViewById(R.id.v_economica)
        cashbackText = root.findViewById(R.id.v_cashback)
        dmsText = root.findViewById(R.id.v_cashback)
        tasksRecycler = root.findViewById(R.id.recycler_view)
        tasksLabel = root.findViewById(R.id.textView4)
        tasksSummary = root.findViewById(R.id.textView3)

        progressBar = ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
        }

        topCard = root.findViewById(R.id.constraintLayout)
        breakdownCard = root.findViewById(R.id.constraintLayout2)
        tasksCard = root.findViewById(R.id.recycler_view)
        formulaCard = root.findViewById(R.id.constraintLayout2)
    }

    private fun setupRecycler() {
        adapter = TaskAdapter()
        tasksRecycler.layoutManager = LinearLayoutManager(requireContext())
        tasksRecycler.adapter = adapter
        tasksRecycler.itemAnimator = null
        tasksRecycler.setHasFixedSize(false)
    }

    private fun setupClicks() {
        topCard.setOnClickListener {
            it.pressAnim()
            currentState?.let { state -> showFormulaDialog(state) }
        }

        breakdownCard.setOnClickListener {
            it.pressAnim()
            currentState?.let { state -> showFormulaDialog(state) }
        }

        tasksLabel.setOnClickListener {
            tasksLabel.pressAnim()
            showTaskDigestDialog()
        }
    }

    private fun buildState(): FinancialEffectState {
        val levelName = EffectSnapshot.levelName
        val rating = EffectSnapshot.ratingPoints
        val tasks = if (EffectSnapshot.tasks.isNotEmpty()) EffectSnapshot.tasks else FinancialTaskUiFallback.demo()

        return buildFinancialModel(levelName, rating, tasks)
    }

    private fun buildFinancialModel(
        levelName: String,
        ratingPoints: Int,
        tasks: List<FinancialTaskUi>
    ): FinancialEffectState {

        val levelMultiplier = when (levelName.lowercase()) {
            "silver" -> 0.92
            "gold" -> 1.08
            "black" -> 1.22
            else -> 1.0
        }

        val ratingMomentum = 1.0 + (ratingPoints.coerceIn(0, 100) / 100.0) * 0.22

        val completed = tasks.count { it.isDone }
        val overdue = tasks.count { it.isOverdue }
        val totalTasks = tasks.size.coerceAtLeast(1)

        val completionRate = ((completed * 100.0) / totalTasks).roundToInt().coerceIn(0, 100)

        val taskCompletionFactor = 0.74 + 0.36 * (1.0 - exp(-(completed.toDouble() / 4.0)))
        val overduePenaltyFactor = 1.0 - min(0.22, overdue * 0.05)
        val balanceBoost = balanceBonus(
            ratingPoints = ratingPoints,
            taskCompletion = completionRate,
            overdue = overdue
        )

        val baseAnnual = 186_000.0
        val annualBenefit = (
                baseAnnual *
                        levelMultiplier *
                        ratingMomentum *
                        taskCompletionFactor *
                        overduePenaltyFactor +
                        balanceBoost * 2100.0
                ).roundToInt().coerceAtLeast(0)

        val bonusBlock = (
                annualBenefit * 0.38 +
                        completed * 7800 +
                        max(0, ratingPoints - 35) * 820
                ).roundToLong()

        val mortgageBlock = (
                annualBenefit * 0.29 +
                        ratingPoints * 520 +
                        completionRate * 430
                ).roundToLong()

        val cashbackBlock = (
                annualBenefit * 0.07 +
                        completed * 1600 +
                        (if (overdue == 0) 4500 else 0)
                ).roundToLong()

        val dmsBlock = (
                annualBenefit -
                        bonusBlock -
                        mortgageBlock -
                        cashbackBlock
                ).coerceAtLeast(0)

        val formulaText = buildString {
            appendLine("Модель эффекта:")
            appendLine()
            appendLine("E = A × L × M × T × O + B")
            appendLine("A — базовая годовая выгода")
            appendLine("L — коэффициент уровня")
            appendLine("M — импульс рейтинга")
            appendLine("T — фактор выполнения задач")
            appendLine("O — штраф за просрочки")
            appendLine("B — бонус за сбалансированность")
            appendLine()
            appendLine("Текущий уровень: $levelName")
            appendLine("Рейтинг: $ratingPoints")
            appendLine("Выполнение задач: $completionRate%")
            appendLine("Просрочки: $overdue")
        }.trimIndent()

        val cards = listOf(
            FinancialCard(
                title = "Бонусы",
                amount = bonusBlock,
                accent = Color.parseColor("#19B34B"),
                subtitle = "От уровня, рейтинга и закрытых задач"
            ),
            FinancialCard(
                title = "Экономия по ипотеке",
                amount = mortgageBlock,
                accent = Color.parseColor("#00A3FF"),
                subtitle = "Чем выше уровень, тем сильнее эффект"
            ),
            FinancialCard(
                title = "Кэшбэк",
                amount = cashbackBlock,
                accent = Color.parseColor("#FFB000"),
                subtitle = "Суммируется от активности и качества работы"
            ),
            FinancialCard(
                title = "Экономия по ДМС",
                amount = dmsBlock,
                accent = Color.parseColor("#A855F7"),
                subtitle = "Дополнительный скрытый эффект пакета"
            )
        )

        return FinancialEffectState(
            totalBenefit = annualBenefit.toLong(),
            bonusBlock = bonusBlock,
            mortgageBlock = mortgageBlock,
            cashbackBlock = cashbackBlock,
            dmsBlock = dmsBlock,
            levelName = levelName,
            ratingPoints = ratingPoints,
            levelBoost = levelMultiplier,
            taskCompletionRate = completionRate,
            overdueCount = overdue,
            balanceBonus = balanceBoost,
            penalty = overdue * 7,
            formulaText = formulaText,
            cards = cards,
            tasks = tasks
        )
    }

    private fun balanceBonus(
        ratingPoints: Int,
        taskCompletion: Int,
        overdue: Int
    ): Int {
        val ratingNorm = ratingPoints / 100.0
        val tasksNorm = taskCompletion / 100.0
        val balance = 1.0 - abs(ratingNorm - tasksNorm)
        val penalty = overdue * 1.5
        return ((balance * 14.0) - penalty).roundToInt().coerceAtLeast(0)
    }

    private fun renderState(state: FinancialEffectState) {
        currentState = state

        animateMoney(totalText, state.totalBenefit, " ₽")
        animateMoney(bonusText, state.bonusBlock, " ₽")
        animateMoney(mortgageText, state.mortgageBlock, " ₽")
        animateMoney(cashbackText, state.cashbackBlock, " ₽")

        subtitleText.text = buildSubtitle(state)
        tasksSummary.text = buildTasksSummary(state)

        adapter.submit(
            state.tasks.map {
                it.copy(
                    impactPercent = computeTaskImpact(it, state)
                )
            }
        )
    }

    private fun buildSubtitle(state: FinancialEffectState): CharSequence {
        return SpannableStringBuilder().apply {
            append("Ежегодная выгода, которую вы получаете\nблагодаря работе со Сбером")
            append("\n")
            append("Уровень: ")
            val start = length
            append(state.levelName)
            setSpan(StyleSpan(Typeface.BOLD), start, length, 0)
        }
    }

    private fun buildTasksSummary(state: FinancialEffectState): String {
        val completed = state.tasks.count { it.isDone }
        val overdue = state.tasks.count { it.isOverdue }
        return "Задачи месяца • Выполнено: $completed • Просрочено: $overdue • Выполнение: ${state.taskCompletionRate}%"
    }

    private fun computeTaskImpact(task: FinancialTaskUi, state: FinancialEffectState): Int {
        val base = when {
            task.isDone -> 100
            task.isOverdue -> 38
            else -> 72
        }

        val levelBoost = when (state.levelName.lowercase()) {
            "silver" -> 0
            "gold" -> 4
            "black" -> 8
            else -> 2
        }

        val ratingBoost = (state.ratingPoints / 20).coerceIn(0, 5)
        return (base + levelBoost + ratingBoost + task.impactPercent / 5).coerceAtMost(100)
    }

    private fun animateScreen(root: View) {
        if (hasAnimated) return
        hasAnimated = true

        val elements = listOf(
            titleText,
            subtitleText,
            topCard,
            tasksSummary,
            breakdownCard,
            tasksLabel,
            tasksRecycler
        )

        elements.forEach {
            it.clearAnimation()
            it.alpha = 0f
            it.translationY = 42f
            it.scaleX = 0.97f
            it.scaleY = 0.97f
        }

        elements.forEachIndexed { index, v ->
            v.postDelayed({
                v.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(360)
                    .setInterpolator(OvershootInterpolator(1.08f))
                    .start()
            }, (index * 75).toLong())
        }

        val cards = listOf(
            root.findViewById<View>(R.id.constr1),
            root.findViewById<View>(R.id.constr2),
            root.findViewById<View>(R.id.constr3),
            root.findViewById<View>(R.id.constr4)
        )

        cards.forEach {
            it.alpha = 0f
            it.translationX = 24f
        }

        cards.forEachIndexed { index, card ->
            card.postDelayed({
                card.animate()
                    .alpha(1f)
                    .translationX(0f)
                    .setDuration(280)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }, 120L + index * 90L)
        }

        val moneyViews = listOf(totalText, bonusText, mortgageText, cashbackText)
        moneyViews.forEach {
            it.alpha = 0f
            it.translationY = 18f
        }

        moneyViews.forEachIndexed { index, tv ->
            tv.postDelayed({
                tv.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(260)
                    .setInterpolator(OvershootInterpolator(1.05f))
                    .start()
            }, 200L + index * 80L)
        }
    }

    private fun animateMoney(view: TextView, target: Long, suffix: String) {
        val current = view.text.toString()
            .filter { it.isDigit() }
            .toLongOrNull() ?: 0L

        ValueAnimator.ofFloat(current.toFloat(), target.toFloat()).apply {
            duration = 700
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val value = (animator.animatedValue as Float).toLong()
                view.text = formatMoney(value) + suffix
            }
            start()
        }
    }

    private fun formatMoney(value: Long): String {
        val raw = value.toString()
        val sb = StringBuilder(raw)
        var i = sb.length - 3
        while (i > 0) {
            sb.insert(i, ' ')
            i -= 3
        }
        return sb.toString()
    }

    private fun showFormulaDialog(state: FinancialEffectState) {
        val msg = buildString {
            appendLine(state.formulaText)
            appendLine()
            appendLine("Дополнительные эффекты:")
            appendLine("• Бонусный блок: ${formatMoney(state.bonusBlock)} ₽")
            appendLine("• Ипотека: ${formatMoney(state.mortgageBlock)} ₽")
            appendLine("• Кэшбэк: ${formatMoney(state.cashbackBlock)} ₽")
            appendLine("• ДМС: ${formatMoney(state.dmsBlock)} ₽")
            appendLine()
            appendLine("Баланс-бонус: +${state.balanceBonus}")
            appendLine("Штраф: -${state.penalty}")
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Математическая модель")
            .setMessage(msg)
            .setPositiveButton("Понятно", null)
            .show()
    }

    private fun showTaskDigestDialog() {
        val state = currentState ?: buildState()
        val top = state.tasks.joinToString("\n\n") { task ->
            val status = when {
                task.isDone -> "Выполнено"
                task.isOverdue -> "Просрочено"
                else -> "В работе"
            }
            "• ${task.title}\n  $status • ${task.dueText}\n  Вклад: ${task.impactPercent}%"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Задачи месяца")
            .setMessage(top)
            .setPositiveButton("Ок", null)
            .show()
    }

    private fun View.pressAnim() {
        animate()
            .scaleX(0.96f)
            .scaleY(0.96f)
            .setDuration(90)
            .withEndAction {
                animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(110)
                    .setInterpolator(OvershootInterpolator(1.18f))
                    .start()
            }
            .start()
    }

    private inner class TaskAdapter : RecyclerView.Adapter<TaskAdapter.TaskHolder>() {
        private val items = mutableListOf<FinancialTaskUi>()

        fun submit(list: List<FinancialTaskUi>) {
            items.clear()
            items.addAll(list)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskHolder {
            val ctx = parent.context

            val root = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(ctx, 16), dp(ctx, 14), dp(ctx, 16), dp(ctx, 14))
                background = roundedBg(Color.parseColor("#1A1A1A"), dp(ctx, 24).toFloat())
                layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(dp(ctx, 10), dp(ctx, 8), dp(ctx, 10), dp(ctx, 8))
                }
                alpha = 0f
                translationY = 24f
            }

            val header = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val badge = TextView(ctx).apply {
                textSize = 14f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(dp(ctx, 34), dp(ctx, 34))
                background = roundedBg(Color.parseColor("#19B34B"), dp(ctx, 17).toFloat())
            }

            val title = TextView(ctx).apply {
                textSize = 16f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = dp(ctx, 12)
                    marginEnd = dp(ctx, 12)
                }
            }

            val impact = TextView(ctx).apply {
                textSize = 12f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.WHITE)
                setPadding(dp(ctx, 10), dp(ctx, 6), dp(ctx, 10), dp(ctx, 6))
                background = roundedBg(Color.parseColor("#2C2C2C"), dp(ctx, 18).toFloat())
            }

            header.addView(badge)
            header.addView(title)
            header.addView(impact)

            val desc = TextView(ctx).apply {
                textSize = 13f
                setTextColor(Color.parseColor("#B8B8B8"))
                setLineSpacing(0f, 1.08f)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(ctx, 10)
                }
            }

            val footer = TextView(ctx).apply {
                textSize = 12f
                setTextColor(Color.parseColor("#8F8F8F"))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(ctx, 8)
                }
            }

            root.addView(header)
            root.addView(desc)
            root.addView(footer)

            return TaskHolder(root, badge, title, impact, desc, footer)
        }

        override fun onBindViewHolder(holder: TaskHolder, position: Int) {
            val item = items[position]
            holder.bind(item)

            holder.itemView.alpha = 0f
            holder.itemView.translationY = 24f
            holder.itemView.postDelayed({
                holder.itemView.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(260)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }, (position * 55).toLong())
        }

        override fun getItemCount(): Int = items.size

        inner class TaskHolder(
            itemView: View,
            private val badge: TextView,
            private val title: TextView,
            private val impact: TextView,
            private val desc: TextView,
            private val footer: TextView
        ) : RecyclerView.ViewHolder(itemView) {

            fun bind(item: FinancialTaskUi) {
                title.text = item.title
                desc.text = item.description
                footer.text = item.dueText

                impact.text = when {
                    item.isDone -> "+${item.impactPercent}%"
                    item.isOverdue -> "-${item.impactPercent}%"
                    else -> "+${item.impactPercent}%"
                }

                badge.text = item.title.firstOrNull()?.uppercase() ?: "?"
                (badge.background as? GradientDrawable)?.setColor(
                    when {
                        item.isDone -> Color.parseColor("#19B34B")
                        item.isOverdue -> Color.parseColor("#D63A3A")
                        else -> Color.parseColor("#00A3FF")
                    }
                )

                (itemView.background as? GradientDrawable)?.setColor(
                    when {
                        item.isDone -> Color.parseColor("#162319")
                        item.isOverdue -> Color.parseColor("#2A1818")
                        else -> Color.parseColor("#14141A")
                    }
                )

                footer.setTextColor(
                    when {
                        item.isOverdue -> Color.parseColor("#FF8686")
                        item.isDone -> Color.parseColor("#84F0A4")
                        else -> Color.parseColor("#8F8F8F")
                    }
                )
            }
        }
    }

    private fun roundedBg(color: Int, radius: Float): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(color)
        }
    }

    private fun dp(context: Context, value: Int): Int {
        return (value * context.resources.displayMetrics.density).roundToInt()
    }

    private object FinancialTaskUiFallback {
        fun demo(): List<FinancialTaskUi> {
            return listOf(
                FinancialTaskUi(
                    id = 101,
                    title = "Закрыть активные заявки",
                    description = "Довести ключевые сделки до результата и поднять темп закрытия.",
                    dueText = "до 31 марта",
                    isDone = false,
                    isOverdue = false,
                    impactPercent = 18
                ),
                FinancialTaskUi(
                    id = 102,
                    title = "Поднять долю банка",
                    description = "Перевести больше предложений в сторону Сбера.",
                    dueText = "до 20 марта",
                    isDone = true,
                    isOverdue = false,
                    impactPercent = 22
                ),
                FinancialTaskUi(
                    id = 103,
                    title = "Разгрузить просрочки",
                    description = "Снять зависшие элементы, чтобы модель не теряла эффективность.",
                    dueText = "вчера",
                    isDone = false,
                    isOverdue = true,
                    impactPercent = 14
                ),
                FinancialTaskUi(
                    id = 104,
                    title = "Добрать месячный объём",
                    description = "Увеличить число оформлений и восстановить план.",
                    dueText = "до 28 марта",
                    isDone = false,
                    isOverdue = false,
                    impactPercent = 20
                )
            )
        }
    }
}
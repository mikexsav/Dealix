package vomatix.ru.spring_2026

import android.app.AlertDialog
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

class PageNews : Fragment(R.layout.fragment_page_news) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NewsAdapter

    private var hasAnimatedOnce = false
    private var currentFeed: List<NewsItem> = emptyList()

    private val rawNews = listOf(
        NewsItem(
            id = 1,
            source = "Сбер Дилер",
            dateLabel = "Сегодня",
            minutesAgo = 18,
            category = "Объявление",
            title = "Обновлён месячный план KPI",
            description = "В системе появились новые рекомендации по улучшению объёма, сделок и доли банка.",
            fullText = "Обновлён месячный план KPI. Теперь система учитывает не только объём и сделки, но и баланс показателей, чтобы рост был устойчивым. Проверьте текущий статус и завершите задачи месяца.",
            importance = 9,
            freshness = 10,
            novelty = 8,
            imageRes = R.drawable.cashback
        ),
        NewsItem(
            id = 2,
            source = "Аналитика Сбера",
            dateLabel = "Вчера",
            minutesAgo = 930,
            category = "Аналитика",
            title = "Рост эффективности у лучших сотрудников",
            description = "У сотрудников с высокой долей банка и стабильным выполнением задач рейтинг растёт заметно быстрее.",
            fullText = "По внутренней аналитике, сотрудники с ровным профилем показателей получают больше бонусов. Сильнее всего на результат влияет сочетание объёма, количества сделок и дисциплины по задачам.",
            importance = 8,
            freshness = 6,
            novelty = 7,
            imageRes = R.drawable.n1
        ),
        NewsItem(
            id = 3,
            source = "Платформа",
            dateLabel = "2 дня назад",
            minutesAgo = 2800,
            category = "Новость",
            title = "Добавлены новые сценарии роста",
            description = "Теперь модель показывает, как меняется рейтинг при росте объёма, сделок и доли банка.",
            fullText = "Добавлены новые сценарии роста. В разделе финансового эффекта можно смоделировать, как меняются прогноз дохода, ипотека, кэшбэк и ДМС при улучшении отдельных метрик.",
            importance = 7,
            freshness = 4,
            novelty = 9,
            imageRes = R.drawable.gift
        ),
        NewsItem(
            id = 4,
            source = "Команда продукта",
            dateLabel = "На неделе",
            minutesAgo = 8200,
            category = "Релиз",
            title = "Улучшена лента новостей",
            description = "Карточки стали плавнее, а подача — более структурной и понятной.",
            fullText = "Мы обновили ленту новостей: улучшили плавность анимаций, сделали карточки более заметными и добавили детальное окно с описанием новости по клику.",
            importance = 6,
            freshness = 3,
            novelty = 6,
            imageRes = R.drawable.gift
        ),
        NewsItem(
            id = 5,
            source = "Сбер Дилер",
            dateLabel = "Сегодня",
            minutesAgo = 42,
            category = "Совет",
            title = "Как быстрее поднять рейтинг",
            description = "Держите объём, сделки и долю банка в балансе — система любит ровный профиль.",
            fullText = "Чтобы ускорить рост рейтинга, не стоит усиливать только одну метрику. Система лучше реагирует на сбалансированный профиль: объём, сделки и доля банка должны расти вместе.",
            importance = 10,
            freshness = 10,
            novelty = 6,
            imageRes = R.drawable.gift
        ),
        NewsItem(
            id = 6,
            source = "Служба поддержки",
            dateLabel = "Сегодня",
            minutesAgo = 120,
            category = "Подсказка",
            title = "Задачи месяца теперь влияют сильнее",
            description = "Выполнение месячных задач повышает итоговый эффект и усиливает сценарий роста.",
            fullText = "Теперь выполнение задач месяца имеет повышенный вклад в общую модель. Чем лучше закрываются задачи и меньше просрочек, тем выше итоговый эффект и быстрее растёт прогноз.",
            importance = 9,
            freshness = 8,
            novelty = 7,
            imageRes = R.drawable.cashback
        )
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerNews)

        val title = view.findViewById<TextView>(R.id.textView)
        title.text = "Новости"

        currentFeed = NewsFeedEngine.rank(rawNews, FeedContext())
        adapter = NewsAdapter(currentFeed) { item ->
            showNewsDialog(item)
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        recyclerView.itemAnimator = null
        recyclerView.setHasFixedSize(true)
        recyclerView.clipToPadding = false
        recyclerView.setPadding(0, 0, 0, dp(20))

        animateScreen(view)
    }

    override fun onResume() {
        super.onResume()
        if (!hasAnimatedOnce) {
            view?.post { animateScreen(requireView()) }
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && view != null) {
            view?.post { animateScreen(requireView()) }
        }
    }

    private fun animateScreen(root: View) {
        if (hasAnimatedOnce) return
        hasAnimatedOnce = true

        val header = root.findViewById<View>(R.id.textView)
        val list = recyclerView

        header.alpha = 0f
        header.translationY = 36f
        header.rotationX = 12f

        list.alpha = 0f
        list.translationY = 48f
        list.scaleX = 0.98f
        list.scaleY = 0.98f

        header.animate()
            .alpha(1f)
            .translationY(0f)
            .rotationX(0f)
            .setDuration(380)
            .setInterpolator(OvershootInterpolator(1.05f))
            .start()

        list.postDelayed({
            list.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(420)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }, 120)

        recyclerView.post {
            recyclerView.smoothScrollToPosition(0)
        }
    }

    private fun showNewsDialog(item: NewsItem) {
        val digest = NewsFeedEngine.buildDigest(item, currentFeed.size)

        val message = buildString {
            appendLine(item.source)
            appendLine(item.dateLabel)
            appendLine()
            appendLine(item.fullText)
            appendLine()
            appendLine(digest)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(item.title)
            .setMessage(message)
            .setPositiveButton("Понятно", null)
            .show()
    }

    data class NewsItem(
        val id: Long,
        val source: String,
        val dateLabel: String,
        val minutesAgo: Int,
        val category: String,
        val title: String,
        val description: String,
        val fullText: String,
        val importance: Int,
        val freshness: Int,
        val novelty: Int,
        val imageRes: Int
    )

    data class FeedContext(
        val userLevel: String = "Silver",
        val rating: Int = 43,
        val tasksCompletion: Int = 72,
        val taskOverdue: Int = 1
    )

    data class RankedNews(
        val item: NewsItem,
        val score: Int,
        val reason: String
    )

    object NewsFeedEngine {

        fun rank(items: List<NewsItem>, context: FeedContext): List<NewsItem> {
            return items
                .map { item ->
                    val score = computeScore(item, context)
                    val reason = explainScore(item, context, score)
                    RankedNews(item, score, reason)
                }
                .sortedWith(compareByDescending<RankedNews> { it.score }.thenBy { it.item.minutesAgo })
                .map { it.item }
        }

        fun computeScore(item: NewsItem, context: FeedContext): Int {
            val freshnessCurve = saturating(item.freshness.toDouble(), 10.0)
            val importanceCurve = saturating(item.importance.toDouble(), 8.0)
            val noveltyCurve = saturating(item.novelty.toDouble(), 7.0)

            val levelBoost = when (context.userLevel.lowercase()) {
                "silver" -> 0.96
                "gold" -> 1.04
                "black" -> 1.10
                else -> 1.0
            }

            val ratingBoost = 1.0 + (context.rating.coerceIn(0, 100) / 100.0) * 0.10
            val taskBoost = 1.0 + (context.tasksCompletion.coerceIn(0, 100) / 100.0) * 0.08
            val overduePenalty = 1.0 - min(0.10, context.taskOverdue * 0.03)

            val timeDecay = decayByMinutes(item.minutesAgo)

            val composite = (
                    freshnessCurve * 0.34 +
                            importanceCurve * 0.38 +
                            noveltyCurve * 0.28
                    ) * 100.0

            return (composite * levelBoost * ratingBoost * taskBoost * overduePenalty * timeDecay)
                .roundToInt()
                .coerceIn(0, 100)
        }

        private fun explainScore(item: NewsItem, context: FeedContext, score: Int): String {
            val recency = when {
                item.minutesAgo < 60 -> "очень свежая"
                item.minutesAgo < 1440 -> "свежая"
                else -> "архивная"
            }

            val levelNote = when (context.userLevel.lowercase()) {
                "silver" -> "под базовый профиль"
                "gold" -> "под усиленный профиль"
                "black" -> "под премиальный профиль"
                else -> "под текущий профиль"
            }

            return "Рейтинг $score/100 • $recency новость • $levelNote"
        }

        fun buildDigest(item: NewsItem, total: Int): String {
            val age = when {
                item.minutesAgo < 60 -> "${item.minutesAgo} мин назад"
                item.minutesAgo < 1440 -> "${item.minutesAgo / 60} ч назад"
                else -> "${item.minutesAgo / 1440} дн назад"
            }

            return buildString {
                appendLine("Источник: ${item.source}")
                appendLine("Возраст: $age")
                appendLine("Категория: ${item.category}")
                appendLine("Лента: $total материалов")
                appendLine("Сигнал важности: ${item.importance}/10")
            }
        }

        private fun saturating(value: Double, k: Double): Double {
            return 1.0 - kotlin.math.exp(-value / k)
        }

        private fun decayByMinutes(minutesAgo: Int): Double {
            return when {
                minutesAgo < 60 -> 1.08
                minutesAgo < 240 -> 1.02
                minutesAgo < 1440 -> 0.97
                else -> 0.90
            }
        }
    }

    private class NewsAdapter(
        private val items: List<NewsItem>,
        private val onClick: (NewsItem) -> Unit
    ) : RecyclerView.Adapter<NewsAdapter.NewsHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_news, parent, false)
            return NewsHolder(view)
        }

        override fun onBindViewHolder(holder: NewsHolder, position: Int) {
            holder.bind(items[position], onClick, position)

            holder.itemView.alpha = 0f
            holder.itemView.translationY = 34f
            holder.itemView.scaleX = 0.98f
            holder.itemView.scaleY = 0.98f
            holder.itemView.rotationX = 8f

            holder.itemView.postDelayed({
                holder.itemView.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .rotationX(0f)
                    .setDuration(320)
                    .setInterpolator(OvershootInterpolator(1.05f))
                    .start()
            }, min(position * 80L, 520L))
        }

        override fun getItemCount(): Int = items.size

        class NewsHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

            private val image: ImageView = itemView.findViewById(R.id.imageView2)
            private val title: TextView = itemView.findViewById(R.id.textView2)
            private val desc: TextView = itemView.findViewById(R.id.textView3)

            fun bind(item: NewsItem, onClick: (NewsItem) -> Unit, position: Int) {
                image.setImageResource(item.imageRes)
                title.text = item.title
                title.setTypeface(title.typeface, Typeface.BOLD)
                desc.text = item.description

                val root = itemView
                root.setOnClickListener {
                    root.animate()
                        .scaleX(0.965f)
                        .scaleY(0.965f)
                        .setDuration(70)
                        .withEndAction {
                            root.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(110)
                                .setInterpolator(OvershootInterpolator(1.15f))
                                .start()
                        }
                        .start()

                    onClick(item)
                }

                if (position == 0) {
                    root.animate().alpha(1f).setDuration(0).start()
                }
            }
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).roundToInt()
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
                    .setInterpolator(OvershootInterpolator(1.15f))
                    .start()
            }
            .start()
    }
}
package vomatix.ru.spring_2026

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.UUID
class support : Fragment(R.layout.fragment_support) {
    val systemPrompt = """
Ты — ассистент поддержки мобильного приложения «СБЕР Дилер».

Приложение «СБЕР Дилер» — это сервис для сотрудников и менеджеров дилерских центров, который помогает:
- отслеживать личный рейтинг и показатели эффективности
- повышать уровень (статус: Bronze, Silver, Gold и т.д.)
- анализировать продажи, сделки и KPI
- получать рекомендации по улучшению результатов
- работать с заявками и одобрениями

Твоя задача:
- помогать пользователю разобраться с приложением
- объяснять, как повысить рейтинг и уровень
- давать конкретные советы по улучшению показателей
- отвечать просто, понятно и по делу

Правила:
- отвечай на русском
- не пиши длинные тексты
- давай конкретные советы (что сделать, чтобы улучшить результат)
- если вопрос не про приложение — всё равно старайся связать ответ с работой дилера и показателями
- будь дружелюбным, но не формальным

Примеры:
- "Как повысить уровень?" → объясни какие показатели влияют
- "Как подняться в рейтинге?" → дай конкретные действия

Не выдумывай функции, которых нет, но можешь давать разумные рекомендации.
""".trimIndent()

    private companion object {
        const val PREFS_NAME = "gigachat_prefs"
        const val KEY_TOKEN = "access_token"
        const val KEY_EXPIRES = "expires_at"

        // Вставь сюда свой Basic-auth key из Sber Studio, БЕЗ слова "Basic"
        const val AUTH_KEY_BASE64 = "MDE5ZDEyM2QtYzI1My03YmRiLTk1NWYtY2YwNzBkMjE3NmRiOjY2OTU1ZTE5LTJkZWYtNGE0MS04YThiLTliNTAzNzNjNmUzNA=="

        // Если в твоём workspace Lite называется иначе, поменяй только эту строку
        const val MODEL_NAME = "GigaChat-Lite"
    }

    private lateinit var adapter: ChatAdapter
    private lateinit var recyclerChat: RecyclerView
    private lateinit var inputMessage: EditText
    private lateinit var voiceBtn: ImageView
    private lateinit var backBtn: ImageView
    private lateinit var levelBlock: ConstraintLayout
    private lateinit var ratingBlock: ConstraintLayout

    private lateinit var gigaHistory: MutableList<GigaMessage>
    private lateinit var chatItems: MutableList<ChatMessage>
    private lateinit var repo: GigaChatRepository

    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.firstOrNull()?.trim().orEmpty()

            if (spokenText.isNotBlank()) {
                inputMessage.setText(spokenText)
                inputMessage.setSelection(spokenText.length)
                sendMessage(spokenText)
            }
        }
    }

    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startVoiceInput()
        } else {
            Toast.makeText(requireContext(), "Нужен доступ к микрофону", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = GigaChatRepository(requireContext().applicationContext)
        gigaHistory = mutableListOf(
            GigaMessage(
                role = "system",
                content = systemPrompt
            )
        )
        chatItems = mutableListOf()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        backBtn = view.findViewById(R.id.imageView9)
        val title = view.findViewById<TextView>(R.id.textView)
        recyclerChat = view.findViewById(R.id.recyclerChat)
        inputMessage = view.findViewById(R.id.inputMessage)
        voiceBtn = view.findViewById(R.id.voiceBtn)
        levelBlock = view.findViewById(R.id.level_up_constr)
        ratingBlock = view.findViewById(R.id.rating_constr)

        adapter = ChatAdapter()
        recyclerChat.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        recyclerChat.adapter = adapter

        chatItems.add(
            ChatMessage(
                text = "Привет! Я СБЕР Дилер. Чем могу помочь?",
                isUser = false
            )
        )
        adapter.submitList(chatItems)
        scrollToBottom()

        backBtn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        voiceBtn.setOnClickListener {
            it.pressAnim()
            requestMicrophonePermission()
        }

        val sendAction = {
            val text = inputMessage.text.toString().trim()
            if (text.isNotBlank()) {
                inputMessage.setText("")
                sendMessage(text)
            }
        }

        inputMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                sendAction()
                true
            } else {
                false
            }
        }

        levelBlock.setOnClickListener {
            it.pressAnim()
            sendMessage("Как повысить уровень?")
        }

        ratingBlock.setOnClickListener {
            it.pressAnim()
            sendMessage("Как подняться в рейтинге?")
        }

        // Анимация появления
        val elements = listOf(backBtn, title, recyclerChat, levelBlock, ratingBlock, inputMessage, voiceBtn)
        elements.forEach {
            it.alpha = 0f
            it.translationY = 50f
        }

        view.post {
            elements.forEachIndexed { index, item ->
                item.postDelayed({
                    item.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(360)
                        .setInterpolator(OvershootInterpolator(1.12f))
                        .start()
                }, (index * 60).toLong())
            }
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            view?.post {
                reanimateVisibleViews()
            }
        }
    }

    private fun reanimateVisibleViews() {
        val v = view ?: return
        val title = v.findViewById<TextView>(R.id.textView)
        val recycler = v.findViewById<RecyclerView>(R.id.recyclerChat)
        val levelBlock = v.findViewById<ConstraintLayout>(R.id.level_up_constr)
        val ratingBlock = v.findViewById<ConstraintLayout>(R.id.rating_constr)
        val input = v.findViewById<EditText>(R.id.inputMessage)
        val voice = v.findViewById<ImageView>(R.id.voiceBtn)
        val back = v.findViewById<ImageView>(R.id.imageView9)

        listOf(back, title, recycler, levelBlock, ratingBlock, input, voice).forEach {
            it.alpha = 0f
            it.translationY = 30f
        }

        listOf(back, title, recycler, levelBlock, ratingBlock, input, voice).forEachIndexed { index, item ->
            item.postDelayed({
                item.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(320)
                    .setInterpolator(OvershootInterpolator(1.08f))
                    .start()
            }, (index * 40).toLong())
        }
    }

    private fun requestMicrophonePermission() {
        val granted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            startVoiceInput()
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Говорите…")
        }
        speechLauncher.launch(intent)
    }

    private fun sendMessage(text: String) {
        chatItems.add(ChatMessage(text = text, isUser = true))
        adapter.addMessage(chatItems.last())
        gigaHistory.add(GigaMessage(role = "user", content = text))
        scrollToBottom()

        chatItems.add(ChatMessage(text = "Печатает…", isUser = false, isLoading = true))
        adapter.addMessage(chatItems.last())
        scrollToBottom()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val answer = repo.ask(gigaHistory)

                adapter.removeLastLoading()
                if (chatItems.isNotEmpty()) {
                    chatItems.removeAt(chatItems.lastIndex)
                }

                val botMessage = ChatMessage(text = answer, isUser = false)
                chatItems.add(botMessage)
                adapter.addMessage(botMessage)
                gigaHistory.add(GigaMessage(role = "assistant", content = answer))
            } catch (e: Exception) {
                adapter.removeLastLoading()
                if (chatItems.isNotEmpty()) {
                    chatItems.removeAt(chatItems.lastIndex)
                }

                val errorMessage = ChatMessage(
                    text = "Не получилось получить ответ. Проверь ключ GigaChat и интернет.",
                    isUser = false
                )
                chatItems.add(errorMessage)
                adapter.addMessage(errorMessage)
            }

            scrollToBottom()
        }
    }

    private fun scrollToBottom() {
        recyclerChat.post {
            if (adapter.itemCount > 0) {
                recyclerChat.smoothScrollToPosition(adapter.itemCount - 1)
            }
        }
    }

    fun View.pressAnim() {
        animate()
            .scaleX(0.94f)
            .scaleY(0.94f)
            .setDuration(80)
            .withEndAction {
                animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .setInterpolator(OvershootInterpolator(1.4f))
                    .start()
            }
            .start()
    }
}

private class GigaChatRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val authApi: GigaAuthApi = Retrofit.Builder()
        .baseUrl("https://ngw.devices.sberbank.ru:9443/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GigaAuthApi::class.java)

    private val chatApi: GigaChatApi = Retrofit.Builder()
        .baseUrl("https://gigachat.devices.sberbank.ru/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GigaChatApi::class.java)

    suspend fun ask(history: List<GigaMessage>): String {
        val token = getValidToken()

        val response = chatApi.chat(
            authorization = "Bearer $token",
            request = GigaChatRequest(
                model = MODEL_NAME,
                messages = history
            )
        )

        return response.choices.firstOrNull()
            ?.message
            ?.content
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: "Пока не получилось сформировать ответ."
    }

    private suspend fun getValidToken(): String {
        val cachedToken = prefs.getString(KEY_TOKEN, null)
        val expiresAt = prefs.getLong(KEY_EXPIRES, 0L)
        val now = System.currentTimeMillis()

        if (!cachedToken.isNullOrBlank() && now < expiresAt) {
            return cachedToken
        }

        val authResponse = authApi.getToken(
            authorization = "Basic $AUTH_KEY_BASE64",
            rqUid = UUID.randomUUID().toString(),
            scope = "GIGACHAT_API_PERS"
        )

        val newToken = authResponse.access_token
        val newExpires = now + 29L * 60L * 1000L

        prefs.edit {
            putString(KEY_TOKEN, newToken)
            putLong(KEY_EXPIRES, newExpires)
        }

        return newToken
    }

    private companion object {
        const val PREFS_NAME = "gigachat_prefs"
        const val KEY_TOKEN = "access_token"
        const val KEY_EXPIRES = "expires_at"
        const val AUTH_KEY_BASE64 = "PASTE_YOUR_BASIC_AUTH_KEY_HERE"
        const val MODEL_NAME = "GigaChat-2"
    }
}

private interface GigaAuthApi {
    @FormUrlEncoded
    @POST("api/v2/oauth")
    suspend fun getToken(
        @Header("Authorization") authorization: String,
        @Header("RqUID") rqUid: String,
        @Field("scope") scope: String
    ): OAuthResponse
}

private interface GigaChatApi {
    @POST("api/v1/chat/completions")
    suspend fun chat(
        @Header("Authorization") authorization: String,
        @Body request: GigaChatRequest
    ): GigaChatResponse
}


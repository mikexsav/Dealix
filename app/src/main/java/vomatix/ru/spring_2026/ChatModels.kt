package vomatix.ru.spring_2026

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val isLoading: Boolean = false
)

data class GigaMessage(
    val role: String,
    val content: String
)

data class OAuthResponse(
    val access_token: String
)

data class GigaChatRequest(
    val model: String,
    val messages: List<GigaMessage>,
    val n: Int = 1,
    val stream: Boolean = false,
    val max_tokens: Int = 512,
    val repetition_penalty: Double = 1.0,
    val update_interval: Int = 0
)

data class GigaChatResponse(
    val choices: List<GigaChoice> = emptyList()
)

data class GigaChoice(
    val message: GigaMessage
)
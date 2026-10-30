package com.sneha.ai.ai

data class ChatMsg(
    val role: String,
    val text: String,
    val ts: Long = System.currentTimeMillis(),
    val modelUsed: String? = null,
    val isError: Boolean = false,
    val retryPrompt: String? = null
)

data class GenConfig(
    val model: String,
    val temperature: Float?,
    val maxTokens: Int,
    val fallbackModel: String? = null,
    val autoFallback: Boolean = true,
    val maxRetries: Int = 3
)

data class AiReply(
    val text: String,
    val promptTokens: Int,
    val outputTokens: Int,
    val modelUsed: String = "",
    val usedFallback: Boolean = false
)

enum class AiError(val retryable: Boolean) {
    NO_KEY(false),
    INVALID_KEY(false),
    QUOTA(false),
    RATE_LIMIT(true),
    HIGH_DEMAND(true),
    TIMEOUT(true),
    NETWORK(true),
    MODEL_NOT_FOUND(false),
    SERVER(true),
    BAD_REQUEST(false),
    BLOCKED(false),
    OTHER(false)
}

class AiException(
    val kind: AiError,
    message: String,
    val httpCode: Int? = null,
    val rawStatus: String? = null
) : Exception(message) {

    val isRetryable: Boolean get() = kind.retryable

    fun userMessage(): String = when (kind) {
        AiError.NO_KEY ->
            "Gemini API key is not set. Go to Settings > AI Brain and enter your API key."
        AiError.INVALID_KEY ->
            "The API key is incorrect, blocked, or not authorised. Get a new API key from Google AI Studio and enter it in Settings > AI Brain."
        AiError.QUOTA ->
            "Your API account's daily quota or billing limit is exhausted. Try again later or choose another model."
        AiError.RATE_LIMIT ->
            "Too many requests were sent in a short time (HTTP 429). Wait a few seconds and tap 'Try again'."
        AiError.HIGH_DEMAND ->
            "This model is under heavy load right now (HTTP 503 High Demand). Wait a moment and try again, or choose a lighter model in Settings."
        AiError.TIMEOUT ->
            "The Gemini server is taking too long to reply (Timeout). Check your internet and try again."
        AiError.NETWORK ->
            "There is a problem with the internet connection. Check that mobile data or Wi-Fi is on, then try again."
        AiError.MODEL_NOT_FOUND ->
            "The selected model was not found for your API key (HTTP 404). Go to Settings > AI Brain, tap 'Test Connection', and pick a model from the list."
        AiError.SERVER ->
            "There was a temporary error on the Gemini server (HTTP ${httpCode ?: 500}). Try again in a moment."
        AiError.BAD_REQUEST ->
            "Request error (HTTP 400): ${message ?: "Invalid parameter"}"
        AiError.BLOCKED ->
            message ?: "The reply was blocked by the safety policy or came back empty."
        AiError.OTHER ->
            "Unexpected error: ${message ?: "Unknown problem"}"
    }
}

interface AiProvider {
    val id: String
    val name: String
    suspend fun listModels(): Result<List<String>>
    suspend fun generate(
        system: String,
        history: List<ChatMsg>,
        cfg: GenConfig,
        json: Boolean
    ): Result<AiReply> = generate(system, history, cfg, json, null)

    suspend fun generate(
        system: String,
        history: List<ChatMsg>,
        cfg: GenConfig,
        json: Boolean,
        onStatus: ((String) -> Unit)?
    ): Result<AiReply>
}

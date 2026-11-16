package com.example.ai

import com.example.data.model.AssistantLanguage
import com.example.data.model.ParsedIntent
import com.example.data.model.ResponseStyle

data class AiGenerationResult(
    val text: String,
    val isPlaceholder: Boolean
)

/**
 * Single AI interface through which all generative/LLM intelligence flows.
 * Currently backed by LocalPlaceholderAiRepository because Firebase AI Logic
 * setup was declined by the user.
 */
interface AiRepository {
    val isCloudAiConnected: Boolean

    suspend fun parseOrClassifyIntent(
        rawInput: String,
        wakePhrase: String,
        preferredLanguage: AssistantLanguage
    ): ParsedIntent

    suspend fun generateConversationalResponse(
        userPrompt: String,
        language: AssistantLanguage,
        responseStyle: ResponseStyle,
        memories: List<String>,
        isOnline: Boolean
    ): AiGenerationResult
}

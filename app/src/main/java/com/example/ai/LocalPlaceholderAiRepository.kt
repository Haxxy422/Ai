package com.example.ai

import com.example.data.model.AssistantLanguage
import com.example.data.model.ParsedIntent
import com.example.data.model.ResponseStyle

/**
 * Fallback/placeholder AiRepository used when Firebase AI Logic is not enabled.
 * All device commands and multilingual intents still parse & execute live on Android;
 * open-ended LLM queries return clearly labeled placeholder sample responses.
 */
class LocalPlaceholderAiRepository(
    private val intentParser: IntentParser = IntentParser()
) : AiRepository {

    override val isCloudAiConnected: Boolean = false

    override suspend fun parseOrClassifyIntent(
        rawInput: String,
        wakePhrase: String,
        preferredLanguage: AssistantLanguage
    ): ParsedIntent {
        return intentParser.parse(
            rawInput = rawInput,
            wakePhrase = wakePhrase,
            preferredLanguage = preferredLanguage
        )
    }

    override suspend fun generateConversationalResponse(
        userPrompt: String,
        language: AssistantLanguage,
        responseStyle: ResponseStyle,
        memories: List<String>,
        isOnline: Boolean
    ): AiGenerationResult {
        if (!isOnline) {
            val offlineMsg = when (language) {
                AssistantLanguage.BENGALI ->
                    "আমি এখন অফলাইন মোডে আছি। তবে অ্যাপ খোলা, অ্যালার্ম সেট করা, মিডিয়া নিয়ন্ত্রণ বা ডিভাইস তথ্য দেখার মতো স্থানীয় কাজ করতে পারি।"
                AssistantLanguage.HINDI ->
                    "मैं अभी ऑफलाइन मोड में हूँ। फिर भी मैं ऐप खोलने, अलार्म सेट करने, मीडिया कंट्रोल करने और समय बताने जैसे लोकल काम कर सकती हूँ।"
                else ->
                    "I'm currently in Offline Mode. I can still open installed apps, control local media, adjust volume, set alarms, and check device info."
            }
            return AiGenerationResult(text = offlineMsg, isPlaceholder = false)
        }

        val lower = userPrompt.lowercase()
        val memoryHint = if (memories.isNotEmpty()) {
            " (Memory context noted: ${memories.first()})"
        } else ""

        val sampleBody = when {
            lower.contains("quantum") -> when (responseStyle) {
                ResponseStyle.CONCISE ->
                    "Quantum physics studies matter and energy at atomic scales, where particles exist in probability states (superposition) and can link instantaneously (entanglement)."
                else ->
                    "Quantum physics describes how energy and matter behave at the atomic and subatomic scale. Unlike everyday objects, quantum particles exist in superpositions of states until measured, carry discrete energy packets called quanta, and can exhibit entanglement across distances."
            }
            lower.contains("who are you") || lower.contains("your name") || lower.contains("তুমি কে") || lower.contains("कौन हो") ->
                when (language) {
                    AssistantLanguage.BENGALI ->
                        "আমি রোজ (ROSE AI) — আপনার ব্যক্তিগত বুদ্ধিমান অ্যাসিস্ট্যান্ট। ভয়েস কমান্ড, অ্যাপ নিয়ন্ত্রণ ও প্রতিদিনের কাজে আপনাকে সাহায্য করতে আমি প্রস্তুত।"
                    AssistantLanguage.HINDI ->
                        "मैं रोज़ (ROSE AI) हूँ — आपकी पर्सनल AI असिस्टेंट। वॉइस कमांड, ऐप कंट्रोल और रोज़मर्रा के कामों में मदद के लिए हमेशा तैयार।"
                    else ->
                        "I'm ROSE — Responsive Operating System & Intelligent Executive, your personal AI assistant designed to help you control your device and get answers effortlessly."
                }
            else -> when (language) {
                AssistantLanguage.BENGALI ->
                    "\"$userPrompt\" সম্পর্কে এটি একটি প্লেসহোল্ডার নমুনা উত্তর। লাইভ Gemini AI উত্তর পেতে Firebase AI Logic চালু করুন।$memoryHint"
                AssistantLanguage.HINDI ->
                    "\"$userPrompt\" के लिए यह एक प्लेसहोल्डर सैंपल उत्तर है। लाइव Gemini AI उत्तरों के लिए Firebase AI Logic सक्षम करें।$memoryHint"
                else ->
                    "Sample placeholder answer for \"$userPrompt\". Connect Firebase AI Logic to unlock live Gemini reasoning and conversational intelligence.$memoryHint"
            }
        }

        return AiGenerationResult(
            text = "[Placeholder AI] $sampleBody",
            isPlaceholder = true
        )
    }
}

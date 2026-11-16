package com.example.ai

import com.example.data.model.ActionType
import com.example.data.model.AssistantLanguage
import com.example.data.model.ParsedIntent
import java.util.Calendar
import java.util.Locale

class IntentParser {

    fun stripWakePhrase(raw: String, customWakePhrase: String = "Hey Rose"): Pair<Boolean, String> {
        val trimmed = raw.trim()
        val lower = trimmed.lowercase(Locale.ROOT)
        val wakeCandidates = listOf(
            customWakePhrase.trim().lowercase(Locale.ROOT),
            "hey rose",
            "ok rose",
            "okay rose",
            "hi rose",
            "hello rose",
            "rose",
            "হেই রোজ",
            "রোজ",
            "हे रोज़",
            "रोज़"
        ).filter { it.isNotBlank() }.distinct().sortedByDescending { it.length }

        for (candidate in wakeCandidates) {
            if (lower == candidate) {
                return true to ""
            }
            if (lower.startsWith("$candidate ") || lower.startsWith("$candidate,")) {
                val remainder = trimmed.substring(candidate.length).trimStart(',', '.', ' ', ':', '—', '-')
                return true to remainder.trim()
            }
        }
        return false to trimmed
    }

    fun detectLanguage(text: String, preferred: AssistantLanguage): AssistantLanguage {
        val hasBengali = text.any { it in '\u0980'..'\u09FF' }
        if (hasBengali) return AssistantLanguage.BENGALI

        val hasHindi = text.any { it in '\u0900'..'\u097F' }
        if (hasHindi) return AssistantLanguage.HINDI

        val lower = text.lowercase(Locale.ROOT)
        val bengaliRomanCues = listOf("koro", "khol", "chala", "chalao", "dekhao", "baje", "amar", "korbe")
        if (bengaliRomanCues.any { lower.contains(it) }) {
            return AssistantLanguage.BENGALI
        }
        val hindiRomanCues = listOf("kholo", "batao", "karo", "bajao", "kitne", "sunao")
        if (hindiRomanCues.any { lower.contains(it) }) {
            return if (preferred == AssistantLanguage.BENGALI) AssistantLanguage.BENGALI else AssistantLanguage.HINDI
        }

        return if (preferred == AssistantLanguage.AUTO) AssistantLanguage.ENGLISH else preferred
    }

    fun parse(
        rawInput: String,
        wakePhrase: String = "Hey Rose",
        preferredLanguage: AssistantLanguage = AssistantLanguage.AUTO
    ): ParsedIntent {
        val (_, cleaned) = stripWakePhrase(rawInput, wakePhrase)
        val command = cleaned.ifBlank { rawInput.trim() }
        val lower = command.lowercase(Locale.ROOT)
        val lang = detectLanguage(command, preferredLanguage)

        // 1. Memory commands
        if (lower.startsWith("clear all my memories") || lower.startsWith("clear memories") ||
            lower.contains("forget everything")
        ) {
            return ParsedIntent(ActionType.CLEAR_MEMORY, emptyMap(), lang, 0.96f, command)
        }
        if (lower.startsWith("what do you remember") || lower.contains("what do you know about me") ||
            lower == "my memories" || lower == "show memories"
        ) {
            return ParsedIntent(ActionType.RECALL_MEMORY, emptyMap(), lang, 0.95f, command)
        }
        if (lower.startsWith("forget ")) {
            val target = command.substringAfter(" ", "").trim()
                .removePrefix("that ")
                .trim()
            val keyword = if (target == "that" || target.isBlank()) "" else target
            return ParsedIntent(ActionType.FORGET_MEMORY, mapOf("keyword" to keyword), lang, 0.92f, command)
        }
        if (lower.startsWith("remember that ") || lower.startsWith("remember my ") ||
            lower.startsWith("remember ")
        ) {
            val fact = command
                .replace(Regex("^(?i)remember\\s+(that\\s+)?"), "")
                .trim()
            return ParsedIntent(ActionType.REMEMBER_FACT, mapOf("fact" to fact), lang, 0.94f, command)
        }

        // 2. Time & Device status
        if (lower.contains("what's the time") || lower.contains("what is the time") ||
            lower.contains("what time is it") || lower == "time" ||
            command.contains("কটা বাজে") || command.contains("সময়") || command.contains("कितने बजे")
        ) {
            return ParsedIntent(ActionType.GET_TIME, emptyMap(), lang, 0.95f, command)
        }
        if (lower.contains("battery") || lower.contains("device info") || lower.contains("phone status")) {
            return ParsedIntent(ActionType.GET_DEVICE_INFO, emptyMap(), lang, 0.92f, command)
        }

        // 3. Accessibility & screen automation
        if (lower == "go back" || lower == "back" || lower == "navigate back") {
            return ParsedIntent(ActionType.GO_BACK, emptyMap(), lang, 0.96f, command)
        }
        if (lower == "go home" || lower == "home" || lower.contains("press the home button") ||
            lower.contains("press home")
        ) {
            return ParsedIntent(ActionType.GO_HOME, emptyMap(), lang, 0.96f, command)
        }
        if (lower.contains("scroll down") || lower.contains("swipe down")) {
            return ParsedIntent(ActionType.SCROLL_DOWN, emptyMap(), lang, 0.95f, command)
        }
        if (lower.contains("scroll up") || lower.contains("swipe up")) {
            return ParsedIntent(ActionType.SCROLL_UP, emptyMap(), lang, 0.95f, command)
        }
        if (lower.startsWith("click ") || lower.startsWith("tap ")) {
            val target = command.substringAfter(" ", "")
                .replace(Regex("^(?i)(the|on)\\s+"), "")
                .trim()
            return ParsedIntent(ActionType.CLICK, mapOf("target" to target), lang, 0.9f, command)
        }
        if (lower.startsWith("type ")) {
            val textToType = command.substringAfter(" ", "").removePrefix("this text ").trim()
            return ParsedIntent(ActionType.TYPE_TEXT, mapOf("text" to textToType), lang, 0.9f, command)
        }

        // 4. Volume & Media controls
        if (lower.contains("increase volume") || lower.contains("volume up") || lower.contains("louder")) {
            return ParsedIntent(ActionType.VOLUME_UP, emptyMap(), lang, 0.95f, command)
        }
        if (lower.contains("decrease volume") || lower.contains("volume down") || lower.contains("lower volume")) {
            return ParsedIntent(ActionType.VOLUME_DOWN, emptyMap(), lang, 0.95f, command)
        }
        if (lower == "pause" || lower == "pause music" || lower == "pause song" || lower == "pause video") {
            return ParsedIntent(ActionType.PAUSE_MEDIA, emptyMap(), lang, 0.95f, command)
        }
        if (lower == "resume" || lower == "resume music" || lower == "play" || lower == "continue playing") {
            return ParsedIntent(ActionType.PLAY_MEDIA, emptyMap(), lang, 0.94f, command)
        }
        if (lower == "next" || lower.contains("next song") || lower.contains("next track") || lower.contains("skip song")) {
            return ParsedIntent(ActionType.NEXT_MEDIA, emptyMap(), lang, 0.95f, command)
        }
        if (lower == "previous" || lower.contains("previous song") || lower.contains("previous track")) {
            return ParsedIntent(ActionType.PREVIOUS_MEDIA, emptyMap(), lang, 0.95f, command)
        }
        if (lower == "stop" || lower == "stop music" || lower == "stop media") {
            return ParsedIntent(ActionType.STOP_MEDIA, emptyMap(), lang, 0.95f, command)
        }

        // 5. Bengali / Hindi music commands ("গান চালাও", "गाना चलाओ", "play Arijit Singh")
        if (command.contains("গান চালাও") || command.contains("গান বাজাও") || command.contains("गाना चलाओ")) {
            return ParsedIntent(ActionType.PLAY_MEDIA, mapOf("query" to "relaxing Bengali Hindi songs"), lang, 0.92f, command)
        }
        if (lower.startsWith("play ")) {
            val query = command.substringAfter("play ", "").trim()
            return ParsedIntent(ActionType.PLAY_MEDIA, mapOf("query" to query), lang, 0.91f, command)
        }

        // 6. YouTube Search
        if (lower.contains("search youtube for ") || lower.startsWith("youtube search ")) {
            val query = command
                .replace(Regex("(?i)^.*?(search youtube for|youtube search)\\s+"), "")
                .trim()
            return ParsedIntent(ActionType.SEARCH_YOUTUBE, mapOf("query" to query), lang, 0.95f, command)
        }
        if (lower.startsWith("search ") && (lower.contains("lecture") || lower.contains("video") || lower.contains("song"))) {
            val query = command.replace(Regex("(?i)^search\\s+(for\\s+)?"), "").trim()
            return ParsedIntent(ActionType.SEARCH_YOUTUBE, mapOf("query" to query), lang, 0.88f, command)
        }

        // 7. Alarm & Reminder
        if (lower.contains("alarm") || command.contains("অ্যালার্ম") || command.contains("अलार्म")) {
            val (hour, minute) = extractTime(command)
            val params = mutableMapOf<String, String>()
            if (hour != null) {
                params["hour"] = hour.toString()
                params["minute"] = minute.toString()
            }
            return ParsedIntent(ActionType.CREATE_ALARM, params, lang, 0.93f, command)
        }
        if (lower.startsWith("remind me") || lower.contains("create a reminder") || lower.contains("set a reminder")) {
            val (hour, minute) = extractTime(command)
            val triggerMillis = computeNextTriggerMillis(hour, minute)
            val title = command
                .replace(Regex("(?i)^(remind me( to)?|create a reminder( to)?|set a reminder( to)?)\\s*"), "")
                .replace(Regex("(?i)\\s+at\\s+\\d{1,2}(:\\d{2})?\\s*(am|pm)?"), "")
                .trim()
                .ifBlank { "Scheduled Reminder" }
            return ParsedIntent(
                ActionType.CREATE_REMINDER,
                mapOf("title" to title, "triggerMillis" to triggerMillis.toString()),
                lang,
                0.91f,
                command
            )
        }

        // 8. Phone Calls & Messaging
        if (lower.startsWith("call ") || lower.startsWith("phone ")) {
            val contact = command.substringAfter(" ", "").trim()
            return ParsedIntent(ActionType.MAKE_CALL, mapOf("contact" to contact), lang, 0.94f, command)
        }
        if (lower.contains("whatsapp") && (lower.contains("send") || lower.contains("message"))) {
            val (recipient, body) = extractMessageTargetAndBody(command)
            return ParsedIntent(
                ActionType.SEND_MESSAGE,
                mapOf("recipient" to recipient, "message" to body, "channel" to "whatsapp"),
                lang,
                0.92f,
                command
            )
        }
        if (lower.startsWith("send a message to ") || lower.startsWith("message ") || lower.startsWith("text ")) {
            val (recipient, body) = extractMessageTargetAndBody(command)
            return ParsedIntent(
                ActionType.SEND_MESSAGE,
                mapOf("recipient" to recipient, "message" to body, "channel" to "auto"),
                lang,
                0.9f,
                command
            )
        }

        // 9. Navigation & Web Search
        if (lower.startsWith("navigate to ") || lower.startsWith("directions to ")) {
            val dest = command.replace(Regex("(?i)^(navigate to|directions to)\\s+"), "").trim()
            return ParsedIntent(ActionType.NAVIGATE_MAPS, mapOf("destination" to dest), lang, 0.93f, command)
        }
        if (lower.startsWith("search the web") || lower.startsWith("search for ") ||
            lower.startsWith("google ") || lower.startsWith("find the latest") ||
            lower.contains("what's the weather") || lower.contains("weather today")
        ) {
            val query = when {
                lower.contains("weather") -> "weather today"
                lower.startsWith("search the web for ") -> command.substringAfter("for ", "").trim()
                lower.startsWith("search the web") -> command.substringAfter("web", "").trim().ifBlank { "today's news" }
                lower.startsWith("search for ") -> command.substringAfter("for ", "").trim()
                lower.startsWith("google ") -> command.substringAfter(" ", "").trim()
                else -> command
            }
            return ParsedIntent(ActionType.SEARCH_WEB, mapOf("query" to query), lang, 0.9f, command)
        }

        // 10. System / App Launch (English, Bengali, Hindi, Mixed!)
        val openAppMatch = extractAppToOpen(command, lower)
        if (openAppMatch != null) {
            return when (openAppMatch.lowercase(Locale.ROOT)) {
                "settings" -> ParsedIntent(ActionType.OPEN_SETTINGS, emptyMap(), lang, 0.95f, command)
                "camera" -> ParsedIntent(ActionType.OPEN_CAMERA, emptyMap(), lang, 0.95f, command)
                "maps", "google maps" -> ParsedIntent(ActionType.OPEN_MAPS, emptyMap(), lang, 0.95f, command)
                "dialer", "phone" -> ParsedIntent(ActionType.OPEN_DIALER, emptyMap(), lang, 0.94f, command)
                "contacts" -> ParsedIntent(ActionType.OPEN_CONTACTS, emptyMap(), lang, 0.94f, command)
                "calendar" -> ParsedIntent(ActionType.OPEN_CALENDAR, emptyMap(), lang, 0.94f, command)
                else -> ParsedIntent(ActionType.OPEN_APP, mapOf("appName" to openAppMatch), lang, 0.94f, command)
            }
        }

        // 11. Open-ended Conversation / Knowledge Question
        return ParsedIntent(ActionType.CONVERSATION, mapOf("prompt" to command), lang, 0.85f, command)
    }

    private fun extractAppToOpen(raw: String, lower: String): String? {
        // English: "open youtube", "launch spotify", "start chrome"
        val enPrefix = Regex("^(?i)(open|launch|start)\\s+(.+)$").find(raw)
        if (enPrefix != null) {
            val target = enPrefix.groupValues[2]
                .replace(Regex("(?i)\\s+(app|please|করো|কর|koro)$"), "")
                .trim()
            if (target.isNotBlank()) return target
        }

        // Mixed / Bengali / Hindi: "youtube khol", "youtube kholo", "chrome open করো", "ইউটিউব খোলো"
        val suffixMatch = Regex("(?i)^(.+?)\\s+(khol|kholo|khulun|open করো|open কর|খোলো|খোল|खोलो|खोल)$").find(raw)
        if (suffixMatch != null) {
            val candidate = suffixMatch.groupValues[1].trim()
            return normalizeLocalizedAppName(candidate)
        }
        return null
    }

    private fun normalizeLocalizedAppName(name: String): String {
        val lower = name.lowercase(Locale.ROOT)
        return when {
            lower.contains("ইউটিউব") || lower.contains("यूट्यूब") -> "youtube"
            lower.contains("ক্রোম") || lower.contains("क्रोम") -> "chrome"
            lower.contains("হোয়াটসঅ্যাপ") || lower.contains("व्हाट्सएप") -> "whatsapp"
            lower.contains("ক্যামেরা") || lower.contains("कैमरा") -> "camera"
            lower.contains("ক্যালকুলেটর") || lower.contains("कैलकुलेटर") -> "calculator"
            lower.contains("ম্যাপ") || lower.contains("मैप्स") -> "maps"
            lower.contains("সেটিংস") || lower.contains("सेटिंग्स") -> "settings"
            else -> name
        }
    }

    private fun extractTime(text: String): Pair<Int?, Int> {
        val match = Regex("(?i)(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?").find(text) ?: return null to 0
        var hour = match.groupValues[1].toIntOrNull() ?: return null to 0
        val minute = match.groupValues[2].toIntOrNull() ?: 0
        val amPm = match.groupValues[3].lowercase(Locale.ROOT)
        if (amPm == "pm" && hour in 1..11) hour += 12
        if (amPm == "am" && hour == 12) hour = 0
        if (hour !in 0..23 || minute !in 0..59) return null to 0
        return hour to minute
    }

    private fun computeNextTriggerMillis(hour: Int?, minute: Int): Long {
        if (hour == null) return System.currentTimeMillis() + 60 * 60 * 1000L
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return cal.timeInMillis
    }

    private fun extractMessageTargetAndBody(raw: String): Pair<String, String> {
        // Patterns: "Send a WhatsApp message to Rahul saying I'll be late", "Message Rahul hello"
        val sayingMatch = Regex(
            "(?i)(?:send\\s+(?:a\\s+)?(?:whatsapp\\s+)?message\\s+to|message|text)\\s+([\\p{L}0-9_ ]+?)\\s+(?:saying|that|:)\\s+(.+)$"
        ).find(raw)
        if (sayingMatch != null) {
            return sayingMatch.groupValues[1].trim() to sayingMatch.groupValues[2].trim()
        }
        val simpleMatch = Regex("(?i)(?:message|text|send\\s+message\\s+to)\\s+([\\p{L}0-9_]+)(?:\\s+(.+))?$").find(raw)
        if (simpleMatch != null) {
            return simpleMatch.groupValues[1].trim() to simpleMatch.groupValues[2].trim().ifBlank { "Hi!" }
        }
        return "Contact" to raw
    }
}

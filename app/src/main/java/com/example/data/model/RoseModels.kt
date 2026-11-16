package com.example.data.model

import androidx.compose.runtime.Immutable

enum class RoseCoreState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    EXECUTING,
    ERROR,
    OFFLINE
}

enum class AssistantLanguage(
    val displayName: String,
    val nativeLabel: String,
    val speechLocaleTag: String,
    val ttsLanguageTag: String
) {
    AUTO("Auto (EN / BN / HI)", "Multi-Lang", "en-US", "en-US"),
    ENGLISH("English", "English", "en-US", "en-US"),
    BENGALI("Bengali", "বাংলা", "bn-BD", "bn-IN"),
    HINDI("Hindi", "हिन्दी", "hi-IN", "hi-IN")
}

enum class ResponseStyle(val label: String, val description: String) {
    CONCISE("Concise", "Short, crisp responses for rapid control"),
    BALANCED("Balanced", "Natural conversational female tone"),
    DETAILED("Detailed", "Expanded explanations when asking questions")
}

enum class CoreDesignStyle(val label: String) {
    QUANTUM_LOTUS("Quantum Lotus"),
    ORBITAL_RINGS("Orbital Rings"),
    PULSE_MATRIX("Pulse Matrix")
}

enum class AnimationIntensity(val label: String, val multiplier: Float) {
    MINIMAL("Minimal", 0.45f),
    BALANCED("Balanced", 1.0f),
    HYPER("Hyper", 1.45f)
}

enum class AccentPreset(val label: String) {
    ROSE_CYAN("Rose & Cyan"),
    ASTRAL_VIOLET("Astral Violet"),
    EMERALD_AURORA("Emerald Aurora"),
    SOLAR_ROSE("Solar Rose")
}

enum class ActionType(val label: String) {
    OPEN_APP("Open App"),
    SEARCH_WEB("Web Search"),
    SEARCH_YOUTUBE("YouTube Search"),
    PLAY_MEDIA("Play Media"),
    PAUSE_MEDIA("Pause Media"),
    NEXT_MEDIA("Next Track"),
    PREVIOUS_MEDIA("Previous Track"),
    STOP_MEDIA("Stop Media"),
    VOLUME_UP("Volume Up"),
    VOLUME_DOWN("Volume Down"),
    GO_BACK("Go Back"),
    GO_HOME("Go Home"),
    SCROLL_DOWN("Scroll Down"),
    SCROLL_UP("Scroll Up"),
    CLICK("Click Element"),
    TYPE_TEXT("Type Text"),
    CREATE_ALARM("Create Alarm"),
    CREATE_REMINDER("Create Reminder"),
    OPEN_SETTINGS("Open Settings"),
    OPEN_CAMERA("Open Camera"),
    OPEN_MAPS("Open Maps"),
    NAVIGATE_MAPS("Navigate"),
    OPEN_DIALER("Open Dialer"),
    OPEN_CONTACTS("Open Contacts"),
    OPEN_CALENDAR("Open Calendar"),
    MAKE_CALL("Make Call"),
    SEND_MESSAGE("Send Message"),
    GET_TIME("Device Time"),
    GET_DEVICE_INFO("Device Status"),
    REMEMBER_FACT("Save Memory"),
    RECALL_MEMORY("Recall Memory"),
    FORGET_MEMORY("Forget Memory"),
    CLEAR_MEMORY("Clear Memories"),
    CONVERSATION("Conversation")
}

enum class ActionExecutionStatus {
    SUCCESS,
    FAILED,
    CANCELLED,
    REQUIRES_CONFIRMATION,
    INFO
}

@Immutable
data class ParsedIntent(
    val action: ActionType,
    val parameters: Map<String, String> = emptyMap(),
    val detectedLanguage: AssistantLanguage = AssistantLanguage.ENGLISH,
    val confidence: Float = 1.0f,
    val normalizedCommand: String = ""
)

@Immutable
data class ActionResult(
    val success: Boolean,
    val action: ActionType,
    val systemSummary: String,
    val spokenResponse: String,
    val errorReason: String? = null,
    val usedPlaceholderAi: Boolean = false
)

@Immutable
data class PendingConfirmation(
    val id: String,
    val action: ActionType,
    val title: String,
    val promptMessage: String,
    val parameters: Map<String, String>,
    val language: AssistantLanguage
)

@Immutable
data class QuickActionItem(
    val id: String,
    val label: String,
    val appKey: String,
    val iconKey: String,
    val enabled: Boolean = true
) {
    companion object {
        val ALL_PRESETS = listOf(
            QuickActionItem("youtube", "YouTube", "youtube", "play", true),
            QuickActionItem("music", "Music", "music", "music", true),
            QuickActionItem("chrome", "Chrome", "chrome", "globe", true),
            QuickActionItem("camera", "Camera", "camera", "camera", true),
            QuickActionItem("maps", "Maps", "maps", "map", true),
            QuickActionItem("calculator", "Calculator", "calculator", "calc", true),
            QuickActionItem("whatsapp", "WhatsApp", "whatsapp", "chat", false),
            QuickActionItem("spotify", "Spotify", "spotify", "headphones", false),
            QuickActionItem("instagram", "Instagram", "instagram", "photo", false),
            QuickActionItem("telegram", "Telegram", "telegram", "send", false),
            QuickActionItem("clock", "Clock", "clock", "alarm", false),
            QuickActionItem("calendar", "Calendar", "calendar", "calendar", false),
            QuickActionItem("settings", "Settings", "settings", "settings", false)
        )
    }
}

@Immutable
data class VoiceOption(
    val name: String,
    val displayLabel: String,
    val localeTag: String,
    val isFemaleHint: Boolean
)

@Immutable
data class PermissionSnapshot(
    val microphoneGranted: Boolean = false,
    val notificationsGranted: Boolean = false,
    val accessibilityEnabled: Boolean = false,
    val notificationListenerEnabled: Boolean = false,
    val contactsGranted: Boolean = false,
    val phoneGranted: Boolean = false
)

@Immutable
data class RoseUserPreferences(
    val onboardingCompleted: Boolean = false,
    val preferredLanguage: AssistantLanguage = AssistantLanguage.AUTO,
    val responseStyle: ResponseStyle = ResponseStyle.BALANCED,
    val aiTemperature: Float = 0.6f,
    val selectedVoiceName: String = "",
    val voiceSpeed: Float = 1.0f,
    val voicePitch: Float = 1.06f,
    val wakePhrase: String = "Hey Rose",
    val wakeListenerEnabled: Boolean = false,
    val coreDesignStyle: CoreDesignStyle = CoreDesignStyle.QUANTUM_LOTUS,
    val animationIntensity: AnimationIntensity = AnimationIntensity.BALANCED,
    val accentPreset: AccentPreset = AccentPreset.ROSE_CYAN,
    val proactiveModeEnabled: Boolean = true,
    val studyReminderMinutes: Int = 50,
    val historyEnabled: Boolean = true,
    val memoryEnabled: Boolean = true,
    val enabledQuickActionIds: List<String> = listOf(
        "youtube", "music", "chrome", "camera", "maps", "calculator"
    ),
    val hasCustomVaultNote: Boolean = false
)

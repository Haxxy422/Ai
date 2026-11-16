package com.example.data.repository

import com.example.data.local.ChatMessageEntity
import com.example.data.local.CommandHistoryEntity
import com.example.data.local.MemoryEntity
import com.example.data.local.ReminderEntity
import com.example.data.local.RoseDatabase
import com.example.data.local.RosePreferencesStore
import com.example.data.model.AccentPreset
import com.example.data.model.ActionExecutionStatus
import com.example.data.model.ActionType
import com.example.data.model.AnimationIntensity
import com.example.data.model.AssistantLanguage
import com.example.data.model.CoreDesignStyle
import com.example.data.model.ResponseStyle
import com.example.data.model.RoseUserPreferences
import kotlinx.coroutines.flow.Flow

class RoseDataRepository(
    private val database: RoseDatabase,
    val preferencesStore: RosePreferencesStore
) {
    val preferencesFlow: Flow<RoseUserPreferences> = preferencesStore.preferencesFlow
    val chatMessagesFlow: Flow<List<ChatMessageEntity>> = database.chatMessageDao().observeAll()
    val commandHistoryFlow: Flow<List<CommandHistoryEntity>> = database.commandHistoryDao().observeAll()
    val memoriesFlow: Flow<List<MemoryEntity>> = database.memoryDao().observeAll()
    val remindersFlow: Flow<List<ReminderEntity>> = database.reminderDao().observeAll()

    suspend fun addChatMessage(
        sender: String,
        content: String,
        systemStatusText: String? = null,
        status: ActionExecutionStatus = ActionExecutionStatus.INFO,
        actionType: ActionType? = null,
        isPlaceholderAi: Boolean = false
    ) {
        database.chatMessageDao().insert(
            ChatMessageEntity(
                sender = sender,
                content = content,
                systemStatusText = systemStatusText,
                status = status,
                actionType = actionType,
                isPlaceholderAi = isPlaceholderAi
            )
        )
    }

    suspend fun clearChatMessages() {
        database.chatMessageDao().clearAll()
    }

    suspend fun recordHistory(
        command: String,
        response: String,
        actionName: String,
        status: ActionExecutionStatus
    ) {
        database.commandHistoryDao().insert(
            CommandHistoryEntity(
                command = command,
                response = response,
                actionName = actionName,
                status = status
            )
        )
    }

    suspend fun deleteHistoryItem(id: Long) {
        database.commandHistoryDao().deleteById(id)
    }

    suspend fun clearHistory() {
        database.commandHistoryDao().clearAll()
    }

    suspend fun addMemory(content: String, category: String = "Preference"): Long {
        return database.memoryDao().insert(
            MemoryEntity(
                category = category,
                content = content.trim()
            )
        )
    }

    suspend fun getAllMemoriesOnce(): List<MemoryEntity> = database.memoryDao().getAllOnce()

    suspend fun deleteMemory(id: Long) {
        database.memoryDao().deleteById(id)
    }

    suspend fun forgetMatchingMemory(keyword: String): Int {
        return database.memoryDao().deleteMatching(keyword)
    }

    suspend fun clearMemories() {
        database.memoryDao().clearAll()
    }

    suspend fun addReminder(title: String, triggerAtMillis: Long): Long {
        return database.reminderDao().insert(
            ReminderEntity(
                title = title,
                triggerAtMillis = triggerAtMillis
            )
        )
    }

    suspend fun getPendingRemindersOnce(): List<ReminderEntity> = database.reminderDao().getPendingOnce()

    suspend fun markReminderCompleted(id: Long) {
        database.reminderDao().markCompleted(id)
    }

    suspend fun deleteReminder(id: Long) {
        database.reminderDao().deleteById(id)
    }

    suspend fun clearAllUserData() {
        database.chatMessageDao().clearAll()
        database.commandHistoryDao().clearAll()
        database.memoryDao().clearAll()
        database.reminderDao().clearAll()
        preferencesStore.clearAllPreferencesPreservingOnboarding()
    }

    suspend fun updateOnboardingCompleted(completed: Boolean) =
        preferencesStore.setOnboardingCompleted(completed)

    suspend fun updatePreferredLanguage(language: AssistantLanguage) =
        preferencesStore.setPreferredLanguage(language)

    suspend fun updateResponseStyle(style: ResponseStyle) =
        preferencesStore.setResponseStyle(style)

    suspend fun updateAiTemperature(temp: Float) =
        preferencesStore.setAiTemperature(temp)

    suspend fun updateSelectedVoice(voiceName: String) =
        preferencesStore.setSelectedVoiceName(voiceName)

    suspend fun updateVoiceSpeed(speed: Float) =
        preferencesStore.setVoiceSpeed(speed)

    suspend fun updateVoicePitch(pitch: Float) =
        preferencesStore.setVoicePitch(pitch)

    suspend fun updateWakePhrase(phrase: String) =
        preferencesStore.setWakePhrase(phrase)

    suspend fun updateWakeListenerEnabled(enabled: Boolean) =
        preferencesStore.setWakeListenerEnabled(enabled)

    suspend fun updateCoreDesignStyle(style: CoreDesignStyle) =
        preferencesStore.setCoreDesignStyle(style)

    suspend fun updateAnimationIntensity(intensity: AnimationIntensity) =
        preferencesStore.setAnimationIntensity(intensity)

    suspend fun updateAccentPreset(preset: AccentPreset) =
        preferencesStore.setAccentPreset(preset)

    suspend fun updateProactiveMode(enabled: Boolean) =
        preferencesStore.setProactiveModeEnabled(enabled)

    suspend fun updateStudyReminderMinutes(minutes: Int) =
        preferencesStore.setStudyReminderMinutes(minutes)

    suspend fun updateHistoryEnabled(enabled: Boolean) =
        preferencesStore.setHistoryEnabled(enabled)

    suspend fun updateMemoryEnabled(enabled: Boolean) =
        preferencesStore.setMemoryEnabled(enabled)

    suspend fun updateQuickActions(ids: List<String>) =
        preferencesStore.setEnabledQuickActionIds(ids)
}

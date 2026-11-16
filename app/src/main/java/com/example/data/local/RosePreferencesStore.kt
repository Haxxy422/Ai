package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AccentPreset
import com.example.data.model.AnimationIntensity
import com.example.data.model.AssistantLanguage
import com.example.data.model.CoreDesignStyle
import com.example.data.model.ResponseStyle
import com.example.data.model.RoseUserPreferences
import com.example.security.KeystoreVault
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.roseDataStore: DataStore<Preferences> by preferencesDataStore(name = "rose_prefs")

class RosePreferencesStore(
    private val context: Context,
    private val keystoreVault: KeystoreVault
) {
    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val PREFERRED_LANGUAGE = stringPreferencesKey("preferred_language")
        val RESPONSE_STYLE = stringPreferencesKey("response_style")
        val AI_TEMPERATURE = floatPreferencesKey("ai_temperature")
        val SELECTED_VOICE_NAME = stringPreferencesKey("selected_voice_name")
        val VOICE_SPEED = floatPreferencesKey("voice_speed")
        val VOICE_PITCH = floatPreferencesKey("voice_pitch")
        val WAKE_PHRASE = stringPreferencesKey("wake_phrase")
        val WAKE_LISTENER_ENABLED = booleanPreferencesKey("wake_listener_enabled")
        val CORE_DESIGN = stringPreferencesKey("core_design")
        val ANIMATION_INTENSITY = stringPreferencesKey("animation_intensity")
        val ACCENT_PRESET = stringPreferencesKey("accent_preset")
        val PROACTIVE_MODE = booleanPreferencesKey("proactive_mode")
        val STUDY_REMINDER_MINUTES = intPreferencesKey("study_reminder_minutes")
        val HISTORY_ENABLED = booleanPreferencesKey("history_enabled")
        val MEMORY_ENABLED = booleanPreferencesKey("memory_enabled")
        val QUICK_ACTIONS = stringPreferencesKey("enabled_quick_actions")
        val ENCRYPTED_CUSTOM_VAULT_NOTE = stringPreferencesKey("encrypted_custom_vault_note")
    }

    val preferencesFlow: Flow<RoseUserPreferences> = context.roseDataStore.data.map { prefs ->
        RoseUserPreferences(
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            preferredLanguage = prefs[Keys.PREFERRED_LANGUAGE]?.let {
                runCatching { AssistantLanguage.valueOf(it) }.getOrNull()
            } ?: AssistantLanguage.AUTO,
            responseStyle = prefs[Keys.RESPONSE_STYLE]?.let {
                runCatching { ResponseStyle.valueOf(it) }.getOrNull()
            } ?: ResponseStyle.BALANCED,
            aiTemperature = prefs[Keys.AI_TEMPERATURE] ?: 0.6f,
            selectedVoiceName = prefs[Keys.SELECTED_VOICE_NAME] ?: "",
            voiceSpeed = prefs[Keys.VOICE_SPEED] ?: 1.0f,
            voicePitch = prefs[Keys.VOICE_PITCH] ?: 1.06f,
            wakePhrase = prefs[Keys.WAKE_PHRASE]?.takeIf { it.isNotBlank() } ?: "Hey Rose",
            wakeListenerEnabled = prefs[Keys.WAKE_LISTENER_ENABLED] ?: false,
            coreDesignStyle = prefs[Keys.CORE_DESIGN]?.let {
                runCatching { CoreDesignStyle.valueOf(it) }.getOrNull()
            } ?: CoreDesignStyle.QUANTUM_LOTUS,
            animationIntensity = prefs[Keys.ANIMATION_INTENSITY]?.let {
                runCatching { AnimationIntensity.valueOf(it) }.getOrNull()
            } ?: AnimationIntensity.BALANCED,
            accentPreset = prefs[Keys.ACCENT_PRESET]?.let {
                runCatching { AccentPreset.valueOf(it) }.getOrNull()
            } ?: AccentPreset.ROSE_CYAN,
            proactiveModeEnabled = prefs[Keys.PROACTIVE_MODE] ?: true,
            studyReminderMinutes = prefs[Keys.STUDY_REMINDER_MINUTES] ?: 50,
            historyEnabled = prefs[Keys.HISTORY_ENABLED] ?: true,
            memoryEnabled = prefs[Keys.MEMORY_ENABLED] ?: true,
            enabledQuickActionIds = prefs[Keys.QUICK_ACTIONS]
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?: listOf("youtube", "music", "chrome", "camera", "maps", "calculator"),
            hasCustomVaultNote = !prefs[Keys.ENCRYPTED_CUSTOM_VAULT_NOTE].isNullOrBlank()
        )
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.roseDataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setPreferredLanguage(language: AssistantLanguage) {
        context.roseDataStore.edit { it[Keys.PREFERRED_LANGUAGE] = language.name }
    }

    suspend fun setResponseStyle(style: ResponseStyle) {
        context.roseDataStore.edit { it[Keys.RESPONSE_STYLE] = style.name }
    }

    suspend fun setAiTemperature(temp: Float) {
        context.roseDataStore.edit { it[Keys.AI_TEMPERATURE] = temp.coerceIn(0f, 1f) }
    }

    suspend fun setSelectedVoiceName(voiceName: String) {
        context.roseDataStore.edit { it[Keys.SELECTED_VOICE_NAME] = voiceName }
    }

    suspend fun setVoiceSpeed(speed: Float) {
        context.roseDataStore.edit { it[Keys.VOICE_SPEED] = speed.coerceIn(0.6f, 1.6f) }
    }

    suspend fun setVoicePitch(pitch: Float) {
        context.roseDataStore.edit { it[Keys.VOICE_PITCH] = pitch.coerceIn(0.7f, 1.5f) }
    }

    suspend fun setWakePhrase(phrase: String) {
        val sanitized = phrase.trim().ifBlank { "Hey Rose" }
        context.roseDataStore.edit { it[Keys.WAKE_PHRASE] = sanitized }
    }

    suspend fun setWakeListenerEnabled(enabled: Boolean) {
        context.roseDataStore.edit { it[Keys.WAKE_LISTENER_ENABLED] = enabled }
    }

    suspend fun setCoreDesignStyle(style: CoreDesignStyle) {
        context.roseDataStore.edit { it[Keys.CORE_DESIGN] = style.name }
    }

    suspend fun setAnimationIntensity(intensity: AnimationIntensity) {
        context.roseDataStore.edit { it[Keys.ANIMATION_INTENSITY] = intensity.name }
    }

    suspend fun setAccentPreset(preset: AccentPreset) {
        context.roseDataStore.edit { it[Keys.ACCENT_PRESET] = preset.name }
    }

    suspend fun setProactiveModeEnabled(enabled: Boolean) {
        context.roseDataStore.edit { it[Keys.PROACTIVE_MODE] = enabled }
    }

    suspend fun setStudyReminderMinutes(minutes: Int) {
        context.roseDataStore.edit { it[Keys.STUDY_REMINDER_MINUTES] = minutes.coerceIn(10, 180) }
    }

    suspend fun setHistoryEnabled(enabled: Boolean) {
        context.roseDataStore.edit { it[Keys.HISTORY_ENABLED] = enabled }
    }

    suspend fun setMemoryEnabled(enabled: Boolean) {
        context.roseDataStore.edit { it[Keys.MEMORY_ENABLED] = enabled }
    }

    suspend fun setEnabledQuickActionIds(ids: List<String>) {
        context.roseDataStore.edit { it[Keys.QUICK_ACTIONS] = ids.joinToString(",") }
    }

    suspend fun saveEncryptedVaultNote(note: String): Boolean {
        if (note.isBlank()) {
            context.roseDataStore.edit { it.remove(Keys.ENCRYPTED_CUSTOM_VAULT_NOTE) }
            return true
        }
        val encrypted = keystoreVault.encrypt(note) ?: return false
        context.roseDataStore.edit { it[Keys.ENCRYPTED_CUSTOM_VAULT_NOTE] = encrypted }
        return true
    }

    suspend fun readEncryptedVaultNote(): String {
        val prefs = context.roseDataStore.data.first()
        val payload = prefs[Keys.ENCRYPTED_CUSTOM_VAULT_NOTE] ?: return ""
        return keystoreVault.decrypt(payload).orEmpty()
    }

    suspend fun clearAllPreferencesPreservingOnboarding() {
        context.roseDataStore.edit { prefs ->
            prefs.clear()
            prefs[Keys.ONBOARDING_COMPLETED] = true
        }
        keystoreVault.deleteKey()
    }
}

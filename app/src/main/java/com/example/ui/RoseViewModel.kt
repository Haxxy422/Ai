package com.example.ui

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiRepository
import com.example.ai.IntentParser
import com.example.ai.LocalPlaceholderAiRepository
import com.example.automation.AndroidActionExecutor
import com.example.automation.PermissionManager
import com.example.data.local.ChatMessageEntity
import com.example.data.local.CommandHistoryEntity
import com.example.data.local.MemoryEntity
import com.example.data.local.ReminderEntity
import com.example.data.local.RoseDatabase
import com.example.data.local.RosePreferencesStore
import com.example.data.model.AccentPreset
import com.example.data.model.AnimationIntensity
import com.example.data.model.AssistantLanguage
import com.example.data.model.CoreDesignStyle
import com.example.data.model.PendingConfirmation
import com.example.data.model.PermissionSnapshot
import com.example.data.model.QuickActionItem
import com.example.data.model.ResponseStyle
import com.example.data.model.RoseCoreState
import com.example.data.model.RoseUserPreferences
import com.example.data.model.VoiceOption
import com.example.data.repository.RoseDataRepository
import com.example.engine.EngineOutcome
import com.example.engine.RoseAssistantEngine
import com.example.security.KeystoreVault
import com.example.voice.ProactiveNotifier
import com.example.voice.RoseVoiceController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class RoseViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val keystoreVault = KeystoreVault()
    private val database = RoseDatabase.getInstance(appContext)
    private val prefsStore = RosePreferencesStore(appContext, keystoreVault)
    private val repository = RoseDataRepository(database, prefsStore)
    val permissionManager = PermissionManager(appContext)
    private val intentParser = IntentParser()
    private val aiRepository: AiRepository = LocalPlaceholderAiRepository(intentParser)
    private val actionExecutor = AndroidActionExecutor(appContext, permissionManager, repository)
    private val engine = RoseAssistantEngine(
        aiRepository = aiRepository,
        intentParser = intentParser,
        actionExecutor = actionExecutor,
        permissionManager = permissionManager,
        repository = repository
    )
    private val voiceController = RoseVoiceController(appContext)
    private val proactiveNotifier = ProactiveNotifier(appContext, permissionManager)

    val preferences: StateFlow<RoseUserPreferences> = repository.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, RoseUserPreferences())

    val chatMessages: StateFlow<List<ChatMessageEntity>> = repository.chatMessagesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val commandHistory: StateFlow<List<CommandHistoryEntity>> = repository.commandHistoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memories: StateFlow<List<MemoryEntity>> = repository.memoriesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reminders: StateFlow<List<ReminderEntity>> = repository.remindersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableVoices: StateFlow<List<VoiceOption>> = voiceController.availableVoices
    val rmsLevel: StateFlow<Float> = voiceController.rmsLevel
    val partialTranscript: StateFlow<String> = voiceController.partialText
    val isWakeActive: StateFlow<Boolean> = voiceController.isWakeListening

    private val _isOnline = MutableStateFlow(permissionManager.isNetworkOnline())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _permissions = MutableStateFlow(permissionManager.getSnapshot())
    val permissions: StateFlow<PermissionSnapshot> = _permissions.asStateFlow()

    private val _manualCoreState = MutableStateFlow(RoseCoreState.IDLE)
    val coreState: StateFlow<RoseCoreState> = combine(
        _manualCoreState,
        voiceController.isListening,
        voiceController.isSpeaking,
        _isOnline
    ) { manual, listening, speaking, online ->
        when {
            !online && manual == RoseCoreState.IDLE -> RoseCoreState.OFFLINE
            listening -> RoseCoreState.LISTENING
            speaking -> RoseCoreState.SPEAKING
            else -> manual
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, RoseCoreState.IDLE)

    private val _liveTranscription = MutableStateFlow("")
    val liveTranscription: StateFlow<String> = _liveTranscription.asStateFlow()

    private val _lastResponseText = MutableStateFlow("Tap the mic or say \"Hey Rose\" to begin.")
    val lastResponseText: StateFlow<String> = _lastResponseText.asStateFlow()

    private val _lastSystemSummary = MutableStateFlow<String?>(null)
    val lastSystemSummary: StateFlow<String?> = _lastSystemSummary.asStateFlow()

    private val _showPlaceholderAiBanner = MutableStateFlow(false)
    val showPlaceholderAiBanner: StateFlow<Boolean> = _showPlaceholderAiBanner.asStateFlow()

    private val _pendingConfirmation = MutableStateFlow<PendingConfirmation?>(null)
    val pendingConfirmation: StateFlow<PendingConfirmation?> = _pendingConfirmation.asStateFlow()

    private val _proactiveBanner = MutableStateFlow<String?>(null)
    val proactiveBanner: StateFlow<String?> = _proactiveBanner.asStateFlow()

    private val _isKeystoreBacked = MutableStateFlow(keystoreVault.isKeystoreAvailable())
    val isKeystoreBacked: StateFlow<Boolean> = _isKeystoreBacked.asStateFlow()

    private val sessionStartMillis = System.currentTimeMillis()
    private var studyBreakNotified = false
    private val notifiedReminderIds = mutableSetOf<Long>()

    private var connectivityCallback: ConnectivityManager.NetworkCallback? = null

    init {
        registerNetworkObserver()
        setupVoiceCallbacks()
        observeVoiceConfig()
        startProactiveMonitor()
    }

    private fun setupVoiceCallbacks() {
        voiceController.setCallbacks(
            onTranscript = { transcript, fromWake ->
                _liveTranscription.value = transcript
                processInput(transcript, fromWakeLoop = fromWake)
            },
            onError = { errMsg ->
                _manualCoreState.value = RoseCoreState.ERROR
                _lastResponseText.value = errMsg
                _lastSystemSummary.value = "✕ $errMsg"
                viewModelScope.launch {
                    delay(2200L)
                    if (_manualCoreState.value == RoseCoreState.ERROR) {
                        _manualCoreState.value = RoseCoreState.IDLE
                    }
                }
            }
        )
    }

    private fun observeVoiceConfig() {
        viewModelScope.launch {
            preferences.collect { prefs ->
                voiceController.configureVoice(
                    language = prefs.preferredLanguage,
                    voiceName = prefs.selectedVoiceName,
                    speed = prefs.voiceSpeed,
                    pitch = prefs.voicePitch
                )
            }
        }
    }

    private fun registerNetworkObserver() {
        try {
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isOnline.value = permissionManager.isNetworkOnline()
                }

                override fun onLost(network: Network) {
                    _isOnline.value = permissionManager.isNetworkOnline()
                }

                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                    _isOnline.value = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                }
            }
            connectivityCallback = callback
            cm.registerNetworkCallback(NetworkRequest.Builder().build(), callback)
        } catch (_: Exception) {
            _isOnline.value = permissionManager.isNetworkOnline()
        }
    }

    private fun startProactiveMonitor() {
        viewModelScope.launch {
            while (isActive) {
                delay(30_000L)
                val prefs = preferences.value
                if (!prefs.proactiveModeEnabled) {
                    _proactiveBanner.value = null
                    continue
                }
                val now = System.currentTimeMillis()
                val upcoming = repository.getPendingRemindersOnce().firstOrNull { reminder ->
                    val diff = reminder.triggerAtMillis - now
                    diff in -60_000L..(10 * 60_000L) && reminder.id !in notifiedReminderIds
                }
                if (upcoming != null) {
                    notifiedReminderIds.add(upcoming.id)
                    val msg = "Upcoming reminder: ${upcoming.title}"
                    _proactiveBanner.value = msg
                    proactiveNotifier.sendGentleNotification(
                        upcoming.id.toInt(),
                        "ROSE Reminder",
                        msg
                    )
                } else {
                    val elapsedMinutes = (now - sessionStartMillis) / 60_000L
                    if (elapsedMinutes >= prefs.studyReminderMinutes && !studyBreakNotified) {
                        studyBreakNotified = true
                        val msg = "You've been active for $elapsedMinutes minutes. Would you like a short break?"
                        _proactiveBanner.value = msg
                        proactiveNotifier.sendGentleNotification(9901, "ROSE Wellness", msg)
                    }
                }
            }
        }
    }

    fun dismissProactiveBanner() {
        _proactiveBanner.value = null
    }

    fun refreshPermissions() {
        _permissions.value = permissionManager.getSnapshot()
        _isOnline.value = permissionManager.isNetworkOnline()
    }

    fun toggleMicListening() {
        refreshPermissions()
        if (!_permissions.value.microphoneGranted) {
            _manualCoreState.value = RoseCoreState.ERROR
            _lastResponseText.value = "Microphone permission is required to listen."
            _lastSystemSummary.value = "✕ Grant Microphone in Permission Center"
            return
        }
        if (voiceController.isListening.value) {
            voiceController.stopListening()
        } else {
            voiceController.stopSpeaking()
            _liveTranscription.value = ""
            voiceController.startSingleListening(preferences.value.preferredLanguage)
        }
    }

    fun toggleWakeMode(enabled: Boolean) {
        refreshPermissions()
        if (enabled && !_permissions.value.microphoneGranted) {
            _lastResponseText.value = "Microphone permission is needed for \"${preferences.value.wakePhrase}\" wake listening."
            return
        }
        viewModelScope.launch {
            repository.updateWakeListenerEnabled(enabled)
            voiceController.setWakeListeningEnabled(enabled, preferences.value.preferredLanguage)
        }
    }

    fun submitTextCommand(command: String) {
        if (command.isBlank()) return
        _liveTranscription.value = command.trim()
        processInput(command.trim(), fromWakeLoop = false)
    }

    fun launchQuickAction(item: QuickActionItem) {
        submitTextCommand("Open ${item.label}")
    }

    private fun processInput(text: String, fromWakeLoop: Boolean) {
        viewModelScope.launch {
            _manualCoreState.value = RoseCoreState.THINKING
            val outcome = engine.processCommand(
                rawInput = text,
                prefs = preferences.value,
                pendingConfirmation = _pendingConfirmation.value,
                fromWakeLoop = fromWakeLoop
            )
            handleOutcome(outcome)
        }
    }

    fun confirmPendingAction() {
        val pending = _pendingConfirmation.value ?: return
        _pendingConfirmation.value = null
        viewModelScope.launch {
            _manualCoreState.value = RoseCoreState.EXECUTING
            val outcome = engine.confirmAndExecute(pending, preferences.value)
            handleOutcome(outcome)
        }
    }

    fun cancelPendingAction() {
        val pending = _pendingConfirmation.value ?: return
        _pendingConfirmation.value = null
        viewModelScope.launch {
            val outcome = engine.cancelConfirmation(pending, preferences.value)
            handleOutcome(outcome)
        }
    }

    private suspend fun handleOutcome(outcome: EngineOutcome) {
        when (outcome) {
            is EngineOutcome.WakeOnly -> {
                if (outcome.greeting.isNotBlank()) {
                    _lastResponseText.value = outcome.greeting
                    _lastSystemSummary.value = "✓ Wake phrase recognized"
                    voiceController.speak(outcome.greeting, preferences.value.preferredLanguage)
                    voiceController.startSingleListening(preferences.value.preferredLanguage)
                }
                _manualCoreState.value = RoseCoreState.IDLE
            }
            is EngineOutcome.NeedsConfirmation -> {
                _pendingConfirmation.value = outcome.confirmation
                _lastResponseText.value = outcome.spokenPrompt
                _lastSystemSummary.value = "⚠ Awaiting your confirmation"
                _manualCoreState.value = RoseCoreState.IDLE
                voiceController.speak(outcome.spokenPrompt, outcome.confirmation.language)
            }
            is EngineOutcome.Executed -> {
                _pendingConfirmation.value = null
                _manualCoreState.value = if (outcome.result.success) {
                    RoseCoreState.EXECUTING
                } else {
                    RoseCoreState.ERROR
                }
                _lastResponseText.value = outcome.result.spokenResponse
                _lastSystemSummary.value = outcome.result.systemSummary
                if (outcome.result.usedPlaceholderAi) {
                    _showPlaceholderAiBanner.value = true
                }
                voiceController.speak(outcome.result.spokenResponse, preferences.value.preferredLanguage)
                delay(900L)
                _manualCoreState.value = RoseCoreState.IDLE
            }
        }
    }

    fun testFemaleVoice(sampleText: String = "Hello, I'm Rose. Your voice, your intelligence, your control.") {
        voiceController.speak(sampleText, preferences.value.preferredLanguage)
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            repository.updateOnboardingCompleted(true)
        }
    }

    fun setPreferredLanguage(language: AssistantLanguage) {
        viewModelScope.launch { repository.updatePreferredLanguage(language) }
    }

    fun setResponseStyle(style: ResponseStyle) {
        viewModelScope.launch { repository.updateResponseStyle(style) }
    }

    fun setAiTemperature(temp: Float) {
        viewModelScope.launch { repository.updateAiTemperature(temp) }
    }

    fun setSelectedVoice(name: String) {
        viewModelScope.launch { repository.updateSelectedVoice(name) }
    }

    fun setVoiceSpeed(speed: Float) {
        viewModelScope.launch { repository.updateVoiceSpeed(speed) }
    }

    fun setVoicePitch(pitch: Float) {
        viewModelScope.launch { repository.updateVoicePitch(pitch) }
    }

    fun setWakePhrase(phrase: String) {
        viewModelScope.launch { repository.updateWakePhrase(phrase) }
    }

    fun setCoreDesignStyle(style: CoreDesignStyle) {
        viewModelScope.launch { repository.updateCoreDesignStyle(style) }
    }

    fun setAnimationIntensity(intensity: AnimationIntensity) {
        viewModelScope.launch { repository.updateAnimationIntensity(intensity) }
    }

    fun setAccentPreset(preset: AccentPreset) {
        viewModelScope.launch { repository.updateAccentPreset(preset) }
    }

    fun setProactiveMode(enabled: Boolean) {
        viewModelScope.launch { repository.updateProactiveMode(enabled) }
    }

    fun setHistoryEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.updateHistoryEnabled(enabled) }
    }

    fun setMemoryEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.updateMemoryEnabled(enabled) }
    }

    fun toggleQuickAction(id: String) {
        viewModelScope.launch {
            val current = preferences.value.enabledQuickActionIds.toMutableList()
            if (current.contains(id)) {
                if (current.size > 1) current.remove(id)
            } else {
                current.add(id)
            }
            repository.updateQuickActions(current)
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch { repository.deleteHistoryItem(id) }
    }

    fun clearHistory() {
        viewModelScope.launch { repository.clearHistory() }
    }

    fun addMemory(content: String, category: String = "Personal") {
        if (content.isBlank()) return
        viewModelScope.launch { repository.addMemory(content, category) }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch { repository.deleteMemory(id) }
    }

    fun clearMemories() {
        viewModelScope.launch { repository.clearMemories() }
    }

    fun clearChat() {
        viewModelScope.launch { repository.clearChatMessages() }
    }

    fun clearAllUserData() {
        viewModelScope.launch {
            voiceController.stopListening()
            voiceController.stopSpeaking()
            repository.clearAllUserData()
            _lastResponseText.value = "All local data has been securely cleared."
            _lastSystemSummary.value = "✓ Local history, memories & Keystore vault wiped"
        }
    }

    suspend fun saveEncryptedVaultNote(note: String): Boolean {
        return repository.preferencesStore.saveEncryptedVaultNote(note)
    }

    suspend fun readEncryptedVaultNote(): String {
        return repository.preferencesStore.readEncryptedVaultNote()
    }

    override fun onCleared() {
        connectivityCallback?.let { cb ->
            runCatching {
                val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                cm?.unregisterNetworkCallback(cb)
            }
        }
        voiceController.release()
        super.onCleared()
    }
}

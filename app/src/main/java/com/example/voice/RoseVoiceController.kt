package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.model.AssistantLanguage
import com.example.data.model.VoiceOption
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages real Android SpeechRecognizer (STT + live RMS waveform + wake listener)
 * and TextToSpeech (female voice discovery, pitch/rate control, multilingual output).
 */
class RoseVoiceController(private val context: Context) {

    companion object {
        private const val TAG = "RoseVoiceController"
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isWakeListening = MutableStateFlow(false)
    val isWakeListening: StateFlow<Boolean> = _isWakeListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<VoiceOption>>(emptyList())
    val availableVoices: StateFlow<List<VoiceOption>> = _availableVoices.asStateFlow()

    private var onFinalTranscript: ((String, Boolean) -> Unit)? = null
    private var onSpeechErrorCallback: ((String) -> Unit)? = null
    private var wakeModeActive = false
    private var currentLanguage = AssistantLanguage.AUTO
    private var currentVoiceName = ""
    private var currentPitch = 1.06f
    private var currentSpeed = 1.0f

    init {
        initTts()
    }

    private fun initTts() {
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isTtsReady = true
                    loadFemaleVoices()
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _isSpeaking.value = true
                        }

                        override fun onDone(utteranceId: String?) {
                            _isSpeaking.value = false
                            if (wakeModeActive) {
                                mainHandler.postDelayed({ restartWakeListeningIfNeeded() }, 350L)
                            }
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            _isSpeaking.value = false
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            _isSpeaking.value = false
                        }
                    })
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "TTS unavailable in current environment", e)
        }
    }

    private fun loadFemaleVoices() {
        val engine = tts ?: return
        val voices = runCatching { engine.voices }.getOrNull().orEmpty()
        val filtered = voices
            .filter { voice ->
                val lang = voice.locale.language
                lang == "en" || lang == "bn" || lang == "hi"
            }
            .map { voice ->
                val nameLower = voice.name.lowercase(Locale.ROOT)
                val isFemaleHint = nameLower.contains("female") ||
                    nameLower.contains("-sfg") ||
                    nameLower.contains("-tpf") ||
                    nameLower.contains("-iob") ||
                    nameLower.contains("-x-af") ||
                    nameLower.contains("-x-bn") ||
                    nameLower.contains("-x-hi") ||
                    nameLower.endsWith("f")
                VoiceOption(
                    name = voice.name,
                    displayLabel = "${voice.locale.displayLanguage} (${voice.locale.country}) · ${voice.name.takeLast(10)}",
                    localeTag = voice.locale.toLanguageTag(),
                    isFemaleHint = isFemaleHint
                )
            }
            .sortedByDescending { it.isFemaleHint }
            .take(18)
        _availableVoices.value = filtered
    }

    fun configureVoice(
        language: AssistantLanguage,
        voiceName: String,
        speed: Float,
        pitch: Float
    ) {
        currentLanguage = language
        currentVoiceName = voiceName
        currentSpeed = speed
        currentPitch = pitch
    }

    fun speak(text: String, language: AssistantLanguage = currentLanguage) {
        if (text.isBlank()) return
        val cleanText = text.replace("[Placeholder AI]", "").trim()
        if (!isTtsReady || tts == null) return

        mainHandler.post {
            try {
                val engine = tts ?: return@post
                val targetLocale = Locale.forLanguageTag(language.ttsLanguageTag)
                engine.language = targetLocale
                engine.setSpeechRate(currentSpeed)
                engine.setPitch(currentPitch)

                if (currentVoiceName.isNotBlank()) {
                    engine.voices?.firstOrNull { it.name == currentVoiceName }?.let {
                        engine.voice = it
                    }
                } else {
                    // Auto-pick best female voice matching locale
                    val autoFemale = _availableVoices.value.firstOrNull {
                        it.localeTag.startsWith(targetLocale.language) && it.isFemaleHint
                    } ?: _availableVoices.value.firstOrNull { it.isFemaleHint }
                    autoFemale?.let { chosen ->
                        engine.voices?.firstOrNull { it.name == chosen.name }?.let { engine.voice = it }
                    }
                }

                engine.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
            } catch (e: Exception) {
                Log.w(TAG, "TTS speak failed", e)
                _isSpeaking.value = false
            }
        }
    }

    fun stopSpeaking() {
        mainHandler.post {
            tts?.stop()
            _isSpeaking.value = false
        }
    }

    fun setCallbacks(
        onTranscript: (transcript: String, fromWakeLoop: Boolean) -> Unit,
        onError: (message: String) -> Unit
    ) {
        onFinalTranscript = onTranscript
        onSpeechErrorCallback = onError
    }

    fun startSingleListening(language: AssistantLanguage = currentLanguage) {
        mainHandler.post {
            stopListeningInternal()
            startRecognizer(language = language, isWake = false)
        }
    }

    fun setWakeListeningEnabled(enabled: Boolean, language: AssistantLanguage = currentLanguage) {
        wakeModeActive = enabled
        currentLanguage = language
        mainHandler.post {
            if (enabled) {
                _isWakeListening.value = true
                if (!_isSpeaking.value && !_isListening.value) {
                    startRecognizer(language = language, isWake = true)
                }
            } else {
                _isWakeListening.value = false
                stopListeningInternal()
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            stopListeningInternal()
        }
    }

    private fun restartWakeListeningIfNeeded() {
        if (wakeModeActive && !_isListening.value && !_isSpeaking.value) {
            startRecognizer(language = currentLanguage, isWake = true)
        }
    }

    private fun startRecognizer(language: AssistantLanguage, isWake: Boolean) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _isListening.value = false
            _isWakeListening.value = false
            if (!isWake) {
                onSpeechErrorCallback?.invoke("Speech recognition service is unavailable on this device.")
            }
            return
        }
        try {
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer = recognizer
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = !isWake
                    _partialText.value = ""
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {
                    val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                    _rmsLevel.value = normalized
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _isListening.value = false
                    _rmsLevel.value = 0f
                }

                override fun onError(error: Int) {
                    _isListening.value = false
                    _rmsLevel.value = 0f
                    if (isWake && wakeModeActive) {
                        val delay = if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) 900L else 450L
                        mainHandler.postDelayed({ restartWakeListeningIfNeeded() }, delay)
                    } else if (!isWake) {
                        val msg = mapSpeechError(error)
                        if (msg != null) onSpeechErrorCallback?.invoke(msg)
                    }
                }

                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    _rmsLevel.value = 0f
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val best = matches?.firstOrNull().orEmpty().trim()
                    _partialText.value = best
                    if (best.isNotBlank()) {
                        onFinalTranscript?.invoke(best, isWake)
                    }
                    if (isWake && wakeModeActive && !_isSpeaking.value) {
                        mainHandler.postDelayed({ restartWakeListeningIfNeeded() }, 500L)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    if (partial.isNotBlank()) {
                        _partialText.value = partial
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.speechLocaleTag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            recognizer.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start SpeechRecognizer", e)
            _isListening.value = false
            if (!isWake) {
                onSpeechErrorCallback?.invoke("Could not activate microphone speech recognition.")
            }
        }
    }

    private fun stopListeningInternal() {
        runCatching {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        }
        speechRecognizer = null
        _isListening.value = false
        _rmsLevel.value = 0f
    }

    private fun mapSpeechError(error: Int): String? = when (error) {
        SpeechRecognizer.ERROR_NO_MATCH -> "I didn't catch that. Please try speaking again."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
            "Speech recognition network unavailable."
        else -> null
    }

    fun release() {
        wakeModeActive = false
        mainHandler.post {
            stopListeningInternal()
            tts?.stop()
            tts?.shutdown()
            tts = null
        }
    }
}

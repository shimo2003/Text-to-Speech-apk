package com.example.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.example.model.SupportedLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

sealed class TtsPlaybackState {
    data object Idle : TtsPlaybackState()
    data class Speaking(val utteranceId: String, val text: String) : TtsPlaybackState()
    data class Completed(val utteranceId: String) : TtsPlaybackState()
    data class Error(val message: String) : TtsPlaybackState()
}

class TextToSpeechHelper(
    private val context: Context,
    private val onInitComplete: ((Boolean) -> Unit)? = null
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _playbackState = MutableStateFlow<TtsPlaybackState>(TtsPlaybackState.Idle)
    val playbackState: StateFlow<TtsPlaybackState> = _playbackState.asStateFlow()

    private val _isArabicSupported = MutableStateFlow(false)
    val isArabicSupported: StateFlow<Boolean> = _isArabicSupported.asStateFlow()

    private val _engineName = MutableStateFlow("")
    val engineName: StateFlow<String> = _engineName.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<Voice>>(emptyList())
    val availableVoices: StateFlow<List<Voice>> = _availableVoices.asStateFlow()

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val engine = tts?.defaultEngine ?: "Default"
            _engineName.value = engine

            // Check Arabic availability
            val arabicStatus = tts?.isLanguageAvailable(Locale("ar", "SA")) ?: TextToSpeech.LANG_NOT_SUPPORTED
            val isSupported = (arabicStatus == TextToSpeech.LANG_AVAILABLE ||
                    arabicStatus == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
                    arabicStatus == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE)
            _isArabicSupported.value = isSupported

            // Load voices if available
            try {
                _availableVoices.value = tts?.voices?.toList() ?: emptyList()
            } catch (_: Exception) {
            }

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _playbackState.value = TtsPlaybackState.Speaking(utteranceId ?: "", "")
                }

                override fun onDone(utteranceId: String?) {
                    _playbackState.value = TtsPlaybackState.Completed(utteranceId ?: "")
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _playbackState.value = TtsPlaybackState.Error("فشل نطق النص / Failed to synthesize speech")
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    val msg = when (errorCode) {
                        TextToSpeech.ERROR_SYNTHESIS -> "خطأ في معالجة الصوت / Audio synthesis error"
                        TextToSpeech.ERROR_SERVICE -> "خدمة النطق غير مستجيبة / TTS service error"
                        TextToSpeech.ERROR_NETWORK -> "فشل تنزيل حزمة الصوت / Network voice data error"
                        TextToSpeech.ERROR_NOT_INSTALLED_YET -> "بيانات الصوت غير مثبتة / Voice data not installed"
                        else -> "خطأ في النطق ($errorCode) / TTS playback error"
                    }
                    _playbackState.value = TtsPlaybackState.Error(msg)
                }
            })

            _isInitialized.value = true
            onInitComplete?.invoke(true)
        } else {
            _isInitialized.value = false
            _playbackState.value = TtsPlaybackState.Error("فشل تهيئة محرك تحويل النص إلى صوت / Failed to initialize TTS")
            onInitComplete?.invoke(false)
        }
    }

    fun speak(
        text: String,
        language: SupportedLanguage,
        pitch: Float = 1.0f,
        speed: Float = 1.0f
    ) {
        val ttsInstance = tts ?: return
        if (!_isInitialized.value) {
            _playbackState.value = TtsPlaybackState.Error("محرك الصوت قيد التهيئة... الرجاء الانتظار / TTS initializing...")
            return
        }

        if (text.isBlank()) {
            _playbackState.value = TtsPlaybackState.Error("الرجاء إدخال نص لنطقه / Please enter text to speak")
            return
        }

        try {
            // Set language
            val langResult = ttsInstance.setLanguage(language.locale)
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Try fallback to base language (e.g. Locale("ar") instead of Locale("ar", "SA"))
                val baseLangResult = ttsInstance.setLanguage(Locale(language.locale.language))
                if (baseLangResult == TextToSpeech.LANG_MISSING_DATA || baseLangResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    _playbackState.value = TtsPlaybackState.Error(
                        "لغة ${language.nativeName} غير مثبتة في محرك الجهاز. اضغط الإعدادات لتثبيتها / Language not installed in TTS engine"
                    )
                    return
                }
            }

            ttsInstance.setPitch(pitch.coerceIn(0.5f, 2.0f))
            ttsInstance.setSpeechRate(speed.coerceIn(0.5f, 2.0f))

            val utteranceId = UUID.randomUUID().toString()
            _playbackState.value = TtsPlaybackState.Speaking(utteranceId, text)

            ttsInstance.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        } catch (e: Exception) {
            _playbackState.value = TtsPlaybackState.Error("خطأ في تشغيل الصوت: ${e.localizedMessage}")
        }
    }

    fun stop() {
        try {
            tts?.stop()
            _playbackState.value = TtsPlaybackState.Idle
        } catch (_: Exception) {
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            _isInitialized.value = false
            _playbackState.value = TtsPlaybackState.Idle
        } catch (_: Exception) {
        }
    }
}

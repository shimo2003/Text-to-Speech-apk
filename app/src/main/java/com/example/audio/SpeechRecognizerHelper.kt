package com.example.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class SpeechRecognizerState {
    data object Idle : SpeechRecognizerState()
    data class Listening(val rmsDb: Float = 0f) : SpeechRecognizerState()
    data object Processing : SpeechRecognizerState()
    data class PartialResult(val text: String) : SpeechRecognizerState()
    data class FinalResult(val text: String) : SpeechRecognizerState()
    data class Error(val message: String, val errorCode: Int) : SpeechRecognizerState()
}

class SpeechRecognizerHelper(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null

    private val _state = MutableStateFlow<SpeechRecognizerState>(SpeechRecognizerState.Idle)
    val state: StateFlow<SpeechRecognizerState> = _state.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _currentRms = MutableStateFlow(0f)
    val currentRms: StateFlow<Float> = _currentRms.asStateFlow()

    fun isRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    private fun ensureRecognizer(): Boolean {
        if (!isRecognitionAvailable()) {
            _state.value = SpeechRecognizerState.Error(
                "خدمة التعرف على الصوت غير متوفرة على هذا الجهاز / Speech recognition not available",
                -1
            )
            return false
        }

        if (speechRecognizer == null) {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createListener())
                }
            } catch (e: Exception) {
                _state.value = SpeechRecognizerState.Error(
                    "فشل تشغيل محرك الصوت: ${e.localizedMessage ?: "Unknown error"}",
                    -2
                )
                return false
            }
        }
        return true
    }

    fun startListening(languageCode: String) {
        if (!ensureRecognizer()) return

        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageCode)
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, languageCode)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }

            _isListening.value = true
            _state.value = SpeechRecognizerState.Listening(0f)
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _isListening.value = false
            _state.value = SpeechRecognizerState.Error(
                "خطأ عند بدء التسجيل / Error starting listener: ${e.localizedMessage}",
                -3
            )
        }
    }

    fun stopListening() {
        try {
            _isListening.value = false
            speechRecognizer?.stopListening()
            _state.value = SpeechRecognizerState.Processing
        } catch (_: Exception) {
        }
    }

    fun cancel() {
        try {
            _isListening.value = false
            _currentRms.value = 0f
            speechRecognizer?.cancel()
            _state.value = SpeechRecognizerState.Idle
        } catch (_: Exception) {
        }
    }

    fun destroy() {
        try {
            _isListening.value = false
            speechRecognizer?.destroy()
            speechRecognizer = null
            _state.value = SpeechRecognizerState.Idle
        } catch (_: Exception) {
        }
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _isListening.value = true
                _state.value = SpeechRecognizerState.Listening(0f)
            }

            override fun onBeginningOfSpeech() {
                _isListening.value = true
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Normalize between 0 and 12 dB
                val normalized = rmsdB.coerceAtLeast(0f)
                _currentRms.value = normalized
                if (_state.value is SpeechRecognizerState.Listening) {
                    _state.value = SpeechRecognizerState.Listening(normalized)
                }
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _isListening.value = false
                _currentRms.value = 0f
                _state.value = SpeechRecognizerState.Processing
            }

            override fun onError(error: Int) {
                _isListening.value = false
                _currentRms.value = 0f
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "خطأ في تسجيل الصوت / Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "خطأ من التطبيق / Client side error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "إذن الميكروفون غير ممنوح / Microphone permission needed"
                    SpeechRecognizer.ERROR_NETWORK -> "خطأ في الاتصال بالإنترنت / Network connection error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "انتهت مهلة الشبكة / Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "لم يتم التعرف على كلمات واضحة / No speech recognized. Try speaking louder."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "محرك الصوت مشغول، يرجى المحاولة ثانية / Speech recognizer busy"
                    SpeechRecognizer.ERROR_SERVER -> "خطأ في خادم الصوت / Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "لم يتم رصد صوت، حاول التحدث مجدداً / No speech detected"
                    else -> "خطأ غير متوقع ($error) / Speech error occurred"
                }
                _state.value = SpeechRecognizerState.Error(errorMessage, error)
            }

            override fun onResults(results: Bundle?) {
                _isListening.value = false
                _currentRms.value = 0f
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognizedText = matches?.firstOrNull() ?: ""
                if (recognizedText.isNotBlank()) {
                    _state.value = SpeechRecognizerState.FinalResult(recognizedText)
                } else {
                    _state.value = SpeechRecognizerState.Idle
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partialText = matches?.firstOrNull() ?: ""
                if (partialText.isNotBlank()) {
                    _state.value = SpeechRecognizerState.PartialResult(partialText)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }
}

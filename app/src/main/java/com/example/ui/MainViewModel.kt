package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SpeechRecognizerHelper
import com.example.audio.SpeechRecognizerState
import com.example.audio.TextToSpeechHelper
import com.example.audio.TtsPlaybackState
import com.example.data.AppDatabase
import com.example.data.SpeechHistoryEntity
import com.example.model.SupportedLanguage
import com.example.util.ArabicTextHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab {
    VOICE_TO_TEXT,
    TEXT_TO_SPEECH,
    HISTORY,
    SETTINGS
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val historyDao = database.speechHistoryDao()

    val speechRecognizerHelper = SpeechRecognizerHelper(application)
    val textToSpeechHelper = TextToSpeechHelper(application)

    // Current tab
    private val _currentTab = MutableStateFlow(AppTab.VOICE_TO_TEXT)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    // Selected Language (Default Arabic Saudi / Standard)
    private val _selectedLanguage = MutableStateFlow(SupportedLanguage.ARABIC_SA)
    val selectedLanguage: StateFlow<SupportedLanguage> = _selectedLanguage.asStateFlow()

    // Language modal dialog visibility
    private val _showLanguageDialog = MutableStateFlow(false)
    val showLanguageDialog: StateFlow<Boolean> = _showLanguageDialog.asStateFlow()

    // Voice to Text (STT) State
    private val _transcribedText = MutableStateFlow("")
    val transcribedText: StateFlow<String> = _transcribedText.asStateFlow()

    private val _sttStatusMessage = MutableStateFlow("جاهز للتسجيل • Tap mic to speak")
    val sttStatusMessage: StateFlow<String> = _sttStatusMessage.asStateFlow()

    private val _recordingDuration = MutableStateFlow(0)
    val recordingDuration: StateFlow<Int> = _recordingDuration.asStateFlow()

    private var timerJob: Job? = null

    // Text to Speech (TTS) State
    private val _ttsInputText = MutableStateFlow("مَرْحَبًا بِكُمْ فِي اسْتُودْيُو الصَّوْتِ الذَّكِيِّ.")
    val ttsInputText: StateFlow<String> = _ttsInputText.asStateFlow()

    private val _ttsPitch = MutableStateFlow(1.0f)
    val ttsPitch: StateFlow<Float> = _ttsPitch.asStateFlow()

    private val _ttsSpeed = MutableStateFlow(1.0f)
    val ttsSpeed: StateFlow<Float> = _ttsSpeed.asStateFlow()

    // History Flow from Room
    val historyItems: StateFlow<List<SpeechHistoryEntity>> = historyDao.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // History filter: "ALL", "VTT", "TTS"
    private val _historyFilter = MutableStateFlow("ALL")
    val historyFilter: StateFlow<String> = _historyFilter.asStateFlow()

    // History search query
    private val _historySearchQuery = MutableStateFlow("")
    val historySearchQuery: StateFlow<String> = _historySearchQuery.asStateFlow()

    // User message events (snackbars)
    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    init {
        // Collect speech recognizer states
        viewModelScope.launch {
            speechRecognizerHelper.state.collect { state ->
                when (state) {
                    is SpeechRecognizerState.Idle -> {
                        stopTimer()
                        _sttStatusMessage.value = "جاهز للتسجيل • Tap mic to speak"
                    }
                    is SpeechRecognizerState.Listening -> {
                        startTimer()
                        _sttStatusMessage.value = "جاري الاستماع... تَحَدَّث الآن • Listening..."
                    }
                    is SpeechRecognizerState.Processing -> {
                        stopTimer()
                        _sttStatusMessage.value = "جاري معالجة الصوت... • Processing..."
                    }
                    is SpeechRecognizerState.PartialResult -> {
                        _transcribedText.value = state.text
                    }
                    is SpeechRecognizerState.FinalResult -> {
                        _transcribedText.value = state.text
                        _sttStatusMessage.value = "تم تحويل الصوت إلى نص بنجاح • Transcribed"
                        // Auto-save transcription to history
                        saveHistoryItem(
                            text = state.text,
                            type = "VTT",
                            language = _selectedLanguage.value
                        )
                    }
                    is SpeechRecognizerState.Error -> {
                        stopTimer()
                        _sttStatusMessage.value = state.message
                        _toastEvent.emit(state.message)
                    }
                }
            }
        }

        // Collect TTS states
        viewModelScope.launch {
            textToSpeechHelper.playbackState.collect { state ->
                when (state) {
                    is TtsPlaybackState.Error -> {
                        _toastEvent.emit(state.message)
                    }
                    is TtsPlaybackState.Completed -> {
                        _toastEvent.emit("اكتمل نطق النص • Speech finished")
                    }
                    else -> {}
                }
            }
        }
    }

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun setLanguage(language: SupportedLanguage) {
        _selectedLanguage.value = language
        viewModelScope.launch {
            _toastEvent.emit("تم تغيير اللغة إلى ${language.nativeName}")
        }
    }

    fun openLanguageDialog() {
        _showLanguageDialog.value = true
    }

    fun closeLanguageDialog() {
        _showLanguageDialog.value = false
    }

    // Voice to Text actions
    fun toggleRecording() {
        if (speechRecognizerHelper.isListening.value) {
            speechRecognizerHelper.stopListening()
        } else {
            speechRecognizerHelper.startListening(_selectedLanguage.value.code)
        }
    }

    fun clearTranscribedText() {
        _transcribedText.value = ""
        speechRecognizerHelper.cancel()
        _sttStatusMessage.value = "تم المسح • Cleared"
    }

    fun removeDiacriticsFromVtt() {
        if (_transcribedText.value.isNotBlank()) {
            _transcribedText.value = ArabicTextHelper.removeDiacritics(_transcribedText.value)
            viewModelScope.launch {
                _toastEvent.emit("تمت إزالة التشكيل • Diacritics removed")
            }
        }
    }

    fun sendVttToTts() {
        if (_transcribedText.value.isNotBlank()) {
            _ttsInputText.value = _transcribedText.value
            _currentTab.value = AppTab.TEXT_TO_SPEECH
            // Speak immediately
            speakText(_transcribedText.value)
        } else {
            viewModelScope.launch {
                _toastEvent.emit("لا يوجد نص للتحدث به • No text transcribed yet")
            }
        }
    }

    // Text to Speech actions
    fun updateTtsInput(text: String) {
        _ttsInputText.value = text
    }

    fun setPitch(pitch: Float) {
        _ttsPitch.value = pitch
    }

    fun setSpeed(speed: Float) {
        _ttsSpeed.value = speed
    }

    fun speakText(textToSpeak: String = _ttsInputText.value) {
        if (textToSpeak.isBlank()) {
            viewModelScope.launch {
                _toastEvent.emit("الرجاء كتابة نص أولاً • Please enter text")
            }
            return
        }

        textToSpeechHelper.speak(
            text = textToSpeak,
            language = _selectedLanguage.value,
            pitch = _ttsPitch.value,
            speed = _ttsSpeed.value
        )

        // Save to history
        saveHistoryItem(
            text = textToSpeak,
            type = "TTS",
            language = _selectedLanguage.value,
            pitch = _ttsPitch.value,
            speed = _ttsSpeed.value
        )
    }

    fun stopSpeaking() {
        textToSpeechHelper.stop()
    }

    fun applySamplePhrase(phrase: String) {
        _ttsInputText.value = phrase
        speakText(phrase)
    }

    fun removeDiacriticsFromTts() {
        if (_ttsInputText.value.isNotBlank()) {
            _ttsInputText.value = ArabicTextHelper.removeDiacritics(_ttsInputText.value)
            viewModelScope.launch {
                _toastEvent.emit("تمت إزالة التشكيل • Diacritics removed")
            }
        }
    }

    fun clearTtsInput() {
        _ttsInputText.value = ""
        textToSpeechHelper.stop()
    }

    // History actions
    fun setHistoryFilter(filter: String) {
        _historyFilter.value = filter
    }

    fun setHistorySearchQuery(query: String) {
        _historySearchQuery.value = query
    }

    fun saveHistoryItem(
        text: String,
        type: String,
        language: SupportedLanguage,
        pitch: Float = 1.0f,
        speed: Float = 1.0f
    ) {
        if (text.isBlank()) return
        viewModelScope.launch {
            val entity = SpeechHistoryEntity(
                text = text.trim(),
                type = type,
                languageCode = language.code,
                languageName = language.nativeName,
                timestamp = System.currentTimeMillis(),
                pitch = pitch,
                speed = speed
            )
            historyDao.insert(entity)
        }
    }

    fun deleteHistoryItem(item: SpeechHistoryEntity) {
        viewModelScope.launch {
            historyDao.delete(item)
            _toastEvent.emit("تم حذف العنصر • Deleted")
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            historyDao.clearAll()
            _toastEvent.emit("تم مسح السجل بالكامل • History cleared")
        }
    }

    fun playHistoryItem(item: SpeechHistoryEntity) {
        val lang = SupportedLanguage.findByCode(item.languageCode)
        textToSpeechHelper.speak(
            text = item.text,
            language = lang,
            pitch = item.pitch,
            speed = item.speed
        )
    }

    private fun startTimer() {
        _recordingDuration.value = 0
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _recordingDuration.value += 1
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizerHelper.destroy()
        textToSpeechHelper.shutdown()
        stopTimer()
    }
}

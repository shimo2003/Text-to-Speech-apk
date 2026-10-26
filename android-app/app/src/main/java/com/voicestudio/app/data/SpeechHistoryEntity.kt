package com.voicestudio.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "speech_history")
data class SpeechHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val text: String,
    val type: String, // "VTT" or "TTS"
    val languageCode: String,
    val languageName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val pitch: Float = 1.0f,
    val speed: Float = 1.0f
)

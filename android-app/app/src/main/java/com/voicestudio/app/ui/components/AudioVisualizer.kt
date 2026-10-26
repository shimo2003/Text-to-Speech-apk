package com.voicestudio.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.voicestudio.app.ui.theme.AccentAmber
import com.voicestudio.app.ui.theme.AccentCyan
import com.voicestudio.app.ui.theme.PrimaryIndigo
import kotlin.math.sin

@Composable
fun AudioVisualizer(
    isListening: Boolean,
    isSpeaking: Boolean,
    rmsDb: Float = 0f,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "audio_bars")

    // Smooth idle/active animation phase
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    // Pulse factor
    val pulse by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .testTag("audio_visualizer")
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val width = size.width
            val height = size.height
            val barCount = 28
            val spacing = 6f
            val totalSpacing = spacing * (barCount - 1)
            val barWidth = ((width - 40f) - totalSpacing) / barCount
            val startX = 20f

            for (i in 0 until barCount) {
                val x = startX + i * (barWidth + spacing)
                val normalizedIndex = i.toFloat() / barCount.toFloat()

                // Calculate dynamic height based on audio state
                val barHeight = when {
                    isListening -> {
                        // Responds to microphone amplitude + animated wave
                        val wave = (sin(normalizedIndex * 4f + phase) + 1f) / 2f
                        val amplitudeBoost = (rmsDb / 10f).coerceIn(0.1f, 1.2f)
                        val h = height * (0.15f + wave * 0.45f * amplitudeBoost * pulse)
                        h.coerceIn(8f, height * 0.88f)
                    }
                    isSpeaking -> {
                        // Smooth rhythmic voice synthesis wave
                        val wave1 = sin(normalizedIndex * 5f + phase)
                        val wave2 = sin(normalizedIndex * 9f - phase * 1.3f)
                        val combined = ((wave1 + wave2) / 2f + 1f) / 2f
                        val h = height * (0.2f + combined * 0.65f * pulse)
                        h.coerceIn(10f, height * 0.90f)
                    }
                    else -> {
                        // Gentle ambient idle ripples
                        val subtleWave = (sin(normalizedIndex * 3f + phase * 0.5f) + 1f) / 2f
                        val h = height * (0.08f + subtleWave * 0.12f)
                        h.coerceIn(6f, 16f)
                    }
                }

                val top = (height - barHeight) / 2f

                val barColor = when {
                    isListening -> {
                        Brush.verticalGradient(
                            colors = listOf(AccentCyan, AccentAmber),
                            startY = top,
                            endY = top + barHeight
                        )
                    }
                    isSpeaking -> {
                        Brush.verticalGradient(
                            colors = listOf(PrimaryIndigo, AccentCyan),
                            startY = top,
                            endY = top + barHeight
                        )
                    }
                    else -> {
                        Brush.verticalGradient(
                            colors = listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.10f)),
                            startY = top,
                            endY = top + barHeight
                        )
                    }
                }

                drawRoundRect(
                    brush = barColor,
                    topLeft = Offset(x, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                )
            }
        }
    }
}

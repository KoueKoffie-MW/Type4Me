package com.transcriptor.hid.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.transcriptor.hid.ui.theme.ElectricViolet
import kotlin.math.sin

/**
 * 7-bar dynamic audio equalizer waveform that animates smoothly in response to live microphone RMS levels.
 */
@Composable
fun AudioWaveformVisualizer(
    audioLevel: Float,
    isRecording: Boolean,
    modifier: Modifier = Modifier,
    maxHeight: Dp = 24.dp
) {
    val barCount = 7
    // Stagger weights for organic central bell curve
    val barWeights = listOf(0.4f, 0.7f, 0.9f, 1.0f, 0.9f, 0.7f, 0.4f)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val weight = barWeights[i]
            val targetFraction = if (isRecording) {
                val wave = (sin(System.currentTimeMillis() / 150.0 + i * 0.8).toFloat() + 1f) * 0.15f
                (audioLevel * weight + wave).coerceIn(0.12f, 1.0f)
            } else {
                0.12f
            }

            val animatedFraction by animateFloatAsState(
                targetValue = targetFraction,
                animationSpec = spring(stiffness = 800f),
                label = "bar_$i"
            )

            val barHeight = maxHeight * animatedFraction

            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (isRecording) ElectricViolet else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
            )
        }
    }
}

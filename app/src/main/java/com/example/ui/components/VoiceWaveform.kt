package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BossCyanPrimary
import com.example.ui.theme.BossGoldSecondary

@Composable
fun VoiceWaveform(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    barsCount: Int = 18
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val animatedProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "progress"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
    ) {
        val width = size.width
        val height = size.height
        val barWidth = width / (barsCount * 1.8f)
        val space = (width - (barsCount * barWidth)) / (barsCount - 1).coerceAtLeast(1)

        val brush = Brush.verticalGradient(
            colors = listOf(BossCyanPrimary, BossGoldSecondary)
        )

        for (i in 0 until barsCount) {
            val factor = if (isActive) {
                val waveOffset = (i.toFloat() / barsCount + animatedProgress) % 1f
                val dynamicHeight = Math.sin(waveOffset * Math.PI).toFloat() * 0.8f + 0.2f
                dynamicHeight.coerceIn(0.15f, 1.0f)
            } else {
                0.1f
            }

            val barHeight = height * factor
            val x = i * (barWidth + space)
            val y = (height - barHeight) / 2

            drawRoundRect(
                brush = brush,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
        }
    }
}

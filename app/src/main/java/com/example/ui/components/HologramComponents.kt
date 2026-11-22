package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.memory.JaniLanguage
import com.example.ui.theme.HoloBorder
import com.example.ui.theme.HoloCyan
import com.example.ui.theme.HoloGlass
import com.example.ui.theme.HoloMint
import com.example.ui.theme.HoloObsidian
import com.example.ui.theme.HoloViolet
import com.example.ui.theme.HoloVoid
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HolographicBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(HoloVoid, HoloObsidian, HoloVoid)
                )
            )

            // Subtle ambient neural glow in top-right and bottom-left
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(HoloCyan.copy(alpha = 0.11f), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.14f),
                    radius = size.maxDimension * 0.5f
                ),
                center = Offset(size.width * 0.85f, size.height * 0.14f),
                radius = size.maxDimension * 0.5f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(HoloViolet.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(size.width * 0.15f, size.height * 0.82f),
                    radius = size.maxDimension * 0.55f
                ),
                center = Offset(size.width * 0.15f, size.height * 0.82f),
                radius = size.maxDimension * 0.55f
            )

            // Precision HUD grid lines
            val stepPx = 44.dp.toPx()
            val gridColor = HoloCyan.copy(alpha = 0.045f)
            var x = 0f
            while (x <= size.width) {
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f
                )
                x += stepPx
            }
            var y = 0f
            while (y <= size.height) {
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
                y += stepPx
            }
        }
        content()
    }
}

@Composable
fun HolographicOrb(
    isListening: Boolean,
    isThinking: Boolean,
    isSpeaking: Boolean,
    audioLevel: Float,
    modifier: Modifier = Modifier,
    size: Dp = 84.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_transition")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isThinking) 2200 else 7500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_rotation"
    )
    val idlePulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_pulse"
    )

    val smoothedAudio by animateFloatAsState(
        targetValue = if (isListening) audioLevel.coerceIn(0f, 1f) else if (isSpeaking) 0.55f else 0f,
        animationSpec = spring(),
        label = "orb_audio"
    )

    val activeColor = when {
        isListening -> HoloMint
        isThinking -> HoloViolet
        isSpeaking -> HoloCyan
        else -> HoloCyan
    }

    Canvas(modifier = modifier.size(size)) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val baseRadius = this.size.minDimension * 0.42f
        val dynamicBoost = 1f + smoothedAudio * 0.22f
        val coreRadius = baseRadius * 0.52f * idlePulse * dynamicBoost

        // Outer ambient halo
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(activeColor.copy(alpha = 0.34f), Color.Transparent),
                center = center,
                radius = baseRadius * 1.25f
            ),
            center = center,
            radius = baseRadius * 1.25f
        )

        // Outer dashed orbital ring
        drawCircle(
            color = activeColor.copy(alpha = 0.55f),
            radius = baseRadius * 0.96f,
            center = center,
            style = Stroke(
                width = 1.6.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), phase = rotationAngle * 0.4f)
            )
        )

        // Rotating sweeping arc
        drawArc(
            color = HoloViolet.copy(alpha = 0.85f),
            startAngle = rotationAngle,
            sweepAngle = 110f,
            useCenter = false,
            topLeft = Offset(center.x - baseRadius * 0.8f, center.y - baseRadius * 0.8f),
            size = androidx.compose.ui.geometry.Size(baseRadius * 1.6f, baseRadius * 1.6f),
            style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
        )

        // Counter-rotating cyan arc
        drawArc(
            color = activeColor,
            startAngle = -rotationAngle * 1.3f,
            sweepAngle = 85f,
            useCenter = false,
            topLeft = Offset(center.x - baseRadius * 0.66f, center.y - baseRadius * 0.66f),
            size = androidx.compose.ui.geometry.Size(baseRadius * 1.32f, baseRadius * 1.32f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        // Glowing core sphere
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, activeColor, HoloViolet.copy(alpha = 0.8f)),
                center = center,
                radius = coreRadius
            ),
            radius = coreRadius,
            center = center
        )

        // Satellite node on outer ring
        val rad = Math.toRadians(rotationAngle.toDouble())
        val nodeOffset = Offset(
            x = center.x + (baseRadius * 0.96f * cos(rad)).toFloat(),
            y = center.y + (baseRadius * 0.96f * sin(rad)).toFloat()
        )
        drawCircle(
            color = Color.White,
            radius = 2.8.dp.toPx(),
            center = nodeOffset
        )
    }
}

@Composable
fun HoloGlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color = HoloBorder,
    containerColor: Color = HoloGlass.copy(alpha = 0.78f),
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 2.dp
    ) {
        content()
    }
}

@Composable
fun LanguageSelectorRow(
    selectedLanguage: JaniLanguage,
    onSelectLanguage: (JaniLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        JaniLanguage.entries.forEach { lang ->
            val selected = lang == selectedLanguage
            FilterChip(
                selected = selected,
                onClick = { onSelectLanguage(lang) },
                label = {
                    Text(
                        text = lang.label,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                    containerColor = HoloGlass.copy(alpha = 0.6f),
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    borderColor = HoloBorder,
                    selectedBorderColor = HoloCyan
                ),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("lang_chip_${lang.code.lowercase()}")
            )
        }
    }
}

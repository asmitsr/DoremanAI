package com.sneha.ai.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin

/**
 * v1.2.0 — ORIGINAL robot-companion animation.
 *
 * This is deliberately NOT a Doraemon replica: no blue/white Doraemon face,
 * no red nose, no bell, no pocket and no Doraemon artwork. It is an original
 * round robot head drawn in the app's own theme colours (including the new
 * Midnight Indigo / Vanilla scheme), with blinking eyes, a speaking mouth,
 * robot ears/antennae and orbiting rings that react to voice level.
 *
 * Signature is unchanged (phase, level, reduceMotion, modifier) so
 * HomeScreen.kt compiles without changes.
 */
@Composable
fun Orb(phase: Phase, level: Float, reduceMotion: Boolean, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme

    // Idle follows the current theme; the other phases keep distinct,
    // semantic state colours so Listening / Thinking / Speaking /
    // Executing stay recognisable in every theme.
    val primaryTarget = when (phase) {
        Phase.Idle -> scheme.primary
        Phase.Listening -> Color(0xFF00D2FF)
        Phase.Thinking -> Color(0xFFC084FC)
        Phase.Speaking -> Color(0xFFF472B6)
        Phase.Executing -> Color(0xFF10B981)
    }
    val secondaryTarget = when (phase) {
        Phase.Idle -> scheme.secondary
        Phase.Listening -> Color(0xFF3B82F6)
        Phase.Thinking -> Color(0xFFEC4899)
        Phase.Speaking -> Color(0xFF8B5CF6)
        Phase.Executing -> Color(0xFF06B6D4)
    }

    val coreColor by animateColorAsState(primaryTarget, tween(500), label = "coreColor")
    val accentColor by animateColorAsState(secondaryTarget, tween(500), label = "accentColor")
    val lv by animateFloatAsState(level.coerceIn(0f, 1f), tween(120), label = "voiceLevel")

    val active = phase != Phase.Idle && !reduceMotion
    // All three animations are created unconditionally (Compose requires
    // stable animation calls); their values are ignored when idle or when
    // reduceMotion is on, so a static frame is still shown in those cases.
    val inf = rememberInfiniteTransition(label = "orbInfinite")
    val pulseRaw = inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1350, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    ).value
    val angleRaw = inf.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(7200, easing = LinearEasing), RepeatMode.Restart),
        label = "angle"
    ).value
    val blinkRaw = inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "blink"
    ).value
    val pulse = if (active) pulseRaw else 0f
    val angle = if (active) angleRaw else 25f
    // Slow blink: mostly open, briefly closed, even while idle.
    val blink = if (reduceMotion) 0f else if (blinkRaw > 0.92f) (blinkRaw - 0.92f) / 0.08f else 0f

    Canvas(modifier) {
        val c = center
        val minDim = size.minDimension
        val baseRadius = minDim * 0.25f
        val dynamicRadius = baseRadius * (1f + 0.08f * pulse + 0.30f * lv)
        val r = dynamicRadius

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    coreColor.copy(alpha = 0.32f + 0.12f * pulse),
                    accentColor.copy(alpha = 0.12f),
                    Color.Transparent
                ),
                center = c,
                radius = r * 2.45f
            ),
            radius = r * 2.45f,
            center = c
        )

        if (phase == Phase.Listening || phase == Phase.Speaking) {
            val waveRadius = r * (1.35f + 0.28f * lv + 0.10f * pulse)
            drawCircle(
                color = coreColor.copy(alpha = (0.35f - 0.15f * pulse).coerceIn(0.08f, 0.45f)),
                radius = waveRadius,
                center = c,
                style = Stroke(width = minDim * 0.012f)
            )
        }

        val orbitRadius1 = r * 1.32f
        val arcSize1 = Size(orbitRadius1 * 2f, orbitRadius1 * 2f)
        val topLeft1 = Offset(c.x - orbitRadius1, c.y - orbitRadius1)
        rotate(degrees = angle, pivot = c) {
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        Color.Transparent,
                        coreColor.copy(alpha = 0.70f),
                        accentColor.copy(alpha = 0.85f),
                        Color.Transparent
                    ),
                    center = c
                ),
                startAngle = 15f,
                sweepAngle = 135f,
                useCenter = false,
                topLeft = topLeft1,
                size = arcSize1,
                style = Stroke(width = minDim * 0.016f, cap = StrokeCap.Round)
            )
            drawArc(
                color = coreColor.copy(alpha = 0.45f),
                startAngle = 195f,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = topLeft1,
                size = arcSize1,
                style = Stroke(width = minDim * 0.010f, cap = StrokeCap.Round)
            )
        }

        val orbitRadius2 = r * 1.15f
        val arcSize2 = Size(orbitRadius2 * 2f, orbitRadius2 * 2f)
        val topLeft2 = Offset(c.x - orbitRadius2, c.y - orbitRadius2)
        rotate(degrees = -angle * 1.4f, pivot = c) {
            drawArc(
                color = accentColor.copy(alpha = if (phase == Phase.Idle) 0.28f else 0.65f),
                startAngle = 60f,
                sweepAngle = 95f,
                useCenter = false,
                topLeft = topLeft2,
                size = arcSize2,
                style = Stroke(width = minDim * 0.011f, cap = StrokeCap.Round)
            )
            drawArc(
                color = Color.White.copy(alpha = if (phase == Phase.Idle) 0.18f else 0.45f),
                startAngle = 240f,
                sweepAngle = 80f,
                useCenter = false,
                topLeft = topLeft2,
                size = arcSize2,
                style = Stroke(width = minDim * 0.011f, cap = StrokeCap.Round)
            )
        }

        val rad = Math.toRadians(angle.toDouble())
        val dotOffset = Offset(
            x = c.x + (orbitRadius1 * cos(rad)).toFloat(),
            y = c.y + (orbitRadius1 * sin(rad)).toFloat()
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.85f),
            radius = minDim * 0.018f,
            center = dotOffset
        )

        // Robot ears / antennae (original shape, not cat ears).
        val earY = c.y - r * 0.92f
        listOf(-0.62f, 0.62f).forEach { dx ->
            val ex = c.x + r * dx
            drawCircle(color = accentColor, radius = r * 0.16f, center = Offset(ex, earY))
            drawCircle(color = scheme.onBackground.copy(alpha = 0.9f), radius = r * 0.05f, center = Offset(ex, earY - r * 0.18f))
        }

        // Core robot head.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.95f),
                    coreColor,
                    accentColor.copy(alpha = 0.88f),
                    coreColor.copy(alpha = 0.35f)
                ),
                center = c - Offset(r * 0.28f, r * 0.28f),
                radius = r * 1.35f
            ),
            radius = r,
            center = c
        )

        // Face plate in a light tone so eyes/mouth read on any theme.
        drawCircle(
            color = Color.White.copy(alpha = 0.22f),
            radius = r * 0.78f,
            center = Offset(c.x, c.y + r * 0.08f)
        )

        // Blinking eyes.
        val eyeOpen = (1f - blink * 0.9f).coerceIn(0.1f, 1f)
        val eyeW = r * 0.24f
        val eyeH = r * 0.28f * eyeOpen
        listOf(-0.36f, 0.36f).forEach { dx ->
            val ex = c.x + r * dx
            val ey = c.y - r * 0.18f
            drawOval(
                color = Color.White,
                topLeft = Offset(ex - eyeW, ey - eyeH),
                size = Size(eyeW * 2f, eyeH * 2f)
            )
            drawCircle(
                color = Color(0xFF1E2140),
                radius = r * 0.11f,
                center = Offset(ex, ey)
            )
            drawCircle(
                color = Color.White,
                radius = r * 0.035f,
                center = Offset(ex + r * 0.04f, ey - r * 0.04f)
            )
        }

        // Small round nose (theme accent, not a character-specific nose).
        drawCircle(color = accentColor, radius = r * 0.07f, center = Offset(c.x, c.y + r * 0.10f))

        // Mouth: opens with voice level while Speaking, smiles otherwise.
        if (phase == Phase.Speaking) {
            val mh = (r * 0.10f + r * 0.28f * lv).coerceAtLeast(r * 0.08f)
            drawOval(
                color = Color(0xFF1E2140).copy(alpha = 0.9f),
                topLeft = Offset(c.x - r * 0.18f, c.y + r * 0.28f),
                size = Size(r * 0.36f, mh * 2f)
            )
        } else {
            drawArc(
                color = Color(0xFF1E2140).copy(alpha = 0.85f),
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(c.x - r * 0.30f, c.y + r * 0.08f),
                size = Size(r * 0.60f, r * 0.55f),
                style = Stroke(width = minDim * 0.010f, cap = StrokeCap.Round)
            )
        }

        // Soft cheeks.
        listOf(-0.58f, 0.58f).forEach { dx ->
            drawCircle(
                color = scheme.tertiary.copy(alpha = 0.35f),
                radius = r * 0.11f,
                center = Offset(c.x + r * dx, c.y + r * 0.28f)
            )
        }

        // Gloss highlight.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.55f), Color.Transparent),
                center = c - Offset(r * 0.35f, r * 0.35f),
                radius = r * 0.55f
            ),
            radius = r * 0.55f,
            center = c - Offset(r * 0.35f, r * 0.35f)
        )
    }
}

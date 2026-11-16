package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AnimationIntensity
import com.example.data.model.CoreDesignStyle
import com.example.data.model.RoseCoreState
import com.example.ui.theme.LocalRoseAccents
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusOffline
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RoseCoreVisualizer(
    coreState: RoseCoreState,
    rmsLevel: Float,
    designStyle: CoreDesignStyle,
    animationIntensity: AnimationIntensity,
    onTapCore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accents = LocalRoseAccents.current
    val intensity = animationIntensity.multiplier

    val primaryColor by animateColorAsState(
        targetValue = when (coreState) {
            RoseCoreState.IDLE -> accents.primaryGlow
            RoseCoreState.LISTENING -> accents.secondaryGlow
            RoseCoreState.THINKING -> accents.tertiaryGlow
            RoseCoreState.SPEAKING -> accents.primaryGlow
            RoseCoreState.EXECUTING -> accents.secondaryGlow
            RoseCoreState.ERROR -> StatusError
            RoseCoreState.OFFLINE -> StatusOffline
        },
        animationSpec = tween(420),
        label = "corePrimaryColor"
    )

    val secondaryColor by animateColorAsState(
        targetValue = when (coreState) {
            RoseCoreState.ERROR -> Color(0xFFFF9859)
            RoseCoreState.OFFLINE -> Color(0xFF475275)
            RoseCoreState.THINKING -> accents.secondaryGlow
            else -> accents.secondaryGlow
        },
        animationSpec = tween(420),
        label = "coreSecondaryColor"
    )

    val smoothRms by animateFloatAsState(
        targetValue = when (coreState) {
            RoseCoreState.LISTENING -> 0.25f + (rmsLevel * 0.75f)
            RoseCoreState.SPEAKING -> 0.55f
            RoseCoreState.EXECUTING -> 0.45f
            else -> 0.1f
        },
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 320f),
        label = "smoothRms"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "roseCoreInfinite")
    val slowOrbit by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (11000 / intensity).toInt().coerceAtLeast(3500), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "slowOrbit"
    )
    val fastOrbit by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (4200 / intensity).toInt().coerceAtLeast(1500), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fastOrbit"
    )
    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulsePhase"
    )

    Box(
        modifier = modifier
            .size(276.dp)
            .clip(CircleShape)
            .clickable(onClick = onTapCore)
            .testTag("rose_core_orb"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.minDimension / 2f
            val breath = 1f + (pulsePhase - 0.5f) * 0.06f * intensity + (smoothRms * 0.12f)

            // 1. Deep atmospheric halo glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.34f + smoothRms * 0.16f),
                        secondaryColor.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxRadius * 0.98f
                ),
                radius = maxRadius * 0.98f,
                center = center
            )

            // 2. Orbiting energy particles
            val particleCount = if (animationIntensity == AnimationIntensity.MINIMAL) 8 else 16
            for (i in 0 until particleCount) {
                val angleDeg = slowOrbit + (i * (360f / particleCount))
                val rad = angleDeg * (PI / 180.0)
                val orbitR = maxRadius * (0.76f + (i % 3) * 0.06f)
                val px = center.x + (cos(rad) * orbitR).toFloat()
                val py = center.y + (sin(rad) * orbitR).toFloat()
                val dotRadius = if (i % 2 == 0) 2.6.dp.toPx() else 1.7.dp.toPx()
                drawCircle(
                    color = if (i % 2 == 0) secondaryColor.copy(alpha = 0.72f) else primaryColor.copy(alpha = 0.65f),
                    radius = dotRadius,
                    center = Offset(px, py)
                )
            }

            // 3. Outer state-reactive ring / waveform
            when (coreState) {
                RoseCoreState.LISTENING, RoseCoreState.SPEAKING -> {
                    drawAudioWaveformRing(
                        center = center,
                        baseRadius = maxRadius * 0.64f,
                        amplitude = (14.dp.toPx() * smoothRms) + (pulsePhase * 6.dp.toPx()),
                        phase = slowOrbit * (PI.toFloat() / 180f) * 3f,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor
                    )
                }
                RoseCoreState.THINKING, RoseCoreState.EXECUTING -> {
                    val speedAngle = fastOrbit * 1.8f
                    rotate(speedAngle, pivot = center) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(primaryColor, secondaryColor, Color.Transparent, primaryColor),
                                center = center
                            ),
                            startAngle = 0f,
                            sweepAngle = 250f,
                            useCenter = false,
                            topLeft = Offset(center.x - maxRadius * 0.72f, center.y - maxRadius * 0.72f),
                            size = Size(maxRadius * 1.44f, maxRadius * 1.44f),
                            style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
                else -> {
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.28f),
                        radius = maxRadius * 0.72f * breath,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }

            // 4. Core architectural geometry based on user-selected Design Style
            when (designStyle) {
                CoreDesignStyle.QUANTUM_LOTUS -> {
                    drawQuantumLotus(
                        center = center,
                        radius = maxRadius * 0.56f * breath,
                        rotation = slowOrbit,
                        counterRotation = fastOrbit,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor
                    )
                }
                CoreDesignStyle.ORBITAL_RINGS -> {
                    drawOrbitalRings(
                        center = center,
                        radius = maxRadius * 0.58f * breath,
                        rotation = slowOrbit,
                        counterRotation = fastOrbit,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor
                    )
                }
                CoreDesignStyle.PULSE_MATRIX -> {
                    drawPulseMatrix(
                        center = center,
                        radius = maxRadius * 0.58f * breath,
                        rotation = slowOrbit,
                        pulse = pulsePhase,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor
                    )
                }
            }

            // 5. Inner glassmorphic core nucleus
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.38f),
                        Color(0xFF0A0E1E).copy(alpha = 0.92f)
                    ),
                    center = center,
                    radius = maxRadius * 0.36f
                ),
                radius = maxRadius * 0.36f,
                center = center
            )
            drawCircle(
                color = primaryColor.copy(alpha = 0.75f),
                radius = maxRadius * 0.36f,
                center = center,
                style = Stroke(width = 1.8.dp.toPx())
            )
        }

        // Center crisp status readout
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(Color(0x66090D1A))
                .border(1.dp, primaryColor.copy(alpha = 0.4f), CircleShape)
                .padding(8.dp)
        ) {
            Text(
                text = "ROSE",
                style = MaterialTheme.typography.titleLarge.copy(
                    letterSpacing = 3.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
            Text(
                text = coreState.name,
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.4.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = primaryColor
            )
        }
    }
}

private fun DrawScope.drawAudioWaveformRing(
    center: Offset,
    baseRadius: Float,
    amplitude: Float,
    phase: Float,
    primaryColor: Color,
    secondaryColor: Color
) {
    val bars = 48
    for (i in 0 until bars) {
        val angle = (i.toFloat() / bars) * (2f * PI.toFloat())
        val wave = (sin(angle * 6f + phase) * 0.5f + 0.5f) * amplitude
        val startR = baseRadius
        val endR = baseRadius + 4.dp.toPx() + wave
        val start = Offset(center.x + cos(angle) * startR, center.y + sin(angle) * startR)
        val end = Offset(center.x + cos(angle) * endR, center.y + sin(angle) * endR)
        drawLine(
            color = if (i % 2 == 0) secondaryColor.copy(alpha = 0.85f) else primaryColor.copy(alpha = 0.85f),
            start = start,
            end = end,
            strokeWidth = 2.6.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawQuantumLotus(
    center: Offset,
    radius: Float,
    rotation: Float,
    counterRotation: Float,
    primaryColor: Color,
    secondaryColor: Color
) {
    val petals = 8
    rotate(rotation, pivot = center) {
        for (i in 0 until petals) {
            rotate(i * (360f / petals), pivot = center) {
                val path = Path().apply {
                    moveTo(center.x, center.y - radius * 0.4f)
                    quadraticTo(
                        center.x + radius * 0.34f,
                        center.y - radius * 0.88f,
                        center.x,
                        center.y - radius
                    )
                    quadraticTo(
                        center.x - radius * 0.34f,
                        center.y - radius * 0.88f,
                        center.x,
                        center.y - radius * 0.4f
                    )
                }
                drawPath(
                    path = path,
                    color = if (i % 2 == 0) primaryColor.copy(alpha = 0.55f) else secondaryColor.copy(alpha = 0.45f),
                    style = Stroke(width = 1.6.dp.toPx())
                )
            }
        }
    }
    rotate(counterRotation, pivot = center) {
        drawArc(
            color = secondaryColor.copy(alpha = 0.6f),
            startAngle = 20f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(center.x - radius * 0.86f, center.y - radius * 0.86f),
            size = Size(radius * 1.72f, radius * 1.72f),
            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
        )
        drawArc(
            color = primaryColor.copy(alpha = 0.7f),
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(center.x - radius * 0.86f, center.y - radius * 0.86f),
            size = Size(radius * 1.72f, radius * 1.72f),
            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

private fun DrawScope.drawOrbitalRings(
    center: Offset,
    radius: Float,
    rotation: Float,
    counterRotation: Float,
    primaryColor: Color,
    secondaryColor: Color
) {
    listOf(1.0f, 0.82f, 0.64f).forEachIndexed { idx, factor ->
        val r = radius * factor
        val angle = if (idx % 2 == 0) rotation else counterRotation
        rotate(angle + idx * 35f, pivot = center) {
            drawArc(
                color = if (idx % 2 == 0) primaryColor.copy(alpha = 0.78f) else secondaryColor.copy(alpha = 0.72f),
                startAngle = 0f,
                sweepAngle = 220f,
                useCenter = false,
                topLeft = Offset(center.x - r, center.y - r),
                size = Size(r * 2f, r * 2f),
                style = Stroke(width = (2.8f - idx * 0.4f).dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

private fun DrawScope.drawPulseMatrix(
    center: Offset,
    radius: Float,
    rotation: Float,
    pulse: Float,
    primaryColor: Color,
    secondaryColor: Color
) {
    val rings = 3
    for (ring in 1..rings) {
        val r = radius * (0.48f + ring * 0.18f) * (0.95f + pulse * 0.08f)
        drawCircle(
            color = if (ring % 2 == 0) secondaryColor.copy(alpha = 0.45f) else primaryColor.copy(alpha = 0.55f),
            radius = r,
            center = center,
            style = Stroke(width = 1.8.dp.toPx())
        )
    }
    val spokes = 12
    rotate(rotation, pivot = center) {
        for (i in 0 until spokes) {
            val rad = (i * (360f / spokes)) * (PI / 180.0)
            val inner = radius * 0.52f
            val outer = radius * (0.85f + (if (i % 2 == 0) pulse * 0.14f else 0f))
            drawLine(
                color = if (i % 2 == 0) primaryColor.copy(alpha = 0.7f) else secondaryColor.copy(alpha = 0.55f),
                start = Offset(center.x + (cos(rad) * inner).toFloat(), center.y + (sin(rad) * inner).toFloat()),
                end = Offset(center.x + (cos(rad) * outer).toFloat(), center.y + (sin(rad) * outer).toFloat()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

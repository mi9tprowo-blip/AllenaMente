package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalPillarColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class ParticleSpec(
    val angleRad: Float,
    val speed: Float,
    val sizeDp: Float,
    val shapeKind: Int, // 0=circle, 1=diamond, 2=rect
    val colorIdx: Int,
    val spinSpeed: Float
)

/**
 * 60fps geometric Canvas celebration burst (max 24 particles, 900ms, zero emojis).
 * Automatically unmounts when finished.
 */
@Composable
fun CelebrationParticlesCanvas(
    triggerKey: Int,
    modifier: Modifier = Modifier,
    onFinished: () -> Unit = {}
) {
    val pillarColors = LocalPillarColors.current
    val palette = remember(pillarColors) {
        listOf(
            pillarColors.logic,
            pillarColors.memory,
            pillarColors.reflex,
            pillarColors.empathy,
            pillarColors.streakIce
        )
    }
    val particles = remember(triggerKey) {
        val rng = Random(triggerKey * 9973 + 17)
        List(24) {
            ParticleSpec(
                angleRad = rng.nextFloat() * (2f * PI.toFloat()),
                speed = 0.25f + rng.nextFloat() * 0.55f,
                sizeDp = 5f + rng.nextFloat() * 6f,
                shapeKind = rng.nextInt(3),
                colorIdx = rng.nextInt(5),
                spinSpeed = (rng.nextFloat() - 0.5f) * 360f
            )
        }
    }

    val progress = remember(triggerKey) { Animatable(0f) }

    LaunchedEffect(triggerKey) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(durationMillis = 900, easing = LinearEasing))
        onFinished()
    }

    if (progress.value < 1f) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val t = progress.value
            val alpha = (1f - t).coerceIn(0f, 1f)
            val cx = size.width / 2f
            val cy = size.height * 0.35f
            val maxR = size.minDimension * 0.45f

            for (p in particles) {
                val dist = maxR * p.speed * t
                val gravityY = 140f * t * t
                val x = cx + cos(p.angleRad) * dist
                val y = cy + sin(p.angleRad) * dist + gravityY
                val sz = p.sizeDp.dp.toPx() * (1f - t * 0.35f)
                val color = palette[p.colorIdx % palette.size].copy(alpha = alpha)

                rotate(degrees = p.spinSpeed * t, pivot = Offset(x, y)) {
                    when (p.shapeKind) {
                        0 -> drawCircle(color = color, radius = sz * 0.5f, center = Offset(x, y))
                        1 -> {
                            val path = Path().apply {
                                moveTo(x, y - sz * 0.6f)
                                lineTo(x + sz * 0.5f, y)
                                lineTo(x, y + sz * 0.6f)
                                lineTo(x - sz * 0.5f, y)
                                close()
                            }
                            drawPath(path, color = color)
                        }
                        else -> drawRect(
                            color = color,
                            topLeft = Offset(x - sz * 0.5f, y - sz * 0.35f),
                            size = Size(sz, sz * 0.7f)
                        )
                    }
                }
            }
        }
    }
}

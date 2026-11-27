package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.engine.BrowStyle
import com.example.engine.EmotionSpec
import com.example.engine.EyeStyle
import com.example.engine.MouthStyle
import com.example.engine.PupilSize

/**
 * Fixes Section 8.1 UX bug: renders EVERY facial expression attribute explicitly:
 * - BrowStyle.FURROWED (angled inner knit + glabella vertical tension creases)
 * - BrowStyle.ASYMMETRIC (one brow arched, one compressed)
 * - BrowStyle.SAD_INNER (inner corners lifted obliquely)
 * - EyeStyle.SQUINT (compressed lower lid smiling/skeptical crescent + crow's feet)
 * - EyeStyle.DROOPING (heavy angled upper lid cover)
 * - EyeStyle.TENSE (flat narrowed upper & lower lid tension)
 * - EyeStyle.WIDE (exposed upper sclera)
 * - PupilSize.CONSTRICTED vs DILATED
 * - MouthStyle.SMIRK (true unilateral raised corner + dimple tension)
 * - Cheek tension lines + optional glowing Didactic Cue Rings during post-answer freeze!
 */
@Composable
fun ExpressiveFaceCanvas(
    spec: EmotionSpec,
    eyesOnlyMode: Boolean,
    showCueHighlights: Boolean,
    modifier: Modifier = Modifier
) {
    val skinTone = Color(0xFFF6D7C3)
    val skinShadow = Color(0xFFE0B298)
    val ink = Color(0xFF1E293B)
    val highlightAccent = MaterialTheme.colorScheme.primary
    val sclera = Color(0xFFFFFFFF)
    val irisColor = Color(0xFF334155)

    Canvas(
        modifier = modifier
            .size(if (eyesOnlyMode) 240.dp else 210.dp)
            .semantics {
                contentDescription = if (eyesOnlyMode) "Regione oculare espressiva" else "Volto espressivo"
            }
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        if (eyesOnlyMode) {
            // RMET-inspired horizontal eye-band portrait
            drawRoundRect(
                color = skinTone,
                topLeft = Offset(w * 0.05f, h * 0.22f),
                size = Size(w * 0.90f, h * 0.56f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(28.dp.toPx(), 28.dp.toPx())
            )
            drawRoundRect(
                color = ink.copy(alpha = 0.25f),
                topLeft = Offset(w * 0.05f, h * 0.22f),
                size = Size(w * 0.90f, h * 0.56f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(28.dp.toPx(), 28.dp.toPx()),
                style = Stroke(width = 2.dp.toPx())
            )
            // Nose bridge subtle shadow
            drawLine(
                color = skinShadow,
                start = Offset(cx, h * 0.44f),
                end = Offset(cx, h * 0.70f),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawBrows(cx, h * 0.36f, w * 0.20f, spec.brow, ink)
            drawEye(cx - w * 0.21f, h * 0.54f, w * 0.13f, spec.eye, spec.pupil, sclera, irisColor, ink, skinTone, isLeft = true)
            drawEye(cx + w * 0.21f, h * 0.54f, w * 0.13f, spec.eye, spec.pupil, sclera, irisColor, ink, skinTone, isLeft = false)

            if (showCueHighlights) {
                drawCircle(
                    color = highlightAccent,
                    radius = w * 0.21f,
                    center = Offset(cx - w * 0.21f, h * 0.48f),
                    style = Stroke(width = 2.5.dp.toPx())
                )
                drawCircle(
                    color = highlightAccent,
                    radius = w * 0.21f,
                    center = Offset(cx + w * 0.21f, h * 0.48f),
                    style = Stroke(width = 2.5.dp.toPx())
                )
            }
        } else {
            // Full expressive face
            val faceR = w * 0.43f
            drawCircle(color = skinTone, radius = faceR, center = Offset(cx, cy))
            drawCircle(
                color = ink.copy(alpha = 0.22f),
                radius = faceR,
                center = Offset(cx, cy),
                style = Stroke(width = 2.2.dp.toPx())
            )

            // Cheek tension arcs / smile lines
            if (spec.cheekTension) {
                val leftCheek = Path().apply {
                    moveTo(cx - faceR * 0.55f, cy + faceR * 0.12f)
                    quadraticTo(cx - faceR * 0.40f, cy + faceR * 0.02f, cx - faceR * 0.26f, cy + faceR * 0.14f)
                }
                val rightCheek = Path().apply {
                    moveTo(cx + faceR * 0.26f, cy + faceR * 0.14f)
                    quadraticTo(cx + faceR * 0.40f, cy + faceR * 0.02f, cx + faceR * 0.55f, cy + faceR * 0.12f)
                }
                drawPath(leftCheek, skinShadow, style = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round))
                drawPath(rightCheek, skinShadow, style = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round))
            }

            // Nose
            val nose = Path().apply {
                moveTo(cx, cy - faceR * 0.06f)
                lineTo(cx - faceR * 0.05f, cy + faceR * 0.14f)
                lineTo(cx + faceR * 0.05f, cy + faceR * 0.14f)
            }
            drawPath(nose, skinShadow, style = Stroke(2.4.dp.toPx(), cap = StrokeCap.Round))

            // Eyebrows & Eyes
            drawBrows(cx, cy - faceR * 0.34f, faceR * 0.34f, spec.brow, ink)
            drawEye(cx - faceR * 0.36f, cy - faceR * 0.13f, faceR * 0.20f, spec.eye, spec.pupil, sclera, irisColor, ink, skinTone, isLeft = true)
            drawEye(cx + faceR * 0.36f, cy - faceR * 0.13f, faceR * 0.20f, spec.eye, spec.pupil, sclera, irisColor, ink, skinTone, isLeft = false)

            // Mouth
            drawMouth(cx, cy + faceR * 0.42f, faceR * 0.44f, spec.mouth, ink)

            if (showCueHighlights) {
                // Highlight upper cues (brows/eyes) and lower cue (mouth)
                drawRoundRect(
                    color = highlightAccent,
                    topLeft = Offset(cx - faceR * 0.70f, cy - faceR * 0.52f),
                    size = Size(faceR * 1.40f, faceR * 0.56f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                    style = Stroke(width = 2.4.dp.toPx())
                )
                drawRoundRect(
                    color = highlightAccent,
                    topLeft = Offset(cx - faceR * 0.48f, cy + faceR * 0.24f),
                    size = Size(faceR * 0.96f, faceR * 0.38f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                    style = Stroke(width = 2.4.dp.toPx())
                )
            }
        }
    }
}

private fun DrawScope.drawBrows(cx: Float, y: Float, halfSpread: Float, style: BrowStyle, ink: Color) {
    val stroke = Stroke(width = 4.2.dp.toPx(), cap = StrokeCap.Round)
    val leftStart = cx - halfSpread * 1.65f
    val leftEnd = cx - halfSpread * 0.42f
    val rightStart = cx + halfSpread * 0.42f
    val rightEnd = cx + halfSpread * 1.65f

    when (style) {
        BrowStyle.NEUTRAL -> {
            drawLine(ink, Offset(leftStart, y), Offset(leftEnd, y - 2f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            drawLine(ink, Offset(rightStart, y - 2f), Offset(rightEnd, y), strokeWidth = stroke.width, cap = StrokeCap.Round)
        }
        BrowStyle.RAISED -> {
            val l = Path().apply {
                moveTo(leftStart, y - 4f)
                quadraticTo((leftStart + leftEnd) / 2f, y - 24f, leftEnd, y - 8f)
            }
            val r = Path().apply {
                moveTo(rightStart, y - 8f)
                quadraticTo((rightStart + rightEnd) / 2f, y - 24f, rightEnd, y - 4f)
            }
            drawPath(l, ink, style = stroke)
            drawPath(r, ink, style = stroke)
        }
        BrowStyle.FURROWED -> {
            // Sharp downward inner angle + two vertical glabella tension creases!
            drawLine(ink, Offset(leftStart, y - 8f), Offset(leftEnd, y + 12f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            drawLine(ink, Offset(rightStart, y + 12f), Offset(rightEnd, y - 8f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            drawLine(ink.copy(alpha = 0.65f), Offset(cx - 5f, y - 2f), Offset(cx - 5f, y + 15f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            drawLine(ink.copy(alpha = 0.65f), Offset(cx + 5f, y - 2f), Offset(cx + 5f, y + 15f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        }
        BrowStyle.ASYMMETRIC -> {
            // Left brow flat/compressed, right brow arched high (skepticism!)
            drawLine(ink, Offset(leftStart, y + 4f), Offset(leftEnd, y + 7f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            val r = Path().apply {
                moveTo(rightStart, y - 6f)
                quadraticTo((rightStart + rightEnd) / 2f, y - 28f, rightEnd, y - 2f)
            }
            drawPath(r, ink, style = stroke)
        }
        BrowStyle.SAD_INNER -> {
            // Inner corners pulled up obliquely
            drawLine(ink, Offset(leftStart, y + 8f), Offset(leftEnd, y - 12f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            drawLine(ink, Offset(rightStart, y - 12f), Offset(rightEnd, y + 8f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        }
    }
}

private fun DrawScope.drawEye(
    cx: Float,
    cy: Float,
    radius: Float,
    eyeStyle: EyeStyle,
    pupilSize: PupilSize,
    sclera: Color,
    irisColor: Color,
    ink: Color,
    skinTone: Color,
    isLeft: Boolean
) {
    val eyeH = when (eyeStyle) {
        EyeStyle.WIDE -> radius * 1.15f
        EyeStyle.NEUTRAL -> radius * 0.78f
        EyeStyle.DROOPING -> radius * 0.74f
        EyeStyle.TENSE -> radius * 0.56f
        EyeStyle.SQUINT -> radius * 0.52f
    }

    // Sclera oval
    drawOval(
        color = sclera,
        topLeft = Offset(cx - radius, cy - eyeH),
        size = Size(radius * 2f, eyeH * 2f)
    )

    // Iris & Pupil (real constricted vs dilated size)
    val irisR = radius * 0.48f
    val pupilR = when (pupilSize) {
        PupilSize.CONSTRICTED -> irisR * 0.32f
        PupilSize.NORMAL -> irisR * 0.54f
        PupilSize.DILATED -> irisR * 0.78f
    }
    drawCircle(irisColor, radius = irisR.coerceAtMost(eyeH * 0.92f), center = Offset(cx, cy))
    drawCircle(ink, radius = pupilR.coerceAtMost(eyeH * 0.80f), center = Offset(cx, cy))
    drawCircle(Color.White, radius = (pupilR * 0.34f).coerceAtLeast(2f), center = Offset(cx - irisR * 0.25f, cy - irisR * 0.25f))

    // Upper & lower eyelid geometry (explicitly renders drooping, tense, and squint!)
    when (eyeStyle) {
        EyeStyle.DROOPING -> {
            // Heavy drooping upper lid covering top 45%
            val lid = Path().apply {
                moveTo(cx - radius - 2f, cy - eyeH - 2f)
                lineTo(cx + radius + 2f, cy - eyeH - 2f)
                lineTo(cx + radius + 2f, cy - eyeH * 0.10f)
                quadraticTo(cx, cy + eyeH * 0.15f, cx - radius - 2f, cy - eyeH * 0.10f)
                close()
            }
            drawPath(lid, skinTone)
            drawLine(ink, Offset(cx - radius, cy - eyeH * 0.12f), Offset(cx + radius, cy - eyeH * 0.12f), strokeWidth = 2.8.dp.toPx(), cap = StrokeCap.Round)
        }
        EyeStyle.SQUINT -> {
            // Raised lower cheek lid crescent + crow's feet wrinkles at outer corner!
            val lowerCrescent = Path().apply {
                moveTo(cx - radius, cy + eyeH * 0.55f)
                quadraticTo(cx, cy + eyeH * 0.12f, cx + radius, cy + eyeH * 0.55f)
            }
            drawPath(lowerCrescent, ink, style = Stroke(2.6.dp.toPx(), cap = StrokeCap.Round))
            val dir = if (isLeft) -1f else 1f
            val outerX = cx + dir * (radius + 3f)
            drawLine(ink.copy(alpha = 0.65f), Offset(outerX, cy - 2f), Offset(outerX + dir * 10f, cy - 8f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
            drawLine(ink.copy(alpha = 0.65f), Offset(outerX, cy + 4f), Offset(outerX + dir * 10f, cy + 9f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
        }
        EyeStyle.TENSE -> {
            // Flat horizontal compressed top & bottom lid lines
            drawLine(ink, Offset(cx - radius, cy - eyeH), Offset(cx + radius, cy - eyeH), strokeWidth = 3.2.dp.toPx(), cap = StrokeCap.Round)
            drawLine(ink, Offset(cx - radius, cy + eyeH), Offset(cx + radius, cy + eyeH), strokeWidth = 2.6.dp.toPx(), cap = StrokeCap.Round)
        }
        else -> Unit
    }

    // Outer eye contour
    drawOval(
        color = ink,
        topLeft = Offset(cx - radius, cy - eyeH),
        size = Size(radius * 2f, eyeH * 2f),
        style = Stroke(width = 2.4.dp.toPx())
    )
}

private fun DrawScope.drawMouth(cx: Float, cy: Float, halfW: Float, style: MouthStyle, ink: Color) {
    val stroke = Stroke(width = 3.8.dp.toPx(), cap = StrokeCap.Round)
    when (style) {
        MouthStyle.NEUTRAL -> {
            drawLine(ink, Offset(cx - halfW * 0.72f, cy), Offset(cx + halfW * 0.72f, cy), strokeWidth = stroke.width, cap = StrokeCap.Round)
        }
        MouthStyle.SMILE -> {
            val p = Path().apply {
                moveTo(cx - halfW, cy - 6f)
                quadraticTo(cx, cy + 28f, cx + halfW, cy - 6f)
            }
            drawPath(p, ink, style = stroke)
        }
        MouthStyle.SMIRK -> {
            // Unilateral asymmetric raised right corner + cheek dimple crease!
            val p = Path().apply {
                moveTo(cx - halfW * 0.75f, cy + 5f)
                quadraticTo(cx + halfW * 0.15f, cy + 7f, cx + halfW * 0.92f, cy - 14f)
            }
            drawPath(p, ink, style = stroke)
            // Dimple crease at raised corner
            drawLine(
                ink.copy(alpha = 0.6f),
                Offset(cx + halfW * 0.96f, cy - 22f),
                Offset(cx + halfW * 1.04f, cy - 8f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        MouthStyle.FROWN -> {
            val p = Path().apply {
                moveTo(cx - halfW * 0.85f, cy + 12f)
                quadraticTo(cx, cy - 18f, cx + halfW * 0.85f, cy + 12f)
            }
            drawPath(p, ink, style = stroke)
        }
        MouthStyle.OPEN_SURPRISE -> {
            drawOval(
                color = Color(0xFF7F1D1D),
                topLeft = Offset(cx - halfW * 0.42f, cy - 18f),
                size = Size(halfW * 0.84f, 36f)
            )
            drawOval(
                color = ink,
                topLeft = Offset(cx - halfW * 0.42f, cy - 18f),
                size = Size(halfW * 0.84f, 36f),
                style = Stroke(width = 3.dp.toPx())
            )
        }
        MouthStyle.TENSE_LINE -> {
            drawLine(ink, Offset(cx - halfW * 0.85f, cy), Offset(cx + halfW * 0.85f, cy), strokeWidth = 4.5.dp.toPx(), cap = StrokeCap.Round)
            drawLine(ink.copy(alpha = 0.55f), Offset(cx - halfW * 0.88f, cy - 6f), Offset(cx - halfW * 0.88f, cy + 6f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            drawLine(ink.copy(alpha = 0.55f), Offset(cx + halfW * 0.88f, cy - 6f), Offset(cx + halfW * 0.88f, cy + 6f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        }
    }
}

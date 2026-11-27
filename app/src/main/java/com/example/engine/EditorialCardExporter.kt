package com.example.engine

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.example.data.model.UserProfile
import java.io.File
import java.io.FileOutputStream
import kotlin.math.cos
import kotlin.math.sin

/**
 * Generates a high-resolution (1080x1350) editorial PNG "Patente Neurale" share card
 * and shares it via FileProvider with zero storage permissions.
 */
object EditorialCardExporter {

    fun shareNeuralPassImage(
        context: Context,
        profile: UserProfile,
        balanceIndex: Int
    ) {
        val bitmap = renderNeuralPassBitmap(profile, balanceIndex)
        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val imageFile = File(exportDir, "allena_mente_patente.png")
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(
                    Intent.EXTRA_TEXT,
                    "La mia Patente Neurale AllenaMente • Indice Velocità ${profile.speedIndex} pt • Armonia $balanceIndex%"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Condividi Patente Neurale"))
        } catch (_: Exception) {
            // Safe text fallback if FileProvider/Chooser fails on exotic OEMs
            val textIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(
                    Intent.EXTRA_TEXT,
                    "PATENTE NEURALE • AllenaMente\n" +
                        "Profilo: ${profile.displayName} (${profile.mascotEnum().labelIt})\n" +
                        "Indice di Velocità: ${profile.speedIndex} pt • Armonia: $balanceIndex%\n" +
                        "Logica ${profile.logicScore} • Memoria ${profile.memoryScore} • Riflessi ${profile.reflexScore} • Empatia ${profile.empathyScore}"
                )
            }
            context.startActivity(Intent.createChooser(textIntent, "Condividi Patente Neurale"))
        } finally {
            bitmap.recycle()
        }
    }

    fun renderNeuralPassBitmap(profile: UserProfile, balanceIndex: Int): Bitmap {
        val width = 1080
        val height = 1350
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Deep Obsidian Editorial Gradient Background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(
                    Color.parseColor("#0B1120"),
                    Color.parseColor("#111B33"),
                    Color.parseColor("#0F172A")
                ),
                floatArrayOf(0f, 0.52f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Subtle atmospheric accents
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(26, 45, 212, 191)
        }
        canvas.drawCircle(width * 0.85f, height * 0.14f, 280f, glowPaint)
        glowPaint.color = Color.argb(22, 129, 140, 248)
        canvas.drawCircle(width * 0.14f, height * 0.82f, 240f, glowPaint)

        // Outer editorial gold/teal hairline frame
        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            color = Color.argb(95, 45, 212, 191)
        }
        canvas.drawRoundRect(RectF(42f, 42f, width - 42f, height - 42f), 44f, 44f, framePaint)

        // Brand Header
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2DD4BF")
            textSize = 28f
            letterSpacing = 0.22f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("ALLENAMENTE  •  PATENTE NEURALE", 96f, 132f, labelPaint)

        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 68f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText(profile.displayName.take(22), 96f, 218f, namePaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 30f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        canvas.drawText(
            "${profile.mascotEnum().labelIt}  •  Fascia ${profile.ageCohort}  •  Serie ${profile.currentStreak} gg",
            96f,
            268f,
            subPaint
        )

        // Top Metric Tiles (Speed Index + Harmony Index)
        drawMetricBox(
            canvas = canvas,
            rect = RectF(96f, 314f, 522f, 460f),
            label = "INDICE DI VELOCITÀ",
            value = "${profile.speedIndex} pt",
            accent = Color.parseColor("#2DD4BF")
        )
        drawMetricBox(
            canvas = canvas,
            rect = RectF(558f, 314f, 984f, 460f),
            label = "ARMONIA 4 PILASTRI",
            value = "$balanceIndex%",
            accent = Color.parseColor("#A78BFA")
        )

        // 4-Axis Radar Chart in Center
        val cx = width / 2f
        val cy = 735f
        val maxR = 185f
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.argb(60, 148, 163, 184)
        }
        for (ring in 1..4) {
            val r = maxR * (ring / 4f)
            val diamond = Path().apply {
                moveTo(cx, cy - r)
                lineTo(cx + r, cy)
                lineTo(cx, cy + r)
                lineTo(cx - r, cy)
                close()
            }
            canvas.drawPath(diamond, gridPaint)
        }
        canvas.drawLine(cx, cy - maxR, cx, cy + maxR, gridPaint)
        canvas.drawLine(cx - maxR, cy, cx + maxR, cy, gridPaint)

        val scores = listOf(
            profile.logicScore / 100f,
            profile.memoryScore / 100f,
            profile.reflexScore / 100f,
            profile.empathyScore / 100f
        )
        val angles = listOf(-Math.PI / 2, 0.0, Math.PI / 2, Math.PI)
        val polyPath = Path()
        scores.forEachIndexed { idx, ratio ->
            val r = maxR * ratio.coerceIn(0.12f, 1f)
            val px = cx + (cos(angles[idx]) * r).toFloat()
            val py = cy + (sin(angles[idx]) * r).toFloat()
            if (idx == 0) polyPath.moveTo(px, py) else polyPath.lineTo(px, py)
        }
        polyPath.close()

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(85, 45, 212, 191)
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 5f
            color = Color.parseColor("#2DD4BF")
        }
        canvas.drawPath(polyPath, fillPaint)
        canvas.drawPath(polyPath, strokePaint)

        val axisLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0")
            textSize = 28f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("LOGICA ${profile.logicScore}", cx, cy - maxR - 26f, axisLabelPaint)
        canvas.drawText("RIFLESSI ${profile.reflexScore}", cx, cy + maxR + 46f, axisLabelPaint)
        axisLabelPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("MEMORIA ${profile.memoryScore}", cx + maxR + 22f, cy + 10f, axisLabelPaint)
        axisLabelPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("EMPATIA ${profile.empathyScore}", cx - maxR - 22f, cy + 10f, axisLabelPaint)

        // 4 Horizontal Pillar Bars
        val bars = listOf(
            Triple("Logica", profile.logicScore, Color.parseColor("#38BDF8")),
            Triple("Memoria", profile.memoryScore, Color.parseColor("#A78BFA")),
            Triple("Riflessi", profile.reflexScore, Color.parseColor("#FB923C")),
            Triple("Empatia", profile.empathyScore, Color.parseColor("#34D399"))
        )
        var barY = 1015f
        for ((title, score, color) in bars) {
            drawPillarBar(canvas, 96f, barY, width - 192f, title, score, color)
            barY += 54f
        }

        // Play-compliant non-clinical footer
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 23f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "Profilo Cognitivo Ludico • Benessere mentale quotidiano senza valenza clinica",
            cx,
            height - 84f,
            footerPaint
        )

        return bitmap
    }

    private fun drawMetricBox(
        canvas: Canvas,
        rect: RectF,
        label: String,
        value: String,
        accent: Int
    ) {
        val boxBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(28, Color.red(accent), Color.green(accent), Color.blue(accent))
        }
        val boxBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.argb(115, Color.red(accent), Color.green(accent), Color.blue(accent))
        }
        canvas.drawRoundRect(rect, 26f, 26f, boxBg)
        canvas.drawRoundRect(rect, 26f, 26f, boxBorder)

        val lPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            textSize = 22f
            letterSpacing = 0.08f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val vPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 52f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText(label, rect.left + 28f, rect.top + 48f, lPaint)
        canvas.drawText(value, rect.left + 28f, rect.top + 114f, vPaint)
    }

    private fun drawPillarBar(
        canvas: Canvas,
        x: Float,
        y: Float,
        totalW: Float,
        label: String,
        score: Int,
        color: Int
    ) {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.parseColor("#E2E8F0")
            textSize = 26f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText(label, x, y, textPaint)

        val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = 26f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("$score/100", x + totalW, y, scorePaint)

        val trackY = y + 14f
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.argb(55, 148, 163, 184)
        }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
        }
        canvas.drawRoundRect(RectF(x, trackY, x + totalW, trackY + 12f), 6f, 6f, trackPaint)
        val fillW = (totalW * (score.coerceIn(0, 100) / 100f)).coerceAtLeast(16f)
        canvas.drawRoundRect(RectF(x, trackY, x + fillW, trackY + 12f), 6f, 6f, fillPaint)
    }
}

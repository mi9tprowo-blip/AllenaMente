package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DailyStat
import com.example.ui.theme.LocalPillarColors

@Composable
fun CognitiveRadarChart(
    logicScore: Int,
    memoryScore: Int,
    reflexScore: Int,
    empathyScore: Int,
    modifier: Modifier = Modifier
) {
    val pillars = LocalPillarColors.current
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val primaryFill = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
    val primaryStroke = MaterialTheme.colorScheme.primary

    val animLogic by animateFloatAsState(logicScore / 100f, tween(650), label = "logic")
    val animMemory by animateFloatAsState(memoryScore / 100f, tween(650), label = "memory")
    val animReflex by animateFloatAsState(reflexScore / 100f, tween(650), label = "reflex")
    val animEmpathy by animateFloatAsState(empathyScore / 100f, tween(650), label = "empathy")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Radar cognitivo: Logica $logicScore, Memoria $memoryScore, Riflessi $reflexScore, Empatia $empathyScore"
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "LOGICA ($logicScore)",
            style = MaterialTheme.typography.labelMedium,
            color = pillars.logic,
            fontWeight = FontWeight.Bold
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "EMPATIA\n($empathyScore)",
                style = MaterialTheme.typography.labelMedium,
                color = pillars.empathy,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .size(210.dp)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(186.dp)) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val maxR = size.minDimension * 0.45f

                    // Concentric diamond guide grids (25%, 50%, 75%, 100%)
                    listOf(0.25f, 0.50f, 0.75f, 1f).forEach { frac ->
                        val r = maxR * frac
                        val ring = Path().apply {
                            moveTo(cx, cy - r)
                            lineTo(cx + r, cy)
                            lineTo(cx, cy + r)
                            lineTo(cx - r, cy)
                            close()
                        }
                        drawPath(ring, color = gridColor, style = Stroke(width = 1.2.dp.toPx()))
                    }

                    // Axes
                    drawLine(gridColor, Offset(cx, cy - maxR), Offset(cx, cy + maxR), strokeWidth = 1.dp.toPx())
                    drawLine(gridColor, Offset(cx - maxR, cy), Offset(cx + maxR, cy), strokeWidth = 1.dp.toPx())

                    // Animated cognitive polygon
                    val top = Offset(cx, cy - maxR * animLogic.coerceAtLeast(0.08f))
                    val right = Offset(cx + maxR * animMemory.coerceAtLeast(0.08f), cy)
                    val bottom = Offset(cx, cy + maxR * animReflex.coerceAtLeast(0.08f))
                    val left = Offset(cx - maxR * animEmpathy.coerceAtLeast(0.08f), cy)

                    val poly = Path().apply {
                        moveTo(top.x, top.y)
                        lineTo(right.x, right.y)
                        lineTo(bottom.x, bottom.y)
                        lineTo(left.x, left.y)
                        close()
                    }
                    drawPath(poly, color = primaryFill)
                    drawPath(
                        poly,
                        color = primaryStroke,
                        style = Stroke(width = 2.5.dp.toPx(), join = StrokeJoin.Round)
                    )

                    // Vertex accent dots
                    drawCircle(pillars.logic, radius = 5.dp.toPx(), center = top)
                    drawCircle(pillars.memory, radius = 5.dp.toPx(), center = right)
                    drawCircle(pillars.reflex, radius = 5.dp.toPx(), center = bottom)
                    drawCircle(pillars.empathy, radius = 5.dp.toPx(), center = left)
                }
            }
            Text(
                text = "MEMORIA\n($memoryScore)",
                style = MaterialTheme.typography.labelMedium,
                color = pillars.memory,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = "RIFLESSI ($reflexScore)",
            style = MaterialTheme.typography.labelMedium,
            color = pillars.reflex,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PerformanceTimelineChart(
    dailyStats: List<DailyStat>,
    modifier: Modifier = Modifier
) {
    val ordered = dailyStats.sortedBy { it.dateKey }.takeLast(10)
    val lineColor = MaterialTheme.colorScheme.primary
    val fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    if (ordered.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Completa la tua prima sessione per tracciare il trend giornaliero.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val avgTimelineAccuracy = ordered.map { it.avgAccuracy }.average().toInt()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Grafico andamento storico degli ultimi ${ordered.size} giorni, accuratezza media $avgTimelineAccuracy percento"
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            val w = size.width
            val h = size.height
            val padY = 12.dp.toPx()
            val usableH = (h - padY * 2f).coerceAtLeast(10f)

            // 3 horizontal guides (0%, 50%, 100% accuracy)
            listOf(0f, 0.5f, 1f).forEach { frac ->
                val y = padY + usableH * (1f - frac)
                drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1.dp.toPx())
            }

            val pts = ordered.mapIndexed { idx, stat ->
                val x = if (ordered.size == 1) w / 2f else (idx.toFloat() / (ordered.size - 1)) * w
                val y = padY + usableH * (1f - stat.avgAccuracy.coerceIn(0, 100) / 100f)
                Offset(x, y)
            }

            if (pts.size >= 2) {
                val area = Path().apply {
                    moveTo(pts.first().x, padY + usableH)
                    pts.forEach { lineTo(it.x, it.y) }
                    lineTo(pts.last().x, padY + usableH)
                    close()
                }
                drawPath(area, color = fillColor)

                val strokePath = Path().apply {
                    moveTo(pts.first().x, pts.first().y)
                    pts.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(
                    strokePath,
                    color = lineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            pts.forEach { pt ->
                drawCircle(Color.White, radius = 5.5.dp.toPx(), center = pt)
                drawCircle(lineColor, radius = 4.dp.toPx(), center = pt)
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = ordered.first().dateKey.takeLast(5),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Accuratezza media giornaliera (%)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = ordered.last().dateKey.takeLast(5),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

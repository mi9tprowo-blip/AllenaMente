package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.engine.CircuitGenerator
import com.example.engine.CompassDirection
import com.example.engine.DIR_E
import com.example.engine.DIR_N
import com.example.engine.DIR_S
import com.example.engine.DIR_W
import com.example.engine.ExpressiveFaceGenerator
import com.example.engine.PerspectiveGenerator
import com.example.engine.PipeKind
import com.example.engine.SoundSynthEngine
import com.example.ui.components.ExpressiveFaceCanvas
import com.example.ui.theme.LocalPillarColors
import kotlinx.coroutines.delay

/**
 * BONUS GAME 1: Circuito Sinaptico (neural-circuit)
 * Spanning tree + real BFS power propagation on 3x3..5x5 grid with 90° rotation & locked nodes.
 */
@Composable
fun NeuralCircuitBoard(
    level: Int,
    seed: Int,
    soundEnabled: Boolean,
    nightCalm: Boolean,
    onCompleted: (score: Int, accuracy: Int, avgReactionMs: Int) -> Unit
) {
    val pillars = LocalPillarColors.current
    val puzzle = remember(level, seed) { CircuitGenerator.generate(level, seed) }
    var nodes by remember(puzzle) { mutableStateOf(puzzle.nodes) }
    var moves by remember(puzzle) { mutableIntStateOf(0) }
    var solved by remember(puzzle) { mutableStateOf(false) }
    val startMs = remember(puzzle) { System.currentTimeMillis() }

    LaunchedEffect(solved) {
        if (solved) {
            delay(500)
            val elapsed = (System.currentTimeMillis() - startMs).coerceAtLeast(1000L)
            val efficiency = (puzzle.parMoves.toFloat() / moves.coerceAtLeast(1).toFloat()).coerceIn(0.45f, 1.0f)
            val accuracy = (efficiency * 100f).toInt().coerceIn(55, 100)
            val score = (450 + accuracy * 5 + (level * 4)).coerceIn(300, 1200)
            val avgRt = (elapsed / moves.coerceAtLeast(1)).toInt().coerceIn(250, 5000)
            onCompleted(score, accuracy, avgRt)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Mosse: $moves (Par: ${puzzle.parMoves})",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Recettori: ${nodes.count { it.kind == PipeKind.TARGET && it.powered }}/${nodes.count { it.kind == PipeKind.TARGET }}",
                style = MaterialTheme.typography.labelMedium,
                color = pillars.logic
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (r in 0 until puzzle.size) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (c in 0 until puzzle.size) {
                        val idx = r * puzzle.size + c
                        val node = nodes[idx]
                        val wireColor = if (node.powered) pillars.logic else MaterialTheme.colorScheme.outline
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    when {
                                        node.kind == PipeKind.SOURCE -> pillars.logic.copy(alpha = 0.24f)
                                        node.kind == PipeKind.TARGET && node.powered -> pillars.success.copy(alpha = 0.28f)
                                        node.kind == PipeKind.TARGET -> pillars.empathy.copy(alpha = 0.16f)
                                        node.powered -> pillars.logic.copy(alpha = 0.11f)
                                        else -> MaterialTheme.colorScheme.surface
                                    }
                                )
                                .clickable(enabled = !node.locked && node.kind != PipeKind.EMPTY && !solved) {
                                    SoundSynthEngine.playTap(soundEnabled, nightCalm)
                                    val rotated = nodes.mapIndexed { i, n ->
                                        if (i == idx) n.copy(rotation = (n.rotation + 1) % 4) else n
                                    }
                                    val evaluated = CircuitGenerator.evaluatePower(puzzle.size, rotated)
                                    nodes = evaluated
                                    moves += 1
                                    if (CircuitGenerator.isSolved(evaluated)) {
                                        SoundSynthEngine.playCorrect(soundEnabled, nightCalm)
                                        solved = true
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (node.kind != PipeKind.EMPTY) {
                                val mask = node.connections()
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val cx = size.width / 2f
                                    val cy = size.height / 2f
                                    val strokeW = (size.minDimension * 0.16f).coerceIn(6f, 18f)
                                    if ((mask and DIR_N) != 0) drawLine(wireColor, Offset(cx, cy), Offset(cx, 0f), strokeW, StrokeCap.Round)
                                    if ((mask and DIR_E) != 0) drawLine(wireColor, Offset(cx, cy), Offset(size.width, cy), strokeW, StrokeCap.Round)
                                    if ((mask and DIR_S) != 0) drawLine(wireColor, Offset(cx, cy), Offset(cx, size.height), strokeW, StrokeCap.Round)
                                    if ((mask and DIR_W) != 0) drawLine(wireColor, Offset(cx, cy), Offset(0f, cy), strokeW, StrokeCap.Round)
                                    drawCircle(
                                        color = when (node.kind) {
                                            PipeKind.SOURCE -> pillars.logic
                                            PipeKind.TARGET -> if (node.powered) pillars.success else pillars.empathy
                                            else -> wireColor
                                        },
                                        radius = if (node.kind == PipeKind.SOURCE || node.kind == PipeKind.TARGET) size.minDimension * 0.24f else strokeW * 0.52f,
                                        center = Offset(cx, cy)
                                    )
                                }
                            }
                            if (node.locked && node.kind != PipeKind.SOURCE && node.kind != PipeKind.TARGET && node.kind != PipeKind.EMPTY) {
                                Icon(
                                    Icons.Rounded.Lock,
                                    contentDescription = "Nodo bloccato",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Text(
            text = "Tocca i nodi per ruotarli di 90° e instradare il segnale verso tutti i recettori.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * BONUS GAME 2: Bussola Empatica (perspective-compass)
 * Gamified Director Task with oriented director (N/E/S/O) and frosted occlusion shields!
 */
@Composable
fun PerspectiveCompassBoard(
    level: Int,
    seed: Int,
    soundEnabled: Boolean,
    nightCalm: Boolean,
    onCompleted: (score: Int, accuracy: Int, avgReactionMs: Int) -> Unit
) {
    val pillars = LocalPillarColors.current
    val totalRounds = 4
    var roundIdx by remember { mutableIntStateOf(0) }
    var correctCount by remember { mutableIntStateOf(0) }
    var totalRtMs by remember { mutableLongStateOf(0L) }
    var roundStartMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var feedbackText by remember { mutableStateOf<String?>(null) }
    var feedbackCorrect by remember { mutableStateOf(false) }

    val round = remember(level, seed, roundIdx) {
        PerspectiveGenerator.generateRound(level, seed + roundIdx * 7919)
    }

    LaunchedEffect(roundIdx) {
        roundStartMs = System.currentTimeMillis()
    }

    LaunchedEffect(feedbackText) {
        if (feedbackText != null) {
            delay(1500)
            feedbackText = null
            if (roundIdx + 1 >= totalRounds) {
                val acc = (correctCount * 100) / totalRounds
                val avgRt = (totalRtMs / totalRounds).toInt().coerceIn(300, 12000)
                val score = (correctCount * 220 + (100 - (avgRt / 80).coerceAtMost(80))).coerceIn(100, 1100)
                onCompleted(score, acc, avgRt)
            } else {
                roundIdx += 1
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = pillars.empathy.copy(alpha = 0.16f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Rounded.Person, contentDescription = null, tint = pillars.empathy, modifier = Modifier.size(16.dp))
                    Text(
                        text = "Direttore: ${round.directorSide.labelIt}",
                        style = MaterialTheme.typography.labelMedium,
                        color = pillars.empathy
                    )
                }
            }
            Text("Turno ${roundIdx + 1}/$totalRounds", style = MaterialTheme.typography.labelMedium)
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = round.instructionIt,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(14.dp)
            )
        }

        // Visual Director Position Indicator
        if (round.directorSide == CompassDirection.NORTH) {
            DirectorSideBadge("DIRETTORE QUI (GUARDA IN GIÙ)", pillars.empathy)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (round.directorSide == CompassDirection.WEST) {
                Surface(shape = CircleShape, color = pillars.empathy) {
                    Icon(Icons.Rounded.Person, contentDescription = "Direttore a sinistra", tint = Color.White, modifier = Modifier.padding(8.dp).size(20.dp))
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (r in 0..2) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (c in 0..2) {
                            val cell = round.cells[r * 3 + c]
                            val isAnswer = feedbackText != null && r == round.targetRow && c == round.targetCol
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = when {
                                    isAnswer -> pillars.success.copy(alpha = 0.28f)
                                    cell.occludedForDirector -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)
                                    else -> MaterialTheme.colorScheme.surface
                                },
                                border = BorderStroke(
                                    width = if (isAnswer) 2.5.dp else 1.5.dp,
                                    color = when {
                                        isAnswer -> pillars.success
                                        cell.occludedForDirector -> MaterialTheme.colorScheme.outline
                                        else -> MaterialTheme.colorScheme.outlineVariant
                                    }
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clickable(enabled = feedbackText == null && cell.shape != null) {
                                        val rt = (System.currentTimeMillis() - roundStartMs).coerceIn(200L, 15000L)
                                        totalRtMs += rt
                                        val ok = r == round.targetRow && c == round.targetCol
                                        if (ok) {
                                            correctCount++
                                            SoundSynthEngine.playCorrect(soundEnabled, nightCalm)
                                            feedbackCorrect = true
                                            feedbackText = "Esatto! ${round.explanationIt}"
                                        } else {
                                            SoundSynthEngine.playError(soundEnabled, nightCalm)
                                            feedbackCorrect = false
                                            feedbackText = if (cell.occludedForDirector) {
                                                "Quella cella ha uno schermo opaco: il Direttore non la vede! ${round.explanationIt}"
                                            } else {
                                                "Quasi! ${round.explanationIt}"
                                            }
                                        }
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    if (cell.shape != null) {
                                        Text(
                                            text = cell.shape.symbol,
                                            style = MaterialTheme.typography.headlineLarge,
                                            color = if (cell.occludedForDirector) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f) else pillars.empathy
                                        )
                                    }
                                    if (cell.occludedForDirector) {
                                        Icon(
                                            Icons.Rounded.VisibilityOff,
                                            contentDescription = "Cella schermata al Direttore",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(6.dp)
                                                .size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (round.directorSide == CompassDirection.EAST) {
                Surface(shape = CircleShape, color = pillars.empathy) {
                    Icon(Icons.Rounded.Person, contentDescription = "Direttore a destra", tint = Color.White, modifier = Modifier.padding(8.dp).size(20.dp))
                }
            }
        }

        if (round.directorSide == CompassDirection.SOUTH) {
            DirectorSideBadge("DIRETTORE QUI (GUARDA IN SU)", pillars.empathy)
        }

        if (feedbackText != null) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = (if (feedbackCorrect) pillars.success else MaterialTheme.colorScheme.error).copy(alpha = 0.14f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = feedbackText!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun DirectorSideBadge(text: String, accent: Color) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = accent.copy(alpha = 0.16f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}

/**
 * EXPRESSIVE FACE & EYE GAMES: Sintonia Emotiva & Oltre lo Sguardo
 * Fixes Section 8.1: renders full facial cues + calibrated exposure + 1.5s Didactic Post-Answer Freeze!
 */
@Composable
fun ExpressiveFaceGameBoard(
    level: Int,
    seed: Int,
    eyesOnlyMode: Boolean,
    zenMode: Boolean,
    soundEnabled: Boolean,
    nightCalm: Boolean,
    onCompleted: (score: Int, accuracy: Int, avgReactionMs: Int) -> Unit
) {
    val pillars = LocalPillarColors.current
    val totalRounds = 5
    var roundIdx by remember { mutableIntStateOf(0) }
    var correctCount by remember { mutableIntStateOf(0) }
    var totalRtMs by remember { mutableLongStateOf(0L) }
    var roundStartMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var faceVisible by remember { mutableStateOf(true) }
    var selectedAnswerId by remember { mutableStateOf<String?>(null) }

    val round = remember(level, seed, roundIdx, eyesOnlyMode, zenMode) {
        ExpressiveFaceGenerator.generateRound(level, seed + roundIdx * 5413, eyesOnlyMode, zenMode)
    }

    // Calibrated exposure timing (infinite in Flusso Zen; 2.8s down to 1.25s by level)
    LaunchedEffect(roundIdx, round.exposureMs) {
        faceVisible = true
        selectedAnswerId = null
        roundStartMs = System.currentTimeMillis()
        if (round.exposureMs != Long.MAX_VALUE) {
            delay(round.exposureMs)
            if (selectedAnswerId == null) {
                faceVisible = false
            }
        }
    }

    // 1.5s Post-Answer Didactic Freeze highlighting the exact 2 visual cues!
    LaunchedEffect(selectedAnswerId) {
        if (selectedAnswerId != null) {
            faceVisible = true // Re-reveal frozen face with cue highlight rings!
            delay(1650)
            if (roundIdx + 1 >= totalRounds) {
                val acc = (correctCount * 100) / totalRounds
                val avgRt = (totalRtMs / totalRounds).toInt().coerceIn(300, 10000)
                val score = (correctCount * 185 + (90 - (avgRt / 100).coerceAtMost(75))).coerceIn(100, 1050)
                onCompleted(score, acc, avgRt)
            } else {
                roundIdx += 1
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (eyesOnlyMode) "Leggi la regione oculare" else "Decodifica la micro-espressione",
                style = MaterialTheme.typography.titleSmall,
                color = pillars.empathy
            )
            Text("${roundIdx + 1}/$totalRounds", style = MaterialTheme.typography.labelMedium)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(235.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (faceVisible) {
                ExpressiveFaceCanvas(
                    spec = round.target,
                    eyesOnlyMode = eyesOnlyMode,
                    showCueHighlights = selectedAnswerId != null
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Espressione nascosta",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Scegli l'emozione percepita qui sotto",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Didactic 2-cue feedback banner during 1.5s freeze
        if (selectedAnswerId != null) {
            val isCorrect = selectedAnswerId == round.target.id
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = (if (isCorrect) pillars.success else MaterialTheme.colorScheme.error).copy(alpha = 0.15f),
                border = BorderStroke(1.dp, if (isCorrect) pillars.success else MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (isCorrect) "Esatto: ${round.target.labelIt}" else "Era: ${round.target.labelIt}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Indizi chiave: ${round.target.cueSummaryIt}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            round.options.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { opt ->
                        val isChosen = selectedAnswerId == opt.id
                        val isRight = selectedAnswerId != null && opt.id == round.target.id
                        Button(
                            onClick = {
                                if (selectedAnswerId == null) {
                                    totalRtMs += (System.currentTimeMillis() - roundStartMs).coerceIn(200L, 12000L)
                                    selectedAnswerId = opt.id
                                    if (opt.id == round.target.id) {
                                        correctCount++
                                        SoundSynthEngine.playCorrect(soundEnabled, nightCalm)
                                    } else {
                                        SoundSynthEngine.playError(soundEnabled, nightCalm)
                                    }
                                }
                            },
                            enabled = selectedAnswerId == null || isChosen || isRight,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when {
                                    isRight -> pillars.success
                                    isChosen && !isRight -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.primary
                                }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                        ) {
                            Text(opt.labelIt, maxLines = 1, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

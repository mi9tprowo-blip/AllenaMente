package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.engine.CorePuzzleGenerators
import com.example.engine.Mulberry32
import com.example.engine.RavenOption
import com.example.engine.SoundSynthEngine
import com.example.ui.theme.LocalPillarColors
import kotlinx.coroutines.delay
import kotlin.math.abs

/**
 * Dispatcher routing every one of the 18 games to its real playable Compose board!
 */
@Composable
fun GameBoardDispatcher(
    gameId: String,
    level: Int,
    seed: Int,
    zenMode: Boolean,
    soundEnabled: Boolean,
    nightCalm: Boolean,
    onCompleted: (score: Int, accuracy: Int, avgReactionMs: Int) -> Unit
) {
    when (gameId) {
        "neural-circuit" -> NeuralCircuitBoard(level, seed, soundEnabled, nightCalm, onCompleted)
        "perspective-compass" -> PerspectiveCompassBoard(level, seed, soundEnabled, nightCalm, onCompleted)
        "emotional-resonance" -> ExpressiveFaceGameBoard(level, seed, eyesOnlyMode = false, zenMode, soundEnabled, nightCalm, onCompleted)
        "mind-eyes" -> ExpressiveFaceGameBoard(level, seed, eyesOnlyMode = true, zenMode, soundEnabled, nightCalm, onCompleted)
        "raven-matrix" -> RavenMatrixBoard(level, seed, soundEnabled, nightCalm, onCompleted)
        "abstract-sequences" -> SequenceBoard(level, seed, soundEnabled, nightCalm, onCompleted)
        "pattern-folding" -> PatternFoldingBoard(level, seed, soundEnabled, nightCalm, onCompleted)
        "subitizing" -> SubitizingBoard(level, seed, soundEnabled, nightCalm, onCompleted)
        "light-matrix" -> LightMatrixBoard(level, seed, zenMode, soundEnabled, nightCalm, onCompleted)
        "n-back" -> NBackBoard(level, seed, soundEnabled, nightCalm, onCompleted)
        "digit-span" -> DigitSpanBoard(level, seed, soundEnabled, nightCalm, onCompleted)
        "hippocampus-maze" -> MazeMemoryBoard(level, seed, soundEnabled, nightCalm, onCompleted)
        "impulse-inhibition" -> ImpulseInhibitionBoard(level, seed, soundEnabled, nightCalm, onCompleted)
        "dynamic-stroop" -> DynamicStroopBoard(level, seed, soundEnabled, nightCalm, onCompleted)
        "multi-tracking" -> MultiTrackingBoard(level, seed, soundEnabled, nightCalm, onCompleted)
        "trail-making" -> TrailMakingBoard(level, seed, soundEnabled, nightCalm, onCompleted)
        "kinetic-mirror" -> KineticMirrorBoard(level, seed, soundEnabled, nightCalm, onCompleted)
        "rhythm-mirror" -> RhythmMirrorBoard(level, seed, soundEnabled, nightCalm, onCompleted)
        else -> SequenceBoard(level, seed, soundEnabled, nightCalm, onCompleted)
    }
}

@Composable
private fun RavenMatrixBoard(level: Int, seed: Int, sound: Boolean, night: Boolean, onDone: (Int, Int, Int) -> Unit) {
    val pillars = LocalPillarColors.current
    val rounds = 4
    var idx by remember { mutableIntStateOf(0) }
    var correct by remember { mutableIntStateOf(0) }
    var rtSum by remember { mutableLongStateOf(0L) }
    var startMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val round = remember(level, seed, idx) { CorePuzzleGenerators.generateRavenMatrix(level, seed + idx * 131) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Matrice ${idx + 1}/$rounds — Trova il tassello mancante", style = MaterialTheme.typography.titleSmall, color = pillars.logic)
        Column(modifier = Modifier.fillMaxWidth(0.84f).aspectRatio(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (r in 0..2) {
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (c in 0..2) {
                        val cell = round.grid[r * 3 + c]
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (cell == null) pillars.logic.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, if (cell == null) pillars.logic else MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.weight(1f).fillMaxSize()
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (cell != null) RavenGlyph(cell, pillars.logic)
                                else Text("?", style = MaterialTheme.typography.headlineMedium, color = pillars.logic)
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            round.options.forEachIndexed { optIdx, opt ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(68.dp)
                        .clickable {
                            rtSum += (System.currentTimeMillis() - startMs).coerceIn(250L, 12000L)
                            if (optIdx == round.correctIndex) {
                                correct++; SoundSynthEngine.playCorrect(sound, night)
                            } else SoundSynthEngine.playError(sound, night)
                            if (idx + 1 >= rounds) onDone(correct * 230 + 80, (correct * 100) / rounds, (rtSum / rounds).toInt())
                            else { idx++; startMs = System.currentTimeMillis() }
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) { RavenGlyph(opt, MaterialTheme.colorScheme.primary) }
                }
            }
        }
    }
}

@Composable
private fun RavenGlyph(opt: RavenOption, accent: Color) {
    Canvas(modifier = Modifier.size(46.dp)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.minDimension * 0.28f
        rotate(opt.rotationDeg, pivot = Offset(cx, cy)) {
            when (opt.shapeIdx) {
                0 -> drawCircle(accent, r, Offset(cx, cy))
                1 -> {
                    val p = Path().apply {
                        moveTo(cx, cy - r); lineTo(cx + r, cy + r); lineTo(cx - r, cy + r); close()
                    }
                    drawPath(p, accent)
                }
                else -> {
                    val p = Path().apply {
                        moveTo(cx, cy - r); lineTo(cx + r, cy); lineTo(cx, cy + r); lineTo(cx - r, cy); close()
                    }
                    drawPath(p, accent)
                }
            }
        }
        // Small count pips at bottom
        for (i in 0 until opt.count) {
            val px = cx + (i - (opt.count - 1) / 2f) * 11f
            drawCircle(accent, radius = 3.5f, center = Offset(px, size.height - 6f))
        }
    }
}

@Composable
private fun SequenceBoard(level: Int, seed: Int, sound: Boolean, night: Boolean, onDone: (Int, Int, Int) -> Unit) {
    val pillars = LocalPillarColors.current
    val rounds = 5
    var idx by remember { mutableIntStateOf(0) }
    var correct by remember { mutableIntStateOf(0) }
    var rtSum by remember { mutableLongStateOf(0L) }
    var startMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val round = remember(level, seed, idx) { CorePuzzleGenerators.generateSequence(level, seed + idx * 313) }

    Column(verticalArrangement = Arrangement.spacedBy(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Sequenza ${idx + 1}/$rounds", style = MaterialTheme.typography.titleSmall, color = pillars.logic)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            round.items.forEach { n ->
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.weight(1f)) {
                    Text(n, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 18.dp))
                }
            }
            Surface(shape = RoundedCornerShape(14.dp), color = pillars.logic.copy(alpha = 0.2f), modifier = Modifier.weight(1f)) {
                Text("?", style = MaterialTheme.typography.headlineSmall, color = pillars.logic, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 18.dp))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            round.options.forEachIndexed { oIdx, opt ->
                Button(
                    onClick = {
                        rtSum += (System.currentTimeMillis() - startMs).coerceIn(250L, 12000L)
                        if (oIdx == round.correctIndex) { correct++; SoundSynthEngine.playCorrect(sound, night) }
                        else SoundSynthEngine.playError(sound, night)
                        if (idx + 1 >= rounds) onDone(correct * 190 + 70, (correct * 100) / rounds, (rtSum / rounds).toInt())
                        else { idx++; startMs = System.currentTimeMillis() }
                    },
                    modifier = Modifier.weight(1f).height(54.dp)
                ) { Text(opt, style = MaterialTheme.typography.titleMedium) }
            }
        }
    }
}

@Composable
private fun PatternFoldingBoard(level: Int, seed: Int, sound: Boolean, night: Boolean, onDone: (Int, Int, Int) -> Unit) {
    val pillars = LocalPillarColors.current
    val rounds = 4
    var idx by remember { mutableIntStateOf(0) }
    var correct by remember { mutableIntStateOf(0) }
    var rtSum by remember { mutableLongStateOf(0L) }
    var startMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val round = remember(level, seed, idx) { CorePuzzleGenerators.generatePatternFolding(level, seed + idx * 419) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Turno ${idx + 1}/$rounds — Quale faccia è OPPOSTA a ${round.askFace}?", style = MaterialTheme.typography.titleMedium, color = pillars.logic, textAlign = TextAlign.Center)
        // Cross cube net: Top(0), row Left(1)-Center(2)-Right(3), Bottom(4), FarBottom(5)
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            CubeNetCell(round.netLabels[0], round.netLabels[0] == round.askFace, pillars.logic)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CubeNetCell(round.netLabels[1], round.netLabels[1] == round.askFace, pillars.logic)
                CubeNetCell(round.netLabels[2], round.netLabels[2] == round.askFace, pillars.logic)
                CubeNetCell(round.netLabels[3], round.netLabels[3] == round.askFace, pillars.logic)
            }
            CubeNetCell(round.netLabels[4], round.netLabels[4] == round.askFace, pillars.logic)
            CubeNetCell(round.netLabels[5], round.netLabels[5] == round.askFace, pillars.logic)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            round.options.forEach { opt ->
                Button(
                    onClick = {
                        rtSum += (System.currentTimeMillis() - startMs).coerceIn(250L, 12000L)
                        if (opt == round.correctOpposite) { correct++; SoundSynthEngine.playCorrect(sound, night) }
                        else SoundSynthEngine.playError(sound, night)
                        if (idx + 1 >= rounds) onDone(correct * 230 + 80, (correct * 100) / rounds, (rtSum / rounds).toInt())
                        else { idx++; startMs = System.currentTimeMillis() }
                    },
                    modifier = Modifier.weight(1f).height(54.dp)
                ) { Text(opt, style = MaterialTheme.typography.headlineSmall) }
            }
        }
    }
}

@Composable
private fun CubeNetCell(symbol: String, highlighted: Boolean, accent: Color) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (highlighted) accent.copy(alpha = 0.24f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (highlighted) 2.dp else 1.dp, if (highlighted) accent else MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.size(52.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(symbol, style = MaterialTheme.typography.titleLarge, color = if (highlighted) accent else MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun SubitizingBoard(level: Int, seed: Int, sound: Boolean, night: Boolean, onDone: (Int, Int, Int) -> Unit) {
    val pillars = LocalPillarColors.current
    val rounds = 5
    var idx by remember { mutableIntStateOf(0) }
    var correct by remember { mutableIntStateOf(0) }
    var rtSum by remember { mutableLongStateOf(0L) }
    var startMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var visible by remember { mutableStateOf(true) }
    val round = remember(level, seed, idx) { CorePuzzleGenerators.generateSubitizing(level, seed + idx * 601) }

    LaunchedEffect(idx) {
        visible = true
        startMs = System.currentTimeMillis()
        delay((1150L - level * 6L).coerceIn(550L, 1150L))
        visible = false
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Colpo d'Occhio ${idx + 1}/$rounds — Quanti punti erano?", style = MaterialTheme.typography.titleSmall, color = pillars.logic)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (visible) {
                Canvas(Modifier.fillMaxSize()) {
                    round.dots.forEach { (fx, fy) ->
                        drawCircle(pillars.logic, radius = 12.dp.toPx(), center = Offset(size.width * fx, size.height * fy))
                    }
                }
            } else {
                Text("Quanti punti hai visto?", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            round.options.forEach { opt ->
                Button(
                    onClick = {
                        rtSum += (System.currentTimeMillis() - startMs).coerceIn(200L, 8000L)
                        if (opt == round.count) { correct++; SoundSynthEngine.playCorrect(sound, night) }
                        else SoundSynthEngine.playError(sound, night)
                        if (idx + 1 >= rounds) onDone(correct * 190 + 70, (correct * 100) / rounds, (rtSum / rounds).toInt())
                        else idx++
                    },
                    modifier = Modifier.weight(1f).height(54.dp)
                ) { Text("$opt", style = MaterialTheme.typography.titleLarge) }
            }
        }
    }
}

@Composable
private fun LightMatrixBoard(level: Int, seed: Int, zenMode: Boolean, sound: Boolean, night: Boolean, onDone: (Int, Int, Int) -> Unit) {
    val pillars = LocalPillarColors.current
    val rounds = 3
    var idx by remember { mutableIntStateOf(0) }
    var hits by remember { mutableIntStateOf(0) }
    var totalTargets by remember { mutableIntStateOf(0) }
    var showing by remember { mutableStateOf(true) }
    var tapped by remember { mutableStateOf(setOf<Int>()) }
    var startMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val round = remember(level, seed, idx) { CorePuzzleGenerators.generateLightMatrix(level, seed + idx * 719, zenMode) }

    LaunchedEffect(idx) {
        showing = true
        tapped = emptySet()
        delay(round.previewMs)
        showing = false
        startMs = System.currentTimeMillis()
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (showing) "Memorizza la costellazione (${idx + 1}/$rounds)…"
            else "Tocca le ${round.litCells.size} celle illuminate (${tapped.size}/${round.litCells.size})",
            style = MaterialTheme.typography.titleSmall,
            color = pillars.memory
        )
        Column(Modifier.fillMaxWidth(0.88f).aspectRatio(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (r in 0 until round.gridSize) {
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (c in 0 until round.gridSize) {
                        val cellIdx = r * round.gridSize + c
                        val lit = (showing && cellIdx in round.litCells) || (!showing && cellIdx in tapped)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (lit) pillars.memory else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable(enabled = !showing && cellIdx !in tapped) {
                                    SoundSynthEngine.playTap(sound, night)
                                    val next = tapped + cellIdx
                                    tapped = next
                                    if (next.size >= round.litCells.size) {
                                        val m = next.intersect(round.litCells).size
                                        hits += m
                                        totalTargets += round.litCells.size
                                        if (m == round.litCells.size) SoundSynthEngine.playCorrect(sound, night)
                                        else SoundSynthEngine.playError(sound, night)
                                        if (idx + 1 >= rounds) {
                                            val acc = ((hits * 100) / totalTargets.coerceAtLeast(1)).coerceIn(0, 100)
                                            onDone(acc * 9 + 100, acc, (System.currentTimeMillis() - startMs).toInt().coerceIn(300, 5000))
                                        } else idx++
                                    }
                                }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NBackBoard(level: Int, seed: Int, sound: Boolean, night: Boolean, onDone: (Int, Int, Int) -> Unit) {
    val pillars = LocalPillarColors.current
    val nBack = if (level >= 12) 2 else 1
    val stream = remember(level, seed) {
        val rng = Mulberry32(seed)
        val symbols = listOf("▲", "●", "◆", "★", "■")
        val list = mutableListOf<String>()
        repeat(10) { i ->
            if (i >= nBack && rng.nextFloat() < 0.42f) list.add(list[i - nBack])
            else list.add(rng.pick(symbols))
        }
        list
    }
    var step by remember { mutableIntStateOf(0) }
    var correct by remember { mutableIntStateOf(0) }
    var answered by remember { mutableStateOf(false) }

    LaunchedEffect(step) {
        answered = false
        delay(1850)
        if (!answered && step >= nBack) {
            val isMatch = stream[step] == stream[step - nBack]
            if (!isMatch) correct++
        }
        if (step + 1 >= stream.size) {
            val evaluated = (stream.size - nBack).coerceAtLeast(1)
            val acc = (correct * 100) / evaluated
            onDone(acc * 9 + 80, acc, 740)
        } else step++
    }

    Column(verticalArrangement = Arrangement.spacedBy(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Doppia Traccia ($nBack-Back) • Simbolo ${step + 1}/${stream.size}", style = MaterialTheme.typography.titleSmall, color = pillars.memory)
        Surface(shape = RoundedCornerShape(24.dp), color = pillars.memory.copy(alpha = 0.14f), modifier = Modifier.size(160.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text(stream[step], style = MaterialTheme.typography.displayLarge, color = pillars.memory)
            }
        }
        Button(
            onClick = {
                if (!answered && step >= nBack) {
                    answered = true
                    val isMatch = stream[step] == stream[step - nBack]
                    if (isMatch) { correct++; SoundSynthEngine.playCorrect(sound, night) }
                    else SoundSynthEngine.playError(sound, night)
                }
            },
            enabled = step >= nBack && !answered,
            modifier = Modifier.fillMaxWidth().height(54.dp)
        ) {
            Text(if (step < nBack) "Osserva i primi $nBack simboli…" else "UGUALE A $nBack PASSI FA")
        }
    }
}

@Composable
private fun DigitSpanBoard(level: Int, seed: Int, sound: Boolean, night: Boolean, onDone: (Int, Int, Int) -> Unit) {
    val pillars = LocalPillarColors.current
    val rounds = 3
    var idx by remember { mutableIntStateOf(0) }
    var correct by remember { mutableIntStateOf(0) }
    var showing by remember { mutableStateOf(true) }
    var entered by remember { mutableStateOf("") }
    val round = remember(level, seed, idx) { CorePuzzleGenerators.generateDigitSpan(level, seed + idx * 887) }

    LaunchedEffect(idx) {
        showing = true
        entered = ""
        delay(2200)
        showing = false
    }

    val expected = if (round.reverseOrder) round.digits.reversed().joinToString("") else round.digits.joinToString("")

    Column(verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (round.reverseOrder) "Eco Inverso (${idx + 1}/$rounds): inserisci AL CONTRARIO" else "Eco Diretto (${idx + 1}/$rounds): ripeti in ordine",
            style = MaterialTheme.typography.titleSmall,
            color = pillars.memory
        )
        Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = if (showing) round.digits.joinToString("  ") else entered.ifEmpty { "• ".repeat(round.digits.size).trim() },
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 22.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9)).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { d ->
                        Button(
                            onClick = {
                                if (!showing && entered.length < expected.length) {
                                    SoundSynthEngine.playTap(sound, night)
                                    val next = entered + d
                                    entered = next
                                    if (next.length == expected.length) {
                                        if (next == expected) { correct++; SoundSynthEngine.playCorrect(sound, night) }
                                        else SoundSynthEngine.playError(sound, night)
                                        if (idx + 1 >= rounds) onDone(correct * 300 + 90, (correct * 100) / rounds, 850)
                                        else idx++
                                    }
                                }
                            },
                            enabled = !showing,
                            modifier = Modifier.weight(1f).height(52.dp)
                        ) { Text("$d", style = MaterialTheme.typography.titleLarge) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MazeMemoryBoard(level: Int, seed: Int, sound: Boolean, night: Boolean, onDone: (Int, Int, Int) -> Unit) {
    val pillars = LocalPillarColors.current
    val round = remember(level, seed) { CorePuzzleGenerators.generateMazePath(level, seed) }
    var showing by remember { mutableStateOf(true) }
    var step by remember { mutableIntStateOf(0) }
    var mistakes by remember { mutableIntStateOf(0) }

    LaunchedEffect(round) {
        showing = true
        delay(2400)
        showing = false
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (showing) "Memorizza il percorso (1 → ${round.path.size})" else "Tocca la tappa ${step + 1} di ${round.path.size}",
            style = MaterialTheme.typography.titleSmall,
            color = pillars.memory
        )
        Column(Modifier.fillMaxWidth(0.88f).aspectRatio(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (r in 0 until round.gridSize) {
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (c in 0 until round.gridSize) {
                        val idx = r * round.gridSize + c
                        val pathPos = round.path.indexOf(idx)
                        val revealed = (showing && pathPos >= 0) || (!showing && pathPos in 0 until step)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (revealed) pillars.memory else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable(enabled = !showing) {
                                    if (idx == round.path[step]) {
                                        SoundSynthEngine.playTap(sound, night)
                                        val next = step + 1
                                        step = next
                                        if (next >= round.path.size) {
                                            SoundSynthEngine.playCorrect(sound, night)
                                            val acc = (100 - mistakes * 18).coerceIn(30, 100)
                                            onDone(acc * 9 + 80, acc, 760)
                                        }
                                    } else {
                                        mistakes++
                                        SoundSynthEngine.playError(sound, night)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (revealed) Text("${pathPos + 1}", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImpulseInhibitionBoard(level: Int, seed: Int, sound: Boolean, night: Boolean, onDone: (Int, Int, Int) -> Unit) {
    val pillars = LocalPillarColors.current
    val trials = remember(level, seed) {
        val rng = Mulberry32(seed)
        List(8) { rng.nextFloat() < 0.65f } // true = GO (green), false = NO-GO (red)
    }
    var idx by remember { mutableIntStateOf(0) }
    var correct by remember { mutableIntStateOf(0) }
    var rtSum by remember { mutableLongStateOf(0L) }
    var rtCount by remember { mutableIntStateOf(0) }
    var tappedThisTrial by remember { mutableStateOf(false) }
    var startMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(idx) {
        tappedThisTrial = false
        startMs = System.currentTimeMillis()
        delay((1350L - level * 5L).coerceIn(750L, 1350L))
        if (!tappedThisTrial && !trials[idx]) {
            correct++ // Successfully inhibited NO-GO!
        }
        if (idx + 1 >= trials.size) {
            val acc = (correct * 100) / trials.size
            val avgRt = if (rtCount == 0) 650 else (rtSum / rtCount).toInt()
            onDone(acc * 9 + 100, acc, avgRt)
        } else idx++
    }

    val isGo = trials[idx]
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Freno Rapido • Stimolo ${idx + 1}/${trials.size}", style = MaterialTheme.typography.titleSmall, color = pillars.reflex)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(if (isGo) Color(0xFF16A34A) else Color(0xFFDC2626))
                .clickable(enabled = !tappedThisTrial) {
                    tappedThisTrial = true
                    if (isGo) {
                        correct++
                        rtSum += (System.currentTimeMillis() - startMs).coerceIn(150L, 2000L)
                        rtCount++
                        SoundSynthEngine.playCorrect(sound, night)
                    } else {
                        SoundSynthEngine.playError(sound, night)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isGo) "GO • TOCCA!" else "STOP • FRENA!",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White
            )
        }
    }
}

@Composable
private fun DynamicStroopBoard(level: Int, seed: Int, sound: Boolean, night: Boolean, onDone: (Int, Int, Int) -> Unit) {
    val pillars = LocalPillarColors.current
    val rounds = 6
    val colorNames = listOf("ROSSO", "BLU", "VERDE", "AMBRA")
    val colorValues = listOf(Color(0xFFDC2626), Color(0xFF2563EB), Color(0xFF16A34A), Color(0xFFD97706))
    var idx by remember { mutableIntStateOf(0) }
    var correct by remember { mutableIntStateOf(0) }
    var rtSum by remember { mutableLongStateOf(0L) }
    var startMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val round = remember(level, seed, idx) { CorePuzzleGenerators.generateStroop(seed + idx * 557) }

    Column(verticalArrangement = Arrangement.spacedBy(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Filtro Colore ${idx + 1}/$rounds — Scegli il COLORE DEL TESTO", style = MaterialTheme.typography.titleSmall, color = pillars.reflex)
        Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = round.wordIt,
                style = MaterialTheme.typography.displayLarge,
                color = colorValues[round.inkColorIndex],
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 36.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(0 to 1, 2 to 3).forEach { (a, b) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(a, b).forEach { cIdx ->
                        Button(
                            onClick = {
                                rtSum += (System.currentTimeMillis() - startMs).coerceIn(200L, 8000L)
                                if (cIdx == round.inkColorIndex) { correct++; SoundSynthEngine.playCorrect(sound, night) }
                                else SoundSynthEngine.playError(sound, night)
                                if (idx + 1 >= rounds) onDone(correct * 165 + 60, (correct * 100) / rounds, (rtSum / rounds).toInt())
                                else { idx++; startMs = System.currentTimeMillis() }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colorValues[cIdx]),
                            modifier = Modifier.weight(1f).height(52.dp)
                        ) { Text(colorNames[cIdx], color = Color.White, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MultiTrackingBoard(level: Int, seed: Int, sound: Boolean, night: Boolean, onDone: (Int, Int, Int) -> Unit) {
    val pillars = LocalPillarColors.current
    val orbCount = 6
    val targets = remember(level, seed) {
        Mulberry32(seed).shuffled((0 until orbCount).toList()).take(if (level >= 10) 3 else 2).toSet()
    }
    var phase by remember { mutableIntStateOf(0) } // 0=highlight, 1=moving, 2=pick
    var tick by remember { mutableIntStateOf(0) }
    var picked by remember { mutableStateOf(setOf<Int>()) }

    LaunchedEffect(level, seed) {
        phase = 0
        delay(1600)
        phase = 1
        repeat(16) {
            delay(140)
            tick++
        }
        phase = 2
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = when (phase) {
                0 -> "Segui le ${targets.size} sfere evidenziate!"
                1 -> "Tienile d'occhio mentre si spostano…"
                else -> "Tocca le ${targets.size} sfere target (${picked.size}/${targets.size})"
            },
            style = MaterialTheme.typography.titleSmall,
            color = pillars.reflex
        )
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
        ) {
            val wDp = maxWidth - 52.dp
            val hDp = maxHeight - 52.dp
            for (i in 0 until orbCount) {
                val angle = (i * 1.05 + tick * 0.24)
                val fx = (0.5 + 0.36 * kotlin.math.cos(angle + i)).toFloat().coerceIn(0.05f, 0.92f)
                val fy = (0.5 + 0.36 * kotlin.math.sin(angle * 1.15 + i)).toFloat().coerceIn(0.05f, 0.92f)
                val isLit = (phase == 0 && i in targets) || (phase == 2 && i in picked)
                Box(
                    modifier = Modifier
                        .offset(x = wDp * fx, y = hDp * fy)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isLit) pillars.reflex else MaterialTheme.colorScheme.primary)
                        .clickable(enabled = phase == 2 && i !in picked) {
                            SoundSynthEngine.playTap(sound, night)
                            val next = picked + i
                            picked = next
                            if (next.size == targets.size) {
                                val hits = next.intersect(targets).size
                                val acc = (hits * 100) / targets.size
                                if (acc == 100) SoundSynthEngine.playCorrect(sound, night) else SoundSynthEngine.playError(sound, night)
                                onDone(acc * 9 + 80, acc, 780)
                            }
                        }
                )
            }
        }
    }
}

@Composable
private fun TrailMakingBoard(level: Int, seed: Int, sound: Boolean, night: Boolean, onDone: (Int, Int, Int) -> Unit) {
    val pillars = LocalPillarColors.current
    val nodes = remember(level, seed) { CorePuzzleGenerators.generateTrailMaking(level, seed) }
    var nextExpected by remember { mutableIntStateOf(0) }
    var mistakes by remember { mutableIntStateOf(0) }
    val startMs = remember(nodes) { System.currentTimeMillis() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Sentiero Fluido • Prossimo: ${nodes.getOrNull(nextExpected)?.label ?: "Fine"}",
            style = MaterialTheme.typography.titleSmall,
            color = pillars.reflex
        )
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(270.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
        ) {
            val wDp = maxWidth - 52.dp
            val hDp = maxHeight - 52.dp
            nodes.forEach { n ->
                val done = n.orderIndex < nextExpected
                Surface(
                    shape = CircleShape,
                    color = if (done) pillars.success else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(2.dp, if (done) pillars.success else pillars.reflex),
                    modifier = Modifier
                        .offset(x = wDp * n.xFrac, y = hDp * n.yFrac)
                        .size(48.dp)
                        .clickable(enabled = !done) {
                            if (n.orderIndex == nextExpected) {
                                SoundSynthEngine.playTap(sound, night)
                                nextExpected++
                                if (nextExpected >= nodes.size) {
                                    SoundSynthEngine.playCorrect(sound, night)
                                    val acc = (100 - mistakes * 12).coerceIn(40, 100)
                                    val avgRt = ((System.currentTimeMillis() - startMs) / nodes.size).toInt().coerceIn(250, 5000)
                                    onDone(acc * 9 + 100, acc, avgRt)
                                }
                            } else {
                                mistakes++
                                SoundSynthEngine.playError(sound, night)
                            }
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(n.label, fontWeight = FontWeight.Bold, color = if (done) Color.White else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

@Composable
private fun KineticMirrorBoard(level: Int, seed: Int, sound: Boolean, night: Boolean, onDone: (Int, Int, Int) -> Unit) {
    val pillars = LocalPillarColors.current
    val rounds = 5
    var idx by remember { mutableIntStateOf(0) }
    var correct by remember { mutableIntStateOf(0) }
    var rtSum by remember { mutableLongStateOf(0L) }
    var startMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val targetLeft = remember(level, seed, idx) {
        val rng = Mulberry32(seed + idx * 911)
        rng.nextInt(3) to rng.nextInt(3)
    }
    val expectedRightCol = 2 - targetLeft.second

    Column(verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Specchio ${idx + 1}/$rounds — Tocca la cella RIFLESSA a destra", style = MaterialTheme.typography.titleSmall, color = pillars.empathy)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Left Model Grid
            Column(Modifier.weight(1f).aspectRatio(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (r in 0..2) {
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (c in 0..2) {
                            val active = r == targetLeft.first && c == targetLeft.second
                            Box(
                                Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(10.dp))
                                    .background(if (active) pillars.empathy else MaterialTheme.colorScheme.surfaceVariant)
                            )
                        }
                    }
                }
            }
            // Mirror Axis
            Box(Modifier.height(140.dp).background(pillars.empathy).padding(horizontal = 2.dp))
            // Right Interactive Grid
            Column(Modifier.weight(1f).aspectRatio(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (r in 0..2) {
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (c in 0..2) {
                            Box(
                                Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .clickable {
                                        rtSum += (System.currentTimeMillis() - startMs).coerceIn(200L, 10000L)
                                        if (r == targetLeft.first && c == expectedRightCol) {
                                            correct++; SoundSynthEngine.playCorrect(sound, night)
                                        } else SoundSynthEngine.playError(sound, night)
                                        if (idx + 1 >= rounds) onDone(correct * 190 + 70, (correct * 100) / rounds, (rtSum / rounds).toInt())
                                        else { idx++; startMs = System.currentTimeMillis() }
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RhythmMirrorBoard(level: Int, seed: Int, sound: Boolean, night: Boolean, onDone: (Int, Int, Int) -> Unit) {
    val pillars = LocalPillarColors.current
    val intervals = remember(level, seed) {
        val rng = Mulberry32(seed)
        List(3) { if (rng.nextFloat() < 0.5f) 420L else 700L }
    }
    var listening by remember { mutableStateOf(true) }
    var pulseLit by remember { mutableStateOf(false) }
    var tapTimes by remember { mutableStateOf(listOf<Long>()) }

    LaunchedEffect(intervals) {
        listening = true
        delay(450)
        pulseLit = true; SoundSynthEngine.playTap(sound, night); delay(140); pulseLit = false
        for (gap in intervals) {
            delay((gap - 140).coerceAtLeast(120))
            pulseLit = true; SoundSynthEngine.playTap(sound, night); delay(140); pulseLit = false
        }
        delay(350)
        listening = false
    }

    Column(verticalArrangement = Arrangement.spacedBy(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (listening) "Ascolta il ritmo (4 battiti)…" else "Ripeti i 4 battiti (${tapTimes.size}/4)",
            style = MaterialTheme.typography.titleSmall,
            color = pillars.empathy
        )
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(if (pulseLit) pillars.empathy else pillars.empathy.copy(alpha = 0.22f))
                .clickable(enabled = !listening) {
                    SoundSynthEngine.playTap(sound, night)
                    val next = tapTimes + System.currentTimeMillis()
                    tapTimes = next
                    if (next.size == 4) {
                        val userGaps = listOf(next[1] - next[0], next[2] - next[1], next[3] - next[2])
                        val avgErr = userGaps.zip(intervals).map { (u, e) -> abs(u - e) }.average()
                        val acc = (100 - (avgErr / 6.5)).toInt().coerceIn(35, 100)
                        if (acc >= 70) SoundSynthEngine.playCorrect(sound, night) else SoundSynthEngine.playError(sound, night)
                        onDone(acc * 9 + 90, acc, avgErr.toInt().coerceIn(120, 1500))
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (listening) "ASCOLTA" else "BATTI",
                style = MaterialTheme.typography.headlineSmall,
                color = if (pulseLit) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

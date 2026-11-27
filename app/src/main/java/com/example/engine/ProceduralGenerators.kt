package com.example.engine

import java.util.ArrayDeque

// ============================================================================
// 1. CIRCUITO SINAPTICO (Neural Circuit — Spanning Tree + Real BFS Solver)
// ============================================================================

enum class PipeKind { SOURCE, TARGET, STRAIGHT, ELBOW, TEE, CROSS, EMPTY }

// Bitmask directions: N=1, E=2, S=4, W=8
const val DIR_N = 1
const val DIR_E = 2
const val DIR_S = 4
const val DIR_W = 8

data class CircuitNode(
    val row: Int,
    val col: Int,
    val kind: PipeKind,
    val rotation: Int, // 0..3 clockwise 90deg steps
    val locked: Boolean = false,
    val powered: Boolean = false
) {
    fun connections(): Int {
        val base = when (kind) {
            PipeKind.SOURCE, PipeKind.TARGET -> DIR_N or DIR_E or DIR_S or DIR_W
            PipeKind.STRAIGHT -> DIR_N or DIR_S
            PipeKind.ELBOW -> DIR_N or DIR_E
            PipeKind.TEE -> DIR_N or DIR_E or DIR_W
            PipeKind.CROSS -> DIR_N or DIR_E or DIR_S or DIR_W
            PipeKind.EMPTY -> 0
        }
        return rotateMask(base, rotation)
    }

    companion object {
        fun rotateMask(mask: Int, steps: Int): Int {
            var m = mask
            repeat(((steps % 4) + 4) % 4) {
                var next = 0
                if ((m and DIR_N) != 0) next = next or DIR_E
                if ((m and DIR_E) != 0) next = next or DIR_S
                if ((m and DIR_S) != 0) next = next or DIR_W
                if ((m and DIR_W) != 0) next = next or DIR_N
                m = next
            }
            return m
        }
    }
}

data class CircuitPuzzle(
    val size: Int,
    val nodes: List<CircuitNode>,
    val parMoves: Int
)

object CircuitGenerator {
    fun generate(level: Int, seed: Int): CircuitPuzzle {
        val rng = Mulberry32(seed)
        val size = when {
            level <= 8 -> 3
            level <= 35 -> 4
            else -> 5
        }
        val targetCount = when {
            level <= 10 -> 1
            level <= 40 -> 2
            else -> 3
        }

        // Build guaranteed-solvable spanning tree from (0, 0) using randomized DFS
        val edgeMasks = IntArray(size * size)
        val visited = BooleanArray(size * size)
        val stack = ArrayList<Pair<Int, Int>>()
        visited[0] = true
        stack.add(0 to 0)

        val dirs = listOf(
            Triple(-1, 0, DIR_N to DIR_S),
            Triple(0, 1, DIR_E to DIR_W),
            Triple(1, 0, DIR_S to DIR_N),
            Triple(0, -1, DIR_W to DIR_E)
        )

        while (stack.isNotEmpty()) {
            val (r, c) = stack.last()
            val neighbors = rng.shuffled(dirs).filter { (dr, dc, _) ->
                val nr = r + dr
                val nc = c + dc
                nr in 0 until size && nc in 0 until size && !visited[nr * size + nc]
            }
            if (neighbors.isEmpty()) {
                stack.removeAt(stack.lastIndex)
            } else {
                val (dr, dc, bits) = neighbors.first()
                val nr = r + dr
                val nc = c + dc
                edgeMasks[r * size + c] = edgeMasks[r * size + c] or bits.first
                edgeMasks[nr * size + nc] = edgeMasks[nr * size + nc] or bits.second
                visited[nr * size + nc] = true
                stack.add(nr to nc)
            }
        }

        // Pick targets far from source (0,0)
        val candidates = (1 until size * size)
            .sortedByDescending { idx -> (idx / size) + (idx % size) }
            .take((targetCount + 3).coerceAtMost(size * size - 1))
        val targetIndices = rng.shuffled(candidates).take(targetCount).toSet()

        // Find active cells along paths from source (0,0) to all targets
        val activeIndices = mutableSetOf(0)
        for (target in targetIndices) {
            val parent = IntArray(size * size) { -1 }
            val q = ArrayDeque<Int>()
            q.add(0)
            parent[0] = 0
            while (q.isNotEmpty()) {
                val cur = q.removeFirst()
                if (cur == target) break
                val r = cur / size
                val c = cur % size
                val m = edgeMasks[cur]
                for ((dr, dc, bits) in dirs) {
                    if ((m and bits.first) != 0) {
                        val nr = r + dr
                        val nc = c + dc
                        val nIdx = nr * size + nc
                        if (parent[nIdx] == -1) {
                            parent[nIdx] = cur
                            q.add(nIdx)
                        }
                    }
                }
            }
            var cur = target
            while (cur != -1 && cur != 0) {
                activeIndices.add(cur)
                cur = parent[cur]
            }
        }

        // Keep only edges between active cells so puzzle is crisp and readable
        val prunedMasks = IntArray(size * size)
        for (idx in activeIndices) {
            val r = idx / size
            val c = idx % size
            for ((dr, dc, bits) in dirs) {
                val nr = r + dr
                val nc = c + dc
                if (nr in 0 until size && nc in 0 until size) {
                    val nIdx = nr * size + nc
                    if (nIdx in activeIndices && (edgeMasks[idx] and bits.first) != 0) {
                        prunedMasks[idx] = prunedMasks[idx] or bits.first
                    }
                }
            }
        }

        var parMoves = 0
        val rawNodes = (0 until size * size).map { idx ->
            val r = idx / size
            val c = idx % size
            when {
                idx == 0 -> CircuitNode(r, c, PipeKind.SOURCE, 0, locked = true)
                idx in targetIndices -> CircuitNode(r, c, PipeKind.TARGET, 0, locked = true)
                idx !in activeIndices -> {
                    // Add decorative rotatable distractor pipes on higher levels
                    if (level >= 5 && rng.nextFloat() < 0.55f) {
                        val kind = if (rng.nextFloat() < 0.5f) PipeKind.ELBOW else PipeKind.STRAIGHT
                        CircuitNode(r, c, kind, rng.nextInt(4), locked = false)
                    } else {
                        CircuitNode(r, c, PipeKind.EMPTY, 0, locked = true)
                    }
                }
                else -> {
                    val (kind, solvedRot) = maskToPipe(prunedMasks[idx])
                    val lockChance = if (level >= 18) 0.18f else 0f
                    val isLocked = rng.nextFloat() < lockChance
                    val scramble = if (isLocked || kind == PipeKind.CROSS) {
                        0
                    } else {
                        val maxRot = if (kind == PipeKind.STRAIGHT) 2 else 4
                        val offset = 1 + rng.nextInt((maxRot - 1).coerceAtLeast(1))
                        val turnsNeeded = (maxRot - offset) % maxRot
                        parMoves += turnsNeeded
                        offset
                    }
                    CircuitNode(r, c, kind, (solvedRot + scramble) % 4, locked = isLocked)
                }
            }
        }

        val evaluated = evaluatePower(size, rawNodes)
        return CircuitPuzzle(size = size, nodes = evaluated, parMoves = parMoves.coerceAtLeast(2))
    }

    private fun maskToPipe(mask: Int): Pair<PipeKind, Int> {
        val count = Integer.bitCount(mask)
        if (count >= 4) return PipeKind.CROSS to 0
        if (count == 3) {
            for (rot in 0..3) {
                if (CircuitNode.rotateMask(DIR_N or DIR_E or DIR_W, rot) == mask) return PipeKind.TEE to rot
            }
        }
        if (count == 2) {
            if (mask == (DIR_N or DIR_S)) return PipeKind.STRAIGHT to 0
            if (mask == (DIR_E or DIR_W)) return PipeKind.STRAIGHT to 1
            for (rot in 0..3) {
                if (CircuitNode.rotateMask(DIR_N or DIR_E, rot) == mask) return PipeKind.ELBOW to rot
            }
        }
        return PipeKind.STRAIGHT to 0
    }

    fun evaluatePower(size: Int, nodes: List<CircuitNode>): List<CircuitNode> {
        val powered = BooleanArray(size * size)
        val queue = ArrayDeque<Int>()
        val sourceIdx = nodes.indexOfFirst { it.kind == PipeKind.SOURCE }
        if (sourceIdx >= 0) {
            powered[sourceIdx] = true
            queue.add(sourceIdx)
        }
        val dirs = listOf(
            Triple(-1, 0, DIR_N to DIR_S),
            Triple(0, 1, DIR_E to DIR_W),
            Triple(1, 0, DIR_S to DIR_N),
            Triple(0, -1, DIR_W to DIR_E)
        )
        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            val r = cur / size
            val c = cur % size
            val curMask = nodes[cur].connections()
            for ((dr, dc, bits) in dirs) {
                if ((curMask and bits.first) == 0) continue
                val nr = r + dr
                val nc = c + dc
                if (nr in 0 until size && nc in 0 until size) {
                    val nIdx = nr * size + nc
                    if (!powered[nIdx] && (nodes[nIdx].connections() and bits.second) != 0) {
                        powered[nIdx] = true
                        queue.add(nIdx)
                    }
                }
            }
        }
        return nodes.mapIndexed { idx, node -> node.copy(powered = powered[idx]) }
    }

    fun isSolved(nodes: List<CircuitNode>): Boolean =
        nodes.filter { it.kind == PipeKind.TARGET }.all { it.powered }
}

// ============================================================================
// 2. BUSSOLA EMPATICA (Perspective Compass — Director Task + Occlusion Shields)
// ============================================================================

enum class CompassDirection(val labelIt: String) {
    NORTH("Nord (in alto)"),
    SOUTH("Sud (in basso)"),
    EAST("Est (a destra)"),
    WEST("Ovest (a sinistra)")
}

enum class PerspectiveShape(val labelIt: String, val symbol: String) {
    CIRCLE("Cerchio", "●"),
    TRIANGLE("Triangolo", "▲"),
    DIAMOND("Rombo", "◆"),
    STAR("Stella", "★")
}

data class PerspectiveCell(
    val row: Int,
    val col: Int,
    val shape: PerspectiveShape?,
    val occludedForDirector: Boolean
)

data class PerspectiveRound(
    val directorSide: CompassDirection,
    val cells: List<PerspectiveCell>,
    val instructionIt: String,
    val targetRow: Int,
    val targetCol: Int,
    val explanationIt: String
)

object PerspectiveGenerator {
    /**
     * Converts board (row, col) into the Director's local coordinate system:
     * returns Pair(rightness, farness) from the Director's point of view!
     */
    fun toDirectorCoords(row: Int, col: Int, directorSide: CompassDirection): Pair<Int, Int> {
        return when (directorSide) {
            // Director sits at TOP looking SOUTH: director's right is player's LEFT (2-col), far is row
            CompassDirection.NORTH -> (2 - col) to row
            // Director sits at BOTTOM looking NORTH: director's right is player's RIGHT (col), far is 2-row
            CompassDirection.SOUTH -> col to (2 - row)
            // Director sits at RIGHT looking WEST: director's right is TOP (2-row), far is 2-col
            CompassDirection.EAST -> (2 - row) to (2 - col)
            // Director sits at LEFT looking EAST: director's right is BOTTOM (row), far is col
            CompassDirection.WEST -> row to col
        }
    }

    fun generateRound(level: Int, seed: Int): PerspectiveRound {
        val rng = Mulberry32(seed)
        val sides = if (level <= 6) {
            listOf(CompassDirection.NORTH, CompassDirection.SOUTH)
        } else {
            CompassDirection.entries
        }
        val directorSide = rng.pick(sides)
        val targetShape = rng.pick(PerspectiveShape.entries)
        val distractorShapes = PerspectiveShape.entries.filter { it != targetShape }

        val allCoords = (0 until 9).map { it / 3 to it % 3 }
        val shuffled = rng.shuffled(allCoords)

        // Place 3 instances of targetShape: 2 visible to director, 1 hidden behind frosted occlusion shield!
        val visibleTargetA = shuffled[0]
        val visibleTargetB = shuffled[1]
        val occludedTarget = shuffled[2]
        val extraOccluded = shuffled[3]
        val distractor1 = shuffled[4]
        val distractor2 = shuffled[5]

        val queryAxisRight = rng.nextFloat() < 0.5f
        val pickMax = rng.nextFloat() < 0.5f

        val visibleTargets = listOf(visibleTargetA, visibleTargetB)
        val sortedByDirector = visibleTargets.sortedBy { (r, c) ->
            val (rightness, farness) = toDirectorCoords(r, c, directorSide)
            if (queryAxisRight) rightness * 10 + farness else farness * 10 + rightness
        }
        val chosen = if (pickMax) sortedByDirector.last() else sortedByDirector.first()

        val relationPhrase = when {
            queryAxisRight && pickMax -> "più a DESTRA"
            queryAxisRight && !pickMax -> "più a SINISTRA"
            !queryAxisRight && pickMax -> "più LONTANO"
            else -> "più VICINO"
        }

        val cells = allCoords.map { (r, c) ->
            when (r to c) {
                visibleTargetA, visibleTargetB -> PerspectiveCell(r, c, targetShape, occludedForDirector = false)
                occludedTarget -> PerspectiveCell(r, c, targetShape, occludedForDirector = true)
                extraOccluded -> PerspectiveCell(r, c, rng.pick(distractorShapes), occludedForDirector = true)
                distractor1 -> PerspectiveCell(r, c, distractorShapes[0], occludedForDirector = false)
                distractor2 -> PerspectiveCell(r, c, distractorShapes[1], occludedForDirector = false)
                else -> PerspectiveCell(r, c, null, occludedForDirector = false)
            }
        }

        val instruction = "Tocca il ${targetShape.labelIt} $relationPhrase dal punto di vista del Direttore (${directorSide.labelIt})."
        val explanation = "Dal lato ${directorSide.labelIt}, gli oggetti con schermo opaco sono invisibili al Direttore. Tra quelli visibili, la cella (${chosen.first + 1}, ${chosen.second + 1}) è $relationPhrase."

        return PerspectiveRound(
            directorSide = directorSide,
            cells = cells,
            instructionIt = instruction,
            targetRow = chosen.first,
            targetCol = chosen.second,
            explanationIt = explanation
        )
    }
}

// ============================================================================
// 3. EXPRESSIVE FACE & EYE GAMES (Fixing Section 8.1: squint, smirk, furrowed, eyelids.curve)
// ============================================================================

enum class BrowStyle { NEUTRAL, RAISED, FURROWED, ASYMMETRIC, SAD_INNER }
enum class EyeStyle { NEUTRAL, WIDE, SQUINT, DROOPING, TENSE }
enum class PupilSize { NORMAL, CONSTRICTED, DILATED }
enum class MouthStyle { NEUTRAL, SMILE, SMIRK, FROWN, OPEN_SURPRISE, TENSE_LINE }

data class EmotionSpec(
    val id: String,
    val labelIt: String,
    val brow: BrowStyle,
    val eye: EyeStyle,
    val pupil: PupilSize,
    val mouth: MouthStyle,
    val cheekTension: Boolean,
    val cueSummaryIt: String
)

val FULL_EMOTIONS_CATALOG = listOf(
    EmotionSpec(
        id = "joy",
        labelIt = "Gioia Autentica",
        brow = BrowStyle.RAISED,
        eye = EyeStyle.SQUINT,
        pupil = PupilSize.DILATED,
        mouth = MouthStyle.SMILE,
        cheekTension = true,
        cueSummaryIt = "Occhi socchiusi dal sorriso (Duchenne) + angoli della bocca sollevati"
    ),
    EmotionSpec(
        id = "skepticism",
        labelIt = "Scetticismo",
        brow = BrowStyle.ASYMMETRIC,
        eye = EyeStyle.SQUINT,
        pupil = PupilSize.CONSTRICTED,
        mouth = MouthStyle.SMIRK,
        cheekTension = true,
        cueSummaryIt = "Sopracciglia asimmetriche + mezzo sorriso tirato (smirk)"
    ),
    EmotionSpec(
        id = "surprise",
        labelIt = "Sorpresa",
        brow = BrowStyle.RAISED,
        eye = EyeStyle.WIDE,
        pupil = PupilSize.DILATED,
        mouth = MouthStyle.OPEN_SURPRISE,
        cheekTension = false,
        cueSummaryIt = "Sopracciglia alte ad arco + occhi spalancati con bocca aperta"
    ),
    EmotionSpec(
        id = "anger",
        labelIt = "Tensione / Rabbia",
        brow = BrowStyle.FURROWED,
        eye = EyeStyle.TENSE,
        pupil = PupilSize.CONSTRICTED,
        mouth = MouthStyle.TENSE_LINE,
        cheekTension = true,
        cueSummaryIt = "Sopracciglia corrugate (furrowed) + palpebre tese e labbra serrate"
    ),
    EmotionSpec(
        id = "sadness",
        labelIt = "Malinconia",
        brow = BrowStyle.SAD_INNER,
        eye = EyeStyle.DROOPING,
        pupil = PupilSize.NORMAL,
        mouth = MouthStyle.FROWN,
        cheekTension = false,
        cueSummaryIt = "Angoli interni delle sopracciglia sollevati + palpebre cadenti"
    ),
    EmotionSpec(
        id = "contempt",
        labelIt = "Sufficienza",
        brow = BrowStyle.NEUTRAL,
        eye = EyeStyle.DROOPING,
        pupil = PupilSize.CONSTRICTED,
        mouth = MouthStyle.SMIRK,
        cheekTension = true,
        cueSummaryIt = "Palpebre semi-abbassate + sorriso asimmetrico su un solo lato"
    ),
    EmotionSpec(
        id = "alert",
        labelIt = "Allerta Vigile",
        brow = BrowStyle.FURROWED,
        eye = EyeStyle.WIDE,
        pupil = PupilSize.CONSTRICTED,
        mouth = MouthStyle.NEUTRAL,
        cheekTension = true,
        cueSummaryIt = "Fronte corrugata con pupille ristrette + sguardo fisso spalancato"
    ),
    EmotionSpec(
        id = "relief",
        labelIt = "Sollievo Sereno",
        brow = BrowStyle.NEUTRAL,
        eye = EyeStyle.DROOPING,
        pupil = PupilSize.DILATED,
        mouth = MouthStyle.SMILE,
        cheekTension = false,
        cueSummaryIt = "Palpebre rilassate + sorriso morbido senza tensione agli zigomi"
    )
)

data class EmotionRound(
    val target: EmotionSpec,
    val options: List<EmotionSpec>,
    val exposureMs: Long,
    val eyesOnlyMode: Boolean
)

object ExpressiveFaceGenerator {
    /**
     * Calibrated exposure time (fixes Section 8.1): generous 2800ms in early levels
     * smoothly decreasing to 1250ms at high levels, or infinite in Flusso Zen.
     */
    fun calibratedExposureMs(level: Int, zenMode: Boolean): Long {
        if (zenMode) return Long.MAX_VALUE
        val clamped = level.coerceIn(1, 100)
        return (2800L - (clamped - 1) * 16L).coerceIn(1250L, 2800L)
    }

    fun generateRound(level: Int, seed: Int, eyesOnlyMode: Boolean, zenMode: Boolean): EmotionRound {
        val rng = Mulberry32(seed)
        val pool = if (level <= 5) FULL_EMOTIONS_CATALOG.take(5) else FULL_EMOTIONS_CATALOG
        val target = rng.pick(pool)
        val distractors = rng.shuffled(pool.filter { it.id != target.id }).take(3)
        val options = rng.shuffled(distractors + target)
        return EmotionRound(
            target = target,
            options = options,
            exposureMs = calibratedExposureMs(level, zenMode),
            eyesOnlyMode = eyesOnlyMode
        )
    }
}

// ============================================================================
// 4. CORE PUZZLES GENERATORS (Logic, Memory, Reflex, Empathy)
// ============================================================================

data class RavenOption(val shapeIdx: Int, val count: Int, val rotationDeg: Float)
data class RavenRound(
    val grid: List<RavenOption?>, // 9 cells, index 8 is null (missing)
    val options: List<RavenOption>,
    val correctIndex: Int
)

object CorePuzzleGenerators {

    fun generateRavenMatrix(level: Int, seed: Int): RavenRound {
        val rng = Mulberry32(seed)
        val baseShape = rng.nextInt(3)
        val rotateStep = if (level >= 4) 45f else 0f
        val countStep = if (level >= 2) 1 else 0
        val cells = (0 until 9).map { idx ->
            val r = idx / 3
            val c = idx % 3
            RavenOption(
                shapeIdx = (baseShape + r + c) % 3,
                count = 1 + ((r + c * countStep) % 3),
                rotationDeg = (c * rotateStep + r * rotateStep) % 360f
            )
        }
        val answer = cells[8]
        val distractors = mutableListOf<RavenOption>()
        var guard = 0
        while (distractors.size < 3 && guard < 40) {
            guard++
            val cand = RavenOption(
                shapeIdx = rng.nextInt(3),
                count = 1 + rng.nextInt(3),
                rotationDeg = (rng.nextInt(4) * 45f)
            )
            if (cand != answer && cand !in distractors) distractors.add(cand)
        }
        val allOptions = rng.shuffled(distractors + answer)
        return RavenRound(
            grid = cells.take(8) + null,
            options = allOptions,
            correctIndex = allOptions.indexOf(answer)
        )
    }

    data class SequenceRound(val items: List<String>, val options: List<String>, val correctIndex: Int, val hintIt: String)

    fun generateSequence(level: Int, seed: Int): SequenceRound {
        val rng = Mulberry32(seed)
        val start = rng.nextInt(2, 12 + level)
        val step = rng.nextInt(2, 5 + (level / 6).coerceAtMost(8))
        val quadratic = level >= 10 && rng.nextFloat() < 0.5f
        val seq = (0..4).map { i ->
            if (quadratic) start + step * i + i * (i - 1) / 2
            else start + step * i
        }
        val answer = seq.last()
        val shown = seq.dropLast(1).map { it.toString() }
        val opts = rng.shuffled(
            listOf(answer, answer + step, (answer - 2).coerceAtLeast(1), answer + 3).distinct().let { list ->
                if (list.size == 4) list else listOf(answer, answer + 2, answer + 5, answer - 1)
            }
        ).map { it.toString() }
        return SequenceRound(
            items = shown,
            options = opts,
            correctIndex = opts.indexOf(answer.toString()),
            hintIt = if (quadratic) "Progressione con incremento crescente" else "Passo costante +$step"
        )
    }

    data class FoldingRound(
        val netLabels: List<String>, // 6 faces of cross net: Top, Left, Center, Right, Bottom, FarBottom
        val askFace: String,
        val correctOpposite: String,
        val options: List<String>
    )

    fun generatePatternFolding(level: Int, seed: Int): FoldingRound {
        val rng = Mulberry32(seed)
        val symbolPool = listOf("▲", "●", "◆", "★", "■", "✚", "❖", "⬟")
        val faces = rng.shuffled(symbolPool).take(6)
        // Standard cross net: 0=Top, 1=Left, 2=Center, 3=Right, 4=Bottom, 5=FarBottom
        // Opposites: (0 <-> 4), (1 <-> 3), (2 <-> 5)
        val pairs = listOf(0 to 4, 1 to 3, 2 to 5)
        val chosenPair = rng.pick(pairs)
        val flip = rng.nextFloat() < 0.5f
        val askIdx = if (flip) chosenPair.first else chosenPair.second
        val oppIdx = if (flip) chosenPair.second else chosenPair.first
        val askFace = faces[askIdx]
        val oppFace = faces[oppIdx]
        val distractors = rng.shuffled(faces.filter { it != askFace && it != oppFace }).take(3)
        return FoldingRound(
            netLabels = faces,
            askFace = askFace,
            correctOpposite = oppFace,
            options = rng.shuffled(distractors + oppFace)
        )
    }

    data class SubitizingRound(val dots: List<Pair<Float, Float>>, val count: Int, val options: List<Int>)

    fun generateSubitizing(level: Int, seed: Int): SubitizingRound {
        val rng = Mulberry32(seed)
        val maxDots = (6 + level / 6).coerceIn(6, 13)
        val minDots = (3 + level / 12).coerceIn(3, maxDots - 2)
        val count = rng.nextInt(minDots, maxDots)
        val pts = mutableListOf<Pair<Float, Float>>()
        var tries = 0
        while (pts.size < count && tries < 250) {
            tries++
            val x = 0.14f + rng.nextFloat() * 0.72f
            val y = 0.14f + rng.nextFloat() * 0.72f
            if (pts.none { (px, py) -> (px - x) * (px - x) + (py - y) * (py - y) < 0.022f }) {
                pts.add(x to y)
            }
        }
        val opts = rng.shuffled(
            listOf(count, count + 1, (count - 1).coerceAtLeast(2), count + 2).distinct()
        )
        return SubitizingRound(dots = pts, count = pts.size, options = opts)
    }

    data class LightMatrixRound(val gridSize: Int, val litCells: Set<Int>, val previewMs: Long)

    fun generateLightMatrix(level: Int, seed: Int, zenMode: Boolean): LightMatrixRound {
        val rng = Mulberry32(seed)
        val size = if (level <= 12) 3 else if (level <= 40) 4 else 5
        val count = (3 + level / 5).coerceIn(3, size * size - 2)
        val all = (0 until size * size).toList()
        val chosen = rng.shuffled(all).take(count).toSet()
        val preview = if (zenMode) 2600L else (2100L - level * 12L).coerceIn(1000L, 2100L)
        return LightMatrixRound(size, chosen, preview)
    }

    data class DigitSpanRound(val digits: List<Int>, val reverseOrder: Boolean)

    fun generateDigitSpan(level: Int, seed: Int): DigitSpanRound {
        val rng = Mulberry32(seed)
        val len = (4 + level / 8).coerceIn(4, 9)
        val digits = List(len) { rng.nextInt(1, 9) }
        val reverse = level >= 6 && rng.nextFloat() < 0.5f
        return DigitSpanRound(digits, reverse)
    }

    data class MazeRound(val gridSize: Int, val path: List<Int>)

    fun generateMazePath(level: Int, seed: Int): MazeRound {
        val rng = Mulberry32(seed)
        val size = if (level <= 15) 4 else 5
        val length = (4 + level / 6).coerceIn(4, 9)
        val path = mutableListOf(rng.nextInt(size * size))
        val dirs = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
        repeat(length - 1) {
            val cur = path.last()
            val r = cur / size
            val c = cur % size
            val cands = dirs.mapNotNull { (dr, dc) ->
                val nr = r + dr
                val nc = c + dc
                if (nr in 0 until size && nc in 0 until size) {
                    val idx = nr * size + nc
                    if (idx !in path) idx else null
                } else null
            }
            if (cands.isNotEmpty()) path.add(rng.pick(cands))
        }
        return MazeRound(size, path)
    }

    data class StroopRound(val wordIt: String, val inkColorIndex: Int) // 0=Rosso, 1=Blu, 2=Verde, 3=Ambra

    fun generateStroop(seed: Int): StroopRound {
        val rng = Mulberry32(seed)
        val words = listOf("ROSSO", "BLU", "VERDE", "AMBRA")
        val wordIdx = rng.nextInt(4)
        val inkIdx = if (rng.nextFloat() < 0.78f) {
            (wordIdx + 1 + rng.nextInt(3)) % 4
        } else wordIdx
        return StroopRound(words[wordIdx], inkIdx)
    }

    data class TrailNode(val label: String, val orderIndex: Int, val xFrac: Float, val yFrac: Float)

    fun generateTrailMaking(level: Int, seed: Int): List<TrailNode> {
        val rng = Mulberry32(seed)
        val pairs = (4 + level / 10).coerceIn(4, 7)
        val labels = mutableListOf<String>()
        for (i in 1..pairs) {
            labels.add(i.toString())
            labels.add(('A' + (i - 1)).toString())
        }
        val nodes = mutableListOf<TrailNode>()
        labels.forEachIndexed { idx, label ->
            var x = 0.5f
            var y = 0.5f
            var tries = 0
            while (tries < 120) {
                tries++
                x = 0.14f + rng.nextFloat() * 0.72f
                y = 0.14f + rng.nextFloat() * 0.72f
                if (nodes.none { (it.xFrac - x) * (it.xFrac - x) + (it.yFrac - y) * (it.yFrac - y) < 0.036f }) break
            }
            nodes.add(TrailNode(label, idx, x, y))
        }
        return nodes
    }
}

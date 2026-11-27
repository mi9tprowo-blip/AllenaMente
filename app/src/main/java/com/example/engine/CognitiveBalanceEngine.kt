package com.example.engine

import com.example.data.model.CoachVerdict
import com.example.data.model.CognitivePillar
import com.example.data.model.DailyStat
import com.example.data.model.GAMES_CATALOG
import com.example.data.model.GameDefinition
import com.example.data.model.TrainingSession
import com.example.data.model.UserProfile
import kotlin.math.abs
import kotlin.math.roundToInt

data class CognitiveBalanceInsight(
    val balanceIndex: Int, // 0..100 harmony across the 4 pillars
    val strongestPillar: CognitivePillar,
    val focusPillar: CognitivePillar,
    val restingPillar: CognitivePillar?, // Non-null when trained 3+ times today (Cognitive Rest suggestion)
    val recommendedGame: GameDefinition,
    val recommendationReason: String
)

/**
 * Deterministic cognitive harmony & rest-cycle advisor.
 * Prevents single-pillar fatigue by tracking today's training distribution and
 * balancing the 4 pillars (Logica, Memoria, Riflessi, Empatia).
 */
object CognitiveBalanceEngine {

    const val FATIGUE_DAILY_THRESHOLD = 3

    fun evaluate(
        profile: UserProfile,
        recentSessions: List<TrainingSession>,
        todayStat: DailyStat?,
        todayDateKey: String
    ): CognitiveBalanceInsight {
        val scores = mapOf(
            CognitivePillar.LOGIC to profile.logicScore.coerceIn(0, 100),
            CognitivePillar.MEMORY to profile.memoryScore.coerceIn(0, 100),
            CognitivePillar.REFLEX to profile.reflexScore.coerceIn(0, 100),
            CognitivePillar.EMPATHY to profile.empathyScore.coerceIn(0, 100)
        )

        val avg = scores.values.average()
        val meanAbsDev = scores.values.map { abs(it - avg) }.average()
        // 0 deviation => 100% balance; >= 40 avg deviation => 20% balance
        val balanceIndex = (100.0 - (meanAbsDev * 2.0)).roundToInt().coerceIn(20, 100)

        val strongest = scores.maxBy { it.value }.key

        // Count today's sessions per pillar to detect pillar fatigue ("Riposo Cognitivo")
        val todayCounts = mutableMapOf<CognitivePillar, Int>()
        for (session in recentSessions) {
            if (session.dateKey == todayDateKey) {
                val p = CognitivePillar.entries.firstOrNull { it.name == session.pillar } ?: continue
                todayCounts[p] = (todayCounts[p] ?: 0) + 1
            }
        }
        val fatiguedPillar = todayCounts.entries
            .filter { it.value >= FATIGUE_DAILY_THRESHOLD }
            .maxByOrNull { it.value }
            ?.key

        // Determine focus pillar: prefer uncompleted daily ring that isn't fatigued, otherwise lowest non-fatigued pillar
        val uncompletedRings = buildList {
            if (todayStat?.logicCompleted != true) add(CognitivePillar.LOGIC)
            if (todayStat?.memoryCompleted != true) add(CognitivePillar.MEMORY)
            if (todayStat?.reflexCompleted != true) add(CognitivePillar.REFLEX)
            if (todayStat?.empathyCompleted != true) add(CognitivePillar.EMPATHY)
        }

        val candidates = (uncompletedRings.ifEmpty { CognitivePillar.entries.toList() })
            .filter { it != fatiguedPillar }
            .ifEmpty { CognitivePillar.entries.toList() }

        val focus = candidates.minBy { scores[it] ?: 50 }

        // Avoid recommending the exact same game played most recently
        val lastGameId = recentSessions.firstOrNull()?.gameId
        val pillarGames = GAMES_CATALOG.filter { it.pillar == focus }
        val zenPreferred = if (profile.zenFlowMode) {
            pillarGames.filter { it.supportsZenMode }.ifEmpty { pillarGames }
        } else {
            pillarGames
        }
        val recommended = zenPreferred.firstOrNull { it.id != lastGameId } ?: zenPreferred.first()

        val reason = when {
            fatiguedPillar != null ->
                "${fatiguedPillar.titleIt} in riposo consigliato (${todayCounts[fatiguedPillar]} sessioni oggi). Ruota su ${focus.titleIt} con ${recommended.titleIt}."
            uncompletedRings.contains(focus) ->
                "Completa l'anello ${focus.titleIt} di oggi con ${recommended.titleIt} per aumentare l'armonia del profilo."
            else ->
                "Equilibrio al $balanceIndex%: rafforza ${focus.titleIt} (${scores[focus]}/100) con ${recommended.titleIt}."
        }

        return CognitiveBalanceInsight(
            balanceIndex = balanceIndex,
            strongestPillar = strongest,
            focusPillar = focus,
            restingPillar = fatiguedPillar,
            recommendedGame = recommended,
            recommendationReason = reason
        )
    }

    fun buildOfflineVerdict(
        profile: UserProfile,
        recentSessions: List<TrainingSession>,
        todayDateKey: String
    ): CoachVerdict {
        val insight = evaluate(profile, recentSessions, null, todayDateKey)
        val streakPart = if (profile.currentStreak > 0) {
            "La tua serie di ${profile.currentStreak} giorni mostra ottima continuità."
        } else {
            "Inizia oggi con una breve sessione per avviare la tua serie."
        }
        val restNote = if (insight.restingPillar != null) {
            "Concedi una pausa a ${insight.restingPillar.titleIt} e punta su ${insight.focusPillar.titleIt}."
        } else {
            "Per rendere il Radar più armonioso (equilibrio ${insight.balanceIndex}%), dedica il prossimo turno a ${insight.focusPillar.titleIt}."
        }
        return CoachVerdict(
            headline = "Armonia al ${insight.balanceIndex}% • Focus ${insight.focusPillar.titleIt}",
            summary = "$streakPart $restNote",
            strongestPillar = insight.strongestPillar.titleIt,
            focusPillar = insight.focusPillar.titleIt,
            recommendedGameId = insight.recommendedGame.id,
            actionableTip = insight.recommendationReason
        )
    }
}

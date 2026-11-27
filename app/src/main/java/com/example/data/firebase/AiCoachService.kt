package com.example.data.firebase

import android.util.Log
import com.example.data.model.CoachVerdict
import com.example.data.model.GAMES_CATALOG
import com.example.data.model.TrainingSession
import com.example.data.model.UserProfile
import com.example.engine.CognitiveBalanceEngine
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val COACH_TAG = "AiCoachService"

class AiCoachService {

    private val modelFallbackChain = listOf("gemini-3.8-flash", "gemini-2.5-flash")

    // Deterministic SHA-256 context cache to avoid redundant LLM calls when profile & sessions haven't changed
    private val verdictCache = LinkedHashMap<String, CoachVerdict>(16, 0.75f, true)

    private val verdictSchema = Schema.obj(
        properties = mapOf(
            "headline" to Schema.string(description = "Short encouraging Italian headline (max 7 words, no emojis, no markdown)"),
            "summary" to Schema.string(description = "2-sentence Italian ludic wellness summary based on the user's 4 pillars, plain text without markdown or emojis"),
            "strongestPillar" to Schema.enumeration(
                values = listOf("Logica", "Memoria", "Riflessi", "Empatia"),
                description = "User's highest performing pillar"
            ),
            "focusPillar" to Schema.enumeration(
                values = listOf("Logica", "Memoria", "Riflessi", "Empatia"),
                description = "Pillar to train next for balance"
            ),
            "recommendedGameId" to Schema.enumeration(
                values = GAMES_CATALOG.map { it.id },
                description = "Exact gameId from the catalog to recommend next"
            ),
            "actionableTip" to Schema.string(description = "One concrete, calm Italian training tip (non-medical, plain text)")
        )
    )

    private fun buildModel(modelName: String) = Firebase.ai.generativeModel(
        modelName = modelName,
        generationConfig = generationConfig {
            responseMimeType = "application/json"
            responseSchema = verdictSchema
            temperature = 0.55f
        }
    )

    suspend fun generateCoachVerdict(
        profile: UserProfile,
        recentSessions: List<TrainingSession>,
        forceRefresh: Boolean = false
    ): Result<CoachVerdict> {
        val contextHash = computeContextHash(profile, recentSessions)
        if (!forceRefresh) {
            synchronized(verdictCache) {
                verdictCache[contextHash]?.let { cached ->
                    return Result.success(cached)
                }
            }
        }

        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val balanceInsight = CognitiveBalanceEngine.evaluate(profile, recentSessions, null, todayKey)

        val recentSummary = if (recentSessions.isEmpty()) {
            "Nessuna sessione completata di recente."
        } else {
            recentSessions.take(8).joinToString("; ") { s ->
                "${s.gameId} (${s.pillar}, Lv.${s.level}): score ${s.score}, acc ${s.accuracyPercent}%, rt ${s.avgReactionMs}ms"
            }
        }

        val restNote = balanceInsight.restingPillar?.let {
            "Pilastro in riposo consigliato oggi (oltre 3 sessioni): ${it.titleIt}."
        } ?: "Nessun sovraccarico rilevato oggi."

        val prompt = """
            Sei il Coach Cognitivo ludico dell'app AllenaMente.
            Regole OBBLIGATORIE di conformità Play Store e stile editoriale:
            - Rispondi esclusivamente in italiano naturale, caldo e motivante.
            - NON usare MAI emoji né marcatori markdown (niente asterischi *, **, ### o backtick).
            - VIETATO fare diagnosi, claim medici o usare termini clinici o "età neurale".
            - Parla solo di allenamento ludico, ritmo, equilibrio tra i 4 pilastri e Indice di Velocità (pt).

            Dati utente:
            - Nome: ${profile.displayName}
            - Fascia d'età: ${profile.ageCohort}
            - Serie attiva: ${profile.currentStreak} giorni (migliore: ${profile.bestStreak})
            - Punteggi 4 Pilastri (0-100): Logica=${profile.logicScore}, Memoria=${profile.memoryScore}, Riflessi=${profile.reflexScore}, Empatia=${profile.empathyScore}
            - Indice di Armonia 4 Pilastri: ${balanceInsight.balanceIndex}%
            - Indice di Velocità: ${profile.speedIndex} pt
            - Modalità Flusso Zen attiva: ${profile.zenFlowMode}
            - $restNote
            - Ultime sessioni: $recentSummary

            Genera il verdetto JSON strutturato consigliando il gioco più adatto dal catalogo.
        """.trimIndent()

        var lastError: Exception? = null
        for (modelName in modelFallbackChain) {
            try {
                val response = withTimeout(35_000L) {
                    buildModel(modelName).generateContent(prompt)
                }
                val rawJson = response.text
                if (rawJson.isNullOrBlank()) {
                    lastError = IllegalStateException("Risposta vuota da $modelName")
                    continue
                }

                val json = JSONObject(rawJson)
                val validIds = GAMES_CATALOG.map { it.id }.toSet()
                val rawGameId = json.optString("recommendedGameId", balanceInsight.recommendedGame.id)
                val safeGameId = if (rawGameId in validIds) rawGameId else balanceInsight.recommendedGame.id

                val verdict = CoachVerdict(
                    headline = sanitizeCoachText(json.optString("headline", "Ottimo equilibrio mentale")),
                    summary = sanitizeCoachText(json.optString("summary", "Continua con sessioni brevi e costanti sui 4 pilastri.")),
                    strongestPillar = sanitizePillar(json.optString("strongestPillar", balanceInsight.strongestPillar.titleIt)),
                    focusPillar = sanitizePillar(json.optString("focusPillar", balanceInsight.focusPillar.titleIt)),
                    recommendedGameId = safeGameId,
                    actionableTip = sanitizeCoachText(json.optString("actionableTip", balanceInsight.recommendationReason))
                )
                cacheVerdict(contextHash, verdict)
                return Result.success(verdict)
            } catch (e: TimeoutCancellationException) {
                Log.w(COACH_TAG, "Model $modelName timed out", e)
                lastError = e
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(COACH_TAG, "Model $modelName failed: ${e.localizedMessage}", e)
                lastError = e
            }
        }

        // Graceful deterministic offline fallback (never leaves user stuck if offline / unavailable)
        Log.i(COACH_TAG, "Falling back to deterministic offline Coach analysis after error: ${lastError?.localizedMessage}")
        val offlineVerdict = CognitiveBalanceEngine.buildOfflineVerdict(profile, recentSessions, todayKey)
        val sanitizedOffline = offlineVerdict.copy(
            headline = sanitizeCoachText(offlineVerdict.headline),
            summary = sanitizeCoachText(offlineVerdict.summary),
            actionableTip = sanitizeCoachText(offlineVerdict.actionableTip)
        )
        return Result.success(sanitizedOffline)
    }

    private fun cacheVerdict(key: String, verdict: CoachVerdict) {
        synchronized(verdictCache) {
            if (verdictCache.size >= 12) {
                val eldest = verdictCache.keys.firstOrNull()
                if (eldest != null) verdictCache.remove(eldest)
            }
            verdictCache[key] = verdict
        }
    }

    companion object {
        private val MARKDOWN_REGEX = Regex("[*`#_~]+")
        private val MULTI_SPACE_REGEX = Regex("\\s{2,}")
        private val CLINICAL_GUARDRAILS = listOf(
            Regex("(?i)et[aà]\\s+neurale") to "Indice di Velocità",
            Regex("(?i)diagnosi") to "panoramica ludica",
            Regex("(?i)terapi[ae]") to "allenamento ludico",
            Regex("(?i)clinic[oaie]") to "ricreativo",
            Regex("(?i)medic[oaie]") to "di benessere",
            Regex("(?i)demenza|alzheimer") to "allenamento mentale"
        )

        /**
         * Strips markdown markers, emojis/surrogate symbols, and enforces Play Store non-clinical copy.
         */
        fun sanitizeCoachText(raw: String): String {
            val noMarkdown = raw
                .replace(Regex("(?m)^\\s*[-*]\\s+"), "• ")
                .replace(MARKDOWN_REGEX, "")

            // Strip emoji / pictographs / surrogate pairs while preserving Italian accented letters & punctuation
            val cleanChars = buildString(noMarkdown.length) {
                var i = 0
                while (i < noMarkdown.length) {
                    val cp = noMarkdown.codePointAt(i)
                    val charCount = Character.charCount(cp)
                    val isEmojiOrSymbol = cp in 0x1F000..0x1FAFF ||
                        cp in 0x2600..0x27BF ||
                        cp == 0xFE0F ||
                        cp == 0x200D
                    if (!isEmojiOrSymbol) {
                        appendCodePoint(cp)
                    }
                    i += charCount
                }
            }

            var safe = cleanChars.trim().replace(MULTI_SPACE_REGEX, " ")
            for ((pattern, replacement) in CLINICAL_GUARDRAILS) {
                safe = safe.replace(pattern, replacement)
            }
            return safe
        }

        private fun sanitizePillar(raw: String): String {
            return when (raw.trim().lowercase(Locale.ITALIAN)) {
                "logica", "logic" -> "Logica"
                "memoria", "memory" -> "Memoria"
                "riflessi", "reflex" -> "Riflessi"
                "empatia", "empathy" -> "Empatia"
                else -> "Logica"
            }
        }

        /**
         * Deterministic SHA-256 fingerprint of user cognitive state + top 5 recent sessions.
         */
        fun computeContextHash(profile: UserProfile, recentSessions: List<TrainingSession>): String {
            val payload = buildString {
                append(profile.userId).append('|')
                append(profile.ageCohort).append('|')
                append(profile.currentStreak).append('|')
                append(profile.logicScore).append(',')
                append(profile.memoryScore).append(',')
                append(profile.reflexScore).append(',')
                append(profile.empathyScore).append('|')
                append(profile.speedIndex).append('|')
                append(profile.zenFlowMode).append('|')
                recentSessions.take(5).forEach { s ->
                    append(s.gameId).append(':')
                        .append(s.level).append(':')
                        .append(s.score).append(':')
                        .append(s.accuracyPercent).append(':')
                        .append(s.dateKey).append(';')
                }
            }
            val digest = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}

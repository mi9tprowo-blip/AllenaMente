package com.example

import com.example.data.firebase.AiCoachService
import com.example.data.model.CognitivePillar
import com.example.data.model.DailyStat
import com.example.data.model.GAMES_CATALOG
import com.example.data.model.TrainingSession
import com.example.data.model.UserProfile
import com.example.engine.CircuitGenerator
import com.example.engine.CognitiveBalanceEngine
import com.example.engine.Mulberry32
import com.example.engine.PipeKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AllenaMenteEngineTest {

    @Test
    fun `Mulberry32 produces identical sequence for identical seed`() {
        val seed = Mulberry32.seedFor("neural-circuit", 12)
        val a = Mulberry32(seed)
        val b = Mulberry32(seed)
        repeat(20) {
            assertEquals(a.nextInt(1000), b.nextInt(1000))
        }
    }

    @Test
    fun `Mulberry32 produces different seeds across games and levels`() {
        val s1 = Mulberry32.seedFor("neural-circuit", 1)
        val s2 = Mulberry32.seedFor("neural-circuit", 2)
        val s3 = Mulberry32.seedFor("perspective-compass", 1)
        assertNotEquals(s1, s2)
        assertNotEquals(s1, s3)
    }

    @Test
    fun `CircuitGenerator generates valid grid with source and targets across levels`() {
        for (level in listOf(1, 20, 60)) {
            val puzzle = CircuitGenerator.generate(level, Mulberry32.seedFor("neural-circuit", level))
            assertEquals(puzzle.size * puzzle.size, puzzle.nodes.size)
            assertEquals(1, puzzle.nodes.count { it.kind == PipeKind.SOURCE })
            assertTrue(puzzle.nodes.count { it.kind == PipeKind.TARGET } >= 1)
        }
    }

    @Test
    fun `sanitizeCoachText strips markdown, emojis, and clinical phrases`() {
        val dirty = "### **Ottimo lavoro!** 🚀 La tua età neurale e diagnosi clinica sono *perfette* 🧠"
        val clean = AiCoachService.sanitizeCoachText(dirty)
        assertFalse(clean.contains("*"))
        assertFalse(clean.contains("#"))
        assertFalse(clean.contains("🚀"))
        assertFalse(clean.contains("🧠"))
        assertFalse(clean.lowercase().contains("età neurale"))
        assertFalse(clean.lowercase().contains("diagnosi"))
        assertTrue(clean.contains("Indice di Velocità"))
    }

    @Test
    fun `computeContextHash is deterministic and changes when session or score updates`() {
        val baseProfile = UserProfile(userId = "u1", logicScore = 60, memoryScore = 55, reflexScore = 50, empathyScore = 65)
        val h1 = AiCoachService.computeContextHash(baseProfile, emptyList())
        val h2 = AiCoachService.computeContextHash(baseProfile, emptyList())
        assertEquals(h1, h2)
        assertEquals(64, h1.length) // SHA-256 hex length

        val updatedProfile = baseProfile.copy(logicScore = 72)
        val h3 = AiCoachService.computeContextHash(updatedProfile, emptyList())
        assertNotEquals(h1, h3)
    }

    @Test
    fun `CognitiveBalanceEngine returns 100 balance for uniform pillars and lower for skewed pillars`() {
        val uniform = UserProfile(logicScore = 70, memoryScore = 70, reflexScore = 70, empathyScore = 70)
        val skewed = UserProfile(logicScore = 95, memoryScore = 30, reflexScore = 90, empathyScore = 25)

        val uniformInsight = CognitiveBalanceEngine.evaluate(uniform, emptyList(), null, "2026-11-26")
        val skewedInsight = CognitiveBalanceEngine.evaluate(skewed, emptyList(), null, "2026-11-26")

        assertEquals(100, uniformInsight.balanceIndex)
        assertTrue(skewedInsight.balanceIndex < 60)
        assertEquals(CognitivePillar.EMPATHY, skewedInsight.focusPillar)
        assertNull(uniformInsight.restingPillar)
    }

    @Test
    fun `CognitiveBalanceEngine detects pillar fatigue after 3 daily sessions and rotates recommendation`() {
        val profile = UserProfile(logicScore = 40, memoryScore = 60, reflexScore = 70, empathyScore = 80)
        val today = "2026-11-26"
        val sessions = List(3) {
            TrainingSession(
                id = "s$it",
                gameId = "neural-circuit",
                pillar = CognitivePillar.LOGIC.name,
                dateKey = today
            )
        }
        val insight = CognitiveBalanceEngine.evaluate(profile, sessions, null, today)
        assertEquals(CognitivePillar.LOGIC, insight.restingPillar)
        assertNotEquals(CognitivePillar.LOGIC, insight.focusPillar)
        assertNotEquals("neural-circuit", insight.recommendedGame.id)
    }

    @Test
    fun `CognitiveBalanceEngine offline verdict references valid catalog game`() {
        val profile = UserProfile(displayName = "Alex", currentStreak = 4, logicScore = 80, memoryScore = 45, reflexScore = 70, empathyScore = 65)
        val verdict = CognitiveBalanceEngine.buildOfflineVerdict(profile, emptyList(), "2026-11-26")
        val catalogIds = GAMES_CATALOG.map { it.id }.toSet()
        assertTrue(verdict.recommendedGameId in catalogIds)
        assertEquals("Memoria", verdict.focusPillar)
        assertEquals("Logica", verdict.strongestPillar)
    }

    @Test
    fun `CognitiveBalanceEngine prioritizes uncompleted daily ring`() {
        val profile = UserProfile(logicScore = 85, memoryScore = 85, reflexScore = 85, empathyScore = 85)
        val todayStat = DailyStat(
            dateKey = "2026-11-26",
            logicCompleted = true,
            memoryCompleted = true,
            reflexCompleted = true,
            empathyCompleted = false
        )
        val insight = CognitiveBalanceEngine.evaluate(profile, emptyList(), todayStat, "2026-11-26")
        assertEquals(CognitivePillar.EMPATHY, insight.focusPillar)
    }
}

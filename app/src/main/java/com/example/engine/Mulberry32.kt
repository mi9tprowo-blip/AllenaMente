package com.example.engine

import android.net.Uri
import com.example.data.model.DuelChallenge
import com.example.data.model.gameById

/**
 * Deterministic 32-bit Mulberry32 PRNG.
 * Guarantees identical procedural puzzles for any (gameId, level, seed) across devices and 1v1 duels.
 */
class Mulberry32(initialSeed: Int) {
    private var state: Int = initialSeed

    fun nextFloat(): Float {
        state = state + 0x6D2B79F5
        var t = state
        t = (t xor (t ushr 15)) * (t or 1)
        t = t xor (t + ((t xor (t ushr 7)) * (t or 61)))
        val unsigned = (t xor (t ushr 14)).toLong() and 0xFFFFFFFFL
        return (unsigned.toDouble() / 4294967296.0).toFloat()
    }

    fun nextInt(bound: Int): Int {
        if (bound <= 1) return 0
        return (nextFloat() * bound).toInt().coerceIn(0, bound - 1)
    }

    fun nextInt(minInclusive: Int, maxInclusive: Int): Int {
        if (maxInclusive <= minInclusive) return minInclusive
        return minInclusive + nextInt(maxInclusive - minInclusive + 1)
    }

    fun <T> pick(items: List<T>): T = items[nextInt(items.size)]

    fun <T> shuffled(items: List<T>): List<T> {
        val list = items.toMutableList()
        for (i in list.lastIndex downTo 1) {
            val j = nextInt(i + 1)
            val tmp = list[i]
            list[i] = list[j]
            list[j] = tmp
        }
        return list
    }

    companion object {
        fun seedFor(gameId: String, level: Int, customSeed: Int? = null): Int {
            if (customSeed != null) return customSeed
            var h = 2166136261L.toInt()
            for (c in gameId) {
                h = (h xor c.code) * 16777619
            }
            return h xor (level * 0x9E3779B9.toInt())
        }
    }
}

object DuelLinkCodec {
    fun buildDuelUri(
        gameId: String,
        level: Int,
        seed: Int,
        challengerName: String,
        score: Int,
        reactionMs: Int,
        accuracy: Int
    ): String {
        val safeName = Uri.encode(challengerName.take(24))
        return "allenamente://duel?g=$gameId&l=$level&s=$seed&n=$safeName&sc=$score&rt=$reactionMs&ac=$accuracy"
    }

    fun buildShareMessage(
        gameId: String,
        level: Int,
        seed: Int,
        challengerName: String,
        score: Int,
        reactionMs: Int,
        accuracy: Int
    ): String {
        val game = gameById(gameId)
        val link = buildDuelUri(gameId, level, seed, challengerName, score, reactionMs, accuracy)
        return "Sfida AllenaMente 1v1 su ${game.titleIt} (Lv.$level)!\n" +
            "$challengerName ha totalizzato $score pt ($accuracy% • ${reactionMs}ms).\n" +
            "Riesci a batterlo sullo stesso identico schema (seed #$seed)?\n$link"
    }

    fun parseUri(uri: Uri?): DuelChallenge? {
        if (uri == null || uri.scheme != "allenamente" || uri.host != "duel") return null
        val gameId = uri.getQueryParameter("g")?.takeIf { it.isNotBlank() } ?: return null
        val level = uri.getQueryParameter("l")?.toIntOrNull()?.coerceIn(1, 100) ?: 1
        val seed = uri.getQueryParameter("s")?.toIntOrNull() ?: return null
        val name = uri.getQueryParameter("n")?.take(24)?.ifBlank { "Amico" } ?: "Amico"
        val score = uri.getQueryParameter("sc")?.toIntOrNull()?.coerceIn(0, 10000) ?: 0
        val rt = uri.getQueryParameter("rt")?.toIntOrNull()?.coerceIn(0, 60000) ?: 800
        val acc = uri.getQueryParameter("ac")?.toIntOrNull()?.coerceIn(0, 100) ?: 80
        return DuelChallenge(
            gameId = gameId,
            level = level,
            seed = seed,
            challengerName = name,
            challengerScore = score,
            challengerReactionMs = rt,
            challengerAccuracy = acc
        )
    }
}

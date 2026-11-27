package com.example.data.firebase

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics

private const val ANALYTICS_TAG = "AllenaMenteAnalytics"
private const val ANALYTICS_PREFS_NAME = "allenamente_analytics_prefs"
private const val KEY_ANALYTICS_ENABLED = "analytics_collection_enabled"

enum class GameLaunchSource(val analyticsValue: String) {
    HOME_RECOMMENDATION("home_recommendation"),
    DAILY_CIRCUIT("daily_circuit"),
    GYM_CATALOG("gym_catalog"),
    DUEL("duel"),
    AI_COACH("ai_coach"),
    REPLAY("replay")
}

object AnalyticsSanitizer {
    private val INVALID_KEY_CHARS = Regex("[^a-zA-Z0-9_]")

    fun sanitizeName(raw: String, maxLength: Int = 40): String {
        val cleaned = raw.trim()
            .replace('-', '_')
            .replace(' ', '_')
            .replace(INVALID_KEY_CHARS, "")
            .lowercase()
        val prefixed = if (cleaned.isEmpty() || !cleaned.first().isLetter()) {
            "ev_$cleaned"
        } else {
            cleaned
        }
        return prefixed.take(maxLength)
    }

    fun sanitizeParamValue(raw: String, maxLength: Int = 100): String =
        raw.trim().take(maxLength)

    fun sanitizeUserPropertyValue(raw: String): String =
        raw.trim().take(36)
}

interface AnalyticsTracker {
    val isCollectionEnabled: Boolean

    fun setCollectionEnabled(enabled: Boolean)
    fun logScreenView(screenName: String, screenClass: String = "MainActivity")
    fun logLogin(method: String)
    fun logSignOut(mode: String)
    fun logOnboardingCompleted(ageCohort: String)
    fun logCalibrationCompleted(speedIndex: Int)
    fun logCalibrationSkipped()
    fun logGameStart(
        gameId: String,
        pillar: String,
        level: Int,
        zenMode: Boolean,
        source: GameLaunchSource,
        isDuel: Boolean
    )
    fun logGameEnd(
        gameId: String,
        pillar: String,
        level: Int,
        score: Int,
        accuracyPercent: Int,
        avgReactionMs: Int,
        durationSec: Int,
        zenMode: Boolean,
        unlockedNextLevel: Boolean,
        isDuel: Boolean
    )
    fun logDailyCircuitStarted(stage: Int, gameId: String)
    fun logDailyCircuitStageAdvanced(nextStage: Int, gameId: String)
    fun logDailyCircuitCompleted()
    fun logAiCoachRequested(forceRefresh: Boolean, sessionCount: Int)
    fun logAiCoachVerdictShown(recommendedGameId: String, focusPillar: String)
    fun logDuelReceived(gameId: String, level: Int)
    fun logDuelAccepted(gameId: String, level: Int)
    fun logDuelDismissed(gameId: String)
    fun logShare(contentType: String, itemId: String)
    fun logTutorialBegin()
    fun logTutorialComplete()
    fun logPreferenceChanged(settingName: String, settingValue: String)
    fun logResultsDeepDiveToggled(expanded: Boolean)
    fun logGdprDataDeleted(mode: String)
    fun syncUserProperties(
        theme: String,
        mascot: String,
        zenMode: Boolean,
        ageCohort: String,
        accountMode: String
    )
}

object NoOpAnalyticsTracker : AnalyticsTracker {
    override val isCollectionEnabled: Boolean = false
    override fun setCollectionEnabled(enabled: Boolean) = Unit
    override fun logScreenView(screenName: String, screenClass: String) = Unit
    override fun logLogin(method: String) = Unit
    override fun logSignOut(mode: String) = Unit
    override fun logOnboardingCompleted(ageCohort: String) = Unit
    override fun logCalibrationCompleted(speedIndex: Int) = Unit
    override fun logCalibrationSkipped() = Unit
    override fun logGameStart(
        gameId: String,
        pillar: String,
        level: Int,
        zenMode: Boolean,
        source: GameLaunchSource,
        isDuel: Boolean
    ) = Unit
    override fun logGameEnd(
        gameId: String,
        pillar: String,
        level: Int,
        score: Int,
        accuracyPercent: Int,
        avgReactionMs: Int,
        durationSec: Int,
        zenMode: Boolean,
        unlockedNextLevel: Boolean,
        isDuel: Boolean
    ) = Unit
    override fun logDailyCircuitStarted(stage: Int, gameId: String) = Unit
    override fun logDailyCircuitStageAdvanced(nextStage: Int, gameId: String) = Unit
    override fun logDailyCircuitCompleted() = Unit
    override fun logAiCoachRequested(forceRefresh: Boolean, sessionCount: Int) = Unit
    override fun logAiCoachVerdictShown(recommendedGameId: String, focusPillar: String) = Unit
    override fun logDuelReceived(gameId: String, level: Int) = Unit
    override fun logDuelAccepted(gameId: String, level: Int) = Unit
    override fun logDuelDismissed(gameId: String) = Unit
    override fun logShare(contentType: String, itemId: String) = Unit
    override fun logTutorialBegin() = Unit
    override fun logTutorialComplete() = Unit
    override fun logPreferenceChanged(settingName: String, settingValue: String) = Unit
    override fun logResultsDeepDiveToggled(expanded: Boolean) = Unit
    override fun logGdprDataDeleted(mode: String) = Unit
    override fun syncUserProperties(
        theme: String,
        mascot: String,
        zenMode: Boolean,
        ageCohort: String,
        accountMode: String
    ) = Unit
}

class FirebaseAnalyticsTracker(context: Context) : AnalyticsTracker {

    private val prefs = context.applicationContext.getSharedPreferences(
        ANALYTICS_PREFS_NAME,
        Context.MODE_PRIVATE
    )

    @Volatile
    private var collectionEnabledState: Boolean = prefs.getBoolean(KEY_ANALYTICS_ENABLED, true)

    override val isCollectionEnabled: Boolean
        get() = collectionEnabledState

    private val analytics: FirebaseAnalytics? by lazy {
        runCatching { Firebase.analytics }
            .onFailure { Log.w(ANALYTICS_TAG, "FirebaseAnalytics unavailable in this runtime", it) }
            .getOrNull()
    }

    init {
        runCatching {
            analytics?.setAnalyticsCollectionEnabled(collectionEnabledState)
        }.onFailure {
            Log.w(ANALYTICS_TAG, "Failed to apply initial analytics collection flag", it)
        }
    }

    override fun setCollectionEnabled(enabled: Boolean) {
        if (!enabled && collectionEnabledState) {
            // Log opt-out once before disabling collection
            logSafeEvent("preference_changed") {
                putString("setting_name", "analytics")
                putString("setting_value", "false")
            }
        }
        collectionEnabledState = enabled
        prefs.edit().putBoolean(KEY_ANALYTICS_ENABLED, enabled).apply()
        runCatching {
            analytics?.setAnalyticsCollectionEnabled(enabled)
        }.onFailure {
            Log.w(ANALYTICS_TAG, "Failed to update analytics collection state", it)
        }
        if (enabled) {
            logSafeEvent("preference_changed") {
                putString("setting_name", "analytics")
                putString("setting_value", "true")
            }
        }
    }

    override fun logScreenView(screenName: String, screenClass: String) {
        logSafeEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, AnalyticsSanitizer.sanitizeParamValue(screenName))
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, AnalyticsSanitizer.sanitizeParamValue(screenClass))
        }
    }

    override fun logLogin(method: String) {
        logSafeEvent(FirebaseAnalytics.Event.LOGIN) {
            putString(FirebaseAnalytics.Param.METHOD, AnalyticsSanitizer.sanitizeParamValue(method))
        }
    }

    override fun logSignOut(mode: String) {
        logSafeEvent("account_signed_out") {
            putString("account_mode", AnalyticsSanitizer.sanitizeParamValue(mode))
        }
    }

    override fun logOnboardingCompleted(ageCohort: String) {
        logSafeEvent("onboarding_completed") {
            putString("age_cohort", AnalyticsSanitizer.sanitizeParamValue(ageCohort))
        }
        setSafeUserProperty("age_cohort", ageCohort)
    }

    override fun logCalibrationCompleted(speedIndex: Int) {
        logSafeEvent("calibration_completed") {
            putLong("speed_index", speedIndex.toLong())
        }
    }

    override fun logCalibrationSkipped() {
        logSafeEvent("calibration_skipped") {}
    }

    override fun logGameStart(
        gameId: String,
        pillar: String,
        level: Int,
        zenMode: Boolean,
        source: GameLaunchSource,
        isDuel: Boolean
    ) {
        logSafeEvent(FirebaseAnalytics.Event.LEVEL_START) {
            putString(FirebaseAnalytics.Param.LEVEL_NAME, AnalyticsSanitizer.sanitizeParamValue(gameId))
            putLong(FirebaseAnalytics.Param.LEVEL, level.toLong())
            putString("pillar", AnalyticsSanitizer.sanitizeParamValue(pillar.lowercase()))
            putString("zen_mode", zenMode.toString())
            putString("launch_source", source.analyticsValue)
            putString("is_duel", isDuel.toString())
        }
    }

    override fun logGameEnd(
        gameId: String,
        pillar: String,
        level: Int,
        score: Int,
        accuracyPercent: Int,
        avgReactionMs: Int,
        durationSec: Int,
        zenMode: Boolean,
        unlockedNextLevel: Boolean,
        isDuel: Boolean
    ) {
        val passed = accuracyPercent >= 70
        logSafeEvent(FirebaseAnalytics.Event.LEVEL_END) {
            putString(FirebaseAnalytics.Param.LEVEL_NAME, AnalyticsSanitizer.sanitizeParamValue(gameId))
            putLong(FirebaseAnalytics.Param.LEVEL, level.toLong())
            putLong(FirebaseAnalytics.Param.SUCCESS, if (passed) 1L else 0L)
            putString("pillar", AnalyticsSanitizer.sanitizeParamValue(pillar.lowercase()))
            putLong("score", score.toLong())
            putLong("accuracy_percent", accuracyPercent.toLong())
            putLong("avg_reaction_ms", avgReactionMs.toLong())
            putLong("duration_sec", durationSec.toLong())
            putString("zen_mode", zenMode.toString())
            putString("unlocked_next_level", unlockedNextLevel.toString())
            putString("is_duel", isDuel.toString())
        }

        logSafeEvent(FirebaseAnalytics.Event.POST_SCORE) {
            putLong(FirebaseAnalytics.Param.SCORE, score.toLong())
            putLong(FirebaseAnalytics.Param.LEVEL, level.toLong())
            putString(FirebaseAnalytics.Param.CHARACTER, AnalyticsSanitizer.sanitizeParamValue(gameId))
        }

        if (unlockedNextLevel) {
            logSafeEvent(FirebaseAnalytics.Event.LEVEL_UP) {
                putLong(FirebaseAnalytics.Param.LEVEL, (level + 1).coerceAtMost(100).toLong())
                putString(FirebaseAnalytics.Param.CHARACTER, AnalyticsSanitizer.sanitizeParamValue(gameId))
            }
        }
    }

    override fun logDailyCircuitStarted(stage: Int, gameId: String) {
        logSafeEvent("daily_circuit_started") {
            putLong("stage", (stage + 1).toLong())
            putString("game_id", AnalyticsSanitizer.sanitizeParamValue(gameId))
        }
    }

    override fun logDailyCircuitStageAdvanced(nextStage: Int, gameId: String) {
        logSafeEvent("daily_circuit_stage_advanced") {
            putLong("stage", (nextStage + 1).toLong())
            putString("game_id", AnalyticsSanitizer.sanitizeParamValue(gameId))
        }
    }

    override fun logDailyCircuitCompleted() {
        logSafeEvent("daily_circuit_completed") {
            putLong("stages_total", 4L)
        }
    }

    override fun logAiCoachRequested(forceRefresh: Boolean, sessionCount: Int) {
        logSafeEvent("ai_coach_requested") {
            putString("force_refresh", forceRefresh.toString())
            putLong("session_count", sessionCount.toLong())
        }
    }

    override fun logAiCoachVerdictShown(recommendedGameId: String, focusPillar: String) {
        logSafeEvent("ai_coach_verdict_shown") {
            putString("recommended_game", AnalyticsSanitizer.sanitizeParamValue(recommendedGameId))
            putString("focus_pillar", AnalyticsSanitizer.sanitizeParamValue(focusPillar))
        }
    }

    override fun logDuelReceived(gameId: String, level: Int) {
        logSafeEvent("duel_received") {
            putString("game_id", AnalyticsSanitizer.sanitizeParamValue(gameId))
            putLong("level", level.toLong())
        }
    }

    override fun logDuelAccepted(gameId: String, level: Int) {
        logSafeEvent("duel_accepted") {
            putString("game_id", AnalyticsSanitizer.sanitizeParamValue(gameId))
            putLong("level", level.toLong())
        }
    }

    override fun logDuelDismissed(gameId: String) {
        logSafeEvent("duel_dismissed") {
            putString("game_id", AnalyticsSanitizer.sanitizeParamValue(gameId))
        }
    }

    override fun logShare(contentType: String, itemId: String) {
        logSafeEvent(FirebaseAnalytics.Event.SHARE) {
            putString(FirebaseAnalytics.Param.CONTENT_TYPE, AnalyticsSanitizer.sanitizeParamValue(contentType))
            putString(FirebaseAnalytics.Param.ITEM_ID, AnalyticsSanitizer.sanitizeParamValue(itemId))
        }
    }

    override fun logTutorialBegin() {
        logSafeEvent(FirebaseAnalytics.Event.TUTORIAL_BEGIN) {}
    }

    override fun logTutorialComplete() {
        logSafeEvent(FirebaseAnalytics.Event.TUTORIAL_COMPLETE) {}
    }

    override fun logPreferenceChanged(settingName: String, settingValue: String) {
        logSafeEvent("preference_changed") {
            putString("setting_name", AnalyticsSanitizer.sanitizeParamValue(settingName))
            putString("setting_value", AnalyticsSanitizer.sanitizeParamValue(settingValue))
        }
    }

    override fun logResultsDeepDiveToggled(expanded: Boolean) {
        logSafeEvent("results_deep_dive_toggled") {
            putString("expanded", expanded.toString())
        }
    }

    override fun logGdprDataDeleted(mode: String) {
        logSafeEvent("gdpr_data_deleted") {
            putString("account_mode", AnalyticsSanitizer.sanitizeParamValue(mode))
        }
    }

    override fun syncUserProperties(
        theme: String,
        mascot: String,
        zenMode: Boolean,
        ageCohort: String,
        accountMode: String
    ) {
        if (!collectionEnabledState) return
        setSafeUserProperty("app_theme", theme)
        setSafeUserProperty("selected_mascot", mascot)
        setSafeUserProperty("zen_flow_mode", zenMode.toString())
        setSafeUserProperty("age_cohort", ageCohort)
        setSafeUserProperty("account_mode", accountMode)
    }

    private fun logSafeEvent(name: String, populate: Bundle.() -> Unit) {
        if (!collectionEnabledState) return
        runCatching {
            val bundle = Bundle().apply(populate)
            analytics?.logEvent(AnalyticsSanitizer.sanitizeName(name), bundle)
        }.onFailure {
            Log.w(ANALYTICS_TAG, "Failed to log analytics event '$name'", it)
        }
    }

    private fun setSafeUserProperty(name: String, value: String) {
        if (!collectionEnabledState) return
        runCatching {
            analytics?.setUserProperty(
                AnalyticsSanitizer.sanitizeName(name, maxLength = 24),
                AnalyticsSanitizer.sanitizeUserPropertyValue(value)
            )
        }.onFailure {
            Log.w(ANALYTICS_TAG, "Failed to set user property '$name'", it)
        }
    }
}

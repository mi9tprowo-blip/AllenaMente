package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.AiCoachService
import com.example.data.firebase.AllenaMenteRepository
import com.example.data.firebase.AnalyticsTracker
import com.example.data.firebase.GameLaunchSource
import com.example.data.firebase.NoOpAnalyticsTracker
import com.example.data.model.AGE_COHORTS
import com.example.data.model.AppTheme
import com.example.data.model.CoachVerdict
import com.example.data.model.CognitivePillar
import com.example.data.model.DailyStat
import com.example.data.model.DuelChallenge
import com.example.data.model.GAMES_CATALOG
import com.example.data.model.MascotType
import com.example.data.model.SessionResult
import com.example.data.model.TrainingSession
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private const val VM_TAG = "AllenaMenteVM"

data class ActiveGameLaunch(
    val gameId: String,
    val level: Int,
    val seed: Int,
    val zenMode: Boolean,
    val duelChallenge: DuelChallenge? = null,
    val dailyCircuitStage: Int? = null // 0..3 when playing the 4-stage Daily Neural Circuit
)

class AllenaMenteViewModel(
    private val repository: AllenaMenteRepository?,
    private val aiCoachService: AiCoachService,
    val currentUserId: String,
    googleDisplayName: String?,
    val isProvisionalMode: Boolean = false,
    private val analytics: AnalyticsTracker = NoOpAnalyticsTracker
) : ViewModel() {

    private val useLocalOnly: Boolean = isProvisionalMode || repository == null

    private val _analyticsEnabled = MutableStateFlow(analytics.isCollectionEnabled)
    val analyticsEnabled: StateFlow<Boolean> = _analyticsEnabled.asStateFlow()

    fun setAnalyticsEnabled(enabled: Boolean) {
        analytics.setCollectionEnabled(enabled)
        _analyticsEnabled.value = analytics.isCollectionEnabled
    }

    val todayDateKey: String = repository?.todayDateKey()
        ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    // Local in-memory state used exclusively in Modalità provvisoria (never hits Firestore unauthenticated)
    private val localProfile = MutableStateFlow<UiState<UserProfile?>>(
        UiState.Success(
            UserProfile(
                id = currentUserId,
                userId = currentUserId,
                displayName = googleDisplayName?.takeIf { it.isNotBlank() } ?: "Tester Provvisorio",
                termsAccepted = true,
                calibrationCompleted = false,
                unlockedLevels = GAMES_CATALOG.associate { it.id to 1L }
            )
        )
    )
    private val localSessions = MutableStateFlow<UiState<List<TrainingSession>>>(UiState.Success(emptyList()))
    private val localDailyStats = MutableStateFlow<UiState<List<DailyStat>>>(UiState.Success(emptyList()))

    // Two-tier Flow error unwrapping for UserProfile (Firestore when authenticated, in-memory when provisional)
    val profileState: StateFlow<UiState<UserProfile?>> = if (useLocalOnly) {
        localProfile.asStateFlow()
    } else {
        repository!!.observeUserProfile(currentUserId)
            .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
            .catch { error ->
                val fsError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsError == null && error is CancellationException) throw error
                val root = fsError ?: error
                Log.w(VM_TAG, "Error observing profile", root)
                emit(UiState.Error(root.localizedMessage ?: "Errore nel caricamento profilo."))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)
    }

    val sessionsState: StateFlow<UiState<List<TrainingSession>>> = if (useLocalOnly) {
        localSessions.asStateFlow()
    } else {
        repository!!.observeRecentSessions(currentUserId)
            .map<List<TrainingSession>, UiState<List<TrainingSession>>> { UiState.Success(it) }
            .catch { error ->
                val fsError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsError == null && error is CancellationException) throw error
                val root = fsError ?: error
                Log.w(VM_TAG, "Error observing sessions", root)
                emit(UiState.Error(root.localizedMessage ?: "Errore nel caricamento sessioni."))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)
    }

    val dailyStatsState: StateFlow<UiState<List<DailyStat>>> = if (useLocalOnly) {
        localDailyStats.asStateFlow()
    } else {
        repository!!.observeDailyStats(currentUserId)
            .map<List<DailyStat>, UiState<List<DailyStat>>> { UiState.Success(it) }
            .catch { error ->
                val fsError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsError == null && error is CancellationException) throw error
                val root = fsError ?: error
                Log.w(VM_TAG, "Error observing daily stats", root)
                emit(UiState.Error(root.localizedMessage ?: "Errore nel caricamento statistiche."))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)
    }

    private val _coachState = MutableStateFlow<UiState<CoachVerdict>>(UiState.Idle)
    val coachState: StateFlow<UiState<CoachVerdict>> = _coachState.asStateFlow()

    private val _activeGame = MutableStateFlow<ActiveGameLaunch?>(null)
    val activeGame: StateFlow<ActiveGameLaunch?> = _activeGame.asStateFlow()

    private val _lastResult = MutableStateFlow<SessionResult?>(null)
    val lastResult: StateFlow<SessionResult?> = _lastResult.asStateFlow()

    private val _pendingDuel = MutableStateFlow<DuelChallenge?>(null)
    val pendingDuel: StateFlow<DuelChallenge?> = _pendingDuel.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _showCalibrationModal = MutableStateFlow(false)
    val showCalibrationModal: StateFlow<Boolean> = _showCalibrationModal.asStateFlow()

    // Daily 4-pillar circuit games
    val dailyCircuitGameIds: List<String> = listOf(
        "neural-circuit",      // Logic
        "light-matrix",        // Memory
        "impulse-inhibition",  // Reflex
        "perspective-compass"  // Empathy
    )

    init {
        if (!useLocalOnly) {
            viewModelScope.launch {
                val res = repository!!.ensureProfileExists(currentUserId, googleDisplayName)
                res.onFailure { err ->
                    _snackbarMessage.value = "Sync iniziale: ${err.localizedMessage ?: "modalità offline attiva"}"
                }
            }
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun setIncomingDuel(duel: DuelChallenge?) {
        _pendingDuel.value = duel
    }

    fun openCalibration() {
        _showCalibrationModal.value = true
    }

    fun closeCalibration() {
        _showCalibrationModal.value = false
    }

    private fun updateLocalProfile(transform: (UserProfile) -> UserProfile) {
        val current = (localProfile.value as? UiState.Success)?.data ?: return
        localProfile.value = UiState.Success(transform(current))
    }

    fun acceptOnboarding(displayName: String, ageCohort: String) {
        val safeCohort = if (ageCohort in AGE_COHORTS) ageCohort else "25-34"
        if (useLocalOnly) {
            val safeName = displayName.trim().take(50).ifBlank { "Tester Provvisorio" }
            updateLocalProfile { it.copy(displayName = safeName, ageCohort = safeCohort, termsAccepted = true) }
            analytics.logOnboardingCompleted(safeCohort)
            _showCalibrationModal.value = true
            return
        }
        viewModelScope.launch {
            val res = repository!!.acceptOnboarding(currentUserId, displayName, safeCohort)
            res.onSuccess {
                analytics.logOnboardingCompleted(safeCohort)
                _showCalibrationModal.value = true
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Impossibile salvare il profilo."
            }
        }
    }

    fun skipCalibration() {
        if (useLocalOnly) {
            updateLocalProfile { it.copy(calibrationCompleted = true) }
            analytics.logCalibrationSkipped()
            _showCalibrationModal.value = false
            return
        }
        viewModelScope.launch {
            repository!!.completeCalibration(currentUserId, null, null, null, null, null)
            analytics.logCalibrationSkipped()
            _showCalibrationModal.value = false
        }
    }

    fun saveCalibrationScores(logic: Int, memory: Int, reflex: Int, empathy: Int, speedIndex: Int) {
        if (useLocalOnly) {
            updateLocalProfile {
                it.copy(
                    calibrationCompleted = true,
                    logicScore = logic.coerceIn(0, 100),
                    memoryScore = memory.coerceIn(0, 100),
                    reflexScore = reflex.coerceIn(0, 100),
                    empathyScore = empathy.coerceIn(0, 100),
                    speedIndex = speedIndex.coerceIn(0, 1000)
                )
            }
            analytics.logCalibrationCompleted(speedIndex.coerceIn(0, 1000))
            _showCalibrationModal.value = false
            _snackbarMessage.value = "Profilo calibrato! Indice di Velocità: $speedIndex pt."
            return
        }
        viewModelScope.launch {
            val res = repository!!.completeCalibration(currentUserId, logic, memory, reflex, empathy, speedIndex)
            res.onSuccess {
                analytics.logCalibrationCompleted(speedIndex.coerceIn(0, 1000))
                _showCalibrationModal.value = false
                _snackbarMessage.value = "Profilo calibrato! Indice di Velocità: $speedIndex pt."
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Errore salvataggio calibrazione."
            }
        }
    }

    fun updatePreferences(
        theme: AppTheme? = null,
        mascot: MascotType? = null,
        soundEnabled: Boolean? = null,
        zenFlowMode: Boolean? = null,
        reminderEnabled: Boolean? = null,
        reminderHour: Int? = null,
        reminderMinute: Int? = null,
        displayName: String? = null
    ) {
        theme?.let { analytics.logPreferenceChanged("theme", it.name) }
        mascot?.let { analytics.logPreferenceChanged("mascot", it.name) }
        soundEnabled?.let { analytics.logPreferenceChanged("sound_enabled", it.toString()) }
        zenFlowMode?.let { analytics.logPreferenceChanged("zen_flow_mode", it.toString()) }
        reminderEnabled?.let { analytics.logPreferenceChanged("reminder_enabled", it.toString()) }

        if (useLocalOnly) {
            updateLocalProfile { p ->
                p.copy(
                    selectedTheme = theme?.name ?: p.selectedTheme,
                    selectedMascot = mascot?.name ?: p.selectedMascot,
                    soundEnabled = soundEnabled ?: p.soundEnabled,
                    zenFlowMode = zenFlowMode ?: p.zenFlowMode,
                    reminderEnabled = reminderEnabled ?: p.reminderEnabled,
                    reminderHour = reminderHour?.coerceIn(0, 23) ?: p.reminderHour,
                    reminderMinute = reminderMinute?.coerceIn(0, 59) ?: p.reminderMinute,
                    displayName = displayName?.trim()?.take(50)?.takeIf { it.isNotBlank() } ?: p.displayName
                )
            }
            return
        }
        viewModelScope.launch {
            val res = repository!!.updatePreferences(
                userId = currentUserId,
                theme = theme,
                mascot = mascot,
                soundEnabled = soundEnabled,
                zenFlowMode = zenFlowMode,
                reminderEnabled = reminderEnabled,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute,
                displayName = displayName
            )
            res.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Errore nel salvataggio preferenze."
            }
        }
    }

    fun launchGame(
        gameId: String,
        level: Int,
        zenMode: Boolean,
        customSeed: Int? = null,
        duelChallenge: DuelChallenge? = null,
        dailyCircuitStage: Int? = null,
        source: GameLaunchSource = GameLaunchSource.GYM_CATALOG
    ) {
        val game = GAMES_CATALOG.firstOrNull { it.id == gameId } ?: GAMES_CATALOG.first()
        val effectiveZen = zenMode && game.supportsZenMode && duelChallenge == null
        val safeLevel = level.coerceIn(1, 100)
        val seed = customSeed ?: com.example.engine.Mulberry32.seedFor(gameId, safeLevel)
        _lastResult.value = null
        _activeGame.value = ActiveGameLaunch(
            gameId = gameId,
            level = safeLevel,
            seed = seed,
            zenMode = effectiveZen,
            duelChallenge = duelChallenge,
            dailyCircuitStage = dailyCircuitStage
        )
        analytics.logGameStart(
            gameId = gameId,
            pillar = game.pillar.name,
            level = safeLevel,
            zenMode = effectiveZen,
            source = source,
            isDuel = duelChallenge != null
        )
    }

    fun launchDailyCircuit(profile: UserProfile, todayStat: DailyStat?) {
        val nextStage = when {
            todayStat?.logicCompleted != true -> 0
            todayStat.memoryCompleted != true -> 1
            todayStat.reflexCompleted != true -> 2
            todayStat.empathyCompleted != true -> 3
            else -> 0
        }
        val gameId = dailyCircuitGameIds[nextStage]
        val level = profile.unlockedLevelFor(gameId)
        analytics.logDailyCircuitStarted(stage = nextStage, gameId = gameId)
        launchGame(
            gameId = gameId,
            level = level,
            zenMode = profile.zenFlowMode,
            dailyCircuitStage = nextStage,
            source = GameLaunchSource.DAILY_CIRCUIT
        )
    }

    fun exitActiveGame() {
        _activeGame.value = null
        _lastResult.value = null
    }

    fun finishActiveGame(result: SessionResult) {
        val currentProfile = (profileState.value as? UiState.Success)?.data ?: return
        val dailyList = (dailyStatsState.value as? UiState.Success)?.data.orEmpty()
        val todayStat = dailyList.firstOrNull { it.dateKey == todayDateKey }
        val activeStage = _activeGame.value?.dailyCircuitStage

        if (useLocalOnly) {
            val unlocked = recordProvisionalSession(currentProfile, dailyList, todayStat, result)
            logFinishedGameAnalytics(result, unlocked, activeStage)
            return
        }

        viewModelScope.launch {
            val saved = repository!!.recordSessionAndProgress(
                userId = currentProfile.userId.ifBlank { currentUserId },
                currentProfile = currentProfile,
                todayStat = todayStat,
                result = result
            )
            val unlocked = saved.getOrDefault(false)
            _lastResult.value = result.copy(unlockedNextLevel = unlocked)
            logFinishedGameAnalytics(result, unlocked, activeStage)
            if (saved.isFailure) {
                _snackbarMessage.value = "Sessione salvata in cache offline; sincronizzazione al prossimo accesso."
            }
        }
    }

    private fun logFinishedGameAnalytics(
        result: SessionResult,
        unlockedNextLevel: Boolean,
        dailyCircuitStage: Int?
    ) {
        analytics.logGameEnd(
            gameId = result.gameId,
            pillar = result.pillar.name,
            level = result.level,
            score = result.score,
            accuracyPercent = result.accuracyPercent,
            avgReactionMs = result.avgReactionMs,
            durationSec = result.durationSec,
            zenMode = result.zenModeUsed,
            unlockedNextLevel = unlockedNextLevel,
            isDuel = result.duelSeed != null
        )
        if (dailyCircuitStage == 3) {
            analytics.logDailyCircuitCompleted()
        }
    }

    private fun recordProvisionalSession(
        currentProfile: UserProfile,
        dailyList: List<DailyStat>,
        todayStat: DailyStat?,
        result: SessionResult
    ): Boolean {
        val session = TrainingSession(
            id = UUID.randomUUID().toString(),
            userId = currentUserId,
            gameId = result.gameId,
            pillar = result.pillar.name,
            level = result.level.coerceIn(1, 100),
            score = result.score.coerceIn(0, 10000),
            accuracyPercent = result.accuracyPercent.coerceIn(0, 100),
            avgReactionMs = result.avgReactionMs.coerceIn(0, 60000),
            durationSec = result.durationSec.coerceIn(0, 3600),
            zenModeUsed = result.zenModeUsed,
            duelSeed = result.duelSeed,
            dateKey = todayDateKey,
            createdAt = Timestamp.now()
        )
        val existingSessions = (localSessions.value as? UiState.Success)?.data.orEmpty()
        localSessions.value = UiState.Success((listOf(session) + existingSessions).take(40))

        val prevCount = todayStat?.sessionsCount ?: 0
        val newCount = (prevCount + 1).coerceAtMost(500)
        val newTotalScore = ((todayStat?.totalScore ?: 0) + result.score).coerceIn(0, 1000000)
        val newAvgAcc = if (prevCount == 0) {
            result.accuracyPercent.coerceIn(0, 100)
        } else {
            (((todayStat?.avgAccuracy ?: 0) * prevCount + result.accuracyPercent) / newCount).coerceIn(0, 100)
        }
        val updatedToday = DailyStat(
            id = todayDateKey,
            userId = currentUserId,
            dateKey = todayDateKey,
            sessionsCount = newCount,
            totalScore = newTotalScore,
            logicCompleted = (todayStat?.logicCompleted == true) || result.pillar == CognitivePillar.LOGIC,
            memoryCompleted = (todayStat?.memoryCompleted == true) || result.pillar == CognitivePillar.MEMORY,
            reflexCompleted = (todayStat?.reflexCompleted == true) || result.pillar == CognitivePillar.REFLEX,
            empathyCompleted = (todayStat?.empathyCompleted == true) || result.pillar == CognitivePillar.EMPATHY,
            avgAccuracy = newAvgAcc,
            createdAt = todayStat?.createdAt ?: Timestamp.now(),
            updatedAt = Timestamp.now()
        )
        val others = dailyList.filterNot { it.dateKey == todayDateKey }
        localDailyStats.value = UiState.Success(listOf(updatedToday) + others)

        val currentUnlocked = currentProfile.unlockedLevelFor(result.gameId)
        val unlockedNext = result.accuracyPercent >= 70 && result.level >= currentUnlocked && currentUnlocked < 100
        val updatedLevels = currentProfile.unlockedLevels.toMutableMap()
        if (unlockedNext) {
            updatedLevels[result.gameId] = (currentUnlocked + 1).toLong()
        }

        val performanceRating = ((result.accuracyPercent * 0.72) + ((result.score.coerceAtMost(1000) / 10.0) * 0.28))
            .toInt()
            .coerceIn(10, 100)
        fun blend(old: Int, incoming: Int): Int = ((old * 0.65) + (incoming * 0.35)).toInt().coerceIn(5, 100)

        val newLogic = if (result.pillar == CognitivePillar.LOGIC) blend(currentProfile.logicScore, performanceRating) else currentProfile.logicScore
        val newMemory = if (result.pillar == CognitivePillar.MEMORY) blend(currentProfile.memoryScore, performanceRating) else currentProfile.memoryScore
        val newReflex = if (result.pillar == CognitivePillar.REFLEX) blend(currentProfile.reflexScore, performanceRating) else currentProfile.reflexScore
        val newEmpathy = if (result.pillar == CognitivePillar.EMPATHY) blend(currentProfile.empathyScore, performanceRating) else currentProfile.empathyScore

        val reactionFactor = (1200 - result.avgReactionMs.coerceIn(250, 1150)) / 4
        val newSpeedIndex = (((newLogic + newMemory + newReflex + newEmpathy) / 4.0) * 7.5 + reactionFactor)
            .toInt()
            .coerceIn(100, 1000)

        val newStreak = if (currentProfile.lastWorkoutDate == todayDateKey) {
            currentProfile.currentStreak
        } else {
            maxOf(1, currentProfile.currentStreak + 1)
        }
        val newBestStreak = maxOf(currentProfile.bestStreak, newStreak)

        updateLocalProfile { p ->
            p.copy(
                currentStreak = newStreak,
                bestStreak = newBestStreak,
                lastWorkoutDate = todayDateKey,
                logicScore = newLogic,
                memoryScore = newMemory,
                reflexScore = newReflex,
                empathyScore = newEmpathy,
                speedIndex = newSpeedIndex,
                unlockedLevels = updatedLevels
            )
        }
        _lastResult.value = result.copy(unlockedNextLevel = unlockedNext)
        return unlockedNext
    }

    fun advanceDailyCircuitIfApplicable() {
        val active = _activeGame.value
        val currentProfile = (profileState.value as? UiState.Success)?.data
        if (active?.dailyCircuitStage != null && currentProfile != null && active.dailyCircuitStage < 3) {
            val nextStage = active.dailyCircuitStage + 1
            val nextGameId = dailyCircuitGameIds[nextStage]
            analytics.logDailyCircuitStageAdvanced(nextStage = nextStage, gameId = nextGameId)
            launchGame(
                gameId = nextGameId,
                level = currentProfile.unlockedLevelFor(nextGameId),
                zenMode = currentProfile.zenFlowMode,
                dailyCircuitStage = nextStage,
                source = GameLaunchSource.DAILY_CIRCUIT
            )
        } else {
            exitActiveGame()
        }
    }

    fun requestAiCoachVerdict(forceRefresh: Boolean = false) {
        val profile = (profileState.value as? UiState.Success)?.data ?: return
        val sessions = (sessionsState.value as? UiState.Success)?.data.orEmpty()
        analytics.logAiCoachRequested(forceRefresh = forceRefresh, sessionCount = sessions.size)
        viewModelScope.launch {
            _coachState.value = UiState.Loading
            val res = aiCoachService.generateCoachVerdict(profile, sessions, forceRefresh = forceRefresh)
            _coachState.value = res.fold(
                onSuccess = { verdict ->
                    analytics.logAiCoachVerdictShown(
                        recommendedGameId = verdict.recommendedGameId,
                        focusPillar = verdict.focusPillar
                    )
                    UiState.Success(verdict)
                },
                onFailure = { UiState.Error(it.localizedMessage ?: "Coach AI non disponibile.") }
            )
        }
    }

    fun exportUserDataJson(onExportReady: (String) -> Unit) {
        if (useLocalOnly) {
            val profile = (localProfile.value as? UiState.Success)?.data
            val sessions = (localSessions.value as? UiState.Success)?.data.orEmpty()
            val root = JSONObject().apply {
                put("app", "AllenaMente")
                put("mode", "modalita_provvisoria")
                put("exportedAt", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()))
                put("displayName", profile?.displayName ?: "Tester Provvisorio")
                put("speedIndex", profile?.speedIndex ?: 500)
                put("sessionsCount", sessions.size)
                put(
                    "sessions",
                    JSONArray().apply {
                        sessions.forEach { s ->
                            put(
                                JSONObject().apply {
                                    put("gameId", s.gameId)
                                    put("pillar", s.pillar)
                                    put("level", s.level)
                                    put("score", s.score)
                                    put("accuracyPercent", s.accuracyPercent)
                                }
                            )
                        }
                    }
                )
            }
            onExportReady(root.toString(2))
            return
        }
        viewModelScope.launch {
            val res = repository!!.exportAllDataJson(currentUserId)
            res.onSuccess { json ->
                onExportReady(json)
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Errore durante l'esportazione GDPR."
            }
        }
    }

    fun deleteAllUserData(onDeleted: () -> Unit) {
        if (useLocalOnly) {
            analytics.logGdprDataDeleted("provisional")
            localSessions.value = UiState.Success(emptyList())
            localDailyStats.value = UiState.Success(emptyList())
            _snackbarMessage.value = "Sessione provvisoria azzerata."
            onDeleted()
            return
        }
        viewModelScope.launch {
            val res = repository!!.deleteAllUserData(currentUserId)
            res.onSuccess {
                analytics.logGdprDataDeleted("google")
                _snackbarMessage.value = "Tutti i dati cloud e locali sono stati eliminati."
                onDeleted()
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Errore durante l'eliminazione dati."
            }
        }
    }
}

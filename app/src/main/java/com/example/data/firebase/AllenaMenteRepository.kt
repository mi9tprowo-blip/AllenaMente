package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.model.AGE_COHORTS
import com.example.data.model.AppTheme
import com.example.data.model.CognitivePillar
import com.example.data.model.DailyStat
import com.example.data.model.GAMES_CATALOG
import com.example.data.model.MascotType
import com.example.data.model.SessionResult
import com.example.data.model.TrainingSession
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class AllenaMenteRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        val uid = auth.currentUser?.uid
        check(!uid.isNullOrBlank()) { "Utente non autenticato con Google." }
        return uid
    }

    fun todayDateKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun currentMonthKey(): String =
        SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        require(userId.isNotBlank()) { "userId must not be blank" }
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (!snapshot.exists()) null
                    else snapshot.toObject(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(ex, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeRecentSessions(userId: String, limitCount: Long = 40L): Flow<List<TrainingSession>> = flow {
        require(userId.isNotBlank()) { "userId must not be blank" }
        val path = "users/$userId/sessions"
        emitAll(
            db.collection("users").document(userId).collection("sessions")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limitCount)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        TrainingSession::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(ex, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeDailyStats(userId: String, limitDays: Long = 30L): Flow<List<DailyStat>> = flow {
        require(userId.isNotBlank()) { "userId must not be blank" }
        val path = "users/$userId/dailyStats"
        emitAll(
            db.collection("users").document(userId).collection("dailyStats")
                .orderBy("dateKey", Query.Direction.DESCENDING)
                .limit(limitDays)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        DailyStat::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(ex, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureProfileExists(userId: String, googleDisplayName: String?): Result<UserProfile> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "UID mismatch" }
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            if (snap.exists()) {
                val existing = snap.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (existing != null) {
                    // Monthly Streak Freeze refill & midnight gap evaluation
                    val refilled = evaluateStreakFreezeRefill(uid, existing)
                    return Result.success(refilled)
                }
            }

            val cleanName = (googleDisplayName?.trim()?.take(50)).takeIf { !it.isNullOrBlank() } ?: "Esploratore"
            val monthKey = currentMonthKey()
            val initialLevels = GAMES_CATALOG.associate { it.id to 1L }
            val createPayload = mapOf(
                "userId" to uid,
                "displayName" to cleanName,
                "ageCohort" to "25-34",
                "termsAccepted" to false,
                "calibrationCompleted" to false,
                "selectedTheme" to AppTheme.ZEN_PEBBLE.name,
                "selectedMascot" to MascotType.OWL.name,
                "soundEnabled" to true,
                "zenFlowMode" to false,
                "reminderEnabled" to false,
                "reminderHour" to 9,
                "reminderMinute" to 0,
                "currentStreak" to 0,
                "bestStreak" to 0,
                "streakFreezesAvailable" to 1,
                "lastFreezeRefillMonth" to monthKey,
                "logicScore" to 50,
                "memoryScore" to 50,
                "reflexScore" to 50,
                "empathyScore" to 50,
                "speedIndex" to 500,
                "unlockedLevels" to initialLevels,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(createPayload).await()
            val created = UserProfile(
                id = uid,
                userId = uid,
                displayName = cleanName,
                lastFreezeRefillMonth = monthKey,
                unlockedLevels = initialLevels
            )
            Result.success(created)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    private suspend fun evaluateStreakFreezeRefill(uid: String, profile: UserProfile): UserProfile {
        val monthNow = currentMonthKey()
        val today = todayDateKey()
        var freezes = profile.streakFreezesAvailable
        var streak = profile.currentStreak
        var freezeUsedDate = profile.lastFreezeUsedDate
        var refillMonth = profile.lastFreezeRefillMonth
        var changed = false

        if (refillMonth != monthNow) {
            freezes = maxOf(freezes, 1).coerceAtMost(10)
            refillMonth = monthNow
            changed = true
        }

        val lastDate = profile.lastWorkoutDate
        if (!lastDate.isNullOrBlank() && streak > 0) {
            val gapDays = daysBetween(lastDate, today)
            if (gapDays == 2 && freezes > 0 && freezeUsedDate != today) {
                // Midnight streak freeze protection consumes 1 crystal and preserves streak
                freezes -= 1
                freezeUsedDate = today
                changed = true
            } else if (gapDays >= 3) {
                streak = 0
                changed = true
            }
        }

        if (!changed) return profile

        val updates = mapOf<String, Any?>(
            "currentStreak" to streak,
            "streakFreezesAvailable" to freezes,
            "lastFreezeUsedDate" to freezeUsedDate,
            "lastFreezeRefillMonth" to refillMonth,
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        db.collection("users").document(uid).update(updates).await()
        return profile.copy(
            currentStreak = streak,
            streakFreezesAvailable = freezes,
            lastFreezeUsedDate = freezeUsedDate,
            lastFreezeRefillMonth = refillMonth
        )
    }

    suspend fun acceptOnboarding(userId: String, displayName: String, ageCohort: String): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val safeName = displayName.trim().take(50).ifBlank { "Esploratore" }
            val safeCohort = if (ageCohort in AGE_COHORTS) ageCohort else "25-34"
            db.collection("users").document(uid).update(
                mapOf(
                    "displayName" to safeName,
                    "ageCohort" to safeCohort,
                    "termsAccepted" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun completeCalibration(
        userId: String,
        logicScore: Int?,
        memoryScore: Int?,
        reflexScore: Int?,
        empathyScore: Int?,
        speedIndex: Int?
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val payload = mutableMapOf<String, Any>(
                "calibrationCompleted" to true,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            logicScore?.let { payload["logicScore"] = it.coerceIn(0, 100) }
            memoryScore?.let { payload["memoryScore"] = it.coerceIn(0, 100) }
            reflexScore?.let { payload["reflexScore"] = it.coerceIn(0, 100) }
            empathyScore?.let { payload["empathyScore"] = it.coerceIn(0, 100) }
            speedIndex?.let { payload["speedIndex"] = it.coerceIn(0, 1000) }

            db.collection("users").document(uid).update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updatePreferences(
        userId: String,
        theme: AppTheme? = null,
        mascot: MascotType? = null,
        soundEnabled: Boolean? = null,
        zenFlowMode: Boolean? = null,
        reminderEnabled: Boolean? = null,
        reminderHour: Int? = null,
        reminderMinute: Int? = null,
        displayName: String? = null
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val updates = mutableMapOf<String, Any>(
                "updatedAt" to FieldValue.serverTimestamp()
            )
            theme?.let { updates["selectedTheme"] = it.name }
            mascot?.let { updates["selectedMascot"] = it.name }
            soundEnabled?.let { updates["soundEnabled"] = it }
            zenFlowMode?.let { updates["zenFlowMode"] = it }
            reminderEnabled?.let { updates["reminderEnabled"] = it }
            reminderHour?.let { updates["reminderHour"] = it.coerceIn(0, 23) }
            reminderMinute?.let { updates["reminderMinute"] = it.coerceIn(0, 59) }
            displayName?.trim()?.take(50)?.takeIf { it.isNotBlank() }?.let {
                updates["displayName"] = it
            }

            db.collection("users").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun recordSessionAndProgress(
        userId: String,
        currentProfile: UserProfile,
        todayStat: DailyStat?,
        result: SessionResult
    ): Result<Boolean> {
        val dateKey = todayDateKey()
        val sessionPath = "users/$userId/sessions"
        return try {
            val uid = requireUserId()
            check(uid == userId)

            // 1. Write append-only TrainingSession (no "id" key, omit null duelSeed)
            val sessionRef = db.collection("users").document(uid).collection("sessions").document()
            val sessionPayload = mapOf<String, Any?>(
                "userId" to uid,
                "gameId" to result.gameId.take(40),
                "pillar" to result.pillar.name,
                "level" to result.level.coerceIn(1, 100),
                "score" to result.score.coerceIn(0, 10000),
                "accuracyPercent" to result.accuracyPercent.coerceIn(0, 100),
                "avgReactionMs" to result.avgReactionMs.coerceIn(0, 60000),
                "durationSec" to result.durationSec.coerceIn(0, 3600),
                "zenModeUsed" to result.zenModeUsed,
                "duelSeed" to result.duelSeed?.coerceAtLeast(0),
                "dateKey" to dateKey,
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            sessionRef.set(sessionPayload).await()

            // 2. Upsert normalized DailyStat for dateKey
            val dailyRef = db.collection("users").document(uid).collection("dailyStats").document(dateKey)
            val existingDailySnap = dailyRef.get().await()
            val resolvedDaily = if (existingDailySnap.exists()) {
                existingDailySnap.toObject(
                    DailyStat::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: todayStat
            } else {
                null
            }
            val prevCount = resolvedDaily?.sessionsCount ?: 0
            val newCount = (prevCount + 1).coerceAtMost(500)
            val newTotalScore = ((resolvedDaily?.totalScore ?: 0) + result.score).coerceIn(0, 1000000)
            val newAvgAcc = if (prevCount == 0) {
                result.accuracyPercent.coerceIn(0, 100)
            } else {
                (((resolvedDaily?.avgAccuracy ?: 0) * prevCount + result.accuracyPercent) / newCount).coerceIn(0, 100)
            }
            val logicDone = (resolvedDaily?.logicCompleted == true) || result.pillar == CognitivePillar.LOGIC
            val memoryDone = (resolvedDaily?.memoryCompleted == true) || result.pillar == CognitivePillar.MEMORY
            val reflexDone = (resolvedDaily?.reflexCompleted == true) || result.pillar == CognitivePillar.REFLEX
            val empathyDone = (resolvedDaily?.empathyCompleted == true) || result.pillar == CognitivePillar.EMPATHY

            if (!existingDailySnap.exists()) {
                dailyRef.set(
                    mapOf(
                        "userId" to uid,
                        "dateKey" to dateKey,
                        "sessionsCount" to newCount,
                        "totalScore" to newTotalScore,
                        "logicCompleted" to logicDone,
                        "memoryCompleted" to memoryDone,
                        "reflexCompleted" to reflexDone,
                        "empathyCompleted" to empathyDone,
                        "avgAccuracy" to newAvgAcc,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                dailyRef.update(
                    mapOf(
                        "sessionsCount" to newCount,
                        "totalScore" to newTotalScore,
                        "logicCompleted" to logicDone,
                        "memoryCompleted" to memoryDone,
                        "reflexCompleted" to reflexDone,
                        "empathyCompleted" to empathyDone,
                        "avgAccuracy" to newAvgAcc,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            // 3. Update UserProfile streak, DDA level unlock, 4-pillar rating & speed index
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

            var newStreak = currentProfile.currentStreak
            var newFreezes = currentProfile.streakFreezesAvailable
            var freezeUsedDate = currentProfile.lastFreezeUsedDate
            val lastWorkout = currentProfile.lastWorkoutDate

            if (lastWorkout != dateKey) {
                val gap = if (lastWorkout.isNullOrBlank()) 99 else daysBetween(lastWorkout, dateKey)
                newStreak = when {
                    lastWorkout.isNullOrBlank() -> 1
                    gap <= 1 -> newStreak + 1
                    gap == 2 && (newFreezes > 0 || freezeUsedDate == dateKey) -> {
                        if (freezeUsedDate != dateKey && newFreezes > 0) {
                            newFreezes -= 1
                            freezeUsedDate = dateKey
                        }
                        newStreak + 1
                    }
                    else -> 1
                }
            }
            val newBestStreak = maxOf(currentProfile.bestStreak, newStreak)

            val profileUpdates = mapOf<String, Any?>(
                "currentStreak" to newStreak,
                "bestStreak" to newBestStreak,
                "streakFreezesAvailable" to newFreezes,
                "lastWorkoutDate" to dateKey,
                "lastFreezeUsedDate" to freezeUsedDate,
                "logicScore" to newLogic,
                "memoryScore" to newMemory,
                "reflexScore" to newReflex,
                "empathyScore" to newEmpathy,
                "speedIndex" to newSpeedIndex,
                "unlockedLevels" to updatedLevels,
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            db.collection("users").document(uid).update(profileUpdates).await()
            Result.success(unlockedNext)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, sessionPath)
            Result.failure(e)
        }
    }

    suspend fun exportAllDataJson(userId: String): Result<String> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val profileSnap = db.collection("users").document(uid).get().await()
            val sessionsSnap = db.collection("users").document(uid).collection("sessions")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(200)
                .get()
                .await()
            val dailySnap = db.collection("users").document(uid).collection("dailyStats")
                .orderBy("dateKey", Query.Direction.DESCENDING)
                .limit(90)
                .get()
                .await()

            val root = JSONObject().apply {
                put("app", "AllenaMente")
                put("exportedAt", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()))
                put("userId", uid)
                put("profile", JSONObject(profileSnap.data ?: emptyMap<String, Any>()))
                put(
                    "sessions",
                    JSONArray().apply {
                        sessionsSnap.documents.forEach { doc ->
                            put(JSONObject(doc.data ?: emptyMap<String, Any>()))
                        }
                    }
                )
                put(
                    "dailyStats",
                    JSONArray().apply {
                        dailySnap.documents.forEach { doc ->
                            put(JSONObject(doc.data ?: emptyMap<String, Any>()))
                        }
                    }
                )
            }
            Result.success(root.toString(2))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAllUserData(userId: String): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId)

            val sessions = db.collection("users").document(uid).collection("sessions").get().await()
            for (doc in sessions.documents) {
                doc.reference.delete().await()
            }
            val daily = db.collection("users").document(uid).collection("dailyStats").get().await()
            for (doc in daily.documents) {
                doc.reference.delete().await()
            }
            db.collection("users").document(uid).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    private fun daysBetween(startDateKey: String, endDateKey: String): Int {
        return try {
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val d1 = fmt.parse(startDateKey) ?: return 99
            val d2 = fmt.parse(endDateKey) ?: return 99
            val c1 = Calendar.getInstance().apply { time = d1; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
            val c2 = Calendar.getInstance().apply { time = d2; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
            TimeUnit.MILLISECONDS.toDays(c2.timeInMillis - c1.timeInMillis).toInt()
        } catch (_: Exception) {
            99
        }
    }
}

package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class CognitivePillar(
    val titleIt: String,
    val shortIt: String,
    val brainRegion: String
) {
    LOGIC("Logica", "Logica", "DLPFC"),
    MEMORY("Memoria", "Memoria", "Ippocampo"),
    REFLEX("Riflessi", "Riflessi", "Corteccia Motoria"),
    EMPATHY("Empatia", "Empatia", "Giunzione TPJ");

    companion object {
        fun fromRaw(raw: String?): CognitivePillar =
            entries.firstOrNull { it.name == raw } ?: LOGIC
    }
}

enum class AppTheme(val labelIt: String, val subtitleIt: String) {
    ZEN_PEBBLE("Zen Garden Pebble", "Pietra calda e salvia riposante"),
    WARM_GLASS("Warm Liquid Glass", "Luce morbida ambra e terracotta"),
    NEO_BRUTALIST("Neo-Brutalist Raw", "Alto contrasto e bordi netti"),
    OBSIDIAN_DARK("Obsidian Dark", "Notturno + audio Night Calm (-3dB)");

    companion object {
        fun fromRaw(raw: String?): AppTheme =
            entries.firstOrNull { it.name == raw } ?: ZEN_PEBBLE
    }
}

enum class MascotType(
    val labelIt: String,
    val soundLabelIt: String,
    val mottoIt: String
) {
    PANDA("Panda Zen", "Campana pentatonica", "Calma e respiro"),
    FOX("Volpe Rapida", "Arpeggio brillante", "Scatto e intuito"),
    OWL("Gufo Saggio", "Accordo cristallino", "Visione chiara"),
    OTTER("Lontra Agile", "Gocce armoniche", "Flusso continuo"),
    CAT("Gatto Curioso", "Rintocco morbido", "Focus elegante"),
    DEER("Cervo Silvano", "Quinta calda", "Equilibrio puro"),
    WOLF("Lupo Stratega", "Risonanza profonda", "Costanza ferma");

    companion object {
        fun fromRaw(raw: String?): MascotType =
            entries.firstOrNull { it.name == raw } ?: OWL
    }
}

val AGE_COHORTS = listOf("14-24", "25-34", "35-44", "45-54", "55-64", "65+")

data class GameDefinition(
    val id: String,
    val titleIt: String,
    val pillar: CognitivePillar,
    val shortDescIt: String,
    val microTutorialIt: String,
    val neuroTag: String,
    val bonusPremium: Boolean = false,
    val supportsZenMode: Boolean = false
)

val GAMES_CATALOG: List<GameDefinition> = listOf(
    // --- LOGIC (4 Core) ---
    GameDefinition(
        id = "raven-matrix",
        titleIt = "Matrici Astratte",
        pillar = CognitivePillar.LOGIC,
        shortDescIt = "Completa lo schema visuo-geometrico 3×3",
        microTutorialIt = "Osserva la regola per righe e colonne e scegli il tassello mancante.",
        neuroTag = "Ragionamento Fluido • DLPFC",
        supportsZenMode = true
    ),
    GameDefinition(
        id = "abstract-sequences",
        titleIt = "Sequenze Nascoste",
        pillar = CognitivePillar.LOGIC,
        shortDescIt = "Scopri la regola della serie simbolica",
        microTutorialIt = "Individua la progressione logica e seleziona l'elemento successivo.",
        neuroTag = "Induzione Regole • Frontale",
        supportsZenMode = true
    ),
    GameDefinition(
        id = "pattern-folding",
        titleIt = "Piegamento 3D",
        pillar = CognitivePillar.LOGIC,
        shortDescIt = "Rotazione mentale e facce opposte del cubo",
        microTutorialIt = "Immagina di chiudere la rete piana in un cubo e indica la faccia opposta.",
        neuroTag = "Rotazione Spaziale • Parietale",
        supportsZenMode = true
    ),
    GameDefinition(
        id = "subitizing",
        titleIt = "Colpo d'Occhio",
        pillar = CognitivePillar.LOGIC,
        shortDescIt = "Stima istantanea della numerosità",
        microTutorialIt = "Cogli a colpo d'occhio quanti elementi appaiono prima che svaniscano.",
        neuroTag = "Senso Numerico • Solco Intraparietale",
        supportsZenMode = false
    ),
    // --- MEMORY (4 Core) ---
    GameDefinition(
        id = "light-matrix",
        titleIt = "Costellazione di Luce",
        pillar = CognitivePillar.MEMORY,
        shortDescIt = "Memoria visuo-spaziale su griglie luminose",
        microTutorialIt = "Memorizza le celle che si illuminano e ricreale nello stesso schema.",
        neuroTag = "Memoria Spaziale • Ippocampo",
        supportsZenMode = true
    ),
    GameDefinition(
        id = "n-back",
        titleIt = "Doppia Traccia",
        pillar = CognitivePillar.MEMORY,
        shortDescIt = "Aggiornamento continuo N-Back",
        microTutorialIt = "Tocca UGUALE se il simbolo coincide con quello visto N passi fa.",
        neuroTag = "Memoria di Lavoro • Prefrontale",
        supportsZenMode = false
    ),
    GameDefinition(
        id = "digit-span",
        titleIt = "Eco di Numeri",
        pillar = CognitivePillar.MEMORY,
        shortDescIt = "Ritenzione sequenziale diretta e inversa",
        microTutorialIt = "Ricorda la sequenza di cifre e inseriscila nell'ordine richiesto.",
        neuroTag = "Loop Fonologico • Temporale",
        supportsZenMode = true
    ),
    GameDefinition(
        id = "hippocampus-maze",
        titleIt = "Labirinto Mentale",
        pillar = CognitivePillar.MEMORY,
        shortDescIt = "Mappa mentale e navigazione allocentrica",
        microTutorialIt = "Osserva il percorso dal via alla meta, poi ripercorrilo a memoria.",
        neuroTag = "Mappa Allocentrica • Ippocampo",
        supportsZenMode = true
    ),
    // --- REFLEX (4 Core) ---
    GameDefinition(
        id = "impulse-inhibition",
        titleIt = "Freno Rapido",
        pillar = CognitivePillar.REFLEX,
        shortDescIt = "Controllo inibitorio Go / No-Go",
        microTutorialIt = "Tocca subito sugli stimoli VERDI (GO), frena sui ROSSI (STOP).",
        neuroTag = "Inibizione • Corteccia Cingolata",
        supportsZenMode = false
    ),
    GameDefinition(
        id = "dynamic-stroop",
        titleIt = "Filtro Colore",
        pillar = CognitivePillar.REFLEX,
        shortDescIt = "Supera l'interferenza parola-colore",
        microTutorialIt = "Scegli il COLORE DELL'INCHIOSTRO, ignorando il testo scritto.",
        neuroTag = "Controllo Conflitto • ACC",
        supportsZenMode = false
    ),
    GameDefinition(
        id = "multi-tracking",
        titleIt = "Tracciamento Multiplo",
        pillar = CognitivePillar.REFLEX,
        shortDescIt = "Segui più nodi in movimento (MOT)",
        microTutorialIt = "Segui con lo sguardo le sfere evidenziate finché si fermano, poi toccale.",
        neuroTag = "Attenzione Divisa • Parietale",
        supportsZenMode = false
    ),
    GameDefinition(
        id = "trail-making",
        titleIt = "Sentiero Fluido",
        pillar = CognitivePillar.REFLEX,
        shortDescIt = "Alternanza rapida numeri e lettere",
        microTutorialIt = "Collega in ordine alternato: 1 → A → 2 → B → 3 → C il più fluido possibile.",
        neuroTag = "Flessibilità Attentiva • Frontale",
        supportsZenMode = true
    ),
    // --- EMPATHY / MIRROR (4 Core) ---
    GameDefinition(
        id = "kinetic-mirror",
        titleIt = "Specchio Visuo-Motorio",
        pillar = CognitivePillar.EMPATHY,
        shortDescIt = "Coordinazione speculare orizzontale",
        microTutorialIt = "Replica la cella esatta RIFLESSA allo specchio rispetto all'asse centrale.",
        neuroTag = "Sistema Specchio • Premotoria",
        supportsZenMode = true
    ),
    GameDefinition(
        id = "emotional-resonance",
        titleIt = "Sintonia Emotiva",
        pillar = CognitivePillar.EMPATHY,
        shortDescIt = "Micro-espressioni complete + indizi guidati",
        microTutorialIt = "Osserva occhi, sopracciglia e bocca e riconosci l'emozione. Dopo ogni risposta vedrai i 2 indizi chiave.",
        neuroTag = "Riconoscimento Affettivo • Insula",
        supportsZenMode = true
    ),
    GameDefinition(
        id = "rhythm-mirror",
        titleIt = "Sincronia Ritmica",
        pillar = CognitivePillar.EMPATHY,
        shortDescIt = "Ascolta e riproduci il ritmo neurale",
        microTutorialIt = "Osserva il battito luminoso e ripeti la cadenza con lo stesso ritmo.",
        neuroTag = "Sincronia Temporale • Cervelletto",
        supportsZenMode = false
    ),
    GameDefinition(
        id = "mind-eyes",
        titleIt = "Oltre lo Sguardo",
        pillar = CognitivePillar.EMPATHY,
        shortDescIt = "Stati mentali dalla regione oculare (RMET)",
        microTutorialIt = "Leggi tensione palpebrale, pupille e sopracciglia con feedback didattico immediato.",
        neuroTag = "Teoria della Mente • STS",
        supportsZenMode = true
    ),
    // --- EXTRA BONUS • ANTEPRIMA PREMIUM (2 Bonus) ---
    GameDefinition(
        id = "neural-circuit",
        titleIt = "Circuito Sinaptico",
        pillar = CognitivePillar.LOGIC,
        shortDescIt = "Ruota i nodi 90° e instrada l'impulso BFS",
        microTutorialIt = "Tocca le celle per ruotare i conduttori di 90° e accendere tutti i recettori dalla sorgente.",
        neuroTag = "Pianificazione Esecutiva • DLPFC",
        bonusPremium = true,
        supportsZenMode = true
    ),
    GameDefinition(
        id = "perspective-compass",
        titleIt = "Bussola Empatica",
        pillar = CognitivePillar.EMPATHY,
        shortDescIt = "Director Task con pannelli opachi",
        microTutorialIt = "Mettiti nei panni del Direttore orientato (N/E/S/O): ignora le celle coperte dallo schermo opaco.",
        neuroTag = "Perspective-Taking • Giunzione TPJ",
        bonusPremium = true,
        supportsZenMode = true
    )
)

fun gameById(id: String): GameDefinition =
    GAMES_CATALOG.firstOrNull { it.id == id } ?: GAMES_CATALOG.first()

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Esploratore",
    val ageCohort: String = "25-34",
    val termsAccepted: Boolean = false,
    val calibrationCompleted: Boolean = false,
    val selectedTheme: String = AppTheme.ZEN_PEBBLE.name,
    val selectedMascot: String = MascotType.OWL.name,
    val soundEnabled: Boolean = true,
    val zenFlowMode: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val streakFreezesAvailable: Int = 1,
    val lastWorkoutDate: String? = null,
    val lastFreezeUsedDate: String? = null,
    val lastFreezeRefillMonth: String? = null,
    val logicScore: Int = 50,
    val memoryScore: Int = 50,
    val reflexScore: Int = 50,
    val empathyScore: Int = 50,
    val speedIndex: Int = 500,
    val unlockedLevels: Map<String, Long> = emptyMap(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun unlockedLevelFor(gameId: String): Int =
        (unlockedLevels[gameId]?.toInt() ?: 1).coerceIn(1, 100)

    fun themeEnum(): AppTheme = AppTheme.fromRaw(selectedTheme)
    fun mascotEnum(): MascotType = MascotType.fromRaw(selectedMascot)
}

@Keep
@IgnoreExtraProperties
data class TrainingSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val gameId: String = "",
    val pillar: String = CognitivePillar.LOGIC.name,
    val level: Int = 1,
    val score: Int = 0,
    val accuracyPercent: Int = 0,
    val avgReactionMs: Int = 0,
    val durationSec: Int = 0,
    val zenModeUsed: Boolean = false,
    val duelSeed: Int? = null,
    val dateKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DailyStat(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "",
    val sessionsCount: Int = 0,
    val totalScore: Int = 0,
    val logicCompleted: Boolean = false,
    val memoryCompleted: Boolean = false,
    val reflexCompleted: Boolean = false,
    val empathyCompleted: Boolean = false,
    val avgAccuracy: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val completedRingsCount: Int
        get() = listOf(logicCompleted, memoryCompleted, reflexCompleted, empathyCompleted).count { it }
}

data class DuelChallenge(
    val gameId: String,
    val level: Int,
    val seed: Int,
    val challengerName: String,
    val challengerScore: Int,
    val challengerReactionMs: Int,
    val challengerAccuracy: Int
)

data class SessionResult(
    val gameId: String,
    val pillar: CognitivePillar,
    val level: Int,
    val score: Int,
    val accuracyPercent: Int,
    val avgReactionMs: Int,
    val durationSec: Int,
    val zenModeUsed: Boolean,
    val duelSeed: Int? = null,
    val unlockedNextLevel: Boolean = false
)

data class CoachVerdict(
    val headline: String,
    val summary: String,
    val strongestPillar: String,
    val focusPillar: String,
    val recommendedGameId: String,
    val actionableTip: String
)

sealed interface UiState<out T> {
    data object Idle : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

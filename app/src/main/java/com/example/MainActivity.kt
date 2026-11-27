package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.firebase.AiCoachService
import com.example.data.firebase.AllenaMenteRepository
import com.example.data.firebase.attemptAutoSignIn
import com.example.data.firebase.configureAppCheck
import com.example.data.firebase.signOutFromGoogle
import com.example.data.model.AppTheme
import com.example.data.model.DuelChallenge
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import com.example.engine.CognitiveBalanceEngine
import com.example.engine.DuelLinkCodec
import com.example.engine.Mulberry32
import com.example.ui.components.AllenaMenteTutorialDialog
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.GameSessionScreen
import com.example.ui.screens.GymCatalogScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.InitialCalibrationDialog
import com.example.ui.screens.OnboardingTermsDialog
import com.example.ui.screens.PrivacyPolicyScreen
import com.example.ui.screens.ResultsDashboardScreen
import com.example.ui.screens.SettingsBottomSheet
import com.example.ui.theme.AllenaMenteTheme
import com.example.viewmodel.AllenaMenteViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private const val PROVISIONAL_SESSION_KEY = "provisional_local_session"

class MainActivity : ComponentActivity() {

    private var deepLinkDuelState by mutableStateOf<DuelChallenge?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1 (MANDATORY): Configure Firebase App Check with launch intent BEFORE UI / ViewModels
        configureAppCheck(this, intent)

        deepLinkDuelState = DuelLinkCodec.parseUri(intent?.data)
        enableEdgeToEdge()

        setContent {
            AppRoot(
                incomingDuel = deepLinkDuelState,
                onConsumeIncomingDuel = { deepLinkDuelState = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        DuelLinkCodec.parseUri(intent.data)?.let { duel ->
            deepLinkDuelState = duel
        }
    }
}

@Composable
private fun AppRoot(
    incomingDuel: DuelChallenge?,
    onConsumeIncomingDuel: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var isProvisionalMode by remember { mutableStateOf(false) }
    var checkedAutoSignIn by remember { mutableStateOf(currentUser != null) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
            if (auth.currentUser != null) {
                isProvisionalMode = false
            }
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(Unit) {
        if (currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    currentUser = Firebase.auth.currentUser
                    isProvisionalMode = false
                    checkedAutoSignIn = true
                },
                onUnauthenticated = {
                    checkedAutoSignIn = true
                },
                scope = scope
            )
        }
    }

    val user = currentUser
    var showAuthPrivacyPolicy by remember { mutableStateOf(false) }

    if (user == null && !isProvisionalMode) {
        AllenaMenteTheme(appTheme = AppTheme.ZEN_PEBBLE) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                when {
                    showAuthPrivacyPolicy -> {
                        PrivacyPolicyScreen(onBack = { showAuthPrivacyPolicy = false })
                    }
                    !checkedAutoSignIn -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    else -> {
                        AuthScreen(
                            onAuthSuccess = {
                                isProvisionalMode = false
                                currentUser = Firebase.auth.currentUser
                            },
                            onProvisionalAccess = {
                                isProvisionalMode = true
                            },
                            onOpenPrivacyPolicy = {
                                showAuthPrivacyPolicy = true
                            }
                        )
                    }
                }
            }
        }
    } else {
        val sessionKey = if (user != null) user.uid else PROVISIONAL_SESSION_KEY
        val provisionalActive = user == null && isProvisionalMode

        // Strict session-keyed composition & ViewModel scoping
        key(sessionKey) {
            val viewModel: AllenaMenteViewModel = viewModel(
                key = sessionKey,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        AllenaMenteViewModel(
                            repository = AllenaMenteRepository(db),
                            aiCoachService = AiCoachService(),
                            currentUserId = sessionKey,
                            googleDisplayName = user?.displayName ?: "Tester Provvisorio",
                            isProvisionalMode = provisionalActive
                        )
                    }
                }
            )

            LaunchedEffect(incomingDuel) {
                if (incomingDuel != null) {
                    viewModel.setIncomingDuel(incomingDuel)
                    onConsumeIncomingDuel()
                }
            }

            AuthenticatedAllenaMenteShell(
                viewModel = viewModel,
                userEmail = user?.email ?: "Modalità provvisoria (locale)",
                isProvisionalMode = provisionalActive,
                onSignOut = {
                    if (provisionalActive) {
                        isProvisionalMode = false
                    } else {
                        signOutFromGoogle(credentialManager, scope) {
                            currentUser = null
                            isProvisionalMode = false
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun AuthenticatedAllenaMenteShell(
    viewModel: AllenaMenteViewModel,
    userEmail: String?,
    isProvisionalMode: Boolean,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val profileUi by viewModel.profileState.collectAsStateWithLifecycle()
    val sessionsUi by viewModel.sessionsState.collectAsStateWithLifecycle()
    val dailyStatsUi by viewModel.dailyStatsState.collectAsStateWithLifecycle()
    val coachUi by viewModel.coachState.collectAsStateWithLifecycle()
    val activeGame by viewModel.activeGame.collectAsStateWithLifecycle()
    val lastResult by viewModel.lastResult.collectAsStateWithLifecycle()
    val pendingDuel by viewModel.pendingDuel.collectAsStateWithLifecycle()
    val snackbarMsg by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val showCalibration by viewModel.showCalibrationModal.collectAsStateWithLifecycle()

    val profile = (profileUi as? UiState.Success)?.data ?: UserProfile(userId = viewModel.currentUserId)
    val sessions = (sessionsUi as? UiState.Success)?.data.orEmpty()
    val dailyStats = (dailyStatsUi as? UiState.Success)?.data.orEmpty()
    val todayStat = remember(dailyStats, viewModel.todayDateKey) {
        dailyStats.firstOrNull { it.dateKey == viewModel.todayDateKey }
    }
    val balanceInsight = remember(profile, sessions, todayStat, viewModel.todayDateKey) {
        CognitiveBalanceEngine.evaluate(profile, sessions, todayStat, viewModel.todayDateKey)
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0=Home, 1=Esercizi, 2=Risultati
    var showSettings by remember { mutableStateOf(false) }
    var showTutorial by remember { mutableStateOf(false) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMsg) {
        if (!snackbarMsg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(snackbarMsg!!)
            viewModel.clearSnackbar()
        }
    }

    AllenaMenteTheme(appTheme = profile.themeEnum()) {
        if (showPrivacyPolicy) {
            PrivacyPolicyScreen(onBack = { showPrivacyPolicy = false })
            return@AllenaMenteTheme
        }

        val currentGame = activeGame
        if (currentGame != null) {
            GameSessionScreen(
                launch = currentGame,
                profile = profile,
                sessionResult = lastResult,
                onCompleteGame = { result -> viewModel.finishActiveGame(result) },
                onAdvanceCircuit = { viewModel.advanceDailyCircuitIfApplicable() },
                onReplayWithSeed = { newSeed ->
                    viewModel.launchGame(
                        gameId = currentGame.gameId,
                        level = currentGame.level,
                        zenMode = currentGame.zenMode,
                        customSeed = newSeed,
                        duelChallenge = currentGame.duelChallenge,
                        dailyCircuitStage = currentGame.dailyCircuitStage
                    )
                },
                onExit = { viewModel.exitActiveGame() }
            )
            return@AllenaMenteTheme
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (isProvisionalMode) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("provisional_mode_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LockOpen,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = stringResource(R.string.provisional_mode_banner),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            TextButton(
                                onClick = onSignOut,
                                modifier = Modifier.testTag("provisional_mode_exit_button")
                            ) {
                                Text(text = stringResource(R.string.provisional_mode_exit))
                            }
                        }
                    }
                }
            },
            bottomBar = {
                NavigationBar(windowInsets = WindowInsets.navigationBars) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(if (selectedTab == 0) Icons.Rounded.Home else Icons.Outlined.Home, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_home)) },
                        modifier = Modifier.testTag("tab_home")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(if (selectedTab == 1) Icons.Rounded.FitnessCenter else Icons.Outlined.FitnessCenter, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_exercises)) },
                        modifier = Modifier.testTag("tab_exercises")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(if (selectedTab == 2) Icons.Rounded.BarChart else Icons.Outlined.BarChart, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_results)) },
                        modifier = Modifier.testTag("tab_results")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (profileUi) {
                    is UiState.Loading, is UiState.Idle -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(12.dp))
                                Text("Sincronizzazione profilo AllenaMente…", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    is UiState.Error -> {
                        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = (profileUi as UiState.Error).message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    is UiState.Success -> {
                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = {
                                val direction = if (targetState > initialState) 1 else -1
                                (fadeIn(tween(220)) + slideInHorizontally(tween(240)) { it / 8 * direction })
                                    .togetherWith(fadeOut(tween(180)) + slideOutHorizontally(tween(200)) { -it / 8 * direction })
                            },
                            label = "main_tab_shared_axis"
                        ) { tab ->
                            when (tab) {
                                0 -> HomeDashboardScreen(
                                    profile = profile,
                                    todayStat = todayStat,
                                    todayDateKey = viewModel.todayDateKey,
                                    pendingDuel = pendingDuel,
                                    balanceInsight = balanceInsight,
                                    onStartDailyCircuit = { viewModel.launchDailyCircuit(profile, todayStat) },
                                    onLaunchGame = { gameId ->
                                        viewModel.launchGame(
                                            gameId = gameId,
                                            level = profile.unlockedLevelFor(gameId),
                                            zenMode = profile.zenFlowMode
                                        )
                                    },
                                    onAcceptDuel = { duel ->
                                        viewModel.setIncomingDuel(null)
                                        viewModel.launchGame(
                                            gameId = duel.gameId,
                                            level = duel.level,
                                            zenMode = false,
                                            customSeed = duel.seed,
                                            duelChallenge = duel
                                        )
                                    },
                                    onDismissDuel = { viewModel.setIncomingDuel(null) },
                                    onToggleZenMode = { enabled -> viewModel.updatePreferences(zenFlowMode = enabled) },
                                    onOpenCalibration = { viewModel.openCalibration() },
                                    onOpenTutorial = { showTutorial = true },
                                    onOpenSettings = { showSettings = true }
                                )

                                1 -> GymCatalogScreen(
                                    profile = profile,
                                    onLaunchGame = { gameId, level, zen ->
                                        viewModel.launchGame(gameId, level, zen)
                                    },
                                    onShareDuelChallenge = { gameId, level ->
                                        val seed = Mulberry32.seedFor(gameId, level, (System.currentTimeMillis() and 0xFFFFFF).toInt())
                                        val msg = DuelLinkCodec.buildShareMessage(
                                            gameId = gameId,
                                            level = level,
                                            seed = seed,
                                            challengerName = profile.displayName,
                                            score = 750,
                                            reactionMs = 680,
                                            accuracy = 85
                                        )
                                        val send = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, msg)
                                        }
                                        context.startActivity(Intent.createChooser(send, "Sfida 1v1 AllenaMente"))
                                    }
                                )

                                2 -> ResultsDashboardScreen(
                                    profile = profile,
                                    sessions = sessions,
                                    dailyStats = dailyStats,
                                    balanceIndex = balanceInsight.balanceIndex,
                                    coachState = coachUi,
                                    onRequestAiCoach = { force -> viewModel.requestAiCoachVerdict(forceRefresh = force) },
                                    onLaunchRecommendedGame = { recId ->
                                        viewModel.launchGame(
                                            gameId = recId,
                                            level = profile.unlockedLevelFor(recId),
                                            zenMode = profile.zenFlowMode
                                        )
                                    }
                                )
                            }
                        }

                        // First-launch Onboarding & Non-Clinical Terms Consent
                        val loadedProfile = (profileUi as UiState.Success).data
                        if (loadedProfile != null && !loadedProfile.termsAccepted) {
                            OnboardingTermsDialog(
                                initialName = loadedProfile.displayName,
                                onComplete = { name, cohort ->
                                    viewModel.acceptOnboarding(name, cohort)
                                }
                            )
                        } else if (showCalibration) {
                            InitialCalibrationDialog(
                                onSkipToHome = { viewModel.skipCalibration() },
                                onCompleteCalibration = { l, m, r, e, spd ->
                                    viewModel.saveCalibrationScores(l, m, r, e, spd)
                                }
                            )
                        }
                    }
                }
            }
        }

        if (showTutorial) {
            AllenaMenteTutorialDialog(onDismiss = { showTutorial = false })
        }

        if (showSettings) {
            SettingsBottomSheet(
                profile = profile,
                userEmail = userEmail,
                onDismiss = { showSettings = false },
                onSelectTheme = { theme -> viewModel.updatePreferences(theme = theme) },
                onSelectMascot = { mascot -> viewModel.updatePreferences(mascot = mascot) },
                onToggleSound = { enabled -> viewModel.updatePreferences(soundEnabled = enabled) },
                onToggleZenMode = { enabled -> viewModel.updatePreferences(zenFlowMode = enabled) },
                onToggleReminder = { enabled -> viewModel.updatePreferences(reminderEnabled = enabled) },
                onOpenTutorial = {
                    showSettings = false
                    showTutorial = true
                },
                onOpenPrivacyPolicy = {
                    showSettings = false
                    showPrivacyPolicy = true
                },
                onExportGdprJson = {
                    viewModel.exportUserDataJson { json ->
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_SUBJECT, "AllenaMente Export GDPR JSON")
                            putExtra(Intent.EXTRA_TEXT, json)
                        }
                        context.startActivity(Intent.createChooser(send, "Esporta Dati JSON AllenaMente"))
                    }
                },
                onConfirmFullReset = {
                    showSettings = false
                    viewModel.deleteAllUserData(onDeleted = onSignOut)
                },
                onSignOut = {
                    showSettings = false
                    onSignOut()
                }
            )
        }
    }
}

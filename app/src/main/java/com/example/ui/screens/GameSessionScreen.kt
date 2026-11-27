package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.AppTheme
import com.example.data.model.SessionResult
import com.example.data.model.UserProfile
import com.example.data.model.gameById
import com.example.engine.DuelLinkCodec
import com.example.engine.SoundSynthEngine
import com.example.ui.components.CelebrationParticlesCanvas
import com.example.ui.theme.LocalPillarColors
import com.example.viewmodel.ActiveGameLaunch
import kotlinx.coroutines.delay

@Composable
fun GameSessionScreen(
    launch: ActiveGameLaunch,
    profile: UserProfile,
    sessionResult: SessionResult?,
    onCompleteGame: (SessionResult) -> Unit,
    onAdvanceCircuit: () -> Unit,
    onReplayWithSeed: (newSeed: Int) -> Unit,
    onExit: () -> Unit
) {
    BackHandler { onExit() }

    val context = LocalContext.current
    val game = remember(launch.gameId) { gameById(launch.gameId) }
    val pillars = LocalPillarColors.current
    val nightCalm = profile.themeEnum() == AppTheme.OBSIDIAN_DARK

    var phase by remember(launch) { mutableStateOf("tutorial") } // tutorial -> countdown -> playing -> result
    var countdown by remember(launch) { mutableIntStateOf(3) }
    var playStartMs by remember(launch) { mutableLongStateOf(System.currentTimeMillis()) }
    var celebrationTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(phase) {
        if (phase == "countdown") {
            for (n in 3 downTo 1) {
                countdown = n
                SoundSynthEngine.playTap(profile.soundEnabled, nightCalm)
                delay(650)
            }
            playStartMs = System.currentTimeMillis()
            phase = "playing"
        }
    }

    LaunchedEffect(sessionResult) {
        if (sessionResult != null) {
            phase = "result"
            celebrationTick += 1
            SoundSynthEngine.playMascotSignature(profile.mascotEnum(), profile.soundEnabled, nightCalm)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top HUD bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onExit,
                    modifier = Modifier.size(48.dp).testTag("game_exit_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Esci dall'esercizio")
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(game.titleIt, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = buildString {
                            append("Lv.${launch.level}")
                            if (launch.dailyCircuitStage != null) append(" • Circuito ${launch.dailyCircuitStage + 1}/4")
                            if (launch.duelChallenge != null) append(" • Duello 1v1")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (launch.zenMode) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Rounded.SelfImprovement, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("Zen", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                } else {
                    Spacer(Modifier.width(48.dp))
                }
            }

            when (phase) {
                "tutorial" -> {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                Text(
                                    text = "${game.pillar.titleIt.uppercase()} • ${game.neuroTag}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                            Text(game.titleIt, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                            Text(
                                text = game.microTutorialIt,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            if (launch.zenMode) {
                                Text(
                                    text = "Modalità Flusso Zen attiva: nessun timer, concentrati sul ragionamento.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                            }
                            Button(
                                onClick = {
                                    if (launch.zenMode) {
                                        playStartMs = System.currentTimeMillis()
                                        phase = "playing"
                                    } else {
                                        phase = "countdown"
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("game_begin_button")
                            ) {
                                Text(if (launch.zenMode) "Inizia in Flusso Zen" else "Pronto! (3-2-1)", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }

                "countdown" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$countdown",
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                "playing" -> {
                    GameBoardDispatcher(
                        gameId = launch.gameId,
                        level = launch.level,
                        seed = launch.seed,
                        zenMode = launch.zenMode,
                        soundEnabled = profile.soundEnabled,
                        nightCalm = nightCalm,
                        onCompleted = { score, accuracy, avgRt ->
                            val durationSec = ((System.currentTimeMillis() - playStartMs) / 1000L).toInt().coerceIn(1, 3600)
                            onCompleteGame(
                                SessionResult(
                                    gameId = launch.gameId,
                                    pillar = game.pillar,
                                    level = launch.level,
                                    score = score.coerceIn(0, 10000),
                                    accuracyPercent = accuracy.coerceIn(0, 100),
                                    avgReactionMs = avgRt.coerceIn(100, 60000),
                                    durationSec = durationSec,
                                    zenModeUsed = launch.zenMode,
                                    duelSeed = launch.seed
                                )
                            )
                        }
                    )
                }

                "result" -> {
                    val res = sessionResult
                    if (res != null) {
                        Card(
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(22.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Rounded.EmojiEvents,
                                    contentDescription = null,
                                    tint = pillars.logic,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text("Sessione Completata!", style = MaterialTheme.typography.headlineMedium)
                                if (res.unlockedNextLevel) {
                                    Surface(shape = RoundedCornerShape(10.dp), color = pillars.success.copy(alpha = 0.18f)) {
                                        Text(
                                            text = "Livello ${res.level + 1} sbloccato!",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = pillars.success,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                        )
                                    }
                                }

                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ResultStatPill("Punteggio", "${res.score} pt", Modifier.weight(1f))
                                    ResultStatPill("Accuratezza", "${res.accuracyPercent}%", Modifier.weight(1f))
                                    ResultStatPill("Reazione", "${res.avgReactionMs}ms", Modifier.weight(1f))
                                }

                                // 1v1 Duel Split Comparison (Tu vs Amico)
                                val duel = launch.duelChallenge
                                if (duel != null) {
                                    val deltaMs = res.avgReactionMs - duel.challengerReactionMs
                                    val userWon = res.score >= duel.challengerScore
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(
                                                text = if (userWon) "Hai vinto il Duello vs ${duel.challengerName}!" else "Duello vs ${duel.challengerName}",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Punteggio: Tu ${res.score} pt vs ${duel.challengerScore} pt",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            Text(
                                                text = "Accuratezza: Tu ${res.accuracyPercent}% vs ${duel.challengerAccuracy}%",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            Text(
                                                text = "Delta Reazione: ${if (deltaMs <= 0) "${deltaMs}ms (più veloce!)" else "+${deltaMs}ms"}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                if (launch.dailyCircuitStage != null && launch.dailyCircuitStage < 3) {
                                    Button(
                                        onClick = onAdvanceCircuit,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth().height(52.dp)
                                    ) {
                                        Text("Prossimo Pilastro (${launch.dailyCircuitStage + 2}/4)")
                                    }
                                } else {
                                    Button(
                                        onClick = onExit,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth().height(52.dp)
                                    ) {
                                        Text("Torna alla Dashboard")
                                    }
                                }

                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedButton(
                                        onClick = { onReplayWithSeed(launch.seed + 101) },
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.weight(1f).height(48.dp)
                                    ) {
                                        Icon(Icons.Rounded.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Nuovo Schema")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            val msg = DuelLinkCodec.buildShareMessage(
                                                gameId = res.gameId,
                                                level = res.level,
                                                seed = launch.seed,
                                                challengerName = profile.displayName,
                                                score = res.score,
                                                reactionMs = res.avgReactionMs,
                                                accuracy = res.accuracyPercent
                                            )
                                            val send = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, msg)
                                            }
                                            context.startActivity(Intent.createChooser(send, "Sfida 1v1 AllenaMente"))
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.weight(1f).height(48.dp)
                                    ) {
                                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Sfida 1v1")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (celebrationTick > 0) {
            CelebrationParticlesCanvas(triggerKey = celebrationTick)
        }
    }
}

@Composable
private fun ResultStatPill(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

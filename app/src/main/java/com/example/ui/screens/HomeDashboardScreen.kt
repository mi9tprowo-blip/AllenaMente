package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Balance
import com.example.data.model.DailyStat
import com.example.data.model.DuelChallenge
import com.example.data.model.UserProfile
import com.example.data.model.gameById
import com.example.engine.CognitiveBalanceInsight
import com.example.ui.theme.LocalPillarColors
import com.example.ui.theme.LocalStyleTokens

@Composable
fun HomeDashboardScreen(
    profile: UserProfile,
    todayStat: DailyStat?,
    todayDateKey: String,
    pendingDuel: DuelChallenge?,
    balanceInsight: CognitiveBalanceInsight,
    onStartDailyCircuit: () -> Unit,
    onLaunchGame: (gameId: String) -> Unit,
    onAcceptDuel: (DuelChallenge) -> Unit,
    onDismissDuel: () -> Unit,
    onToggleZenMode: (Boolean) -> Unit,
    onOpenCalibration: () -> Unit,
    onOpenTutorial: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val pillars = LocalPillarColors.current
    val style = LocalStyleTokens.current
    val ringsDone = todayStat?.completedRingsCount ?: 0
    val freezeProtectedToday = profile.lastFreezeUsedDate == todayDateKey

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top bar: Greeting + Streak + Streak Freeze + Settings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Ciao, ${profile.displayName}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${profile.mascotEnum().labelIt} • ${profile.mascotEnum().mottoIt}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onOpenTutorial,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("open_tutorial_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.HelpOutline,
                            contentDescription = "Apri Guida Rapida",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("open_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Apri Impostazioni",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            // Status Pills Row: Streak + Ghiaccio della Serie + Speed Index
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Streak Pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = pillars.streakFlame.copy(alpha = 0.13f),
                    border = BorderStroke(style.cardBorderWidth, pillars.streakFlame.copy(alpha = 0.35f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.LocalFireDepartment,
                            contentDescription = "Serie giorni",
                            tint = pillars.streakFlame,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "${profile.currentStreak} gg",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Serie attiva",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Ghiaccio della Serie (Monthly Streak Freeze)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = pillars.streakIce.copy(alpha = 0.13f),
                    border = BorderStroke(style.cardBorderWidth, pillars.streakIce.copy(alpha = 0.35f)),
                    modifier = Modifier.weight(1.25f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.AcUnit,
                            contentDescription = "Ghiaccio della Serie",
                            tint = pillars.streakIce,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "${profile.streakFreezesAvailable}/1 protetto",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Ghiaccio Serie",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (freezeProtectedToday) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = pillars.streakIce.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, pillars.streakIce.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Rounded.AcUnit, contentDescription = null, tint = pillars.streakIce)
                        Text(
                            text = "Il tuo Ghiaccio della Serie ha protetto lo streak dopo un giorno di pausa!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Incoming 1v1 Duel Banner if opened from deep link
            if (pendingDuel != null) {
                val duelGame = gameById(pendingDuel.gameId)
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    border = BorderStroke(style.cardBorderWidth, MaterialTheme.colorScheme.secondary)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.SportsEsports, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Text(
                                text = "Sfida 1v1 da ${pendingDuel.challengerName}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Text(
                            text = "${duelGame.titleIt} (Lv.${pendingDuel.level}) • Record da battere: ${pendingDuel.challengerScore} pt (${pendingDuel.challengerAccuracy}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { onAcceptDuel(pendingDuel) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Accetta Sfida")
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                                modifier = Modifier
                                    .clickable { onDismissDuel() }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text("Dopo", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }

            // MAIN HERO CARD: Circuito Neurale del Giorno (Single Focused Operational Card)
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(style.cardBorderWidth, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = if (style.cardBorderWidth > 1.dp) 0.dp else 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                    MaterialTheme.colorScheme.surface,
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.06f)
                                )
                            )
                        )
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "CIRCUITO NEURALE DEL GIORNO",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (ringsDone >= 4) "Circuito completato!" else "4 Pilastri in ~3 minuti",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "$ringsDone/4",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // 4 Animated Pillar Completion Rings
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DailyRingBadge("Logica", todayStat?.logicCompleted == true, pillars.logic)
                        DailyRingBadge("Memoria", todayStat?.memoryCompleted == true, pillars.memory)
                        DailyRingBadge("Riflessi", todayStat?.reflexCompleted == true, pillars.reflex)
                        DailyRingBadge("Empatia", todayStat?.empathyCompleted == true, pillars.empathy)
                    }

                    // Flusso Zen Quick Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = profile.zenFlowMode,
                            onClick = { onToggleZenMode(!profile.zenFlowMode) },
                            label = { Text("Flusso Zen (Senza Timer)") },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.SelfImprovement,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.testTag("home_zen_mode_chip")
                        )
                        Text(
                            text = "${profile.speedIndex} pt",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = onStartDailyCircuit,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("start_daily_circuit_button")
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (ringsDone >= 4) "Ripeti Circuito Quotidiano" else "Avvia Circuito (${ringsDone + 1}/4)",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            // Smart Cognitive Balance & Rest Advisor Card (Harmony + Anti-Fatigue)
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(style.cardBorderWidth, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLaunchGame(balanceInsight.recommendedGame.id) }
                    .testTag("cognitive_balance_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Balance,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = if (balanceInsight.restingPillar != null) {
                                "Riposo Cognitivo • Armonia ${balanceInsight.balanceIndex}%"
                            } else {
                                "Consiglio Smart • Armonia ${balanceInsight.balanceIndex}%"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = balanceInsight.recommendationReason,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Avvia esercizio consigliato",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Optional Calibration Reminder Banner if user skipped earlier
            if (!profile.calibrationCompleted) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenCalibration() }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Calibrazione Iniziale disponibile",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "4 micro-sfide (~2 min) per tarare subito il Radar.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }

            // Extra Bonus Spotlight Quick Access
            Text(
                text = "Extra Bonus • Anteprima Premium",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BonusQuickCard(
                    title = "Circuito Sinaptico",
                    subtitle = "Logica • BFS 90°",
                    icon = Icons.Rounded.Memory,
                    accent = pillars.logic,
                    onClick = { onLaunchGame("neural-circuit") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_neural_circuit_card")
                )
                BonusQuickCard(
                    title = "Bussola Empatica",
                    subtitle = "Empatia • Director",
                    icon = Icons.Rounded.Explore,
                    accent = pillars.empathy,
                    onClick = { onLaunchGame("perspective-compass") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_perspective_compass_card")
                )
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun DailyRingBadge(label: String, completed: Boolean, accent: Color) {
    val sweep by animateFloatAsState(
        targetValue = if (completed) 360f else 48f,
        animationSpec = tween(600),
        label = label
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(52.dp)) {
                val strokePx = 6.dp.toPx()
                drawArc(
                    color = accent.copy(alpha = 0.18f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
                drawArc(
                    color = accent,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
            if (completed) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = "$label completato",
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Icon(
                    Icons.Rounded.Bolt,
                    contentDescription = "$label da completare",
                    tint = accent.copy(alpha = 0.65f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BonusQuickCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val style = LocalStyleTokens.current
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(style.cardBorderWidth, accent.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(accent.copy(alpha = 0.16f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

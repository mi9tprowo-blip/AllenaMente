package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CognitivePillar
import com.example.data.model.GAMES_CATALOG
import com.example.data.model.GameDefinition
import com.example.data.model.UserProfile
import com.example.ui.theme.LocalPillarColors
import com.example.ui.theme.LocalStyleTokens
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymCatalogScreen(
    profile: UserProfile,
    onLaunchGame: (gameId: String, level: Int, zenMode: Boolean) -> Unit,
    onShareDuelChallenge: (gameId: String, level: Int) -> Unit
) {
    val pillars = LocalPillarColors.current
    val style = LocalStyleTokens.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedPillar by remember { mutableStateOf<CognitivePillar?>(null) }
    var selectedGameForSheet by remember { mutableStateOf<GameDefinition?>(null) }

    val filteredGames = remember(searchQuery, selectedPillar) {
        GAMES_CATALOG.filter { game ->
            val matchesPillar = selectedPillar == null || game.pillar == selectedPillar
            val q = searchQuery.trim().lowercase()
            val matchesSearch = q.isEmpty() ||
                game.titleIt.lowercase().contains(q) ||
                game.shortDescIt.lowercase().contains(q) ||
                game.neuroTag.lowercase().contains(q)
            matchesPillar && matchesSearch
        }
    }

    val coreGames = remember(filteredGames) { filteredGames.filter { !it.bonusPremium } }
    val bonusGames = remember(filteredGames) { filteredGames.filter { it.bonusPremium } }

    fun pillarColor(p: CognitivePillar): Color = when (p) {
        CognitivePillar.LOGIC -> pillars.logic
        CognitivePillar.MEMORY -> pillars.memory
        CognitivePillar.REFLEX -> pillars.reflex
        CognitivePillar.EMPATHY -> pillars.empathy
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Palestra Cognitiva",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "18 giochi procedurali • 100 livelli ciascuno (1.800 configurazioni)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cerca gioco o area cognitiva…") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Pulisci ricerca")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("catalog_search_input")
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedPillar == null,
                        onClick = { selectedPillar = null },
                        label = { Text("Tutti (18)") }
                    )
                }
                items(CognitivePillar.entries) { pillar ->
                    FilterChip(
                        selected = selectedPillar == pillar,
                        onClick = { selectedPillar = if (selectedPillar == pillar) null else pillar },
                        label = { Text(pillar.titleIt) }
                    )
                }
            }
        }

        // Extra Bonus • Anteprima Premium section
        if (bonusGames.isNotEmpty()) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Extra Bonus • Anteprima Premium",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            items(bonusGames, key = { "bonus_${it.id}" }) { game ->
                val accent = pillarColor(game.pillar)
                val unlocked = profile.unlockedLevelFor(game.id)
                GameCatalogCard(
                    game = game,
                    unlockedLevel = unlocked,
                    accent = accent,
                    borderWidth = style.cardBorderWidth,
                    isBonusSpotlight = true,
                    onClick = { selectedGameForSheet = game }
                )
            }
        }

        if (coreGames.isNotEmpty()) {
            item {
                Text(
                    text = "Core Suite (${coreGames.size} Esercizi)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            items(coreGames.chunked(2), key = { row -> row.joinToString("_") { it.id } }) { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    pair.forEach { game ->
                        GameCatalogCard(
                            game = game,
                            unlockedLevel = profile.unlockedLevelFor(game.id),
                            accent = pillarColor(game.pillar),
                            borderWidth = style.cardBorderWidth,
                            isBonusSpotlight = false,
                            onClick = { selectedGameForSheet = game },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        item { Spacer(Modifier.height(14.dp)) }
    }

    // Launch & Level Selector Sheet
    val game = selectedGameForSheet
    if (game != null) {
        val unlocked = profile.unlockedLevelFor(game.id)
        var chosenLevel by remember(game.id) { mutableFloatStateOf(unlocked.toFloat()) }
        var localZen by remember(game.id) { mutableStateOf(profile.zenFlowMode && game.supportsZenMode) }
        val accent = pillarColor(game.pillar)

        ModalBottomSheet(onDismissRequest = { selectedGameForSheet = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = accent.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = "${game.pillar.titleIt.uppercase()} • ${game.neuroTag}",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                Text(
                    text = game.titleIt,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = game.microTutorialIt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Level slider (1..100, highlights unlocked progression)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Livello Procedurale",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Lv. ${chosenLevel.roundToInt()} / 100 (sbloccato: $unlocked)",
                            style = MaterialTheme.typography.labelMedium,
                            color = accent
                        )
                    }
                    Slider(
                        value = chosenLevel,
                        onValueChange = { chosenLevel = it },
                        valueRange = 1f..maxOf(unlocked, 5).coerceAtMost(100).toFloat(),
                        steps = (maxOf(unlocked, 5).coerceAtMost(100) - 2).coerceAtLeast(0)
                    )
                }

                if (game.supportsZenMode) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Rounded.SelfImprovement, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Column {
                                    Text("Flusso Zen (Senza Timer)", style = MaterialTheme.typography.titleSmall)
                                    Text("Gioca senza pressione temporale", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Switch(checked = localZen, onCheckedChange = { localZen = it })
                        }
                    }
                }

                Button(
                    onClick = {
                        val lv = chosenLevel.roundToInt()
                        selectedGameForSheet = null
                        onLaunchGame(game.id, lv, localZen)
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("launch_selected_game_button")
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Avvia Allenamento", style = MaterialTheme.typography.labelLarge)
                }

                OutlinedButton(
                    onClick = {
                        val lv = chosenLevel.roundToInt()
                        selectedGameForSheet = null
                        onShareDuelChallenge(game.id, lv)
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sfida un Amico 1v1 (Stesso Seed)")
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun GameCatalogCard(
    game: GameDefinition,
    unlockedLevel: Int,
    accent: Color,
    borderWidth: androidx.compose.ui.unit.Dp,
    isBonusSpotlight: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isBonusSpotlight) accent.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            borderWidth,
            if (isBonusSpotlight) accent.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier.testTag("game_card_${game.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(accent.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = game.pillar.shortIt,
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Lv.$unlockedLevel",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = game.titleIt,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = game.shortDescIt,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (game.supportsZenMode) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Rounded.SelfImprovement,
                        contentDescription = "Supporta Flusso Zen",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Flusso Zen",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

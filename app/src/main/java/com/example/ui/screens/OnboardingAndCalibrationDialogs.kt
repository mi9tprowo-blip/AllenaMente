package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.AGE_COHORTS
import com.example.engine.CorePuzzleGenerators
import com.example.engine.ExpressiveFaceGenerator
import com.example.ui.components.ExpressiveFaceCanvas
import com.example.ui.theme.LocalPillarColors
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingTermsDialog(
    initialName: String,
    onOpenLegal: () -> Unit = {},
    onComplete: (displayName: String, ageCohort: String, analyticsOptIn: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedCohort by remember { mutableStateOf("25-34") }
    // All consents start strictly UNCHECKED (GDPR / Garante Privacy Italy)
    var acceptedTerms by remember { mutableStateOf(false) }
    var confirmedAgeOrParentalConsent by remember { mutableStateOf(false) }
    var analyticsOptIn by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { /* Mandatory consent before play */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Benvenuto in AllenaMente",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "App 100% gratuita, senza pubblicità né costi nascosti. Personalizza il tuo profilo ludico.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 50) name = it },
                    label = { Text("Nome profilo") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_name_input")
                )

                Text(
                    text = "Fascia d'età (solo per tarare l'Indice di Velocità ludico)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AGE_COHORTS.forEach { cohort ->
                        FilterChip(
                            selected = selectedCohort == cohort,
                            onClick = { selectedCohort = cohort },
                            label = { Text(cohort) }
                        )
                    }
                }

                // Consent 1 (Mandatory, NOT pre-checked): Terms, Privacy & Non-Clinical Disclaimer
                ConsentCheckboxCard(
                    checked = acceptedTerms,
                    onCheckedChange = { acceptedTerms = it },
                    text = "Accetto i Termini e Condizioni e l'Informativa Privacy; comprendo che AllenaMente è un'app ludica e non uno strumento medico o diagnostico.",
                    testTag = "onboarding_terms_checkbox"
                )

                // Consent 2 (Mandatory, NOT pre-checked): Under-14 Age / Parental Consent (Art. 2-quinquies Italy)
                ConsentCheckboxCard(
                    checked = confirmedAgeOrParentalConsent,
                    onCheckedChange = { confirmedAgeOrParentalConsent = it },
                    text = "Dichiaro di avere almeno 14 anni, oppure di utilizzare l'app con il consenso di un genitore o tutore legale.",
                    testTag = "onboarding_age_checkbox"
                )

                // Consent 3 (Optional, NOT pre-checked): Anonymous Firebase Analytics
                ConsentCheckboxCard(
                    checked = analyticsOptIn,
                    onCheckedChange = { analyticsOptIn = it },
                    text = "Facoltativo: attiva statistiche anonime di utilizzo (Firebase Analytics senza Advertising ID né dati personali). Modificabile in ogni momento.",
                    testTag = "onboarding_analytics_checkbox"
                )

                TextButton(
                    onClick = onOpenLegal,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .testTag("onboarding_open_legal_button")
                ) {
                    Text(stringResource(R.string.privacy_link_button))
                }

                Button(
                    onClick = { onComplete(name, selectedCohort, analyticsOptIn) },
                    enabled = acceptedTerms && confirmedAgeOrParentalConsent && name.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("onboarding_continue_button")
                ) {
                    Text("Inizia il Percorso", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun ConsentCheckboxCard(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    text: String,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = checked,
                    role = Role.Checkbox,
                    onValueChange = onCheckedChange
                )
                .padding(10.dp)
                .testTag(testTag),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = null
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

/**
 * GDPR / Garante Privacy compliant opt-in Analytics Consent Banner:
 * - Zero pre-checked consents (analytics stays OFF until accepted)
 * - Visually equal "Rifiuta" and "Accetta" buttons (no dark patterns)
 */
@Composable
fun AnalyticsConsentDialog(
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit
) {
    Dialog(
        onDismissRequest = onReject,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.QueryStats,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = stringResource(R.string.consent_banner_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = stringResource(R.string.consent_banner_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TextButton(
                    onClick = onOpenPrivacyPolicy,
                    modifier = Modifier.testTag("analytics_consent_privacy_link")
                ) {
                    Text(stringResource(R.string.consent_banner_learn_more))
                }

                // Equal-prominence Reject and Accept actions (zero dark patterns)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("analytics_consent_reject")
                    ) {
                        Text(
                            text = stringResource(R.string.consent_banner_reject),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    Button(
                        onClick = onAccept,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("analytics_consent_accept")
                    ) {
                        Text(
                            text = stringResource(R.string.consent_banner_accept),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

/**
 * High-contrast Initial Calibration Modal (fixes Section 8.2 dark-title-on-dark-glass contrast bug).
 * Offers free immediate exit ("Esplora subito la Home") OR 4 real micro-challenges (~2 min).
 */
@Composable
fun InitialCalibrationDialog(
    onSkipToHome: () -> Unit,
    onCompleteCalibration: (logic: Int, memory: Int, reflex: Int, empathy: Int, speedIndex: Int) -> Unit
) {
    val pillars = LocalPillarColors.current
    var step by remember { mutableIntStateOf(0) } // 0=Intro, 1=Logic, 2=Memory, 3=Reflex, 4=Empathy
    var logicScore by remember { mutableIntStateOf(60) }
    var memoryScore by remember { mutableIntStateOf(60) }
    var reflexScore by remember { mutableIntStateOf(60) }
    var empathyScore by remember { mutableIntStateOf(60) }
    var avgRtMs by remember { mutableLongStateOf(650L) }

    Dialog(
        onDismissRequest = onSkipToHome,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Calibrazione Iniziale",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (step in 1..4) {
                        Text(
                            text = "$step / 4",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (step in 1..4) {
                    LinearProgressIndicator(
                        progress = { step / 4f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }

                when (step) {
                    0 -> {
                        Text(
                            text = "Popola subito il tuo Radar a 4 Assi e l'Indice di Velocità (pt) con 4 micro-sfide rapide da ~2 minuti, oppure entra direttamente nella Home.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("1. Logica — Sequenza rapida", style = MaterialTheme.typography.bodyMedium, color = pillars.logic, fontWeight = FontWeight.SemiBold)
                                Text("2. Memoria — Griglia di Luce 3×3", style = MaterialTheme.typography.bodyMedium, color = pillars.memory, fontWeight = FontWeight.SemiBold)
                                Text("3. Riflessi — Freno Rapido Go/No-Go", style = MaterialTheme.typography.bodyMedium, color = pillars.reflex, fontWeight = FontWeight.SemiBold)
                                Text("4. Empatia — Micro-espressione guidata", style = MaterialTheme.typography.bodyMedium, color = pillars.empathy, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Button(
                            onClick = { step = 1 },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("calibration_start_button")
                        ) {
                            Text("Calibra il tuo Profilo (~2 min)", style = MaterialTheme.typography.labelLarge)
                        }
                        OutlinedButton(
                            onClick = onSkipToHome,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("calibration_skip_button")
                        ) {
                            Text("Esplora subito la Home")
                        }
                    }

                    1 -> {
                        // Micro-challenge 1: Logic Sequence
                        val seq = remember { CorePuzzleGenerators.generateSequence(level = 2, seed = 20261) }
                        Text(
                            text = "Pilastro 1 • Logica: quale numero completa la sequenza?",
                            style = MaterialTheme.typography.titleMedium,
                            color = pillars.logic
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            seq.items.forEach { num ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = num,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 14.dp)
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = pillars.logic.copy(alpha = 0.18f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "?",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = pillars.logic,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 14.dp)
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            seq.options.forEachIndexed { idx, opt ->
                                Button(
                                    onClick = {
                                        logicScore = if (idx == seq.correctIndex) 82 else 48
                                        step = 2
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                ) {
                                    Text(opt)
                                }
                            }
                        }
                    }

                    2 -> {
                        // Micro-challenge 2: Memory 3x3 Light Grid
                        val litSet = remember { setOf(0, 4, 8) }
                        var showing by remember { mutableStateOf(true) }
                        var tapped by remember { mutableStateOf(setOf<Int>()) }
                        LaunchedEffect(Unit) {
                            delay(1800)
                            showing = false
                        }
                        Text(
                            text = if (showing) "Pilastro 2 • Memoria: memorizza le 3 celle illuminate…"
                            else "Tocca le 3 celle che si sono illuminate (${tapped.size}/3)",
                            style = MaterialTheme.typography.titleMedium,
                            color = pillars.memory
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (r in 0..2) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    for (c in 0..2) {
                                        val idx = r * 3 + c
                                        val isLit = (showing && idx in litSet) || (!showing && idx in tapped)
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1.4f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    if (isLit) pillars.memory else MaterialTheme.colorScheme.surfaceVariant
                                                )
                                                .clickable(enabled = !showing && idx !in tapped) {
                                                    val next = tapped + idx
                                                    tapped = next
                                                    if (next.size == litSet.size) {
                                                        val matches = next.intersect(litSet).size
                                                        memoryScore = (35 + matches * 18).coerceIn(40, 88)
                                                        step = 3
                                                    }
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        // Micro-challenge 3: Reflex Go Tap
                        var goReady by remember { mutableStateOf(false) }
                        var goStartMs by remember { mutableLongStateOf(0L) }
                        LaunchedEffect(Unit) {
                            delay(1200)
                            goStartMs = System.currentTimeMillis()
                            goReady = true
                        }
                        Text(
                            text = "Pilastro 3 • Riflessi: tocca appena il segnale diventa VERDE!",
                            style = MaterialTheme.typography.titleMedium,
                            color = pillars.reflex
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (goReady) Color(0xFF16A34A) else Color(0xFFDC2626)
                                )
                                .clickable {
                                    if (goReady) {
                                        val rt = (System.currentTimeMillis() - goStartMs).coerceIn(180L, 1500L)
                                        avgRtMs = rt
                                        reflexScore = ((1250 - rt) / 11).toInt().coerceIn(45, 92)
                                        step = 4
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (goReady) "TOCCA ORA!" else "ATTENDI…",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                        }
                    }

                    4 -> {
                        // Micro-challenge 4: Empathy Expressive Face
                        val round = remember {
                            ExpressiveFaceGenerator.generateRound(level = 1, seed = 4049, eyesOnlyMode = false, zenMode = true)
                        }
                        Text(
                            text = "Pilastro 4 • Empatia: quale emozione esprime questo volto?",
                            style = MaterialTheme.typography.titleMedium,
                            color = pillars.empathy
                        )
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            ExpressiveFaceCanvas(
                                spec = round.target,
                                eyesOnlyMode = false,
                                showCueHighlights = false
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            round.options.chunked(2).forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    pair.forEach { opt ->
                                        Button(
                                            onClick = {
                                                empathyScore = if (opt.id == round.target.id) 84 else 52
                                                val avgPillar = (logicScore + memoryScore + reflexScore + empathyScore) / 4
                                                val speedIdx = (avgPillar * 7 + (1100 - avgRtMs.toInt()) / 4).coerceIn(320, 860)
                                                onCompleteCalibration(logicScore, memoryScore, reflexScore, empathyScore, speedIdx)
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                        ) {
                                            Text(opt.labelIt, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (step in 1..4) {
                    TextButton(
                        onClick = onSkipToHome,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Salta e vai alla Home")
                    }
                }
            }
        }
    }
}

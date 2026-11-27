package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

data class TutorialChapter(
    val number: String,
    val title: String,
    val body: String,
    val highlight: String,
    val icon: ImageVector
)

val ALLENA_MENTE_TUTORIAL_CHAPTERS: List<TutorialChapter> = listOf(
    TutorialChapter(
        number = "01 / 04",
        title = "Circuito dei 4 Pilastri",
        body = "Ogni giorno completa i 4 anelli — Logica, Memoria, Riflessi ed Empatia — in circa 3 minuti per mantenere viva la tua serie.",
        highlight = "18 esercizi procedurali con 100 livelli progressivi ciascuno.",
        icon = Icons.Rounded.GridView
    ),
    TutorialChapter(
        number = "02 / 04",
        title = "Flusso Zen & Riposo Cognitivo",
        body = "Attiva Flusso Zen per rimuovere il timer dai puzzle spaziali e logici. Il motore di Armonia ti avvisa quando un pilastro ha già lavorato molto oggi.",
        highlight = "Il Ghiaccio della Serie protegge lo streak in caso di un giorno di pausa.",
        icon = Icons.Rounded.SelfImprovement
    ),
    TutorialChapter(
        number = "03 / 04",
        title = "Coach AI & Sfide 1v1",
        body = "Nella scheda Risultati il Coach AI analizza il tuo Radar a 4 Assi e suggerisce il prossimo esercizio. Con i link sfida 1v1 puoi competere sullo stesso identico schema.",
        highlight = "Condividi la tua Patente Neurale come card visiva HD.",
        icon = Icons.Rounded.AutoAwesome
    ),
    TutorialChapter(
        number = "04 / 04",
        title = "Privacy GDPR & Uso Ludico",
        body = "AllenaMente è progettata per il benessere cognitivo quotidiano e l'intrattenimento, senza valenza clinica né tracker pubblicitari.",
        highlight = "Esporta i tuoi dati JSON o azzera tutto in 2 passaggi dalle Impostazioni.",
        icon = Icons.Rounded.VerifiedUser
    )
)

@Composable
fun AllenaMenteTutorialDialog(
    onDismiss: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    val chapter = ALLENA_MENTE_TUTORIAL_CHAPTERS[step]

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tutorial_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "GUIDA RAPIDA • ${chapter.number}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = chapter.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = chapter.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = chapter.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = chapter.highlight,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                // Step dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ALLENA_MENTE_TUTORIAL_CHAPTERS.indices.forEach { idx ->
                        Box(
                            modifier = Modifier
                                .size(if (idx == step) 10.dp else 7.dp)
                                .background(
                                    if (idx == step) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant,
                                    CircleShape
                                )
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            if (step > 0) step-- else onDismiss()
                        },
                        modifier = Modifier.testTag("tutorial_back_button")
                    ) {
                        Text(if (step > 0) "Indietro" else "Chiudi")
                    }

                    Button(
                        onClick = {
                            if (step < ALLENA_MENTE_TUTORIAL_CHAPTERS.lastIndex) {
                                step++
                            } else {
                                onDismiss()
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("tutorial_next_button")
                    ) {
                        Spacer(Modifier.width(6.dp))
                        Text(if (step < ALLENA_MENTE_TUTORIAL_CHAPTERS.lastIndex) "Avanti" else "Inizia")
                        Spacer(Modifier.width(6.dp))
                    }
                }
            }
        }
    }
}

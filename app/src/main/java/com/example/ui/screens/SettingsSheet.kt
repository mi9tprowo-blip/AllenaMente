package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.AppTheme
import com.example.data.model.MascotType
import com.example.data.model.UserProfile
import com.example.engine.ReminderHelper
import com.example.engine.SoundSynthEngine

import androidx.compose.material.icons.rounded.HelpOutline

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsBottomSheet(
    profile: UserProfile,
    userEmail: String?,
    onDismiss: () -> Unit,
    onSelectTheme: (AppTheme) -> Unit,
    onSelectMascot: (MascotType) -> Unit,
    onToggleSound: (Boolean) -> Unit,
    onToggleZenMode: (Boolean) -> Unit,
    onToggleReminder: (Boolean) -> Unit,
    onOpenTutorial: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onExportGdprJson: () -> Unit,
    onConfirmFullReset: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    var deleteConfirmStep by remember { mutableIntStateOf(0) } // 0=none, 1=step1, 2=step2 final

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onToggleReminder(true)
            ReminderHelper.showReminderConfirmation(context, profile.reminderHour, profile.reminderMinute)
        } else {
            onToggleReminder(false)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Impostazioni & Privacy",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Google Account Cloud Sync Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.AccountCircle, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text(profile.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                text = userEmail ?: "Account Google connesso (Sync Cloud attivo)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    TextButton(
                        onClick = onSignOut,
                        modifier = Modifier.testTag("settings_sign_out_button")
                    ) {
                        Icon(Icons.Rounded.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Esci")
                    }
                }
            }

            // 4 Visual Themes
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Tema Visivo (4 Stili)", style = MaterialTheme.typography.titleMedium)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppTheme.entries.forEach { theme ->
                    val selected = profile.themeEnum() == theme
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            if (selected) 2.dp else 1.dp,
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectTheme(theme) }
                            .testTag("theme_option_${theme.name}")
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                text = theme.labelIt,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = theme.subtitleIt,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // 7 Mascots + Live Sound Preview
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Mascotte & Firma Sonora (7 Profili)", style = MaterialTheme.typography.titleMedium)
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MascotType.entries.forEach { mascot ->
                    FilterChip(
                        selected = profile.mascotEnum() == mascot,
                        onClick = {
                            onSelectMascot(mascot)
                            SoundSynthEngine.playMascotSignature(
                                mascot = mascot,
                                enabled = profile.soundEnabled,
                                nightCalm = profile.themeEnum() == AppTheme.OBSIDIAN_DARK
                            )
                        },
                        label = { Text("${mascot.labelIt} • ${mascot.soundLabelIt}") }
                    )
                }
            }

            HorizontalDivider()

            // Audio, Flusso Zen & Reminder Toggles
            SettingsToggleRow(
                icon = Icons.Rounded.VolumeUp,
                title = "Audio Sintetizzato",
                subtitle = if (profile.themeEnum() == AppTheme.OBSIDIAN_DARK) "Filtro Night Calm (-3dB) attivo" else "Sintesi PCM nativa a zero asset",
                checked = profile.soundEnabled,
                onCheckedChange = onToggleSound
            )

            SettingsToggleRow(
                icon = Icons.Rounded.SelfImprovement,
                title = "Flusso Zen (Senza Timer)",
                subtitle = "Rimuove la pressione temporale su puzzle spaziali e logici",
                checked = profile.zenFlowMode,
                onCheckedChange = onToggleZenMode
            )

            SettingsToggleRow(
                icon = Icons.Rounded.NotificationsActive,
                title = "Promemoria Quotidiano",
                subtitle = "Notifica gentile alle %02d:%02d (senza permessi allarme esatto)".format(profile.reminderHour, profile.reminderMinute),
                checked = profile.reminderEnabled,
                onCheckedChange = { wantEnable ->
                    if (!wantEnable) {
                        onToggleReminder(false)
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val hasPerm = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPerm) {
                            onToggleReminder(true)
                            ReminderHelper.showReminderConfirmation(context, profile.reminderHour, profile.reminderMinute)
                        } else {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    } else {
                        onToggleReminder(true)
                        ReminderHelper.showReminderConfirmation(context, profile.reminderHour, profile.reminderMinute)
                    }
                }
            )

            HorizontalDivider()

            // GDPR & Play Store Compliance Controls
            Text("Guida, Privacy & GDPR", style = MaterialTheme.typography.titleMedium)

            OutlinedButton(
                onClick = onOpenTutorial,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("settings_tutorial_button")
            ) {
                Icon(Icons.Rounded.HelpOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Guida Rapida AllenaMente (4 Capitoli)")
            }

            OutlinedButton(
                onClick = onOpenPrivacyPolicy,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("settings_privacy_policy_button")
            ) {
                Icon(Icons.Rounded.Policy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Informativa Privacy & Disclaimer Non-Clinico")
            }

            OutlinedButton(
                onClick = onExportGdprJson,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("gdpr_export_button")
            ) {
                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Esporta Tutti i Dati (JSON GDPR)")
            }

            Button(
                onClick = { deleteConfirmStep = 1 },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("gdpr_delete_button")
            ) {
                Icon(Icons.Rounded.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Elimina Dati & Reset Account (2 Step)")
            }

            Spacer(Modifier.height(22.dp))
        }
    }

    // 2-Step GDPR Reset Confirmation Dialogs
    if (deleteConfirmStep == 1) {
        AlertDialog(
            onDismissRequest = { deleteConfirmStep = 0 },
            title = { Text("Eliminare tutti i dati? (Step 1/2)") },
            text = {
                Text("Questa operazione cancellerà il profilo, i livelli sbloccati, lo streak e tutte le sessioni salvate nel cloud Firestore.")
            },
            confirmButton = {
                TextButton(onClick = { deleteConfirmStep = 2 }) {
                    Text("Continua", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmStep = 0 }) { Text("Annulla") }
            }
        )
    } else if (deleteConfirmStep == 2) {
        AlertDialog(
            onDismissRequest = { deleteConfirmStep = 0 },
            title = { Text("Conferma Definitiva (Step 2/2)") },
            text = {
                Text("Azione irreversibile. Confermi l'eliminazione integrale dei tuoi dati e la disconnessione?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        deleteConfirmStep = 0
                        onConfirmFullReset()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Elimina Tutto")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmStep = 0 }) { Text("Annulla") }
            }
        )
    }
}

@Composable
private fun SettingsToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

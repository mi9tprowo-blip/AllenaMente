package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.firebase.onGoogleSignInClicked
import com.example.ui.theme.LocalPillarColors

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation

private const val PROVISIONAL_ACCESS_CODE = "0000"

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    onProvisionalAccess: () -> Unit = {},
    onOpenPrivacyPolicy: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val pillars = LocalPillarColors.current

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showProvisionalPanel by remember { mutableStateOf(false) }
    var provisionalPin by remember { mutableStateOf("") }
    var provisionalError by remember { mutableStateOf<String?>(null) }
    val invalidPinMessage = stringResource(R.string.provisional_mode_invalid_pin)

    fun submitProvisionalPin() {
        if (provisionalPin.trim() == PROVISIONAL_ACCESS_CODE) {
            provisionalError = null
            onProvisionalAccess()
        } else {
            provisionalError = invalidPinMessage
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        // Subtle geometric background atmosphere
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = pillars.memory.copy(alpha = 0.08f),
                radius = size.minDimension * 0.42f,
                center = Offset(size.width * 0.88f, size.height * 0.14f)
            )
            drawCircle(
                color = pillars.logic.copy(alpha = 0.08f),
                radius = size.minDimension * 0.36f,
                center = Offset(size.width * 0.12f, size.height * 0.84f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Emblem
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color(0xFF0F172A),
                shadowElevation = 6.dp,
                modifier = Modifier.size(84.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Psychology,
                        contentDescription = "Emblema AllenaMente",
                        tint = Color(0xFF2DD4BF),
                        modifier = Modifier.size(46.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.auth_title),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.auth_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            // 4-Pillar Preview Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PillarAuthChip("Logica", Icons.Rounded.GridView, pillars.logic, Modifier.weight(1f))
                PillarAuthChip("Memoria", Icons.Rounded.AutoAwesome, pillars.memory, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PillarAuthChip("Riflessi", Icons.Rounded.Bolt, pillars.reflex, Modifier.weight(1f))
                PillarAuthChip("Empatia", Icons.Rounded.Favorite, pillars.empathy, Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))

            // Feature highlights card
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AuthFeatureRow(
                        icon = Icons.Rounded.CloudDone,
                        title = "Sync Cloud + Cache Offline",
                        subtitle = "I tuoi 18 giochi, livelli e streak restano al sicuro anche cambiando telefono."
                    )
                    AuthFeatureRow(
                        icon = Icons.Rounded.AutoAwesome,
                        title = "Coach AI & Flusso Zen",
                        subtitle = "Analisi personalizzata sui 4 pilastri e modalità puzzle senza timer."
                    )
                    AuthFeatureRow(
                        icon = Icons.Rounded.VerifiedUser,
                        title = "100% Gratuita • Zero Pubblicità • GDPR",
                        subtitle = "Nessun costo nascosto, statistiche solo su consenso, export JSON e cancellazione in 2 tocchi."
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            if (errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                ) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            Button(
                onClick = {
                    errorMessage = null
                    isLoading = true
                    onGoogleSignInClicked(
                        context = context,
                        credentialManager = credentialManager,
                        onAuthSuccess = {
                            isLoading = false
                            onAuthSuccess()
                        },
                        onAuthError = { msg ->
                            isLoading = false
                            errorMessage = msg
                        },
                        scope = scope,
                        onAuthCancelled = {
                            isLoading = false
                        }
                    )
                },
                enabled = !isLoading,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("google_sign_in_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Accesso in corso…", style = MaterialTheme.typography.labelLarge)
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Login,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.auth_google_button),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    text = "oppure",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
            }

            Spacer(Modifier.height(14.dp))

            if (!showProvisionalPanel) {
                OutlinedButton(
                    onClick = { showProvisionalPanel = true },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("provisional_mode_toggle")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Key,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.provisional_mode_title),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            } else {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("provisional_mode_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LockOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.provisional_mode_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = stringResource(R.string.provisional_mode_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = provisionalPin,
                            onValueChange = { input ->
                                val digits = input.filter { it.isDigit() }.take(4)
                                provisionalPin = digits
                                if (provisionalError != null) provisionalError = null
                            },
                            label = { Text(stringResource(R.string.provisional_mode_pin_label)) },
                            placeholder = { Text("0000") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { submitProvisionalPin() }),
                            isError = provisionalError != null,
                            supportingText = provisionalError?.let { err ->
                                { Text(err, color = MaterialTheme.colorScheme.error) }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("provisional_pin_input")
                        )
                        Button(
                            onClick = { submitProvisionalPin() },
                            enabled = provisionalPin.length == 4,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("provisional_enter_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LockOpen,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.provisional_mode_button),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.auth_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(4.dp))

            TextButton(
                onClick = onOpenPrivacyPolicy,
                modifier = Modifier.testTag("auth_privacy_policy_button")
            ) {
                Text(
                    text = stringResource(R.string.privacy_link_button),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun PillarAuthChip(
    label: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = accent.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(accent.copy(alpha = 0.20f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun AuthFeatureRow(icon: ImageVector, title: String, subtitle: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp)
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

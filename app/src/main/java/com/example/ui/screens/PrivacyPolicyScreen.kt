package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Copyright
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.FamilyRestroom
import androidx.compose.material.icons.outlined.GppGood
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoneyOff
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.AllenaMenteTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialTab: Int = 0,
    privacyDocumentUrl: String = stringResource(R.string.privacy_policy_url),
    termsDocumentUrl: String = stringResource(R.string.terms_conditions_url)
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 1)) }

    val contactEmail = stringResource(R.string.privacy_contact_email)
    val activeUrl = if (selectedTab == 0) privacyDocumentUrl else termsDocumentUrl
    val openErrorTemplate = stringResource(R.string.privacy_open_error, activeUrl)

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("privacy_policy_screen"),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.privacy_policy_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("privacy_policy_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.privacy_back_description)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                PrimaryTabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(stringResource(R.string.legal_tab_privacy)) },
                        modifier = Modifier.testTag("legal_tab_privacy")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(stringResource(R.string.legal_tab_terms)) },
                        modifier = Modifier.testTag("legal_tab_terms")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Summary Card
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Policy,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.privacy_policy_updated),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.privacy_policy_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // External Documentation Primary CTA (Google Play Store & EU legal requirement)
                Button(
                    onClick = {
                        runCatching {
                            uriHandler.openUri(activeUrl)
                        }.onFailure {
                            scope.launch {
                                snackbarHostState.showSnackbar(openErrorTemplate)
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("privacy_open_doc_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(
                            if (selectedTab == 0) R.string.privacy_open_full_doc else R.string.terms_open_full_doc
                        ),
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                if (selectedTab == 0) {
                    PrivacySectionCard(
                        icon = Icons.Outlined.CloudDone,
                        title = stringResource(R.string.privacy_section_data_title),
                        body = stringResource(R.string.privacy_section_data_body)
                    )
                    PrivacySectionCard(
                        icon = Icons.Outlined.Code,
                        title = stringResource(R.string.privacy_section_sdk_title),
                        body = stringResource(R.string.privacy_section_sdk_body)
                    )
                    PrivacySectionCard(
                        icon = Icons.Outlined.Lock,
                        title = stringResource(R.string.privacy_section_permissions_title),
                        body = stringResource(R.string.privacy_section_permissions_body)
                    )
                    PrivacySectionCard(
                        icon = Icons.Outlined.VerifiedUser,
                        title = stringResource(R.string.privacy_section_security_title),
                        body = stringResource(R.string.privacy_section_security_body)
                    )
                    PrivacySectionCard(
                        icon = Icons.Outlined.GppGood,
                        title = stringResource(R.string.privacy_section_gdpr_title),
                        body = stringResource(R.string.privacy_section_gdpr_body)
                    )
                } else {
                    PrivacySectionCard(
                        icon = Icons.Outlined.MoneyOff,
                        title = stringResource(R.string.terms_section_free_title),
                        body = stringResource(R.string.terms_section_free_body)
                    )
                    PrivacySectionCard(
                        icon = Icons.Outlined.FamilyRestroom,
                        title = stringResource(R.string.terms_section_minors_title),
                        body = stringResource(R.string.terms_section_minors_body)
                    )
                    PrivacySectionCard(
                        icon = Icons.Outlined.Info,
                        title = stringResource(R.string.privacy_section_disclaimer_title),
                        body = stringResource(R.string.privacy_section_disclaimer_body)
                    )
                    PrivacySectionCard(
                        icon = Icons.Outlined.Copyright,
                        title = stringResource(R.string.terms_section_licenses_title),
                        body = stringResource(R.string.terms_section_licenses_body)
                    )
                    PrivacySectionCard(
                        icon = Icons.Outlined.Business,
                        title = stringResource(R.string.terms_section_company_title),
                        body = stringResource(R.string.terms_section_company_body)
                    )
                }

                // Direct Privacy & Legal Contact CTA
                OutlinedButton(
                    onClick = {
                        val mailIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:$contactEmail")
                            putExtra(Intent.EXTRA_SUBJECT, "AllenaMente — Richiesta Privacy / Legale / GDPR")
                        }
                        runCatching {
                            context.startActivity(mailIntent)
                        }.onFailure {
                            scope.launch {
                                snackbarHostState.showSnackbar("Contatto privacy: $contactEmail")
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("privacy_contact_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Email,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.privacy_contact_button),
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Text(
                    text = activeUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .testTag("privacy_policy_url_text")
                )

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun PrivacySectionCard(
    icon: ImageVector,
    title: String,
    body: String
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PrivacyPolicyScreenPreview() {
    AllenaMenteTheme {
        PrivacyPolicyScreen(onBack = {})
    }
}

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.example.R
import com.example.data.firebase.attemptAutoSignIn
import com.example.data.firebase.authStateFlow
import com.example.data.firebase.onGoogleSignInClicked
import com.example.data.firebase.signOutFromGoogle
import com.example.ui.theme.AllenaMenteTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.DateFormat
import java.util.Date

data class AuthProfileSnapshot(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val phoneNumber: String?,
    val photoUrl: String?,
    val emailVerified: Boolean,
    val providers: List<String>,
    val createdTimestampMs: Long?,
    val lastSignInTimestampMs: Long?
)

private fun FirebaseUser.toAuthProfileSnapshot(): AuthProfileSnapshot {
    val providerNames = providerData
        .map { it.providerId }
        .filter { it.isNotBlank() && it != "firebase" }
        .distinct()
        .map { providerId ->
            when (providerId) {
                "google.com" -> "Google"
                "apple.com" -> "Apple"
                "phone" -> "Phone"
                "password" -> "Email"
                else -> providerId
            }
        }
    return AuthProfileSnapshot(
        uid = uid,
        displayName = displayName?.takeIf { it.isNotBlank() },
        email = email?.takeIf { it.isNotBlank() },
        phoneNumber = phoneNumber?.takeIf { it.isNotBlank() },
        photoUrl = photoUrl?.toString(),
        emailVerified = isEmailVerified,
        providers = providerNames.ifEmpty { listOf("Firebase Auth") },
        createdTimestampMs = metadata?.creationTimestamp?.takeIf { it > 0L },
        lastSignInTimestampMs = metadata?.lastSignInTimestamp?.takeIf { it > 0L }
    )
}

/**
 * Fetches and displays the current Firebase Auth user profile information,
 * with silent startup check, interactive Google Sign-In via Credential Manager,
 * live profile refresh via FirebaseUser.reload(), and safe sign-out.
 */
@Composable
fun UserProfile(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val authUser by remember { Firebase.auth.authStateFlow() }
        .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    var checkedAutoSignIn by remember { mutableStateOf(authUser != null) }
    var isSigningIn by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var profileSnapshot by remember { mutableStateOf(authUser?.toAuthProfileSnapshot()) }

    val refreshFailedMessage = stringResource(R.string.profile_refresh_failed)

    // Attempt silent Credential Manager auto sign-in once when no user is currently signed in
    LaunchedEffect(Unit) {
        if (Firebase.auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    profileSnapshot = Firebase.auth.currentUser?.toAuthProfileSnapshot()
                    checkedAutoSignIn = true
                },
                onUnauthenticated = {
                    checkedAutoSignIn = true
                },
                scope = scope
            )
        } else {
            checkedAutoSignIn = true
        }
    }

    // Fetch fresh profile fields from Firebase Auth whenever the authenticated UID changes
    LaunchedEffect(authUser?.uid) {
        val current = authUser
        if (current == null) {
            profileSnapshot = null
            statusMessage = null
            isRefreshing = false
        } else {
            profileSnapshot = current.toAuthProfileSnapshot()
            isRefreshing = true
            statusMessage = null
            try {
                current.reload().await()
                profileSnapshot = (Firebase.auth.currentUser ?: current).toAuthProfileSnapshot()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                statusMessage = refreshFailedMessage
            } finally {
                isRefreshing = false
            }
        }
    }

    val snapshot = profileSnapshot
    when {
        !checkedAutoSignIn -> {
            UserProfileLoadingCard(modifier = modifier)
        }

        snapshot == null -> {
            UserProfileSignedOutCard(
                isSigningIn = isSigningIn,
                errorMessage = statusMessage,
                onSignInClick = {
                    isSigningIn = true
                    statusMessage = null
                    onGoogleSignInClicked(
                        context = context,
                        credentialManager = credentialManager,
                        onAuthSuccess = {
                            isSigningIn = false
                            profileSnapshot = Firebase.auth.currentUser?.toAuthProfileSnapshot()
                        },
                        onAuthError = { msg ->
                            isSigningIn = false
                            statusMessage = msg
                        },
                        onAuthCancelled = {
                            isSigningIn = false
                        },
                        scope = scope
                    )
                },
                modifier = modifier
            )
        }

        else -> {
            UserProfileDetailsCard(
                profile = snapshot,
                isRefreshing = isRefreshing,
                statusMessage = statusMessage,
                onRefresh = {
                    val user = Firebase.auth.currentUser ?: return@UserProfileDetailsCard
                    scope.launch {
                        isRefreshing = true
                        statusMessage = null
                        try {
                            user.reload().await()
                            profileSnapshot = (Firebase.auth.currentUser ?: user).toAuthProfileSnapshot()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            statusMessage = refreshFailedMessage
                        } finally {
                            isRefreshing = false
                        }
                    }
                },
                onSignOut = {
                    statusMessage = null
                    signOutFromGoogle(
                        credentialManager = credentialManager,
                        scope = scope,
                        onSignOutComplete = {
                            profileSnapshot = null
                        }
                    )
                },
                modifier = modifier
            )
        }
    }
}

@Composable
private fun UserProfileLoadingCard(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.testTag("user_profile_loading"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.profile_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun UserProfileSignedOutCard(
    isSigningIn: Boolean,
    errorMessage: String?,
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag("user_profile_signed_out"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.AccountCircle,
                        contentDescription = stringResource(R.string.profile_signed_out_icon_description),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.profile_signed_out_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.profile_signed_out_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onSignInClick,
                enabled = !isSigningIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("user_profile_sign_in_button")
            ) {
                if (isSigningIn) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = stringResource(R.string.profile_signing_in))
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Login,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.profile_sign_in_google))
                }
            }
        }
    }
}

@Composable
fun UserProfileDetailsCard(
    profile: AuthProfileSnapshot,
    isRefreshing: Boolean,
    statusMessage: String?,
    onRefresh: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayName = profile.displayName ?: stringResource(R.string.profile_anonymous_name)
    val email = profile.email ?: stringResource(R.string.profile_no_email)
    val notAvailable = stringResource(R.string.profile_not_available)

    val dateFormatter = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
    val createdLabel = remember(profile.createdTimestampMs, notAvailable) {
        profile.createdTimestampMs?.let { dateFormatter.format(Date(it)) } ?: notAvailable
    }
    val lastSignInLabel = remember(profile.lastSignInTimestampMs, notAvailable) {
        profile.lastSignInTimestampMs?.let { dateFormatter.format(Date(it)) } ?: notAvailable
    }

    Card(
        modifier = modifier.testTag("user_profile_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ProfileAvatar(
                photoUrl = profile.photoUrl,
                displayName = displayName
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = displayName,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("user_profile_display_name")
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Email,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("user_profile_email")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = CircleShape,
                color = if (profile.emailVerified) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
                modifier = Modifier.testTag("user_profile_verified_badge")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (profile.emailVerified) Icons.Rounded.Verified else Icons.Outlined.VerifiedUser,
                        contentDescription = null,
                        tint = if (profile.emailVerified) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        },
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(
                            if (profile.emailVerified) R.string.profile_verified else R.string.profile_unverified
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (profile.emailVerified) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        }
                    )
                }
            }

            if (!statusMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(16.dp))

            ProfileMetadataRow(
                icon = Icons.Outlined.Fingerprint,
                label = stringResource(R.string.profile_label_uid),
                value = profile.uid,
                valueTag = "user_profile_uid"
            )
            Spacer(modifier = Modifier.height(12.dp))
            ProfileMetadataRow(
                icon = Icons.Outlined.VerifiedUser,
                label = stringResource(R.string.profile_label_providers),
                value = profile.providers.joinToString(", ")
            )
            if (!profile.phoneNumber.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                ProfileMetadataRow(
                    icon = Icons.Outlined.Phone,
                    label = stringResource(R.string.profile_label_phone),
                    value = profile.phoneNumber
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            ProfileMetadataRow(
                icon = Icons.Outlined.CalendarToday,
                label = stringResource(R.string.profile_label_created),
                value = createdLabel
            )
            Spacer(modifier = Modifier.height(12.dp))
            ProfileMetadataRow(
                icon = Icons.Outlined.Schedule,
                label = stringResource(R.string.profile_label_last_sign_in),
                value = lastSignInLabel
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onRefresh,
                    enabled = !isRefreshing,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("user_profile_refresh_button")
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(R.string.profile_refreshing), maxLines = 1)
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = stringResource(R.string.profile_refresh_button), maxLines = 1)
                    }
                }

                Button(
                    onClick = onSignOut,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("user_profile_sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(R.string.profile_sign_out_button), maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun ProfileAvatar(
    photoUrl: String?,
    displayName: String
) {
    val initials = remember(displayName) {
        displayName
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifBlank { "?" }
    }

    val avatarDescription = stringResource(R.string.profile_avatar_description)

    if (!photoUrl.isNullOrBlank()) {
        SubcomposeAsyncImage(
            model = photoUrl,
            contentDescription = avatarDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .testTag("user_profile_avatar"),
            loading = {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            },
            error = {
                InitialsAvatarContent(initials = initials)
            }
        )
    } else {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
                .size(84.dp)
                .testTag("user_profile_avatar")
        ) {
            InitialsAvatarContent(initials = initials)
        }
    }
}

@Composable
private fun InitialsAvatarContent(initials: String) {
    Box(contentAlignment = Alignment.Center) {
        Text(
            text = initials,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun ProfileMetadataRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueTag: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = if (valueTag != null) Modifier.testTag(valueTag) else Modifier
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UserProfileDetailsCardPreview() {
    AllenaMenteTheme {
        UserProfileDetailsCard(
            profile = AuthProfileSnapshot(
                uid = "uid_9f8a7b6c5d4e3f2a",
                displayName = "Elena Rossi",
                email = "elena.rossi@example.com",
                phoneNumber = null,
                photoUrl = null,
                emailVerified = true,
                providers = listOf("Google"),
                createdTimestampMs = 1735689600000L,
                lastSignInTimestampMs = 1764195600000L
            ),
            isRefreshing = false,
            statusMessage = null,
            onRefresh = {},
            onSignOut = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

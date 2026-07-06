@file:OptIn(ExperimentalMaterial3Api::class)

package ru.vertical.climbing.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import ru.vertical.climbing.app.navigation.ProfileScreenComponent
import ru.vertical.climbing.app.ui.DebugEnvPanel
import ru.vertical.climbing.app.ui.ErrorView
import ru.vertical.climbing.app.ui.LoadingView
import ru.vertical.climbing.app.ui.OfflineBanner
import ru.vertical.climbing.app.ui.format.formatBirthDateRu
import ru.vertical.climbing.app.ui.format.formatPhoneRu
import ru.vertical.climbing.app.ui.format.initialsFromFullName
import ru.vertical.climbing.domain.model.Client
import ru.vertical.climbing.domain.model.ProfileLoyaltyState
import ru.vertical.climbing.domain.model.buildProfileLoyaltyState
import ru.vertical.climbing.domain.usecase.ProfileData
import ru.vertical.climbing.presentation.Async
import ru.vertical.climbing.resources.Res
import ru.vertical.climbing.resources.action_cancel
import ru.vertical.climbing.resources.action_confirm
import ru.vertical.climbing.resources.profile_birth_date_label
import ru.vertical.climbing.resources.profile_loyal_badge
import ru.vertical.climbing.resources.profile_loyal_complete
import ru.vertical.climbing.resources.profile_loyal_discount
import ru.vertical.climbing.resources.profile_loyal_progress
import ru.vertical.climbing.resources.profile_loyal_remaining
import ru.vertical.climbing.resources.profile_logout
import ru.vertical.climbing.resources.profile_logout_confirm_message
import ru.vertical.climbing.resources.profile_logout_confirm_title
import ru.vertical.climbing.resources.profile_notification_settings
import ru.vertical.climbing.resources.profile_sanctions_late_cancel
import ru.vertical.climbing.resources.profile_sanctions_no_show
import ru.vertical.climbing.resources.profile_stale_banner
import ru.vertical.climbing.resources.title_profile

/** SCR-010 — профиль клиента и лояльность (FR-026, FR-027). */
@Composable
fun ProfileScreen(component: ProfileScreenComponent) {
    val model by component.model.subscribeAsState()

    if (model.showLogoutDialog) {
        AlertDialog(
            onDismissRequest = component::onLogoutDismissed,
            title = { Text(stringResource(Res.string.profile_logout_confirm_title)) },
            text = { Text(stringResource(Res.string.profile_logout_confirm_message)) },
            confirmButton = {
                TextButton(onClick = component::onLogoutConfirmed) {
                    Text(stringResource(Res.string.profile_logout))
                }
            },
            dismissButton = {
                TextButton(onClick = component::onLogoutDismissed) {
                    Text(stringResource(Res.string.action_cancel))
                }
            },
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(Res.string.title_profile)) }) },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = model.isRefreshing,
            onRefresh = component::onRefresh,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            when (val content = model.content) {
                Async.Idle, Async.Loading -> LoadingView()
                is Async.Error -> ErrorView(error = content.error, onRetry = component::onRetry)
                is Async.Content -> ProfileContent(
                    data = content.data,
                    debugEnv = model.debugEnv,
                    onNotificationSettingsClicked = component::onNotificationSettingsClicked,
                    onLogoutClicked = component::onLogoutClicked,
                    onDebugEnvironmentSelected = component::onDebugEnvironmentSelected,
                    onDebugMockToggled = component::onDebugMockToggled,
                )
                Async.Empty -> LoadingView()
            }
        }
    }
}

@Composable
private fun ProfileContent(
    data: ProfileData,
    debugEnv: ProfileScreenComponent.DebugEnvState?,
    onNotificationSettingsClicked: () -> Unit,
    onLogoutClicked: () -> Unit,
    onDebugEnvironmentSelected: (ru.vertical.climbing.data.remote.ApiEnvironment) -> Unit,
    onDebugMockToggled: (Boolean) -> Unit,
) {
    val client = data.client
    val loyalty = buildProfileLoyaltyState(client, data.visitsForLoyalty)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        if (data.isStale) {
            OfflineBanner()
            Text(
                text = stringResource(Res.string.profile_stale_banner),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        ProfileHeader(client = client)
        InfoRow(
            label = stringResource(Res.string.profile_birth_date_label),
            value = client.birthDate.formatBirthDateRu(),
        )
        LoyaltyBlock(loyalty = loyalty)
        SanctionsBlock(client = client)
        debugEnv?.let { debug ->
            DebugEnvPanel(
                environment = debug.environment,
                useMock = debug.useMock,
                onEnvironmentSelected = onDebugEnvironmentSelected,
                onMockToggled = onDebugMockToggled,
            )
        }
        ActionsBlock(
            onNotificationSettingsClicked = onNotificationSettingsClicked,
            onLogoutClicked = onLogoutClicked,
        )
    }
}

@Composable
private fun ProfileHeader(client: Client) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = initialsFromFullName(client.fullName),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Text(
            text = client.fullName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = formatPhoneRu(client.phone),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun LoyaltyBlock(loyalty: ProfileLoyaltyState) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (loyalty.showBadge) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        text = stringResource(Res.string.profile_loyal_badge),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            LinearProgressIndicator(
                progress = { loyalty.progress },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(
                    Res.string.profile_loyal_progress,
                    loyalty.completedVisits,
                    loyalty.visitsForLoyalty,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = if (loyalty.isLoyalClient) {
                    stringResource(Res.string.profile_loyal_complete)
                } else {
                    stringResource(Res.string.profile_loyal_remaining, loyalty.remainingVisits)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            loyalty.loyaltyDiscountPercent?.let { discount ->
                Text(
                    text = stringResource(Res.string.profile_loyal_discount, discount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun SanctionsBlock(client: Client) {
    if (client.lateCancellationCount == 0 && client.noShowCount == 0) return
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (client.lateCancellationCount > 0) {
                Text(
                    text = stringResource(Res.string.profile_sanctions_late_cancel, client.lateCancellationCount),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (client.noShowCount > 0) {
                Text(
                    text = stringResource(Res.string.profile_sanctions_no_show, client.noShowCount),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun ActionsBlock(
    onNotificationSettingsClicked: () -> Unit,
    onLogoutClicked: () -> Unit,
) {
    Column(modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)) {
        ActionRow(
            title = stringResource(Res.string.profile_notification_settings),
            onClick = onNotificationSettingsClicked,
            titleColor = MaterialTheme.colorScheme.onSurface,
        )
        ActionRow(
            title = stringResource(Res.string.profile_logout),
            onClick = onLogoutClicked,
            titleColor = MaterialTheme.colorScheme.error,
            showChevron = false,
        )
    }
}

@Composable
private fun ActionRow(
    title: String,
    onClick: () -> Unit,
    titleColor: androidx.compose.ui.graphics.Color,
    showChevron: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge, color = titleColor)
        if (showChevron) {
            androidx.compose.material3.Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

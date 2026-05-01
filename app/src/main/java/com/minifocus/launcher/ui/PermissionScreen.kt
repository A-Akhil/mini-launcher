/*
 * Minimalist Focus Launcher
 * Copyright (C) 2025 A-Akhil
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.minifocus.launcher.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import com.minifocus.launcher.permissions.PermissionsState
import androidx.compose.ui.res.stringResource
import com.minifocus.launcher.R

@Composable
fun PermissionScreen(
    state: PermissionsState,
    allowDismiss: Boolean = false,
    onRequestNotifications: () -> Unit,
    onRequestNotificationListener: () -> Unit,
    onRequestExactAlarms: () -> Unit,
    onRequestLockAccessibility: () -> Unit,
    onRequestUsageStats: () -> Unit,
    onRequestOverlay: () -> Unit,
    showRestrictedNotificationHint: Boolean,
    onOpenRestrictedSettings: () -> Unit,
    onClose: () -> Unit,
    onContinue: () -> Unit,
    onRefreshPermissions: () -> Unit = {}
) {
    var showAccessibilityDisclosure by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        onRefreshPermissions()
    }
    
    BackHandler(enabled = allowDismiss) {
        onClose()
    }

    if (showAccessibilityDisclosure) {
        AlertDialog(
            onDismissRequest = { showAccessibilityDisclosure = false },
            title = {
                Text(
                    text = stringResource(R.string.permissions_accessibility_title),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.permissions_accessibility_rationale),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAccessibilityDisclosure = false
                        onRequestLockAccessibility()
                    }
                ) {
                    Text(stringResource(R.string.permissions_go_to_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAccessibilityDisclosure = false }) {
                    Text(stringResource(R.string.permissions_not_now))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    val requiredPermissions = listOf(
        PermissionRequest(
            title = stringResource(R.string.permissions_post_notifications),
            description = stringResource(R.string.permissions_post_notifications_desc),
            granted = state.notificationsGranted,
            onRequest = onRequestNotifications
        )
    )

    val optionalPermissions = listOf(
        PermissionRequest(
            title = stringResource(R.string.permissions_exact_alarms),
            description = stringResource(R.string.permissions_exact_alarms_desc),
            granted = state.exactAlarmsGranted,
            onRequest = onRequestExactAlarms
        ),
        PermissionRequest(
            title = stringResource(R.string.permissions_notification_access),
            description = stringResource(R.string.permissions_notification_access_desc),
            granted = state.notificationListenerGranted,
            onRequest = onRequestNotificationListener
        ),
        PermissionRequest(
            title = stringResource(R.string.permissions_lock_screen),
            description = stringResource(R.string.permissions_lock_screen_desc),
            granted = state.lockAccessibilityGranted,
            onRequest = { showAccessibilityDisclosure = true }
        ),
        PermissionRequest(
            title = stringResource(R.string.permissions_usage_stats),
            description = stringResource(R.string.permissions_usage_stats_desc),
            granted = state.usageStatsGranted,
            onRequest = onRequestUsageStats
        ),
        PermissionRequest(
            title = stringResource(R.string.permissions_display_over_apps),
            description = stringResource(R.string.permissions_display_over_apps_desc),
            granted = state.overlayGranted,
            onRequest = onRequestOverlay
        )
    )

    val optionalPending = optionalPermissions.any { !it.granted }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.permissions_title),
            fontSize = 28.sp,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.permissions_subtitle),
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.label_required),
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        requiredPermissions.forEach { permission ->
            PermissionCard(
                title = permission.title,
                description = permission.description,
                granted = permission.granted,
                onClick = permission.onRequest
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.label_optional),
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        optionalPermissions.forEach { permission ->
            PermissionCard(
                title = permission.title,
                description = permission.description,
                granted = permission.granted,
                onClick = permission.onRequest
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (showRestrictedNotificationHint && !state.notificationListenerGranted) {
                Text(
                    text = stringResource(R.string.permissions_restricted_build),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onOpenRestrictedSettings,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.onBackground),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground)
            ) {
                Text(text = stringResource(R.string.permissions_open_app_info))
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        val statusMessage = when {
            !state.requiredGranted -> stringResource(R.string.permissions_required_to_continue)
            optionalPending -> stringResource(R.string.permissions_optional_unlock_features)
            else -> stringResource(R.string.permissions_all_granted_sentence)
        }

        Text(
            text = statusMessage,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onContinue,
            enabled = state.requiredGranted,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onBackground,
                contentColor = MaterialTheme.colorScheme.background
            )
        ) {
            Text(text = stringResource(R.string.action_continue))
        }
    }
}

private data class PermissionRequest(
    val title: String,
    val description: String,
    val granted: Boolean,
    val onRequest: () -> Unit
)

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    granted: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    lineHeight = 18.sp
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            if (granted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary
                )
            } else {
                OutlinedButton(
                    onClick = onClick,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.onBackground),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground)
                ) {
                    Text(text = stringResource(R.string.action_grant))
                }
            }
        }
    }
}

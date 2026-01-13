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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import com.minifocus.launcher.permissions.PermissionsState

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
    LaunchedEffect(Unit) {
        onRefreshPermissions()
    }
    
    BackHandler(enabled = allowDismiss) {
        onClose()
    }

    val requiredPermissions = listOf(
        PermissionRequest(
            title = "Post notifications",
            description = "Allow reminders and essential alerts to reach you.",
            granted = state.notificationsGranted,
            onRequest = onRequestNotifications
        )
    )

    val optionalPermissions = listOf(
        PermissionRequest(
            title = "Exact alarms & reminders",
            description = "Let reminders ring exactly on time, even in battery saver.",
            granted = state.exactAlarmsGranted,
            onRequest = onRequestExactAlarms
        ),
        PermissionRequest(
            title = "Notification access",
            description = "Enable the optional inbox that curates notifications.",
            granted = state.notificationListenerGranted,
            onRequest = onRequestNotificationListener
        ),
        PermissionRequest(
            title = "Lock screen shortcut",
            description = "Enable the accessibility service that powers double-tap lock without disabling biometrics.",
            granted = state.lockAccessibilityGranted,
            onRequest = onRequestLockAccessibility
        ),
        PermissionRequest(
            title = "Usage stats access",
            description = "Tracks launched apps so optional app locks stay enforced.",
            granted = state.usageStatsGranted,
            onRequest = onRequestUsageStats
        ),
        PermissionRequest(
            title = "Display over other apps",
            description = "Shows the lock overlay when you try opening locked apps.",
            granted = state.overlayGranted,
            onRequest = onRequestOverlay
        )
    )

    val optionalPending = optionalPermissions.any { !it.granted }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Permissions required",
            fontSize = 28.sp,
            fontWeight = FontWeight.Light,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "We need a few permissions to keep everything minimal and under your control.",
            fontSize = 16.sp,
            color = Color(0xFFAAAAAA),
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Required",
            fontSize = 18.sp,
            color = Color.White,
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
            text = "Optional",
            fontSize = 18.sp,
            color = Color.White,
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
                text = "This build is restricted by Android. Open App Info → three dots → Allow restricted settings before enabling notification access.",
                fontSize = 14.sp,
                color = Color(0xFF888888)
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onOpenRestrictedSettings,
                border = BorderStroke(1.5.dp, Color.White),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text(text = "Open App Info")
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        val statusMessage = when {
            !state.requiredGranted -> "Grant required permissions to continue."
            optionalPending -> "Optional permissions unlock the notification inbox and advanced locks."
            else -> "All permissions granted."
        }

        Text(
            text = statusMessage,
            fontSize = 16.sp,
            color = Color(0xFF888888)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onContinue,
            enabled = state.requiredGranted,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            )
        ) {
            Text(text = if (optionalPending) "Continue" else "Continue")
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
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
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = description,
                    color = Color(0xFF858585),
                    fontSize = 14.sp,
                    lineHeight = 18.sp
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            if (granted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50)
                )
            } else {
                OutlinedButton(
                    onClick = onClick,
                    border = BorderStroke(1.5.dp, Color.White),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text(text = "Grant")
                }
            }
        }
    }
}

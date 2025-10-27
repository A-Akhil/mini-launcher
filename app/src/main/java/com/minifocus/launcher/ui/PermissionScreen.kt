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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minifocus.launcher.permissions.PermissionsState

@Composable
fun PermissionScreen(
    state: PermissionsState,
    onRequestNotifications: () -> Unit,
    onRequestNotificationListener: () -> Unit,
    onRequestExactAlarms: () -> Unit,
    onRequestDeviceAdmin: () -> Unit,
    showRestrictedNotificationHint: Boolean,
    onOpenRestrictedSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
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

        PermissionCard(
            title = "Post notifications",
            description = "Allow reminders and essential alerts to reach you.",
            granted = state.notificationsGranted,
            onClick = onRequestNotifications
        )

        PermissionCard(
            title = "Notification access",
            description = "Let the launcher curate notifications for the inbox.",
            granted = state.notificationListenerGranted,
            onClick = onRequestNotificationListener
        )

        PermissionCard(
            title = "Exact alarms & reminders",
            description = "Allow reminders to ring exactly on time, even in battery saver.",
            granted = state.exactAlarmsGranted,
            onClick = onRequestExactAlarms
        )

        PermissionCard(
            title = "Device admin",
            description = "Required to lock the screen with future double-tap gesture.",
            granted = state.deviceAdminGranted,
            onClick = onRequestDeviceAdmin
        )

        Spacer(modifier = Modifier.weight(1f))

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

        Text(
            text = if (state.allGranted) "All set!" else "Grant every permission to proceed.",
            fontSize = 16.sp,
            color = Color(0xFF888888)
        )
    }
}

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

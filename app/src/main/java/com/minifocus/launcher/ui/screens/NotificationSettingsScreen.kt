package com.minifocus.launcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minifocus.launcher.ui.components.ScreenHeader

@Composable
fun NotificationSettingsScreen(
    notificationInboxEnabled: Boolean,
    notificationRetentionDays: Int,
    logRetentionDays: Int,
    onBack: () -> Unit,
    onOpenNotificationRetention: () -> Unit,
    onOpenLogRetention: () -> Unit,
    onOpenAppFilters: () -> Unit,
    onNotificationInboxToggle: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 36.dp)
    ) {
        ScreenHeader(
            title = "Notification Settings",
            onBack = onBack
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Notification inbox",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Capture notifications",
                    color = Color.White,
                    fontSize = 16.sp
                )
                Text(
                    text = "Store notifications in the in-app inbox and post a single summary alert.",
                    color = Color(0xFFAAAAAA),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Switch(
                checked = notificationInboxEnabled,
                onCheckedChange = onNotificationInboxToggle
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (notificationInboxEnabled) {
            SettingsRow(
                title = "Auto-clear notifications",
                value = "$notificationRetentionDays days",
                onClick = onOpenNotificationRetention
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsRow(
                title = "Log retention",
                value = "$logRetentionDays days",
                onClick = onOpenLogRetention
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsRow(
                title = "App filters",
                value = "",
                onClick = onOpenAppFilters
            )
        } else {
            Text(
                text = "Turn on the inbox to manage retention and filters.",
                color = Color(0xFFAAAAAA),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )
            if (value.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    color = Color(0xFFAAAAAA),
                    fontSize = 16.sp
                )
            }
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = Color(0xFFAAAAAA)
        )
    }
}

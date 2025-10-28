@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material.ExperimentalMaterialApi::class
)

package com.minifocus.launcher.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.rememberDismissState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.minifocus.launcher.model.NotificationItem
import com.minifocus.launcher.viewmodel.NotificationInboxViewModel.NotificationInboxUiState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun NotificationInboxScreen(
    state: NotificationInboxUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onOpenRetention: () -> Unit,
    onOpenLogRetention: () -> Unit,
    onOpenFilters: () -> Unit,
    onMarkAllRead: () -> Unit,
    onDelete: (Long) -> Unit,
    onUndoDelete: () -> Unit,
    onDismissUndo: () -> Unit
) {
    LaunchedEffect(state.lastDeleted?.id) {
        val deleted = state.lastDeleted
        if (deleted != null) {
            val result = snackbarHostState.showSnackbar(
                message = "Notification removed",
                actionLabel = "Undo"
            )
            if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                onUndoDelete()
            } else {
                onDismissUndo()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 24.dp, vertical = 36.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Notification Inbox",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (state.unreadCount > 0) {
                    Text(
                        text = "${state.unreadCount} unread",
                        color = Color(0xFFAAAAAA),
                        fontSize = 14.sp
                    )
                }
            }
            IconButton(onClick = onOpenFilters) {
                Icon(
                    imageVector = Icons.Filled.FilterList,
                    contentDescription = "Filters",
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RetentionChip(
                label = "Auto-clears in ${state.notificationRetentionDays} days",
                onClick = onOpenRetention
            )
            RetentionChip(
                label = "Logs kept ${state.logRetentionDays} days",
                onClick = onOpenLogRetention
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onMarkAllRead) {
                Icon(
                    imageVector = Icons.Filled.MarkEmailRead,
                    contentDescription = "Mark all read",
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (state.notifications.isEmpty()) {
            EmptyInboxState(onOpenFilters)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.notifications, key = NotificationItem::id) { item ->
                    NotificationRow(
                        item = item,
                        onDelete = { onDelete(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(
    item: NotificationItem,
    onDelete: () -> Unit
) {
    val dismissState = rememberDismissState(
        confirmStateChange = { value ->
            if (value == DismissValue.DismissedToEnd || value == DismissValue.DismissedToStart) {
                onDelete()
            }
            true
        }
    )

    SwipeToDismiss(
        state = dismissState,
        background = {
            val color = when (dismissState.targetValue) {
                DismissValue.DismissedToEnd, DismissValue.DismissedToStart -> Color(0xFF880000)
                else -> Color(0xFF222222)
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteSweep,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        },
        dismissContent = {
            NotificationCard(item = item)
        },
        directions = setOf(DismissDirection.EndToStart, DismissDirection.StartToEnd)
    )
}

@Composable
private fun NotificationCard(
    item: NotificationItem
) {
    val formattedTimestamp = remember(item.timestamp) {
        formatTimestamp(item.timestamp)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(Color(0xFF111111))
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.appName,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = formattedTimestamp,
                    color = Color(0xFF777777),
                    fontSize = 12.sp
                )
            }
        }
        if (!item.title.isNullOrBlank()) {
            Text(
                text = item.title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (!item.text.isNullOrBlank()) {
            Text(
                text = item.text,
                color = Color(0xFFCCCCCC),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun RetentionChip(label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0x22FFFFFF))
    ) {
        Text(text = label, fontSize = 12.sp)
    }
}

@Composable
private fun EmptyInboxState(onOpenFilters: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No notifications yet",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Notifications you intercept will show up here.",
            color = Color(0xFF777777),
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
        TextButton(onClick = onOpenFilters, modifier = Modifier.padding(top = 24.dp)) {
            Text("Adjust filters", color = Color.White)
        }
    }
}

private val timestampFormatter = DateTimeFormatter.ofPattern("EEE, MMM d • h:mm a", Locale.getDefault())

private fun formatTimestamp(timestamp: Long): String {
    return Instant.ofEpochMilli(timestamp)
    .atZone(ZoneId.systemDefault())
        .format(timestampFormatter)
}

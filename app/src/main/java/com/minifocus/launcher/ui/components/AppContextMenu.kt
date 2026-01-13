package com.minifocus.launcher.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.minifocus.launcher.model.AppEntry

data class AppMenuAction(
    val icon: ImageVector,
    val label: String,
    val color: Color = Color.White,
    val action: () -> Unit
)

@Composable
fun AppContextMenu(
    app: AppEntry,
    onDismiss: () -> Unit,
    onPin: (() -> Unit)? = null,
    onUnpin: (() -> Unit)? = null,
    onHide: (() -> Unit)? = null,
    onUnhide: (() -> Unit)? = null,
    onLock: (() -> Unit)? = null
) {
    val context = LocalContext.current
    
    val actions = buildList {
        // Pin/Unpin
        if (onPin != null) {
            add(AppMenuAction(
                icon = Icons.Filled.PushPin,
                label = "Pin to Home",
                action = {
                    onPin()
                    onDismiss()
                }
            ))
        }
        if (onUnpin != null) {
            add(AppMenuAction(
                icon = Icons.Filled.PushPin,
                label = "Unpin",
                action = {
                    onUnpin()
                    onDismiss()
                }
            ))
        }
        
        // Hide/Unhide
        if (onHide != null) {
            add(AppMenuAction(
                icon = Icons.Filled.VisibilityOff,
                label = "Hide App",
                action = {
                    onHide()
                    onDismiss()
                }
            ))
        }
        if (onUnhide != null) {
            add(AppMenuAction(
                icon = Icons.Filled.Visibility,
                label = "Unhide",
                action = {
                    onUnhide()
                    onDismiss()
                }
            ))
        }
        
        // Lock
        if (onLock != null) {
            add(AppMenuAction(
                icon = Icons.Filled.Lock,
                label = "Lock App",
                action = {
                    onLock()
                    onDismiss()
                }
            ))
        }
        
        // App Info
        add(AppMenuAction(
            icon = Icons.Filled.Info,
            label = "App Info",
            action = {
                openAppInfo(context, app.packageName)
                onDismiss()
            }
        ))
        
        // Uninstall
        add(AppMenuAction(
            icon = Icons.Filled.Delete,
            label = "Uninstall",
            color = Color(0xFFFF6B6B),
            action = {
                uninstallApp(context, app.packageName)
                onDismiss()
            }
        ))
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        containerColor = Color.Transparent,
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1A1A1A))
                    .padding(vertical = 16.dp)
            ) {
                // App name header
                Text(
                    text = app.label,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Menu items
                actions.forEach { menuAction ->
                    AppMenuItem(
                        label = menuAction.label,
                        color = menuAction.color,
                        onClick = menuAction.action
                    )
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun AppMenuItem(
    label: String,
    color: Color = Color.White,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

private fun openAppInfo(context: Context, packageName: String) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

private fun uninstallApp(context: Context, packageName: String) {
    try {
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // If uninstall fails (system app, etc.), try opening app info instead
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (ex: Exception) {
            // Silently fail if both attempts fail
        }
    }
}

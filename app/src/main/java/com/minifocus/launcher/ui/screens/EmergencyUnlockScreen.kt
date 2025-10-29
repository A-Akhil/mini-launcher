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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minifocus.launcher.manager.LockManager
import com.minifocus.launcher.ui.components.ScreenHeader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EmergencyUnlockScreen(
    lockManager: LockManager,
    onBack: () -> Unit,
    onUnlockApp: suspend (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val allLocks by lockManager.observeLocks().collectAsState(initial = emptyList())
    
    // Filter to only show currently locked apps (not expired)
    val lockedApps = remember(allLocks) {
        val now = System.currentTimeMillis()
        allLocks.filter { it.lockedUntil > now }
            .map { it.packageName to it.lockedUntil }
    }
    
    var unlockingApp by remember { mutableStateOf<String?>(null) }
    var countdown by remember { mutableStateOf(60) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var confirmUnlockPackage by remember { mutableStateOf<String?>(null) }

    // Countdown timer for unlocking
    LaunchedEffect(unlockingApp, countdown) {
        if (unlockingApp != null && countdown > 0) {
            delay(1000)
            countdown -= 1
        } else if (unlockingApp != null && countdown == 0) {
            // Show confirmation dialog
            confirmUnlockPackage = unlockingApp
            showConfirmDialog = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (unlockingApp != null) {
            // Loading overlay
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Text(
                        text = "Unlocking",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Light,
                        color = Color.White
                    )
                    
                    Text(
                        text = unlockingApp ?: "",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Light,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    CircularProgressIndicator(
                        modifier = Modifier.size(80.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "$countdown",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Light,
                        color = Color.White
                    )
                    
                    Text(
                        text = "seconds remaining",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            // Locked apps list
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 36.dp)
            ) {
                ScreenHeader(
                    title = "Emergency Unlock",
                    onBack = onBack
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (lockedApps.isEmpty()) {
                    Text(
                        text = "No locked apps",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Light
                    )
                } else {
                    lockedApps.forEach { (packageName, unlockTime) ->
                        LockedAppItem(
                            packageName = packageName,
                            unlockTime = unlockTime,
                            onUnlock = {
                                unlockingApp = packageName
                                countdown = 60
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }

        // Confirmation dialog
        if (showConfirmDialog && confirmUnlockPackage != null) {
            AlertDialog(
                onDismissRequest = { },
                title = {
                    Text(
                        text = "Do you really wanna stop?",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Light
                    )
                },
                text = {
                    Text(
                        text = "This will unlock $confirmUnlockPackage",
                        fontSize = 16.sp
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val pkg = confirmUnlockPackage
                            if (pkg != null) {
                                scope.launch {
                                    onUnlockApp(pkg)
                                }
                            }
                            showConfirmDialog = false
                            confirmUnlockPackage = null
                            unlockingApp = null
                            countdown = 60
                        }
                    ) {
                        Text("Yes", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showConfirmDialog = false
                            confirmUnlockPackage = null
                            unlockingApp = null
                            countdown = 60
                        }
                    ) {
                        Text("No", color = Color.White)
                    }
                },
                containerColor = Color(0xFF1A1A1A),
                titleContentColor = Color.White,
                textContentColor = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun LockedAppItem(
    packageName: String,
    unlockTime: Long,
    onUnlock: () -> Unit
) {
    val unlockTimeText = remember(unlockTime) {
        SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(unlockTime))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = packageName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Light,
                color = Color.White
            )
            Text(
                text = "Until $unlockTimeText",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }

        Button(
            onClick = onUnlock,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            )
        ) {
            Text(
                text = "Unlock",
                fontSize = 14.sp,
                fontWeight = FontWeight.Light
            )
        }
    }
}

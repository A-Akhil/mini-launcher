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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.minifocus.launcher.manager.LockManager
import com.minifocus.launcher.ui.components.ScreenHeader
import com.minifocus.launcher.R
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
    
    val lockedApps = remember(allLocks) {
        val now = System.currentTimeMillis()
        allLocks.filter { it.lockedUntil > now }
            .map { it.packageName to it.lockedUntil }
    }
    
    var unlockingApp by remember { mutableStateOf<String?>(null) }
    var countdown by remember { mutableStateOf(60) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var confirmUnlockPackage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(unlockingApp, countdown) {
        if (unlockingApp != null && countdown > 0) {
            delay(1000)
            countdown -= 1
        } else if (unlockingApp != null && countdown == 0) {
            confirmUnlockPackage = unlockingApp
            showConfirmDialog = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (unlockingApp != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Text(
                        text = stringResource(R.string.emergency_unlock_unlocking),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    Text(
                        text = unlockingApp ?: "",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    CircularProgressIndicator(
                        modifier = Modifier.size(80.dp),
                        color = MaterialTheme.colorScheme.onBackground,
                        strokeWidth = 2.dp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "$countdown",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    Text(
                        text = stringResource(R.string.seconds_remaining_format, countdown),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 36.dp)
            ) {
                ScreenHeader(
                    title = stringResource(R.string.emergency_unlock_title),
                    onBack = onBack
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (lockedApps.isEmpty()) {
                    Text(
                        text = stringResource(R.string.emergency_unlock_no_locked_apps),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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

        if (showConfirmDialog && confirmUnlockPackage != null) {
                AlertDialog(
                onDismissRequest = { },
                title = {
                    Text(
                        text = stringResource(R.string.emergency_unlock_confirm_stop),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Light
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.emergency_unlock_confirm_message, confirmUnlockPackage ?: ""),
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
                        Text(stringResource(R.string.action_yes), color = MaterialTheme.colorScheme.onBackground)
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
                        Text(stringResource(R.string.action_no), color = MaterialTheme.colorScheme.onBackground)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
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
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.emergency_unlock_until, unlockTimeText),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Button(
            onClick = onUnlock,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onBackground,
                contentColor = MaterialTheme.colorScheme.background
            )
        ) {
            Text(
                text = stringResource(R.string.emergency_unlock_action),
                fontSize = 14.sp,
                fontWeight = FontWeight.Light
            )
        }
    }
}

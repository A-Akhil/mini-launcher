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

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.minifocus.launcher.model.AppEntry
import com.minifocus.launcher.model.ExpiryAction
import com.minifocus.launcher.R
import kotlinx.coroutines.delay
import java.util.Calendar

/**
 * Dialog shown before launching a tracked app (time intention) or
 * after the timer expires (time-expired prompt with cooldown).
 *
 * @param app             The app being launched or whose time has expired.
 * @param defaultDurationMinutes  Previously chosen duration (null if first time).
 * @param defaultExpiryAction     Previously chosen expiry action.
 * @param isTimeExpired   True when this dialog is shown because the timer expired.
 * @param cooldownEndTime Epoch millis when the cooldown timer ends (0 = no cooldown).
 * @param onConfirm       Called with the chosen duration and expiry action.
 * @param onDismiss       Called when the user dismisses the dialog.
 */
@Composable
fun TimeIntentionDialog(
    app: AppEntry,
    defaultDurationMinutes: Int?,
    defaultExpiryAction: ExpiryAction,
    isTimeExpired: Boolean = false,
    cooldownEndTime: Long = 0L,
    onConfirm: (durationMinutes: Int, expiryAction: ExpiryAction) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var customMinutes by remember { mutableStateOf(defaultDurationMinutes?.toString() ?: "") }
    var selectedAction by remember { mutableStateOf(defaultExpiryAction) }
    var todayUsageMinutes by remember { mutableLongStateOf(0L) }
    var weekUsageMinutes by remember { mutableLongStateOf(0L) }

    // Cooldown state
    var cooldownProgress by remember { mutableFloatStateOf(1f) }
    var cooldownSecondsLeft by remember { mutableStateOf(0) }
    val isInCooldown = isTimeExpired && cooldownSecondsLeft > 0

    // Keep this as one-shot on dialog open (optimized); no periodic polling.
    LaunchedEffect(app.packageName) {
        val stats = queryUsageStats(context, app.packageName)
        todayUsageMinutes = stats.first
        weekUsageMinutes = stats.second
    }

    // Cooldown countdown
    LaunchedEffect(cooldownEndTime) {
        if (cooldownEndTime <= 0L) {
            cooldownProgress = 0f
            cooldownSecondsLeft = 0
            return@LaunchedEffect
        }
        val totalMs = COOLDOWN_DURATION_MS.toFloat()
        while (true) {
            val remaining = cooldownEndTime - System.currentTimeMillis()
            if (remaining <= 0) {
                cooldownProgress = 0f
                cooldownSecondsLeft = 0
                break
            }
            cooldownProgress = (remaining / totalMs).coerceIn(0f, 1f)
            cooldownSecondsLeft = ((remaining / 1000) + 1).toInt().coerceAtLeast(1)
            delay(100L)
        }
    }

    val presets = listOf(5, 10, 15, 30)

    // Scrim background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.85f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { /* consume taps on scrim */ },
        contentAlignment = Alignment.Center
    ) {
        // Dialog card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: app name and subtitle
            Text(
                text = app.label,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isTimeExpired) stringResource(R.string.time_intention_time_ended) else stringResource(R.string.time_intention_how_long),
                color = if (isTimeExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            // Cooldown timer (only in time-expired mode with active cooldown)
            if (isTimeExpired) {
                Spacer(modifier = Modifier.height(20.dp))
                CooldownTimer(
                    progress = cooldownProgress,
                    secondsLeft = cooldownSecondsLeft,
                    isActive = isInCooldown
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Prominent usage stats reminder
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Today stat
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.time_intention_today),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatUsageTime(todayUsageMinutes),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Vertical divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(48.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )

                // 7-day stat
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.time_intention_past_7_days),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatUsageTime(weekUsageMinutes),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Duration presets -- 2x2 grid
            Text(
                text = if (isTimeExpired) stringResource(R.string.time_intention_extend_by) else stringResource(R.string.time_intention_duration),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.5.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            val selectedPreset = customMinutes.toIntOrNull()
            val presetPairs = presets.chunked(2)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presetPairs.forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pair.forEach { minutes ->
                            DurationChip(
                                label = stringResource(R.string.label_minutes_format, minutes),
                                selected = selectedPreset == minutes,
                                enabled = !isInCooldown,
                                modifier = Modifier.weight(1f)
                            ) {
                                onConfirm(minutes, selectedAction)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Custom minutes input -- field only, no inline Start button
            OutlinedTextField(
                value = customMinutes,
                onValueChange = { value ->
                    if (value.all { it.isDigit() } && value.length <= 3) {
                        customMinutes = value
                    }
                },
                enabled = !isInCooldown,
                placeholder = {
                    Text(stringResource(R.string.time_intention_custom_minutes), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (!isInCooldown) {
                            val mins = customMinutes.toIntOrNull()
                            if (mins != null && mins > 0) {
                                onConfirm(mins, selectedAction)
                            }
                        }
                    }
                ),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedBorderColor = MaterialTheme.colorScheme.outline,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    cursorColor = MaterialTheme.colorScheme.onBackground
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Expiry action chips (hidden during time-expired mode)
            if (!isTimeExpired) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.time_intention_on_expire),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ExpiryAction.entries.forEach { action ->
                        val label = when (action) {
                            ExpiryAction.NOTIFICATION -> stringResource(R.string.time_intention_notify)
                            ExpiryAction.PROMPT -> stringResource(R.string.time_intention_ask_me)
                            ExpiryAction.RETURN_HOME -> stringResource(R.string.time_intention_go_home)
                        }
                        val isSelected = selectedAction == action
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceContainer
                                )
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedAction = action }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.surfaceDim)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom action row: Go back (left) + Start (right)
            val isCustomValid = !isInCooldown && customMinutes.isNotBlank() &&
                (customMinutes.toIntOrNull() ?: 0) > 0
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Go back
                Text(
                    text = if (isTimeExpired) stringResource(R.string.action_go_back_home) else stringResource(R.string.action_go_back),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(vertical = 6.dp, horizontal = 2.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                // Start (only active when custom input has a valid value)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isCustomValid) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceContainer
                        )
                        .then(
                            if (isCustomValid) Modifier.clickable {
                                val mins = customMinutes.toIntOrNull()
                                if (mins != null && mins > 0) onConfirm(mins, selectedAction)
                            } else Modifier
                        )
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.action_start),
                        color = if (isCustomValid) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun DurationChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    !enabled -> MaterialTheme.colorScheme.surfaceContainer
                    selected -> MaterialTheme.colorScheme.surfaceVariant
                    else -> MaterialTheme.colorScheme.surfaceDim
                }
            )
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = when {
                !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                selected -> MaterialTheme.colorScheme.onBackground
                else -> MaterialTheme.colorScheme.onSurface
            },
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

/**
 * Circular countdown timer displayed when the time-expired dialog is shown.
 * Shows a shrinking arc and the number of seconds remaining.
 * When the cooldown finishes, displays a checkmark-style indicator.
 */
@Composable
private fun CooldownTimer(
    progress: Float,
    secondsLeft: Int,
    isActive: Boolean
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(72.dp)
    ) {
        val trackColor = MaterialTheme.colorScheme.surfaceVariant
        val arcColor = if (isActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
        Canvas(modifier = Modifier.size(72.dp)) {
            val strokeWidth = 5.dp.toPx()
            val padding = strokeWidth / 2f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(padding, padding)

            // Track
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Progress arc
            drawArc(
                color = arcColor,
                startAngle = -90f,
                sweepAngle = progress * 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Text(
            text = if (isActive) "$secondsLeft" else stringResource(R.string.action_ok),
            color = if (isActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
            fontSize = if (isActive) 20.sp else 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

private fun queryUsageStats(context: Context, packageName: String): Pair<Long, Long> {
    return try {
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return 0L to 0L

        val now = System.currentTimeMillis()

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStart = calendar.timeInMillis
        val past7DaysStartCal = Calendar.getInstance().apply {
            timeInMillis = todayStart
            add(Calendar.DAY_OF_YEAR, -7)
        }
        val past7DaysStart = past7DaysStartCal.timeInMillis

        // Notes for future maintenance (attempt history):
        // 1) Tried simple queryUsageStats(INTERVAL_DAILY/WEEKLY) with rolling windows.
        //    Result: inconsistent totals across devices (boundary expansion/interval mismatch).
        // 2) Tried calendar-bounded weekly windows + lastTimeUsed filtering.
        //    Result: still leaked/shifted buckets on some OEM builds.
        // 3) Tried pure event pairing (ACTIVITY_RESUMED -> PAUSED/STOPPED).
        //    Result: often closest for TODAY, but can undercount in some app flows.
        // 4) Tried blending TODAY with max(events, aggregate).
        //    Result: severe overcount on devices where daily aggregate bucket is not midnight-aligned.
        // 5) Tried per-day summation for previous 7 days (7 separate windows).
        //    Result: duplicate/expanded totals on some devices.
        // 6) Current stable compromise:
        //    - TODAY: event-based foreground sessions only.
        //    - PAST 7 DAYS: one bounded aggregate query over previous 7 full days,
        //      ending at (todayStart - 1) to strictly exclude today.

        // Today: use event pairing only.
        // Aggregate buckets can be OEM-shifted (not midnight-aligned) and can overcount today.
        val todayEventMinutes = queryForegroundMinutesFromEvents(
            usageStatsManager = usageStatsManager,
            packageName = packageName,
            startTime = todayStart,
            endTime = now
        )

        // Past 7 Days: one exact bounded aggregate range for previous 7 full days only.
        // End at todayStart - 1 to avoid any overlap with today's bucket.
        val past7DaysAggregate =
            usageStatsManager.queryAndAggregateUsageStats(past7DaysStart, todayStart - 1L)
        val past7DaysMinutes = (past7DaysAggregate[packageName]?.totalTimeInForeground ?: 0L) / 60_000L

        todayEventMinutes to past7DaysMinutes
    } catch (_: Exception) {
        0L to 0L
    }
}

private fun queryForegroundMinutesFromEvents(
    usageStatsManager: UsageStatsManager,
    packageName: String,
    startTime: Long,
    endTime: Long
): Long {
    // Event-stream strategy details:
    // - Track the target app's active foreground session start.
    // - Close session when target app pauses/stops.
    // - Also close session when any other package resumes, which protects against
    //   intra-app overlays and activity handoff edge cases.
    val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
    val event = UsageEvents.Event()

    var totalForegroundMs = 0L
    var activeStartMs: Long? = null
    var isTargetForeground = false

    fun closeTargetSession(at: Long) {
        val start = activeStartMs
        if (isTargetForeground && start != null && at > start) {
            totalForegroundMs += (at - start)
        }
        activeStartMs = null
        isTargetForeground = false
    }

    while (usageEvents.hasNextEvent()) {
        usageEvents.getNextEvent(event)
        when (event.eventType) {
            UsageEvents.Event.ACTIVITY_RESUMED -> {
                if (event.packageName == packageName) {
                    if (!isTargetForeground) {
                        activeStartMs = event.timeStamp
                        isTargetForeground = true
                    }
                } else {
                    closeTargetSession(event.timeStamp)
                }
            }

            UsageEvents.Event.ACTIVITY_PAUSED,
            UsageEvents.Event.ACTIVITY_STOPPED -> {
                if (event.packageName == packageName) {
                    closeTargetSession(event.timeStamp)
                }
            }
        }
    }

    val ongoingStart = activeStartMs
    if (isTargetForeground && ongoingStart != null && endTime > ongoingStart) {
        totalForegroundMs += (endTime - ongoingStart)
    }

    return totalForegroundMs / 60_000L
}

private fun formatUsageTime(minutes: Long): String {
    return when {
        minutes < 1 -> "< 1 min"
        minutes < 60 -> "$minutes min"
        else -> {
            val hours = minutes / 60
            val mins = minutes % 60
            if (mins == 0L) "${hours}h" else "${hours}h ${mins}m"
        }
    }
}

private const val COOLDOWN_DURATION_MS = 15_000L

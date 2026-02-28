package com.minifocus.launcher.ui

import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minifocus.launcher.model.AppEntry
import com.minifocus.launcher.model.ExpiryAction
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
    var expiryExpanded by remember { mutableStateOf(false) }

    // Cooldown state
    var cooldownProgress by remember { mutableFloatStateOf(1f) }
    var cooldownSecondsLeft by remember { mutableStateOf(0) }
    val isInCooldown = isTimeExpired && cooldownSecondsLeft > 0

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

    val expiryLabel = when (selectedAction) {
        ExpiryAction.NOTIFICATION -> "Notification"
        ExpiryAction.PROMPT -> "Screen prompt"
        ExpiryAction.RETURN_HOME -> "Return to home"
    }

    // Scrim background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
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
                .background(Color(0xFF141414))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: app name and subtitle
            Text(
                text = app.label,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isTimeExpired) "Your time has ended" else "Set your time intention",
                color = if (isTimeExpired) Color(0xFFCC6666) else Color(0xFF777777),
                fontSize = 14.sp
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

            Spacer(modifier = Modifier.height(20.dp))

            // Usage stats cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                UsageStatCard(
                    label = "Today",
                    value = formatUsageTime(todayUsageMinutes),
                    modifier = Modifier.weight(1f)
                )
                UsageStatCard(
                    label = "Past 7 days",
                    value = formatUsageTime(weekUsageMinutes),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Duration presets
            Text(
                text = if (isTimeExpired) "Extend by" else "Quick pick",
                color = Color(0xFF666666),
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { minutes ->
                    val enabled = !isInCooldown
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (enabled) Color(0xFF1E1E1E) else Color(0xFF111111)
                            )
                            .then(
                                if (enabled) Modifier.clickable {
                                    onConfirm(minutes, selectedAction)
                                } else Modifier
                            )
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${minutes}m",
                            color = if (enabled) Color.White else Color(0xFF333333),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Custom input row
            Text(
                text = "Or set custom minutes",
                color = Color(0xFF666666),
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = customMinutes,
                    onValueChange = { value ->
                        if (value.all { it.isDigit() } && value.length <= 3) {
                            customMinutes = value
                        }
                    },
                    enabled = !isInCooldown,
                    placeholder = {
                        Text("Minutes", color = Color(0xFF444444), fontSize = 14.sp)
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            if (!isInCooldown) {
                                val mins = customMinutes.toIntOrNull()
                                if (mins != null && mins > 0) {
                                    onConfirm(mins, selectedAction)
                                }
                            }
                        }
                    ),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1E1E1E),
                        unfocusedContainerColor = Color(0xFF1E1E1E),
                        disabledContainerColor = Color(0xFF111111),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        disabledTextColor = Color(0xFF333333),
                        cursorColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )

                val isValid = !isInCooldown && customMinutes.isNotBlank() &&
                    (customMinutes.toIntOrNull() ?: 0) > 0
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isValid) {
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF2A2A2A), Color(0xFF3A3A3A))
                                )
                            } else {
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF111111), Color(0xFF111111))
                                )
                            }
                        )
                        .then(
                            if (isValid) Modifier.clickable {
                                val mins = customMinutes.toIntOrNull()
                                if (mins != null && mins > 0) {
                                    onConfirm(mins, selectedAction)
                                }
                            } else Modifier
                        )
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Start",
                        color = if (isValid) Color.White else Color(0xFF333333),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Expiry action selector (hidden during time-expired mode)
            if (!isTimeExpired) {
                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F0F0F))
                        .clickable { expiryExpanded = !expiryExpanded }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "After time expires",
                            color = Color(0xFF555555),
                            fontSize = 11.sp
                        )
                        Text(
                            text = expiryLabel,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Icon(
                        imageVector = if (expiryExpanded) Icons.Filled.KeyboardArrowUp
                        else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (expiryExpanded) "Collapse" else "Expand",
                        tint = Color(0xFF555555),
                        modifier = Modifier.size(22.dp)
                    )
                }

                AnimatedVisibility(
                    visible = expiryExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        ExpiryAction.entries.forEach { action ->
                            if (action != selectedAction) {
                                val label = when (action) {
                                    ExpiryAction.NOTIFICATION -> "Notification"
                                    ExpiryAction.PROMPT -> "Screen prompt"
                                    ExpiryAction.RETURN_HOME -> "Return to home"
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            selectedAction = action
                                            expiryExpanded = false
                                        }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color(0xFF444444))
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = label,
                                        color = Color(0xFFAAAAAA),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Dismiss / go back (always clickable)
            Text(
                text = if (isTimeExpired) "Go back home" else "Go back",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable { onDismiss() }
                    .padding(6.dp)
            )
        }
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
        val trackColor = Color(0xFF222222)
        val arcColor = if (isActive) Color(0xFFCC6666) else Color(0xFF44AA44)
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
            text = if (isActive) "$secondsLeft" else "OK",
            color = if (isActive) Color(0xFFCC6666) else Color(0xFF44AA44),
            fontSize = if (isActive) 20.sp else 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun UsageStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F0F0F))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = label,
            color = Color(0xFF555555),
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
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
        val todayStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, todayStart, now
        )
        val todayMinutes = todayStats
            .filter { it.packageName == packageName }
            .sumOf { it.totalTimeInForeground } / 60_000L

        val weekStart = now - 7 * 24 * 60 * 60 * 1000L
        val weekStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_WEEKLY, weekStart, now
        )
        val weekMinutes = weekStats
            .filter { it.packageName == packageName }
            .sumOf { it.totalTimeInForeground } / 60_000L

        todayMinutes to weekMinutes
    } catch (_: Exception) {
        0L to 0L
    }
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

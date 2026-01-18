package com.minifocus.launcher.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ClockSecondsPreview(showSeconds: Boolean) {
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = System.currentTimeMillis()
        }
    }
    
    val timeFormat = if (showSeconds) "HH:mm:ss" else "HH:mm"
    val formatter = remember(timeFormat) { SimpleDateFormat(timeFormat, Locale.getDefault()) }
    val timeText = formatter.format(Date(currentTime))
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(targetState = timeText, label = "clock") { time ->
            Text(
                text = time,
                fontSize = 56.sp,
                fontWeight = FontWeight.Light,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SmartSuggestionsPreview(enabled: Boolean) {
    val mockApps = listOf("WhatsApp", "Chrome", "YouTube", "Camera", "Gallery", "Maps", "Settings")
    val mockSuggestions = listOf("WhatsApp", "Chrome", "YouTube")
    
    var showAnimation by remember { mutableStateOf(false) }
    
    LaunchedEffect(enabled) {
        showAnimation = false
        delay(100)
        showAnimation = true
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AnimatedVisibility(
                visible = enabled && showAnimation,
                enter = slideInVertically(initialOffsetY = { -it }),
                exit = slideOutVertically(targetOffsetY = { -it })
            ) {
                Column {
                    Text(
                        text = "Frequently opened",
                        fontSize = 12.sp,
                        color = Color(0xFF888888),
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                    )
                    mockSuggestions.forEach { app ->
                        AppListItem(app, highlighted = true)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "All apps",
                        fontSize = 12.sp,
                        color = Color(0xFF888888),
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                    )
                }
            }
            
            val displayApps = if (enabled) mockApps.drop(3) else mockApps
            displayApps.forEach { app ->
                AppListItem(app, highlighted = false)
            }
        }
    }
}

@Composable
private fun AppListItem(name: String, highlighted: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (highlighted) 1.0f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )
    
    Text(
        text = name,
        fontSize = 16.sp,
        color = if (highlighted) Color.White else Color(0xFFCCCCCC),
        fontWeight = if (highlighted) FontWeight.Medium else FontWeight.Light,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .scale(scale)
    )
}

@Composable
fun KeyboardOnSwipePreview(enabled: Boolean) {
    var drawerVisible by remember { mutableStateOf(false) }
    var keyboardVisible by remember { mutableStateOf(false) }
    var searchFieldVisible by remember { mutableStateOf(false) }
    
    val drawerOffset by animateDpAsState(
        targetValue = if (drawerVisible) 0.dp else 300.dp,
        animationSpec = tween(400),
        label = "drawer"
    )
    
    val keyboardOffset by animateDpAsState(
        targetValue = if (keyboardVisible) 0.dp else 150.dp,
        animationSpec = tween(300),
        label = "keyboard"
    )
    
    LaunchedEffect(enabled) {
        while (true) {
            delay(1500)
            drawerVisible = true
            delay(300)
            if (enabled) {
                searchFieldVisible = true
                delay(200)
                keyboardVisible = true
            }
            delay(2500)
            keyboardVisible = false
            delay(200)
            searchFieldVisible = false
            drawerVisible = false
            delay(1000)
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(Color.Black)
    ) {
        // Home screen with instruction
        if (!drawerVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "10:45",
                        fontSize = 36.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Light
                    )
                    Spacer(modifier = Modifier.height(40.dp))
                    Text(
                        text = "Swipe up →",
                        fontSize = 14.sp,
                        color = Color(0xFF666666),
                        fontWeight = FontWeight.Light
                    )
                }
            }
        }
        
        // App Drawer sliding up
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .offset(y = drawerOffset)
                .background(Color.Black)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Search field (only visible with keyboard enabled)
                AnimatedVisibility(
                    visible = searchFieldVisible && enabled,
                    enter = slideInVertically(initialOffsetY = { -it / 2 }),
                    exit = slideOutVertically(targetOffsetY = { -it / 2 })
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(Color(0xFF1A1A1A), RoundedCornerShape(8.dp))
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = if (keyboardVisible) "wh|" else "Search apps...",
                            fontSize = 16.sp,
                            color = if (keyboardVisible) Color.White else Color(0xFF666666),
                            fontWeight = FontWeight.Light
                        )
                    }
                }
                
                // App list
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "WhatsApp",
                    fontSize = 16.sp,
                    color = if (keyboardVisible && enabled) Color.White else Color(0xFFCCCCCC),
                    fontWeight = if (keyboardVisible && enabled) FontWeight.Medium else FontWeight.Light,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
                Text(
                    text = "Chrome",
                    fontSize = 16.sp,
                    color = Color(0xFFCCCCCC),
                    fontWeight = FontWeight.Light,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
                Text(
                    text = "YouTube",
                    fontSize = 16.sp,
                    color = Color(0xFFCCCCCC),
                    fontWeight = FontWeight.Light,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        
        // Keyboard sliding up from bottom
        if (enabled) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .offset(y = keyboardOffset)
                    .align(Alignment.BottomCenter)
                    .background(Color(0xFF2A2A2A))
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Keyboard Ready",
                        fontSize = 12.sp,
                        color = Color(0xFF888888),
                        fontWeight = FontWeight.Light
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        repeat(9) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                                    .background(Color(0xFF444444), RoundedCornerShape(4.dp))
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        repeat(8) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                                    .background(Color(0xFF444444), RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DailyTasksOnHomePreview(enabled: Boolean) {
    var showTasks by remember { mutableStateOf(false) }
    
    LaunchedEffect(enabled) {
        showTasks = false
        delay(200)
        showTasks = enabled
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Clock (always visible)
            Text(
                text = "10:45",
                fontSize = 48.sp,
                fontWeight = FontWeight.Light,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Daily tasks section
            AnimatedVisibility(
                visible = showTasks,
                enter = slideInVertically(initialOffsetY = { it / 2 }),
                exit = slideOutVertically(targetOffsetY = { it / 2 })
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TaskCard("Morning exercise", completed = true)
                    TaskCard("Review goals", completed = false)
                }
            }
        }
    }
}

@Composable
private fun TaskCard(title: String, completed: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (completed) 0.5f else 1.0f),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(
                        if (completed) Color(0xFF00FF00) else Color.Transparent,
                        CircleShape
                    )
            )
            Text(
                text = title,
                fontSize = 14.sp,
                color = Color.White,
                fontWeight = FontWeight.Light
            )
        }
    }
}

@Composable
fun NotificationInboxPreview(enabled: Boolean) {
    var showInbox by remember { mutableStateOf(false) }
    var notificationCount by remember { mutableStateOf(0) }
    
    LaunchedEffect(enabled) {
        if (!enabled) return@LaunchedEffect
        
        while (true) {
            delay(2000)
            // Simulate new notification arriving
            notificationCount++
            delay(1000)
            // User swipes down to check
            showInbox = true
            delay(3000)
            // User closes inbox
            showInbox = false
            notificationCount = 0
            delay(2000)
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(Color.Black)
    ) {
        // Home screen with notification indicator
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top status bar area with notification icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (enabled && notificationCount > 0 && !showInbox) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color(0xFF1A1A1A), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = notificationCount.toString(),
                            fontSize = 12.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // Clock
            Text(
                text = "10:45",
                fontSize = 40.sp,
                color = Color.White,
                fontWeight = FontWeight.Light
            )
            
            // Instruction
            if (enabled && notificationCount > 0 && !showInbox) {
                Text(
                    text = "Swipe down to view",
                    fontSize = 12.sp,
                    color = Color(0xFF666666),
                    fontWeight = FontWeight.Light
                )
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        
        // Notification Inbox overlay
        AnimatedVisibility(
            visible = showInbox && enabled,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(300)
            ),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(300)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .background(Color(0xF0000000))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Notifications",
                        fontSize = 24.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Light
                    )
                    Text(
                        text = "${notificationCount} new",
                        fontSize = 14.sp,
                        color = Color(0xFF888888),
                        fontWeight = FontWeight.Light
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                NotificationCard(
                    "WhatsApp",
                    "Hey, are you free tonight?",
                    "2 min ago"
                )
                NotificationCard(
                    "Gmail",
                    "Meeting reminder: Team sync at 3 PM",
                    "5 min ago"
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                Text(
                    text = "Curated & organized",
                    fontSize = 12.sp,
                    color = Color(0xFF666666),
                    fontWeight = FontWeight.Light,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun NotificationCard(app: String, message: String, time: String = "") {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = app,
                    fontSize = 13.sp,
                    color = Color(0xFF888888),
                    fontWeight = FontWeight.Medium
                )
                if (time.isNotEmpty()) {
                    Text(
                        text = time,
                        fontSize = 11.sp,
                        color = Color(0xFF666666),
                        fontWeight = FontWeight.Light
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                fontSize = 15.sp,
                color = Color.White,
                fontWeight = FontWeight.Light
            )
        }
    }
}

@Composable
fun DoubleTapLockPreview(enabled: Boolean) {
    var tapCount by remember { mutableStateOf(0) }
    var isLocked by remember { mutableStateOf(false) }
    var ripplePositions by remember { mutableStateOf(listOf<Pair<Float, Float>>()) }
    
    LaunchedEffect(enabled) {
        if (!enabled) return@LaunchedEffect
        
        while (true) {
            delay(4000)
            // Simulate first tap
            tapCount = 1
            ripplePositions = listOf(0.4f to 0.5f)
            delay(200)
            ripplePositions = emptyList()
            
            delay(200)
            // Simulate second tap
            tapCount = 2
            ripplePositions = listOf(0.6f to 0.5f)
            delay(200)
            ripplePositions = emptyList()
            
            // Lock animation
            isLocked = true
            delay(1500)
            isLocked = false
            tapCount = 0
            delay(500)
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(if (isLocked) Color.Black else Color(0xFF0A0A0A))
    ) {
        if (!isLocked) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "11:30",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap twice to lock",
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    fontWeight = FontWeight.Light
                )
            }
            
            // Tap ripples
            ripplePositions.forEach { (x, y) ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(
                            x = (x * 400).dp - 40.dp,
                            y = (y * 280).dp - 40.dp
                        )
                        .size(80.dp)
                        .background(Color.White.copy(alpha = 0.3f), CircleShape)
                )
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Locked",
                    fontSize = 24.sp,
                    color = Color(0xFF444444),
                    fontWeight = FontWeight.Light,
                    modifier = Modifier.alpha(0.5f)
                )
            }
        }
    }
}

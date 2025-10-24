@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.minifocus.launcher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minifocus.launcher.model.AppEntry
import com.minifocus.launcher.model.BottomIconSlot
import com.minifocus.launcher.model.ClockFormat
import com.minifocus.launcher.model.SearchResult
import com.minifocus.launcher.model.TaskItem
import com.minifocus.launcher.viewmodel.LauncherUiState
import kotlinx.coroutines.launch

@Composable
fun LauncherApp(
    state: LauncherUiState,
    onToggleTask: (Long) -> Unit,
    onAddTask: (String) -> Unit,
    onDeleteTask: (Long) -> Unit,
    onPinApp: (String) -> Unit,
    onUnpinApp: (String) -> Unit,
    onHideApp: (String) -> Unit,
    onUnhideApp: (String) -> Unit,
    onLockApp: (String, Long) -> Unit,
    onUnlockApp: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchVisibilityChange: (Boolean) -> Unit,
    onBottomIconChange: (BottomIconSlot, String) -> Unit,
    onSettingsVisibilityChange: (Boolean) -> Unit,
    onClockFormatChange: (ClockFormat) -> Unit,
    onKeyboardSearchOnSwipeChange: (Boolean) -> Unit,
    onConsumeMessage: () -> Unit,
    canLaunch: suspend (String) -> Boolean,
    onLaunchApp: (String) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val bottomIconPickerSlot = remember { mutableStateOf<BottomIconSlot?>(null) }
    val searchVisible = state.isSearchVisible
    val shouldShowInlineSearch = state.isKeyboardSearchOnSwipe
    val shouldFocusInlineSearch = shouldShowInlineSearch && pagerState.currentPage == 2

    LaunchedEffect(shouldShowInlineSearch, searchVisible, pagerState.currentPage) {
        if (shouldShowInlineSearch && pagerState.currentPage == 2 && searchVisible) {
            onSearchVisibilityChange(false)
        }
    }

    LaunchedEffect(shouldShowInlineSearch) {
        if (!shouldShowInlineSearch && state.searchQuery.isNotEmpty()) {
            onSearchQueryChange("")
        }
    }

    LaunchedEffect(pagerState.currentPage, shouldShowInlineSearch) {
        if (pagerState.currentPage != 2 && shouldShowInlineSearch && state.searchQuery.isNotEmpty()) {
            onSearchQueryChange("")
        }
    }

    LaunchedEffect(state.message) {
        val message = state.message
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onConsumeMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                when (page) {
                    0 -> TasksScreen(
                        tasks = state.tasks,
                        onToggleTask = onToggleTask,
                        onAddTask = onAddTask,
                        onDeleteTask = onDeleteTask
                    )
                    1 -> HomeScreen(
                        state = state,
                        onLaunchApp = { entry -> handleAppLaunch(entry, coroutineScope, canLaunch, onLaunchApp, snackbarHostState) },
                        onSearchVisibilityChange = onSearchVisibilityChange,
                        bottomIconPickerSlot = bottomIconPickerSlot,
                        onUnpinApp = onUnpinApp,
                        onHideApp = onHideApp,
                        onLockApp = onLockApp,
                        onUnlockApp = onUnlockApp
                    )
                    else -> AllAppsScreen(
                        apps = state.allApps,
                        hiddenApps = state.hiddenApps,
                        keyboardOnSwipe = shouldShowInlineSearch,
                        searchQuery = state.searchQuery,
                        shouldFocusSearch = shouldFocusInlineSearch,
                        onQueryChange = onSearchQueryChange,
                        onLaunchApp = { entry -> handleAppLaunch(entry, coroutineScope, canLaunch, onLaunchApp, snackbarHostState) },
                        onPinApp = onPinApp,
                        onUnpinApp = onUnpinApp,
                        onHideApp = onHideApp,
                        onUnhideApp = onUnhideApp,
                        onLockApp = onLockApp,
                        onUnlockApp = onUnlockApp,
                        onOpenSettings = { onSettingsVisibilityChange(!state.isSettingsVisible) }
                    )
                }
            }

            AnimatedVisibility(
                visible = searchVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                SearchOverlay(
                    query = state.searchQuery,
                    results = state.searchResults,
                    onQueryChange = onSearchQueryChange,
                    onDismiss = { onSearchVisibilityChange(false) },
                    autoFocus = state.isSearchVisible,
                    onResultClick = { entry ->
                        coroutineScope.launch {
                            if (canLaunch(entry.packageName)) {
                                onSearchVisibilityChange(false)
                                onLaunchApp(entry.packageName)
                            } else {
                                snackbarHostState.showSnackbar("App locked")
                            }
                        }
                    }
                )
            }

            BottomIconPickerDialog(
                slot = bottomIconPickerSlot.value,
                apps = state.allApps,
                onDismiss = { bottomIconPickerSlot.value = null },
                onSelect = { slot, packageName ->
                    onBottomIconChange(slot, packageName)
                    bottomIconPickerSlot.value = null
                }
            )

            if (state.isSettingsVisible) {
                SettingsDialog(
                    clockFormat = state.clockFormat,
                    keyboardOnSwipe = state.isKeyboardSearchOnSwipe,
                    onKeyboardToggle = onKeyboardSearchOnSwipeChange,
                    onClockFormatChange = onClockFormatChange,
                    onDismiss = { onSettingsVisibilityChange(false) }
                )
            }
        }
    }
}

private fun handleAppLaunch(
    entry: AppEntry,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    canLaunch: suspend (String) -> Boolean,
    onLaunchApp: (String) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    coroutineScope.launch {
        if (canLaunch(entry.packageName)) {
            onLaunchApp(entry.packageName)
        } else {
            snackbarHostState.showSnackbar("App locked")
        }
    }
}

@Composable
private fun HomeScreen(
    state: LauncherUiState,
    onLaunchApp: (AppEntry) -> Unit,
    onSearchVisibilityChange: (Boolean) -> Unit,
    bottomIconPickerSlot: MutableState<BottomIconSlot?>,
    onUnpinApp: (String) -> Unit,
    onHideApp: (String) -> Unit,
    onLockApp: (String, Long) -> Unit,
    onUnlockApp: (String) -> Unit
) {
    val expandedPinned = remember { mutableStateOf<String?>(null) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 32.dp, vertical = 48.dp)
            .pointerInput(Unit) {
                detectSwipeUp { onSearchVisibilityChange(true) }
            },
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = state.timeFormatted,
                fontSize = 72.sp,
                fontWeight = FontWeight.Light,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = state.dateFormatted,
                fontSize = 18.sp,
                color = Color(0xFFAAAAAA),
                textAlign = TextAlign.Center
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(state.pinnedApps) { app ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = app.label,
                        fontSize = 22.sp,
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .combinedClickable(
                                onClick = { onLaunchApp(app) },
                                onLongClick = { expandedPinned.value = app.packageName }
                            ),
                        textAlign = TextAlign.Center
                    )
                    DropdownMenu(
                        expanded = expandedPinned.value == app.packageName,
                        onDismissRequest = { expandedPinned.value = null }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Unpin") },
                            onClick = {
                                onUnpinApp(app.packageName)
                                expandedPinned.value = null
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Hide") },
                            onClick = {
                                onHideApp(app.packageName)
                                expandedPinned.value = null
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Lock 30m") },
                            onClick = {
                                onLockApp(app.packageName, 30)
                                expandedPinned.value = null
                            }
                        )
                        if (app.isLocked) {
                            DropdownMenuItem(
                                text = { Text("Unlock") },
                                onClick = {
                                    onUnlockApp(app.packageName)
                                    expandedPinned.value = null
                                }
                            )
                        }
                    }
                }
            }
        }

        BottomIconRow(
            left = state.bottomLeft,
            right = state.bottomRight,
            onLaunch = onLaunchApp,
            onLongPress = { slot -> bottomIconPickerSlot.value = slot }
        )
    }
}

@Composable
private fun BottomIconRow(
    left: AppEntry?,
    right: AppEntry?,
    onLaunch: (AppEntry) -> Unit,
    onLongPress: (BottomIconSlot) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        BottomIconButton(
            label = left?.label ?: "Phone",
            onClick = { left?.let(onLaunch) },
            onLongPress = { onLongPress(BottomIconSlot.LEFT) }
        )
        BottomIconButton(
            label = right?.label ?: "Camera",
            onClick = { right?.let(onLaunch) },
            onLongPress = { onLongPress(BottomIconSlot.RIGHT) }
        )
    }
}

@Composable
private fun BottomIconButton(
    label: String,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    Text(
        text = label.uppercase(),
        color = Color.White,
        modifier = Modifier
            .padding(16.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
    )
}

@Composable
private fun TasksScreen(
    tasks: List<TaskItem>,
    onToggleTask: (Long) -> Unit,
    onAddTask: (String) -> Unit,
    onDeleteTask: (Long) -> Unit
) {
    val newTaskTitle = remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 24.dp, vertical = 36.dp)
    ) {
        Text(text = "Tasks", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(24.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(tasks) { task ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = task.isCompleted,
                        onCheckedChange = { onToggleTask(task.id) }
                    )
                    Text(
                        text = task.title,
                        color = if (task.isCompleted) Color(0xFF777777) else Color.White,
                        modifier = Modifier.weight(1f).padding(start = 16.dp)
                    )
                    Text(
                        text = "×",
                        color = Color(0xFF777777),
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .combinedClickable(onClick = { onDeleteTask(task.id) }, onLongClick = { onDeleteTask(task.id) })
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x33FFFFFF))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                if (newTaskTitle.value.isBlank()) {
                    Text(text = "New task", color = Color(0x88FFFFFF))
                }
                BasicTextField(
                    value = newTaskTitle.value,
                    onValueChange = { newTaskTitle.value = it },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = {
                val title = newTaskTitle.value.trim()
                if (title.isNotEmpty()) {
                    onAddTask(title)
                    newTaskTitle.value = ""
                }
            }) {
                Text(text = "Add")
            }
        }
    }
}

@Composable
private fun AllAppsScreen(
    apps: List<AppEntry>,
    hiddenApps: List<AppEntry>,
    keyboardOnSwipe: Boolean,
    searchQuery: String,
    shouldFocusSearch: Boolean,
    onQueryChange: (String) -> Unit,
    onLaunchApp: (AppEntry) -> Unit,
    onPinApp: (String) -> Unit,
    onUnpinApp: (String) -> Unit,
    onHideApp: (String) -> Unit,
    onUnhideApp: (String) -> Unit,
    onLockApp: (String, Long) -> Unit,
    onUnlockApp: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    val expandedApp = remember { mutableStateOf<String?>(null) }
    val searchActive = searchQuery.isNotBlank()
    val filteredApps = remember(apps, searchQuery) {
        if (searchActive) {
            apps.filter { it.label.contains(searchQuery, ignoreCase = true) }
        } else {
            apps
        }
    }
    val filteredHiddenApps = remember(hiddenApps, searchQuery) {
        if (searchActive) {
            hiddenApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
        } else {
            hiddenApps
        }
    }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(keyboardOnSwipe) {
        if (!keyboardOnSwipe) {
            keyboardController?.hide()
        }
    }

    LaunchedEffect(shouldFocusSearch, keyboardOnSwipe) {
        if (keyboardOnSwipe && shouldFocusSearch) {
            focusRequester.requestFocus()
            keyboardController?.show()
        } else if (!shouldFocusSearch) {
            keyboardController?.hide()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 24.dp, vertical = 36.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "All Apps",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x33FFFFFF))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                if (searchQuery.isBlank()) {
                    Text(
                        text = "Search apps",
                        color = Color(0x88FFFFFF)
                    )
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (filteredApps.isEmpty() && filteredHiddenApps.isEmpty()) {
            item {
                Text(
                    text = "No apps match your search",
                    color = Color(0xFF666666),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            items(filteredApps) { app ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = app.label,
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .combinedClickable(
                                onClick = { onLaunchApp(app) },
                                onLongClick = { expandedApp.value = app.packageName }
                            )
                    )
                    DropdownMenu(
                        expanded = expandedApp.value == app.packageName,
                        onDismissRequest = { expandedApp.value = null }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (app.isPinned) "Unpin" else "Pin") },
                            onClick = {
                                if (app.isPinned) onUnpinApp(app.packageName) else onPinApp(app.packageName)
                                expandedApp.value = null
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Hide") },
                            onClick = {
                                onHideApp(app.packageName)
                                expandedApp.value = null
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Lock 30m") },
                            onClick = {
                                onLockApp(app.packageName, 30)
                                expandedApp.value = null
                            }
                        )
                        if (app.isLocked) {
                            DropdownMenuItem(
                                text = { Text("Unlock") },
                                onClick = {
                                    onUnlockApp(app.packageName)
                                    expandedApp.value = null
                                }
                            )
                        }
                    }
                }
            }
            if (filteredHiddenApps.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(text = "Hidden Apps", color = Color.Gray, fontSize = 20.sp)
                }
                items(filteredHiddenApps) { app ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = app.label,
                            color = Color.Gray,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .combinedClickable(
                                    onClick = { onLaunchApp(app) },
                                    onLongClick = { expandedApp.value = app.packageName }
                                )
                        )
                        DropdownMenu(
                            expanded = expandedApp.value == app.packageName,
                            onDismissRequest = { expandedApp.value = null }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Unhide") },
                                onClick = {
                                    onUnhideApp(app.packageName)
                                    expandedApp.value = null
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Unlock") },
                                onClick = {
                                    onUnlockApp(app.packageName)
                                    expandedApp.value = null
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchOverlay(
    query: String,
    results: List<SearchResult>,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    autoFocus: Boolean = false,
    onResultClick: (AppEntry) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE6000000))
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.align(Alignment.TopCenter)) {
            val focusRequester = remember { FocusRequester() }
            val keyboardController = LocalSoftwareKeyboardController.current
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = MaterialTheme.typography.headlineMedium.copy(color = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )

            LaunchedEffect(autoFocus) {
                if (autoFocus) {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                } else {
                    keyboardController?.hide()
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn {
                items(results) { result ->
                    when (result) {
                        is SearchResult.App -> Text(
                            text = result.entry.label,
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .combinedClickable(
                                    onClick = { onResultClick(result.entry) },
                                    onLongClick = {}
                                )
                        )
                        is SearchResult.Task -> Text(
                            text = result.item.title,
                            color = Color(0xFF888888),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        )
                        is SearchResult.Command -> {}
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomIconPickerDialog(
    slot: BottomIconSlot?,
    apps: List<AppEntry>,
    onDismiss: () -> Unit,
    onSelect: (BottomIconSlot, String) -> Unit
) {
    if (slot == null) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Assign App") },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().height(320.dp)) {
                items(apps) { app ->
                    Text(
                        text = app.label,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .combinedClickable(
                                onClick = { onSelect(slot, app.packageName) },
                                onLongClick = { onSelect(slot, app.packageName) }
                            )
                    )
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun SettingsDialog(
    clockFormat: ClockFormat,
    keyboardOnSwipe: Boolean,
    onKeyboardToggle: (Boolean) -> Unit,
    onClockFormatChange: (ClockFormat) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Settings", color = Color.White) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Keyboard on All Apps swipe", color = Color.White, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text(text = "Open search and keyboard when swiping to All Apps", color = Color.White, modifier = Modifier.weight(1f))
                    Switch(checked = keyboardOnSwipe, onCheckedChange = onKeyboardToggle)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Clock Format", color = Color.White, fontWeight = FontWeight.SemiBold)
                ClockFormat.values().forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = clockFormat == option,
                            onClick = { onClockFormatChange(option) }
                        )
                        Text(
                            text = when (option) {
                                ClockFormat.H24 -> "24-hour"
                                ClockFormat.H12 -> "12-hour"
                            },
                            color = Color.White,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Close")
            }
        }
    )
}

private suspend fun PointerInputScope.detectSwipeUp(onSwipeUp: () -> Unit) {
    var totalDrag = 0f
    detectVerticalDragGestures(
        onDragStart = { totalDrag = 0f },
        onDragEnd = {
            if (totalDrag < -120f) {
                onSwipeUp()
            }
            totalDrag = 0f
        },
        onDragCancel = { totalDrag = 0f }
    ) { change, dragAmount ->
        totalDrag += dragAmount
    }
}

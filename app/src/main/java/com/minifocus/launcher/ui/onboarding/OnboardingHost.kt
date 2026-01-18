package com.minifocus.launcher.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minifocus.launcher.permissions.PermissionsState
import com.minifocus.launcher.ui.PermissionScreen
import com.minifocus.launcher.viewmodel.OnboardingViewModel
import kotlinx.coroutines.launch

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun OnboardingHost(
    viewModel: OnboardingViewModel,
    permissionsState: PermissionsState,
    onRequestLauncher: () -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestNotificationListener: () -> Unit,
    onRequestExactAlarms: () -> Unit,
    onRequestLockAccessibility: () -> Unit,
    onRequestUsageStats: () -> Unit,
    onRequestOverlay: () -> Unit,
    showRestrictedNotificationHint: Boolean,
    onOpenRestrictedSettings: () -> Unit,
    onRefreshPermissions: () -> Unit,
    onComplete: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val completionEvent by viewModel.completionEvent.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { state.totalSteps })
    val scope = rememberCoroutineScope()
    
    // Update permissions state in viewmodel
    LaunchedEffect(permissionsState) {
        viewModel.updatePermissionsState(permissionsState)
    }
    
    // Handle completion
    LaunchedEffect(completionEvent) {
        if (completionEvent) {
            onComplete()
        }
    }
    
    // Sync pager with viewmodel step
    LaunchedEffect(state.currentStep) {
        if (pagerState.currentPage != state.currentStep) {
            pagerState.animateScrollToPage(state.currentStep)
        }
    }
    
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != state.currentStep) {
            viewModel.setCurrentStep(pagerState.currentPage)
        }
    }
    
    BackHandler(enabled = state.currentStep > 0) {
        viewModel.previousStep()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Progress indicator
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0A0A0A))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Step ${state.currentStep + 1} of ${state.totalSteps}",
                    fontSize = 14.sp,
                    color = Color(0xFF888888),
                    fontWeight = FontWeight.Light
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (state.currentStep + 1).toFloat() / state.totalSteps },
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                trackColor = Color(0xFF333333)
            )
        }
        
        // Pager content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            userScrollEnabled = false
        ) { page ->
            when (page) {
                0 -> WelcomeScreen(
                    onContinue = { viewModel.nextStep() }
                )
                
                1 -> WhyScreen(
                    onContinue = { viewModel.nextStep() },
                    onBack = { viewModel.previousStep() }
                )
                
                2 -> SetLauncherScreen(
                    isGranted = state.launcherRoleGranted,
                    onRequestLauncher = onRequestLauncher,
                    onContinue = { viewModel.nextStep() },
                    onBack = { viewModel.previousStep() }
                )
                
                3 -> PermissionScreen(
                    state = state.permissionsState,
                    allowDismiss = false,
                    onRequestNotifications = onRequestNotifications,
                    onRequestNotificationListener = onRequestNotificationListener,
                    onRequestExactAlarms = onRequestExactAlarms,
                    onRequestLockAccessibility = onRequestLockAccessibility,
                    onRequestUsageStats = onRequestUsageStats,
                    onRequestOverlay = onRequestOverlay,
                    showRestrictedNotificationHint = showRestrictedNotificationHint,
                    onOpenRestrictedSettings = onOpenRestrictedSettings,
                    onClose = { viewModel.previousStep() },
                    onContinue = { viewModel.nextStep() },
                    onRefreshPermissions = onRefreshPermissions
                )
                
                4 -> FeaturePreviewScreen(
                    title = "Clock Seconds",
                    description = "See every second tick on your home clock, or keep it minimal with just hours and minutes.",
                    preview = {
                        ClockSecondsPreview(showSeconds = state.showSeconds ?: true)
                    },
                    currentState = state.showSeconds ?: false,
                    onEnable = {
                        viewModel.setShowSeconds(true)
                        viewModel.nextStep()
                    },
                    onSkip = {
                        viewModel.setShowSeconds(false)
                        viewModel.nextStep()
                    },
                    onBack = { viewModel.previousStep() }
                )
                
                5 -> FeaturePreviewScreen(
                    title = "Smart Suggestions",
                    description = "Smart suggestions learns your habits and surfaces frequently-used apps at the top of your drawer.",
                    preview = {
                        SmartSuggestionsPreview(enabled = state.smartSuggestionsEnabled ?: true)
                    },
                    currentState = state.smartSuggestionsEnabled ?: false,
                    onEnable = {
                        viewModel.setSmartSuggestions(true)
                        viewModel.nextStep()
                    },
                    onSkip = {
                        viewModel.setSmartSuggestions(false)
                        viewModel.nextStep()
                    },
                    onBack = { viewModel.previousStep() }
                )
                
                6 -> FeaturePreviewScreen(
                    title = "Keyboard on Swipe",
                    description = "Open the app drawer with the keyboard ready to search instantly, saving you an extra tap.",
                    preview = {
                        KeyboardOnSwipePreview(enabled = state.keyboardOnSwipe ?: false)
                    },
                    currentState = state.keyboardOnSwipe ?: false,
                    onEnable = {
                        viewModel.setKeyboardOnSwipe(true)
                        viewModel.nextStep()
                    },
                    onSkip = {
                        viewModel.setKeyboardOnSwipe(false)
                        viewModel.nextStep()
                    },
                    onBack = { viewModel.previousStep() }
                )
                
                7 -> FeaturePreviewScreen(
                    title = "Daily Tasks on Home",
                    description = "Show your daily recurring tasks on the home screen until completed. Auto-hides after you finish them.",
                    preview = {
                        DailyTasksOnHomePreview(enabled = state.showDailyTasksOnHome ?: true)
                    },
                    currentState = state.showDailyTasksOnHome ?: false,
                    onEnable = {
                        viewModel.setShowDailyTasksOnHome(true)
                        viewModel.nextStep()
                    },
                    onSkip = {
                        viewModel.setShowDailyTasksOnHome(false)
                        viewModel.nextStep()
                    },
                    onBack = { viewModel.previousStep() }
                )
                
                8 -> FeaturePreviewScreen(
                    title = "Notification Inbox",
                    description = "Curate and manage notifications in a dedicated inbox instead of the system shade. Keeps distractions minimal.",
                    preview = {
                        NotificationInboxPreview(enabled = state.notificationInboxEnabled ?: false)
                    },
                    currentState = state.notificationInboxEnabled ?: false,
                    onEnable = {
                        viewModel.setNotificationInbox(true)
                        viewModel.nextStep()
                    },
                    onSkip = {
                        viewModel.setNotificationInbox(false)
                        viewModel.nextStep()
                    },
                    onBack = { viewModel.previousStep() }
                )
                
                9 -> FeaturePreviewScreen(
                    title = "Double-Tap Lock",
                    description = "Lock your screen with a double-tap gesture without disabling biometrics. Quick and convenient.",
                    preview = {
                        DoubleTapLockPreview(enabled = state.doubleTapLockScreen ?: false)
                    },
                    currentState = state.doubleTapLockScreen ?: false,
                    onEnable = {
                        viewModel.setDoubleTapLock(true)
                        viewModel.nextStep()
                    },
                    onSkip = {
                        viewModel.setDoubleTapLock(false)
                        viewModel.nextStep()
                    },
                    onBack = { viewModel.previousStep() }
                )
                
                10 -> FinishScreen(
                    state = state,
                    onFinish = { viewModel.finishOnboarding() },
                    onRestart = {
                        scope.launch {
                            viewModel.resetOnboarding()
                            pagerState.scrollToPage(0)
                        }
                    }
                )
            }
        }
    }
}

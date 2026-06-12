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

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Onboarding step definitions.
 *
 * Pager layout: [0=Calendar, 1=Tasks, 2=Home(initial), 3=Drawer]
 *
 * Gesture directions (from user's finger movement):
 *   - "Swipe right" (finger left->right) reveals LOWER page index (left neighbor)
 *   - "Swipe left"  (finger right->left) reveals HIGHER page index (right neighbor)
 *
 * Home(2)->Tasks(1): swipe RIGHT
 * Tasks(1)->Calendar(0): swipe RIGHT
 * Calendar(0)->Home(2): swipe LEFT
 * Home(2)->Drawer(3): swipe LEFT
 */
object OnboardingSteps {
    /** Welcome / intro -- full overlay, user taps to begin. */
    const val WELCOME = 0
    /** Home visible, swipe RIGHT to reach Tasks (page 2->1). */
    const val SWIPE_TO_TASKS = 1
    /** Tasks visible, swipe RIGHT to reach Calendar (page 1->0). */
    const val SWIPE_TO_CALENDAR = 2
    /** Calendar visible, swipe LEFT to go back home (page 0->2). */
    const val SWIPE_BACK_HOME = 3
    /** Home visible, swipe LEFT to open App Drawer (page 2->3). */
    const val SWIPE_TO_DRAWER = 4
    /** App Drawer visible, long-press an app. */
    const val LONG_PRESS_TO_PIN = 5
    /** Context menu showing, tap Pin to Home. */
    const val TAP_PIN = 6
    /** Done. */
    const val COMPLETE = 7

    const val TOTAL = 8
}

// ---- Main overlay ----

@Composable
fun OnboardingOverlay(
    step: Int,
    onFinish: () -> Unit,
    onWelcomeDismiss: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Scrim layer
        val scrimAlpha = when (step) {
            OnboardingSteps.WELCOME -> 0.82f
            OnboardingSteps.COMPLETE -> 0.78f
            OnboardingSteps.LONG_PRESS_TO_PIN,
            OnboardingSteps.TAP_PIN -> 0f
            else -> 0.35f
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
                .then(
                    when (step) {
                        // Welcome: tap anywhere to advance
                        OnboardingSteps.WELCOME -> Modifier.clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onWelcomeDismiss() }
                        // Complete: block all touches behind
                        OnboardingSteps.COMPLETE -> Modifier.pointerInput(Unit) {
                            awaitEachGesture { awaitFirstDown(requireUnconsumed = false) }
                        }
                        else -> Modifier
                    }
                )
        )

        // Step-specific content
        when (step) {
            OnboardingSteps.WELCOME ->
                WelcomeScreen()

            // Directions fixed: Home->Tasks = RIGHT, Tasks->Calendar = RIGHT
            OnboardingSteps.SWIPE_TO_TASKS ->
                SwipeGuide(direction = SwipeDirection.RIGHT, label = "Swipe to see your tasks", step = step)

            OnboardingSteps.SWIPE_TO_CALENDAR ->
                SwipeGuide(direction = SwipeDirection.RIGHT, label = "Swipe for your calendar", step = step)

            // Calendar->Home = LEFT, Home->Drawer = LEFT
            OnboardingSteps.SWIPE_BACK_HOME ->
                SwipeGuide(direction = SwipeDirection.LEFT, label = "Swipe back home", step = step)

            OnboardingSteps.SWIPE_TO_DRAWER ->
                SwipeGuide(direction = SwipeDirection.LEFT, label = "Swipe to open apps", step = step)

            OnboardingSteps.LONG_PRESS_TO_PIN ->
                FloatingHint(text = "Long-press any app to pin it")

            OnboardingSteps.TAP_PIN ->
                FloatingHint(text = "Tap 'Pin to Home'")

            OnboardingSteps.COMPLETE ->
                CompletionScreen(onFinish = onFinish)
        }
    }
}

// ---- Welcome screen ----

@Composable
private fun WelcomeScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 48.dp)
        ) {
            Text(
                text = "MiniFocus",
                color = Color.White,
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "A minimalist launcher\nfor a focused life",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(56.dp))

            // Pulsing "tap to begin" hint
            val infiniteTransition = rememberInfiniteTransition(label = "tap")
            val tapAlpha by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 0.9f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "tapAlpha"
            )

            Text(
                text = "Tap anywhere to begin",
                color = Color.White.copy(alpha = tapAlpha),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ---- Swipe guide ----

private enum class SwipeDirection { LEFT, RIGHT }

@Composable
private fun SwipeGuide(
    direction: SwipeDirection,
    label: String,
    step: Int
) {
    val infiniteTransition = rememberInfiniteTransition(label = "swipe")

    // Circle slides in the swipe direction
    // RIGHT = finger moves left->right = positive offset
    // LEFT  = finger moves right->left = negative offset
    val slideOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (direction == SwipeDirection.RIGHT) 80f else -80f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "slide"
    )

    val circleFade by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fade"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Animated circle + trailing arrows
            Box(
                modifier = Modifier
                    .width(220.dp)
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                SwipeArrows(direction = direction, progress = slideOffset)

                // Finger dot
                Box(
                    modifier = Modifier
                        .offset { IntOffset(slideOffset.dp.roundToPx(), 0) }
                        .alpha(circleFade.coerceIn(0.2f, 1f))
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.85f))
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            Text(
                text = label,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            StepDots(currentStep = step)
        }
    }
}

@Composable
private fun SwipeArrows(
    direction: SwipeDirection,
    progress: Float
) {
    val normalizedProgress = (progress / 80f).let {
        if (direction == SwipeDirection.LEFT) -it else it
    }

    Canvas(modifier = Modifier.size(220.dp, 80.dp)) {
        val centerY = size.height / 2f
        val arrowSize = 12f
        val spacing = 26f

        for (i in 0..2) {
            val baseX = if (direction == SwipeDirection.RIGHT) {
                size.width / 2f + 30f + (i * spacing)
            } else {
                size.width / 2f - 30f - (i * spacing)
            }
            val alpha = (0.1f + normalizedProgress * 0.25f * (3 - i) / 3f).coerceIn(0.06f, 0.35f)

            val path = Path().apply {
                if (direction == SwipeDirection.RIGHT) {
                    moveTo(baseX - arrowSize, centerY - arrowSize)
                    lineTo(baseX, centerY)
                    lineTo(baseX - arrowSize, centerY + arrowSize)
                } else {
                    moveTo(baseX + arrowSize, centerY - arrowSize)
                    lineTo(baseX, centerY)
                    lineTo(baseX + arrowSize, centerY + arrowSize)
                }
            }
            drawPath(
                path = path,
                color = Color.White.copy(alpha = alpha),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
            )
        }
    }
}

// ---- Floating hint ----

@Composable
private fun FloatingHint(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.75f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )

        Box(
            modifier = Modifier
                .padding(top = 80.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(horizontal = 24.dp, vertical = 14.dp)
                .alpha(pulseAlpha)
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ---- Completion screen ----

@Composable
private fun CompletionScreen(onFinish: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 48.dp)
        ) {
            Text(
                text = "You're ready",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Let's set up a few permissions next",
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(44.dp))

            Button(
                onClick = onFinish,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(52.dp)
            ) {
                Text(
                    text = "Continue",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            StepDots(currentStep = OnboardingSteps.COMPLETE)
        }
    }
}

// ---- Step dots ----

@Composable
private fun StepDots(currentStep: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(OnboardingSteps.TOTAL) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == currentStep) 8.dp else 5.dp)
                    .clip(CircleShape)
                    .background(
                        if (index <= currentStep) Color.White
                        else Color.White.copy(alpha = 0.2f)
                    )
            )
        }
    }
}

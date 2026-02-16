package com.minifocus.launcher.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minifocus.launcher.ui.components.ScreenHeader

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onNavigateToEmergencyUnlock: () -> Unit
) {
    val context = LocalContext.current
    var clickCount by remember { mutableStateOf(0) }
    
    // Get version info from package manager
    val packageInfo = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (e: Exception) {
            null
        }
    }
    val versionName = packageInfo?.versionName ?: "Unknown"
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 36.dp)
    ) {
        ScreenHeader(
            title = "About",
            onBack = onBack
        )

        Spacer(modifier = Modifier.height(40.dp))

        // App name
        Text(
            text = "Mini Launcher",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Version $versionName",
            color = Color(0xFFAAAAAA),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "A minimalist focus launcher",
            color = Color(0xFFAAAAAA),
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Rate & Share section
        Text(
            text = "Support the App",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
        ) {
            // Rate button
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFF1A1A1A), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .clickable {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("market://details?id=com.minifocus.launcher")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.minifocus.launcher"))
                            context.startActivity(webIntent)
                        }
                    }
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "⭐",
                    fontSize = 24.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Rate on Play Store",
                    color = Color.White,
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            // Share button
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFF1A1A1A), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .clickable {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Mini Launcher")
                            putExtra(Intent.EXTRA_TEXT, "Check out Mini Launcher - A minimalist focus launcher\n\nhttps://play.google.com/store/apps/details?id=com.minifocus.launcher")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share via"))
                    }
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "📤",
                    fontSize = 24.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Share App",
                    color = Color.White,
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Developer info
        Text(
            text = "Developer",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "A Akhil",
            color = Color.White,
            fontSize= 16.sp,
            modifier = Modifier.clickable {
                clickCount++
                if (clickCount >= 20) {
                    clickCount = 0
                    onNavigateToEmergencyUnlock()
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Contact section
        Text(
            text = "Contact",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Email
        Text(
            text = "akhil@devakhil.com",
            color = Color(0xFF6BB6FF),
            fontSize = 16.sp,
            modifier = Modifier.clickable {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:akhil@devakhil.com")
                }
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Website
        Text(
            text = "devakhil.com",
            color = Color(0xFF6BB6FF),
            fontSize = 16.sp,
            modifier = Modifier.clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://devakhil.com"))
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Feedback section
        Text(
            text = "Feedback & Issues",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "For bug reports and feature requests:",
            color = Color(0xFFAAAAAA),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // GitHub link
        Text(
            text = "github.com/A-Akhil/mini-launcher",
            color = Color(0xFF6BB6FF),
            fontSize = 16.sp,
            modifier = Modifier.clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/A-Akhil/mini-launcher"))
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Or send an email to:",
            color = Color(0xFFAAAAAA),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "akhil@devakhil.com",
            color = Color(0xFF6BB6FF),
            fontSize = 16.sp,
            modifier = Modifier.clickable {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:akhil@devakhil.com?subject=Mini%20Launcher%20Feedback")
                }
                context.startActivity(intent)
            }
        )
    }
}

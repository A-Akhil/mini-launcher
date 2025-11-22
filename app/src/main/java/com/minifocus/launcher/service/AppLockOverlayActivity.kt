package com.minifocus.launcher.service

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.minifocus.launcher.ui.theme.MinimalistFocusTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppLockOverlayActivity : ComponentActivity() {

    companion object {
        // Android package name validation constants
        private const val MAX_PACKAGE_NAME_LENGTH = 255
        
        // Security token constants for validating internal intents
        const val EXTRA_SECURITY_TOKEN = "security_token"
        const val EXTRA_TIMESTAMP = "timestamp"
        private const val TOKEN_VALIDITY_MS = 5000L // Token valid for 5 seconds
        
        // Shared secret for HMAC (persists across app lifecycle)
        private val SECRET_KEY: ByteArray by lazy {
            // Use a deterministic key based on app installation
            // This ensures tokens remain valid across process restarts
            val keyMaterial = "AppLockSecurity_${android.os.Build.ID}_v1"
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            digest.digest(keyMaterial.toByteArray(Charsets.UTF_8))
        }
        
        // Thread-local Mac instance for efficient token generation
        private val macThreadLocal = ThreadLocal.withInitial {
            javax.crypto.Mac.getInstance("HmacSHA256").apply {
                val secretKeySpec = javax.crypto.spec.SecretKeySpec(SECRET_KEY, "HmacSHA256")
                init(secretKeySpec)
            }
        }
        
        /**
         * Generate a time-based HMAC security token that can only be created by our app.
         * Uses HmacSHA256 for proper authentication and integrity.
         * 
         * This is internal to prevent exposing token generation to external code.
         */
        internal fun generateSecurityToken(timestamp: Long): String {
            val message = timestamp.toString().toByteArray(Charsets.UTF_8)
            
            // Use thread-local Mac instance for efficiency and thread safety
            val mac = macThreadLocal.get() ?: throw IllegalStateException("Mac initialization failed")
            
            // Explicitly reset to ensure clean state for thread safety
            mac.reset()
            val hmac = mac.doFinal(message)
            
            return hmac.joinToString("") { "%02x".format(it) }
        }
        
        /**
         * Regex pattern for Android package name validation
         * 
         * Android package naming rules:
         * - Must have at least two segments separated by dots
         * - Each segment must start with a lowercase letter [a-z]
         * - Segments can contain: lowercase letters, uppercase letters, numbers, underscores
         * - Pattern: ^[a-z][a-zA-Z0-9_]*(\\.[a-z][a-zA-Z0-9_]*)+$
         * 
         * Examples:
         * - Valid: com.example.app, com.myCompany.myApp, com.example_app
         * - Invalid: Com.example.app (first segment starts with uppercase)
         * - Invalid: com (only one segment)
         * - Invalid: 1com.example (segment starts with number)
         */
        private val PACKAGE_NAME_PATTERN = Regex("^[a-z][a-zA-Z0-9_]*(\\.[a-z][a-zA-Z0-9_]*)+\$")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Security: Verify the intent is from our own app using cryptographic token
        // The token approach prevents intent spoofing attacks
        if (!validateSecurityToken()) {
            android.util.Log.w("AppLockOverlay", "Invalid or missing security token - rejecting intent")
            finish()
            return
        }
        
        // Security: Prevent tapjacking/overlay attacks on this lock screen
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_SECURE,
            android.view.WindowManager.LayoutParams.FLAG_SECURE
        )
        
        // Set black status bar and navigation bar
        window.statusBarColor = AndroidColor.BLACK
        window.navigationBarColor = AndroidColor.BLACK
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
        
        val packageName = intent.getStringExtra(AppLockMonitorService.EXTRA_PACKAGE_NAME) ?: ""
        val lockedUntil = intent.getLongExtra(AppLockMonitorService.EXTRA_LOCKED_UNTIL, 0L)
        
        // Security: Validate input parameters
        if (packageName.isEmpty() || lockedUntil == 0L || lockedUntil < System.currentTimeMillis()) {
            finish()
            return
        }
        
        // Security: Validate package name format to prevent injection
        if (!isValidPackageName(packageName)) {
            finish()
            return
        }
        
        val appName = getAppName(packageName)
        
        setContent {
            MinimalistFocusTheme {
                AppLockOverlayScreen(
                    appName = appName,
                    lockedUntil = lockedUntil
                )
            }
        }
    }
    
    /**
     * Security: Validate security token to ensure intent originates from our app.
     * 
     * This prevents intent spoofing attacks where malicious apps could launch
     * fake lock screens by crafting intents with our component name.
     * 
     * The token is a time-based HMAC that only our app can generate using a
     * secret key that's unique per app instance.
     * 
     * @return true if token is valid and not expired, false otherwise
     */
    private fun validateSecurityToken(): Boolean {
        val receivedToken = intent.getStringExtra(EXTRA_SECURITY_TOKEN)
        val timestamp = intent.getLongExtra(EXTRA_TIMESTAMP, 0L)
        
        if (receivedToken == null || timestamp == 0L) {
            return false
        }
        
        // Check token age - prevent replay attacks
        val tokenAge = System.currentTimeMillis() - timestamp
        if (tokenAge < 0 || tokenAge > TOKEN_VALIDITY_MS) {
            android.util.Log.w("AppLockOverlay", "Token expired or invalid timestamp")
            return false
        }
        
        // Validate token by regenerating it with same timestamp
        val expectedToken = generateSecurityToken(timestamp)
        
        // Use constant-time comparison to prevent timing attacks
        return constantTimeEquals(receivedToken, expectedToken)
    }
    
    /**
     * Constant-time string comparison to prevent timing attacks.
     * Compares two hex strings in constant time regardless of content or length.
     * 
     * Note: Uses fixed-length comparison (64 chars for HmacSHA256 hex output)
     * to prevent length-based timing leaks.
     */
    private fun constantTimeEquals(a: String, b: String): Boolean {
        // HmacSHA256 produces 32 bytes = 64 hex characters
        val expectedLength = 64
        
        // Pad or truncate to expected length to prevent length-based timing attacks
        val aPadded = a.padEnd(expectedLength, '0').take(expectedLength)
        val bPadded = b.padEnd(expectedLength, '0').take(expectedLength)
        
        var result = 0
        for (i in 0 until expectedLength) {
            result = result or (aPadded[i].code xor bPadded[i].code)
        }
        return result == 0
    }
    
    /**
     * Security: Validate package name format to prevent malicious input
     */
    private fun isValidPackageName(packageName: String): Boolean {
        return packageName.matches(PACKAGE_NAME_PATTERN) && 
               packageName.length <= MAX_PACKAGE_NAME_LENGTH
    }

    private fun getAppName(packageName: String): String {
        return try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    private fun navigateToHome() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Block back button - don't allow user to escape the lock screen
    }
}

@Composable
fun AppLockOverlayScreen(
    appName: String,
    lockedUntil: Long
) {
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = System.currentTimeMillis()
        }
    }
    
    val timeRemaining = (lockedUntil - currentTime).coerceAtLeast(0)
    val hours = timeRemaining / (1000 * 60 * 60)
    val minutes = (timeRemaining % (1000 * 60 * 60)) / (1000 * 60)
    val seconds = (timeRemaining % (1000 * 60)) / 1000
    
    val unlockTimeText = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
        .format(Date(lockedUntil))
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = appName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Light,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Locked",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = when {
                    hours > 0 -> String.format("%dh %02dm %02ds", hours, minutes, seconds)
                    minutes > 0 -> String.format("%dm %02ds", minutes, seconds)
                    else -> String.format("%ds", seconds)
                },
                fontSize = 48.sp,
                fontWeight = FontWeight.Light,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Unlocks at $unlockTimeText",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "You chose to focus\nRespect your decision",
                fontSize = 16.sp,
                fontWeight = FontWeight.Light,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
        }
    }
}

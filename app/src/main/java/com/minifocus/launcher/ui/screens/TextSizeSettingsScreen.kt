package com.minifocus.launcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minifocus.launcher.model.TextSize
import com.minifocus.launcher.ui.components.ScreenHeader
import com.minifocus.launcher.ui.theme.TextSizeProvider

@Composable
fun TextSizeSettingsScreen(
    textSize: TextSize,
    onTextSizeChange: (TextSize) -> Unit,
    onBack: () -> Unit
) {
    // Wrap preview in the new multiplier so preview updates live, but screen UI stays at system scale?
    // Actually, should the settings screen itself scale? Probably yes for consistency.
    // Let's assume the outer provider handles scaling, unless we want the preview to be responsive while controls are static.
    // However, the preview specifically needs to show the *selected* size, not just the currently applied one.
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 36.dp)
    ) {
        ScreenHeader(
            title = "Text Size",
            onBack = onBack
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Options List
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextSize.entries.forEach { size ->
                TextSizeOption(
                    size = size,
                    isSelected = textSize == size,
                    onSelect = { onTextSizeChange(size) }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Preview",
            color = Color(0xFFAAAAAA),
            fontSize = 14.sp, // Keep label static or scaled? Let's use static for "meta" labels
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Preview Box 
        // We don't need another TextSizeProvider here because the whole screen is already wrapped in one by LauncherApp.
        // And since the state updates immediately on selection, the global provider updates immediately.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x11FFFFFF))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "12:30",
                    color = Color.White,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Calendar",
                    color = Color.White,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Review project goals and update roadmap.",
                    color = Color(0xFFAAAAAA),
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun TextSizeOption(
    size: TextSize,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onSelect)
            .background(if (isSelected) Color(0x22FFFFFF) else Color.Transparent)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = Color.White,
                unselectedColor = Color(0xFFAAAAAA)
            )
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = size.label,
                color = Color.White,
                fontSize = 18.sp, 
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
            Text(
                text = "${(size.multiplier * 100).toInt()}%",
                color = Color(0xFFAAAAAA),
                fontSize = 14.sp
            )
        }
    }
}

package com.minifocus.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun MinimalCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(5.dp)
    val borderColor = if (enabled) Color.White else Color(0xFF555555)
    val fillColor = if (checked) Color.White else Color.Transparent

    Box(
        modifier = modifier
            .size(22.dp)
            .clip(shape)
            .background(fillColor, shape)
            .border(width = 2.dp, color = borderColor, shape = shape)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox
            ) { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.Black)
            )
        }
    }
}

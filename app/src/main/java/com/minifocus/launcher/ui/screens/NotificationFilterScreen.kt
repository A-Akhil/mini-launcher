@file:OptIn(ExperimentalMaterial3Api::class)

package com.minifocus.launcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minifocus.launcher.viewmodel.NotificationFilterViewModel.FilterUiState
import com.minifocus.launcher.viewmodel.NotificationFilterViewModel.NotificationFilterItem

@Composable
fun NotificationFilterScreen(
    state: FilterUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onToggle: (NotificationFilterItem) -> Unit,
    onToggleAll: (Boolean) -> Unit
) {
    val menuExpanded = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        HeaderContent(
            menuExpanded = menuExpanded,
            onToggleAll = onToggleAll,
            onBack = onBack
        )

        Spacer(modifier = Modifier.height(24.dp))

        TextField(
            value = state.query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search apps", color = Color(0x55FFFFFF)) },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0x0AFFFFFF),
                unfocusedContainerColor = Color(0x05FFFFFF),
                disabledContainerColor = Color.Transparent,
                errorContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedPlaceholderColor = Color(0x55FFFFFF),
                unfocusedPlaceholderColor = Color(0x55FFFFFF),
                cursorColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (state.items.isEmpty()) {
            EmptyFiltersState()
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(state.items, key = { it.packageName }) { item ->
                    FilterRow(item = item, onToggle = onToggle)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun HeaderContent(
    menuExpanded: MutableState<Boolean>,
    onToggleAll: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp)
        ) {
            Text(
                text = "Filters",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "Manage notification preferences",
                color = Color(0x66FFFFFF),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    Box(modifier = Modifier.padding(start = 4.dp)) {
            IconButton(onClick = { menuExpanded.value = true }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "More",
                    tint = Color(0xCCFFFFFF)
                )
            }
            DropdownMenu(
                expanded = menuExpanded.value,
                onDismissRequest = { menuExpanded.value = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Enable all") },
                    onClick = {
                        menuExpanded.value = false
                        onToggleAll(true)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Disable all") },
                    onClick = {
                        menuExpanded.value = false
                        onToggleAll(false)
                    }
                )
            }
        }
    }
}

@Composable
private fun FilterRow(
    item: NotificationFilterItem,
    onToggle: (NotificationFilterItem) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (item.isEnabled) Color(0x08FFFFFF) else Color.Transparent,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = item.appName,
            color = if (item.isEnabled) Color.White else Color(0xAAFFFFFF),
            fontSize = 17.sp,
            fontWeight = if (item.isEnabled) FontWeight.Medium else FontWeight.Normal
        )
        Switch(
            checked = item.isEnabled,
            onCheckedChange = { onToggle(item) }
        )
    }
}

@Composable
private fun EmptyFiltersState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "No matching apps",
            color = Color(0xFF777777),
            fontSize = 16.sp
        )
    }
}

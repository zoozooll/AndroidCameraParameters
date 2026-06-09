package com.aaron.cameraparams.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.aaron.cameraparams.ui.CameraViewModel
import com.aaron.cameraparams.ui.theme.MonospaceTypography

@Composable
fun RawJsonScreen(viewModel: CameraViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Raw (JSON)") },
            actions = {
                IconButton(onClick = { clipboardManager.setText(AnnotatedString(uiState.rawJson)) }) {
                    Icon(Icons.Default.Info, contentDescription = "Copy Info")
                }
                IconButton(onClick = { /* Share action */ }) {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                }
            }
        )

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().background(Color(0xFF0D0E11))
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = uiState.rawJson,
                style = MonospaceTypography,
                color = Color(0xFFCE9178) // JSON String color like VS Code
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopAppBar(title: @Composable () -> Unit, actions: @Composable RowScope.() -> Unit) {
    CenterAlignedTopAppBar(
        title = title,
        actions = actions,
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

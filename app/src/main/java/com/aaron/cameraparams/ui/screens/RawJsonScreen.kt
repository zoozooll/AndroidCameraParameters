package com.aaron.cameraparams.ui.screens

import android.content.ClipData
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
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.aaron.cameraparams.R
import com.aaron.cameraparams.ui.CameraViewModel
import com.aaron.cameraparams.ui.theme.MonospaceTypography
import kotlinx.coroutines.launch

@Composable
fun RawJsonScreen(viewModel: CameraViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.nav_raw_json),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                val clipData = ClipData.newPlainText("Raw JSON", uiState.rawJson)
                scope.launch { clipboard.setClipEntry(ClipEntry(clipData)) }
            }) {
                Icon(Icons.Default.Info, contentDescription = stringResource(R.string.cd_copy_info))
            }
            IconButton(onClick = { /* Share action */ }) {
                Icon(Icons.Default.Share, contentDescription = stringResource(R.string.cd_share))
            }
        }

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

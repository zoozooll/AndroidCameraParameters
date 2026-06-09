package com.aaron.cameraparams.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aaron.cameraparams.ui.CameraViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParameterDetailScreen(viewModel: CameraViewModel, parameterKey: String, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val parameter = uiState.categories.flatMap { it.parameters }.find { it.key == parameterKey }
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(parameter?.key?.substringAfterLast(".") ?: "Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (parameter != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DetailCard("Value", parameter.value, onCopy = { clipboardManager.setText(AnnotatedString(parameter.value)) })
                
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Info", style = MaterialTheme.typography.labelSmall)
                        Spacer(Modifier.height(8.dp))
                        InfoRow("Key", parameter.key)
                        InfoRow("Category", parameter.category)
                    }
                }

                DetailCard("Raw Value", parameter.rawValue, onCopy = { clipboardManager.setText(AnnotatedString(parameter.rawValue)) })
            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Parameter not found")
            }
        }
    }
}

@Composable
fun DetailCard(title: String, value: String, onCopy: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                IconButton(onClick = onCopy, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Info, contentDescription = "Copy")
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.width(100.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

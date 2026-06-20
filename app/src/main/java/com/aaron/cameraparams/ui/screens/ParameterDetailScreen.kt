package com.aaron.cameraparams.ui.screens

import android.content.ClipData
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.aaron.cameraparams.R
import com.aaron.cameraparams.ui.CameraViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParameterDetailScreen(viewModel: CameraViewModel, parameterKey: String, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val parameter = uiState.parameters.categories.flatMap { it.parameters }.find { it.key == parameterKey }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(parameter?.key?.substringAfterLast(".") ?: stringResource(R.string.nav_detail)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
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
                DetailCard(
                    stringResource(R.string.detail_value_label),
                    parameter.value,
                    onCopy = {
                        val clipData = ClipData.newPlainText(parameter.key, parameter.value)
                        scope.launch { clipboard.setClipEntry(ClipEntry(clipData)) }
                    }
                )
                
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.detail_info_label), style = MaterialTheme.typography.labelSmall)
                        Spacer(Modifier.height(8.dp))
                        InfoRow(stringResource(R.string.detail_key_label), parameter.key)
                        InfoRow(stringResource(R.string.detail_category_label), parameter.category)
                    }
                }

                DetailCard(
                    stringResource(R.string.detail_raw_value_label),
                    parameter.rawValue,
                    onCopy = {
                        val clipData = ClipData.newPlainText(parameter.key, parameter.rawValue)
                        scope.launch { clipboard.setClipEntry(ClipEntry(clipData)) }
                    }
                )
            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text(stringResource(R.string.error_parameter_not_found))
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
                    Icon(Icons.Default.Info, contentDescription = stringResource(R.string.cd_copy))
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

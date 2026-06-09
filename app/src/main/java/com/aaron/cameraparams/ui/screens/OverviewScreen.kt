package com.aaron.cameraparams.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aaron.cameraparams.ui.CameraViewModel

@Composable
fun OverviewScreen(viewModel: CameraViewModel, onNavigateToCategories: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            CameraSelector(uiState.cameras, uiState.selectedCameraIndex) {
                viewModel.selectCamera(it)
            }
        }

        item {
            HardwareLevelCard(uiState.hardwareLevel)
        }

        item {
            KeyFeaturesSection(uiState.featureFlags)
        }

        item {
            Text("Quick Access", style = MaterialTheme.typography.titleMedium)
        }

        // List some categories as quick access
        uiState.categories.take(5).forEach { category ->
            item {
                CategoryRow(category.name, category.parameters.size, onNavigateToCategories)
            }
        }
    }
}

@Composable
fun CameraSelector(cameras: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }) {
            Icon(Icons.Default.PhotoCamera, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(
                if (cameras.isNotEmpty()) "Camera ${cameras[selectedIndex]}" else "No Camera",
                style = MaterialTheme.typography.titleMedium
            )
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            cameras.forEachIndexed { index, name ->
                DropdownMenuItem(
                    text = { Text("Camera $name") },
                    onClick = {
                        onSelect(index)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun HardwareLevelCard(level: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Hardware Level", style = MaterialTheme.typography.labelSmall)
                Text(level, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            }
            Icon(
                Icons.Default.VerifiedUser,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KeyFeaturesSection(features: Map<String, Boolean>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Key Features", style = MaterialTheme.typography.titleMedium)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            features.forEach { (label, supported) ->
                FeatureChip(label, supported)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeatureChip(label: String, supported: Boolean) {
    Surface(
        color = if (supported) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        border = if (supported) null else FilterChipDefaults.filterChipBorder(enabled = true, selected = false)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = if (supported) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CategoryRow(name: String, count: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                Text(count.toString())
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

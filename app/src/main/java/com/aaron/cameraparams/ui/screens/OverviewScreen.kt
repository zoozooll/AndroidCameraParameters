package com.aaron.cameraparams.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
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
            CameraSelector(uiState.cameraName, uiState.cameraId, uiState.cameras) {
                viewModel.selectCamera(it)
            }
        }

        item {
            SummaryCard(
                uiState.hardwareLevel,
                uiState.sensorResolution,
                uiState.maxFps
            )
        }

        item {
            KeyFeaturesSection(uiState.featureFlags)
        }

        item {
            Text("Categories", style = MaterialTheme.typography.titleMedium)
        }

        items(uiState.categories.size) { index ->
            val category = uiState.categories[index]
            CategoryRow(category.name, category.parameters.size, onNavigateToCategories)
        }
    }
}

@Composable
fun CameraSelector(name: String, id: String, cameras: List<String>, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .clickable { expanded = true }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Menu, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("ID: $id", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }

            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                cameras.forEachIndexed { index, camId ->
                    DropdownMenuItem(
                        text = { Text("Camera $camId") },
                        onClick = {
                            onSelect(index)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryCard(level: String, resolution: String, maxFps: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("HARDWARE LEVEL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(level, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                }
                HardwareLevelIcon(level)
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                SummaryStat(Modifier.weight(1f), "SENSOR", resolution)
                SummaryStat(Modifier.weight(1f), "MAX FPS", maxFps)
            }
        }
    }
}

@Composable
fun HardwareLevelIcon(level: String) {
    val (icon, color) = when {
        level.contains("LEVEL_3") -> Icons.Default.Security to Color(0xFF7B61FF)
        level.contains("FULL") -> Icons.Default.FiberManualRecord to Color(0xFF2196F3)
        level.contains("LIMITED") -> Icons.Default.Warning to Color(0xFFFFC107)
        else -> Icons.Default.History to Color.Gray
    }
    Icon(
        icon,
        contentDescription = null,
        modifier = Modifier.size(48.dp),
        tint = color
    )
}

@Composable
fun SummaryStat(modifier: Modifier, label: String, value: String) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KeyFeaturesSection(features: Map<String, Boolean>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
    val (icon, color) = when {
        name.contains("Sensor") -> Icons.Default.Sensors to Color(0xFF4CAF50)
        name.contains("Lens") -> Icons.Default.Lens to Color(0xFF2196F3)
        name.contains("AE") -> Icons.Default.Exposure to Color(0xFFFF9800)
        name.contains("AF") -> Icons.Default.FilterCenterFocus to Color(0xFF4CAF50)
        name.contains("AWB") -> Icons.Default.WbSunny to Color(0xFF9C27B0)
        name.contains("Output") -> Icons.Default.SettingsInputComponent to Color(0xFF2196F3)
        else -> Icons.Default.Folder to MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color)
            }
            
            Spacer(Modifier.width(16.dp))
            
            Text(
                name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    count.toString(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

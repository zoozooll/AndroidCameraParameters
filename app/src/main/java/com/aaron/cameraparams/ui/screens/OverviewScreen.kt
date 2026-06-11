package com.aaron.cameraparams.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aaron.cameraparams.ui.*
import com.aaron.cameraparams.ui.theme.CameraParamsTheme

@Composable
fun OverviewScreen(viewModel: CameraViewModel, onNavigateToDetail: (String) -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    OverviewScreenContent(
        overviewState = uiState.overview,
        parametersState = uiState.parameters,
        onIntent = { viewModel.handleIntent(it) },
        onNavigateToDetail = onNavigateToDetail
    )
}

@Composable
fun OverviewScreenContent(
    overviewState: CameraOverviewState,
    parametersState: CameraParametersState,
    onIntent: (CameraIntent) -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SummaryCard(overviewState)
        }

        item {
            KeyFeaturesSection(overviewState.featureFlags)
        }

        items(parametersState.categories.size) { index ->
            val category = parametersState.categories[index]
            CategoryRow(
                category = category,
                onToggleExpand = { onIntent(CameraIntent.ToggleCategory(category.name)) },
                onNavigateToDetail = onNavigateToDetail
            )
        }
    }
}

@Composable
fun SummaryCard(state: CameraOverviewState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "HARDWARE LEVEL",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        letterSpacing = 1.sp
                    )
                    Text(
                        state.hardwareLevel.replace("INFO_SUPPORTED_HARDWARE_LEVEL_", ""),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                HardwareLevelIcon(state.hardwareLevel)
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            SummaryStat(Modifier.weight(1f), "RESOLUTION", state.sensorResolution)
            SummaryStat(Modifier.weight(1f), "MAX FPS", state.maxFps)
        }
    }
}

@Composable
fun HardwareLevelIcon(level: String) {
    val color = Color(0xFF7B61FF) // Design purple
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.VerifiedUser, // Shield with check mark
            contentDescription = "Hardware Level: $level",
            modifier = Modifier.size(32.dp),
            tint = color
        )
    }
}

@Composable
fun SummaryStat(modifier: Modifier, label: String, value: String) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            letterSpacing = 1.sp
        )
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KeyFeaturesSection(features: Map<String, Boolean>) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "Key Features",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            maxItemsInEachRow = 4
        ) {
            features.forEach { (label, supported) ->
                FeatureChip(label, supported)
            }
        }
    }
}

@Composable
fun FeatureChip(label: String, supported: Boolean) {
    val (icon, iconColor) = when {
        label.contains("RAW", ignoreCase = true) -> Icons.Default.PhotoCamera to Color(0xFF7B61FF)
        label.contains("Exp", ignoreCase = true) -> Icons.Default.BrightnessMedium to Color(0xFF8BC34A)
        label.contains("Focus", ignoreCase = true) -> Icons.Default.FilterCenterFocus to Color(0xFF4CAF50)
        label.contains("Flash", ignoreCase = true) -> Icons.Default.FlashOn to Color(0xFFFFEB3B)
        label.contains("OIS", ignoreCase = true) -> Icons.Default.HdrStrong to Color(0xFF03A9F4)
        label.contains("Face", ignoreCase = true) -> Icons.Default.Face to Color(0xFF00BCD4)
        label.contains("HDR", ignoreCase = true) -> Icons.Default.HdrOn to Color(0xFF2196F3)
        label.contains("YUV", ignoreCase = true) -> Icons.Default.Refresh to Color(0xFF9C27B0)
        else -> Icons.Default.CheckCircle to Color.Gray
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp)
    ) {
        Surface(
            color = if (supported) Color(0xFF1E1F23) else Color(0xFF1E1F23).copy(alpha = 0.4f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.size(60.dp),
            border = if (supported) null else BorderStroke(1.dp, Color(0xFF2C2E33))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = if (supported) iconColor else Color(0xFF2C2E33)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            textAlign = TextAlign.Center,
            color = if (supported) Color.White else Color.White.copy(alpha = 0.4f),
            maxLines = 2,
            lineHeight = 12.sp
        )
    }
}

@Composable
fun CategoryRow(
    category: ParameterCategory,
    onToggleExpand: () -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    val name = category.name
    val count = category.parameters.size
    val (icon, color) = when {
        name.contains("Sensor") -> Icons.Default.Sensors to Color(0xFF4CAF50)
        name.contains("Lens") -> Icons.Default.Lens to Color(0xFF2196F3)
        name.contains("AE") -> Icons.Default.Exposure to Color(0xFFFF9800)
        name.contains("AF") -> Icons.Default.FilterCenterFocus to Color(0xFF4CAF50)
        name.contains("AWB") -> Icons.Default.WbSunny to Color(0xFF9C27B0)
        name.contains("Output") -> Icons.Default.SettingsInputComponent to Color(0xFF2196F3)
        else -> Icons.Default.Folder to Color(0xFF7B61FF)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onToggleExpand,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1F23)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(color.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                }
                
                Spacer(Modifier.width(16.dp))
                
                Text(
                    name.substringBefore("(").trim(),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                
                Surface(
                    color = Color(0xFF2C2E33),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        count.toString(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7B61FF)
                    )
                }
                
                Spacer(Modifier.width(8.dp))
                
                Icon(
                    if (category.expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color(0xFF2C2E33),
                    modifier = Modifier.size(20.dp)
                )
            }

            if (category.expanded) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = Color(0xFF2C2E33)
                )
                category.parameters.forEach { parameter ->
                    ParameterItem(parameter, onNavigateToDetail)
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun ParameterItem(parameter: CameraParameter, onClick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(parameter.key) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                parameter.key.substringAfterLast("."),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.9f)
            )
            Text(
                parameter.value,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF7B61FF),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFF2C2E33),
            modifier = Modifier.size(16.dp)
        )
    }
}



@Preview(showBackground = true)
@Composable
fun OverviewScreenPreview() {
    CameraParamsTheme {
        OverviewScreenContent(
            overviewState = CameraOverviewState(
                hardwareLevel = "LEVEL_3",
                sensorResolution = "4000 x 3000 (12.0 MP)",
                maxFps = "60 FPS",
                featureFlags = mapOf(
                    "Flash" to true,
                    "Manual Focus" to true,
                    "RAW Support" to false,
                    "Face Detection" to true
                )
            ),
            parametersState = CameraParametersState(
                categories = listOf(
                    ParameterCategory("Sensor Info", listOf(CameraParameter("key", "val", "raw", "cat"))),
                    ParameterCategory("Lens Settings", listOf(CameraParameter("key", "val", "raw", "cat"))),
                    ParameterCategory("AE Control", listOf(CameraParameter("key", "val", "raw", "cat")))
                )
            ),
            onIntent = {},
            onNavigateToDetail = {}
        )
    }
}

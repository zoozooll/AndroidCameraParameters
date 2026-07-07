package com.aaron.cameraparams.ui.screens

import android.telecom.Connection
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.aaron.cameraparams.R
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

@OptIn(ExperimentalLayoutApi::class)
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
                        stringResource(R.string.hardware_level_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        letterSpacing = 1.sp
                    )
                    Text(
                        state.hardwareLevel,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                HardwareLevelIcon(state.hardwareLevel)
            }
        }
    }
    
    val featureFlags = state.featureFlags
    var selectedFeature by remember { mutableStateOf<FeatureDetail?>(null) }
    
    // Resolve strings here to avoid calling @Composable inside onClick
    val resTitle = stringResource(R.string.feature_resolution)
    val fpsTitle = stringResource(R.string.feature_max_fps)
    val sizeTitle = stringResource(R.string.feature_sensor_size)
    val rawTitle = stringResource(R.string.feature_raw)
    val flashTitle = stringResource(R.string.feature_flash)
    val oisTitle = stringResource(R.string.feature_ois)
    val faceTitle = stringResource(R.string.feature_face_detection)
    val manualExpTitle = stringResource(R.string.feature_manual_exp)
    val manualFocusTitle = stringResource(R.string.feature_manual_focus)
    val hdrTitle = stringResource(R.string.feature_hdr)
    val yuvTitle = stringResource(R.string.feature_yuv_repro)
    val redEyeTitle = stringResource(R.string.feature_redeye)

    if (selectedFeature != null) {
        FeatureDetailDialog(
            feature = selectedFeature!!,
            onDismiss = { selectedFeature = null }
        )
    }

    Spacer(Modifier.height(6.dp))
    
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 3
    ) {
        val itemModifier = Modifier.weight(1f).fillMaxWidth(0.33f)

        // Text-based cards
        FeatureSummaryCard(
            modifier = itemModifier,
            painter = painterResource(R.drawable.ic_sensor),
            footerText = stringResource(R.string.feature_resolution),
            accentColor = Color(0xFF4CAF50),
            onClick = {
                selectedFeature = FeatureDetail(
                    title = resTitle,
                    description = "The total number of pixels on the camera sensor. Higher resolution allows for more detail in captured images.",
                    keyName = "android.sensor.info.pixelArraySize",
                    icon = IconSource.Resource(R.drawable.ic_sensor),
                    accentColor = Color(0xFF4CAF50)
                )
            }
        ) {
            FeatureValueContent(state.sensorResolution, state.sensorResolutionDetails)
        }
        
        FeatureSummaryCard(
            modifier = itemModifier,
            painter = painterResource(R.drawable.ic_video),
            footerText = stringResource(R.string.feature_max_fps),
            accentColor = Color(0xFF2196F3),
            onClick = {
                selectedFeature = FeatureDetail(
                    title = fpsTitle,
                    description = "The maximum number of frames per second the camera can capture. Higher FPS results in smoother video playback.",
                    keyName = "android.control.aeAvailableTargetFpsRanges",
                    icon = IconSource.Resource(R.drawable.ic_video),
                    accentColor = Color(0xFF2196F3)
                )
            }
        ) {
            FeatureValueContent(state.maxFps, state.maxFpsDetails)
        }

        FeatureSummaryCard(
            modifier = itemModifier,
            painter = painterResource(R.drawable.ic_sensor),
            footerText = stringResource(R.string.category_sensor),
            accentColor = Color(0xFFFF9800),
            onClick = {
                selectedFeature = FeatureDetail(
                    title = sizeTitle,
                    description = "The physical dimensions of the camera sensor. Larger sensors generally perform better in low-light conditions.",
                    keyName = "android.sensor.info.physicalSize",
                    icon = IconSource.Vector(Icons.Default.Straighten),
                    accentColor = Color(0xFFFF9800)
                )
            }
        ) {
            FeatureValueContent(sizeTitle, state.sensorPhysicalSize)
        }

        // Status-based cards
        FeatureSummaryCard(
            modifier = itemModifier,
            painter = painterResource(R.drawable.ic_raw_box),
            footerText = stringResource(R.string.feature_capture),
            accentColor = Color(0xFF7B61FF),
            onClick = {
                selectedFeature = FeatureDetail(
                    title = rawTitle,
                    description = "Support for capturing raw sensor data. RAW files contain more information and allow for greater flexibility in post-processing.",
                    keyName = "REQUEST_AVAILABLE_CAPABILITIES_RAW",
                    icon = IconSource.Resource(R.drawable.ic_raw_box),
                    accentColor = Color(0xFF7B61FF)
                )
            }
        ) {
            FeatureStatusContent(featureFlags["RAW"] == true, rawTitle)
        }

        FeatureSummaryCard(
            modifier = itemModifier,
            painter = painterResource(R.drawable.ic_flash_bolt),
            footerText = stringResource(R.string.feature_flash_feature),
            accentColor = Color(0xFFFFEB3B),
            onClick = {
                selectedFeature = FeatureDetail(
                    title = flashTitle,
                    description = "Indicates if the camera device has a built-in flash unit.",
                    keyName = "android.flash.info.available",
                    icon = IconSource.Resource(R.drawable.ic_flash_bolt),
                    accentColor = Color(0xFFFFEB3B)
                )
            }
        ) {
            FeatureStatusContent(featureFlags["Flash"] == true, flashTitle)
        }

        FeatureSummaryCard(
            modifier = itemModifier,
            painter = painterResource(R.drawable.ic_ois_hand),
            footerText = stringResource(R.string.feature_stabilization),
            accentColor = Color(0xFF00BCD4),
            onClick = {
                selectedFeature = FeatureDetail(
                    title = oisTitle,
                    description = "Optical Image Stabilization helps reduce blur caused by camera shake by physically moving the lens elements.",
                    keyName = "android.lens.info.availableOpticalStabilization",
                    icon = IconSource.Resource(R.drawable.ic_ois_hand),
                    accentColor = Color(0xFF00BCD4)
                )
            }
        ) {
            FeatureStatusContent(featureFlags["OIS"] == true, oisTitle)
        }

        FeatureSummaryCard(
            modifier = itemModifier,
            painter = painterResource(R.drawable.ic_face_detect_smile),
            footerText = stringResource(R.string.feature_ai_feature),
            accentColor = Color(0xFFFF4081),
            onClick = {
                selectedFeature = FeatureDetail(
                    title = faceTitle,
                    description = "The ability to detect faces within the camera's field of view, often used for improving focus and exposure on subjects.",
                    keyName = "android.statistics.info.availableFaceDetectModes",
                    icon = IconSource.Resource(R.drawable.ic_face_detect_smile),
                    accentColor = Color(0xFFFF4081)
                )
            }
        ) {
            FeatureStatusContent(featureFlags["Face Detection"] == true, faceTitle)
        }

        FeatureSummaryCard(
            modifier = itemModifier,
            painter = rememberVectorPainter(Icons.Default.BrightnessMedium),
            footerText = stringResource(R.string.category_ae),
            accentColor = Color(0xFF8BC34A),
            onClick = {
                selectedFeature = FeatureDetail(
                    title = manualExpTitle,
                    description = "Support for manually controlling exposure settings like shutter speed and ISO.",
                    keyName = "CONTROL_AE_MODE_OFF",
                    icon = IconSource.Vector(Icons.Default.BrightnessMedium),
                    accentColor = Color(0xFF8BC34A)
                )
            }
        ) {
            FeatureStatusContent(featureFlags["Manual Exp"] == true, manualExpTitle)
        }

        FeatureSummaryCard(
            modifier = itemModifier,
            painter = rememberVectorPainter(Icons.Default.FilterCenterFocus),
            footerText = stringResource(R.string.category_af),
            accentColor = Color(0xFF4CAF50),
            onClick = {
                selectedFeature = FeatureDetail(
                    title = manualFocusTitle,
                    description = "Support for manually controlling the lens focus distance.",
                    keyName = "android.lens.info.minimumFocusDistance",
                    icon = IconSource.Vector(Icons.Default.FilterCenterFocus),
                    accentColor = Color(0xFF4CAF50)
                )
            }
        ) {
            FeatureStatusContent(featureFlags["Manual Focus"] == true, manualFocusTitle)
        }

        FeatureSummaryCard(
            modifier = itemModifier,
            painter = rememberVectorPainter(Icons.Default.HdrOn),
            footerText = stringResource(R.string.category_ae),
            accentColor = Color(0xFF2196F3),
            onClick = {
                selectedFeature = FeatureDetail(
                    title = hdrTitle,
                    description = "High Dynamic Range mode captures multiple exposures and combines them to preserve detail in both highlights and shadows.",
                    keyName = "CONTROL_SCENE_MODE_HDR",
                    icon = IconSource.Vector(Icons.Default.HdrOn),
                    accentColor = Color(0xFF2196F3)
                )
            }
        ) {
            FeatureStatusContent(featureFlags["HDR"] == true, hdrTitle)
        }

        FeatureSummaryCard(
            modifier = itemModifier,
            painter = rememberVectorPainter(Icons.Default.Refresh),
            footerText = stringResource(R.string.category_output),
            accentColor = Color(0xFF9C27B0),
            onClick = {
                selectedFeature = FeatureDetail(
                    title = yuvTitle,
                    description = "Support for reprocessing YUV images, allowing for high-quality noise reduction and sharpening after capture.",
                    keyName = "REQUEST_AVAILABLE_CAPABILITIES_YUV_REPROCESSING",
                    icon = IconSource.Vector(Icons.Default.Refresh),
                    accentColor = Color(0xFF9C27B0)
                )
            }
        ) {
            FeatureStatusContent(featureFlags["YUV Reprocessing"] == true, yuvTitle)
        }

        FeatureSummaryCard(
            modifier = itemModifier,
            painter = rememberVectorPainter(Icons.Default.RemoveRedEye),
            footerText = stringResource(R.string.category_ae),
            accentColor = Color(0xFFF44336),
            onClick = {
                selectedFeature = FeatureDetail(
                    title = redEyeTitle,
                    description = "Support for red-eye reduction during flash capture.",
                    keyName = "CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE",
                    icon = IconSource.Vector(Icons.Default.RemoveRedEye),
                    accentColor = Color(0xFFF44336)
                )
            }
        ) {
            FeatureStatusContent(featureFlags["RedEye"] == true, redEyeTitle)
        }
    }
}

@Composable
fun FeatureValueContent(primary: String, secondary: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            primary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Text(
            secondary,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun FeatureStatusContent(supported: Boolean, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = if (supported) Icons.Default.CheckCircle else Icons.Default.RemoveCircleOutline,
            contentDescription = null,
            tint = if (supported) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.3f),
            modifier = Modifier.size(24.dp)
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (supported) Color.White else Color.White.copy(alpha = 0.4f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun FeatureSummaryCard(
    modifier: Modifier,
    painter: Painter,
    footerText: String,
    accentColor: Color,
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .height(140.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1F23)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(accentColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painter,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(48.dp)
                )
            }
            
            content()
            
            Text(
                footerText,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.3f),
                fontSize = 9.sp
            )
        }
    }
}

sealed class IconSource {
    data class Vector(val imageVector: ImageVector) : IconSource()
    data class Resource(@DrawableRes val resId: Int) : IconSource()
    
    @Composable
    fun rememberPainter(): Painter {
        return when (this) {
            is Vector -> rememberVectorPainter(imageVector)
            is Resource -> painterResource(resId)
        }
    }
}

data class FeatureDetail(
    val title: String,
    val description: String,
    val keyName: String,
    val icon: IconSource,
    val accentColor: Color
)

@Composable
fun FeatureDetailDialog(
    feature: FeatureDetail,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(feature.accentColor.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = feature.icon.rememberPainter(),
                    contentDescription = null,
                    tint = feature.accentColor,
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Text(
                feature.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    feature.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Camera2 API Key:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            feature.keyName,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        containerColor = Color(0xFF1E1F23),
        titleContentColor = Color.White,
        textContentColor = Color.White.copy(alpha = 0.7f)
    )
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
            contentDescription = stringResource(R.string.hardware_level_cd_format, level),
            modifier = Modifier.size(32.dp),
            tint = color
        )
    }
}

@Composable
fun FeatureChip(label: String, supported: Boolean) {
    val (painter, iconColor) = when {
        label.contains("RAW", ignoreCase = true) -> painterResource(R.drawable.ic_raw_box) to Color(0xFF7B61FF)
        label.contains("Exp", ignoreCase = true) -> rememberVectorPainter(Icons.Default.BrightnessMedium) to Color(0xFF8BC34A)
        label.contains("Focus", ignoreCase = true) -> rememberVectorPainter(Icons.Default.FilterCenterFocus) to Color(0xFF4CAF50)
        label.contains("Flash", ignoreCase = true) -> painterResource(R.drawable.ic_flash_bolt) to Color(0xFFFFEB3B)
        label.contains("OIS", ignoreCase = true) -> painterResource(R.drawable.ic_ois_hand) to Color(0xFF03A9F4)
        label.contains("Face", ignoreCase = true) -> painterResource(R.drawable.ic_face_detect_smile) to Color(0xFF00BCD4)
        label.contains("HDR", ignoreCase = true) -> rememberVectorPainter(Icons.Default.HdrOn) to Color(0xFF2196F3)
        label.contains("YUV", ignoreCase = true) -> rememberVectorPainter(Icons.Default.Refresh) to Color(0xFF9C27B0)
        else -> rememberVectorPainter(Icons.Default.CheckCircle) to Color.Gray
    }

    val displayName = when {
        label == "RAW" -> stringResource(R.string.feature_raw)
        label == "Manual Exp" -> stringResource(R.string.feature_manual_exp)
        label == "Manual Focus" -> stringResource(R.string.feature_manual_focus)
        label == "Flash" -> stringResource(R.string.feature_flash)
        label == "RedEye" -> stringResource(R.string.feature_redeye)
        label == "OIS" -> stringResource(R.string.feature_ois)
        label == "Face Detection" -> stringResource(R.string.feature_face_detection)
        label == "HDR" -> stringResource(R.string.feature_hdr)
        label == "YUV Reprocessing" -> stringResource(R.string.feature_yuv_repro)
        else -> label
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
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = if (supported) iconColor else Color(0xFF2C2E33)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            displayName,
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
    val (painter, color) = when {
        name.contains("Sensor") -> painterResource(R.drawable.ic_sensor) to Color(0xFF4CAF50)
        name.contains("Lens") -> rememberVectorPainter(Icons.Default.Lens) to Color(0xFF2196F3)
        name.contains("AE") -> rememberVectorPainter(Icons.Default.Exposure) to Color(0xFFFF9800)
        name.contains("AF") -> rememberVectorPainter(Icons.Default.FilterCenterFocus) to Color(0xFF4CAF50)
        name.contains("AWB") -> rememberVectorPainter(Icons.Default.WbSunny) to Color(0xFF9C27B0)
        name.contains("Output") -> rememberVectorPainter(Icons.Default.SettingsInputComponent) to Color(0xFF2196F3)
        else -> rememberVectorPainter(Icons.Default.Folder) to Color(0xFF7B61FF)
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
                    Icon(painter = painter, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
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
                sensorResolution = "12 MP",
                sensorResolutionDetails = "4000 x 3000",
                maxFps = "60 fps",
                maxFpsDetails = "1920x1080",
                featureFlags = mapOf(
                    "Flash" to true,
                    "Manual Focus" to true,
                    "RAW" to false,
                    "Face Detection" to true,
                    "OIS" to true
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

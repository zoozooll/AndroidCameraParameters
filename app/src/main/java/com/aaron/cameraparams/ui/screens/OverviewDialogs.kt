package com.aaron.cameraparams.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aaron.cameraparams.R
import com.aaron.cameraparams.ui.CameraOverviewState
import com.aaron.cameraparams.ui.theme.CameraParamsTheme

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

sealed class FeatureDialog {
    data object Resolution : FeatureDialog()
    data object VideoFps : FeatureDialog()
    data object SensorSize : FeatureDialog()
    data object Flash : FeatureDialog()
    data class Status(val feature: FeatureDetail, val supported: Boolean) : FeatureDialog()
}

data class FeatureDetail(
    val title: String,
    val description: String,
    val keyName: String,
    val icon: IconSource,
    val accentColor: Color
)

@Composable
fun ResolutionDetailDialog(state: CameraOverviewState, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ResolutionIcon()
                Text(stringResource(R.string.feature_resolution), fontWeight = FontWeight.Bold)
            }
        },
        text = { ResolutionDetailContent(state) },
        confirmButton = {}
    )
}

@Composable
fun VideoFpsDetailDialog(state: CameraOverviewState, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FpsIcon()
                Text(stringResource(R.string.feature_max_video_fps), fontWeight = FontWeight.Bold)
            }
        },
        text = { VideoFpsDetailContent(state) },
        confirmButton = {}
    )
}

@Composable
fun FlashDetailDialog(state: CameraOverviewState, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FlashIcon()
                Text(stringResource(R.string.feature_flash), fontWeight = FontWeight.Bold)
            }
        },
        text = { FlashDetailContent(state) },
        confirmButton = {}
    )
}

@Composable
fun SensorSizeDetailDialog(state: CameraOverviewState, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SensorSizeIcon()
                Text(stringResource(R.string.feature_sensor_size), fontWeight = FontWeight.Bold)
            }
        },
        text = { SensorSizeDetailContent(state) },
        confirmButton = {}
    )
}

@Composable
fun StatusDetailDialog(feature: FeatureDetail, supported: Boolean, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatusIcon(feature)
                Text(feature.title, fontWeight = FontWeight.Bold)
            }
        },
        text = { StatusDetailContent(feature, supported) },
        confirmButton = {}
    )
}

// --- Specialized Content Composables (Extracted for Previewing) ---

@Composable
fun ResolutionIcon() {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(Color(0xFF4CAF50).copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_sensor),
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun ResolutionDetailContent(state: CameraOverviewState) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column {
            Text(stringResource(R.string.dialog_primary_resolution), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(state.sensorResolution, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        Column {
            Text(stringResource(R.string.dialog_pixel_array_size), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(state.sensorResolutionDetails, style = MaterialTheme.typography.bodyLarge)
        }
        Text(
            stringResource(R.string.feature_resolution_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun FpsIcon() {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(Color(0xFF2196F3).copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_video),
            contentDescription = null,
            tint = Color(0xFF2196F3),
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun VideoFpsDetailContent(state: CameraOverviewState) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // High Speed Status Field
        Column {
            Text(
                stringResource(R.string.dialog_high_speed_recording),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(
                    imageVector = if (state.highSpeedVideoSupported) Icons.Default.CheckCircle else Icons.Default.RemoveCircle,
                    contentDescription = null,
                    tint = if (state.highSpeedVideoSupported) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (state.highSpeedVideoSupported) stringResource(R.string.status_supported) else stringResource(R.string.status_not_supported),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (state.highSpeedVideoSupported) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                )
            }
        }

        // FPS Field
        Column {
            Text(
                stringResource(if (state.highSpeedVideoSupported) R.string.dialog_max_frame_rate else R.string.feature_max_video_fps),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(state.maxFps, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }

        if (state.highSpeedVideoSupported && state.maxFpsDetails.isNotEmpty()) {
            Column {
                Text(stringResource(R.string.dialog_at_resolution), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(state.maxFpsDetails, style = MaterialTheme.typography.bodyLarge)
            }
        }

        Text(
            stringResource(R.string.feature_max_video_fps_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun FlashIcon() {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(Color(0xFFFFEB3B).copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_flash_bolt),
            contentDescription = null,
            tint = Color(0xFFFFEB3B),
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun FlashDetailContent(state: CameraOverviewState) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Flash Mode Statuses
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FlashModeRow(stringResource(R.string.mode_flash_auto), state.flashAutoSupported)
            FlashModeRow(stringResource(R.string.mode_flash_always), state.flashAlwaysSupported)
            FlashModeRow(stringResource(R.string.mode_flash_red_eye), state.redEyeReductionSupported)
        }

        Text(
            stringResource(R.string.feature_flash_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun FlashModeRow(label: String, supported: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (supported) Icons.Default.CheckCircle else Icons.Default.RemoveCircle,
            contentDescription = null,
            tint = if (supported) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (supported) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun SensorSizeIcon() {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(Color(0xFFFF9800).copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Straighten,
            contentDescription = null,
            tint = Color(0xFFFF9800),
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun SensorSizeDetailContent(state: CameraOverviewState) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column {
            Text(stringResource(R.string.dialog_physical_dimensions), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(state.sensorPhysicalSize, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        Text(
            stringResource(R.string.feature_sensor_size_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun StatusIcon(feature: FeatureDetail) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(feature.accentColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = feature.icon.rememberPainter(),
            contentDescription = null,
            tint = feature.accentColor,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun StatusDetailContent(feature: FeatureDetail, supported: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                imageVector = if (supported) Icons.Default.CheckCircle else Icons.Default.RemoveCircle,
                contentDescription = null,
                tint = if (supported) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            Text(
                if (supported) stringResource(R.string.status_supported) else stringResource(R.string.status_not_supported),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (supported) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
            )
        }
        Text(feature.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// --- Previews ---

@Preview(showBackground = true)
@Composable
fun ResolutionDetailPreview() {
    CameraParamsTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ResolutionIcon()
                    Text(stringResource(R.string.feature_resolution), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(16.dp))
                ResolutionDetailContent(CameraOverviewState(sensorResolution = "12 MP", sensorResolutionDetails = "4000 x 3000"))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VideoFpsDetailPreview() {
    CameraParamsTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FpsIcon()
                    Text(stringResource(R.string.feature_max_video_fps), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(16.dp))
                VideoFpsDetailContent(CameraOverviewState(
                    maxFps = "240 FPS", 
                    maxFpsDetails = "1920x1080",
                    highSpeedVideoSupported = true
                ))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FlashDetailPreview() {
    CameraParamsTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FlashIcon()
                    Text(stringResource(R.string.feature_flash), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(16.dp))
                FlashDetailContent(CameraOverviewState(
                    flashAutoSupported = true,
                    flashAlwaysSupported = true,
                    redEyeReductionSupported = false
                ))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StatusDetailPreview() {
    CameraParamsTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            val feature = FeatureDetail(
                title = "RAW Support",
                description = "Support for capturing raw sensor data. RAW files contain more information.",
                keyName = "REQUEST_AVAILABLE_CAPABILITIES_RAW",
                icon = IconSource.Vector(Icons.Default.Camera),
                accentColor = Color(0xFF7B61FF)
            )
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatusIcon(feature)
                    Text(feature.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(16.dp))
                StatusDetailContent(feature, supported = true)
            }
        }
    }
}

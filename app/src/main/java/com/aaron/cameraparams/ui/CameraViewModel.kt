package com.aaron.cameraparams.ui

import android.app.Application
 import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aaron.cameraparams.CameraParamsHelper
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.TreeMap

data class CameraParameter(
    val key: String,
    val value: String,
    val rawValue: String,
    val category: String,
    val description: String = ""
)

data class ParameterCategory(
    val name: String,
    val parameters: List<CameraParameter>,
    val expanded: Boolean = false
)

data class CameraHeaderState(
    val cameras: List<String> = emptyList(),
    val selectedCameraIndex: Int = 0,
    val cameraName: String = "",
    val cameraId: String = ""
)

data class CameraOverviewState(
    val hardwareLevel: String = "",
    val sensorResolution: String = "",
    val sensorResolutionDetails: String = "",
    val maxFps: String = "",
    val maxFpsDetails: String = "",
    val featureFlags: Map<String, Boolean> = emptyMap()
)

data class CameraParametersState(
    val categories: List<ParameterCategory> = emptyList(),
    val searchQuery: String = "",
    val filteredCategories: List<ParameterCategory> = emptyList()
)

data class UiState(
    val header: CameraHeaderState = CameraHeaderState(),
    val overview: CameraOverviewState = CameraOverviewState(),
    val parameters: CameraParametersState = CameraParametersState(),
    val rawJson: String = ""
)

sealed class CameraIntent {
    data class SelectCamera(val index: Int) : CameraIntent()
    data class ToggleCategory(val name: String) : CameraIntent()
    data class UpdateSearchQuery(val query: String) : CameraIntent()
}

class CameraViewModel(application: Application) : AndroidViewModel(application) {
    private val cameraManager = application.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val helper = CameraParamsHelper(application)
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        val cameras = cameraManager.cameraIdList.toList()
        _uiState.value = _uiState.value.copy(
            header = _uiState.value.header.copy(cameras = cameras)
        )
        if (cameras.isNotEmpty()) {
            handleIntent(CameraIntent.SelectCamera(0))
        }
    }

    fun handleIntent(intent: CameraIntent) {
        when (intent) {
            is CameraIntent.SelectCamera -> selectCamera(intent.index)
            is CameraIntent.ToggleCategory -> toggleCategoryExpansion(intent.name)
            is CameraIntent.UpdateSearchQuery -> onSearchQueryChange(intent.query)
        }
    }

    private fun selectCamera(index: Int) {
        viewModelScope.launch {
            val cameraId = uiState.value.header.cameras.getOrNull(index) ?: return@launch
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            updateParameters(characteristics, cameraId, index)
        }
    }

    private fun updateParameters(chars: CameraCharacteristics, cameraId: String, index: Int) {
        val keys = chars.keys
        val params = keys.mapNotNull { key ->
            val value = chars.get(key)
            @Suppress("UNCHECKED_CAST")
            val formattedValue = helper.keyValue(key as CameraCharacteristics.Key<Any?>, value)
            val rawValue = value?.toString() ?: "null"
            val category = getCategoryForKey(key.name)
            CameraParameter(key.name, formattedValue, rawValue, category)
        }

        val categoriesMap = params.groupBy { it.category }
        val categoryOrder = listOf("Sensor", "Lens", "AE (Auto Exposure)", "AF (Auto Focus)",
            "AWB (White Balance)", "Output", "Request", "Statistics", "Other")
        
        val categories = categoryOrder.map { name ->
            ParameterCategory(name, categoriesMap[name] ?: emptyList())
        }.filter { it.parameters.isNotEmpty() }

        val hardwareLevel = helper.keyValue(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL as CameraCharacteristics.Key<Any?>,
            chars.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
        )
        
        val pixelArray = chars.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
        val sensorRes = if (pixelArray != null) {
            val mp = (pixelArray.width.toLong() * pixelArray.height.toLong()) / 1_000_000.0
            "%.0f MP".format(mp)
        } else "N/A"

        val sensorResSum = if (pixelArray != null) {
            "${pixelArray.width}x${pixelArray.height}"
        } else "N/A"

        val fpsRanges = chars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
        val maxFpsVal = fpsRanges?.maxByOrNull { it.upper }?.upper ?: 0
        
        val facing = chars.get(CameraCharacteristics.LENS_FACING)
        val cameraName = when (facing) {
            CameraCharacteristics.LENS_FACING_BACK -> "Rear Camera"
            CameraCharacteristics.LENS_FACING_FRONT -> "Front Camera"
            CameraCharacteristics.LENS_FACING_EXTERNAL -> "External Camera"
            else -> "Unknown Camera"
        }

        _uiState.value = _uiState.value.copy(
            header = CameraHeaderState(
                cameras = _uiState.value.header.cameras,
                selectedCameraIndex = index,
                cameraName = cameraName,
                cameraId = cameraId
            ),
            overview = CameraOverviewState(
                hardwareLevel = hardwareLevel,
                sensorResolution = sensorRes,
                sensorResolutionDetails = sensorResSum,
                maxFps = "$maxFpsVal fps",
                maxFpsDetails = if (pixelArray != null) "1920x1080" else "N/A", // Placeholder for common video res
                featureFlags = detectFeatureFlags(chars)
            ),
            parameters = CameraParametersState(
                categories = categories,
                filteredCategories = categories,
                searchQuery = _uiState.value.parameters.searchQuery
            ),
            rawJson = generateRawJson(chars)
        )
    }

    private fun detectFeatureFlags(chars: CameraCharacteristics): Map<String, Boolean> {
        val capabilities = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
        val rawSupport = capabilities?.contains(
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW
        ) ?: false
        
        val yuvReprocessing = capabilities?.contains(
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_YUV_REPROCESSING
        ) ?: false
        
        val manualExp = chars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES)?.contains(
            CameraCharacteristics.CONTROL_AE_MODE_OFF
        ) ?: false
        
        val minFocusDist = chars.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0f
        val manualFocus = minFocusDist > 0
        
        val flashAvailable = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
        
        val oisAvailable = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)?.contains(
            CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_ON
        ) ?: false
        
        val faceModes = chars.get(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES)
        val faceDetection = faceModes?.any { 
            it == CameraCharacteristics.STATISTICS_FACE_DETECT_MODE_SIMPLE || 
            it == CameraCharacteristics.STATISTICS_FACE_DETECT_MODE_FULL 
        } ?: false

        val sceneModes = chars.get(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES)
        val hdrSupport = sceneModes?.contains(CameraCharacteristics.CONTROL_SCENE_MODE_HDR) ?: false

        return mapOf(
            "RAW" to rawSupport,
            "Manual Exp" to manualExp,
            "Manual Focus" to manualFocus,
            "Flash" to flashAvailable,
            "RedEye" to false,
            "OIS" to oisAvailable,
            "Face Detection" to faceDetection,
            "HDR" to hdrSupport,
            "YUV Reprocessing" to yuvReprocessing
        )
    }

    private fun getCategoryForKey(keyName: String): String {
        return when {
            keyName.contains("sensor", ignoreCase = true) -> "Sensor"
            keyName.contains("lens", ignoreCase = true) -> "Lens"
            keyName.contains("control.ae", ignoreCase = true) -> "AE (Auto Exposure)"
            keyName.contains("control.af", ignoreCase = true) -> "AF (Auto Focus)"
            keyName.contains("control.awb", ignoreCase = true) -> "AWB (White Balance)"
            keyName.contains("scaler", ignoreCase = true) || keyName.contains("reprocess", ignoreCase = true) -> "Output"
            keyName.contains("request", ignoreCase = true) -> "Request"
            keyName.contains("statistics", ignoreCase = true) -> "Statistics"
            else -> "Other"
        }
    }

    private fun onSearchQueryChange(query: String) {
        val filtered = if (query.isEmpty()) {
            _uiState.value.parameters.categories
        } else {
            _uiState.value.parameters.categories.map { category ->
                category.copy(parameters = category.parameters.filter { 
                    it.key.contains(query, ignoreCase = true) || it.value.contains(query, ignoreCase = true)
                })
            }.filter { it.parameters.isNotEmpty() }
        }
        _uiState.value = _uiState.value.copy(
            parameters = _uiState.value.parameters.copy(
                searchQuery = query,
                filteredCategories = filtered
            )
        )
    }

    private fun toggleCategoryExpansion(categoryName: String) {
        val currentCategories = _uiState.value.parameters.categories
        val updatedCategories = currentCategories.map { 
            if (it.name == categoryName) it.copy(expanded = !it.expanded) else it
        }
        
        // Also update filtered categories if they are being displayed
        val updatedFiltered = _uiState.value.parameters.filteredCategories.map {
            if (it.name == categoryName) it.copy(expanded = !it.expanded) else it
        }
        
        _uiState.value = _uiState.value.copy(
            parameters = _uiState.value.parameters.copy(
                categories = updatedCategories,
                filteredCategories = updatedFiltered
            )
        )
    }

    private fun generateRawJson(chars: CameraCharacteristics): String {
        val map = TreeMap<String, Any?>()
        chars.keys.forEach { key ->
            map[key.name] = chars.get(key)
        }
        val gson = GsonBuilder().setPrettyPrinting().create()
        return gson.toJson(map)
    }
}

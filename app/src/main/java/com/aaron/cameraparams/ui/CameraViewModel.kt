package com.aaron.cameraparams.ui

import android.app.Application
import android.hardware.camera2.CameraCharacteristics
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

data class UiState(
    val cameras: List<String> = emptyList(),
    val selectedCameraIndex: Int = 0,
    val cameraName: String = "",
    val cameraId: String = "",
    val hardwareLevel: String = "",
    val sensorResolution: String = "",
    val maxFps: String = "",
    val categories: List<ParameterCategory> = emptyList(),
    val searchQuery: String = "",
    val filteredCategories: List<ParameterCategory> = emptyList(),
    val rawJson: String = "",
    val featureFlags: Map<String, Boolean> = emptyMap()
)

class CameraViewModel(application: Application) : AndroidViewModel(application) {
    private val helper = CameraParamsHelper(application)
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        val cameras = helper.supportCameraIds.filterNotNull()
        _uiState.value = _uiState.value.copy(cameras = cameras)
        if (cameras.isNotEmpty()) {
            selectCamera(0)
        }
    }

    fun selectCamera(index: Int) {
        viewModelScope.launch {
            helper.bindCameraId(index)
            updateParameters(index)
        }
    }

    private fun updateParameters(index: Int) {
        val keys = helper.availableKeys
        val params = keys.mapNotNull { key ->
            if (key == null) return@mapNotNull null
            val value = helper.getCharacteristicInfo(key)
            val rawValue = helper.getCharacteristic(key)?.toString() ?: "null"
            val category = getCategoryForKey(key.name)
            CameraParameter(key.name, value, rawValue, category)
        }

        val categoriesMap = params.groupBy { it.category }
        val categoryOrder = listOf("Sensor", "Lens", "AE (Auto Exposure)", "AF (Auto Focus)", "AWB (White Balance)", "Output", "Request", "Statistics", "Other")
        
        val categories = categoryOrder.map { name ->
            ParameterCategory(name, categoriesMap[name] ?: emptyList())
        }.filter { it.parameters.isNotEmpty() }

        val hardwareLevel = helper.getCharacteristicInfo(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
        
        val chars = helper.getCameraCharacteristics()
        val pixelArray = chars?.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
        val sensorRes = if (pixelArray != null) {
            val mp = (pixelArray.width.toLong() * pixelArray.height.toLong()) / 1_000_000.0
            "%.1f MP".format(mp)
        } else "N/A"

        val fpsRanges = chars?.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
        val maxFpsVal = fpsRanges?.maxByOrNull { it.upper }?.upper ?: 0
        
        val facing = chars?.get(CameraCharacteristics.LENS_FACING)
        val cameraName = when (facing) {
            CameraCharacteristics.LENS_FACING_BACK -> "Rear Camera"
            CameraCharacteristics.LENS_FACING_FRONT -> "Front Camera"
            CameraCharacteristics.LENS_FACING_EXTERNAL -> "External Camera"
            else -> "Unknown Camera"
        }
        val cameraId = helper.supportCameraIds.getOrNull(index) ?: ""

        _uiState.value = _uiState.value.copy(
            selectedCameraIndex = index,
            cameraName = cameraName,
            cameraId = cameraId,
            hardwareLevel = hardwareLevel,
            sensorResolution = sensorRes,
            maxFps = "$maxFpsVal fps",
            categories = categories,
            filteredCategories = categories,
            rawJson = generateRawJson(),
            featureFlags = detectFeatureFlags()
        )
    }

    private fun detectFeatureFlags(): Map<String, Boolean> {
        val chars = helper.getCameraCharacteristics() ?: return emptyMap()
        
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

    fun onSearchQueryChange(query: String) {
        val filtered = if (query.isEmpty()) {
            _uiState.value.categories
        } else {
            _uiState.value.categories.map { category ->
                category.copy(parameters = category.parameters.filter { 
                    it.key.contains(query, ignoreCase = true) || it.value.contains(query, ignoreCase = true)
                })
            }.filter { it.parameters.isNotEmpty() }
        }
        _uiState.value = _uiState.value.copy(searchQuery = query, filteredCategories = filtered)
    }

    private fun generateRawJson(): String {
        val characteristics = helper.getCameraCharacteristics() ?: return "{}"
        val map = TreeMap<String, Any?>()
        helper.availableKeys.forEach { key ->
            if (key != null) {
                map[key.name] = helper.getCharacteristic(key)
            }
        }
        val gson = GsonBuilder().setPrettyPrinting().create()
        return gson.toJson(map)
    }
}

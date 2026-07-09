package com.aaron.cameraparams.ui

import android.app.Application
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aaron.cameraparams.CameraParamsHelper
import com.aaron.cameraparams.R
import com.aaron.cameraparams.camera.getAeAvailableModes
import com.aaron.cameraparams.camera.getAfAvailableModes
import com.aaron.cameraparams.camera.getAvailableEffects
import com.aaron.cameraparams.camera.getAvailableModes
import com.aaron.cameraparams.camera.getAvailableNoiseReductionModes
import com.aaron.cameraparams.camera.getAvailableSceneModes
import com.aaron.cameraparams.camera.getAwbAvailableModes
import com.aaron.cameraparams.camera.getColorCorrectionAvailableAberrationMode
import com.aaron.cameraparams.camera.getHardwareLevelInfo
import com.aaron.cameraparams.camera.getRequestAvailableCapabilities
import com.aaron.cameraparams.camera.streamConfigurationMapToString
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Arrays
import java.util.TreeMap
import kotlin.String

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
    val sensorPhysicalSize: String = "",
    val maxFps: String = "",
    val maxFpsDetails: String = "",
    val rawFormatSupported: Boolean = false,
    val autoFlashSupported: Boolean = false,
    val oisSupported: Boolean = false,
    val faceDetectionSupported: Boolean = false,
    val manualExpSupported: Boolean = false,
    val manualFocusSupported: Boolean = false,
    val hdrSupported: Boolean = false,
    val yuvReprocessingSupported: Boolean = false,
    val redEyeReductionSupported: Boolean = false,
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
            withContext(Dispatchers.Default) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)

                val keyList = characteristics.getKeys()
                Log.i(TAG, "selectCamera [$index]: ")
                keyList.forEach {
                    val value = getCharacteristicInfo(characteristics, it)
                    Log.i(TAG, "CharacteristicInfo( [$it]: $value")
                }
                updateParameters(characteristics, cameraId, index)
            }
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
        val categoryOrder = listOf(
            getApplication<Application>().getString(R.string.category_sensor),
            getApplication<Application>().getString(R.string.category_lens),
            getApplication<Application>().getString(R.string.category_ae),
            getApplication<Application>().getString(R.string.category_af),
            getApplication<Application>().getString(R.string.category_awb),
            getApplication<Application>().getString(R.string.category_output),
            getApplication<Application>().getString(R.string.category_request),
            getApplication<Application>().getString(R.string.category_statistics),
            getApplication<Application>().getString(R.string.category_other)
        )
        
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

        val physicalSize = chars.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
        val sensorPhysicalSize = if (physicalSize != null) {
            "%.1fx%.1f".format(physicalSize.width, physicalSize.height)
        } else "N/A"

        val fpsRanges = chars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
        val maxFpsVal = fpsRanges?.maxByOrNull { it.upper }?.upper ?: 0
        
        val facing = chars.get(CameraCharacteristics.LENS_FACING)
        val cameraName = when (facing) {
            CameraCharacteristics.LENS_FACING_BACK -> getApplication<Application>().getString(R.string.camera_facing_back)
            CameraCharacteristics.LENS_FACING_FRONT -> getApplication<Application>().getString(R.string.camera_facing_front)
            CameraCharacteristics.LENS_FACING_EXTERNAL -> getApplication<Application>().getString(R.string.camera_facing_external)
            else -> getApplication<Application>().getString(R.string.camera_facing_unknown)
        }

        val streamConfiguration: StreamConfigurationMap? = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val ranges = streamConfiguration?.highSpeedVideoFpsRanges
        
        var maxFps = ""
        var maxFpsDetails = ""
        
        if (ranges != null) {
            val largestRange = ranges.maxWithOrNull(compareBy({ it.upper }, { it.lower }))
            if (largestRange != null) {
                maxFps = "${largestRange.upper} FPS"
                val videoSizes = streamConfiguration.getHighSpeedVideoSizesFor(largestRange)
                val maxVideoSize = videoSizes?.maxByOrNull { it.width.toLong() * it.height.toLong() }
                
                if (maxVideoSize != null) {
                    maxFpsDetails = "${maxVideoSize.width}x${maxVideoSize.height}"
                }
            }
        }

        if ("" == maxFps) {
            val aeRanges = chars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
            val maxRange = aeRanges?.maxWithOrNull(compareBy({ it.upper }, { it.lower }))
            if (maxRange != null) {
                maxFps = "${maxRange.upper} FPS"
            }
            maxFpsDetails = ""
        }

        val features = detectFeatureFlags(chars)

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
                sensorPhysicalSize = sensorPhysicalSize,
                maxFps = maxFps,
                maxFpsDetails = maxFpsDetails,
                rawFormatSupported = features["RAW"] ?: false,
                autoFlashSupported = features["Flash"] ?: false,
                oisSupported = features["OIS"] ?: false,
                faceDetectionSupported = features["Face Detection"] ?: false,
                manualExpSupported = features["Manual Exp"] ?: false,
                manualFocusSupported = features["Manual Focus"] ?: false,
                hdrSupported = features["HDR"] ?: false,
                yuvReprocessingSupported = features["YUV Reprocessing"] ?: false,
                redEyeReductionSupported = features["RedEye"] ?: false,
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

        val aeModes = chars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES)
        val redEyeSupport = aeModes?.contains(
            CameraCharacteristics.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE
        ) ?: false

        return mapOf(
            "RAW" to rawSupport,
            "Manual Exp" to manualExp,
            "Manual Focus" to manualFocus,
            "Flash" to flashAvailable,
            "RedEye" to redEyeSupport,
            "OIS" to oisAvailable,
            "Face Detection" to faceDetection,
            "HDR" to hdrSupport,
            "YUV Reprocessing" to yuvReprocessing
        )
    }

    private fun getCategoryForKey(keyName: String): String {
        return when {
            keyName.contains("sensor", ignoreCase = true) -> getApplication<Application>().getString(R.string.category_sensor)
            keyName.contains("lens", ignoreCase = true) -> getApplication<Application>().getString(R.string.category_lens)
            keyName.contains("control.ae", ignoreCase = true) -> getApplication<Application>().getString(R.string.category_ae)
            keyName.contains("control.af", ignoreCase = true) -> getApplication<Application>().getString(R.string.category_af)
            keyName.contains("control.awb", ignoreCase = true) -> getApplication<Application>().getString(R.string.category_awb)
            keyName.contains("scaler", ignoreCase = true) || keyName.contains("reprocess", ignoreCase = true) -> getApplication<Application>().getString(R.string.category_output)
            keyName.contains("request", ignoreCase = true) -> getApplication<Application>().getString(R.string.category_request)
            keyName.contains("statistics", ignoreCase = true) -> getApplication<Application>().getString(R.string.category_statistics)
            else -> getApplication<Application>().getString(R.string.category_other)
        }
    }

    private fun onSearchQueryChange(query: String) {
        viewModelScope.launch {
            val filtered = withContext(Dispatchers.Default) {
                if (query.isEmpty()) {
                    _uiState.value.parameters.categories
                } else {
                    _uiState.value.parameters.categories.map { category ->
                        category.copy(parameters = category.parameters.filter {
                            it.key.contains(query, ignoreCase = true) || it.value.contains(query, ignoreCase = true)
                        })
                    }.filter { it.parameters.isNotEmpty() }
                }
            }
            _uiState.value = _uiState.value.copy(
                parameters = _uiState.value.parameters.copy(
                    searchQuery = query,
                    filteredCategories = filtered
                )
            )
        }
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

    fun <T> getCharacteristicInfo(characteristics: CameraCharacteristics, key: CameraCharacteristics.Key<T>): String {
        val value = characteristics.get(key)
        return keyValueToString(key, value)
    }

    fun <T> keyValueToString(key: CameraCharacteristics.Key<T>, value: T?): String {
        if (CameraCharacteristics.COLOR_CORRECTION_AVAILABLE_ABERRATION_MODES == key) {
            return getColorCorrectionAvailableAberrationMode(getApplication<Application>().applicationContext, (value as kotlin.IntArray?)!!)
        } else if (CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL == key) {
            return getHardwareLevelInfo(getApplication<Application>().applicationContext, (value as kotlin.Int?)!!)
        } else if (CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES == key) {
            return getAeAvailableModes(getApplication<Application>().applicationContext, (value as kotlin.IntArray?)!!)
        } else if (CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES == key) {
            return getAfAvailableModes(getApplication<Application>().applicationContext, (value as kotlin.IntArray?)!!)
        } else if (CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES == key) {
            return getAwbAvailableModes(getApplication<Application>().applicationContext, (value as kotlin.IntArray?)!!)
        } else if (CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS == key) {
            return getAvailableEffects(getApplication<Application>().applicationContext, (value as kotlin.IntArray?)!!)
        } else if (CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES == key) {
            return getAvailableSceneModes(getApplication<Application>().applicationContext, (value as kotlin.IntArray?)!!)
        } else if (CameraCharacteristics.NOISE_REDUCTION_AVAILABLE_NOISE_REDUCTION_MODES == key) {
            return getAvailableNoiseReductionModes(getApplication<Application>().applicationContext, (value as kotlin.IntArray?)!!)
        } else if (CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES == key) {
            return getRequestAvailableCapabilities((value as kotlin.IntArray?)!!)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && CameraCharacteristics.CONTROL_AVAILABLE_MODES == key) {
            return getAvailableModes(getApplication<Application>().applicationContext, (value as kotlin.IntArray?)!!)
        } else if (value is IntArray) {
            return (((value as IntArray).contentToString()))
        } else if (value is FloatArray) {
            return (((value as FloatArray).contentToString()))
        } else if (value is BooleanArray) {
            return (((value as BooleanArray).contentToString()))
        } else if (value is Array<*>) {
            return (((value as Array<Any?>).contentToString()))
        } else if (value is StreamConfigurationMap) {
            return streamConfigurationMapToString(value as StreamConfigurationMap)
        } else {
            return value.toString()
        }
    }

    companion object {
        private const val TAG = "CameraViewModel"
    }
}

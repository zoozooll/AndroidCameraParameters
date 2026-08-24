package com.aaron.cameraparams.ui

import android.app.Application
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.StreamConfigurationMap
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
import com.aaron.cameraparams.camera.getMandatoryStreamCombinationsString
import com.aaron.cameraparams.camera.getRequestAvailableCapabilities
import com.aaron.cameraparams.camera.streamConfigurationMapToString
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Date
import java.util.Locale
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

/** A camera registered from an imported raw JSON dump rather than the Camera2 API. */
data class ImportedCamera(
    val name: String,
    val json: String,
    val categories: List<ParameterCategory>
)

data class CameraHeaderState(
    val cameras: List<String> = emptyList(),
    val importedCameras: List<ImportedCamera> = emptyList(),
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
    val highSpeedVideoSupported: Boolean = false,
    val rawFormatSupported: Boolean = false,
    val autoFlashSupported: Boolean = false,
    val flashAutoSupported: Boolean = false,
    val flashAlwaysSupported: Boolean = false,
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
        loadPersistedImportedCameras()
    }

    fun handleIntent(intent: CameraIntent) {
        when (intent) {
            is CameraIntent.SelectCamera -> selectCamera(intent.index)
            is CameraIntent.ToggleCategory -> toggleCategoryExpansion(intent.name)
            is CameraIntent.UpdateSearchQuery -> onSearchQueryChange(intent.query)
        }
    }

    private fun selectCamera(index: Int) {
        val header = _uiState.value.header
        val importedIndex = index - header.cameras.size
        if (importedIndex >= 0) {
            val imported = header.importedCameras.getOrNull(importedIndex) ?: return
            applyImportedCamera(index, imported)
            return
        }
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

    private fun applyImportedCamera(index: Int, imported: ImportedCamera) {
        _uiState.update { current ->
            current.copy(
                header = current.header.copy(
                    selectedCameraIndex = index,
                    cameraName = imported.name,
                    cameraId = "I${index - current.header.cameras.size + 1}"
                ),
                overview = buildOverviewFromParams(imported.categories),
                parameters = CameraParametersState(
                    categories = imported.categories,
                    filteredCategories = imported.categories,
                    searchQuery = ""
                ),
                rawJson = imported.json
            )
        }
    }

    private fun updateParameters(chars: CameraCharacteristics, cameraId: String, index: Int) {
        val keys = chars.keys
        val params = keys.mapNotNull { key ->
            val value = chars.get(key)
            @Suppress("UNCHECKED_CAST")
            val formattedValue = keyValueToString(key as CameraCharacteristics.Key<Any?>, value)
            
            // For complex objects like MandatoryStreamCombination, use the formatted value as rawValue too
            val rawValue = if (value is Array<*> && value.isNotEmpty() && value[0]?.javaClass?.name?.contains("MandatoryStreamCombination") == true) {
                formattedValue
            } else {
                value?.toString() ?: "null"
            }
            
            val category = getCategoryForKey(key.name)
            CameraParameter(key.name, formattedValue, rawValue, category)
        }

        val categories = buildOrderedCategories(params)

        val hardwareLevel = keyValueToString(
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
        var highSpeedVideoSupported = false
        
        if (ranges != null && ranges.isNotEmpty()) {
            highSpeedVideoSupported = true
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
        val flashAuto = aeModes?.contains(
            CameraCharacteristics.CONTROL_AE_MODE_ON_AUTO_FLASH
        ) ?: false
        val flashAlways = aeModes?.contains(
            CameraCharacteristics.CONTROL_AE_MODE_ON_ALWAYS_FLASH
        ) ?: false
        val redEyeSupport = aeModes?.contains(
            CameraCharacteristics.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE
        ) ?: false

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
                highSpeedVideoSupported = highSpeedVideoSupported,
                rawFormatSupported = rawSupport,
                autoFlashSupported = flashAvailable,
                flashAutoSupported = flashAuto,
                flashAlwaysSupported = flashAlways,
                oisSupported = oisAvailable,
                faceDetectionSupported = faceDetection,
                manualExpSupported = manualExp,
                manualFocusSupported = manualFocus,
                hdrSupported = hdrSupport,
                yuvReprocessingSupported = yuvReprocessing,
                redEyeReductionSupported = redEyeSupport,
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
        val map = TreeMap<String, String>()
        chars.keys.forEach { key ->
            // Collapse newlines/extra whitespace into single spaces so the raw JSON
            // stays clean. Detail screens keep using the original multi-line value.
            map[key.name] = getCharacteristicInfo(chars, key)
                .replace(Regex("\\s+"), " ")
                .trim()
        }
        val gson = GsonBuilder().setPrettyPrinting().create()
        return gson.toJson(map)
    }

    /**
     * Imports a camera from a raw JSON string in the format produced by
     * [generateRawJson]. On success the dump is persisted and registered as an
     * extra entry in the camera selector.
     *
     * @return true when the JSON was parsed and the camera registered.
     */
    fun importCameraFromText(name: String, json: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            onResult(importCameraInternal(name.trim(), json))
        }
    }

    /**
     * Imports a camera from a JSON file picked via SAF. The file name (without
     * extension) becomes the camera name unless [nameOverride] is non-blank.
     *
     * @return true when the file was read, parsed and the camera registered.
     */
    fun importCameraFromUri(uri: Uri, nameOverride: String?, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val content = runCatching {
                withContext(Dispatchers.IO) {
                    val text = getApplication<Application>().contentResolver
                        .openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    if (text == null) null else text to queryDisplayName(uri)
                }
            }.getOrNull()
            val name = nameOverride?.takeIf { it.isNotBlank() } ?: content?.second
            onResult(content != null && importCameraInternal(name?.trim(), content.first))
        }
    }

    /** Removes an imported camera and deletes its persisted file. */
    fun removeImportedCamera(index: Int) {
        val imported = _uiState.value.header.importedCameras.getOrNull(index) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                File(File(getApplication<Application>().filesDir, IMPORT_DIR), safeFileName(imported.name) + ".json").delete()
            }.onFailure { Log.e(TAG, "Failed to delete imported camera file", it) }
        }
        _uiState.update { current ->
            current.copy(
                header = current.header.copy(
                    importedCameras = current.header.importedCameras.filterIndexed { i, _ -> i != index }
                )
            )
        }
        // If an imported camera was selected, fall back to the first real camera.
        if (_uiState.value.header.selectedCameraIndex >= _uiState.value.header.cameras.size) {
            selectCamera(0)
        }
    }

    /**
     * Builds the export payload for the current camera. Imported dumps are
     * returned as-is; live Camera2 dumps are wrapped with device metadata and
     * the camera id so the saved file is self-describing.
     */
    fun buildExportContent(): String {
        val header = _uiState.value.header
        val rawJson = _uiState.value.rawJson
        if (header.selectedCameraIndex >= header.cameras.size) return rawJson

        val meta = mapOf(
            "device" to "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
            "brand" to Build.BRAND,
            "model" to Build.MODEL,
            "androidVersion" to Build.VERSION.RELEASE,
            "sdkInt" to Build.VERSION.SDK_INT.toString(),
            "cameraId" to header.cameraId,
            "cameraName" to header.cameraName,
            "exportedAt" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).format(Date())
        )
        val type = object : TypeToken<Map<String, String>>() {}.type
        val dump: Map<String, String> = Gson().fromJson(rawJson, type)
        return GsonBuilder().setPrettyPrinting().create().toJson(mapOf("meta" to meta, "characteristics" to dump))
    }

    /** Suggested file name for the export dialog, e.g. camera0_23127PN0CC_20260824_144500.json. */
    fun buildSuggestedFileName(): String {
        val header = _uiState.value.header
        val isImported = header.selectedCameraIndex >= header.cameras.size
        val base = if (isImported) header.cameraName else "camera${header.cameraId}_${Build.MODEL}"
        val sanitized = base.map { if (it.isLetterOrDigit()) it else '_' }.joinToString("").trim('_')
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "${sanitized.ifBlank { "camera" }}_$stamp.json"
    }

    /** Writes the export payload for the current camera to the given SAF uri. */
    fun saveRawJsonToUri(uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = runCatching {
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(buildExportContent().toByteArray())
                        true
                    } ?: false
                }
            }.getOrDefault(false)
            if (!success) Log.e(TAG, "saveRawJsonToUri: failed to write $uri")
            onResult(success)
        }
    }

    private suspend fun importCameraInternal(nameHint: String?, json: String): Boolean = withContext(Dispatchers.Default) {
        val categories = parseRawJsonToCategories(json)
        if (categories == null) {
            Log.e(TAG, "importCameraInternal: failed to parse JSON (length=${json.length}, start=${json.take(50)})")
            return@withContext false
        }
        val baseName = nameHint?.substringBeforeLast('.')?.ifBlank { null }
            ?: "Imported ${_uiState.value.header.importedCameras.size + 1}"
        val camera = ImportedCamera(makeUniqueName(baseName), json, categories)
        persistImportedCamera(camera.name, json)
        _uiState.update { current ->
            current.copy(header = current.header.copy(importedCameras = current.header.importedCameras + camera))
        }
        // Select the newly imported camera so the user sees the result immediately.
        selectCamera(_uiState.value.header.cameras.size + _uiState.value.header.importedCameras.size - 1)
        true
    }

    private fun makeUniqueName(base: String): String {
        val existing = _uiState.value.header.importedCameras.map { it.name }
        if (existing.none { it.equals(base, ignoreCase = true) }) return base
        var suffix = 2
        while (existing.any { it.equals("$base $suffix", ignoreCase = true) }) suffix++
        return "$base $suffix"
    }

    private fun queryDisplayName(uri: Uri): String? {
        if (uri.scheme != "content") return uri.lastPathSegment
        return runCatching {
            getApplication<Application>().contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
    }

    private fun persistImportedCamera(name: String, json: String) {
        runCatching {
            val dir = File(getApplication<Application>().filesDir, IMPORT_DIR)
            dir.mkdirs()
            File(dir, safeFileName(name) + ".json").writeText(json)
        }.onFailure { Log.e(TAG, "Failed to persist imported camera", it) }
    }

    private fun loadPersistedImportedCameras() {
        viewModelScope.launch {
            val restored = withContext(Dispatchers.IO) {
                val dir = File(getApplication<Application>().filesDir, IMPORT_DIR)
                dir.listFiles { file -> file.isFile && file.extension == "json" }
                    ?.mapNotNull { file ->
                        val json = runCatching { file.readText() }.getOrNull() ?: return@mapNotNull null
                        val categories = parseRawJsonToCategories(json) ?: return@mapNotNull null
                        ImportedCamera(file.nameWithoutExtension, json, categories)
                    }
                    .orEmpty()
            }
            if (restored.isNotEmpty()) {
                _uiState.update { current ->
                    current.copy(header = current.header.copy(importedCameras = current.header.importedCameras + restored))
                }
            }
        }
    }

    private fun safeFileName(name: String): String {
        val sanitized = name.map { if (it.isLetterOrDigit()) it else '_' }.joinToString("")
        return sanitized.ifBlank { "camera" } + "_" + Integer.toHexString(name.hashCode())
    }

    /**
     * Best-effort reconstruction of the overview cards from an imported dump.
     * Values are derived from the formatted strings, so flags that need
     * structured data (e.g. high-speed video) stay off.
     */
    private fun buildOverviewFromParams(categories: List<ParameterCategory>): CameraOverviewState {
        val context = getApplication<Application>().applicationContext
        val map = categories.flatMap { it.parameters }.associate { it.key to it.value }
        val aeModes = map["android.control.aeAvailableModes"] ?: ""
        val capabilities = map["android.request.availableCapabilities"] ?: ""
        val pixelArray = map["android.sensor.info.pixelArraySize"]?.split("x")
        val megapixels = pixelArray?.takeIf { it.size == 2 }
            ?.let { (w, h) -> w.toLongOrNull()?.let { width -> h.toLongOrNull()?.let { height -> width * height } } }
            ?.div(1_000_000.0)
        val maxFps = Regex("\\d+").findAll(map["android.control.aeAvailableTargetFpsRanges"] ?: "")
            .maxOfOrNull { it.value.toInt() } ?: 0

        return CameraOverviewState(
            hardwareLevel = map["android.info.supportedHardwareLevel"] ?: "",
            sensorResolution = megapixels?.let { "%.0f MP".format(it) } ?: "",
            sensorResolutionDetails = map["android.sensor.info.pixelArraySize"] ?: "",
            sensorPhysicalSize = map["android.sensor.info.physicalSize"] ?: "",
            maxFps = maxFps.takeIf { it > 0 }?.let { "$it FPS" } ?: "",
            rawFormatSupported = capabilities.contains("RAW"),
            autoFlashSupported = map["android.flash.info.available"] == "true",
            flashAutoSupported = aeModes.contains(context.getString(R.string.mode_flash_auto)),
            flashAlwaysSupported = aeModes.contains(context.getString(R.string.mode_flash_always)),
            oisSupported = (map["android.lens.info.availableOpticalStabilization"] ?: "").contains("1"),
            faceDetectionSupported = (map["android.statistics.info.availableFaceDetectModes"] ?: "")
                .let { it.contains("1") || it.contains("2") },
            manualExpSupported = aeModes.contains("OFF"),
            manualFocusSupported = (map["android.lens.info.minimumFocusDistance"]?.toFloatOrNull() ?: 0f) > 0f,
            hdrSupported = (map["android.control.availableSceneModes"] ?: "").contains("HDR"),
            yuvReprocessingSupported = capabilities.contains("YUV_REPROCESSING"),
            redEyeReductionSupported = aeModes.contains(context.getString(R.string.mode_flash_red_eye), ignoreCase = true)
        )
    }

    /**
     * Converts a raw JSON dump back into [ParameterCategory]s. Accepts both the
     * bare key/value format produced by [generateRawJson] and the export format
     * that wraps it in a "characteristics" object alongside device metadata.
     * Returns null when the input is blank or malformed.
     */
    private fun parseRawJsonToCategories(json: String): List<ParameterCategory>? {
        if (json.isBlank()) return null
        return try {
            val type = object : TypeToken<Map<String, Any?>>() {}.type
            @Suppress("UNCHECKED_CAST")
            val root = Gson().fromJson(json, type) as? Map<String, Any?> ?: return null
            // Export format wraps the dump in "characteristics"; legacy dumps are bare.
            @Suppress("UNCHECKED_CAST")
            val characteristics = root["characteristics"] as? Map<String, Any?> ?: root
            val params = characteristics.entries.mapNotNull { (name, value) ->
                // Saved JSON keeps only the formatted value; reuse it as rawValue so
                // detail screens and copy actions keep working for imported dumps.
                val formatted = value?.toString() ?: return@mapNotNull null
                CameraParameter(
                    key = name,
                    value = formatted,
                    rawValue = formatted,
                    category = getCategoryForKey(name)
                )
            }
            buildOrderedCategories(params)
        } catch (e: JsonParseException) {
            Log.e(TAG, "parseRawJsonToCategories: malformed JSON", e)
            null
        }
    }

    private fun buildOrderedCategories(params: List<CameraParameter>): List<ParameterCategory> {
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
        return categoryOrder.mapNotNull { name ->
            categoriesMap[name]?.takeIf { it.isNotEmpty() }?.let { ParameterCategory(name, it) }
        }
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
        } else if (CameraCharacteristics.CONTROL_AVAILABLE_MODES == key) {
            return getAvailableModes(getApplication<Application>().applicationContext, (value as kotlin.IntArray?)!!)
        } else if (value is Array<*> && (key.name.contains("mandatoryStreamCombinations") || 
            (value.isNotEmpty() && value[0]?.javaClass?.name?.contains("MandatoryStreamCombination") == true))) {
            return getMandatoryStreamCombinationsString(value)
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
        private const val IMPORT_DIR = "imported_cameras"
    }
}

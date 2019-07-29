package com.aaron.cameraparams.camera

import android.graphics.ImageFormat
import android.graphics.PixelFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Build
import com.aaron.cameraparams.CameraParamsHelper
import java.util.*
import android.hardware.camera2.CameraCharacteristics.*
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CameraMetadata.*
import android.util.Rational
import android.util.Size
import kotlin.collections.HashMap

private val cameraSizeComparator = Comparator<Size> { o1, o2 ->
    o2.width * o2.height - o1.width * o1.height
}

val ALL_KEYS: ArrayList<CameraCharacteristics.Key<*>>
    get() {
        val list = ArrayList<CameraCharacteristics.Key<*>>()
        list.add(CameraCharacteristics.COLOR_CORRECTION_AVAILABLE_ABERRATION_MODES)
        list.add(CameraCharacteristics.CONTROL_AE_AVAILABLE_ANTIBANDING_MODES)
        list.add(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES)
        list.add(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
        list.add(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)
        list.add(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)
        list.add(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)
        list.add(CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS)
        list.add(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES)
        list.add(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)
        list.add(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES)
//hide        list.add(CameraCharacteristics.CONTROL_MAX_REGIONS)
        list.add(CameraCharacteristics.CONTROL_MAX_REGIONS_AE)
        list.add(CameraCharacteristics.CONTROL_MAX_REGIONS_AWB)
        list.add(CameraCharacteristics.CONTROL_MAX_REGIONS_AF)
//hide        list.add(CameraCharacteristics.CONTROL_AVAILABLE_HIGH_SPEED_VIDEO_CONFIGURATIONS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) list.add(CameraCharacteristics.CONTROL_AE_LOCK_AVAILABLE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) list.add(CameraCharacteristics.CONTROL_AWB_LOCK_AVAILABLE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) list.add(CameraCharacteristics.CONTROL_AVAILABLE_MODES)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) list.add(CameraCharacteristics.CONTROL_POST_RAW_SENSITIVITY_BOOST_RANGE)
        list.add(CameraCharacteristics.EDGE_AVAILABLE_EDGE_MODES)
        list.add(CameraCharacteristics.FLASH_INFO_AVAILABLE)
        list.add(CameraCharacteristics.HOT_PIXEL_AVAILABLE_HOT_PIXEL_MODES)
        list.add(CameraCharacteristics.JPEG_AVAILABLE_THUMBNAIL_SIZES)
        list.add(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)
        list.add(CameraCharacteristics.LENS_INFO_AVAILABLE_FILTER_DENSITIES)
        list.add(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
        list.add(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
        list.add(CameraCharacteristics.LENS_INFO_HYPERFOCAL_DISTANCE)
        list.add(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
//hide        list.add(CameraCharacteristics.LENS_INFO_SHADING_MAP_SIZE)
        list.add(CameraCharacteristics.LENS_INFO_FOCUS_DISTANCE_CALIBRATION)
        list.add(CameraCharacteristics.LENS_FACING)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) list.add(CameraCharacteristics.LENS_POSE_ROTATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) list.add(CameraCharacteristics.LENS_POSE_TRANSLATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) list.add(CameraCharacteristics.LENS_INTRINSIC_CALIBRATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) list.add(CameraCharacteristics.LENS_RADIAL_DISTORTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) list.add(CameraCharacteristics.LENS_POSE_REFERENCE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) list.add(CameraCharacteristics.LENS_DISTORTION)
        list.add(CameraCharacteristics.NOISE_REDUCTION_AVAILABLE_NOISE_REDUCTION_MODES)
//hide        list.add(CameraCharacteristics.QUIRKS_USE_PARTIAL_RESULT)
//hide        list.add(CameraCharacteristics.REQUEST_MAX_NUM_OUTPUT_STREAMS)
        list.add(CameraCharacteristics.REQUEST_MAX_NUM_OUTPUT_RAW)
        list.add(CameraCharacteristics.REQUEST_MAX_NUM_OUTPUT_PROC)
        list.add(CameraCharacteristics.REQUEST_MAX_NUM_OUTPUT_PROC_STALLING)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) list.add(CameraCharacteristics.REQUEST_MAX_NUM_INPUT_STREAMS)
        list.add(CameraCharacteristics.REQUEST_PIPELINE_MAX_DEPTH)
        list.add(CameraCharacteristics.REQUEST_PARTIAL_RESULT_COUNT)
        list.add(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
//hide        list.add(CameraCharacteristics.REQUEST_AVAILABLE_REQUEST_KEYS)
//hide        list.add(CameraCharacteristics.REQUEST_AVAILABLE_RESULT_KEYS)
//hide        list.add(CameraCharacteristics.REQUEST_AVAILABLE_CHARACTERISTICS_KEYS)
//hide        list.add(CameraCharacteristics.REQUEST_AVAILABLE_SESSION_KEYS)
//hide        list.add(CameraCharacteristics.REQUEST_AVAILABLE_PHYSICAL_CAMERA_REQUEST_KEYS)
//hide        list.add(CameraCharacteristics.SCALER_AVAILABLE_FORMATS)
//hide        list.add(CameraCharacteristics.SCALER_AVAILABLE_JPEG_MIN_DURATIONS)
//hide        list.add(CameraCharacteristics.SCALER_AVAILABLE_JPEG_SIZES)
        list.add(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)
//hide       list.add(CameraCharacteristics.SCALER_AVAILABLE_PROCESSED_MIN_DURATIONS)
//hide       list.add(CameraCharacteristics.SCALER_AVAILABLE_PROCESSED_SIZES)
//hide       list.add(CameraCharacteristics.SCALER_AVAILABLE_INPUT_OUTPUT_FORMATS_MAP)
//hide       list.add(CameraCharacteristics.SCALER_AVAILABLE_STREAM_CONFIGURATIONS)
//hide       list.add(CameraCharacteristics.SCALER_AVAILABLE_MIN_FRAME_DURATIONS)
//hide       list.add(CameraCharacteristics.SCALER_AVAILABLE_STALL_DURATIONS)
        list.add(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        list.add(CameraCharacteristics.SCALER_CROPPING_TYPE)
        list.add(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
        list.add(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        list.add(CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT)
        list.add(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
        list.add(CameraCharacteristics.SENSOR_INFO_MAX_FRAME_DURATION)
        list.add(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
        list.add(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
        list.add(CameraCharacteristics.SENSOR_INFO_WHITE_LEVEL)
        list.add(CameraCharacteristics.SENSOR_INFO_TIMESTAMP_SOURCE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) list.add(CameraCharacteristics.SENSOR_INFO_LENS_SHADING_APPLIED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) list.add(CameraCharacteristics.SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE)
        list.add(CameraCharacteristics.SENSOR_REFERENCE_ILLUMINANT1)
        list.add(CameraCharacteristics.SENSOR_REFERENCE_ILLUMINANT2)
        list.add(CameraCharacteristics.SENSOR_CALIBRATION_TRANSFORM1)
        list.add(CameraCharacteristics.SENSOR_CALIBRATION_TRANSFORM2)
        list.add(CameraCharacteristics.SENSOR_COLOR_TRANSFORM1)
        list.add(CameraCharacteristics.SENSOR_COLOR_TRANSFORM2)
        list.add(CameraCharacteristics.SENSOR_FORWARD_MATRIX1)
        list.add(CameraCharacteristics.SENSOR_FORWARD_MATRIX2)
        list.add(CameraCharacteristics.SENSOR_BLACK_LEVEL_PATTERN)
        list.add(CameraCharacteristics.SENSOR_MAX_ANALOG_SENSITIVITY)
        list.add(CameraCharacteristics.SENSOR_ORIENTATION)
        list.add(CameraCharacteristics.SENSOR_AVAILABLE_TEST_PATTERN_MODES)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) list.add(CameraCharacteristics.SENSOR_OPTICAL_BLACK_REGIONS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) list.add(CameraCharacteristics.SHADING_AVAILABLE_MODES)
        list.add(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES)
        list.add(CameraCharacteristics.STATISTICS_INFO_MAX_FACE_COUNT)
        list.add(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_HOT_PIXEL_MAP_MODES)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) list.add(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_LENS_SHADING_MAP_MODES)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) list.add(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_OIS_DATA_MODES)
        list.add(CameraCharacteristics.TONEMAP_MAX_CURVE_POINTS)
        list.add(CameraCharacteristics.TONEMAP_AVAILABLE_TONE_MAP_MODES)
//hide        list.add(CameraCharacteristics.LED_AVAILABLE_LEDS)
        list.add(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) list.add(CameraCharacteristics.INFO_VERSION)
        list.add(CameraCharacteristics.SYNC_MAX_LATENCY)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) list.add(CameraCharacteristics.REPROCESS_MAX_CAPTURE_STALL)
//hide        list.add(CameraCharacteristics.DEPTH_AVAILABLE_DEPTH_STREAM_CONFIGURATIONS)
//hide        list.add(CameraCharacteristics.DEPTH_AVAILABLE_DEPTH_MIN_FRAME_DURATIONS)
//hide        list.add(CameraCharacteristics.DEPTH_AVAILABLE_DEPTH_STALL_DURATIONS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) list.add(CameraCharacteristics.DEPTH_DEPTH_IS_EXCLUSIVE)
//hide        list.add(CameraCharacteristics.LOGICAL_MULTI_CAMERA_PHYSICAL_IDS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) list.add(CameraCharacteristics.LOGICAL_MULTI_CAMERA_SENSOR_SYNC_TYPE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) list.add(CameraCharacteristics.DISTORTION_CORRECTION_AVAILABLE_MODES)

        return list
    }


fun getAeAvailableModes(value: IntArray): String {
    val sb = StringBuilder()
    sb.append("[")
    for (i in value) {
        when (i) {
            CONTROL_AE_MODE_OFF -> sb.append("OFF")
            CONTROL_AE_MODE_ON -> sb.append("ON")
            CONTROL_AE_MODE_ON_AUTO_FLASH -> sb.append("Flash Auto")
            CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE -> sb.append("Flash Red Eye")
            CONTROL_AE_MODE_ON_EXTERNAL_FLASH -> sb.append("Flash External")
            CONTROL_AE_MODE_ON_ALWAYS_FLASH -> sb.append("Flash ALWAYS")
            else -> sb.append(i)
        }
        sb.append(",\n ")
    }
    sb.deleteCharAt(sb.length - 1)
    sb.append("]")
    return sb.toString()
}

fun getAfAvailableModes(value: IntArray): String {
    val sb = StringBuilder()
    sb.append("[")
    for (i in value) {
        when (i) {
            CONTROL_AF_MODE_OFF -> sb.append("OFF")
            CONTROL_AF_MODE_AUTO -> sb.append("AUTO")
            CONTROL_AF_MODE_CONTINUOUS_PICTURE -> sb.append("Continuous Picture")
            CONTROL_AF_MODE_CONTINUOUS_VIDEO -> sb.append("Continuous Video")
            CONTROL_AF_MODE_EDOF -> sb.append("EDOF")
            CONTROL_AF_MODE_MACRO -> sb.append("Macro")
            else -> sb.append(i)
        }
        sb.append(", \n")
    }
    sb.deleteCharAt(sb.length - 1)
    sb.append("]")
    return sb.toString()
}

fun getAwbAvailableModes(value: IntArray): String {
    val sb = StringBuilder()
    sb.append("[")
    for (i in value) {
        when (i) {
            CONTROL_AWB_MODE_OFF -> sb.append("OFF")
            CONTROL_AWB_MODE_AUTO -> sb.append("AUTO")
            CONTROL_AWB_MODE_INCANDESCENT -> sb.append("INCANDESCENT")
            CONTROL_AWB_MODE_FLUORESCENT -> sb.append("FLUORESCENT")
            CONTROL_AWB_MODE_WARM_FLUORESCENT -> sb.append("WARM_FLUORESCENT")
            CONTROL_AWB_MODE_DAYLIGHT -> sb.append("DAYLIGHT")
            CONTROL_AWB_MODE_CLOUDY_DAYLIGHT -> sb.append("CLOUDY_DAYLIGHT")
            CONTROL_AWB_MODE_TWILIGHT -> sb.append("TWILIGHT")
            CONTROL_AWB_MODE_SHADE -> sb.append("SHADE")
            else -> sb.append(i)
        }
        sb.append(", \n")
    }
    sb.deleteCharAt(sb.length - 1)
    sb.append("]")
    return sb.toString()
}

fun getAvailableModes(value: IntArray): String {
    val sb = StringBuilder()
    sb.append("[")
    for (i in value) {
        when (i) {
            CONTROL_MODE_OFF -> sb.append("OFF")
            CONTROL_MODE_AUTO -> sb.append("AUTO")
            CONTROL_MODE_USE_SCENE_MODE -> sb.append("USE_SCENE_MODE")
            CONTROL_MODE_OFF_KEEP_STATE -> sb.append("OFF_KEEP_STATE")
            else -> sb.append(i)
        }
        sb.append(", \n")
    }
    sb.deleteCharAt(sb.length - 1)
    sb.append("]")
    return sb.toString()
}

fun getAvailableEffects(value: IntArray): String {
    val sb = StringBuilder()
    sb.append("[")
    for (i in value) {
        when (i) {
            CONTROL_EFFECT_MODE_OFF -> sb.append("OFF")
            CONTROL_EFFECT_MODE_MONO -> sb.append("MONO")
            CONTROL_EFFECT_MODE_NEGATIVE -> sb.append("Negative")
            CONTROL_EFFECT_MODE_SOLARIZE -> sb.append("Solarize")
            CONTROL_EFFECT_MODE_SEPIA -> sb.append("SEPIA")
            CONTROL_EFFECT_MODE_POSTERIZE -> sb.append("POSTERIZE")
            CONTROL_EFFECT_MODE_WHITEBOARD -> sb.append("WHITEBOARD")
            CONTROL_EFFECT_MODE_BLACKBOARD -> sb.append("BLACKBOARD")
            CONTROL_EFFECT_MODE_AQUA -> sb.append("AQUA")
            else -> sb.append(i)
        }
        sb.append(", \n")
    }
    sb.deleteCharAt(sb.length - 1)
    sb.append("]")
    return sb.toString()
}

fun getAvailableSceneModes(value: IntArray): String {
    val sb = StringBuilder()
    sb.append("[")
    for (i in value) {
        when (i) {
            CONTROL_SCENE_MODE_DISABLED -> sb.append("DISABLED")
            CONTROL_SCENE_MODE_FACE_PRIORITY -> sb.append("FACE_PRIORITY")
            CONTROL_SCENE_MODE_ACTION -> sb.append("ACTION")
            CONTROL_SCENE_MODE_PORTRAIT -> sb.append("PORTRAIT")
            CONTROL_SCENE_MODE_LANDSCAPE -> sb.append("LANDSCAPE")
            CONTROL_SCENE_MODE_NIGHT -> sb.append("NIGHT")
            CONTROL_SCENE_MODE_NIGHT_PORTRAIT -> sb.append("NIGHT_PORTRAIT")
            CONTROL_SCENE_MODE_THEATRE -> sb.append("THEATRE")
            CONTROL_SCENE_MODE_BEACH -> sb.append("BEACH")
            CONTROL_SCENE_MODE_SNOW -> sb.append("SNOW")
            CONTROL_SCENE_MODE_SUNSET -> sb.append("SUNSET")
            CONTROL_SCENE_MODE_STEADYPHOTO -> sb.append("STEADYPHOTO")
            CONTROL_SCENE_MODE_FIREWORKS -> sb.append("FIREWORKS")
            CONTROL_SCENE_MODE_SPORTS -> sb.append("SPORTS")
            CONTROL_SCENE_MODE_PARTY -> sb.append("PARTY")
            CONTROL_SCENE_MODE_CANDLELIGHT -> sb.append("CANDLELIGHT")
            CONTROL_SCENE_MODE_BARCODE -> sb.append("BARCODE")
            CONTROL_SCENE_MODE_HIGH_SPEED_VIDEO -> sb.append("HIGH_SPEED_VIDEO")
            CONTROL_SCENE_MODE_HDR -> sb.append("HDR")
            else -> sb.append(i)
        }
        sb.append(", \n")
    }
    sb.deleteCharAt(sb.length - 1)
    sb.append("]")
    return sb.toString()
}

fun getAvailableNoiseReductionModes(value: IntArray): String {
    val sb = StringBuilder()
    sb.append("[")
    for (i in value) {
        when (i) {
            NOISE_REDUCTION_MODE_OFF -> sb.append("OFF")
            NOISE_REDUCTION_MODE_FAST -> sb.append("FAST")
            NOISE_REDUCTION_MODE_HIGH_QUALITY -> sb.append("HIGH_QUALITY")
            NOISE_REDUCTION_MODE_MINIMAL -> sb.append("MINIMAL")
            NOISE_REDUCTION_MODE_ZERO_SHUTTER_LAG -> sb.append("ZERO_SHUTTER_LAG")
            else -> sb.append(i)
        }
        sb.append(", \n")
    }
    sb.deleteCharAt(sb.length - 1)
    sb.append("]")
    return sb.toString()
}

fun getColorCorrectionAvailableAberrationMode(value: IntArray): String {
    val sb = StringBuilder()
    sb.append("[")
    for (i in value) {
        when (i) {
            COLOR_CORRECTION_ABERRATION_MODE_OFF -> sb.append("OFF")
            COLOR_CORRECTION_MODE_FAST -> sb.append("FAST")
            COLOR_CORRECTION_MODE_HIGH_QUALITY -> sb.append("QUALITY")
            else -> sb.append(i)
        }
        sb.append(", \n")
    }
    sb.deleteCharAt(sb.length - 1)
    sb.append("]")
    return sb.toString()
}

fun getRequestAvailableCapabilities(value: IntArray): String {
    val sb = StringBuilder()
    sb.append("[")
    for (i in value) {
        when (i) {
            REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE -> sb.append("BACKWARD_COMPATIBLE")
            REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR -> sb.append("MANUAL_SENSOR")
            REQUEST_AVAILABLE_CAPABILITIES_MANUAL_POST_PROCESSING -> sb.append("MANUAL_POST_PROCESSING")
            REQUEST_AVAILABLE_CAPABILITIES_RAW -> sb.append("RAW")
            REQUEST_AVAILABLE_CAPABILITIES_PRIVATE_REPROCESSING -> sb.append("PRIVATE_REPROCESSING")
            REQUEST_AVAILABLE_CAPABILITIES_READ_SENSOR_SETTINGS -> sb.append("READ_SENSOR_SETTINGS")
            REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE -> sb.append("BURST_CAPTURE")
            REQUEST_AVAILABLE_CAPABILITIES_YUV_REPROCESSING -> sb.append("YUV_REPROCESSING")
            REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT -> sb.append("DEPTH_OUTPUT")
            REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO -> sb.append("CONSTRAINED_HIGH_SPEED_VIDEO")
            REQUEST_AVAILABLE_CAPABILITIES_MOTION_TRACKING -> sb.append("MOTION_TRACKING")
            REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA -> sb.append("LOGICAL_MULTI_CAMERA")
            REQUEST_AVAILABLE_CAPABILITIES_MONOCHROME -> sb.append("MONOCHROME")
            0x0000000d-> sb.append("SECURE_IMAGE_DATA")
            else -> sb.append(i)
        }
        sb.append(", \n")
    }
    sb.deleteCharAt(sb.length - 1)
    sb.append("]")
    return sb.toString()
}

fun getHardwareLevelInfo(value: Int): String {
    val sortedHwLevels = intArrayOf(
        INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
        INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL,
        INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
        INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
        INFO_SUPPORTED_HARDWARE_LEVEL_3)
    val sb = StringBuilder()
    for (sortedlevel in sortedHwLevels) {
        if (value == sortedlevel) {
            sb.append(" [ ")
        }
        when (sortedlevel) {
           INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> sb.append(" Legacy ")
           INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> sb.append(" External ")
           INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> sb.append(" Limited ")
           INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> sb.append(" Full ")
           INFO_SUPPORTED_HARDWARE_LEVEL_3 -> sb.append(" 3 ")
            else -> {
            }
        }
        if (value == sortedlevel) {
            sb.append(" ] ")
        }
    }
    return sb.toString()
}

fun streamConfigurationMapToString(map: StreamConfigurationMap): String {
    val sb = StringBuilder("StreamConfiguration\n(")
    appendOutputsString(map, sb)
    sb.append(", \n")
    appendHighResOutputsString(map, sb)
    sb.append(", \n")
    appendInputsString(map, sb)
    sb.append(", \n")
    appendValidOutputFormatsForInputString(map, sb)
    sb.append(", \n")
    appendHighSpeedVideoConfigurationsString(map, sb)
    sb.append(")")

    return sb.toString()
}

fun appendOutputsString(map: StreamConfigurationMap, sb: StringBuilder) {
    sb.append("Outputs\n(")
    val formats = map.outputFormats
    for (format in formats) {
        val sizes = map.getOutputSizes(format)
        if (sizes == null) {
            sb.append(String.format(Locale.ENGLISH, "[format:%s(%d), NULL], \n", formatToString(format), format))
            continue
        }
        Arrays.sort(sizes, cameraSizeComparator)
        sb.append('\n')
        val supportedPhotoMap = java.util.HashMap<Rational, ArrayList<Size>>()
        sizes.forEachIndexed { _, size ->
            val rational = Rational(size.width, size.height)
            var list = supportedPhotoMap!![rational]
            if (list == null) {
                list = ArrayList()
                supportedPhotoMap!![rational] = list
            }
            list.add(size)
        }
        /*for (size in sizes) {
            val minFrameDuration = map.getOutputMinFrameDuration(format, size)
            val stallDuration = map.getOutputStallDuration(format, size)
            sb.append(
                String.format(
                    Locale.ENGLISH, "[w:%d, h:%d, format:%s(%d), min_duration:%d, " + "stall:%d], \n", size.width, size.height, formatToString(format),
                    format, minFrameDuration, stallDuration
                )
            )
        }*/
        for (entry in supportedPhotoMap.entries)
        {
            sb.append("${formatToString(format)}($format) : ${entry.key.numerator}:${entry.key.denominator}\n")
            entry.value?.forEachIndexed { _, size ->
                val minFrameDuration = map.getOutputMinFrameDuration(format, size)
                val stallDuration = map.getOutputStallDuration(format, size)
                sb.append(
                    String.format(
                        Locale.ENGLISH, "[w:%d, h:%d, min_duration:%d, " + "stall:%d], \n", size.width, size.height,
                        minFrameDuration, stallDuration
                    )
                )
            }
        }
    }
    // Remove the pending ", "
    if (sb[sb.length - 1] == ' ') {
        sb.delete(sb.length - 2, sb.length)
    }
    sb.append(")")
}

private fun appendHighResOutputsString(map: StreamConfigurationMap, sb: StringBuilder) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        return
    }
    sb.append("HighResolutionOutputs\n(")
    val formats = map.outputFormats
    for (format in formats) {
        val sizes = map.getHighResolutionOutputSizes(format) ?: continue
        for (size in sizes) {
            val minFrameDuration = map.getOutputMinFrameDuration(format, size)
            val stallDuration = map.getOutputStallDuration(format, size)
            sb.append(
                String.format(
                    Locale.ENGLISH, "[w:%d, h:%d, format:%s(%d), min_duration:%d, " + "stall:%d], \n", size.width, size.height, formatToString(format),
                    format, minFrameDuration, stallDuration
                )
            )
        }
    }
    // Remove the pending ", "
    if (sb[sb.length - 1] == ' ') {
        sb.delete(sb.length - 2, sb.length)
    }
    sb.append(")")
}

private fun appendInputsString(map: StreamConfigurationMap, sb: StringBuilder) {
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
        return
    }
    sb.append("Inputs\n(")
    val formats = map.inputFormats
    for (format in formats) {
        val sizes = map.getInputSizes(format)
        for (size in sizes) {
            sb.append(
                String.format(
                    Locale.ENGLISH, "[w:%d, h:%d, format:%s(%d)],\n ", size.width,
                    size.height, formatToString(format), format
                )
            )
        }
    }
    // Remove the pending ", "
    if (sb[sb.length - 1] == ' ') {
        sb.delete(sb.length - 2, sb.length)
    }
    sb.append(")")
}

private fun appendValidOutputFormatsForInputString(map: StreamConfigurationMap, sb: StringBuilder) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        return
    }
    sb.append("ValidOutputFormatsForInput\n(")
    val inputFormats = map.inputFormats
    for (inputFormat in inputFormats) {
        sb.append(String.format(Locale.ENGLISH, "[in:%s(%d), out:", formatToString(inputFormat), inputFormat))
        val outputFormats = map.getValidOutputFormatsForInput(inputFormat)
        for (i in outputFormats.indices) {
            sb.append(
                String.format(
                    Locale.ENGLISH, "%s(%d)", formatToString(outputFormats[i]),
                    outputFormats[i]
                )
            )
            if (i < outputFormats.size - 1) {
                sb.append(", ")
            }
        }
        sb.append("], \n")
    }
    // Remove the pending ", "
    if (sb[sb.length - 1] == ' ') {
        sb.delete(sb.length - 2, sb.length)
    }
    sb.append(")")
}

private fun appendHighSpeedVideoConfigurationsString(map: StreamConfigurationMap, sb: StringBuilder) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        return
    }
    sb.append("HighSpeedVideoConfigurations\n(")
    val sizes = map.highSpeedVideoSizes
    for (size in sizes) {
        val ranges = map.getHighSpeedVideoFpsRangesFor(size)
        for (range in ranges) {
            sb.append(
                String.format(
                    Locale.ENGLISH, "[w:%d, h:%d, min_fps:%d, max_fps:%d], \n", size.width,
                    size.height, range.lower, range.upper
                )
            )
        }
    }
    // Remove the pending ", "
    if (sb[sb.length - 1] == ' ') {
        sb.delete(sb.length - 2, sb.length)
    }
    sb.append(")")
}

private fun formatToString(format: Int): String {
    when (format) {
        ImageFormat.YV12 -> return "YV12"
        ImageFormat.YUV_420_888 -> return "YUV_420_888"
        ImageFormat.NV21 -> return "NV21"
        ImageFormat.NV16 -> return "NV16"
        PixelFormat.RGB_565 -> return "RGB_565"
        PixelFormat.RGBA_8888 -> return "RGBA_8888"
        PixelFormat.RGBX_8888 -> return "RGBX_8888"
        PixelFormat.RGB_888 -> return "RGB_888"
        ImageFormat.JPEG -> return "JPEG"
        ImageFormat.YUY2 -> return "YUY2"
        0x20203859 -> return "Y8"
        0x20363159 -> return "Y16"
        ImageFormat.RAW_SENSOR -> return "RAW_SENSOR"
        ImageFormat.RAW_PRIVATE -> return "RAW_PRIVATE"
        ImageFormat.RAW10 -> return "RAW10"
        ImageFormat.DEPTH16 -> return "DEPTH16"
        ImageFormat.DEPTH_POINT_CLOUD -> return "DEPTH_POINT_CLOUD"
        0x1002 -> return "RAW_DEPTH"
        ImageFormat.PRIVATE -> return "PRIVATE"
        else -> return "UNKNOWN"
    }
}

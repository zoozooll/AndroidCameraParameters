package com.aaron.cameraparams

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Build
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


class CameraParamsHelper(private val context: Context) {
    private var cameraIndex = 0
    var supportCameraIds: Array<String?> = emptyArray()
        private set
    private var characteristics: CameraCharacteristics? = null
    private var keyList: MutableList<CameraCharacteristics.Key<*>?>? = null

    init {
        generalSupportedCameraIds()
    }

    private fun generalSupportedCameraIds() {
        try {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            supportCameraIds = manager.cameraIdList
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun bindCameraId(index: Int) {
        this.cameraIndex = index
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            characteristics = manager.getCameraCharacteristics(supportCameraIds[index]!!)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    val availableKeys: MutableList<CameraCharacteristics.Key<*>?>
        get() {
            keyList = characteristics!!.getKeys()
            //        Log.d("aaron", "characteristics " + keyList.get(3).getName());
            return keyList!!
        }

    fun getCharacteristic(key: CameraCharacteristics.Key<*>?): Any? {
        return characteristics?.get(key)
    }

    fun getHardwareLevel(): Int {
        return characteristics?.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL) ?: -1
    }

    fun getCameraFacing(): Int {
        return characteristics?.get(CameraCharacteristics.LENS_FACING) ?: -1
    }

    fun getCameraCharacteristics(): CameraCharacteristics? = characteristics

    fun getCharacteristicInfo(key: CameraCharacteristics.Key<*>?): String {
        val value = characteristics?.get(key as CameraCharacteristics.Key<Any?>)
        return keyValue(key as? CameraCharacteristics.Key<Any?>, value)
    }

    fun keyValue(key: CameraCharacteristics.Key<Any?>?, value: Any?): String {
        if (CameraCharacteristics.COLOR_CORRECTION_AVAILABLE_ABERRATION_MODES == key) {
            return getColorCorrectionAvailableAberrationMode(context, (value as kotlin.IntArray?)!!)
        } else if (CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL == key) {
            return getHardwareLevelInfo(context, (value as kotlin.Int?)!!)
        } else if (CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES == key) {
            return getAeAvailableModes(context, (value as kotlin.IntArray?)!!)
        } else if (CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES == key) {
            return getAfAvailableModes(context, (value as kotlin.IntArray?)!!)
        } else if (CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES == key) {
            return getAwbAvailableModes(context, (value as kotlin.IntArray?)!!)
        } else if (CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS == key) {
            return getAvailableEffects(context, (value as kotlin.IntArray?)!!)
        } else if (CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES == key) {
            return getAvailableSceneModes(context, (value as kotlin.IntArray?)!!)
        } else if (CameraCharacteristics.NOISE_REDUCTION_AVAILABLE_NOISE_REDUCTION_MODES == key) {
            return getAvailableNoiseReductionModes(context, (value as kotlin.IntArray?)!!)
        } else if (CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES == key) {
            return getRequestAvailableCapabilities((value as kotlin.IntArray?)!!)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && CameraCharacteristics.CONTROL_AVAILABLE_MODES == key) {
            return getAvailableModes(context, (value as kotlin.IntArray?)!!)
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
}

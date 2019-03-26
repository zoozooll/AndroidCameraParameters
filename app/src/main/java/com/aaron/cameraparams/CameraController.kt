package com.aaron.cameraparams

import android.annotation.SuppressLint

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Rect
import android.hardware.camera2.*
import android.hardware.camera2.params.Face
import android.media.MediaRecorder
import android.os.*
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.Surface
import android.view.SurfaceHolder

import android.hardware.camera2.CameraCharacteristics.*
import android.content.ContentValues.TAG
import androidx.annotation.NonNull
import java.util.*
import kotlin.collections.ArrayList

class CameraController(private val context: Context) : ICameraController {

    private val cameraIndex: Int = 0
    private lateinit var supportCameraIds: Array<String>
    private lateinit var characteristics: CameraCharacteristics
    private lateinit var cameraId: String
    private var mBackgroundThread: HandlerThread? = null
    private lateinit var manager: CameraManager
    private var mBackgroundHandler: Handler? = null
    private lateinit var supportedFeature: CameraFeature
    private var mCameraDevice: CameraDevice? = null

    private var previewSurface: Surface? = null
    private var recordSurface: Surface? = null
    private var imageReaderSurface: Surface? = null

    private var mPreviewBuilder: CaptureRequest.Builder? = null
    private var mPreviewSession: CameraCaptureSession? = null
    private var want_video_high_speed: Boolean = false
    private val mCaptureCallback = object : CameraCaptureSession.CaptureCallback() {

        private fun process(result: CaptureResult) {}

        override fun onCaptureProgressed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            partialResult: CaptureResult
        ) {
            process(partialResult)
        }

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            process(result)
        }

    }
    private val mStateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(cameraDevice: CameraDevice) {
            mCameraDevice = cameraDevice
            startPreview()
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {}

        override fun onError(cameraDevice: CameraDevice, error: Int) {}

    }


    override fun init() {
        try {
            manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            startBackgroundThread()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun release() {
        stopBackgroundThread()
    }

    override fun getSupportedCameraIds(): Array<String>? {
        supportCameraIds = manager.cameraIdList
        return supportCameraIds
    }

    override fun bindCameraId(cameraId: String): Boolean {
        if (supportCameraIds.contains(cameraId)) {
            characteristics = manager.getCameraCharacteristics(cameraId)
            isAvailableStates()
            return true
        }
        return false
    }

    override fun getSensorOrientation(): Int {
        return supportedFeature.mSensorOrientation
    }

    override fun isFrontCamera(): Boolean {
        return supportedFeature.isFrontFacing
    }

    override fun getSupportedPreviewSizes(): Array<out Size>? {
        return supportedFeature.supportedPreviewSizes
    }


    override fun filterSupportPreviewSizes(targetSize: Size): Size? {
        return null
    }

    // Run after camera id is bound
    override fun getSupportedVideoSizes(): Array<out Size>? {
        return supportedFeature.supportedVideoSizes
    }

    // Run after camera id is bound
    override fun getSupportedPhotoSizes(): Array<out Size>? {
        return supportedFeature.supportedPhotoSizes
    }

    private fun startBackgroundThread() {
        mBackgroundThread = HandlerThread("CameraBackground")
        mBackgroundThread!!.start()
        mBackgroundHandler = Handler(mBackgroundThread!!.looper)
    }

    private fun stopBackgroundThread() {
        if (mBackgroundThread != null) {

            mBackgroundThread!!.quitSafely()
            try {
                mBackgroundThread!!.join()
                mBackgroundThread = null
                mBackgroundHandler = null
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

    // Run after camera id is bound
    @SuppressLint("MissingPermission")
    override fun openCamera() {
        if (mCameraDevice != null) {
            closePreviewSession()
            if (null != mCameraDevice) {
                mCameraDevice!!.close()
                mCameraDevice = null
            }
        }
        try {
            manager.openCamera(cameraId, mStateCallback, mBackgroundHandler)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun startRecording() {

    }

    override fun stopRecording() {

    }

    override fun takePicture() {}

    override fun onTakePictureComplete() {}


    override fun closeCamera() {
        try {
            closePreviewSession()
            mCameraDevice?.close()
            mCameraDevice = null
        } catch (e: Exception) {
            Log.e(TAG, "close camera failed")
        }
    }

    override fun setCameraSize(cameraSize: Size) {

    }

    override fun getBackgroundHandler(): Handler? {
        return mBackgroundHandler
    }

    override fun getMAXZoom(): Float {
        return supportedFeature.maxZoomNum
    }

    override fun getMAXAwbList(): IntArray? {
        return supportedFeature.awb_modes
    }

    override fun getISORange(): Range<Int>? {
        return supportedFeature.sensitivity_range
    }

    override fun getExposureTimeRange(): Range<Long>? {
        return supportedFeature.exposure_time_range
    }

    override fun getAERange(): Range<Int>? {
        return supportedFeature.ev_range
    }

    override fun setZoom(num: Int) {}

    override fun setISO(num: Int) {}

    override fun setAWB(num: Int) {}

    override fun setAE_exposure(num: Int) {}

    override fun setAutoFocus(flag: Boolean) {}

    override fun setLenFocus(num: Int) {}

    override fun setExposure(num: Int) {}

    override fun getHDRMode(): Boolean {
        return false
    }

    override fun setHDRMode(flag: Boolean) {}

    override fun openFlash(type: Int) {}

    override fun setFaceDetectCallBack(callback: ICameraController.FaceDetectCallBack) {

    }

    override fun setAutoFocusCallBack(callback: ICameraController.AutoFocusCallBack) {

    }


    override fun setPreviewSurface(previewSurface: Surface) {
        this.previewSurface = previewSurface
    }

    /**
     * 判断可用状态
     */
    private fun isAvailableStates() {
        supportedFeature = CameraFeature()
        characteristics.keys.apply {

            supportedFeature.hardwareLevel = characteristics.get(INFO_SUPPORTED_HARDWARE_LEVEL) ?: 0

            supportedFeature.intsCapabilities = characteristics.get(REQUEST_AVAILABLE_CAPABILITIES)

            //传感器方向
            supportedFeature.mSensorOrientation = characteristics.get(SENSOR_ORIENTATION) ?: 0
            // 是否前置摄像头（对准脸部摄像头）
            supportedFeature.isFrontFacing =
                characteristics.get(LENS_FACING) == LENS_FACING_FRONT

            supportedFeature.mFlashSupported =  characteristics.get(FLASH_INFO_AVAILABLE) ?: false

            val map = characteristics.get(SCALER_STREAM_CONFIGURATION_MAP)
            // Check if the flash is supported.
            supportedFeature.supportedPhotoSizes = map?.getOutputSizes(ImageFormat.JPEG)
            supportedFeature.supportedPreviewSizes = map?.getOutputSizes(SurfaceHolder::class.java)
            supportedFeature.supportedVideoSizes = map?.getOutputSizes(MediaRecorder::class.java)

            supportedFeature.optical_stabilization_size =  if (this.contains(LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION))
                characteristics.get(LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
            else null
            //曝光平衡
            supportedFeature.ev_range = characteristics.get(CONTROL_AE_COMPENSATION_RANGE)
            //色彩效果列表
            supportedFeature.effects = characteristics.get(CONTROL_AVAILABLE_EFFECTS)
            //控制模式列表
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                supportedFeature.control_modes = characteristics.get(CONTROL_AVAILABLE_MODES)
            }
            supportedFeature.awb_modes = characteristics.get(CONTROL_AWB_AVAILABLE_MODES)
            supportedFeature.max_regions_awb = characteristics.get(CONTROL_MAX_REGIONS_AWB) ?: 0
            //视频稳定模式列表
            supportedFeature.video_stabilization_modes =
                characteristics.get(CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)

            supportedFeature.sensitivity_range = if (isHardwareLevelSupported(supportedFeature.hardwareLevel, CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL) && this.contains(SENSOR_INFO_SENSITIVITY_RANGE))
                characteristics.get(SENSOR_INFO_SENSITIVITY_RANGE)
            else null

            supportedFeature.exposure_time_range = if (isHardwareLevelSupported(supportedFeature.hardwareLevel, CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL) && this.contains(SENSOR_INFO_EXPOSURE_TIME_RANGE))
                characteristics.get(SENSOR_INFO_EXPOSURE_TIME_RANGE)
            else null

            supportedFeature.max_analog_sensitivity = if (isHardwareLevelSupported(supportedFeature.hardwareLevel, CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL) && this.contains(SENSOR_MAX_ANALOG_SENSITIVITY))
                characteristics.get(SENSOR_MAX_ANALOG_SENSITIVITY) ?: 0
            else 0

            supportedFeature.max_regions_ae = characteristics.get(CONTROL_MAX_REGIONS_AE) ?: 0

            supportedFeature.maxFaceCount = characteristics.get(STATISTICS_INFO_MAX_FACE_COUNT) ?: 0

            val faceModes = characteristics.get(STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES)
            supportedFeature.supports_face_detect_mode_simple = false
            supportedFeature.supports_face_detect_mode_full = false
            faceModes?.forEach {
                Log.i(TAG, "face detection mode: $it")
                // we currently only make use of the "SIMPLE" features, documented as:
                // "Return face rectangle and confidence values only."
                // note that devices that support STATISTICS_FACE_DETECT_MODE_FULL (e.g., Nexus 6) don't return
                // STATISTICS_FACE_DETECT_MODE_SIMPLE in the list, so we have check for either
                if (it == STATISTICS_FACE_DETECT_MODE_SIMPLE) {
                    supportedFeature.supports_face_detection = true
                    supportedFeature.supports_face_detect_mode_simple = true
                } else if (it == STATISTICS_FACE_DETECT_MODE_FULL) {
                    supportedFeature.supports_face_detection = true
                    supportedFeature.supports_face_detect_mode_full = true
                }
            }
            if (supportedFeature.maxFaceCount <= 0) {
                supportedFeature.supports_face_detection = false
                supportedFeature.supports_face_detect_mode_simple = false
                supportedFeature.supports_face_detect_mode_full = false
            }
            val values2 = characteristics.get(CONTROL_AVAILABLE_SCENE_MODES)
            val has_face_priority = values2?.contains(CameraMetadata.CONTROL_SCENE_MODE_FACE_PRIORITY) ?: false

            if (!has_face_priority) {
                supportedFeature.supports_face_detection = false
                supportedFeature.supports_face_detect_mode_simple = false
                supportedFeature.supports_face_detect_mode_full = false
            }

            val m = characteristics.get(SENSOR_INFO_ACTIVE_ARRAY_SIZE)
            supportedFeature.maxZoomNum  = characteristics.get(SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)?: 1.0f
            //        checkDeviceBrand();

            supportedFeature.minimum_focus_distance = if (isHardwareLevelSupported(supportedFeature.hardwareLevel, CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED) && this.contains(LENS_INFO_MINIMUM_FOCUS_DISTANCE))
                characteristics.get(LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0.0f
            else 0.0F

            supportedFeature.lens_apertures = if (isHardwareLevelSupported(supportedFeature.hardwareLevel, CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL) && this.contains(LENS_INFO_AVAILABLE_APERTURES))
                characteristics.get(LENS_INFO_AVAILABLE_APERTURES)
            else null

            supportedFeature.pixel_array_size = characteristics.get(SENSOR_INFO_PIXEL_ARRAY_SIZE)



            val capabilities = characteristics.get(REQUEST_AVAILABLE_CAPABILITIES)
            supportedFeature.supportedHighSpeedVideo =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && capabilities != null)
                    capabilities.contains(REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO)
                else
                    false
            if (supportedFeature.supportedHighSpeedVideo) {
                supportedFeature.video_sizes_high_speed = ArrayList()
                val camera_video_sizes_high_speed = map.getHighSpeedVideoSizes()
                for (camera_size in camera_video_sizes_high_speed) {
                    val fr = ArrayList<Range<Int>>()
                    for (r in map.getHighSpeedVideoFpsRangesFor(camera_size)) {
                        fr.add(r)
                    }
                    val hs_video_size = CameraController.VideoSize(camera_size.getWidth(), camera_size.getHeight(), fr, true)

                    supportedFeature.video_sizes_high_speed!!.add(hs_video_size)
                }
            }
        }

    }




    private fun startPreview() {
        if (mPreviewBuilder == null) {
            return
        }
        if (mCameraDevice == null) {
            return
        }

        mPreviewSession?.close()

        try {

            class MyStateCallback : CameraCaptureSession.StateCallback() {
                private var callback_done: Boolean = false // must sychronize on this and notifyAll when setting to true
                override fun onConfigured(session: CameraCaptureSession) {
                    if (mCameraDevice == null) {
                        return
                    }
                    mPreviewSession = session
                    updatePreview()
                }

                override fun onConfigureFailed(@NonNull session: CameraCaptureSession) {

                }

            }

            val myStateCallback = MyStateCallback()

            val surfaces: MutableList<Surface> = ArrayList()
            previewSurface?.let { surfaces.add(it) }
            recordSurface?.let { surfaces.add(it) }
            imageReaderSurface?.let { surfaces.add(it) }
            if (recordSurface != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && want_video_high_speed) {
                mCameraDevice?.createConstrainedHighSpeedCaptureSession(
                    surfaces,
                    myStateCallback,
                    backgroundHandler
                )
            } else {
                    mCameraDevice?.createCaptureSession(
                        surfaces,
                        myStateCallback,
                        backgroundHandler
                    )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun updatePreview() {
        if (null == mCameraDevice || mPreviewSession == null || mPreviewBuilder == null) {
            return
        }
        try {
            setupBuilder(mPreviewBuilder!!)
            mPreviewSession!!.setRepeatingRequest(mPreviewBuilder!!.build(), mCaptureCallback, mBackgroundHandler)
        } catch (e: Exception) {
            e.printStackTrace()
        } catch (e: IllegalAccessError) {
            e.printStackTrace()
        }

    }

    private fun setupBuilder(previewBuilder: CaptureRequest.Builder) {

    }

    private fun closePreviewSession() {
        if (mPreviewSession != null) {
            try {
                mPreviewSession!!.stopRepeating()
                mPreviewSession!!.abortCaptures()
            } catch (e: Exception) {
                e.printStackTrace()
            } catch (e: IllegalAccessError) {
                e.printStackTrace()
            }

            mPreviewSession!!.close()
            mPreviewSession = null
        }
    }

    override fun takePictureComplete() {}

    override fun lockAE() {}

    override fun unlockAE() {}


    override fun setFocus(x: Int, y: Int): Boolean {
        return false
    }

    override fun isFrontFace(): Boolean {
        return false
    }

    override fun openFace(flag: Boolean) {}


    override fun setCameraControllerCallback(cameraControllerCallback: ICameraController.CameraControllerCallback) {}

    private class CompareSizesByArea : Comparator<Size> {

        override fun compare(lhs: Size, rhs: Size): Int {
            // We cast here to ensure the multiplications won't overflow
            return java.lang.Long.signum(lhs.width.toLong() * lhs.height - rhs.width.toLong() * rhs.height)
        }

    }

    inner class CameraFeature {
        var mSensorOrientation: Int = 0
        var hardwareLevel: Int = 0
        var intsCapabilities: IntArray? = null
        var isFrontFacing: Boolean = false
        var mFlashSupported: Boolean? = false
        var supportedPhotoSizes: Array<out Size>? = null
        var supportedPreviewSizes: Array<out Size>? = null
        var supportedVideoSizes: Array<out Size>? = null
        var optical_stabilization_size: IntArray? = null
        var ev_range: Range<Int>? = null
        var effects: IntArray? = null
        var control_modes: IntArray? = null
        var awb_modes: IntArray? = null
        var max_regions_awb: Int = 0
        var video_stabilization_modes: IntArray? = null
        var sensitivity_range: Range<Int>? = null
        var exposure_time_range: Range<Long>? = null
        var max_analog_sensitivity: Int = 0
        var max_regions_ae: Int = 0
        var maxFaceCount: Int = 0
        var supports_face_detect_mode_simple: Boolean = false
        var supports_face_detect_mode_full: Boolean = false
        var supports_face_detection: Boolean = false
        var has_face_priority: Boolean = false
        var maxZoomNum: Float = 1.0f
        var minimum_focus_distance: Float = 0.0F
        var lens_apertures: FloatArray? = null
        var pixel_array_size: Size? = null
        var supportedHighSpeedVideo: Boolean = false
        var video_sizes_high_speed: ArrayList<VideoSize>? = null
    }

    inner class CameraSettings {
        var zoom: Float = 0.0f
        var focusLength:Float = 0.0f
        var whiteBalance = 0
        var iso = 0
        var exposure_time: Float = 1.0f
        var ev: Float = 0.0f
        var autoExposure: Boolean? = false
    }

    class Area(internal val rect: Rect, internal val weight: Int)

    class FaceData {
        var bounds = ArrayList<Face>()               // 人脸列表
        var largest: Size? = null                                             // 最大分辨率
        var isFront: Boolean = false                                        //是否使用前置摄像头
    }

    class VideoSize(val width: Int, val height: Int, val fpss: ArrayList<Range<Int>>, val isHighSpeed: Boolean)

    companion object {

        private val EXPOSURE_TIME_DEFAULT = 1000000000L / 30
        /**
         * Camera state: Showing camera preview.
         */
        private val STATE_PREVIEW = 0
        /**
         * Camera state: Waiting for the focus to be locked.
         */
        private val STATE_WAITING_LOCK = 1
        /**
         * Camera state: Waiting for the exposure to be precapture state.
         */
        private val STATE_WAITING_PRECAPTURE = 2
        /**
         * Camera state: Waiting for the exposure state to be something other than precapture.
         */
        private val STATE_WAITING_NON_PRECAPTURE = 3
        /**
         * Camera state: Picture was taken.
         */
        private val STATE_PICTURE_TAKEN = 4
        private val STATE_RE_FOCUSING = 5
        /**
         * 聚焦开始
         */
        private val FOCUS_START = 5
        /**
         * 聚焦完成
         */
        private val FOCUS_COMPLETE = 6
        /**
         * flash 状态
         */
        private val STATE_FLASH = 7
        /**
         * flash 状态
         */
        private val STATE_FLASH_PREPARE = 8
        /**
         * flash 状态
         */
        private val STATE_FLASH_NON_PREPARE = 9
        /**
         * flash 状态
         */
        private val STATE_FLASH_DONE = 10

        private fun seekbarScaling(frac: Double): Double {
            // For various seekbars, we want to use a non-linear scaling, so user has more control over smaller values
            return (Math.pow(100.0, frac) - 1.0) / 99.0
        }

        private fun chooseVideoSize(choices: Array<Size>): Size {
            for (size in choices) {
                if (size.width == size.height * 4 / 3 && size.width <= 1080) {
                    return size
                }
            }
            Log.w(TAG, "Couldn't find any suitable video size")
            return choices[choices.size - 1]
        }

        private fun chooseOptimalSize(choices: Array<Size>, width: Int, height: Int, aspectRatio: Size): Size {
            // Collect the supported resolutions that are at least as big as the preview Surface
            val bigEnough = ArrayList<Size>()
            val w = aspectRatio.width
            val h = aspectRatio.height
            for (option in choices) {
                if (option.height == option.width * h / w &&
                    option.width >= width && option.height >= height
                ) {
                    bigEnough.add(option)
                }
            }

            // Pick the smallest of those, assuming we found any
            if (bigEnough.size > 0) {
                return Collections.min(bigEnough, CompareSizesByArea())
            } else {
                Log.w(TAG, "Couldn't find any suitable preview size")
                return choices[0]
            }
        }

        fun isHardwareLevelSupported(deviceLevel: Int, requiredLevel: Int) :Boolean {
             val sortedHwLevels =   intArrayOf (
                     INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
                     if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL else 4,
                    INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
                    INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
                     if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) INFO_SUPPORTED_HARDWARE_LEVEL_3 else 3
                 )
            if (requiredLevel == deviceLevel) {
                 return true
             }

            sortedHwLevels.forEach{
                 if (it == requiredLevel) {
                     return true
                 } else if (it == deviceLevel) {
                     return false
                 }
             }
             return false
         }
    }
}

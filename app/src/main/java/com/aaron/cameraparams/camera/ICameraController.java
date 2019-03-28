package com.aaron.cameraparams.camera;

import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCharacteristics;
import android.location.Location;
import android.media.MediaRecorder;
import android.os.Build;

import android.view.SurfaceHolder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

interface ICameraController {


    SupportedValues setSceneMode(String value);
    String getSceneMode();
    boolean sceneModeAffectsFunctionality();
    SupportedValues setColorEffect(String value);
    String getColorEffect();
    SupportedValues setWhiteBalance(String value);
    String getWhiteBalance();
    boolean setWhiteBalanceTemperature(int temperature);
    int getWhiteBalanceTemperature();
    SupportedValues setAntiBanding(String value);
    String getAntiBanding();
    SupportedValues setEdgeMode(String value);
    String getEdgeMode();
    SupportedValues setNoiseReductionMode(String value);
    String getNoiseReductionMode();
    SupportedValues setISO(String value);
    void setManualISO(boolean manual_iso, int iso);

    boolean isManualISO();
    boolean setISO(int iso);
    String getISOKey();
    int getISO();
    long getExposureTime();
    boolean setExposureTime(long exposure_time);
    Size getPictureSize();
    void setPictureSize(int width, int height);
    Size getPreviewSize();
    void setPreviewSize(int width, int height);

    void setBurstType(int new_burst_type);
    int getBurstType();
    void setBurstNImages(int burst_requested_n_images);
    void setBurstForNoiseReduction(boolean burst_for_noise_reduction, boolean noise_reduction_low_light);
    boolean isContinuousBurstInProgress();
    void stopContinuousBurst();
    void stopFocusBracketingBurst();
    void setExpoBracketingNImages(int n_images);
    void setExpoBracketingStops(double stops);
    void setUseExpoFastBurst(boolean use_expo_fast_burst);
    boolean isBurstOrExpo();
    boolean isCapturingBurst();

    int getNBurstTaken();
    int getBurstTotal();
    void setOptimiseAEForDRO(boolean optimise_ae_for_dro);

    void setRaw(boolean want_raw, int max_raw_images);
    void setVideoHighSpeed(boolean setVideoHighSpeed);
    void setUseCamera2FakeFlash(boolean use_fake_precapture);
    boolean getUseCamera2FakeFlash();
    void setVideoStabilization(boolean enabled);
    boolean getVideoStabilization();
    void setLogProfile(boolean use_log_profile, float log_profile_strength);
    boolean isLogProfile();
    int getJpegQuality();
    void setJpegQuality(int quality);
    int getZoom();
    void setZoom(int value);
    int getExposureCompensation();
    boolean setExposureCompensation(int new_exposure);
    void setPreviewFpsRange(int min, int max);
    void clearPreviewFpsRange();
    List<int []> getSupportedPreviewFpsRange(); // result depends on setting of setVideoHighSpeed()

    void setFocusValue(String focus_value);
    String getFocusValue();
    float getFocusDistance();
    boolean setFocusDistance(float focus_distance);
    void setFocusBracketingNImages(int n_images);
    void setFocusBracketingAddInfinity(boolean focus_bracketing_add_infinity);
    void setFocusBracketingSourceDistance(float focus_bracketing_source_distance);
    float getFocusBracketingSourceDistance();
    void setFocusBracketingTargetDistance(float focus_bracketing_target_distance);
    float getFocusBracketingTargetDistance();
    void setFlashValue(String flash_value);
    String getFlashValue();
    void setRecordingHint(boolean hint);
    void setAutoExposureLock(boolean enabled);
    boolean getAutoExposureLock();
    void setAutoWhiteBalanceLock(boolean enabled);
    boolean getAutoWhiteBalanceLock();
    void setRotation(int rotation);
    void setLocationInfo(Location location);
    void removeLocationInfo();
    void enableShutterSound(boolean enabled);
    boolean setFocusAndMeteringArea(List<Area> areas);
    void clearFocusAndMetering();
    List<Area> getFocusAreas();
    List<Area> getMeteringAreas();
    boolean supportsAutoFocus();
    boolean focusIsContinuous();
    boolean focusIsVideo();
    void reconnect() ;
    void setPreviewDisplay(SurfaceHolder holder) ;
    void setPreviewTexture(SurfaceTexture texture) ;
    void startPreview() ;
    void stopPreview();
    boolean startFaceDetection();
    void setFaceDetectionListener(final FaceDetectionListener listener);
    void autoFocus(final AutoFocusCallback cb, boolean capture_follows_autofocus_hint);
    void setCaptureFollowAutofocusHint(boolean capture_follows_autofocus_hint);
    void cancelAutoFocus();
    void setContinuousFocusMoveCallback(ContinuousFocusMoveCallback cb);
    void takePicture(final PictureCallback picture, final ErrorCallback error);
    int getCameraOrientation();
    boolean isFrontFacing();
    void unlock();
    void initVideoRecorderPrePrepare(MediaRecorder video_recorder);
    void initVideoRecorderPostPrepare(MediaRecorder video_recorder, boolean want_photo_video_recording) ;
    String getParametersString();
    boolean captureResultIsAEScanning();
    boolean needsFlash();
    boolean needsFrontScreenFlash();
    boolean captureResultHasWhiteBalanceTemperature();
    int captureResultWhiteBalanceTemperature();
    boolean captureResultHasIso();
    int captureResultIso();
    boolean captureResultHasExposureTime();
    long captureResultExposureTime();
    boolean captureResultHasFrameDuration();
    long captureResultFrameDuration();

    public static class CameraFeatures {
        boolean is_zoom_supported;
        int max_zoom;
        List<Integer> zoom_ratios;
        boolean supports_face_detection;
        List<Size> picture_sizes;
        List<Size> video_sizes;
        List<Size> video_sizes_high_speed; // may be null if high speed not supported
        List<Size> preview_sizes;
        List<String> supported_flash_values;
        List<String> supported_focus_values;
        int max_num_focus_areas;
        float minimum_focus_distance;
        boolean is_exposure_lock_supported;
        boolean is_white_balance_lock_supported;
        boolean is_video_stabilization_supported;
        boolean is_photo_video_recording_supported;
        boolean supports_white_balance_temperature;
        int min_temperature;
        int max_temperature;
        boolean supports_iso_range;
        int min_iso;
        int max_iso;
        boolean supports_exposure_time;
        long min_exposure_time;
        long max_exposure_time;
        int min_exposure;
        int max_exposure;
        float exposure_step;
        boolean can_disable_shutter_sound;
        int tonemap_max_curve_points;
        boolean supports_tonemap_curve;
        boolean supports_expo_bracketing; // whether setBurstTye(BURSTTYPE_EXPO) can be used
        int max_expo_bracketing_n_images;
        boolean supports_focus_bracketing; // whether setBurstTye(BURSTTYPE_FOCUS) can be used
        boolean supports_burst; // whether setBurstTye(BURSTTYPE_NORMAL) can be used
        boolean supports_raw;
        float view_angle_x; // horizontal angle of view in degrees (when unzoomed)
        float view_angle_y; // vertical angle of view in degrees (when unzoomed)

        /** Returns whether any of the supplied sizes support the requested fps.
         */
        static boolean supportsFrameRate(List<Size> sizes, int fps) {
            if( sizes == null )
                return false;
            for(Size size : sizes) {
                if( size.supportsFrameRate(fps) ) {
                    return true;
                }
            }
            return false;
        }

        static Size findSize(List<Size> sizes, Size size, double fps, boolean return_closest) {
            Size last_s = null;
            for(Size s : sizes) {
                if (size.equals(s)) {
                    last_s = s;
                    if (fps > 0) {
                        if (s.supportsFrameRate(fps)) {
                            return s;
                        }
                    } else {
                        return s;
                    }
                }
            }
            return return_closest ? last_s : null;
        }
    }

    // Android docs and FindBugs recommend that Comparators also be Serializable
    public static class RangeSorter implements Comparator<int[]>, Serializable {
        private static final long serialVersionUID = 5802214721073728212L;
        @Override
        public int compare(int[] o1, int[] o2) {
            if (o1[0] == o2[0]) return o1[1] - o2[1];
            return o1[0] - o2[0];
        }
    }

    /* Sorts resolutions from highest to lowest, by area.
     * Android docs and FindBugs recommend that Comparators also be Serializable
     */
    public static class SizeSorter implements Comparator<Size>, Serializable {
        private static final long serialVersionUID = 5802214721073718212L;

        @Override
        public int compare(final Size a, final Size b) {
            return b.width * b.height - a.width * a.height;
        }
    }

    public static class Size {
        final int width;
        final int height;
        boolean supports_burst; // for photo
        final List<int[]> fps_ranges; // for video
        final boolean high_speed; // for video

        Size(int width, int height, List<int[]> fps_ranges, boolean high_speed) {
            this.width = width;
            this.height = height;
            this.supports_burst = true;
            this.fps_ranges = fps_ranges;
            this.high_speed = high_speed;
            Collections.sort(this.fps_ranges, new RangeSorter());
        }

        Size(int width, int height) {
            this(width, height, new ArrayList<int[]>(), false);
        }

        boolean supportsFrameRate(double fps) {
            for (int[] f : this.fps_ranges) {
                if (f[0] <= fps && fps <= f[1])
                    return true;
            }
            return false;
        }

        @Override
        public boolean equals(Object o) {
            if( !(o instanceof Size) )
                return false;
            Size that = (Size)o;
            return this.width == that.width && this.height == that.height;
        }

        @Override
        public int hashCode() {
            // must override this, as we override equals()
            // can't use:
            //return Objects.hash(width, height);
            // as this requires API level 19
            // so use this from http://stackoverflow.com/questions/11742593/what-is-the-hashcode-for-a-custom-class-having-just-two-int-properties
            return width*31 + height;
        }

        public String toString() {
            StringBuilder s = new StringBuilder();
            for (int[] f : this.fps_ranges) {
                s.append(" [").append(f[0]).append("-").append(f[1]).append("]");
            }
            return this.width + "x" + this.height + " " + s + (this.high_speed ? "-hs" : "");
        }
    }

    /** An area has values from [-1000,-1000] (for top-left) to [1000,1000] (for bottom-right) for whatever is
     * the current field of view (i.e., taking zoom into account).
     */
    public static class Area {
        final Rect rect;
        final int weight;

        Area(Rect rect, int weight) {
            this.rect = rect;
            this.weight = weight;
        }
    }

    interface FaceDetectionListener {
        void onFaceDetection(Face[] faces);
    }

    interface PictureCallback {
        void onStarted(); // called immediately before we start capturing the picture
        void onCompleted(); // called after all relevant on*PictureTaken() callbacks have been called and returned
        void onPictureTaken(byte[] data);
        void onBurstPictureTaken(List<byte[]> images);
        /* This is called for flash_frontscreen_auto or flash_frontscreen_on mode to indicate the caller should light up the screen
         * (for flash_frontscreen_auto it will only be called if the scene is considered dark enough to require the screen flash).
         * The screen flash can be removed when or after onCompleted() is called.
         */
        /* This is called for when burst mode is BURSTTYPE_FOCUS or BURSTTYPE_CONTINUOUS, to ask whether it's safe to take
         * n_jpegs extra images, or whether to wait.
         */
        boolean imageQueueWouldBlock(int n_jpegs);
        void onFrontScreenTurnOn();
    }

    interface AutoFocusCallback {
        void onAutoFocus(boolean success);
    }

    interface ContinuousFocusMoveCallback {
        void onContinuousFocusMove(boolean start);
    }

    interface ErrorCallback {
        void onError();
    }

    public static class Face {
        final int score;
        /* The has values from [-1000,-1000] (for top-left) to [1000,1000] (for bottom-right) for whatever is
         * the current field of view (i.e., taking zoom into account).
         */
        final Rect rect;

        Face(int score, Rect rect) {
            this.score = score;
            this.rect = rect;
        }
    }

    public static class SupportedValues {
        final List<String> values;
        final String selected_value;
        SupportedValues(List<String> values, String selected_value) {
            this.values = values;
            this.selected_value = selected_value;
        }
    }

    static boolean isHardwareLevelSupported(int deviceLevel, int requiredLevel) {
        final List<Integer> sortedHwLevels = new ArrayList<>();
        sortedHwLevels.add(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            sortedHwLevels.add(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL);
        }
        sortedHwLevels.add(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED);
        sortedHwLevels.add(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            sortedHwLevels.add(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3);
        }

        if (requiredLevel == deviceLevel) {
            return true;
        }

        for (int sortedlevel : sortedHwLevels) {
            if (sortedlevel == requiredLevel) {
                return true;
            } else if (sortedlevel == deviceLevel) {
                return false;
            }
        }
        return false; // Should never reach here
    }

    static SupportedValues checkModeIsSupported(List<String> values, String value, String default_value) {
        if( values != null && values.size() > 1 ) { // n.b., if there is only 1 supported value, we also return null, as no point offering the choice to the user (there are some devices, e.g., Samsung, that only have a scene mode of "auto")
            // make sure result is valid
            if( !values.contains(value) ) {
                if( values.contains(default_value) )
                    value = default_value;
                else
                    value = values.get(0);
            }
            return new SupportedValues(values, value);
        }
        return null;
    }
}

package com.aaron.cameraparams;

import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.Surface;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public interface ICameraController {

    void init();

    void release();

    String[] getSupportedCameraIds();

    boolean bindCameraId(String cameraId);

    int getSensorOrientation();

    boolean isFrontCamera();

    Size[] getSupportedPreviewSizes();

    Size filterSupportPreviewSizes(Size target);

    Size[] getSupportedVideoSizes();

    Size[] getSupportedPhotoSizes();

    void setPreviewSurface(Surface surface);

    void openCamera();

    void startRecording();

    void stopRecording();

    void takePicture();

    void onTakePictureComplete();

    void closeCamera();

    void setCameraSize(Size cameraSize);

    boolean setFocus(int x, int y);

//    void switchCameraMode(int type);

    Handler getBackgroundHandler();

    //     int MODE_TYPE_DELAY = 0;
//     int MODE_TYPE_SLOW = 1;
//     int MODE_TYPE_VIDEO = 2;
//     int MODE_TYPE_PHOTO = 3;
//     int MODE_TYPE_PANORAMIC = 4;
    int FLASH_OFF = 0;
    //     int FLASH_ON = 1;
    int FLASH_TORCH = 1;
    int FLASH_ON = 2;
    int FLASH_AUTO = 3;
    boolean isPanoStatus = false;

    float getMAXZoom();

    int[] getMAXAwbList();

    Range<Integer> getISORange();

    Range<Long> getExposureTimeRange();

    Range<Integer> getAERange();

    void setZoom(int num);

    void setISO(int num);

    void setAWB(int num);

    void setAE_exposure(int num);

    void setAutoFocus(boolean flag);

    void setLenFocus(int num);

    void setExposure(int num);

    void setHDRMode(boolean flag);

    boolean getHDRMode();

    void openFlash(int type);

    void setFaceDetectCallBack(FaceDetectCallBack callback);

    void setAutoFocusCallBack(AutoFocusCallBack callback);

    boolean isFrontFace();

    void openFace(boolean flag);

    void setCameraControllerCallback(CameraControllerCallback cameraControllerCallback);

    void takePictureComplete();

    void lockAE();

    void unlockAE();

    interface FaceDetectCallBack {
        void onDrawFace(CameraController.FaceData rect);

        void onClearFace();
    }

    interface AutoFocusCallBack {

        void focusStart(Message msg);

        void focusComplete();

        void focusError();
    }

    interface CameraControllerCallback {

        void onCameraOpenError();

        void onCameraSessionError();

        void onTakePictureCompleted();
    }

    public static class CameraFeatures {
        public boolean is_zoom_supported;
        public int max_zoom;
        public List<Integer> zoom_ratios;
        public boolean supports_face_detection;
        public List<CameraController.Size> picture_sizes;
        public List<CameraController.Size> video_sizes;
        public List<CameraController.Size> video_sizes_high_speed; // may be null if high speed not supported
        public List<CameraController.Size> preview_sizes;
        public List<String> supported_flash_values;
        public List<String> supported_focus_values;
        public int max_num_focus_areas;
        public float minimum_focus_distance;
        public boolean is_exposure_lock_supported;
        public boolean is_white_balance_lock_supported;
        public boolean is_video_stabilization_supported;
        public boolean is_photo_video_recording_supported;
        public boolean supports_white_balance_temperature;
        public int min_temperature;
        public int max_temperature;
        public boolean supports_iso_range;
        public int min_iso;
        public int max_iso;
        public boolean supports_exposure_time;
        public long min_exposure_time;
        public long max_exposure_time;
        public int min_exposure;
        public int max_exposure;
        public float exposure_step;
        public boolean can_disable_shutter_sound;
        public int tonemap_max_curve_points;
        public boolean supports_tonemap_curve;
        public boolean supports_expo_bracketing; // whether setBurstTye(BURSTTYPE_EXPO) can be used
        public int max_expo_bracketing_n_images;
        public boolean supports_focus_bracketing; // whether setBurstTye(BURSTTYPE_FOCUS) can be used
        public boolean supports_burst; // whether setBurstTye(BURSTTYPE_NORMAL) can be used
        public boolean supports_raw;
        public float view_angle_x; // horizontal angle of view in degrees (when unzoomed)
        public float view_angle_y; // vertical angle of view in degrees (when unzoomed)

        /** Returns whether any of the supplied sizes support the requested fps.
         */
        public static boolean supportsFrameRate(List<Size> sizes, int fps) {
            if( sizes == null )
                return false;
            for(Size size : sizes) {
                if( size.supportsFrameRate(fps) ) {
                    return true;
                }
            }
            return false;
        }

        public static Size findSize(List<Size> sizes, Size size, double fps, boolean return_closest) {
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
        public int compare(final CameraController.Size a, final CameraController.Size b) {
            return b.width * b.height - a.width * a.height;
        }
    }

    public static class Size {
        public final int width;
        public final int height;
        public boolean supports_burst; // for photo
        final List<int[]> fps_ranges; // for video
        public final boolean high_speed; // for video

        Size(int width, int height, List<int[]> fps_ranges, boolean high_speed) {
            this.width = width;
            this.height = height;
            this.supports_burst = true;
            this.fps_ranges = fps_ranges;
            this.high_speed = high_speed;
            Collections.sort(this.fps_ranges, new RangeSorter());
        }

        public Size(int width, int height) {
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

        public Area(Rect rect, int weight) {
            this.rect = rect;
            this.weight = weight;
        }
    }

    public interface FaceDetectionListener {
        void onFaceDetection(Face[] faces);
    }

    public interface PictureCallback {
        void onStarted(); // called immediately before we start capturing the picture
        void onCompleted(); // called after all relevant on*PictureTaken() callbacks have been called and returned
        void onPictureTaken(byte[] data);
        /** Only called if RAW is requested.
         *  Caller should call raw_image.close() when done with the image.
         */
        void onRawPictureTaken(RawImage raw_image);
        /** Only called if burst is requested.
         */
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

    public interface AutoFocusCallback {
        void onAutoFocus(boolean success);
    }

    public interface ContinuousFocusMoveCallback {
        void onContinuousFocusMove(boolean start);
    }

    public interface ErrorCallback {
        void onError();
    }

    public static class Face {
        public final int score;
        /* The has values from [-1000,-1000] (for top-left) to [1000,1000] (for bottom-right) for whatever is
         * the current field of view (i.e., taking zoom into account).
         */
        public final Rect rect;

        Face(int score, Rect rect) {
            this.score = score;
            this.rect = rect;
        }
    }

    public static class SupportedValues {
        public final List<String> values;
        public final String selected_value;
        SupportedValues(List<String> values, String selected_value) {
            this.values = values;
            this.selected_value = selected_value;
        }
    }
}

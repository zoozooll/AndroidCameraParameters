package com.aaron.cameraparams;

import android.os.Handler;
import android.os.Message;
import android.util.Range;
import android.util.Size;
import android.view.Surface;

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

    void tryToOpenCamera();

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


}

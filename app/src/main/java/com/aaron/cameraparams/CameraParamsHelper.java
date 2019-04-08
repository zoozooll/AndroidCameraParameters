package com.aaron.cameraparams;


import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import com.aaron.cameraparams.camera.CameraConfigsKt;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static android.hardware.camera2.CameraCharacteristics.*;

public class CameraParamsHelper {

    private Context context;
    private int cameraIndex;
    private String[] supportCameraIds;
    private CameraCharacteristics characteristics;
    private List<CameraCharacteristics.Key<?>> keyList;

    public CameraParamsHelper(Context context) {
        this.context = context;
        generalSupportedCameraIds();
    }

    private void generalSupportedCameraIds() {
        try {
            CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            supportCameraIds = manager.getCameraIdList();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String[] getSupportCameraIds() {
        return supportCameraIds;
    }

    public void bindCameraId(int index) {
        this.cameraIndex = index;
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            characteristics = manager.getCameraCharacteristics(supportCameraIds[index]);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public List<CameraCharacteristics.Key<?>> getAvailableKeys() {
        keyList = characteristics.getKeys();
//        Log.d("aaron", "characteristics " + keyList.get(3).getName());
        return keyList;
    }

    public String getCharacteristicInfo(CameraCharacteristics.Key key) {
        return keyValue(key, characteristics.get(key));
    }

    public <T> String keyValue(CameraCharacteristics.Key<T> key, T value) {
        if  (COLOR_CORRECTION_AVAILABLE_ABERRATION_MODES.equals(key)) {
             return CameraConfigsKt.getColorCorrectionAvailableAberrationMode((int[]) value);
        } else if (INFO_SUPPORTED_HARDWARE_LEVEL.equals(key)) {
            return CameraConfigsKt.getHardwareLevelInfo((Integer) value);
        } else if (CONTROL_AE_AVAILABLE_MODES.equals(key)) {
            return CameraConfigsKt.getAeAvailableModes((int[]) value);
        } else if (CONTROL_AF_AVAILABLE_MODES.equals(key)) {
            return CameraConfigsKt.getAfAvailableModes((int[]) value);
        } else if (CONTROL_AWB_AVAILABLE_MODES.equals(key)) {
            return CameraConfigsKt.getAwbAvailableModes((int[]) value);
        } else if (CONTROL_AVAILABLE_EFFECTS.equals(key)) {
            return CameraConfigsKt.getAvailableEffects((int[]) value);
        } else if (CONTROL_AVAILABLE_SCENE_MODES.equals(key)) {
            return CameraConfigsKt.getAvailableSceneModes((int[]) value);
        } else if (NOISE_REDUCTION_AVAILABLE_NOISE_REDUCTION_MODES.equals(key)) {
            return CameraConfigsKt.getAvailableNoiseReductionModes((int[]) value);
        } else if (REQUEST_AVAILABLE_CAPABILITIES.equals(key)) {
            return CameraConfigsKt.getRequestAvailableCapabilities((int[]) value);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && CONTROL_AVAILABLE_MODES.equals(key)) {
            return CameraConfigsKt.getAvailableModes((int[]) value);
        } else if (value instanceof int[]) {
            return ((Arrays.toString((int[]) value)));
        } else if (value instanceof float[]) {
            return ((Arrays.toString((float[]) value)));
        } else if (value instanceof boolean[]) {
            return ((Arrays.toString((boolean []) value)));
        } else if (value instanceof Object[]) {
            return ((Arrays.toString((Object[]) value)));
        } else if (value instanceof StreamConfigurationMap){
            return CameraConfigsKt.streamConfigurationMapToString((StreamConfigurationMap)value);
        } else {
            return value.toString();
        }
    }





}

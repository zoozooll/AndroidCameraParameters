package com.aaron.cameraparams;


import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Log;
import android.util.Range;
import android.util.Size;

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
             return getColorCorrectionAvailableAberrationMode((int[]) value);
        } else if (INFO_SUPPORTED_HARDWARE_LEVEL.equals(key)) {
            return getHardwareLevelInfo((Integer) value);
        } else if (value instanceof int[]) {
            return ((Arrays.toString((int[]) value)));
        } else if (value instanceof float[]) {
            return ((Arrays.toString((float[]) value)));
        } else if (value instanceof boolean[]) {
            return ((Arrays.toString((boolean []) value)));
        } else if (value instanceof Object[]) {
            return ((Arrays.toString((Object[]) value)));
        } else if (value instanceof StreamConfigurationMap){
            return CameraParamsHelper.streamConfigurationMapToString((StreamConfigurationMap)value);
        } else {
            return value.toString();
        }
    }


    private String getColorCorrectionAvailableAberrationMode(int[] value) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i : value) {
            switch (i) {
                case COLOR_CORRECTION_ABERRATION_MODE_OFF:
                    sb.append("OFF");
                    break;
                case COLOR_CORRECTION_MODE_FAST:
                    sb.append("FAST");
                    break;
                case COLOR_CORRECTION_MODE_HIGH_QUALITY:
                    sb.append("QUALITY");
                    break;
                default:
                    break;
            }
            sb.append(", ");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append("]");
        return sb.toString();
    }

    private String getHardwareLevelInfo(int value) {
        final int[] sortedHwLevels = {
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3
        };
        StringBuilder sb = new StringBuilder();
        for (int sortedlevel : sortedHwLevels) {
            if (value == sortedlevel) {
                sb.append(" [ ");
            }
            switch (sortedlevel) {
                case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY:
                    sb.append(" Legacy ");
                    break;
                case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL:
                    sb.append(" External ");
                    break;
                case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED:
                    sb.append(" Limited ");
                    break;
                case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL:
                    sb.append(" Full ");
                    break;
                case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3:
                    sb.append(" 3 ");
                    break;
                default:
                    break;
            }
            if (value == sortedlevel) {
                sb.append(" ] ");
            }
        }
        return sb.toString();
    }

    private static String streamConfigurationMapToString(StreamConfigurationMap map) {
        StringBuilder sb = new StringBuilder("StreamConfiguration\n(");
        appendOutputsString(map, sb);
        sb.append(", \n");
        appendHighResOutputsString(map, sb);
        sb.append(", \n");
        appendInputsString(map, sb);
        sb.append(", \n");
        appendValidOutputFormatsForInputString(map, sb);
        sb.append(", \n");
        appendHighSpeedVideoConfigurationsString(map, sb);
        sb.append(")");

        return sb.toString();
    }

    private static void appendOutputsString(StreamConfigurationMap map, StringBuilder sb) {
        sb.append("Outputs\n(");
        int[] formats = map.getOutputFormats();
        for (int format : formats) {
            Size[] sizes = map.getOutputSizes(format);
            if (sizes == null) {
                sb.append(String.format(Locale.ENGLISH, "[format:%s(%d), NULL], \n", formatToString(format), format));
                continue;
            }
            for (Size size : sizes) {
                long minFrameDuration = map.getOutputMinFrameDuration(format, size);
                long stallDuration = map.getOutputStallDuration(format, size);
                sb.append(String.format(Locale.ENGLISH, "[w:%d, h:%d, format:%s(%d), min_duration:%d, " +
                                "stall:%d], \n", size.getWidth(), size.getHeight(), formatToString(format),
                        format, minFrameDuration, stallDuration));
            }
        }
        // Remove the pending ", "
        if (sb.charAt(sb.length() - 1) == ' ') {
            sb.delete(sb.length() - 2, sb.length());
        }
        sb.append(")");
    }

    private static void appendHighResOutputsString(StreamConfigurationMap map, StringBuilder sb) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
            return;
        }
        sb.append("HighResolutionOutputs\n(");
        int[] formats = map.getOutputFormats();
        for (int format : formats) {
            Size[] sizes = map.getHighResolutionOutputSizes(format);
            if (sizes == null) continue;
            for (Size size : sizes) {
                long minFrameDuration = map.getOutputMinFrameDuration(format, size);
                long stallDuration = map.getOutputStallDuration(format, size);
                sb.append(String.format(Locale.ENGLISH, "[w:%d, h:%d, format:%s(%d), min_duration:%d, " +
                                "stall:%d], \n", size.getWidth(), size.getHeight(), formatToString(format),
                        format, minFrameDuration, stallDuration));
            }
        }
        // Remove the pending ", "
        if (sb.charAt(sb.length() - 1) == ' ') {
            sb.delete(sb.length() - 2, sb.length());
        }
        sb.append(")");
    }

    private static void appendInputsString(StreamConfigurationMap map, StringBuilder sb) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
            return;
        }
        sb.append("Inputs\n(");
        int[] formats = map.getInputFormats();
        for (int format : formats) {
            Size[] sizes = map.getInputSizes(format);
            for (Size size : sizes) {
                sb.append(String.format(Locale.ENGLISH, "[w:%d, h:%d, format:%s(%d)],\n ", size.getWidth(),
                        size.getHeight(), formatToString(format), format));
            }
        }
        // Remove the pending ", "
        if (sb.charAt(sb.length() - 1) == ' ') {
            sb.delete(sb.length() - 2, sb.length());
        }
        sb.append(")");
    }

    private static void appendValidOutputFormatsForInputString(StreamConfigurationMap map, StringBuilder sb) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
            return;
        }
        sb.append("ValidOutputFormatsForInput\n(");
        int[] inputFormats = map.getInputFormats();
        for (int inputFormat : inputFormats) {
            sb.append(String.format(Locale.ENGLISH, "[in:%s(%d), out:", formatToString(inputFormat), inputFormat));
            int[] outputFormats = map.getValidOutputFormatsForInput(inputFormat);
            for (int i = 0; i < outputFormats.length; i++) {
                sb.append(String.format(Locale.ENGLISH, "%s(%d)", formatToString(outputFormats[i]),
                        outputFormats[i]));
                if (i < outputFormats.length - 1) {
                    sb.append(", ");
                }
            }
            sb.append("], \n");
        }
        // Remove the pending ", "
        if (sb.charAt(sb.length() - 1) == ' ') {
            sb.delete(sb.length() - 2, sb.length());
        }
        sb.append(")");
    }

    private static void appendHighSpeedVideoConfigurationsString(StreamConfigurationMap map, StringBuilder sb) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
            return;
        }
        sb.append("HighSpeedVideoConfigurations\n(");
        Size[] sizes = map.getHighSpeedVideoSizes();
        for (Size size : sizes) {
            Range<Integer>[] ranges = map.getHighSpeedVideoFpsRangesFor(size);
            for (Range<Integer> range : ranges) {
                sb.append(String.format(Locale.ENGLISH, "[w:%d, h:%d, min_fps:%d, max_fps:%d], \n", size.getWidth(),
                        size.getHeight(), range.getLower(), range.getUpper()));
            }
        }
        // Remove the pending ", "
        if (sb.charAt(sb.length() - 1) == ' ') {
            sb.delete(sb.length() - 2, sb.length());
        }
        sb.append(")");
    }

    private static String formatToString(int format) {
        switch (format) {
            case ImageFormat.YV12:
                return "YV12";
            case ImageFormat.YUV_420_888:
                return "YUV_420_888";
            case ImageFormat.NV21:
                return "NV21";
            case ImageFormat.NV16:
                return "NV16";
            case PixelFormat.RGB_565:
                return "RGB_565";
            case PixelFormat.RGBA_8888:
                return "RGBA_8888";
            case PixelFormat.RGBX_8888:
                return "RGBX_8888";
            case PixelFormat.RGB_888:
                return "RGB_888";
            case ImageFormat.JPEG:
                return "JPEG";
            case ImageFormat.YUY2:
                return "YUY2";
            case 0x20203859:
                return "Y8";
            case 0x20363159:
                return "Y16";
            case ImageFormat.RAW_SENSOR:
                return "RAW_SENSOR";
            case ImageFormat.RAW_PRIVATE:
                return "RAW_PRIVATE";
            case ImageFormat.RAW10:
                return "RAW10";
            case ImageFormat.DEPTH16:
                return "DEPTH16";
            case ImageFormat.DEPTH_POINT_CLOUD:
                return "DEPTH_POINT_CLOUD";
            case 0x1002:
                return "RAW_DEPTH";
            case ImageFormat.PRIVATE:
                return "PRIVATE";
            default:
                return "UNKNOWN";
        }
    }

}

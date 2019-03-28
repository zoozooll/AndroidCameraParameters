package com.aaron.cameraparams.camera;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.RggbChannelVector;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.hardware.camera2.params.TonemapCurve;
import android.location.Location;
import android.media.*;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Range;
import android.util.SizeF;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.*;

public class CameraController implements ICameraController {

    private static final String TAG = "CameraController";
    private final static int tonemap_max_curve_points_c = 64;
    // for BURSTTYPE_EXPO:
    private final static int max_expo_bracketing_n_images = 5; // could be more, but limit to 5 for now
    private static final int STATE_NORMAL = 0;
    private static final int STATE_WAITING_AUTOFOCUS = 1;
    private static final int STATE_WAITING_PRECAPTURE_START = 2;
    private static final int STATE_WAITING_PRECAPTURE_DONE = 3;
    private static final int STATE_WAITING_FAKE_PRECAPTURE_START = 4;
    private static final int STATE_WAITING_FAKE_PRECAPTURE_DONE = 5;
    private static final int BURSTTYPE_NONE = 0;
    private static final int BURSTTYPE_EXPO = 1;
    private static final int BURSTTYPE_FOCUS = 2;
    private static final int BURSTTYPE_NORMAL = 3;
    private static final int BURSTTYPE_CONTINUOUS = 4;
    private static final long precapture_start_timeout_c = 2000;
    private static final long precapture_done_timeout_c = 3000;
    private final static int min_white_balance_temperature_c = 1000;
    private final static int max_white_balance_temperature_c = 15000;
    private final static boolean do_af_trigger_for_continuous = true;


    private ICameraController.CameraFeatures camera_features;
    private final ICameraController.ErrorCallback preview_error_cb;
    private final ICameraController.ErrorCallback camera_error_cb;
    private final Object image_reader_lock = new Object(); // lock to make sure we only handle one image being available at a time
    private final Object open_camera_lock = new Object(); // lock to wait for camera to be opened from CameraDevice.StateCallback
    private final Object create_capture_session_lock = new Object(); // lock to wait for capture session to be created from CameraCaptureSession.StateCallback
    private final List<byte[]> pending_burst_images = new ArrayList<>(); // burst images that have been captured so far, but not yet sent to the application
    private final MediaActionSound media_action_sound = new MediaActionSound();
    private final CameraSettings camera_settings = new CameraSettings();
    private Context context;
    private CameraDevice camera;
    private String cameraIdS;
    private CameraCharacteristics characteristics;
    // cached characteristics (use this for values that need to be frequently accessed, e.g., per frame, to improve performance);
    private int characteristics_sensor_orientation;
    private boolean characteristics_is_front_facing;
    private int current_zoom_value;
    private boolean supports_face_detect_mode_simple;
    private boolean supports_face_detect_mode_full;
    private boolean supports_photo_video_recording;
    private CameraCaptureSession captureSession;
    private CaptureRequest.Builder previewBuilder;
    private boolean previewIsVideoMode;
    private ICameraController.AutoFocusCallback autofocus_cb;
    private boolean capture_follows_autofocus_hint;
    private ICameraController.FaceDetectionListener face_detection_listener;
    private int last_faces_detected = -1;
    private ImageReader imageReader;
    private int burst_type = BURSTTYPE_NONE;
    private int expo_bracketing_n_images = 3;
    private double expo_bracketing_stops = 2.0;
    private boolean use_expo_fast_burst = true;
    // for BURSTTYPE_FOCUS:
    private boolean focus_bracketing_in_progress; // whether focus bracketing in progress; set back to false to cancel
    private int focus_bracketing_n_images = 3;
    private float focus_bracketing_source_distance = 0.0f;
    private float focus_bracketing_target_distance = 0.0f;
    private boolean focus_bracketing_add_infinity = false;
    // for BURSTTYPE_NORMAL:
    private boolean burst_for_noise_reduction; // chooses number of burst images and other settings for Open Camera's noise reduction (NR) photo mode
    private boolean noise_reduction_low_light; // if burst_for_noise_reduction==true, whether to optimise for low light scenes
    private int burst_requested_n_images; // if burst_for_noise_reduction==false, this gives the number of images for the burst
    //for BURSTTYPE_CONTINUOUS:
    private boolean continuous_burst_in_progress; // whether we're currently taking a continuous burst
    private boolean continuous_burst_requested_last_capture; // whether we've requested the last capture
    private boolean optimise_ae_for_dro = false;
    private boolean want_raw;
    //private boolean want_raw = true;
    private int max_raw_images;
    private android.util.Size raw_size;
    private ImageReader imageReaderRaw;
    private OnRawImageAvailableListener onRawImageAvailableListener;
    private ICameraController.PictureCallback picture_cb;
    private boolean jpeg_todo; // whether we are still waiting for JPEG images
    private boolean raw_todo; // whether we are still waiting for RAW images
    private boolean done_all_captures; // whether we've received the capture for the image (or all images if a burst)
    //private CaptureRequest pending_request_when_ready;
    private int n_burst; // number of expected (remaining) burst images in this capture
    private int n_burst_taken; // number of burst images taken so far in this capture
    private int n_burst_total; // total number of expected burst images in this capture (if known)
    private boolean burst_single_request; // if true then the burst images are returned in a single call to onBurstPictureTaken(), if false, then multiple calls to onPictureTaken() are made as soon as the image is available
    private List<CaptureRequest> slow_burst_capture_requests; // the set of burst capture requests - used when not using captureBurst() (e.g., when use_expo_fast_burst==false, or for focus bracketing)
    private long slow_burst_start_ms = 0; // time when burst started (used for measuring performance of captures when not using captureBurst())
    private ICameraController.ErrorCallback take_picture_error_cb;
    private boolean want_video_high_speed;
    private boolean is_video_high_speed; // whether we're actually recording in high speed
    private List<int[]> ae_fps_ranges;
    private List<int[]> hs_fps_ranges;
    //private ImageReader previewImageReader;
    private SurfaceTexture texture;
    private Surface surface_texture;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private Surface video_recorder_surface;
    private int preview_width;
    private int preview_height;
    private int picture_width;
    private int picture_height;
    private int state = STATE_NORMAL;
    private long precapture_state_change_time_ms = -1; // time we changed state for precapture modes
    private boolean ready_for_capture;
    private boolean use_fake_precapture; // see CameraController.setUseCamera2FakeFlash() for details - this is the user/application setting, see use_fake_precapture_mode for whether fake precapture is enabled (as we may do this for other purposes, e.g., front screen flash)
    private boolean use_fake_precapture_mode; // true if either use_fake_precapture is true, or we're temporarily using fake precapture mode (e.g., for front screen flash or exposure bracketing)
    private boolean fake_precapture_torch_performed; // whether we turned on torch to do a fake precapture
    private boolean fake_precapture_torch_focus_performed; // whether we turned on torch to do an autofocus, in fake precapture mode
    private boolean fake_precapture_use_flash; // whether we decide to use flash in auto mode (if fake_precapture_use_autoflash_time_ms != -1)
    private long fake_precapture_use_flash_time_ms = -1; // when we last checked to use flash in auto mode
    private ICameraController.ContinuousFocusMoveCallback continuous_focus_move_callback;
    private boolean sounds_enabled = true;
    private boolean has_received_frame;
    private boolean capture_result_is_ae_scanning;
    private Integer capture_result_ae; // latest ae_state, null if not available
    private boolean is_flash_required; // whether capture_result_ae suggests FLASH_REQUIRED? Or in neither FLASH_REQUIRED nor CONVERGED, this stores the last known result
    private boolean modified_from_camera_settings;
    // if modified_from_camera_settings set to true, then we've temporarily requested captures with settings such as
    // exposure modified from the normal ones in camera_settings
    private boolean capture_result_has_white_balance_rggb;
    private RggbChannelVector capture_result_white_balance_rggb;
    private boolean capture_result_has_iso;
    private int capture_result_iso;
    private boolean capture_result_has_exposure_time;
    private long capture_result_exposure_time;
    private boolean capture_result_has_frame_duration;
    private long capture_result_frame_duration;
    private boolean is_samsung_s7;
    private boolean push_repeating_request_when_torch_off = false;
    private CaptureRequest push_repeating_request_when_torch_off_id = null;
    private CaptureRequest fake_precapture_turn_on_torch_id = null; // the CaptureRequest used to turn on torch when starting the "fake" precapture
    private final CameraCaptureSession.CaptureCallback previewCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        private long last_process_frame_number = 0;
        private int last_af_state = -1;

        private RequestTagType getRequestTagType(@NonNull CaptureRequest request) {
            Object tag = request.getTag();
            if (tag == null)
                return null;
            RequestTagObject requestTag = (RequestTagObject) tag;
            return requestTag.getType();
        }

        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            if (getRequestTagType(request) == RequestTagType.CAPTURE) {
                //return;
            }
            process(request, result);
            processCompleted(request, result);
            super.onCaptureCompleted(session, request, result); // API docs say this does nothing, but call it just to be safe (as with Google Camera)
        }

        /** Processes either a partial or total result.
         */
        private void process(CaptureRequest request, CaptureResult result) {
            if (result.getFrameNumber() < last_process_frame_number) {
                return;
            }
            last_process_frame_number = result.getFrameNumber();

            // use Integer instead of int, so can compare to null: Google Play crashes confirmed that this can happen; Google Camera also ignores cases with null af state
            Integer af_state = result.get(CaptureResult.CONTROL_AF_STATE);

            // CONTROL_AE_STATE can be null on some devices, so as with af_state, use Integer
            Integer ae_state = result.get(CaptureResult.CONTROL_AE_STATE);
            Integer flash_mode = result.get(CaptureResult.FLASH_MODE);
            if (use_fake_precapture_mode && (fake_precapture_torch_focus_performed || fake_precapture_torch_performed) && flash_mode != null && flash_mode == CameraMetadata.FLASH_MODE_TORCH) {
                // don't change ae state while torch is on for fake flash
            } else if (ae_state == null) {
                capture_result_ae = null;
                is_flash_required = false;
            } else if (!ae_state.equals(capture_result_ae)) {
                // need to store this before calling the autofocus callbacks below
                capture_result_ae = ae_state;
                // capture_result_ae should always be non-null here, as we've already handled ae_state separately
                if (capture_result_ae == CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED && !is_flash_required) {
                    is_flash_required = true;
                } else if (capture_result_ae == CaptureResult.CONTROL_AE_STATE_CONVERGED && is_flash_required) {
                    is_flash_required = false;
                }
            }

            if (af_state != null && af_state == CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN) {
                ready_for_capture = false;
            } else {
                ready_for_capture = true;
                if (autofocus_cb != null && (!do_af_trigger_for_continuous || use_fake_precapture_mode) && focusIsContinuous()) {
                    Integer focus_mode = previewBuilder.get(CaptureRequest.CONTROL_AF_MODE);
                    if (focus_mode != null && focus_mode == CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE) {
                        // need to check af_state != null, I received Google Play crash in 1.33 where it was null
                        boolean focus_success = af_state != null && (af_state == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED || af_state == CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED);
//						if( af_state == null ) {
//							test_af_state_null_focus++;
//						}
                        autofocus_cb.onAutoFocus(focus_success);
                        autofocus_cb = null;
                        capture_follows_autofocus_hint = false;
                    }
                }
            }

            if (ae_state != null && ae_state == CaptureResult.CONTROL_AE_STATE_SEARCHING) {
                capture_result_is_ae_scanning = true;
            } else {
                capture_result_is_ae_scanning = false;
            }


            if (fake_precapture_turn_on_torch_id != null && fake_precapture_turn_on_torch_id == request) {
                fake_precapture_turn_on_torch_id = null;
            }

            if (state == STATE_NORMAL) {
                // do nothing
            } else if (state == STATE_WAITING_AUTOFOCUS) {
                if (af_state == null) {
                    // autofocus shouldn't really be requested if af not available, but still allow this rather than getting stuck waiting for autofocus to complete
//					test_af_state_null_focus++;
                    state = STATE_NORMAL;
                    precapture_state_change_time_ms = -1;
                    if (autofocus_cb != null) {
                        autofocus_cb.onAutoFocus(false);
                        autofocus_cb = null;
                    }
                    capture_follows_autofocus_hint = false;
                } else if (af_state != last_af_state) {
                    // check for autofocus completing
                    if (af_state == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED || af_state == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED /*||
							af_state == CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED || af_state == CaptureResult.CONTROL_AF_STATE_PASSIVE_UNFOCUSED*/
                    ) {
                        boolean focus_success = af_state == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED || af_state == CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED;
                        state = STATE_NORMAL;
                        precapture_state_change_time_ms = -1;
                        if (use_fake_precapture_mode && fake_precapture_torch_focus_performed) {
                            fake_precapture_torch_focus_performed = false;
                            if (!capture_follows_autofocus_hint) {

                                // same hack as in setFlashValue() - for fake precapture we need to turn off the torch mode that was set, but
                                // at least on Nexus 6, we need to turn to flash_off to turn off the torch!
                                String saved_flash_value = camera_settings.flash_value;
                                camera_settings.flash_value = "flash_off";
                                camera_settings.setAEMode(previewBuilder, false);
                                try {
                                    capture();
                                } catch (CameraAccessException e) {
                                    e.printStackTrace();
                                }

                                // now set the actual (should be flash auto or flash on) mode
                                camera_settings.flash_value = saved_flash_value;
                                camera_settings.setAEMode(previewBuilder, false);
                                try {
                                    setRepeatingRequest();
                                } catch (CameraAccessException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        if (autofocus_cb != null) {
                            autofocus_cb.onAutoFocus(focus_success);
                            autofocus_cb = null;
                        }
                        capture_follows_autofocus_hint = false;
                    }
                }
            } else if (state == STATE_WAITING_PRECAPTURE_START) {
                if (ae_state == null || ae_state == CaptureResult.CONTROL_AE_STATE_PRECAPTURE /*|| ae_state == CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED*/) {
                    // we have to wait for CONTROL_AE_STATE_PRECAPTURE; if we allow CONTROL_AE_STATE_FLASH_REQUIRED, then on Nexus 6 at least we get poor quality results with flash:
                    // varying levels of brightness, sometimes too bright or too dark, sometimes with blue tinge, sometimes even with green corruption
                    // similarly photos with flash come out too dark on OnePlus 3T
                    state = STATE_WAITING_PRECAPTURE_DONE;
                    precapture_state_change_time_ms = System.currentTimeMillis();
                } else if (precapture_state_change_time_ms != -1 && System.currentTimeMillis() - precapture_state_change_time_ms > precapture_start_timeout_c) {
                    // hack - give up waiting - sometimes we never get a CONTROL_AE_STATE_PRECAPTURE so would end up stuck
                    // always log error, so we can look for it when manually testing with logging disabled
                    Log.e(TAG, "precapture start timeout");
//					count_precapture_timeout++;
                    state = STATE_WAITING_PRECAPTURE_DONE;
                    precapture_state_change_time_ms = System.currentTimeMillis();
                }
            } else if (state == STATE_WAITING_PRECAPTURE_DONE) {
                if (ae_state == null || ae_state != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                    state = STATE_NORMAL;
                    precapture_state_change_time_ms = -1;
                    takePictureAfterPrecapture();
                } else if (precapture_state_change_time_ms != -1 && System.currentTimeMillis() - precapture_state_change_time_ms > precapture_done_timeout_c) {
                    // just in case
                    // always log error, so we can look for it when manually testing with logging disabled
                    Log.e(TAG, "precapture done timeout");
//					count_precapture_timeout++;
                    state = STATE_NORMAL;
                    precapture_state_change_time_ms = -1;
                    takePictureAfterPrecapture();
                }
            } else if (state == STATE_WAITING_FAKE_PRECAPTURE_START) {

                if (fake_precapture_turn_on_torch_id == null && (ae_state == null || ae_state == CaptureResult.CONTROL_AE_STATE_SEARCHING)) {
                    state = STATE_WAITING_FAKE_PRECAPTURE_DONE;
                    precapture_state_change_time_ms = System.currentTimeMillis();
                } else if (precapture_state_change_time_ms != -1 && System.currentTimeMillis() - precapture_state_change_time_ms > precapture_start_timeout_c) {
                    // just in case
                    // always log error, so we can look for it when manually testing with logging disabled
                    Log.e(TAG, "fake precapture start timeout");
//					count_precapture_timeout++;
                    state = STATE_WAITING_FAKE_PRECAPTURE_DONE;
                    precapture_state_change_time_ms = System.currentTimeMillis();
                    fake_precapture_turn_on_torch_id = null;
                }
            } else if (state == STATE_WAITING_FAKE_PRECAPTURE_DONE) {
                // wait for af and ae scanning to end (need to check af too, as in continuous focus mode, a focus may start again after switching torch on for the fake precapture)
                if (ready_for_capture && (ae_state == null || ae_state != CaptureResult.CONTROL_AE_STATE_SEARCHING)) {
                    state = STATE_NORMAL;
                    precapture_state_change_time_ms = -1;
                    takePictureAfterPrecapture();
                } else if (precapture_state_change_time_ms != -1 && System.currentTimeMillis() - precapture_state_change_time_ms > precapture_done_timeout_c) {
                    // sometimes camera can take a while to stop ae/af scanning, better to just go ahead and take photo
                    // always log error, so we can look for it when manually testing with logging disabled
                    Log.e(TAG, "fake precapture done timeout");
//					count_precapture_timeout++;
                    state = STATE_NORMAL;
                    precapture_state_change_time_ms = -1;
                    takePictureAfterPrecapture();
                }
            }

            if (af_state != null && af_state == CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN && af_state != last_af_state) {
                if (continuous_focus_move_callback != null) {
                    continuous_focus_move_callback.onContinuousFocusMove(true);
                }
            } else if (af_state != null && last_af_state == CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN && af_state != last_af_state) {
                if (continuous_focus_move_callback != null) {
                    continuous_focus_move_callback.onContinuousFocusMove(false);
                }
            }

            if (af_state != null && af_state != last_af_state) {
                last_af_state = af_state;
            }
        }

        /** Processes a total result.
         */
        private void processCompleted(CaptureRequest request, CaptureResult result) {
			/*if( MyDebug.LOG )
				Log.d(TAG, "processCompleted");*/

            if (!has_received_frame) {
                has_received_frame = true;
            }

            if (modified_from_camera_settings) {
                // don't update capture results!
                // otherwise have problem taking HDR photos twice in a row, the second one will pick up the exposure time as
                // being from the long exposure of the previous HDR/expo burst!
            } else if (result.get(CaptureResult.SENSOR_SENSITIVITY) != null) {
                capture_result_has_iso = true;
                capture_result_iso = result.get(CaptureResult.SENSOR_SENSITIVITY);
            } else {
                capture_result_has_iso = false;
            }

            if (modified_from_camera_settings) {
                // see note above
            } else if (result.get(CaptureResult.SENSOR_EXPOSURE_TIME) != null) {
                capture_result_has_exposure_time = true;
                capture_result_exposure_time = result.get(CaptureResult.SENSOR_EXPOSURE_TIME);
                if (capture_result_exposure_time <= 0) {
                    // wierd bug seen on Nokia 8
                    capture_result_has_exposure_time = false;
                }
            } else {
                capture_result_has_exposure_time = false;
            }

            if (modified_from_camera_settings) {
                // see note above
            } else if (result.get(CaptureResult.SENSOR_FRAME_DURATION) != null) {
                capture_result_has_frame_duration = true;
                capture_result_frame_duration = result.get(CaptureResult.SENSOR_FRAME_DURATION);
            } else {
                capture_result_has_frame_duration = false;
            }
            {
                RggbChannelVector vector = result.get(CaptureResult.COLOR_CORRECTION_GAINS);
                if (modified_from_camera_settings) {
                    // see note above
                } else if (vector != null) {
                    capture_result_has_white_balance_rggb = true;
                    capture_result_white_balance_rggb = vector;
                }
            }

            if (face_detection_listener != null && previewBuilder != null) {
                Integer face_detect_mode = previewBuilder.get(CaptureRequest.STATISTICS_FACE_DETECT_MODE);
                if (face_detect_mode != null && face_detect_mode != CaptureRequest.STATISTICS_FACE_DETECT_MODE_OFF) {
                    Rect sensor_rect = getViewableRect();
                    android.hardware.camera2.params.Face[] camera_faces = result.get(CaptureResult.STATISTICS_FACES);
                    if (camera_faces != null) {
                        if (camera_faces.length == 0 && last_faces_detected == 0) {
                            // no point continually calling the callback if 0 faces detected (same behaviour as CameraController1)
                        } else {
                            last_faces_detected = camera_faces.length;
                            ICameraController.Face[] faces = new ICameraController.Face[camera_faces.length];
                            for (int i = 0; i < camera_faces.length; i++) {
                                faces[i] = convertFromCameraFace(sensor_rect, camera_faces[i]);
                            }
                            face_detection_listener.onFaceDetection(faces);
                        }
                    }
                }
            }

            if (push_repeating_request_when_torch_off && push_repeating_request_when_torch_off_id == request && previewBuilder != null) {
                Integer flash_state = result.get(CaptureResult.FLASH_STATE);
                if (flash_state != null && flash_state == CaptureResult.FLASH_STATE_READY) {
                    push_repeating_request_when_torch_off = false;
                    push_repeating_request_when_torch_off_id = null;
                    try {
                        setRepeatingRequest();
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (getRequestTagType(request) == RequestTagType.CAPTURE) {
                modified_from_camera_settings = false;


                if (onRawImageAvailableListener != null) {
                    onRawImageAvailableListener.setCaptureResult(result);
                }
                // actual parsing of image data is done in the imageReader's OnImageAvailableListener()
                // need to cancel the autofocus, and restart the preview after taking the photo
                // Camera2Basic does a capture then sets a repeating request - do the same here just to be safe
                if (previewBuilder != null) {
                    previewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
                    String saved_flash_value = camera_settings.flash_value;
                    if (use_fake_precapture_mode && fake_precapture_torch_performed) {
                        // same hack as in setFlashValue() - for fake precapture we need to turn off the torch mode that was set, but
                        // at least on Nexus 6, we need to turn to flash_off to turn off the torch!
                        camera_settings.flash_value = "flash_off";
                    }
                    // if not using fake precapture, not sure if we need to set the ae mode, but the AE mode is set again in Camera2Basic
                    camera_settings.setAEMode(previewBuilder, false);
                    // n.b., if capture/setRepeatingRequest throw exception, we don't call the take_picture_error_cb.onError() callback, as the photo should have been taken by this point
                    try {
                        capture();
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                    if (use_fake_precapture_mode && fake_precapture_torch_performed) {
                        // now set up the request to switch to the correct flash value
                        camera_settings.flash_value = saved_flash_value;
                        camera_settings.setAEMode(previewBuilder, false);
                    }
                    previewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE); // ensure set back to idle
                    try {
                        setRepeatingRequest();
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                        preview_error_cb.onError();
                    }
                }
                fake_precapture_torch_performed = false;

                if (burst_type == BURSTTYPE_FOCUS && previewBuilder != null) { // make sure camera wasn't released in the meantime
                    camera_settings.setFocusDistance(previewBuilder);
                    try {
                        setRepeatingRequest();
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }


                // Important that we only call the picture onCompleted callback after we've received the capture request, so
                // we need to check if we already received all the images.
                // Also needs to be run on UI backgroundThread.
                // Needed for testContinuousPictureFocusRepeat on Nexus 7; also testable on other devices via
                // testContinuousPictureFocusRepeatWaitCaptureResult.
                final Activity activity = (Activity) context;
                activity.runOnUiThread(new Runnable() {

                    public void run() {
                        synchronized (image_reader_lock) {
                            done_all_captures = true;
                            checkImagesCompleted();
                        }
                    }
                });
            }
        }
    };

    public CameraController(Context context, final ICameraController.ErrorCallback preview_error_cb, final ICameraController.ErrorCallback camera_error_cb) {

        this.context = context;
        this.preview_error_cb = preview_error_cb;
        this.camera_error_cb = camera_error_cb;

        this.is_samsung_s7 = Build.MODEL.toLowerCase(Locale.US).contains("sm-g93");

        startBackgroundThread();
        // preload sounds to reduce latency - important so that START_VIDEO_RECORDING sound doesn't play after video has started (which means it'll be heard in the resultant video)
        media_action_sound.load(MediaActionSound.START_VIDEO_RECORDING);
        media_action_sound.load(MediaActionSound.STOP_VIDEO_RECORDING);
        media_action_sound.load(MediaActionSound.SHUTTER_CLICK);
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());

    }

    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            // should only close backgroundThread after closing the camera, otherwise we get messages "sending message to a Handler on a dead backgroundThread"
            // see https://sourceforge.net/p/opencamera/discussion/general/thread/32c2b01b/?limit=25
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean openCamera(int cameraId) {
        final CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);

        class MyStateCallback extends CameraDevice.StateCallback {
            boolean callback_done; // must sychronize on this and notifyAll when setting to true
            boolean first_callback = true; // Google Camera says we may get multiple callbacks, but only the first indicates the status of the camera opening operation

            public void onOpened(@NonNull CameraDevice cam) {
				/*if( true ) // uncomment to test timeout code
					return;*/
                if (first_callback) {
                    first_callback = false;

                    try {
                        // we should be able to get characteristics at any time, but Google Camera only does so when camera opened - so do so similarly to be safe
                        characteristics = manager.getCameraCharacteristics(cameraIdS);
                        // now read cached values
                        characteristics_sensor_orientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                        characteristics_is_front_facing = characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT;

                        CameraController.this.camera = cam;

                        // note, this won't start the preview yet, but we create the previewBuilder in order to start setting camera parameters
                        createPreviewRequest();
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                        // don't throw CameraControllerException here - instead error is handled by setting callback_done to callback_done, and the fact that camera will still be null
                    }

                    synchronized (open_camera_lock) {
                        callback_done = true;
                        open_camera_lock.notifyAll();
                    }
                }
            }


            public void onClosed(@NonNull CameraDevice cam) {
                // caller should ensure camera variables are set to null
                if (first_callback) {
                    first_callback = false;
                }
            }


            public void onDisconnected(@NonNull CameraDevice cam) {
                if (first_callback) {
                    first_callback = false;
                    // must call close() if disconnected before camera was opened
                    // need to set the camera to null first, as closing the camera may take some time, and we don't want any other operations to continue (if called from main backgroundThread)
                    CameraController.this.camera = null;
                    cam.close();
                    synchronized (open_camera_lock) {
                        callback_done = true;
                        open_camera_lock.notifyAll();
                    }
                }
            }


            public void onError(@NonNull CameraDevice cam, int error) {
                // n.b., as this is potentially serious error, we always log even if MyDebug.LOG is false
                Log.e(TAG, "camera error: " + error);
                if (first_callback) {
                    first_callback = false;
                }
                CameraController.this.onError(cam);
                synchronized (open_camera_lock) {
                    callback_done = true;
                    open_camera_lock.notifyAll();
                }
            }
        }
        final MyStateCallback myStateCallback = new MyStateCallback();

        try {
            this.cameraIdS = manager.getCameraIdList()[cameraId];
            manager.openCamera(cameraIdS, myStateCallback, backgroundHandler);
        } catch (SecurityException e) {
            // Google Camera catches SecurityException
            e.printStackTrace();
        } catch (Exception e) {
            // have seen this from Google Play
            e.printStackTrace();
        }

        // set up a timeout - sometimes if the camera has got in a state where it can't be opened until after a reboot, we'll never even get a myStateCallback callback called
        backgroundHandler.postDelayed(new Runnable() {

            public void run() {
                synchronized (open_camera_lock) {
                    if (!myStateCallback.callback_done) {
                        // n.b., as this is potentially serious error, we always log even if MyDebug.LOG is false
                        Log.e(TAG, "timeout waiting for camera callback");
                        myStateCallback.first_callback = true;
                        myStateCallback.callback_done = true;
                        open_camera_lock.notifyAll();
                    }
                }
            }
        }, 10000);

        // need to wait until camera is opened
        // whilst this blocks, this should be running on a background backgroundThread anyway (see Preview.openCamera()) - due to maintaining
        // compatibility with the way the old camera API works, it's easier to handle running on a background backgroundThread at a higher level,
        // rather than exiting here
        synchronized (open_camera_lock) {
            while (!myStateCallback.callback_done) {
                try {
                    // release the lock, and wait until myStateCallback calls notifyAll()
                    open_camera_lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        if (camera == null) {
            // n.b., as this is potentially serious error, we always log even if MyDebug.LOG is false
            Log.e(TAG, "camera failed to open");
            return false;
        }
        return true;
    }


    /**
     * Converts a white balance temperature to red, green even, green odd and blue components.
     */
    private RggbChannelVector convertTemperatureToRggb(int temperature_kelvin) {
        float temperature = temperature_kelvin / 100.0f;
        float red;
        float green;
        float blue;

        if (temperature <= 66) {
            red = 255;
        } else {
            red = temperature - 60;
            red = (float) (329.698727446 * (Math.pow((double) red, -0.1332047592)));
            if (red < 0)
                red = 0;
            if (red > 255)
                red = 255;
        }

        if (temperature <= 66) {
            green = temperature;
            green = (float) (99.4708025861 * Math.log(green) - 161.1195681661);
            if (green < 0)
                green = 0;
            if (green > 255)
                green = 255;
        } else {
            green = temperature - 60;
            green = (float) (288.1221695283 * (Math.pow((double) green, -0.0755148492)));
            if (green < 0)
                green = 0;
            if (green > 255)
                green = 255;
        }

        if (temperature >= 66)
            blue = 255;
        else if (temperature <= 19)
            blue = 0;
        else {
            blue = temperature - 10;
            blue = (float) (138.5177312231 * Math.log(blue) - 305.0447927307);
            if (blue < 0)
                blue = 0;
            if (blue > 255)
                blue = 255;
        }

        return new RggbChannelVector((red / 255) * 2, (green / 255), (green / 255), (blue / 255) * 2);
    }

    /**
     * Converts a red, green even, green odd and blue components to a white balance temperature.
     * Note that this is not necessarily an inverse of convertTemperatureToRggb, since many rggb
     * values can map to the same temperature.
     */
    private int convertRggbToTemperature(RggbChannelVector rggbChannelVector) {
        float red = rggbChannelVector.getRed();
        float green_even = rggbChannelVector.getGreenEven();
        float green_odd = rggbChannelVector.getGreenOdd();
        float blue = rggbChannelVector.getBlue();
        float green = 0.5f * (green_even + green_odd);

        float max = Math.max(red, blue);
        if (green > max)
            green = max;

        float scale = 255.0f / max;
        red *= scale;
        green *= scale;
        blue *= scale;

        int red_i = (int) red;
        int green_i = (int) green;
        int blue_i = (int) blue;
        int temperature;
        if (red_i == blue_i) {
            temperature = 6600;
        } else if (red_i > blue_i) {
            // temperature <= 6600
            int t_g = (int) (100 * Math.exp((green_i + 161.1195681661) / 99.4708025861));
            if (blue_i == 0) {
                temperature = t_g;
            } else {
                int t_b = (int) (100 * (Math.exp((blue_i + 305.0447927307) / 138.5177312231) + 10));
                temperature = (t_g + t_b) / 2;
            }
        } else {
            // temperature >= 6700
            if (red_i <= 1 || green_i <= 1) {
                temperature = max_white_balance_temperature_c;
            } else {
                int t_r = (int) (100 * (Math.pow(red_i / 329.698727446, 1.0 / -0.1332047592) + 60.0));
                int t_g = (int) (100 * (Math.pow(green_i / 288.1221695283, 1.0 / -0.0755148492) + 60.0));
                temperature = (t_r + t_g) / 2;
            }
        }
        temperature = Math.max(temperature, min_white_balance_temperature_c);
        temperature = Math.min(temperature, max_white_balance_temperature_c);
        return temperature;
    }

    private void onError(@NonNull CameraDevice cam) {
        Log.e(TAG, "onError");
        boolean camera_already_opened = this.camera != null;
        // need to set the camera to null first, as closing the camera may take some time, and we don't want any other operations to continue (if called from main backgroundThread)
        this.camera = null;
        cam.close();

        if (camera_already_opened) {
            // need to communicate the problem to the application
            // n.b., as this is potentially serious error, we always log even if MyDebug.LOG is false
            Log.e(TAG, "error occurred after camera was opened");
            camera_error_cb.onError();
        }
    }

    public void release() {
        closeSession();
        previewBuilder = null;
        previewIsVideoMode = false;
        closeCamera();
        closePictureImageReader();
        stopBackgroundThread();
    }

    private void closeCamera() {
        if (camera != null) {
            camera.close();
            camera = null;
        }
    }

    private void closeSession() {
        if (captureSession != null) {
            captureSession.close();
            captureSession = null;
            //pending_request_when_ready = null;
        }
    }

    private void closePictureImageReader() {
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
        if (imageReaderRaw != null) {
            imageReaderRaw.close();
            imageReaderRaw = null;
            onRawImageAvailableListener = null;
        }
    }

    private List<String> convertFocusModesToValues(int[] supported_focus_modes_arr, float minimum_focus_distance) {
        if (supported_focus_modes_arr.length == 0)
            return null;
        List<Integer> supported_focus_modes = new ArrayList<>();
        for (Integer supported_focus_mode : supported_focus_modes_arr)
            supported_focus_modes.add(supported_focus_mode);
        List<String> output_modes = new ArrayList<>();
        // also resort as well as converting
        if (supported_focus_modes.contains(CaptureRequest.CONTROL_AF_MODE_AUTO)) {
            output_modes.add("focus_mode_auto");
        }
        if (supported_focus_modes.contains(CaptureRequest.CONTROL_AF_MODE_MACRO)) {
            output_modes.add("focus_mode_macro");
        }
        if (supported_focus_modes.contains(CaptureRequest.CONTROL_AF_MODE_AUTO)) {
            output_modes.add("focus_mode_locked");
        }
        if (supported_focus_modes.contains(CaptureRequest.CONTROL_AF_MODE_OFF)) {
            output_modes.add("focus_mode_infinity");
            if (minimum_focus_distance > 0.0f) {
                output_modes.add("focus_mode_manual2");
            }
        }
        if (supported_focus_modes.contains(CaptureRequest.CONTROL_AF_MODE_EDOF)) {
            output_modes.add("focus_mode_edof");
        }
        if (supported_focus_modes.contains(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)) {
            output_modes.add("focus_mode_continuous_picture");
        }
        if (supported_focus_modes.contains(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)) {
            output_modes.add("focus_mode_continuous_video");
        }
        return output_modes;
    }

    public ICameraController.CameraFeatures getCameraFeatures() {
        camera_features = new ICameraController.CameraFeatures();

        Float max_zoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
        camera_features.is_zoom_supported = max_zoom != null && max_zoom > 0.0f;
        if (camera_features.is_zoom_supported) {
            // set 20 steps per 2x factor
            final int steps_per_2x_factor = 20;
            //final double scale_factor = Math.pow(2.0, 1.0/(double)steps_per_2x_factor);
            int n_steps = (int) ((steps_per_2x_factor * Math.log(max_zoom + 1.0e-11)) / Math.log(2.0));
            final double scale_factor = Math.pow(max_zoom, 1.0 / (double) n_steps);
            camera_features.zoom_ratios = new ArrayList<>();
            camera_features.zoom_ratios.add(100);
            double zoom = 1.0;
            for (int i = 0; i < n_steps - 1; i++) {
                zoom *= scale_factor;
                camera_features.zoom_ratios.add((int) (zoom * 100));
            }
            camera_features.zoom_ratios.add((int) (max_zoom * 100));
            camera_features.max_zoom = camera_features.zoom_ratios.size() - 1;
        }

        int[] face_modes = characteristics.get(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES);
        camera_features.supports_face_detection = false;
        supports_face_detect_mode_simple = false;
        supports_face_detect_mode_full = false;
        if (face_modes != null)
            for (int face_mode : face_modes) {
                // we currently only make use of the "SIMPLE" features, documented as:
                // "Return face rectangle and confidence values only."
                // note that devices that support STATISTICS_FACE_DETECT_MODE_FULL (e.g., Nexus 6) don't return
                // STATISTICS_FACE_DETECT_MODE_SIMPLE in the list, so we have check for either
                if (face_mode == CameraCharacteristics.STATISTICS_FACE_DETECT_MODE_SIMPLE) {
                    camera_features.supports_face_detection = true;
                    supports_face_detect_mode_simple = true;
                } else if (face_mode == CameraCharacteristics.STATISTICS_FACE_DETECT_MODE_FULL) {
                    camera_features.supports_face_detection = true;
                    supports_face_detect_mode_full = true;
                }
            }
        if (camera_features.supports_face_detection) {
            int face_count = characteristics.get(CameraCharacteristics.STATISTICS_INFO_MAX_FACE_COUNT);
            if (face_count <= 0) {
                camera_features.supports_face_detection = false;
                supports_face_detect_mode_simple = false;
                supports_face_detect_mode_full = false;
            }
        }
        if (camera_features.supports_face_detection) {
            // check we have scene mode CONTROL_SCENE_MODE_FACE_PRIORITY
            int[] values2 = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES);
            boolean has_face_priority = false;
            for (int value2 : values2) {
                if (value2 == CameraMetadata.CONTROL_SCENE_MODE_FACE_PRIORITY) {
                    has_face_priority = true;
                    break;
                }
            }
            if (!has_face_priority) {
                camera_features.supports_face_detection = false;
                supports_face_detect_mode_simple = false;
                supports_face_detect_mode_full = false;
            }
        }

        int[] capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
        //boolean capabilities_manual_sensor = false;
        boolean capabilities_manual_post_processing = false;
        boolean capabilities_raw = false;
        boolean capabilities_high_speed_video = false;
        for (int capability : capabilities) {
			/*if( capability == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR ) {
				// At least some Huawei devices (at least, the Huawei device model FIG-LX3, device code-name hi6250) don't
				// have REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR, but I had a user complain that HDR mode and manual ISO
				// had previously worked for them. Note that we still check below for SENSOR_INFO_SENSITIVITY_RANGE and
				// SENSOR_INFO_EXPOSURE_TIME_RANGE, so not checking REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR shouldn't
				// enable manual ISO/exposure on devices that don't support it.
				// Also may affect Samsung Galaxy A8(2018).
				// Instead we just block LEGACY devices (probably don't need to, again because we check
				// SENSOR_INFO_SENSITIVITY_RANGE and SENSOR_INFO_EXPOSURE_TIME_RANGE, but just in case).
				capabilities_manual_sensor = true;
			}
			else*/
            if (capability == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_POST_PROCESSING) {
                capabilities_manual_post_processing = true;
            } else if (capability == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) {
                capabilities_raw = true;
            }
			/*else if( capability == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE ) {
				// see note below
				camera_features.supports_burst = true;
			}*/
            else if (capability == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // we test for at least Android M just to be safe (this is needed for createConstrainedHighSpeedCaptureSession())
                capabilities_high_speed_video = true;
            }
        }
        // At least some Huawei devices (at least, the Huawei device model FIG-LX3, device code-name hi6250) don't have
        // REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE, but I had a user complain that NR mode at least had previously
        // (before 1.45) worked for them. It might be that this can still work, just not at 20fps.
        // So instead set to true for all LIMITED devices. Still keep block for LEGACY devices (which definitely shouldn't
        // support fast burst - and which Open Camera never allowed with Camera2 before 1.45).
        // Also may affect Samsung Galaxy A8(2018).
        camera_features.supports_burst = ICameraController.isHardwareLevelSupported(characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL), CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED);

        StreamConfigurationMap configs;
        try {
            configs = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        } catch (IllegalArgumentException | NullPointerException e) {
            // have had IllegalArgumentException crashes from Google Play - unclear what the cause is, but at least fail gracefully
            // similarly for NullPointerException - note, these aren't from characteristics being null, but from
            // com.android.internal.util.Preconditions.checkArrayElementsNotNull (Preconditions.java:395) - all are from
            // Nexus 7 (2013)s running Android 8.1, but again better to fail gracefully
            e.printStackTrace();
            return null;
        }

        android.util.Size[] camera_picture_sizes = configs.getOutputSizes(ImageFormat.JPEG);
        camera_features.picture_sizes = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.util.Size[] camera_picture_sizes_hires = configs.getHighResolutionOutputSizes(ImageFormat.JPEG);
            if (camera_picture_sizes_hires != null) {
                for (android.util.Size camera_size : camera_picture_sizes_hires) {
                    // Check not already listed? If it's listed in both, we'll add it later on when scanning camera_picture_sizes
                    // (and we don't want to set supports_burst to false for such a resolution).
                    boolean found = false;
                    for (android.util.Size sz : camera_picture_sizes) {
                        if (sz.equals(camera_size))
                            found = true;
                    }
                    if (!found) {
                        ICameraController.Size size = new ICameraController.Size(camera_size.getWidth(), camera_size.getHeight());
                        size.supports_burst = false;
                        camera_features.picture_sizes.add(size);
                    }
                }
            }
        }
        for (android.util.Size camera_size : camera_picture_sizes) {
            camera_features.picture_sizes.add(new ICameraController.Size(camera_size.getWidth(), camera_size.getHeight()));
        }
        // sizes are usually already sorted from high to low, but sort just in case
        // note some devices do have sizes in a not fully sorted order (e.g., Nokia 8)
        Collections.sort(camera_features.picture_sizes, new ICameraController.SizeSorter());
        // test high resolution modes not supporting burst:
        //camera_features.picture_sizes.get(0).supports_burst = false;

        raw_size = null;
        if (capabilities_raw) {
            android.util.Size[] raw_camera_picture_sizes = configs.getOutputSizes(ImageFormat.RAW_SENSOR);
            if (raw_camera_picture_sizes == null) {
                want_raw = false; // just in case it got set to true somehow
            } else {
                for (android.util.Size size : raw_camera_picture_sizes) {
                    if (raw_size == null || size.getWidth() * size.getHeight() > raw_size.getWidth() * raw_size.getHeight()) {
                        raw_size = size;
                    }
                }
                if (raw_size == null) {
                    want_raw = false; // just in case it got set to true somehow
                } else {
                    camera_features.supports_raw = true;
                }
            }
        } else {
            want_raw = false; // just in case it got set to true somehow
        }

        ae_fps_ranges = new ArrayList<>();
        for (Range<Integer> r : characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)) {
            ae_fps_ranges.add(new int[]{r.getLower(), r.getUpper()});
        }
        Collections.sort(ae_fps_ranges, new ICameraController.RangeSorter());

        android.util.Size[] camera_video_sizes = configs.getOutputSizes(MediaRecorder.class);
        camera_features.video_sizes = new ArrayList<>();
        int min_fps = 9999;
        for (int[] r : this.ae_fps_ranges) {
            min_fps = Math.min(min_fps, r[0]);
        }
        for (android.util.Size camera_size : camera_video_sizes) {
            long mfd = configs.getOutputMinFrameDuration(MediaRecorder.class, camera_size);
            int max_fps = (int) ((1.0 / mfd) * 1000000000L);
            ArrayList<int[]> fr = new ArrayList<>();
            fr.add(new int[]{min_fps, max_fps});
            ICameraController.Size normal_video_size = new ICameraController.Size(camera_size.getWidth(), camera_size.getHeight(), fr, false);
            camera_features.video_sizes.add(normal_video_size);
        }
        Collections.sort(camera_features.video_sizes, new ICameraController.SizeSorter());

        if (capabilities_high_speed_video) {
            hs_fps_ranges = new ArrayList<>();
            camera_features.video_sizes_high_speed = new ArrayList<>();

            for (Range<Integer> r : configs.getHighSpeedVideoFpsRanges()) {
                hs_fps_ranges.add(new int[]{r.getLower(), r.getUpper()});
            }
            Collections.sort(hs_fps_ranges, new ICameraController.RangeSorter());


            android.util.Size[] camera_video_sizes_high_speed = configs.getHighSpeedVideoSizes();
            for (android.util.Size camera_size : camera_video_sizes_high_speed) {
                ArrayList<int[]> fr = new ArrayList<>();
                for (Range<Integer> r : configs.getHighSpeedVideoFpsRangesFor(camera_size)) {
                    fr.add(new int[]{r.getLower(), r.getUpper()});
                }
                ICameraController.Size hs_video_size = new ICameraController.Size(camera_size.getWidth(), camera_size.getHeight(), fr, true);
                camera_features.video_sizes_high_speed.add(hs_video_size);
            }
            Collections.sort(camera_features.video_sizes_high_speed, new ICameraController.SizeSorter());
        }

        android.util.Size[] camera_preview_sizes = configs.getOutputSizes(SurfaceTexture.class);
        camera_features.preview_sizes = new ArrayList<>();
        Point display_size = new Point();
        Activity activity = (Activity) context;
        {
            Display display = activity.getWindowManager().getDefaultDisplay();
            display.getRealSize(display_size);
            // getRealSize() is adjusted based on the current rotation, so should already be landscape format, but it
            // would be good to not assume Open Camera runs in landscape mode (if we ever ran in portrait mode,
            // we'd still want display_size.x > display_size.y as preview resolutions also have width > height)
            if (display_size.x < display_size.y) {
                display_size.set(display_size.y, display_size.x);
            }
        }
        for (android.util.Size camera_size : camera_preview_sizes) {
            if (camera_size.getWidth() > display_size.x || camera_size.getHeight() > display_size.y) {
                // Nexus 6 returns these, even though not supported?! (get green corruption lines if we allow these)
                // Google Camera filters anything larger than height 1080, with a todo saying to use device's measurements
                continue;
            }
            camera_features.preview_sizes.add(new ICameraController.Size(camera_size.getWidth(), camera_size.getHeight()));
        }

        if (characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)) {
            camera_features.supported_flash_values = new ArrayList<>();
            camera_features.supported_flash_values.add("flash_off");
            camera_features.supported_flash_values.add("flash_auto");
            camera_features.supported_flash_values.add("flash_on");
            camera_features.supported_flash_values.add("flash_torch");
            if (!use_fake_precapture) {
                camera_features.supported_flash_values.add("flash_red_eye");
            }
        } else if (isFrontFacing()) {
            camera_features.supported_flash_values = new ArrayList<>();
            camera_features.supported_flash_values.add("flash_off");
            camera_features.supported_flash_values.add("flash_frontscreen_auto");
            camera_features.supported_flash_values.add("flash_frontscreen_on");
            camera_features.supported_flash_values.add("flash_frontscreen_torch");
        }

        Float minimum_focus_distance = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE); // may be null on some devices
        if (minimum_focus_distance != null) {
            camera_features.minimum_focus_distance = minimum_focus_distance;
        } else {
            camera_features.minimum_focus_distance = 0.0f;
        }

        int[] supported_focus_modes = characteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES); // Android format
        camera_features.supported_focus_values = convertFocusModesToValues(supported_focus_modes, camera_features.minimum_focus_distance); // convert to our format (also resorts)
        if (camera_features.supported_focus_values != null && camera_features.supported_focus_values.contains("focus_mode_manual2")) {
            camera_features.supports_focus_bracketing = true;
        }
        camera_features.max_num_focus_areas = characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF);

        camera_features.is_exposure_lock_supported = true;

        camera_features.is_white_balance_lock_supported = true;

        camera_features.is_video_stabilization_supported = false;
        int[] supported_video_stabilization_modes = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES);
        if (supported_video_stabilization_modes != null) {
            for (int supported_video_stabilization_mode : supported_video_stabilization_modes) {
                if (supported_video_stabilization_mode == CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_ON) {
                    camera_features.is_video_stabilization_supported = true;
                }
            }
        }

        camera_features.is_photo_video_recording_supported = ICameraController.isHardwareLevelSupported(characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL), CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED);
        supports_photo_video_recording = camera_features.is_photo_video_recording_supported;

        int[] white_balance_modes = characteristics.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES);
        if (white_balance_modes != null) {
            for (int value : white_balance_modes) {
                if (value == CameraMetadata.CONTROL_AWB_MODE_OFF && capabilities_manual_post_processing && allowManualWB()) {
                    camera_features.supports_white_balance_temperature = true;
                    camera_features.min_temperature = min_white_balance_temperature_c;
                    camera_features.max_temperature = max_white_balance_temperature_c;
                }
            }
        }

        // see note above
        //if( capabilities_manual_sensor )
        if (ICameraController.isHardwareLevelSupported(characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL), CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)) {
            Range<Integer> iso_range = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE); // may be null on some devices
            if (iso_range != null) {
                camera_features.supports_iso_range = true;
                camera_features.min_iso = iso_range.getLower();
                camera_features.max_iso = iso_range.getUpper();
                // we only expose exposure_time if iso_range is supported
                Range<Long> exposure_time_range = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE); // may be null on some devices
                if (exposure_time_range != null) {
                    camera_features.supports_exposure_time = true;
                    camera_features.supports_expo_bracketing = true;
                    camera_features.max_expo_bracketing_n_images = max_expo_bracketing_n_images;
                    camera_features.min_exposure_time = exposure_time_range.getLower();
                    camera_features.max_exposure_time = exposure_time_range.getUpper();
                }
            }
        }

        Range<Integer> exposure_range = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
        camera_features.min_exposure = exposure_range.getLower();
        camera_features.max_exposure = exposure_range.getUpper();
        camera_features.exposure_step = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP).floatValue();

        camera_features.can_disable_shutter_sound = true;

        if (capabilities_manual_post_processing) {
            Integer tonemap_max_curve_points = characteristics.get(CameraCharacteristics.TONEMAP_MAX_CURVE_POINTS);
            if (tonemap_max_curve_points != null) {
                camera_features.tonemap_max_curve_points = tonemap_max_curve_points;
                camera_features.supports_tonemap_curve = tonemap_max_curve_points >= tonemap_max_curve_points_c;
            }
        }

        {
            // Calculate view angles
            // Note this is an approximation (see http://stackoverflow.com/questions/39965408/what-is-the-android-camera2-api-equivalent-of-camera-parameters-gethorizontalvie ).
            // Potentially we could do better, taking into account the aspect ratio of the current resolution.
            // Note that we'd want to distinguish between the field of view of the preview versus the photo (or view) (for example,
            // DrawPreview would want the preview's field of view).
            // Also if we wanted to do this, we'd need to make sure that this was done after the caller had set the desired preview
            // and photo/video resolutions.
            SizeF physical_size = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
            float[] focal_lengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
            camera_features.view_angle_x = (float) Math.toDegrees(2.0 * Math.atan2(physical_size.getWidth(), (2.0 * focal_lengths[0])));
            camera_features.view_angle_y = (float) Math.toDegrees(2.0 * Math.atan2(physical_size.getHeight(), (2.0 * focal_lengths[0])));
        }

        return camera_features;
    }

    public boolean shouldCoverPreview() {
        return !has_received_frame;
    }

    private String convertSceneMode(int value2) {
        String value;
        switch (value2) {
            case CameraMetadata.CONTROL_SCENE_MODE_ACTION:
                value = "action";
                break;
            case CameraMetadata.CONTROL_SCENE_MODE_BARCODE:
                value = "barcode";
                break;
            case CameraMetadata.CONTROL_SCENE_MODE_BEACH:
                value = "beach";
                break;
            case CameraMetadata.CONTROL_SCENE_MODE_CANDLELIGHT:
                value = "candlelight";
                break;
            case CameraMetadata.CONTROL_SCENE_MODE_DISABLED:
                value = "disabled";
                break;
            case CameraMetadata.CONTROL_SCENE_MODE_FIREWORKS:
                value = "fireworks";
                break;
            case CameraMetadata.CONTROL_SCENE_MODE_LANDSCAPE:
                value = "landscape";
                break;
            case CameraMetadata.CONTROL_SCENE_MODE_NIGHT:
                value = "night";
                break;
            case CameraMetadata.CONTROL_SCENE_MODE_NIGHT_PORTRAIT:
                value = "night-portrait";
                break;
            case CameraMetadata.CONTROL_SCENE_MODE_PARTY:
                value = "party";
                break;
            case CameraMetadata.CONTROL_SCENE_MODE_PORTRAIT:
                value = "portrait";
                break;
            case CameraMetadata.CONTROL_SCENE_MODE_SNOW:
                value = "snow";
                break;
            case CameraMetadata.CONTROL_SCENE_MODE_SPORTS:
                value = "sports";
                break;
            case CameraMetadata.CONTROL_SCENE_MODE_STEADYPHOTO:
                value = "steadyphoto";
                break;
            case CameraMetadata.CONTROL_SCENE_MODE_SUNSET:
                value = "sunset";
                break;
            case CameraMetadata.CONTROL_SCENE_MODE_THEATRE:
                value = "theatre";
                break;
            default:
                value = null;
                break;
        }
        return value;
    }

    public ICameraController.SupportedValues setSceneMode(String value) {
        // we convert to/from strings to be compatible with original Android Camera API
        int[] values2 = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES);
        boolean has_disabled = false;
        List<String> values = new ArrayList<>();
        if (values2 != null) {
            // CONTROL_AVAILABLE_SCENE_MODES is supposed to always be available, but have had some (rare) crashes from Google Play due to being null
            for (int value2 : values2) {
                if (value2 == CameraMetadata.CONTROL_SCENE_MODE_DISABLED)
                    has_disabled = true;
                String this_value = convertSceneMode(value2);
                if (this_value != null) {
                    values.add(this_value);
                }
            }
        }
        if (!has_disabled) {
            values.add(0, "disabled");
        }
        ICameraController.SupportedValues supported_values = ICameraController.checkModeIsSupported(values, value, "disabled");
        if (supported_values != null) {
            int selected_value2 = CameraMetadata.CONTROL_SCENE_MODE_DISABLED;
            switch (supported_values.selected_value) {
                case "action":
                    selected_value2 = CameraMetadata.CONTROL_SCENE_MODE_ACTION;
                    break;
                case "barcode":
                    selected_value2 = CameraMetadata.CONTROL_SCENE_MODE_BARCODE;
                    break;
                case "beach":
                    selected_value2 = CameraMetadata.CONTROL_SCENE_MODE_BEACH;
                    break;
                case "candlelight":
                    selected_value2 = CameraMetadata.CONTROL_SCENE_MODE_CANDLELIGHT;
                    break;
                case "disabled":
                    selected_value2 = CameraMetadata.CONTROL_SCENE_MODE_DISABLED;
                    break;
                case "fireworks":
                    selected_value2 = CameraMetadata.CONTROL_SCENE_MODE_FIREWORKS;
                    break;
                // "hdr" no longer available in Camera2
                case "landscape":
                    selected_value2 = CameraMetadata.CONTROL_SCENE_MODE_LANDSCAPE;
                    break;
                case "night":
                    selected_value2 = CameraMetadata.CONTROL_SCENE_MODE_NIGHT;
                    break;
                case "night-portrait":
                    selected_value2 = CameraMetadata.CONTROL_SCENE_MODE_NIGHT_PORTRAIT;
                    break;
                case "party":
                    selected_value2 = CameraMetadata.CONTROL_SCENE_MODE_PARTY;
                    break;
                case "portrait":
                    selected_value2 = CameraMetadata.CONTROL_SCENE_MODE_PORTRAIT;
                    break;
                case "snow":
                    selected_value2 = CameraMetadata.CONTROL_SCENE_MODE_SNOW;
                    break;
                case "sports":
                    selected_value2 = CameraMetadata.CONTROL_SCENE_MODE_SPORTS;
                    break;
                case "steadyphoto":
                    selected_value2 = CameraMetadata.CONTROL_SCENE_MODE_STEADYPHOTO;
                    break;
                case "sunset":
                    selected_value2 = CameraMetadata.CONTROL_SCENE_MODE_SUNSET;
                    break;
                case "theatre":
                    selected_value2 = CameraMetadata.CONTROL_SCENE_MODE_THEATRE;
                    break;
                default:
                    break;
            }

            camera_settings.scene_mode = selected_value2;
            if (camera_settings.setSceneMode(previewBuilder)) {
                try {
                    setRepeatingRequest();
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return supported_values;
    }

    public String getSceneMode() {
        if (previewBuilder.get(CaptureRequest.CONTROL_SCENE_MODE) == null)
            return null;
        int value2 = previewBuilder.get(CaptureRequest.CONTROL_SCENE_MODE);
        return convertSceneMode(value2);
    }

    public boolean sceneModeAffectsFunctionality() {
        // Camera2 API doesn't seem to have any warnings that changing scene mode can affect available functionality
        return false;
    }

    private String convertColorEffect(int value2) {
        String value;
        switch (value2) {
            case CameraMetadata.CONTROL_EFFECT_MODE_AQUA:
                value = "aqua";
                break;
            case CameraMetadata.CONTROL_EFFECT_MODE_BLACKBOARD:
                value = "blackboard";
                break;
            case CameraMetadata.CONTROL_EFFECT_MODE_MONO:
                value = "mono";
                break;
            case CameraMetadata.CONTROL_EFFECT_MODE_NEGATIVE:
                value = "negative";
                break;
            case CameraMetadata.CONTROL_EFFECT_MODE_OFF:
                value = "default";
                break;
            case CameraMetadata.CONTROL_EFFECT_MODE_POSTERIZE:
                value = "posterize";
                break;
            case CameraMetadata.CONTROL_EFFECT_MODE_SEPIA:
                value = "sepia";
                break;
            case CameraMetadata.CONTROL_EFFECT_MODE_SOLARIZE:
                value = "solarize";
                break;
            case CameraMetadata.CONTROL_EFFECT_MODE_WHITEBOARD:
                value = "whiteboard";
                break;
            default:
                value = null;
                break;
        }
        return value;
    }

    public ICameraController.SupportedValues setColorEffect(String value) {
        // we convert to/from strings to be compatible with original Android Camera API
        int[] values2 = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS);
        if (values2 == null) {
            return null;
        }
        List<String> values = new ArrayList<>();
        for (int value2 : values2) {
            String this_value = convertColorEffect(value2);
            if (this_value != null) {
                values.add(this_value);
            }
        }
        ICameraController.SupportedValues supported_values = ICameraController.checkModeIsSupported(values, value, "off");
        if (supported_values != null) {
            int selected_value2 = CameraMetadata.CONTROL_EFFECT_MODE_OFF;
            switch (supported_values.selected_value) {
                case "aqua":
                    selected_value2 = CameraMetadata.CONTROL_EFFECT_MODE_AQUA;
                    break;
                case "blackboard":
                    selected_value2 = CameraMetadata.CONTROL_EFFECT_MODE_BLACKBOARD;
                    break;
                case "mono":
                    selected_value2 = CameraMetadata.CONTROL_EFFECT_MODE_MONO;
                    break;
                case "negative":
                    selected_value2 = CameraMetadata.CONTROL_EFFECT_MODE_NEGATIVE;
                    break;
                case "off":
                    selected_value2 = CameraMetadata.CONTROL_EFFECT_MODE_OFF;
                    break;
                case "posterize":
                    selected_value2 = CameraMetadata.CONTROL_EFFECT_MODE_POSTERIZE;
                    break;
                case "sepia":
                    selected_value2 = CameraMetadata.CONTROL_EFFECT_MODE_SEPIA;
                    break;
                case "solarize":
                    selected_value2 = CameraMetadata.CONTROL_EFFECT_MODE_SOLARIZE;
                    break;
                case "whiteboard":
                    selected_value2 = CameraMetadata.CONTROL_EFFECT_MODE_WHITEBOARD;
                    break;
                default:
                    break;
            }

            camera_settings.color_effect = selected_value2;
            if (camera_settings.setColorEffect(previewBuilder)) {
                try {
                    setRepeatingRequest();
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return supported_values;
    }

    public String getColorEffect() {
        if (previewBuilder.get(CaptureRequest.CONTROL_EFFECT_MODE) == null)
            return null;
        int value2 = previewBuilder.get(CaptureRequest.CONTROL_EFFECT_MODE);
        return convertColorEffect(value2);
    }

    private String convertWhiteBalance(int value2) {
        String value;
        switch (value2) {
            case CameraMetadata.CONTROL_AWB_MODE_AUTO:
                value = "default";
                break;
            case CameraMetadata.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT:
                value = "cloudy-daylight";
                break;
            case CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT:
                value = "daylight";
                break;
            case CameraMetadata.CONTROL_AWB_MODE_FLUORESCENT:
                value = "fluorescent";
                break;
            case CameraMetadata.CONTROL_AWB_MODE_INCANDESCENT:
                value = "incandescent";
                break;
            case CameraMetadata.CONTROL_AWB_MODE_SHADE:
                value = "shade";
                break;
            case CameraMetadata.CONTROL_AWB_MODE_TWILIGHT:
                value = "twilight";
                break;
            case CameraMetadata.CONTROL_AWB_MODE_WARM_FLUORESCENT:
                value = "warm-fluorescent";
                break;
            case CameraMetadata.CONTROL_AWB_MODE_OFF:
                value = "manual";
                break;
            default:
                value = null;
                break;
        }
        return value;
    }

    /**
     * Whether we should allow manual white balance, even if the device supports CONTROL_AWB_MODE_OFF.
     */
    private boolean allowManualWB() {
        boolean is_nexus6 = Build.MODEL.toLowerCase(Locale.US).contains("nexus 6");
        // manual white balance doesn't seem to work on Nexus 6!
        return !is_nexus6;
    }

    public ICameraController.SupportedValues setWhiteBalance(String value) {
        // we convert to/from strings to be compatible with original Android Camera API
        int[] values2 = characteristics.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES);
        if (values2 == null) {
            return null;
        }
        List<String> values = new ArrayList<>();
        for (int value2 : values2) {
            String this_value = convertWhiteBalance(value2);
            if (this_value != null) {
                if (value2 == CameraMetadata.CONTROL_AWB_MODE_OFF && !allowManualWB()) {
                    // filter
                } else {
                    values.add(this_value);
                }
            }
        }
        {
            // re-order so that auto is first, manual is second
            boolean has_auto = values.remove("default");
            boolean has_manual = values.remove("manual");
            if (has_manual)
                values.add(0, "manual");
            if (has_auto)
                values.add(0, "default");
        }
        ICameraController.SupportedValues supported_values = ICameraController.checkModeIsSupported(values, value, "default");
        if (supported_values != null) {
            int selected_value2 = CameraMetadata.CONTROL_AWB_MODE_AUTO;
            switch (supported_values.selected_value) {
                case "default":
                    selected_value2 = CameraMetadata.CONTROL_AWB_MODE_AUTO;
                    break;
                case "cloudy-daylight":
                    selected_value2 = CameraMetadata.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT;
                    break;
                case "daylight":
                    selected_value2 = CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT;
                    break;
                case "fluorescent":
                    selected_value2 = CameraMetadata.CONTROL_AWB_MODE_FLUORESCENT;
                    break;
                case "incandescent":
                    selected_value2 = CameraMetadata.CONTROL_AWB_MODE_INCANDESCENT;
                    break;
                case "shade":
                    selected_value2 = CameraMetadata.CONTROL_AWB_MODE_SHADE;
                    break;
                case "twilight":
                    selected_value2 = CameraMetadata.CONTROL_AWB_MODE_TWILIGHT;
                    break;
                case "warm-fluorescent":
                    selected_value2 = CameraMetadata.CONTROL_AWB_MODE_WARM_FLUORESCENT;
                    break;
                case "manual":
                    selected_value2 = CameraMetadata.CONTROL_AWB_MODE_OFF;
                    break;
                default:
                    break;
            }

            camera_settings.white_balance = selected_value2;
            if (camera_settings.setWhiteBalance(previewBuilder)) {
                try {
                    setRepeatingRequest();
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return supported_values;
    }

    public String getWhiteBalance() {
        if (previewBuilder.get(CaptureRequest.CONTROL_AWB_MODE) == null)
            return null;
        int value2 = previewBuilder.get(CaptureRequest.CONTROL_AWB_MODE);
        return convertWhiteBalance(value2);
    }

    // Returns whether white balance temperature was modified
    public boolean setWhiteBalanceTemperature(int temperature) {
        if (camera_settings.white_balance == temperature) {
            return false;
        }
        try {
            temperature = Math.max(temperature, min_white_balance_temperature_c);
            temperature = Math.min(temperature, max_white_balance_temperature_c);
            camera_settings.white_balance_temperature = temperature;
            if (camera_settings.setWhiteBalance(previewBuilder)) {
                setRepeatingRequest();
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return true;
    }

    public int getWhiteBalanceTemperature() {
        return camera_settings.white_balance_temperature;
    }

    private String convertAntiBanding(int value2) {
        String value;
        switch (value2) {
            case CameraMetadata.CONTROL_AE_ANTIBANDING_MODE_AUTO:
                value = "default";
                break;
            case CameraMetadata.CONTROL_AE_ANTIBANDING_MODE_50HZ:
                value = "50hz";
                break;
            case CameraMetadata.CONTROL_AE_ANTIBANDING_MODE_60HZ:
                value = "60hz";
                break;
            case CameraMetadata.CONTROL_AE_ANTIBANDING_MODE_OFF:
                value = "off";
                break;
            default:
                value = null;
                break;
        }
        return value;
    }

    public ICameraController.SupportedValues setAntiBanding(String value) {
        // we convert to/from strings to be compatible with original Android Camera API
        int[] values2 = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_ANTIBANDING_MODES);
        if (values2 == null) {
            return null;
        }
        List<String> values = new ArrayList<>();
        for (int value2 : values2) {
            String this_value = convertAntiBanding(value2);
            if (this_value != null) {
                values.add(this_value);
            }
        }
        ICameraController.SupportedValues supported_values = ICameraController.checkModeIsSupported(values, value, "default");
        if (supported_values != null) {
            // for antibanding, if the requested value isn't available, we don't modify it at all
            // (so we stick with the device's default setting)
            if (supported_values.selected_value.equals(value)) {
                int selected_value2 = CameraMetadata.CONTROL_AE_ANTIBANDING_MODE_AUTO;
                switch (supported_values.selected_value) {
                    case "default":
                        selected_value2 = CameraMetadata.CONTROL_AE_ANTIBANDING_MODE_AUTO;
                        break;
                    case "50hz":
                        selected_value2 = CameraMetadata.CONTROL_AE_ANTIBANDING_MODE_50HZ;
                        break;
                    case "60hz":
                        selected_value2 = CameraMetadata.CONTROL_AE_ANTIBANDING_MODE_60HZ;
                        break;
                    case "off":
                        selected_value2 = CameraMetadata.CONTROL_AE_ANTIBANDING_MODE_OFF;
                        break;
                    default:
                        break;
                }

                camera_settings.has_antibanding = true;
                camera_settings.antibanding = selected_value2;
                if (camera_settings.setAntiBanding(previewBuilder)) {
                    try {
                        setRepeatingRequest();
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return supported_values;
    }

    public String getAntiBanding() {
        if (previewBuilder.get(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE) == null)
            return null;
        int value2 = previewBuilder.get(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE);
        return convertAntiBanding(value2);
    }

    private String convertEdgeMode(int value2) {
        String value;
        switch (value2) {
            case CameraMetadata.EDGE_MODE_FAST:
                value = "fast";
                break;
            case CameraMetadata.EDGE_MODE_HIGH_QUALITY:
                value = "high_quality";
                break;
            case CameraMetadata.EDGE_MODE_OFF:
                value = "off";
                break;
            case CameraMetadata.EDGE_MODE_ZERO_SHUTTER_LAG:
                // we don't make use of zero shutter lag
                value = null;
                break;
            default:
                value = null;
                break;
        }
        return value;
    }

    public ICameraController.SupportedValues setEdgeMode(String value) {
        int[] values2 = characteristics.get(CameraCharacteristics.EDGE_AVAILABLE_EDGE_MODES);
        if (values2 == null) {
            return null;
        }
        List<String> values = new ArrayList<>();
        values.add("default");
        for (int value2 : values2) {
            String this_value = convertEdgeMode(value2);
            if (this_value != null) {
                values.add(this_value);
            }
        }
        ICameraController.SupportedValues supported_values = ICameraController.checkModeIsSupported(values, value, "default");
        if (supported_values != null) {
            // for edge mode, if the requested value isn't available, we don't modify it at all
            if (supported_values.selected_value.equals(value)) {
                boolean has_edge_mode = false;
                int selected_value2 = CameraMetadata.EDGE_MODE_FAST;
                // if EDGE_MODE_DEFAULT, this means to stick with the device default
                if (!value.equals("default")) {
                    switch (supported_values.selected_value) {
                        case "fast":
                            has_edge_mode = true;
                            selected_value2 = CameraMetadata.EDGE_MODE_FAST;
                            break;
                        case "high_quality":
                            has_edge_mode = true;
                            selected_value2 = CameraMetadata.EDGE_MODE_HIGH_QUALITY;
                            break;
                        case "off":
                            has_edge_mode = true;
                            selected_value2 = CameraMetadata.EDGE_MODE_OFF;
                            break;
                        default:
                            break;
                    }
                }

                if (camera_settings.has_edge_mode != has_edge_mode || camera_settings.edge_mode != selected_value2) {
                    camera_settings.has_edge_mode = has_edge_mode;
                    camera_settings.edge_mode = selected_value2;
                    if (camera_settings.setEdgeMode(previewBuilder)) {
                        try {
                            setRepeatingRequest();
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        return supported_values;
    }

    public String getEdgeMode() {
        if (previewBuilder.get(CaptureRequest.EDGE_MODE) == null)
            return null;
        int value2 = previewBuilder.get(CaptureRequest.EDGE_MODE);
        return convertEdgeMode(value2);
    }

    private String convertNoiseReductionMode(int value2) {
        String value;
        switch (value2) {
            case CameraMetadata.NOISE_REDUCTION_MODE_FAST:
                value = "fast";
                break;
            case CameraMetadata.NOISE_REDUCTION_MODE_HIGH_QUALITY:
                value = "high_quality";
                break;
            case CameraMetadata.NOISE_REDUCTION_MODE_MINIMAL:
                value = "minimal";
                break;
            case CameraMetadata.NOISE_REDUCTION_MODE_OFF:
                value = "off";
                break;
            case CameraMetadata.NOISE_REDUCTION_MODE_ZERO_SHUTTER_LAG:
                // we don't make use of zero shutter lag
                value = null;
                break;
            default:
                value = null;
                break;
        }
        return value;
    }

    public ICameraController.SupportedValues setNoiseReductionMode(String value) {
        int[] values2 = characteristics.get(CameraCharacteristics.NOISE_REDUCTION_AVAILABLE_NOISE_REDUCTION_MODES);
        if (values2 == null) {
            return null;
        }
        List<String> values = new ArrayList<>();
        values.add("default");
        for (int value2 : values2) {
            String this_value = convertNoiseReductionMode(value2);
            if (this_value != null) {
                values.add(this_value);
            }
        }
        ICameraController.SupportedValues supported_values = ICameraController.checkModeIsSupported(values, value, "default");
        if (supported_values != null) {
            // for noise reduction, if the requested value isn't available, we don't modify it at all
            if (supported_values.selected_value.equals(value)) {
                boolean has_noise_reduction_mode = false;
                int selected_value2 = CameraMetadata.NOISE_REDUCTION_MODE_FAST;
                // if NOISE_REDUCTION_MODE_DEFAULT, this means to stick with the device default
                if (!value.equals("default")) {
                    switch (supported_values.selected_value) {
                        case "fast":
                            has_noise_reduction_mode = true;
                            selected_value2 = CameraMetadata.NOISE_REDUCTION_MODE_FAST;
                            break;
                        case "high_quality":
                            has_noise_reduction_mode = true;
                            selected_value2 = CameraMetadata.NOISE_REDUCTION_MODE_HIGH_QUALITY;
                            break;
                        case "minimal":
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                has_noise_reduction_mode = true;
                                selected_value2 = CameraMetadata.NOISE_REDUCTION_MODE_MINIMAL;
                            } else {
                                // shouldn't ever be here, as NOISE_REDUCTION_MODE_MINIMAL shouldn't be a supported value!
                                // treat as fast instead
                                Log.e(TAG, "noise reduction minimal, but pre-Android M!");
                                has_noise_reduction_mode = true;
                                selected_value2 = CameraMetadata.NOISE_REDUCTION_MODE_FAST;
                            }
                            break;
                        case "off":
                            has_noise_reduction_mode = true;
                            selected_value2 = CameraMetadata.NOISE_REDUCTION_MODE_OFF;
                            break;
                        default:
                            break;
                    }
                }

                if (camera_settings.has_noise_reduction_mode != has_noise_reduction_mode || camera_settings.noise_reduction_mode != selected_value2) {
                    camera_settings.has_noise_reduction_mode = has_noise_reduction_mode;
                    camera_settings.noise_reduction_mode = selected_value2;
                    if (camera_settings.setNoiseReductionMode(previewBuilder)) {
                        try {
                            setRepeatingRequest();
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        return supported_values;
    }

    public String getNoiseReductionMode() {
        if (previewBuilder.get(CaptureRequest.NOISE_REDUCTION_MODE) == null)
            return null;
        int value2 = previewBuilder.get(CaptureRequest.NOISE_REDUCTION_MODE);
        return convertNoiseReductionMode(value2);
    }

    public ICameraController.SupportedValues setISO(String value) {
        // not supported for CameraController2 - but Camera2 devices that don't support manual ISO can call this,
        // so assume this is for auto ISO
        this.setManualISO(false, 0);
        return null;
    }

    public String getISOKey() {
        return "";
    }

    public void setManualISO(boolean manual_iso, int iso) {
        try {
            if (manual_iso) {
                Range<Integer> iso_range = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE); // may be null on some devices
                if (iso_range == null) {
                    return;
                }

                camera_settings.isManualExposure = true;
                iso = Math.max(iso, iso_range.getLower());
                iso = Math.min(iso, iso_range.getUpper());
                camera_settings.iso = iso;
            } else {
                camera_settings.isManualExposure = false;
                camera_settings.iso = 0;
            }

            if (camera_settings.setAEMode(previewBuilder, false)) {
                setRepeatingRequest();
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public boolean isManualISO() {
        return camera_settings.isManualExposure;
    }

    // Returns whether ISO was modified
    // N.B., use setManualISO() to switch between auto and manual mode
    public boolean setISO(int iso) {
        if (camera_settings.iso == iso) {
            return false;
        }
        try {
            camera_settings.iso = iso;
            if (camera_settings.setAEMode(previewBuilder, false)) {
                setRepeatingRequest();
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return true;
    }

    public int getISO() {
        return camera_settings.iso;
    }

    public long getExposureTime() {
        return camera_settings.exposure_time;
    }

    // Returns whether exposure time was modified
    // N.B., use setISO(String) to switch between auto and manual mode
    public boolean setExposureTime(long exposure_time) {
        if (camera_settings.exposure_time == exposure_time) {
            return false;
        }
        try {
            camera_settings.exposure_time = exposure_time;
            if (camera_settings.setAEMode(previewBuilder, false)) {
                setRepeatingRequest();
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return true;
    }

    public Size getPictureSize() {
        return new Size(picture_width, picture_height);
    }

    public void setPictureSize(int width, int height) {
        if (camera == null) {
            return;
        }
        if (captureSession != null) {
            // can only call this when captureSession not created - as the surface of the imageReader we create has to match the surface we pass to the captureSession
            throw new RuntimeException(); // throw as RuntimeException, as this is a programming error
        }
        this.picture_width = width;
        this.picture_height = height;
    }

    public void setRaw(boolean want_raw, int max_raw_images) {
        if (camera == null) {
            return;
        }
        if (this.want_raw == want_raw && this.max_raw_images == max_raw_images) {
            return;
        }
        if (want_raw && this.raw_size == null) {
            return;
        }
        if (captureSession != null) {
            // can only call this when captureSession not created - as it affects how we create the imageReader
            throw new RuntimeException(); // throw as RuntimeException, as this is a programming error
        }
        this.want_raw = want_raw;
        this.max_raw_images = max_raw_images;
    }

    public void setVideoHighSpeed(boolean want_video_high_speed) {
        if (camera == null) {
            return;
        }
        if (this.want_video_high_speed == want_video_high_speed) {
            return;
        }
        if (captureSession != null) {
            // can only call this when captureSession not created - as it affects how we create the session
            throw new RuntimeException(); // throw as RuntimeException, as this is a programming error
        }
        this.want_video_high_speed = want_video_high_speed;
        this.is_video_high_speed = false; // reset just to be safe
    }

    public int getBurstType() {
        return burst_type;
    }

    public void setBurstType(int burst_type) {
        if (camera == null) {
            return;
        }
        if (this.burst_type == burst_type) {
            return;
        }
		/*if( captureSession != null ) {
			// can only call this when captureSession not created - as it affects how we create the imageReader
			if( MyDebug.LOG )
				Log.e(TAG, "can't set burst type when captureSession running!");
			throw new RuntimeException(); // throw as RuntimeException, as this is a programming error
		}*/
        this.burst_type = burst_type;
        updateUseFakePrecaptureMode(camera_settings.flash_value);
        camera_settings.setAEMode(previewBuilder, false); // may need to set the ae mode, as flash is disabled for burst modes
    }

    public void setExpoBracketingNImages(int n_images) {
        if (n_images <= 1 || (n_images % 2) == 0) {
            throw new RuntimeException(); // throw as RuntimeException, as this is a programming error
        }
        if (n_images > max_expo_bracketing_n_images) {
            n_images = max_expo_bracketing_n_images;
        }
        this.expo_bracketing_n_images = n_images;
    }

    public void setExpoBracketingStops(double stops) {
        if (stops <= 0.0) {
            throw new RuntimeException(); // throw as RuntimeException, as this is a programming error
        }
        this.expo_bracketing_stops = stops;
    }

    public void setUseExpoFastBurst(boolean use_expo_fast_burst) {
        this.use_expo_fast_burst = use_expo_fast_burst;
    }

    public boolean isBurstOrExpo() {
        return this.burst_type != BURSTTYPE_NONE;
    }

    public boolean isCapturingBurst() {
        if (!isBurstOrExpo())
            return false;
        if (burst_type == BURSTTYPE_CONTINUOUS)
            return continuous_burst_in_progress || n_burst > 0;
        return getBurstTotal() > 1 && getNBurstTaken() < getBurstTotal();
    }

    public int getNBurstTaken() {
        return n_burst_taken;
    }

    public int getBurstTotal() {
        if (burst_type == BURSTTYPE_CONTINUOUS)
            return 0; // total burst size is unknown
        return n_burst_total;
    }

    public void setOptimiseAEForDRO(boolean optimise_ae_for_dro) {
        boolean is_oneplus = Build.MANUFACTURER.toLowerCase(Locale.US).contains("oneplus");
        if (is_oneplus) {
            // OnePlus 3T has preview corruption / camera freezing problems when using manual shutter speeds
            // So best not to modify auto-exposure for DRO
            this.optimise_ae_for_dro = false;
        } else {
            this.optimise_ae_for_dro = optimise_ae_for_dro;
        }
    }

    public void setBurstNImages(int burst_requested_n_images) {
        this.burst_requested_n_images = burst_requested_n_images;
    }

    public void setBurstForNoiseReduction(boolean burst_for_noise_reduction, boolean noise_reduction_low_light) {
        this.burst_for_noise_reduction = burst_for_noise_reduction;
        this.noise_reduction_low_light = noise_reduction_low_light;
    }

    public boolean isContinuousBurstInProgress() {
        return continuous_burst_in_progress;
    }

    public void stopContinuousBurst() {
        continuous_burst_in_progress = false;
    }

    public void stopFocusBracketingBurst() {
        if (burst_type == BURSTTYPE_FOCUS) {
            focus_bracketing_in_progress = false;
        } else {
            Log.e(TAG, "stopFocusBracketingBurst burst_type is: " + burst_type);
        }
    }

    public boolean getUseCamera2FakeFlash() {
        return this.use_fake_precapture;
    }

    public void setUseCamera2FakeFlash(boolean use_fake_precapture) {
        if (camera == null) {
            return;
        }
        if (this.use_fake_precapture == use_fake_precapture) {
            return;
        }
        this.use_fake_precapture = use_fake_precapture;
        this.use_fake_precapture_mode = use_fake_precapture;
        // no need to call updateUseFakePrecaptureMode(), as this method should only be called after first creating camera controller
    }

    private void createPictureImageReader() {
        if (captureSession != null) {
            // can only call this when captureSession not created - as the surface of the imageReader we create has to match the surface we pass to the captureSession
            throw new RuntimeException(); // throw as RuntimeException, as this is a programming error
        }
        closePictureImageReader();
        if (picture_width == 0 || picture_height == 0) {
            throw new RuntimeException(); // throw as RuntimeException, as this is a programming error
        }
        // maxImages only needs to be 2, as we always read the JPEG data and close the image straight away in the imageReader
        imageReader = ImageReader.newInstance(picture_width, picture_height, ImageFormat.JPEG, 2);
        //imageReader = ImageReader.newInstance(picture_width, picture_height, ImageFormat.YUV_420_888, 2);
        imageReader.setOnImageAvailableListener(new OnImageAvailableListener(), null);
        if (want_raw && raw_size != null && !previewIsVideoMode) {
            // unlike the JPEG imageReader, we can't read the data and close the image straight away, so we need to allow a larger
            // value for maxImages
            imageReaderRaw = ImageReader.newInstance(raw_size.getWidth(), raw_size.getHeight(), ImageFormat.RAW_SENSOR, max_raw_images);
            imageReaderRaw.setOnImageAvailableListener(onRawImageAvailableListener = new OnRawImageAvailableListener(), null);
        }
    }

    private void clearPending() {
        pending_burst_images.clear();
        if (onRawImageAvailableListener != null) {
            onRawImageAvailableListener.clear();
        }
        slow_burst_capture_requests = null;
        n_burst = 0;
        n_burst_taken = 0;
        n_burst_total = 0;
        burst_single_request = false;
        slow_burst_start_ms = 0;
    }

    private void checkImagesCompleted() {
        synchronized (image_reader_lock) {
            if (!done_all_captures) {
            } else if (picture_cb == null) {
                // just in case?
            } else if (!jpeg_todo && !raw_todo) {
                // need to set picture_cb to null before calling onCompleted, as that may reenter CameraController to take another photo (if in auto-repeat burst mode) - see testTakePhotoRepeat()
                ICameraController.PictureCallback cb = picture_cb;
                picture_cb = null;
                cb.onCompleted();
            }
        }
    }

    public Size getPreviewSize() {
        return new Size(preview_width, preview_height);
    }

    public void setPreviewSize(int width, int height) {
		/*if( texture != null ) {
			if( MyDebug.LOG )
				Log.d(TAG, "set size of preview texture");
			texture.setDefaultBufferSize(width, height);
		}*/
        preview_width = width;
        preview_height = height;
		/*if( previewImageReader != null ) {
			previewImageReader.close();
		}
		previewImageReader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 2);
		*/
    }

    public boolean getVideoStabilization() {
        return camera_settings.video_stabilization;
    }

    public void setVideoStabilization(boolean enabled) {
        camera_settings.video_stabilization = enabled;
        camera_settings.setVideoStabilization(previewBuilder);
        try {
            setRepeatingRequest();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void setLogProfile(boolean use_log_profile, float log_profile_strength) {
        if (camera_settings.use_log_profile == use_log_profile && camera_settings.log_profile_strength == log_profile_strength)
            return; // no change
        camera_settings.use_log_profile = use_log_profile;
        if (use_log_profile)
            camera_settings.log_profile_strength = log_profile_strength;
        else
            camera_settings.log_profile_strength = 0.0f;
        camera_settings.setLogProfile(previewBuilder);
        try {
            setRepeatingRequest();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public boolean isLogProfile() {
        return camera_settings.use_log_profile;
    }

    /**
     * For testing.
     */
    public CaptureRequest.Builder testGetPreviewBuilder() {
        return previewBuilder;
    }

    public TonemapCurve testGetTonemapCurve() {
        return previewBuilder.get(CaptureRequest.TONEMAP_CURVE);
    }

    public int getJpegQuality() {
        return this.camera_settings.jpeg_quality;
    }

    public void setJpegQuality(int quality) {
        if (quality < 0 || quality > 100) {
            throw new RuntimeException(); // throw as RuntimeException, as this is a programming error
        }
        this.camera_settings.jpeg_quality = (byte) quality;
    }

    public int getZoom() {
        return this.current_zoom_value;
    }

    public void setZoom(int value) {
        if (camera_features.zoom_ratios == null) {
            return;
        }
        if (value < 0 || value > camera_features.zoom_ratios.size()) {
            throw new RuntimeException(); // throw as RuntimeException, as this is a programming error
        }
        float zoom = camera_features.zoom_ratios.get(value) / 100.0f;
        Rect sensor_rect = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        int left = sensor_rect.width() / 2;
        int right = left;
        int top = sensor_rect.height() / 2;
        int bottom = top;
        int hwidth = (int) (sensor_rect.width() / (2.0 * zoom));
        int hheight = (int) (sensor_rect.height() / (2.0 * zoom));
        left -= hwidth;
        right += hwidth;
        top -= hheight;
        bottom += hheight;
        camera_settings.scalar_crop_region = new Rect(left, top, right, bottom);
        camera_settings.setCropRegion(previewBuilder);
        this.current_zoom_value = value;
        try {
            setRepeatingRequest();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public int getExposureCompensation() {
        if (previewBuilder.get(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION) == null)
            return 0;
        return previewBuilder.get(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION);
    }

    // Returns whether exposure was modified
    public boolean setExposureCompensation(int new_exposure) {
        camera_settings.has_ae_exposure_compensation = true;
        camera_settings.ae_exposure_compensation = new_exposure;
        if (camera_settings.setExposureCompensation(previewBuilder)) {
            try {
                setRepeatingRequest();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    public void setPreviewFpsRange(int min, int max) {
        camera_settings.ae_target_fps_range = new Range<>(min / 1000, max / 1000);
//		Frame duration is in nanoseconds.  Using min to be safe.
        camera_settings.sensor_frame_duration =
                (long) (1.0 / (min / 1000.0) * 1000000000L);

        try {
            if (camera_settings.setAEMode(previewBuilder, false)) {
                setRepeatingRequest();
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void clearPreviewFpsRange() {
        // needed e.g. on Nokia 8 when switching back from slow motion to regular speed, in order to reset to the regular
        // frame rate
        if (camera_settings.ae_target_fps_range != null || camera_settings.sensor_frame_duration != 0) {
            // set back to default
            camera_settings.ae_target_fps_range = null;
            camera_settings.sensor_frame_duration = 0;
            createPreviewRequest();
            // createPreviewRequest() needed so that the values in the previewBuilder reset to default values, for
            // CONTROL_AE_TARGET_FPS_RANGE and SENSOR_FRAME_DURATION

            try {
                if (camera_settings.setAEMode(previewBuilder, false)) {
                    setRepeatingRequest();
                }
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public List<int[]> getSupportedPreviewFpsRange() {
        List<int[]> l = new ArrayList<>();

        List<int[]> rr = want_video_high_speed ? hs_fps_ranges : ae_fps_ranges;
        for (int[] r : rr) {
            int[] ir = {r[0] * 1000, r[1] * 1000};
            l.add(ir);
        }

        return l;
    }

    private String convertFocusModeToValue(int focus_mode) {
        String focus_value = "";
        switch (focus_mode) {
            case CaptureRequest.CONTROL_AF_MODE_AUTO:
                focus_value = "focus_mode_auto";
                break;
            case CaptureRequest.CONTROL_AF_MODE_MACRO:
                focus_value = "focus_mode_macro";
                break;
            case CaptureRequest.CONTROL_AF_MODE_EDOF:
                focus_value = "focus_mode_edof";
                break;
            case CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE:
                focus_value = "focus_mode_continuous_picture";
                break;
            case CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO:
                focus_value = "focus_mode_continuous_video";
                break;
            case CaptureRequest.CONTROL_AF_MODE_OFF:
                focus_value = "focus_mode_manual2"; // n.b., could be infinity
                break;
        }
        return focus_value;
    }

    public String getFocusValue() {
        Integer focus_mode = previewBuilder.get(CaptureRequest.CONTROL_AF_MODE);
        if (focus_mode == null)
            focus_mode = CaptureRequest.CONTROL_AF_MODE_AUTO;
        return convertFocusModeToValue(focus_mode);
    }

    public void setFocusValue(String focus_value) {
        int focus_mode;
        switch (focus_value) {
            case "focus_mode_auto":
            case "focus_mode_locked":
                focus_mode = CaptureRequest.CONTROL_AF_MODE_AUTO;
                break;
            case "focus_mode_infinity":
                focus_mode = CaptureRequest.CONTROL_AF_MODE_OFF;
                camera_settings.focus_distance = 0.0f;
                break;
            case "focus_mode_manual2":
                focus_mode = CaptureRequest.CONTROL_AF_MODE_OFF;
                camera_settings.focus_distance = camera_settings.focus_distance_manual;
                break;
            case "focus_mode_macro":
                focus_mode = CaptureRequest.CONTROL_AF_MODE_MACRO;
                break;
            case "focus_mode_edof":
                focus_mode = CaptureRequest.CONTROL_AF_MODE_EDOF;
                break;
            case "focus_mode_continuous_picture":
                focus_mode = CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE;
                break;
            case "focus_mode_continuous_video":
                focus_mode = CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO;
                break;
            default:
                return;
        }
        camera_settings.has_af_mode = true;
        camera_settings.af_mode = focus_mode;
        camera_settings.setFocusMode(previewBuilder);
        camera_settings.setFocusDistance(previewBuilder); // also need to set distance, in case changed between infinity, manual or other modes
        try {
            setRepeatingRequest();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public float getFocusDistance() {
        return camera_settings.focus_distance;
    }

    public boolean setFocusDistance(float focus_distance) {
        if (camera_settings.focus_distance == focus_distance) {
            return false;
        }
        camera_settings.focus_distance = focus_distance;
        camera_settings.focus_distance_manual = focus_distance;
        camera_settings.setFocusDistance(previewBuilder);
        try {
            setRepeatingRequest();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return true;
    }

    public void setFocusBracketingNImages(int n_images) {
        this.focus_bracketing_n_images = n_images;
    }

    public void setFocusBracketingAddInfinity(boolean focus_bracketing_add_infinity) {
        this.focus_bracketing_add_infinity = focus_bracketing_add_infinity;
    }

    public float getFocusBracketingSourceDistance() {
        return this.focus_bracketing_source_distance;
    }

    public void setFocusBracketingSourceDistance(float focus_bracketing_source_distance) {
        this.focus_bracketing_source_distance = focus_bracketing_source_distance;
    }

    public float getFocusBracketingTargetDistance() {
        return this.focus_bracketing_target_distance;
    }

    public void setFocusBracketingTargetDistance(float focus_bracketing_target_distance) {
        this.focus_bracketing_target_distance = focus_bracketing_target_distance;
    }

    /**
     * Decides whether we should be using fake precapture mode.
     */
    private void updateUseFakePrecaptureMode(String flash_value) {
        boolean frontscreen_flash = flash_value.equals("flash_frontscreen_auto") || flash_value.equals("flash_frontscreen_on");
        if (frontscreen_flash) {
            use_fake_precapture_mode = true;
        } else if (burst_type != BURSTTYPE_NONE)
            use_fake_precapture_mode = true;
        else {
            use_fake_precapture_mode = use_fake_precapture;
        }
    }

    public String getFlashValue() {
        // returns "" if flash isn't supported
        if (!characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)) {
            return "";
        }
        return camera_settings.flash_value;
    }

    public void setFlashValue(String flash_value) {
        if (camera_settings.flash_value.equals(flash_value)) {
            return;
        }

        try {
            updateUseFakePrecaptureMode(flash_value);

            if (camera_settings.flash_value.equals("flash_torch") && !flash_value.equals("flash_off")) {
                // hack - if switching to something other than flash_off, we first need to turn torch off, otherwise torch remains on (at least on Nexus 6 and Nokia 8)
                camera_settings.flash_value = "flash_off";
                camera_settings.setAEMode(previewBuilder, false);
                CaptureRequest request = previewBuilder.build();

                // need to wait until torch actually turned off
                camera_settings.flash_value = flash_value;
                camera_settings.setAEMode(previewBuilder, false);
                push_repeating_request_when_torch_off = true;
                push_repeating_request_when_torch_off_id = request;

                setRepeatingRequest(request);
            } else {
                camera_settings.flash_value = flash_value;
                if (camera_settings.setAEMode(previewBuilder, false)) {
                    setRepeatingRequest();
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void setRecordingHint(boolean hint) {
        // not relevant for CameraController2
    }

    public boolean getAutoExposureLock() {
        if (previewBuilder.get(CaptureRequest.CONTROL_AE_LOCK) == null)
            return false;
        return previewBuilder.get(CaptureRequest.CONTROL_AE_LOCK);
    }

    public void setAutoExposureLock(boolean enabled) {
        camera_settings.ae_lock = enabled;
        camera_settings.setAutoExposureLock(previewBuilder);
        try {
            setRepeatingRequest();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public boolean getAutoWhiteBalanceLock() {
        if (previewBuilder.get(CaptureRequest.CONTROL_AWB_LOCK) == null)
            return false;
        return previewBuilder.get(CaptureRequest.CONTROL_AWB_LOCK);
    }

    public void setAutoWhiteBalanceLock(boolean enabled) {
        camera_settings.wb_lock = enabled;
        camera_settings.setAutoWhiteBalanceLock(previewBuilder);
        try {
            setRepeatingRequest();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void setRotation(int rotation) {
        this.camera_settings.rotation = rotation;
    }

    public void setLocationInfo(Location location) {
        this.camera_settings.location = location;
    }

    public void removeLocationInfo() {
        this.camera_settings.location = null;
    }

    public void enableShutterSound(boolean enabled) {
        this.sounds_enabled = enabled;
    }

    /**
     * Returns the viewable rect - this is crop region if available.
     * We need this as callers will pass in (or expect returned) CameraController.Area values that
     * are relative to the current view (i.e., taking zoom into account) (the old Camera API in
     * CameraController1 always works in terms of the current view, whilst Camera2 works in terms
     * of the full view always). Similarly for the rect field in CameraController.Face.
     */
    private Rect getViewableRect() {
        if (previewBuilder != null) {
            Rect crop_rect = previewBuilder.get(CaptureRequest.SCALER_CROP_REGION);
            if (crop_rect != null) {
                return crop_rect;
            }
        }
        Rect sensor_rect = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        sensor_rect.right -= sensor_rect.left;
        sensor_rect.left = 0;
        sensor_rect.bottom -= sensor_rect.top;
        sensor_rect.top = 0;
        return sensor_rect;
    }

    private Rect convertRectToCamera2(Rect crop_rect, Rect rect) {
        // CameraController.Area is always [-1000, -1000] to [1000, 1000] for the viewable region
        // but for CameraController2, we must convert to be relative to the crop region
        double left_f = (rect.left + 1000) / 2000.0;
        double top_f = (rect.top + 1000) / 2000.0;
        double right_f = (rect.right + 1000) / 2000.0;
        double bottom_f = (rect.bottom + 1000) / 2000.0;
        int left = (int) (crop_rect.left + left_f * (crop_rect.width() - 1));
        int right = (int) (crop_rect.left + right_f * (crop_rect.width() - 1));
        int top = (int) (crop_rect.top + top_f * (crop_rect.height() - 1));
        int bottom = (int) (crop_rect.top + bottom_f * (crop_rect.height() - 1));
        left = Math.max(left, crop_rect.left);
        right = Math.max(right, crop_rect.left);
        top = Math.max(top, crop_rect.top);
        bottom = Math.max(bottom, crop_rect.top);
        left = Math.min(left, crop_rect.right);
        right = Math.min(right, crop_rect.right);
        top = Math.min(top, crop_rect.bottom);
        bottom = Math.min(bottom, crop_rect.bottom);

        return new Rect(left, top, right, bottom);
    }

    private MeteringRectangle convertAreaToMeteringRectangle(Rect sensor_rect, ICameraController.Area area) {
        Rect camera2_rect = convertRectToCamera2(sensor_rect, area.rect);
        return new MeteringRectangle(camera2_rect, area.weight);
    }

    private Rect convertRectFromCamera2(Rect crop_rect, Rect camera2_rect) {
        // inverse of convertRectToCamera2()
        double left_f = (camera2_rect.left - crop_rect.left) / (double) (crop_rect.width() - 1);
        double top_f = (camera2_rect.top - crop_rect.top) / (double) (crop_rect.height() - 1);
        double right_f = (camera2_rect.right - crop_rect.left) / (double) (crop_rect.width() - 1);
        double bottom_f = (camera2_rect.bottom - crop_rect.top) / (double) (crop_rect.height() - 1);
        int left = (int) (left_f * 2000) - 1000;
        int right = (int) (right_f * 2000) - 1000;
        int top = (int) (top_f * 2000) - 1000;
        int bottom = (int) (bottom_f * 2000) - 1000;

        left = Math.max(left, -1000);
        right = Math.max(right, -1000);
        top = Math.max(top, -1000);
        bottom = Math.max(bottom, -1000);
        left = Math.min(left, 1000);
        right = Math.min(right, 1000);
        top = Math.min(top, 1000);
        bottom = Math.min(bottom, 1000);

        return new Rect(left, top, right, bottom);
    }

    private ICameraController.Area convertMeteringRectangleToArea(Rect sensor_rect, MeteringRectangle metering_rectangle) {
        Rect area_rect = convertRectFromCamera2(sensor_rect, metering_rectangle.getRect());
        return new ICameraController.Area(area_rect, metering_rectangle.getMeteringWeight());
    }

    private ICameraController.Face convertFromCameraFace(Rect sensor_rect, android.hardware.camera2.params.Face camera2_face) {
        Rect area_rect = convertRectFromCamera2(sensor_rect, camera2_face.getBounds());
        return new ICameraController.Face(camera2_face.getScore(), area_rect);
    }

    public boolean setFocusAndMeteringArea(List<ICameraController.Area> areas) {
        Rect sensor_rect = getViewableRect();
        boolean has_focus = false;
        boolean has_metering = false;
        if (characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF) > 0) {
            has_focus = true;
            camera_settings.af_regions = new MeteringRectangle[areas.size()];
            int i = 0;
            for (ICameraController.Area area : areas) {
                camera_settings.af_regions[i++] = convertAreaToMeteringRectangle(sensor_rect, area);
            }
            camera_settings.setAFRegions(previewBuilder);
        } else
            camera_settings.af_regions = null;
        if (characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE) > 0) {
            has_metering = true;
            camera_settings.ae_regions = new MeteringRectangle[areas.size()];
            int i = 0;
            for (ICameraController.Area area : areas) {
                camera_settings.ae_regions[i++] = convertAreaToMeteringRectangle(sensor_rect, area);
            }
            camera_settings.setAERegions(previewBuilder);
        } else
            camera_settings.ae_regions = null;
        if (has_focus || has_metering) {
            try {
                setRepeatingRequest();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
        return has_focus;
    }

    public void clearFocusAndMetering() {
        Rect sensor_rect = getViewableRect();
        boolean has_focus = false;
        boolean has_metering = false;
        if (sensor_rect.width() <= 0 || sensor_rect.height() <= 0) {
            // had a crash on Google Play due to creating a MeteringRectangle with -ve width/height ?!
            camera_settings.af_regions = null;
            camera_settings.ae_regions = null;
        } else {
            if (characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF) > 0) {
                has_focus = true;
                camera_settings.af_regions = new MeteringRectangle[1];
                camera_settings.af_regions[0] = new MeteringRectangle(0, 0, sensor_rect.width() - 1, sensor_rect.height() - 1, 0);
                camera_settings.setAFRegions(previewBuilder);
            } else
                camera_settings.af_regions = null;
            if (characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE) > 0) {
                has_metering = true;
                camera_settings.ae_regions = new MeteringRectangle[1];
                camera_settings.ae_regions[0] = new MeteringRectangle(0, 0, sensor_rect.width() - 1, sensor_rect.height() - 1, 0);
                camera_settings.setAERegions(previewBuilder);
            } else
                camera_settings.ae_regions = null;
        }
        if (has_focus || has_metering) {
            try {
                setRepeatingRequest();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public List<ICameraController.Area> getFocusAreas() {
        if (characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF) == 0)
            return null;
        MeteringRectangle[] metering_rectangles = previewBuilder.get(CaptureRequest.CONTROL_AF_REGIONS);
        if (metering_rectangles == null)
            return null;
        Rect sensor_rect = getViewableRect();
        camera_settings.af_regions[0] = new MeteringRectangle(0, 0, sensor_rect.width() - 1, sensor_rect.height() - 1, 0);
        if (metering_rectangles.length == 1 && metering_rectangles[0].getRect().left == 0 && metering_rectangles[0].getRect().top == 0 && metering_rectangles[0].getRect().right == sensor_rect.width() - 1 && metering_rectangles[0].getRect().bottom == sensor_rect.height() - 1) {
            // for compatibility with CameraController1
            return null;
        }
        List<ICameraController.Area> areas = new ArrayList<>();
        for (MeteringRectangle metering_rectangle : metering_rectangles) {
            areas.add(convertMeteringRectangleToArea(sensor_rect, metering_rectangle));
        }
        return areas;
    }

    public List<ICameraController.Area> getMeteringAreas() {
        if (characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE) == 0)
            return null;
        MeteringRectangle[] metering_rectangles = previewBuilder.get(CaptureRequest.CONTROL_AE_REGIONS);
        if (metering_rectangles == null)
            return null;
        Rect sensor_rect = getViewableRect();
        if (metering_rectangles.length == 1 && metering_rectangles[0].getRect().left == 0 && metering_rectangles[0].getRect().top == 0 && metering_rectangles[0].getRect().right == sensor_rect.width() - 1 && metering_rectangles[0].getRect().bottom == sensor_rect.height() - 1) {
            // for compatibility with CameraController1
            return null;
        }
        List<ICameraController.Area> areas = new ArrayList<>();
        for (MeteringRectangle metering_rectangle : metering_rectangles) {
            areas.add(convertMeteringRectangleToArea(sensor_rect, metering_rectangle));
        }
        return areas;
    }

    public boolean supportsAutoFocus() {
        if (previewBuilder == null)
            return false;
        Integer focus_mode = previewBuilder.get(CaptureRequest.CONTROL_AF_MODE);
        if (focus_mode == null)
            return false;
        if (focus_mode == CaptureRequest.CONTROL_AF_MODE_AUTO || focus_mode == CaptureRequest.CONTROL_AF_MODE_MACRO)
            return true;
        return false;
    }

    public boolean focusIsContinuous() {
        if (previewBuilder == null)
            return false;
        Integer focus_mode = previewBuilder.get(CaptureRequest.CONTROL_AF_MODE);
        if (focus_mode == null)
            return false;
        if (focus_mode == CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE || focus_mode == CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
            return true;
        return false;
    }

    public boolean focusIsVideo() {
        if (previewBuilder == null)
            return false;
        Integer focus_mode = previewBuilder.get(CaptureRequest.CONTROL_AF_MODE);
        if (focus_mode == null)
            return false;
        if (focus_mode == CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO) {
            return true;
        }
        return false;
    }

    public void setPreviewDisplay(SurfaceHolder holder) {
        throw new RuntimeException(); // throw as RuntimeException, as this is a programming error
    }

    public void setPreviewTexture(SurfaceTexture texture) {
        if (this.texture != null) {
            throw new RuntimeException(); // throw as RuntimeException, as this is a programming error
        }
        this.texture = texture;
    }

    private void setRepeatingRequest() throws CameraAccessException {
        setRepeatingRequest(previewBuilder.build());
    }

    private void setRepeatingRequest(CaptureRequest request) throws CameraAccessException {
        if (camera == null || captureSession == null) {
            return;
        }
        try {
            if (is_video_high_speed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                CameraConstrainedHighSpeedCaptureSession captureSessionHighSpeed = (CameraConstrainedHighSpeedCaptureSession) captureSession;
                List<CaptureRequest> mPreviewBuilderBurst = captureSessionHighSpeed.createHighSpeedRequestList(request);
                captureSessionHighSpeed.setRepeatingBurst(mPreviewBuilderBurst, previewCaptureCallback, backgroundHandler);
            } else {
                captureSession.setRepeatingRequest(request, previewCaptureCallback, backgroundHandler);
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
            // got this as a Google Play exception (from onCaptureCompleted->processCompleted) - this means the capture session is already closed
        }
    }

    private void capture() throws CameraAccessException {
        capture(previewBuilder.build());
    }

    private void capture(CaptureRequest request) throws CameraAccessException {
        if (camera == null || captureSession == null) {
            return;
        }
        captureSession.capture(request, previewCaptureCallback, backgroundHandler);
    }

    private void createPreviewRequest() {
        if (camera == null) {
            return;
        }
        try {
            previewBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewIsVideoMode = false;
            previewBuilder.set(CaptureRequest.CONTROL_CAPTURE_INTENT, CaptureRequest.CONTROL_CAPTURE_INTENT_PREVIEW);
            camera_settings.setupBuilder(previewBuilder, false);
        } catch (CameraAccessException e) {
            //captureSession = null;
            e.printStackTrace();
        }
    }

    private Surface getPreviewSurface() {
        return surface_texture;
    }

    private void createCaptureSession(final MediaRecorder video_recorder, boolean want_photo_video_recording) {
        if (previewBuilder == null) {
            throw new RuntimeException(); // throw as RuntimeException, as this is a programming error
        }
        if (camera == null) {
            return;
        }

        closeSession();

        try {
            if (video_recorder != null) {
                if (supports_photo_video_recording && !want_video_high_speed && want_photo_video_recording) {
                    createPictureImageReader();
                } else {
                    closePictureImageReader();
                }
            } else {
                // in some cases need to recreate picture imageReader and the texture default buffer size (e.g., see test testTakePhotoPreviewPaused())
                createPictureImageReader();
            }
            if (texture != null) {
                // need to set the texture size
                if (preview_width == 0 || preview_height == 0) {
                    throw new RuntimeException(); // throw as RuntimeException, as this is a programming error
                }
                texture.setDefaultBufferSize(preview_width, preview_height);
                // also need to create a new surface for the texture, in case the size has changed - but make sure we remove the old one first!
                if (surface_texture != null) {
                    previewBuilder.removeTarget(surface_texture);
                }
                this.surface_texture = new Surface(texture);
            }
            if (video_recorder != null) {
            } else {
            }
			/*if( MyDebug.LOG )
			Log.d(TAG, "preview size: " + previewImageReader.getWidth() + " x " + previewImageReader.getHeight());*/

            if (video_recorder != null)
                video_recorder_surface = video_recorder.getSurface();
            else
                video_recorder_surface = null;

            class MyStateCallback extends CameraCaptureSession.StateCallback {
                private boolean callback_done; // must sychronize on this and notifyAll when setting to true

                public void onConfigured(@NonNull CameraCaptureSession session) {
                    if (camera == null) {
                        synchronized (create_capture_session_lock) {
                            callback_done = true;
                            create_capture_session_lock.notifyAll();
                        }
                        return;
                    }
                    captureSession = session;
                    Surface surface = getPreviewSurface();
                    previewBuilder.addTarget(surface);
                    if (video_recorder != null)
                        previewBuilder.addTarget(video_recorder_surface);
                    try {
                        setRepeatingRequest();
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                        // we indicate that we failed to start the preview by setting captureSession back to null
                        // this will cause a CameraControllerException to be thrown below
                        captureSession = null;
                    }
                    synchronized (create_capture_session_lock) {
                        callback_done = true;
                        create_capture_session_lock.notifyAll();
                    }
                }


                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    synchronized (create_capture_session_lock) {
                        callback_done = true;
                        create_capture_session_lock.notifyAll();
                    }
                    // don't throw CameraControllerException here, as won't be caught - instead we throw CameraControllerException below
                }

				/*
				public void onReady(CameraCaptureSession session) {
					if( MyDebug.LOG )
						Log.d(TAG, "onReady: " + session);
					if( pending_request_when_ready != null ) {
						if( MyDebug.LOG )
							Log.d(TAG, "have pending_request_when_ready: " + pending_request_when_ready);
						CaptureRequest request = pending_request_when_ready;
						pending_request_when_ready = null;
						try {
							captureSession.capture(request, previewCaptureCallback, backgroundHandler);
						}
						catch(CameraAccessException e) {
							if( MyDebug.LOG ) {
								Log.e(TAG, "failed to take picture");
								Log.e(TAG, "reason: " + e.getReason());
								Log.e(TAG, "message: " + e.getMessage());
							}
							e.printStackTrace();
							jpeg_todo = false;
							raw_todo = false;
							picture_cb = null;
							if( take_picture_error_cb != null ) {
								take_picture_error_cb.onError();
								take_picture_error_cb = null;
							}
						}
					}
				}*/
            }
            final MyStateCallback myStateCallback = new MyStateCallback();

            Surface preview_surface = getPreviewSurface();
            List<Surface> surfaces;
            if (video_recorder != null) {
                if (supports_photo_video_recording && !want_video_high_speed && want_photo_video_recording) {
                    surfaces = Arrays.asList(preview_surface, video_recorder_surface, imageReader.getSurface());
                } else {
                    surfaces = Arrays.asList(preview_surface, video_recorder_surface);
                }
                // n.b., raw not supported for photo snapshots while video recording
            } else if (imageReaderRaw != null) {
                surfaces = Arrays.asList(preview_surface, imageReader.getSurface(), imageReaderRaw.getSurface());
            } else {
                surfaces = Arrays.asList(preview_surface, imageReader.getSurface());
            }
            if (video_recorder != null && want_video_high_speed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                camera.createConstrainedHighSpeedCaptureSession(surfaces,
                        myStateCallback,
                        backgroundHandler);
                is_video_high_speed = true;
            } else {
                try {
                    camera.createCaptureSession(surfaces,
                            myStateCallback,
                            backgroundHandler);
                    is_video_high_speed = false;
                } catch (NullPointerException e) {
                    // have had this from some devices on Google Play, from deep within createCaptureSession
                    // note, we put the catch here rather than below, so as to not mask nullpointerexceptions
                    // from my code
                    e.printStackTrace();
                    throw new RuntimeException();
                }
            }
            synchronized (create_capture_session_lock) {
                while (!myStateCallback.callback_done) {
                    try {
                        // release the lock, and wait until myStateCallback calls notifyAll()
                        create_capture_session_lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (captureSession == null) {
                throw new RuntimeException();
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
            throw new RuntimeException();
        } catch (IllegalArgumentException e) {
            // have had crashes from Google Play, from both createConstrainedHighSpeedCaptureSession and
            // createCaptureSession
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    public void startPreview() {
        if (captureSession != null) {
            try {
                setRepeatingRequest();
            } catch (CameraAccessException e) {
                e.printStackTrace();
                // do via CameraControllerException instead of preview_error_cb, so caller immediately knows preview has failed
                throw new RuntimeException();
            }
            return;
        }
        createCaptureSession(null, false);
    }

    public void stopPreview() {
        if (camera == null || captureSession == null) {
            return;
        }
        try {
            //pending_request_when_ready = null;

            try {
                captureSession.stopRepeating();
            } catch (IllegalStateException e) {
                e.printStackTrace();
                // got this as a Google Play exception
                // we still call close() below, as it has no effect if captureSession is already closed
            }
            // although stopRepeating() alone will pause the preview, seems better to close captureSession altogether - this allows the app to make changes such as changing the picture size
            captureSession.close();
            captureSession = null;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        // simulate CameraController1 behaviour where face detection is stopped when we stop preview
        if (camera_settings.has_face_detect_mode) {
            camera_settings.has_face_detect_mode = false;
            camera_settings.setFaceDetectMode(previewBuilder);
            // no need to call setRepeatingRequest(), we're just setting the camera_settings for when we restart the preview
        }
    }

    public boolean startFaceDetection() {
        if (previewBuilder.get(CaptureRequest.STATISTICS_FACE_DETECT_MODE) != null && previewBuilder.get(CaptureRequest.STATISTICS_FACE_DETECT_MODE) != CaptureRequest.STATISTICS_FACE_DETECT_MODE_OFF) {
            return false;
        }
        if (supports_face_detect_mode_full) {
            camera_settings.has_face_detect_mode = true;
            camera_settings.face_detect_mode = CaptureRequest.STATISTICS_FACE_DETECT_MODE_FULL;
        } else if (supports_face_detect_mode_simple) {
            camera_settings.has_face_detect_mode = true;
            camera_settings.face_detect_mode = CaptureRequest.STATISTICS_FACE_DETECT_MODE_SIMPLE;
        } else {
            Log.e(TAG, "startFaceDetection() called but face detection not available");
            return false;
        }
        camera_settings.setFaceDetectMode(previewBuilder);
        camera_settings.setSceneMode(previewBuilder); // also need to set the scene mode
        try {
            setRepeatingRequest();
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void setFaceDetectionListener(final ICameraController.FaceDetectionListener listener) {
        this.face_detection_listener = listener;
        this.last_faces_detected = -1;
    }

    public void autoFocus(final ICameraController.AutoFocusCallback cb, boolean capture_follows_autofocus_hint) {
        fake_precapture_torch_focus_performed = false;
        if (camera == null || captureSession == null) {
            // should call the callback, so the application isn't left waiting (e.g., when we autofocus before trying to take a photo)
            cb.onAutoFocus(false);
            return;
        }
        Integer focus_mode = previewBuilder.get(CaptureRequest.CONTROL_AF_MODE);
        if (focus_mode == null) {
            // we preserve the old Camera API where calling autoFocus() on a device without autofocus immediately calls the callback
            // (unclear if Open Camera needs this, but just to be safe and consistent between camera APIs)
            cb.onAutoFocus(true);
            return;
        } else if ((!do_af_trigger_for_continuous || use_fake_precapture_mode) && focus_mode == CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE) {
            // See note above for do_af_trigger_for_continuous
            this.capture_follows_autofocus_hint = capture_follows_autofocus_hint;
            this.autofocus_cb = cb;
            return;
        } else if (is_video_high_speed) {
            // CONTROL_AF_TRIGGER_IDLE/CONTROL_AF_TRIGGER_START not supported for high speed video
            cb.onAutoFocus(true);
            return;
        }
		/*if( state == STATE_WAITING_AUTOFOCUS ) {
			if( MyDebug.LOG )
				Log.d(TAG, "already waiting for an autofocus");
			// need to update the callback!
			this.capture_follows_autofocus_hint = capture_follows_autofocus_hint;
			this.autofocus_cb = cb;
			return;
		}*/
        CaptureRequest.Builder afBuilder = previewBuilder;
        state = STATE_WAITING_AUTOFOCUS;
        precapture_state_change_time_ms = -1;
        this.capture_follows_autofocus_hint = capture_follows_autofocus_hint;
        this.autofocus_cb = cb;
        try {
            if (use_fake_precapture_mode && !camera_settings.isManualExposure) {
                boolean want_flash = false;
                if (camera_settings.flash_value.equals("flash_auto") || camera_settings.flash_value.equals("flash_frontscreen_auto")) {
                    // calling fireAutoFlash() also caches the decision on whether to flash - otherwise if the flash fires now, we'll then think the scene is bright enough to not need the flash!
                    if (fireAutoFlash())
                        want_flash = true;
                } else if (camera_settings.flash_value.equals("flash_on")) {
                    want_flash = true;
                }
                if (want_flash) {
                    afBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
                    afBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
//					test_fake_flash_focus++;
                    fake_precapture_torch_focus_performed = true;
                    setRepeatingRequest(afBuilder.build());
                    // We sleep for a short time as on some devices (e.g., OnePlus 3T), the torch will turn off when autofocus
                    // completes even if we don't want that (because we'll be taking a photo).
                    // Note that on other devices such as Nexus 6, this problem doesn't occur even if we don't have a separate
                    // setRepeatingRequest.
                    // Update for 1.37: now we do need this for Nexus 6 too, after switching to setting CONTROL_AE_MODE_ON_AUTO_FLASH
                    // or CONTROL_AE_MODE_ON_ALWAYS_FLASH even for fake flash (see note in CameraSettings.setAEMode()) - and we
                    // needed to increase to 200ms! Otherwise photos come out too dark for flash on if doing touch to focus then
                    // quickly taking a photo. (It also work to previously switch to CONTROL_AE_MODE_ON/FLASH_MODE_OFF first,
                    // but then the same problem shows up on OnePlus 3T again!)
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            // Camera2Basic sets a trigger with capture
            // Google Camera sets to idle with a repeating request, then sets af trigger to start with a capture
            afBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE);
            setRepeatingRequest(afBuilder.build());
            afBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
            capture(afBuilder.build());
        } catch (CameraAccessException e) {
            e.printStackTrace();
            state = STATE_NORMAL;
            precapture_state_change_time_ms = -1;
            autofocus_cb.onAutoFocus(false);
            autofocus_cb = null;
            this.capture_follows_autofocus_hint = false;
        }
        afBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE); // ensure set back to idle
    }

    public void setCaptureFollowAutofocusHint(boolean capture_follows_autofocus_hint) {
        this.capture_follows_autofocus_hint = capture_follows_autofocus_hint;
    }

    public void cancelAutoFocus() {
        if (camera == null || captureSession == null) {
            return;
        }

        if (is_video_high_speed) {
            return;
        }

        previewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
        // Camera2Basic does a capture then sets a repeating request - do the same here just to be safe
        try {
            capture();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        previewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE);
        this.autofocus_cb = null;
        this.capture_follows_autofocus_hint = false;
        state = STATE_NORMAL;
        precapture_state_change_time_ms = -1;
        try {
            setRepeatingRequest();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void setContinuousFocusMoveCallback(ICameraController.ContinuousFocusMoveCallback cb) {
        this.continuous_focus_move_callback = cb;
    }

    /**
     * Sets up a builder to have manual exposure time, if supported. The exposure time will be
     * clamped to the allowed values, and manual ISO will also be set based on the current ISO value.
     */
    private void setManualExposureTime(CaptureRequest.Builder stillBuilder, long exposure_time) {
        Range<Long> exposure_time_range = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE); // may be null on some devices
        Range<Integer> iso_range = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE); // may be null on some devices
        if (exposure_time_range != null && iso_range != null) {
            long min_exposure_time = exposure_time_range.getLower();
            long max_exposure_time = exposure_time_range.getUpper();
            if (exposure_time < min_exposure_time)
                exposure_time = min_exposure_time;
            if (exposure_time > max_exposure_time)
                exposure_time = max_exposure_time;
            stillBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF);
            {
                // set ISO
                int iso = 800;
                if (capture_result_has_iso)
                    iso = capture_result_iso;
                // see https://sourceforge.net/p/opencamera/tickets/321/ - some devices may have auto ISO that's
                // outside of the allowed manual iso range!
                iso = Math.max(iso, iso_range.getLower());
                iso = Math.min(iso, iso_range.getUpper());
                stillBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, iso);
            }
            if (capture_result_has_frame_duration)
                stillBuilder.set(CaptureRequest.SENSOR_FRAME_DURATION, capture_result_frame_duration);
            else
                stillBuilder.set(CaptureRequest.SENSOR_FRAME_DURATION, 1000000000L / 30);
            stillBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, exposure_time);
        }
    }

    private void takePictureAfterPrecapture() {
        if (!previewIsVideoMode) {
            // special burst modes not supported for photo snapshots when recording video
            if (burst_type == BURSTTYPE_EXPO || burst_type == BURSTTYPE_FOCUS) {
                takePictureBurstBracketing();
                return;
            } else if (burst_type == BURSTTYPE_NORMAL || burst_type == BURSTTYPE_CONTINUOUS) {
                takePictureBurst(false);
                return;
            }
        }
        if (camera == null || captureSession == null) {
            return;
        }
        try {
            CaptureRequest.Builder stillBuilder = camera.createCaptureRequest(previewIsVideoMode ? CameraDevice.TEMPLATE_VIDEO_SNAPSHOT : CameraDevice.TEMPLATE_STILL_CAPTURE);
            stillBuilder.set(CaptureRequest.CONTROL_CAPTURE_INTENT, CaptureRequest.CONTROL_CAPTURE_INTENT_STILL_CAPTURE);
            stillBuilder.setTag(new RequestTagObject(RequestTagType.CAPTURE));
            camera_settings.setupBuilder(stillBuilder, true);
            if (use_fake_precapture_mode && fake_precapture_torch_performed) {
                if (!camera_settings.isManualExposure)
                    stillBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
                stillBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
//				test_fake_flash_photo++;
            }
            if (!camera_settings.isManualExposure && this.optimise_ae_for_dro && capture_result_has_exposure_time && (camera_settings.flash_value.equals("flash_off") || camera_settings.flash_value.equals("flash_auto") || camera_settings.flash_value.equals("flash_frontscreen_auto"))) {
                final double full_exposure_time_scale = Math.pow(2.0, -0.5);
                final long fixed_exposure_time = 1000000000L / 60; // we only scale the exposure time at all if it's less than this value
                final long scaled_exposure_time = 1000000000L / 120; // we only scale the exposure time by the full_exposure_time_scale if the exposure time is less than this value
                long exposure_time = capture_result_exposure_time;
                if (exposure_time <= fixed_exposure_time) {
                    double exposure_time_scale = getScaleForExposureTime(exposure_time, fixed_exposure_time, scaled_exposure_time, full_exposure_time_scale);
                    exposure_time *= exposure_time_scale;
                    modified_from_camera_settings = true;
                    setManualExposureTime(stillBuilder, exposure_time);
                }
            }
            //stillBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            //stillBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // unclear why we wouldn't want to request ZSL
                // this is also required to enable HDR+ on Google Pixel devices when using Camera2: https://opensource.google.com/projects/pixelvisualcorecamera
                stillBuilder.set(CaptureRequest.CONTROL_ENABLE_ZSL, true);
            }
            clearPending();
            // shouldn't add preview surface as a target - no known benefit to doing so
            stillBuilder.addTarget(imageReader.getSurface());
            if (imageReaderRaw != null)
                stillBuilder.addTarget(imageReaderRaw.getSurface());

            n_burst = 1;
            n_burst_taken = 0;
            n_burst_total = n_burst;
            burst_single_request = false;
            if (!previewIsVideoMode) {
                // need to stop preview before capture (as done in Camera2Basic; otherwise we get bugs such as flash remaining on after taking a photo with flash)
                // but don't do this in video mode - if we're taking photo snapshots while video recording, we don't want to pause video!
                // update: bug with flash may have been device specific (things are fine with Nokia 8)
                captureSession.stopRepeating();
            }
            if (picture_cb != null) {
                picture_cb.onStarted();
            }
            //pending_request_when_ready = stillBuilder.build();
            captureSession.capture(stillBuilder.build(), previewCaptureCallback, backgroundHandler);
            //captureSession.capture(stillBuilder.build(), new CameraCaptureSession.CaptureCallback() {
            //}, backgroundHandler);
            if (sounds_enabled) // play shutter sound asap, otherwise user has the illusion of being slow to take photos
                media_action_sound.play(MediaActionSound.SHUTTER_CLICK);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            jpeg_todo = false;
            raw_todo = false;
            picture_cb = null;
            if (take_picture_error_cb != null) {
                take_picture_error_cb.onError();
                take_picture_error_cb = null;
            }
        }
    }

    private void takePictureBurstBracketing() {
        if (burst_type != BURSTTYPE_EXPO && burst_type != BURSTTYPE_FOCUS) {
            Log.e(TAG, "takePictureBurstBracketing called but unexpected burst_type: " + burst_type);
        }
        if (camera == null || captureSession == null) {
            return;
        }
        try {

            CaptureRequest.Builder stillBuilder = camera.createCaptureRequest(previewIsVideoMode ? CameraDevice.TEMPLATE_VIDEO_SNAPSHOT : CameraDevice.TEMPLATE_STILL_CAPTURE);
            stillBuilder.set(CaptureRequest.CONTROL_CAPTURE_INTENT, CaptureRequest.CONTROL_CAPTURE_INTENT_STILL_CAPTURE);
            // n.b., don't set RequestTagType.CAPTURE here - we only do it for the last of the burst captures (see below)
            camera_settings.setupBuilder(stillBuilder, true);
            clearPending();
            // shouldn't add preview surface as a target - see note in takePictureAfterPrecapture()
            // but also, adding the preview surface causes the dark/light exposures to be visible, which we don't want
            stillBuilder.addTarget(imageReader.getSurface());
            // don't add target imageReaderRaw, as Raw not supported for burst
            raw_todo = false; // raw not supported for burst

            List<CaptureRequest> requests = new ArrayList<>();

            if (burst_type == BURSTTYPE_EXPO) {

                stillBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF);
                if (use_fake_precapture_mode && fake_precapture_torch_performed) {
                    stillBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
//				test_fake_flash_photo++;
                }
                // else don't turn torch off, as user may be in torch on mode

                Range<Integer> iso_range = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE); // may be null on some devices
                if (iso_range == null) {
                    Log.e(TAG, "takePictureBurstBracketing called but null iso_range");
                } else {
                    // set ISO
                    int iso = 800;
                    // obtain current ISO/etc settings from the capture result - but if we're in manual ISO mode,
                    // might as well use the settings the user has actually requested (also useful for workaround for
                    // OnePlus 3T bug where the reported ISO and exposure_time are wrong in dark scenes)
                    if (camera_settings.isManualExposure)
                        iso = camera_settings.iso;
                    else if (capture_result_has_iso)
                        iso = capture_result_iso;
                    // see https://sourceforge.net/p/opencamera/tickets/321/ - some devices may have auto ISO that's
                    // outside of the allowed manual iso range!
                    iso = Math.max(iso, iso_range.getLower());
                    iso = Math.min(iso, iso_range.getUpper());
                    stillBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, iso);
                }
                if (capture_result_has_frame_duration)
                    stillBuilder.set(CaptureRequest.SENSOR_FRAME_DURATION, capture_result_frame_duration);
                else
                    stillBuilder.set(CaptureRequest.SENSOR_FRAME_DURATION, 1000000000L / 30);

                long base_exposure_time = 1000000000L / 30;
                if (camera_settings.isManualExposure)
                    base_exposure_time = camera_settings.exposure_time;
                else if (capture_result_has_exposure_time)
                    base_exposure_time = capture_result_exposure_time;

                int n_half_images = expo_bracketing_n_images / 2;
                long min_exposure_time = base_exposure_time;
                long max_exposure_time = base_exposure_time;
                final double scale = Math.pow(2.0, expo_bracketing_stops / (double) n_half_images);
                Range<Long> exposure_time_range = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE); // may be null on some devices
                if (exposure_time_range != null) {
                    min_exposure_time = exposure_time_range.getLower();
                    max_exposure_time = exposure_time_range.getUpper();
                }

                // darker images
                for (int i = 0; i < n_half_images; i++) {
                    long exposure_time = base_exposure_time;
                    if (exposure_time_range != null) {
                        double this_scale = scale;
                        for (int j = i; j < n_half_images - 1; j++)
                            this_scale *= scale;
                        exposure_time /= this_scale;
                        if (exposure_time < min_exposure_time)
                            exposure_time = min_exposure_time;
                        stillBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, exposure_time);
                        requests.add(stillBuilder.build());
                    }
                }

                // base image
                stillBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, base_exposure_time);
                requests.add(stillBuilder.build());

                // lighter images
                for (int i = 0; i < n_half_images; i++) {
                    long exposure_time = base_exposure_time;
                    if (exposure_time_range != null) {
                        double this_scale = scale;
                        for (int j = 0; j < i; j++)
                            this_scale *= scale;
                        exposure_time *= this_scale;
                        if (exposure_time > max_exposure_time)
                            exposure_time = max_exposure_time;
                        stillBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, exposure_time);
                        if (i == n_half_images - 1) {
                            // RequestTagType.CAPTURE should only be set for the last request, otherwise we'll may do things like turning
                            // off torch (for fake flash) before all images are received
                            // More generally, doesn't seem a good idea to be doing the post-capture commands (resetting ae state etc)
                            // multiple times, and before all captures are complete!
                            stillBuilder.setTag(new RequestTagObject(RequestTagType.CAPTURE));
                        }
                        requests.add(stillBuilder.build());
                    }
                }

                burst_single_request = true;
            } else {
                // BURSTTYPE_FOCUS

                if (use_fake_precapture_mode && fake_precapture_torch_performed) {
                    if (!camera_settings.isManualExposure)
                        stillBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
                    stillBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
//					test_fake_flash_photo++;
                }

                stillBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF); // just in case

                if (Math.abs(camera_settings.focus_distance - focus_bracketing_source_distance) < 1.0e-5) {
                    Log.d(TAG, "current focus matches source");
                } else if (Math.abs(camera_settings.focus_distance - focus_bracketing_target_distance) < 1.0e-5) {
                    Log.d(TAG, "current focus matches target");
                } else {
                    Log.d(TAG, "current focus matches neither source nor target");
                }

                List<Float> focus_distances = setupFocusBracketingDistances(focus_bracketing_source_distance, focus_bracketing_target_distance, focus_bracketing_n_images);
                if (focus_bracketing_add_infinity) {
                    focus_distances.add(0.0f);
                }
                for (int i = 0; i < focus_distances.size(); i++) {
                    stillBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, focus_distances.get(i));
                    if (i == focus_distances.size() - 1) {
                        stillBuilder.setTag(new RequestTagObject(RequestTagType.CAPTURE)); // set capture tag for last only
                    } else {
                        // but to cancel focus bracketing, we need to set a NONE tag on every other capture
                        stillBuilder.setTag(new RequestTagObject(RequestTagType.NONE));
                    }
                    requests.add(stillBuilder.build());

                    focus_bracketing_in_progress = true;
                }

                burst_single_request = false; // we set to false for focus bracketing, as we support bracketing with large numbers of images in this mode
                //burst_single_request = true; // test
            }

            n_burst = requests.size();
            n_burst_total = n_burst;
            n_burst_taken = 0;

            if (!previewIsVideoMode) {
                captureSession.stopRepeating(); // see note under takePictureAfterPrecapture()
            }

            if (picture_cb != null) {
                picture_cb.onStarted();
            }

            modified_from_camera_settings = true;
            if (use_expo_fast_burst && burst_type == BURSTTYPE_EXPO) { // alway use slow burst for focus bracketing
                int sequenceId = captureSession.captureBurst(requests, previewCaptureCallback, backgroundHandler);
            } else {
                slow_burst_capture_requests = requests;
                slow_burst_start_ms = System.currentTimeMillis();
                captureSession.capture(requests.get(0), previewCaptureCallback, backgroundHandler);
            }

            if (sounds_enabled) // play shutter sound asap, otherwise user has the illusion of being slow to take photos
                media_action_sound.play(MediaActionSound.SHUTTER_CLICK);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            jpeg_todo = false;
            raw_todo = false;
            picture_cb = null;
            if (take_picture_error_cb != null) {
                take_picture_error_cb.onError();
                take_picture_error_cb = null;
            }
        }
    }

    private void takePictureBurst(boolean continuing_fast_burst) {
        if (burst_type != BURSTTYPE_NORMAL && burst_type != BURSTTYPE_CONTINUOUS) {
            Log.e(TAG, "takePictureBurstBracketing called but unexpected burst_type: " + burst_type);
        }
        if (camera == null || captureSession == null) {
            return;
        }
        try {

            CaptureRequest.Builder stillBuilder = camera.createCaptureRequest(previewIsVideoMode ? CameraDevice.TEMPLATE_VIDEO_SNAPSHOT : CameraDevice.TEMPLATE_STILL_CAPTURE);
            stillBuilder.set(CaptureRequest.CONTROL_CAPTURE_INTENT, CaptureRequest.CONTROL_CAPTURE_INTENT_STILL_CAPTURE);
            // n.b., don't set RequestTagType.CAPTURE here - we only do it for the last of the burst captures (see below)
            camera_settings.setupBuilder(stillBuilder, true);
            if (use_fake_precapture_mode && fake_precapture_torch_performed) {
                if (!camera_settings.isManualExposure)
                    stillBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
                stillBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
//				test_fake_flash_photo++;
            }

            if (burst_type == BURSTTYPE_NORMAL && burst_for_noise_reduction) {
                // must be done after calling setupBuilder(), so we override the default EDGE_MODE and NOISE_REDUCTION_MODE
                stillBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_OFF);
                stillBuilder.set(CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE, CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE_OFF);
                stillBuilder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_OFF);
            }

            if (!continuing_fast_burst) {
                clearPending();
            }
            // shouldn't add preview surface as a target - see note in takePictureAfterPrecapture()
            stillBuilder.addTarget(imageReader.getSurface());
            // don't add target imageReaderRaw, as Raw not supported for burst
            raw_todo = false; // raw not supported for burst

            if (use_fake_precapture_mode && fake_precapture_torch_performed) {
                stillBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
//				test_fake_flash_photo++;
            }
            // else don't turn torch off, as user may be in torch on mode

            boolean is_new_burst = true;

            if (burst_type == BURSTTYPE_CONTINUOUS) {
                if (continuing_fast_burst) {
                    n_burst++;
                    is_new_burst = false;
                    /*if( !continuous_burst_in_progress ) // test bug where we call callback onCompleted() before all burst images are received
                    	n_burst = 1;*/
                } else {
                    continuous_burst_in_progress = true;
                    n_burst = 1;
                    n_burst_taken = 0;
                }
            } else if (burst_for_noise_reduction) {
                n_burst = 4;
                n_burst_taken = 0;

                if (capture_result_has_iso) {
                    // For Nexus 6, max reported ISO is 1196, so the limit for dark scenes shouldn't be more than this
                    // Nokia 8's max reported ISO is 1551
                    // Note that OnePlus 3T has max reported ISO of 800, but this is a device bug
                    if (capture_result_iso >= 1100) {
                        n_burst = noise_reduction_low_light ? 15 : 8;
                        boolean is_oneplus = Build.MANUFACTURER.toLowerCase(Locale.US).contains("oneplus");
                        // OnePlus 3T at least has bug where manual ISO can't be set to above 800, so dark images end up too dark -
                        // so no point enabling this code, which is meant to brighten the scene, not make it darker!
                        if (!camera_settings.isManualExposure && !is_oneplus) {
                            long exposure_time = noise_reduction_low_light ? 1000000000L / 3 : 1000000000L / 10;
                            if (!capture_result_has_exposure_time || capture_result_exposure_time < exposure_time) {
                                modified_from_camera_settings = true;
                                setManualExposureTime(stillBuilder, exposure_time);
                            }
                        }
                    } else if (capture_result_has_exposure_time) {
                        //final double full_exposure_time_scale = 0.5;
                        final double full_exposure_time_scale = Math.pow(2.0, -0.5);
                        final long fixed_exposure_time = 1000000000L / 60; // we only scale the exposure time at all if it's less than this value
                        final long scaled_exposure_time = 1000000000L / 120; // we only scale the exposure time by the full_exposure_time_scale if the exposure time is less than this value
                        long exposure_time = capture_result_exposure_time;
                        if (exposure_time <= fixed_exposure_time) {
                            //n_burst = 2;
                            n_burst = 3;
                            if (!camera_settings.isManualExposure) {
                                double exposure_time_scale = getScaleForExposureTime(exposure_time, fixed_exposure_time, scaled_exposure_time, full_exposure_time_scale);
                                exposure_time *= exposure_time_scale;
                                modified_from_camera_settings = true;
                                setManualExposureTime(stillBuilder, exposure_time);
                            }
                        }
                    }
                }
            } else {
                n_burst = burst_requested_n_images;
                n_burst_taken = 0;
            }
            n_burst_total = n_burst;
            burst_single_request = false;

            final CaptureRequest request = stillBuilder.build();
            stillBuilder.setTag(new RequestTagObject(RequestTagType.CAPTURE));
            final CaptureRequest last_request = stillBuilder.build();

            // n.b., don't stop the preview with stop.Repeating when capturing a burst

            if (picture_cb != null && is_new_burst) {
                picture_cb.onStarted();
            }

            final boolean use_burst = true;
            //final boolean use_burst = false;

            if (burst_type == BURSTTYPE_CONTINUOUS) {
                continuous_burst_requested_last_capture = !continuous_burst_in_progress;
                captureSession.capture(continuous_burst_in_progress ? request : last_request, previewCaptureCallback, backgroundHandler);

                if (continuous_burst_in_progress) {
                    final int continuous_burst_rate_ms = 100;
                    // also take the next burst after a delay
                    backgroundHandler.postDelayed(new Runnable() {

                        public void run() {
                            // note, even if continuous_burst_in_progress has become false by this point, still take one last
                            // photo, as need to ensure that we have a request with RequestTagType.CAPTURE, as well as ensuring
                            // we call the onCompleted() method of the callback
                            if (n_burst >= 10) {
                                // Nokia 8 in std mode without post-processing options doesn't hit this limit (we only hit this
                                // if it's set to "n_burst >= 5")
                                //throw new RuntimeException(); // test
                                backgroundHandler.postDelayed(this, continuous_burst_rate_ms);
                            } else if (picture_cb.imageQueueWouldBlock(n_burst + 1)) {
                                //throw new RuntimeException(); // test
                                backgroundHandler.postDelayed(this, continuous_burst_rate_ms);
                            } else {
                                takePictureBurst(true);
                            }
                        }
                    }, continuous_burst_rate_ms);
                }
            } else if (use_burst) {
                List<CaptureRequest> requests = new ArrayList<>();
                for (int i = 0; i < n_burst - 1; i++)
                    requests.add(request);
                requests.add(last_request);
                int sequenceId = captureSession.captureBurst(requests, previewCaptureCallback, backgroundHandler);
            } else {
                final int burst_delay = 100;
                new Runnable() {
                    int n_remaining = n_burst;


                    public void run() {
                        try {
                            captureSession.capture(n_remaining == 1 ? last_request : request, previewCaptureCallback, backgroundHandler);
                            n_remaining--;
                            if (n_remaining > 0) {
                                backgroundHandler.postDelayed(this, burst_delay);
                            }
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                            jpeg_todo = false;
                            raw_todo = false;
                            picture_cb = null;
                            if (take_picture_error_cb != null) {
                                take_picture_error_cb.onError();
                                take_picture_error_cb = null;
                            }
                        }
                    }
                }.run();
            }

            if (sounds_enabled && !continuing_fast_burst) // play shutter sound asap, otherwise user has the illusion of being slow to take photos
                media_action_sound.play(MediaActionSound.SHUTTER_CLICK);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            jpeg_todo = false;
            raw_todo = false;
            picture_cb = null;
            if (take_picture_error_cb != null) {
                take_picture_error_cb.onError();
                take_picture_error_cb = null;
            }
        }
    }

    private void runPrecapture() {
        // first run precapture sequence
        try {
            // use a separate builder for precapture - otherwise have problem that if we take photo with flash auto/on of dark scene, then point to a bright scene, the autoexposure isn't running until we autofocus again
            final CaptureRequest.Builder precaptureBuilder = camera.createCaptureRequest(previewIsVideoMode ? CameraDevice.TEMPLATE_VIDEO_SNAPSHOT : CameraDevice.TEMPLATE_STILL_CAPTURE);
            precaptureBuilder.set(CaptureRequest.CONTROL_CAPTURE_INTENT, CaptureRequest.CONTROL_CAPTURE_INTENT_STILL_CAPTURE);

            camera_settings.setupBuilder(precaptureBuilder, false);
            precaptureBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE);
            precaptureBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_IDLE);

            precaptureBuilder.addTarget(getPreviewSurface());

            state = STATE_WAITING_PRECAPTURE_START;
            precapture_state_change_time_ms = System.currentTimeMillis();

            // first set precapture to idle - this is needed, otherwise we hang in state STATE_WAITING_PRECAPTURE_START, because precapture already occurred whilst autofocusing, and it doesn't occur again unless we first set the precapture trigger to idle
            captureSession.capture(precaptureBuilder.build(), previewCaptureCallback, backgroundHandler);
            captureSession.setRepeatingRequest(precaptureBuilder.build(), previewCaptureCallback, backgroundHandler);

            // now set precapture
            precaptureBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            captureSession.capture(precaptureBuilder.build(), previewCaptureCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            jpeg_todo = false;
            raw_todo = false;
            picture_cb = null;
            if (take_picture_error_cb != null) {
                take_picture_error_cb.onError();
                take_picture_error_cb = null;
            }
        }
    }

    private void runFakePrecapture() {
        switch (camera_settings.flash_value) {
            case "flash_auto":
            case "flash_on":
                previewBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
                previewBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
//				test_fake_flash_precapture++;
                fake_precapture_torch_performed = true;
                break;
            case "flash_frontscreen_auto":
            case "flash_frontscreen_on":
                if (picture_cb != null) {
                    picture_cb.onFrontScreenTurnOn();
                }
                break;
            default:
                break;
        }
        state = STATE_WAITING_FAKE_PRECAPTURE_START;
        precapture_state_change_time_ms = System.currentTimeMillis();
        fake_precapture_turn_on_torch_id = null;
        try {
            CaptureRequest request = previewBuilder.build();
            if (fake_precapture_torch_performed) {
                fake_precapture_turn_on_torch_id = request;
            }
            setRepeatingRequest(request);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            jpeg_todo = false;
            raw_todo = false;
            picture_cb = null;
            if (take_picture_error_cb != null) {
                take_picture_error_cb.onError();
                take_picture_error_cb = null;
            }
        }
    }

    private boolean fireAutoFlashFrontScreen() {
        // iso_threshold fine-tuned for Nexus 6 - front camera ISO never goes above 805, but a threshold of 700 is too low
        final int iso_threshold = 750;
        return capture_result_has_iso && capture_result_iso >= iso_threshold;
    }

    /**
     * Used in use_fake_precapture mode when flash is auto, this returns whether we fire the flash.
     * If the decision was recently calculated, we return that same decision - used to fix problem that if
     * we fire flash during autofocus (for autofocus mode), we don't then want to decide the scene is too
     * bright to not need flash for taking photo!
     */
    private boolean fireAutoFlash() {
        long time_now = System.currentTimeMillis();
        final long cache_time_ms = 3000; // needs to be at least the time of a typical autoflash, see comment for this function above
        if (fake_precapture_use_flash_time_ms != -1 && time_now - fake_precapture_use_flash_time_ms < cache_time_ms) {
            fake_precapture_use_flash_time_ms = time_now;
            return fake_precapture_use_flash;
        }
        switch (camera_settings.flash_value) {
            case "flash_auto":
                fake_precapture_use_flash = is_flash_required;
                break;
            case "flash_frontscreen_auto":
                fake_precapture_use_flash = fireAutoFlashFrontScreen();
                break;
            default:
                // shouldn't really be calling this function if not flash auto...
                fake_precapture_use_flash = false;
                break;
        }
        // We only cache the result if we decide to turn on torch, as that mucks up our ability to tell if we need the flash (since once the torch
        // is on, the ae_state thinks it's bright enough to not need flash!)
        // But if we don't turn on torch, this problem doesn't occur, so no need to cache - and good that the next time we should make an up-to-date
        // decision.
        if (fake_precapture_use_flash) {
            fake_precapture_use_flash_time_ms = time_now;
        } else {
            fake_precapture_use_flash_time_ms = -1;
        }
        return fake_precapture_use_flash;
    }

    public void takePicture(final ICameraController.PictureCallback picture, final ICameraController.ErrorCallback error) {
        if (camera == null || captureSession == null) {
            error.onError();
            return;
        }
        this.picture_cb = picture;
        this.jpeg_todo = true;
        this.raw_todo = imageReaderRaw != null;
        this.done_all_captures = false;
        this.take_picture_error_cb = error;
        this.fake_precapture_torch_performed = false; // just in case still on?
        if (!ready_for_capture) {
            //throw new RuntimeException(); // debugging
        }

        {
            // Don't need precapture if flash off or torch
            // And currently isManualExposure manual mode doesn't support flash - but just in case that's changed later, we still probably don't want to be doing a precapture...
            if (camera_settings.isManualExposure || camera_settings.flash_value.equals("flash_off") || camera_settings.flash_value.equals("flash_torch")) {
                takePictureAfterPrecapture();
            } else if (use_fake_precapture_mode) {
                // fake flash auto/on mode
                // fake precapture works by turning on torch (or using a "front screen flash"), so we can't use the camera's own decision for flash auto
                // instead we check the current ISO value
                boolean auto_flash = camera_settings.flash_value.equals("flash_auto") || camera_settings.flash_value.equals("flash_frontscreen_auto");
                Integer flash_mode = previewBuilder.get(CaptureRequest.FLASH_MODE);
                if (auto_flash && !fireAutoFlash()) {
                    takePictureAfterPrecapture();
                } else if (flash_mode != null && flash_mode == CameraMetadata.FLASH_MODE_TORCH) {
                    // On some devices (e.g., OnePlus 3T), if we've already turned on torch for an autofocus immediately before
                    // taking the photo, ae convergence may have already occurred - so if we called runFakePrecapture(), we'd just get
                    // stuck waiting for CONTROL_AE_STATE_SEARCHING which will never happen, until we hit the timeout - it works,
                    // but it means taking photos is slower as we have to wait until the timeout
                    // Instead we assume that ae scanning has already started, so go straight to STATE_WAITING_FAKE_PRECAPTURE_DONE,
                    // which means wait until we're no longer CONTROL_AE_STATE_SEARCHING.
                    // (Note, we don't want to go straight to takePictureAfterPrecapture(), as it might be that ae scanning is still
                    // taking place.)
                    // An alternative solution would be to switch torch off and back on again to cause ae scanning to start - but
                    // at worst this is tricky to get working, and at best, taking photos would be slower.
                    fake_precapture_torch_performed = true; // so we know to fire the torch when capturing
//					test_fake_flash_precapture++; // for testing, should treat this same as if we did do the precapture
                    state = STATE_WAITING_FAKE_PRECAPTURE_DONE;
                    precapture_state_change_time_ms = System.currentTimeMillis();
                } else {
                    runFakePrecapture();
                }
            } else {
                // standard flash, flash auto or on
                // note that we don't call needsFlash() (or use is_flash_required) - as if ae state is neither CONVERGED nor FLASH_REQUIRED, we err on the side
                // of caution and don't skip the precapture
                //boolean needs_flash = capture_result_ae != null && capture_result_ae == CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED;
                boolean needs_flash = capture_result_ae != null && capture_result_ae != CaptureResult.CONTROL_AE_STATE_CONVERGED;
                if (camera_settings.flash_value.equals("flash_auto") && !needs_flash) {
                    // if we call precapture anyway, flash wouldn't fire - but we tend to have a pause
                    // so skipping the precapture if flash isn't going to fire makes this faster
                    takePictureAfterPrecapture();
                } else {
                    runPrecapture();
                }
            }
        }

		/*camera_settings.setupBuilder(previewBuilder, false);
    	previewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
		state = STATE_WAITING_AUTOFOCUS;
		precapture_started = -1;
    	//capture();
    	setRepeatingRequest();*/
    }

    public int getCameraOrientation() {
        // cached for performance, as this method is frequently called from Preview.onOrientationChanged
        return characteristics_sensor_orientation;
    }

    public boolean isFrontFacing() {
        // cached for performance, as this method is frequently called from Preview.onOrientationChanged
        return characteristics_is_front_facing;
    }

    public void unlock() {
        // do nothing at this stage
    }

    public void initVideoRecorderPrePrepare(MediaRecorder video_recorder) {
        // if we change where we play the START_VIDEO_RECORDING sound, make sure it can't be heard in resultant video
        if (sounds_enabled)
            media_action_sound.play(MediaActionSound.START_VIDEO_RECORDING);
    }

    public void initVideoRecorderPostPrepare(MediaRecorder video_recorder, boolean want_photo_video_recording) {
        if (camera == null) {
            Log.e(TAG, "no camera");
            return;
        }
        try {
            previewBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            previewIsVideoMode = true;
            previewBuilder.set(CaptureRequest.CONTROL_CAPTURE_INTENT, CaptureRequest.CONTROL_CAPTURE_INTENT_VIDEO_RECORD);
            camera_settings.setupBuilder(previewBuilder, false);
            createCaptureSession(video_recorder, want_photo_video_recording);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void reconnect() {
        // if we change where we play the STOP_VIDEO_RECORDING sound, make sure it can't be heard in resultant video
        if (sounds_enabled)
            media_action_sound.play(MediaActionSound.STOP_VIDEO_RECORDING);
        createPreviewRequest();
        createCaptureSession(null, false);
		/*if( MyDebug.LOG )
			Log.d(TAG, "add preview surface to previewBuilder");
    	Surface surface = getPreviewSurface();
		previewBuilder.addTarget(surface);*/
        //setRepeatingRequest();
    }

    public String getParametersString() {
        return null;
    }

    public boolean captureResultIsAEScanning() {
        return capture_result_is_ae_scanning;
    }

    public boolean needsFlash() {
        //boolean needs_flash = capture_result_ae != null && capture_result_ae == CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED;
        //return needs_flash;
        return is_flash_required;
    }

    public boolean needsFrontScreenFlash() {
        return camera_settings.flash_value.equals("flash_frontscreen_on") ||
                (camera_settings.flash_value.equals("flash_frontscreen_auto") && fireAutoFlashFrontScreen());
    }

    public boolean captureResultHasWhiteBalanceTemperature() {
        return capture_result_has_white_balance_rggb;
    }

    public int captureResultWhiteBalanceTemperature() {
        // for performance reasons, we don't convert from rggb to temperature in every frame, rather only when requested
        return convertRggbToTemperature(capture_result_white_balance_rggb);
    }

    public boolean captureResultHasIso() {
        return capture_result_has_iso;
    }

    public int captureResultIso() {
        return capture_result_iso;
    }

    public boolean captureResultHasExposureTime() {
        return capture_result_has_exposure_time;
    }

    public long captureResultExposureTime() {
        return capture_result_exposure_time;
    }

    public boolean captureResultHasFrameDuration() {
        return capture_result_has_frame_duration;
    }

    public long captureResultFrameDuration() {
        return capture_result_frame_duration;
    }


    private enum RequestTagType {
        CAPTURE, // request is either for a regular non-burst capture, or the last of a burst capture
        NONE // should be treated the same as if no tag had been set on the request
    }

    /* The class that we use for setTag() and getTag() for capture requests.
	   We use this class instead of assigning the RequestTagType directly, so we can modify it
	   (even though CaptureRequest only has a getTag() method).
	 */
    private static class RequestTagObject {
        private RequestTagType type;

        private RequestTagObject(RequestTagType type) {
            this.type = type;
        }

        private RequestTagType getType() {
            return type;
        }

        private void setType(RequestTagType type) {
            this.type = type;
        }
    }

    private class CameraSettings {
        // keys that we need to store, to pass to the stillBuilder, but doesn't need to be passed to previewBuilder (should set sensible defaults)
        private int rotation;
        private Location location;
        private byte jpeg_quality = 90;

        // keys that we have passed to the previewBuilder, that we need to store to also pass to the stillBuilder (should set sensible defaults, or use a has_ boolean if we don't want to set a default)
        private int scene_mode = CameraMetadata.CONTROL_SCENE_MODE_DISABLED;
        private int color_effect = CameraMetadata.CONTROL_EFFECT_MODE_OFF;
        private int white_balance = CameraMetadata.CONTROL_AWB_MODE_AUTO;
        private boolean has_antibanding;
        private int antibanding = CameraMetadata.CONTROL_AE_ANTIBANDING_MODE_AUTO;
        private boolean has_edge_mode;
        private int edge_mode = CameraMetadata.EDGE_MODE_FAST;
        private Integer default_edge_mode;
        private boolean has_noise_reduction_mode;
        private int noise_reduction_mode = CameraMetadata.NOISE_REDUCTION_MODE_FAST;
        private Integer default_noise_reduction_mode;
        private int white_balance_temperature = 5000; // used for white_balance == CONTROL_AWB_MODE_OFF
        private String flash_value = "flash_off";
        private boolean isManualExposure; // Whether manual iso mode. Change its value in settings.
        //private int ae_mode = CameraMetadata.CONTROL_AE_MODE_ON;
        //private int flash_mode = CameraMetadata.FLASH_MODE_OFF;
        private int iso;
        private long exposure_time = 0;
        private Rect scalar_crop_region; // no need for has_scalar_crop_region, as we can set to null instead
        private boolean has_ae_exposure_compensation;
        private int ae_exposure_compensation;
        private boolean has_af_mode;
        private int af_mode = CaptureRequest.CONTROL_AF_MODE_AUTO;
        private float focus_distance; // actual value passed to camera device (set to 0.0 if in infinity mode)
        private float focus_distance_manual; // saved setting when in manual mode (so if user switches to infinity mode and back, we'll still remember the manual focus distance)
        private boolean ae_lock;
        private boolean wb_lock;
        private MeteringRectangle[] af_regions; // no need for has_scalar_crop_region, as we can set to null instead
        private MeteringRectangle[] ae_regions; // no need for has_scalar_crop_region, as we can set to null instead
        private boolean has_face_detect_mode;
        private int face_detect_mode = CaptureRequest.STATISTICS_FACE_DETECT_MODE_OFF;
        private boolean video_stabilization;
        private boolean use_log_profile;
        private float log_profile_strength;
        private Integer default_tonemap_mode; // since we don't know what a device's tonemap mode is, we save it so we can switch back to it
        private Range<Integer> ae_target_fps_range;
        private long sensor_frame_duration;

        private int getExifOrientation() {
            int exif_orientation = ExifInterface.ORIENTATION_NORMAL;
            switch ((rotation + 360) % 360) {
                case 0:
                    exif_orientation = ExifInterface.ORIENTATION_NORMAL;
                    break;
                case 90:
                    exif_orientation = isFrontFacing() ?
                            ExifInterface.ORIENTATION_ROTATE_270 :
                            ExifInterface.ORIENTATION_ROTATE_90;
                    break;
                case 180:
                    exif_orientation = ExifInterface.ORIENTATION_ROTATE_180;
                    break;
                case 270:
                    exif_orientation = isFrontFacing() ?
                            ExifInterface.ORIENTATION_ROTATE_90 :
                            ExifInterface.ORIENTATION_ROTATE_270;
                    break;
                default:
                    // leave exif_orientation unchanged
                    break;
            }
            return exif_orientation;
        }

        private void setupBuilder(CaptureRequest.Builder builder, boolean is_still) {
            //builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            //builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            //builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
            //builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            //builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);

            builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE);

            setSceneMode(builder);
            setColorEffect(builder);
            setWhiteBalance(builder);
            setAntiBanding(builder);
            setAEMode(builder, is_still);
            setCropRegion(builder);
            setExposureCompensation(builder);
            setFocusMode(builder);
            setFocusDistance(builder);
            setAutoExposureLock(builder);
            setAutoWhiteBalanceLock(builder);
            setAFRegions(builder);
            setAERegions(builder);
            setFaceDetectMode(builder);
            setRawMode(builder);
            setVideoStabilization(builder);
            setLogProfile(builder);

            if (is_still) {
                if (location != null) {
                    builder.set(CaptureRequest.JPEG_GPS_LOCATION, location);
                }
                builder.set(CaptureRequest.JPEG_ORIENTATION, rotation);
                builder.set(CaptureRequest.JPEG_QUALITY, jpeg_quality);
            }

            setEdgeMode(builder);
            setNoiseReductionMode(builder);

			/*builder.set(CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE, CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE_OFF);
			builder.set(CaptureRequest.SHADING_MODE, CaptureRequest.SHADING_MODE_OFF);
			builder.set(CaptureRequest.STATISTICS_LENS_SHADING_MAP_MODE, CaptureRequest.STATISTICS_LENS_SHADING_MAP_MODE_OFF);
			builder.set(CaptureRequest.HOT_PIXEL_MODE, CaptureRequest.HOT_PIXEL_MODE_OFF);*/

			/*builder.set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_OFF);
			builder.set(CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE, CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE_OFF);
			builder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_OFF);
			if( Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M ) {
				builder.set(CaptureRequest.TONEMAP_MODE, CaptureRequest.TONEMAP_MODE_GAMMA_VALUE);
				builder.set(CaptureRequest.TONEMAP_GAMMA, 5.0f);
			}*/
			/*if( Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N ) {
				builder.set(CaptureRequest.CONTROL_POST_RAW_SENSITIVITY_BOOST, 0);
			}*/
			/*builder.set(CaptureRequest.CONTROL_EFFECT_MODE, CaptureRequest.CONTROL_EFFECT_MODE_OFF);
			builder.set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_OFF);
			builder.set(CaptureRequest.HOT_PIXEL_MODE, CaptureRequest.HOT_PIXEL_MODE_OFF);
			builder.set(CaptureRequest.CONTROL_SCENE_MODE, CaptureRequest.CONTROL_SCENE_MODE_DISABLED);
			builder.set(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_MODE_HIGH_QUALITY);
			builder.set(CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE, CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE_HIGH_QUALITY);
			builder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_OFF);
			builder.set(CaptureRequest.SHADING_MODE, CaptureRequest.SHADING_MODE_OFF);
			builder.set(CaptureRequest.STATISTICS_LENS_SHADING_MAP_MODE, CaptureRequest.STATISTICS_LENS_SHADING_MAP_MODE_OFF);*/
			/*if( Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M ) {
				builder.set(CaptureRequest.TONEMAP_MODE, CaptureRequest.TONEMAP_MODE_PRESET_CURVE);
				builder.set(CaptureRequest.TONEMAP_PRESET_CURVE, CaptureRequest.TONEMAP_PRESET_CURVE_SRGB);
			}*/

        }

        private boolean setSceneMode(CaptureRequest.Builder builder) {
            Integer current_scene_mode = builder.get(CaptureRequest.CONTROL_SCENE_MODE);
            if (has_face_detect_mode) {
                // face detection mode overrides scene mode
                if (current_scene_mode == null || current_scene_mode != CameraMetadata.CONTROL_SCENE_MODE_FACE_PRIORITY) {
                    builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_USE_SCENE_MODE);
                    builder.set(CaptureRequest.CONTROL_SCENE_MODE, CameraMetadata.CONTROL_SCENE_MODE_FACE_PRIORITY);
                    return true;
                }
            } else if (current_scene_mode == null || current_scene_mode != scene_mode) {
                if (scene_mode == CameraMetadata.CONTROL_SCENE_MODE_DISABLED) {
                    builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                } else {
                    builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_USE_SCENE_MODE);
                }
                builder.set(CaptureRequest.CONTROL_SCENE_MODE, scene_mode);
                return true;
            }
            return false;
        }

        private boolean setColorEffect(CaptureRequest.Builder builder) {
            if (builder.get(CaptureRequest.CONTROL_EFFECT_MODE) == null || builder.get(CaptureRequest.CONTROL_EFFECT_MODE) != color_effect) {
                builder.set(CaptureRequest.CONTROL_EFFECT_MODE, color_effect);
                return true;
            }
            return false;
        }

        private boolean setWhiteBalance(CaptureRequest.Builder builder) {
            boolean changed = false;
            if (builder.get(CaptureRequest.CONTROL_AWB_MODE) == null || builder.get(CaptureRequest.CONTROL_AWB_MODE) != white_balance) {
                builder.set(CaptureRequest.CONTROL_AWB_MODE, white_balance);
                changed = true;
            }
            if (white_balance == CameraMetadata.CONTROL_AWB_MODE_OFF) {
                // manual white balance
                RggbChannelVector rggbChannelVector = convertTemperatureToRggb(white_balance_temperature);
                builder.set(CaptureRequest.COLOR_CORRECTION_MODE, CameraMetadata.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX);
                builder.set(CaptureRequest.COLOR_CORRECTION_GAINS, rggbChannelVector);
                changed = true;
            }
            return changed;
        }

        private boolean setAntiBanding(CaptureRequest.Builder builder) {
            boolean changed = false;
            if (has_antibanding) {
                if (builder.get(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE) == null || builder.get(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE) != antibanding) {
                    builder.set(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE, antibanding);
                    changed = true;
                }
            }
            return changed;
        }

        private boolean setEdgeMode(CaptureRequest.Builder builder) {
            boolean changed = false;
            if (has_edge_mode) {
                if (default_edge_mode == null) {
                    // save the default_edge_mode edge_mode
                    default_edge_mode = builder.get(CaptureRequest.EDGE_MODE);
                }
                if (builder.get(CaptureRequest.EDGE_MODE) == null || builder.get(CaptureRequest.EDGE_MODE) != edge_mode) {
                    builder.set(CaptureRequest.EDGE_MODE, edge_mode);
                    changed = true;
                } else {
                }
            } else if (is_samsung_s7) {
                // see https://sourceforge.net/p/opencamera/discussion/general/thread/48bd836b/ ,
                // https://stackoverflow.com/questions/36028273/android-camera-api-glossy-effect-on-galaxy-s7
                // need EDGE_MODE_OFF to avoid a "glow" effect
                builder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_OFF);
            } else if (default_edge_mode != null) {
                if (builder.get(CaptureRequest.EDGE_MODE) != null && !builder.get(CaptureRequest.EDGE_MODE).equals(default_edge_mode)) {
                    builder.set(CaptureRequest.EDGE_MODE, default_edge_mode);
                    changed = true;
                }
            }
            return changed;
        }

        private boolean setNoiseReductionMode(CaptureRequest.Builder builder) {
            boolean changed = false;
            if (has_noise_reduction_mode) {
                if (default_noise_reduction_mode == null) {
                    // save the default_noise_reduction_mode noise_reduction_mode
                    default_noise_reduction_mode = builder.get(CaptureRequest.NOISE_REDUCTION_MODE);
                }
                if (builder.get(CaptureRequest.NOISE_REDUCTION_MODE) == null || builder.get(CaptureRequest.NOISE_REDUCTION_MODE) != noise_reduction_mode) {
                    builder.set(CaptureRequest.NOISE_REDUCTION_MODE, noise_reduction_mode);
                    changed = true;
                } else {
                }
            } else if (is_samsung_s7) {
                // see https://sourceforge.net/p/opencamera/discussion/general/thread/48bd836b/ ,
                // https://stackoverflow.com/questions/36028273/android-camera-api-glossy-effect-on-galaxy-s7
                // need NOISE_REDUCTION_MODE_OFF to avoid excessive blurring
                builder.set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_OFF);
            } else if (default_noise_reduction_mode != null) {
                if (builder.get(CaptureRequest.NOISE_REDUCTION_MODE) != null && !builder.get(CaptureRequest.NOISE_REDUCTION_MODE).equals(default_noise_reduction_mode)) {
                    builder.set(CaptureRequest.NOISE_REDUCTION_MODE, default_noise_reduction_mode);
                    changed = true;
                }
            }
            return changed;
        }

        private boolean setAEMode(CaptureRequest.Builder builder, boolean is_still) {
            if (isManualExposure) {
                builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF);
                builder.set(CaptureRequest.SENSOR_SENSITIVITY, iso);
                long actual_exposure_time = exposure_time;
                if (!is_still) {
                    // if this isn't for still capture, have a max exposure time of 1/12s
                    actual_exposure_time = Math.min(exposure_time, 1000000000L / 12);
                }
                builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, actual_exposure_time);
                if (sensor_frame_duration > 0) {
                    builder.set(CaptureRequest.SENSOR_FRAME_DURATION, sensor_frame_duration);
                }
                // for now, flash is disabled when using manual iso - it seems to cause ISO level to jump to 100 on Nexus 6 when flash is turned on!
                builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
            } else {
                if (ae_target_fps_range != null) {
                    Log.d(TAG, "set ae_target_fps_range: " + ae_target_fps_range);
                    builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, ae_target_fps_range);
                }

                // prefer to set flash via the ae mode (otherwise get even worse results), except for torch which we can't
                switch (flash_value) {
                    case "flash_off":
                        builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
                        builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
                        break;
                    case "flash_auto":
                        builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH);
                        builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
                        break;
                    case "flash_on":
                        // see note above for "flash_auto" for why we set this even fake flash mode - arguably we don't need to know
                        // about FLASH_REQUIRED in flash_on mode, but we set it for consistency...
		    		/*if( use_fake_precapture || CameraController.this.want_expo_bracketing )
			    		builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
		    		else*/
                        builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
                        builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
                        break;
                    case "flash_torch":
                        builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
                        builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
                        break;
                    case "flash_red_eye":
                        // not supported for expo bracketing or burst
                        if (CameraController.this.burst_type != BURSTTYPE_NONE)
                            builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
                        else
                            builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE);
                        builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
                        break;
                    case "flash_frontscreen_auto":
                    case "flash_frontscreen_on":
                    case "flash_frontscreen_torch":
                        builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
                        builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
                        break;
                }
            }
            return true;
        }

        private void setCropRegion(CaptureRequest.Builder builder) {
            if (scalar_crop_region != null) {
                builder.set(CaptureRequest.SCALER_CROP_REGION, scalar_crop_region);
            }
        }

        private boolean setExposureCompensation(CaptureRequest.Builder builder) {
            if (!has_ae_exposure_compensation)
                return false;
            if (isManualExposure) {
                return false;
            }
            if (builder.get(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION) == null || ae_exposure_compensation != builder.get(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION)) {
                builder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, ae_exposure_compensation);
                return true;
            }
            return false;
        }

        private void setFocusMode(CaptureRequest.Builder builder) {
            if (has_af_mode) {
                builder.set(CaptureRequest.CONTROL_AF_MODE, af_mode);
            }
        }

        private void setFocusDistance(CaptureRequest.Builder builder) {
            builder.set(CaptureRequest.LENS_FOCUS_DISTANCE, focus_distance);
        }

        private void setAutoExposureLock(CaptureRequest.Builder builder) {
            builder.set(CaptureRequest.CONTROL_AE_LOCK, ae_lock);
        }

        private void setAutoWhiteBalanceLock(CaptureRequest.Builder builder) {
            builder.set(CaptureRequest.CONTROL_AWB_LOCK, wb_lock);
        }

        private void setAFRegions(CaptureRequest.Builder builder) {
            if (af_regions != null && characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF) > 0) {
                builder.set(CaptureRequest.CONTROL_AF_REGIONS, af_regions);
            }
        }

        private void setAERegions(CaptureRequest.Builder builder) {
            if (ae_regions != null && characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE) > 0) {
                builder.set(CaptureRequest.CONTROL_AE_REGIONS, ae_regions);
            }
        }

        private void setFaceDetectMode(CaptureRequest.Builder builder) {
            if (has_face_detect_mode)
                builder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE, face_detect_mode);
            else
                builder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE, CaptureRequest.STATISTICS_FACE_DETECT_MODE_OFF);
        }

        private void setRawMode(CaptureRequest.Builder builder) {
            // DngCreator says "For best quality DNG files, it is strongly recommended that lens shading map output is enabled if supported"
            // docs also say "ON is always supported on devices with the RAW capability", so we don't check for STATISTICS_LENS_SHADING_MAP_MODE_ON being available
            if (want_raw && !previewIsVideoMode) {
                builder.set(CaptureRequest.STATISTICS_LENS_SHADING_MAP_MODE, CaptureRequest.STATISTICS_LENS_SHADING_MAP_MODE_ON);
            }
        }

        private void setVideoStabilization(CaptureRequest.Builder builder) {
            builder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, video_stabilization ? CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON : CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF);
        }

        private float getLogProfile(float in) {
            //final float black_level = 4.0f/255.0f;
            final float power = 1.0f / 2.2f;
            final float log_A = log_profile_strength;
			/*float out;
			if( in <= black_level ) {
				out = in;
			}
			else {
				float in_m = (in - black_level) / (1.0f - black_level);
				out = (float) (Math.log1p(log_A * in_m) / Math.log1p(log_A));
				out = black_level + (1.0f - black_level)*out;
			}*/
            float out = (float) (Math.log1p(log_A * in) / Math.log1p(log_A));

            // apply gamma
            out = (float) Math.pow(out, power);
            //out = Math.max(out, 0.5f);

            return out;
        }

        private void setLogProfile(CaptureRequest.Builder builder) {
            if (use_log_profile && log_profile_strength > 0.0f) {
                if (default_tonemap_mode == null) {
                    // save the default tonemap_mode
                    default_tonemap_mode = builder.get(CaptureRequest.TONEMAP_MODE);
                }
                // if changing this, make sure we don't exceed tonemap_max_curve_points_c
                // we want:
                // 0-15: step 1 (16 values)
                // 16-47: step 2 (16 values)
                // 48-111: step 4 (16 values)
                // 112-231 : step 8 (15 values)
                // 232-255: step 24 (1 value)
                int step = 1, c = 0;
                float[] values = new float[2 * tonemap_max_curve_points_c];
                for (int i = 0; i < 232; i += step) {
                    float in = ((float) i) / 255.0f;
                    float out = getLogProfile(in);
                    values[c++] = in;
                    values[c++] = out;
                    if ((c / 2) % 16 == 0) {
                        step *= 2;
                    }
                }
                values[c++] = 1.0f;
                values[c++] = getLogProfile(1.0f);
				/*{
					int n_values = 257;
					float [] values = new float [2*n_values];
					for(int i=0;i<n_values;i++) {
						float in = ((float)i) / (n_values-1.0f);
						float out = getLogProfile(in);
						values[2*i] = in;
						values[2*i+1] = out;
					}
				}*/
                // sRGB:
				/*float [] values = new float []{0.0000f, 0.0000f, 0.0667f, 0.2864f, 0.1333f, 0.4007f, 0.2000f, 0.4845f,
						0.2667f, 0.5532f, 0.3333f, 0.6125f, 0.4000f, 0.6652f, 0.4667f, 0.7130f,
						0.5333f, 0.7569f, 0.6000f, 0.7977f, 0.6667f, 0.8360f, 0.7333f, 0.8721f,
						0.8000f, 0.9063f, 0.8667f, 0.9389f, 0.9333f, 0.9701f, 1.0000f, 1.0000f};*/
				/*float [] values = new float []{0.0000f, 0.0000f, 0.05f, 0.3f, 0.1f, 0.4f, 0.2000f, 0.4845f,
						0.2667f, 0.5532f, 0.3333f, 0.6125f, 0.4000f, 0.6652f,
						0.5f, 0.78f, 1.0000f, 1.0000f};*/
				/*float [] values = new float []{0.0f, 0.0f, 0.05f, 0.4f, 0.1f, 0.54f, 0.2f, 0.6f, 0.3f, 0.65f, 0.4f, 0.7f,
						0.5f, 0.78f, 1.0f, 1.0f};*/
                //float [] values = new float []{0.0f, 0.5f, 0.05f, 0.6f, 0.1f, 0.7f, 0.2f, 0.8f, 0.5f, 0.9f, 1.0f, 1.0f};
                builder.set(CaptureRequest.TONEMAP_MODE, CaptureRequest.TONEMAP_MODE_CONTRAST_CURVE);
                TonemapCurve tonemap_curve = new TonemapCurve(values, values, values);
                builder.set(CaptureRequest.TONEMAP_CURVE, tonemap_curve);
            } else if (default_tonemap_mode != null) {
                builder.set(CaptureRequest.TONEMAP_MODE, default_tonemap_mode);
            }
        }

        // n.b., if we add more methods, remember to update setupBuilder() above!
    }

    private class OnImageAvailableListener implements ImageReader.OnImageAvailableListener {

        public void onImageAvailable(ImageReader reader) {
            if (picture_cb == null || !jpeg_todo) {
                // in theory this shouldn't happen - but if this happens, still free the image to avoid risk of memory leak,
                // or strange behaviour where an old image appears when the user next takes a photo
                Log.e(TAG, "no picture callback available");
                Image image = reader.acquireNextImage();
                image.close();
                return;
            }
            synchronized (image_reader_lock) {
                /* Whilst in theory the two setOnImageAvailableListener methods (for JPEG and RAW) seem to be called separately, I don't know if this is always true;
                 * also, we may process the RAW image when the capture result is available, which may be in a separate backgroundThread.
                 */
                Image image = reader.acquireNextImage();
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                image.close();
                n_burst_taken++;
                if (burst_single_request) {
                    pending_burst_images.add(bytes);
                    if (pending_burst_images.size() >= n_burst) { // shouldn't ever be greater, but just in case
                        if (pending_burst_images.size() > n_burst) {
                            Log.e(TAG, "pending_burst_images size " + pending_burst_images.size() + " is greater than n_burst " + n_burst);
                        }
                        // take a copy, so that we can clear pending_burst_images
                        List<byte[]> images = new ArrayList<>(pending_burst_images);
                        picture_cb.onBurstPictureTaken(images);
                        pending_burst_images.clear();

                        takePhotoCompleted();
                    } else {
                        takePhotoPartial();
                    }
                } else {
                    picture_cb.onPictureTaken(bytes);
                    n_burst--;
                    if (burst_type == BURSTTYPE_CONTINUOUS && !continuous_burst_requested_last_capture) {
                        // even if n_burst is 0, we don't want to give up if we're still in continuous burst mode
                        // also note if we do have continuous_burst_requested_last_capture==true, we still check for
                        // n_burst==0 below (as there may have been more than one image still to be received)
                        takePhotoPartial();
                    } else if (n_burst == 0) {
                        takePhotoCompleted();
                    } else {
                        takePhotoPartial();
                    }
                }
            }
        }

        /**
         * Called when an image has been received, but we're in a burst mode, and not all images have
         * been received.
         */
        private void takePhotoPartial() {
            if (slow_burst_capture_requests != null) {
                if (burst_type != BURSTTYPE_FOCUS) {
                    try {
                        if (camera != null && captureSession != null) { // make sure camera wasn't released in the meantime
                            captureSession.capture(slow_burst_capture_requests.get(n_burst_taken), previewCaptureCallback, backgroundHandler);
                        }
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                        jpeg_todo = false;
                        raw_todo = false;
                        picture_cb = null;
                        if (take_picture_error_cb != null) {
                            take_picture_error_cb.onError();
                            take_picture_error_cb = null;
                        }
                    }
                } else if (previewBuilder != null) { // make sure camera wasn't released in the meantime

                    if (!focus_bracketing_in_progress) {
                        slow_burst_capture_requests.subList(n_burst_taken + 1, slow_burst_capture_requests.size()).clear(); // https://stackoverflow.com/questions/1184636/shrinking-an-arraylist-to-a-new-size
                        // if burst_single_request==true, n_burst is constant and we stop when pending_burst_images.size() >= n_burst
                        // if burst_single_request==false, n_burst counts down and we stop when n_burst==0
                        if (burst_single_request)
                            n_burst = slow_burst_capture_requests.size();
                        else
                            n_burst = 1;
                        RequestTagObject requestTag = (RequestTagObject) slow_burst_capture_requests.get(slow_burst_capture_requests.size() - 1).getTag();
                        requestTag.setType(RequestTagType.CAPTURE);
                    }

                    // code for focus bracketing
                    try {
                        float focus_distance = slow_burst_capture_requests.get(n_burst_taken).get(CaptureRequest.LENS_FOCUS_DISTANCE);
                        previewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF);
                        previewBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, focus_distance);

                        setRepeatingRequest(previewBuilder.build());
                        //captureSession.capture(slow_burst_capture_requests.get(n_burst_taken), previewCaptureCallback, backgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                        jpeg_todo = false;
                        raw_todo = false;
                        picture_cb = null;
                        if (take_picture_error_cb != null) {
                            take_picture_error_cb.onError();
                            take_picture_error_cb = null;
                        }
                    }
                    backgroundHandler.postDelayed(new Runnable() {

                        public void run() {
                            if (camera != null && captureSession != null) { // make sure camera wasn't released in the meantime
                                if (picture_cb.imageQueueWouldBlock(1)) {
                                    backgroundHandler.postDelayed(this, 100);
                                    //throw new RuntimeException(); // test
                                } else {
                                    // For focus bracketing mode, we play the shutter sound per shot (so the user can tell when the sequence is complete).
                                    // From a user mode, the gap between shots in focus bracketing mode makes this more analogous to the auto-repeat mode
                                    // (at the Preview level), which makes the shutter sound per shot.

                                    if (sounds_enabled)
                                        media_action_sound.play(MediaActionSound.SHUTTER_CLICK);
                                    try {
                                        captureSession.capture(slow_burst_capture_requests.get(n_burst_taken), previewCaptureCallback, backgroundHandler);
                                    } catch (CameraAccessException e) {
                                        e.printStackTrace();
                                        jpeg_todo = false;
                                        raw_todo = false;
                                        picture_cb = null;
                                        if (take_picture_error_cb != null) {
                                            take_picture_error_cb.onError();
                                            take_picture_error_cb = null;
                                        }
                                    }
                                }
                            }
                        }
                    }, 500);
                }
            }
        }

        /**
         * Called when an image has been received, but either we're not in a burst mode, or we are
         * but all images have been received.
         */
        private void takePhotoCompleted() {
            // need to set jpeg_todo to false before calling onCompleted, as that may reenter CameraController to take another photo (if in auto-repeat burst mode) - see testTakePhotoRepeat()
            jpeg_todo = false;
            checkImagesCompleted();

            if (burst_type == BURSTTYPE_FOCUS)
                focus_bracketing_in_progress = false;
        }
    }

    private class OnRawImageAvailableListener implements ImageReader.OnImageAvailableListener {
        private CaptureResult capture_result;
        private Image image;

        void setCaptureResult(CaptureResult capture_result) {
            synchronized (image_reader_lock) {
                /* synchronize, as we don't want to set the capture_result, at the same time that onImageAvailable() is called, as
                 * we'll end up calling processImage() both in onImageAvailable() and here.
                 */
                this.capture_result = capture_result;
                if (image != null) {
                    // should call processImage() on UI backgroundThread, to be consistent with onImageAvailable()->processImage()
                    // important to avoid crash when pause preview is option, tested in testTakePhotoRawWaitCaptureResult()
                    final Activity activity = (Activity) context;
                    activity.runOnUiThread(new Runnable() {

                        public void run() {
                            // need to synchronize again
                            synchronized (image_reader_lock) {
                                processImage();
                            }
                        }
                    });
                }
            }
        }

        void clear() {
            synchronized (image_reader_lock) {
                // synchronize just to be safe?
                capture_result = null;
                image = null;
            }
        }

        /**
         * Calls to this method should synchronize on image_reader_lock.
         */
        private void processImage() {
            if (capture_result == null) {
                return;
            }
            if (image == null) {
                return;
            }
            DngCreator dngCreator = new DngCreator(characteristics, capture_result);
            // set fields
            dngCreator.setOrientation(camera_settings.getExifOrientation());
            if (camera_settings.location != null) {
                dngCreator.setLocation(camera_settings.location);
            }

            checkImagesCompleted();
        }


        public void onImageAvailable(ImageReader reader) {
            if (picture_cb == null || !raw_todo) {
                // in theory this shouldn't happen - but if this happens, still free the image to avoid risk of memory leak,
                // or strange behaviour where an old image appears when the user next takes a photo
                Log.e(TAG, "no picture callback available");
                Image this_image = reader.acquireNextImage();
                this_image.close();
                return;
            }
            synchronized (image_reader_lock) {
                // see comment above in setCaptureResult() for why we synchronize
                image = reader.acquireNextImage();
                processImage();
            }
        }
    }

    static public double getScaleForExposureTime(long exposure_time, long fixed_exposure_time, long scaled_exposure_time, double full_exposure_time_scale) {
        double alpha = (exposure_time - fixed_exposure_time) / (double) (scaled_exposure_time - fixed_exposure_time);
        if (alpha < 0.0)
            alpha = 0.0;
        else if (alpha > 1.0)
            alpha = 1.0;
        // alpha==0 means exposure_time_scale==1; alpha==1 means exposure_time_scale==full_exposure_time_scale
        return (1.0 - alpha) + alpha * full_exposure_time_scale;
    }

    public static List<Float> setupFocusBracketingDistances(float source, float target, int count) {
        List<Float> focus_distances = new ArrayList<>();
        float focus_distance_s = source;
        float focus_distance_e = target;
        final float max_focus_bracket_distance_c = 0.1f; // 10m
        focus_distance_s = Math.max(focus_distance_s, max_focus_bracket_distance_c); // since we'll dealing with 1/distance, use Math.max
        focus_distance_e = Math.max(focus_distance_e, max_focus_bracket_distance_c); // since we'll dealing with 1/distance, use Math.max
        // we want to interpolate linearly in distance, not 1/distance
        float real_focus_distance_s = 1.0f / focus_distance_s;
        float real_focus_distance_e = 1.0f / focus_distance_e;
        for (int i = 0; i < count; i++) {
            // for first and last, we still use the real focus distances; for intermediate values, we interpolate
            // with first/last clamped to max of 10m (to avoid taking reciprocal of 0)
            float distance;
            if (i == 0) {
                distance = source;
            } else if (i == count - 1) {
                distance = target;
            } else {
                //float alpha = ((float)i)/(count-1.0f);
                // rather than linear interpolation, we use log, see https://stackoverflow.com/questions/5215459/android-mediaplayer-setvolume-function
                // this gives more shots are closer focus distances
                int value = i;
                if (real_focus_distance_s > real_focus_distance_e) {
                    // if source is further than target, we still want the interpolation distances to be the same, but in reversed order
                    value = count - 1 - i;
                }
                float alpha = (float) (1.0 - Math.log(count - value) / Math.log(count));
                if (real_focus_distance_s > real_focus_distance_e) {
                    alpha = 1.0f - alpha;
                }
                float real_distance = (1.0f - alpha) * real_focus_distance_s + alpha * real_focus_distance_e;
                distance = 1.0f / real_distance;
            }
            focus_distances.add(distance);
        }
        return focus_distances;
    }
}

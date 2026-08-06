---
sidebar_position: 27
title: "Chapter 27: Camera Testing"
description: "The complete Android camera testing guide. Understand Camera ITS (Image Test Suite), what OEMs validate before shipping (feature combinations, scene tests, sensor fusion), CTS Verifier manual tests, and how to write your own instrumentation tests with Mockito mocks and parameterized hardware-level tests that run on CI."
keywords: [camera its, camera image test suite, cts camera, cts verifier, android camera testing, instrumentation test camera, mockito cameradevice, mock cameramanager, parameterized hardware level, sensor fusion test]
---

# Chapter 27: Camera Testing

## Summary

You built a camera app. It works on your Pixel. It works on your Galaxy. Does it work on the $99 Android Go device with a `LEGACY` HAL whose vendor misimplemented `CONTROL_AF_TRIGGER_START` and returns every `SENSOR_EXPOSURE_TIME` in *microseconds* instead of nanoseconds?

Testing camera software is a two-part problem: **OEM validation at the HAL level** (the tests Google *forces* every manufacturer to pass before a device can ship with Google Play) and **app-level testing on CI** (the tests you run against your own code without requiring physical camera hardware). This chapter covers both. First you will learn what Camera ITS (the Image Test Suite, part of CTS) actually validates on physical test rigs: stream combination enumeration, physical scene luminance linearity, and sensor/gyro timestamp fusion. Then you will learn how to write your own instrumentation tests using Mockito mocks for `CameraManager`/`CameraDevice`/`CaptureSession` so your entire camera stack runs on headless CI servers, plus a parameterized test pattern that asserts your code gracefully degrades on `LEGACY` hardware instead of crashing.

Use **Android Camera Parameters** ([Google Play](https://play.google.com/store/apps/details?id=com.minininja.cameraparams), [GitHub](https://github.com/zoozooll/AndroidCameraParameters)) as the reference tool to inspect the exact capabilities your tests should assert against — it surfaces every `CameraCharacteristics` key that ITS also validates on real rigs.

---

## Part One: How OEMs Validate Cameras — Camera ITS and CTS

Before a device can ship with Google Mobile Services (GMS), it must pass the Android Compatibility Test Suite (CTS). Camera CTS has two halves: programmatic CTS tests that run via `Tradefed`, and the Camera ITS (Image Test Suite) that requires a physical test lab with automated rigs.

### Test Categories

```mermaid
graph TB
    subgraph CTS[Android CTS — Camera Section]
        direction TB
        CTS_API[API Tests<br/>— CameraCharacteristics keys<br/>— isSessionConfigurationSupported<br/>— All use cases enumerate correctly]
        CTS_FLOW[Flow Tests<br/>— open → close<br/>— open → session → capture → close<br/>— Rapid open/close stress]
        CTS_V[CTS Verifier<br/>Manual on-device tests<br/>— Preview smoothness<br/>— Capture quality<br/>— Multi-camera switch]
    end
    subgraph ITS[Camera ITS — Image Test Suite]
        direction TB
        ITS_COMBI[test_feature_combination<br/>Stream permutations × FPS × HDR<br/>Thousands of calls to<br/>isSessionConfigurationSupported]
        ITS_SCENE[Physical Scene Tests<br/>scene0 (uniform gray)<br/>scene1_1 (color checker)<br/>Automated tablet display → DUT]
        ITS_FUSION[sensor_fusion test<br/>Gyro timestamps must align with<br/>SENSOR_TIMESTAMP in CaptureResult<br/>±1ms tolerance]
        ITS_3A[3A Convergence tests<br/>AE/AF/AWB must converge within<br/>N frames under standard lighting]
        ITS_HDR[HDR / Ultra HDR tests<br/>JPEG_R gainmap validity<br/>Dynamic range measurement]
    end
    CTS --> SHIP[(Ships if ALL pass)]
    ITS --> SHIP

    style ITS_COMBI fill:#d9e6f2
    style ITS_SCENE fill:#d9f2e6
    style ITS_FUSION fill:#f2d9e6
    style CTS_API fill:#fff3cd
    style CTS_V fill:#fff3cd
```

Everything in the diagram is required. If even *one* of these tests fails on a single camera ID, the device does not ship. That is why understanding ITS helps your app: it guarantees a baseline below which no HAL can fall, and it documents exactly what behaviors you can rely on.

### The Camera ITS Test Rig Architecture

A real Camera ITS lab looks like this:

```mermaid
graph LR
    TC[Test Controller PC<br/>Linux + Tradefed CLI<br/>Runs python3 its/scripts]
    TC -->|USB 3.x ADB| DUT[DUT Phone or Tablet<br/>Device Under Test<br/>Camera facing tablet display]
    TC -->|USB 3.x| TPD[Tablet Display<br/>~10" calibrated 4K panel<br/>Runs ITS tabletd APK]
    TC -->|GPIO / USB relay| LIGHT[Controlled Lighting<br/>CCT-tunable LED panels<br/>2700K-6500K ±2%]
    TPD -->|projects scene0 / scene1_1<br/>via HDMI/Internal display| DUT_CAM[DUT Rear Camera Sensor]
    DUT_CAM -->|captures frames over MIPI → HAL| DUT
    DUT -->|DNG/JPEG + CaptureResults<br/>pulled via adb pull| TC
    TC -->|runs numpy / scipy analysis<br/>luminance linearity, color error, sharpness| RESULT[(PASS / FAIL report + JSON)]
```

The key detail is the *closed loop*. The test controller knows *exactly* what pixel values it commanded the tablet display to show (e.g. a uniform gray at 50% intensity with a precisely known 6500K color temperature) and then verifies numerically that the DUT camera's output — both pixel luminance in the JPEG/DNG *and* the reported `SENSOR_EXPOSURE_TIME` × `SENSOR_SENSITIVITY` in the `CaptureResult` — matches the physical input to within allowed tolerances.

#### `test_feature_combination`: The Enumeration Gauntlet

The single biggest ITS test by runtime is `test_feature_combination`. It enumerates every legal stream size from `SCALER_STREAM_CONFIGURATION_MAP`, every format (`PRIV`, `YUV`, `JPEG`, `RAW_SENSOR`, `JPEG_R`, `DEPTH`), every FPS range from `CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES`, every output-surface *count* combination (1-output, 2-output, 3-output session configs), and every HDR mode flag — then calls `isSessionConfigurationSupported` on the resulting SessionConfiguration, captures one frame per supported config, and asserts the frame is not corrupted. The total number of combinations is often 30,000–100,000 per camera ID.

For you as an app developer, the takeaway is simple: **if `isSessionConfigurationSupported` returns `true` on a CTS-passing device, that stream combination actually works, in both directions.** If it returns `false`, do not try it. Rely on this call before you fall back to smaller sizes. This is the exact same query CameraX uses internally in its resolution selector.

#### Physical Scene Tests: Linearity of Exposure

Scene0 and scene1_1 tests validate that the camera's *reported* exposure math matches its *measured* pixel output. The test rig projects a uniform gray field (scene0) of known luminance `L` onto the DUT. It then commands a sweep of N different `(SENSOR_EXPOSURE_TIME, SENSOR_SENSITIVITY)` pairs from the full available range, captures a DNG frame per pair, and computes the arithmetic mean pixel value `Y` across the sensor's active array.

The assertion is strictly linear:

```
Y_i / (EXPOSURE_TIME_i × SENSITIVITY_i) = constant ± tolerance
```

across *all* captured pairs. If the product doubles, pixel luminance must double. If it halves, luminance must halve. Any deviation above ~1% in the midtones fails the test.

Why this matters to you: it is the guarantee that your manual exposure slider (Chapter 14) produces mathematically predictable results on a CTS-compliant device. If your app computes the "next ISO/exposure pair" for a +1 EV step, the output image really will be one stop brighter. On non-CTS-compliant devices (imported gray-market phones, custom ROMs without CTS), this guarantee does not hold, and your manual exposure UI will appear broken.

#### `sensor_fusion`: The Timestamp Fusion Test

EIS (Electronic Image Stabilization) and AR tracking live or die by this test. While the DUT is recording a video, the test controller physically rotates the phone on a motorized gimbal at known angular velocity. Simultaneously, it polls the DUT's gyroscope sensor via `SensorManager` at 400Hz+ and the camera's `CaptureResult.SENSOR_TIMESTAMP` at 30/60fps.

The pass condition: every gyro timestamp and every frame `SENSOR_TIMESTAMP` must be in the exact same `CLOCK_MONOTONIC` timebase, with gyro samples interpolated at the camera sample time matching the gimbal's commanded angular velocity to within ±0.1 rad/s and ±1ms.

If this test fails, the HAL's timestamp source is wrong — typically it mixed `CLOCK_REALTIME` (wall time, which jumps during NTP sync) with `CLOCK_MONOTONIC` (steady, monotonic). Google rejects the device. For you, this means you can safely feed `CaptureResult.SENSOR_TIMESTAMP` directly into ARCore's camera image update call without applying any custom timestamp offset, on any GMS-certified device.

### CTS Verifier: Manual User Tests

Not everything can be automated. CTS Verifier is an on-device APK that a human QA tester uses for subjective tests:

- **Preview smoothness:** 30 seconds of panning the device; tester scores perceived smoothness 1–5. (Objective telemetry is also captured via Choreographer dumpsys.)
- **Capture quality:** 5 photos of standard scenes under standard lighting; tester compares against a gold-reference device.
- **Multi-camera zoom transition:** While zooming continuously 0.5×–10×, there must be no visible pop, glitch, or black frame between physical camera switches.
- **HDR/JPEG_R quality:** Side-by-side SDR and HDR captures are compared against known-good reference imagery.

These are subjective, but the bar is public. If your app targets similar UX goals (smooth zoom transitions, HDR captures), you can replicate the same test procedures in your internal QA lab with the same scene0/scene1_1 tablet rig.

---

## Part Two: Testing Your Own App — Instrumentation and Mocking

OEM tests validate the HAL. You need to validate *your* code. The canonical mistake teams make is requiring a real phone with a working camera on their CI server. Don't. With Mockito's `mock()` + `ArgumentCaptor`, every single Camera2 class — `CameraManager`, `CameraDevice`, `CameraCaptureSession`, `CaptureResult` — is an interface or a non-final class that mocks cleanly. You can run your entire camera pipeline on CI on a headless Linux x86 emulator with no camera hardware at all.

### Example 1: Capture a "Frame" and Verify CaptureResult Contains Expected EXPOSURE_TIME (AndroidTest with Mockito)

```kotlin
// app/src/androidTest/java/com/example/camera/CameraCaptureTest.kt
@RunWith(AndroidJUnit4::class)
@SmallTest
class CameraCaptureTest {

    @Test
    fun captureRequest_containsManualExposureTime_andResultEchoesIt() = runTest {
        // ---- Arrange ----
        val mockCameraManager = mock<CameraManager>()
        val mockCameraDevice = mock<CameraDevice>()
        val mockSession = mock<CameraCaptureSession>()
        val mockSurface = mock<Surface>()
        val testHandler = Handler(Looper.getMainLooper())

        val expectedExposureNs = 16_666_666L // 1/60 s
        val expectedIso = 400

        // Capture the StateCallback passed to openCamera
        val deviceCallbackCaptor =
            argumentCaptor<CameraDevice.StateCallback>()
        whenever(mockCameraManager.openCamera(
            anyString(), deviceCallbackCaptor.capture(), any()
        )).thenAnswer { /* no-op; we fire callback manually */ }

        // Capture the session state callback
        val sessionCallbackCaptor =
            argumentCaptor<CameraCaptureSession.StateCallback>()
        whenever(mockCameraDevice.createCaptureSession(
            anyList<Surface>(),
            sessionCallbackCaptor.capture(),
            any()
        )).thenAnswer { /* no-op */ }

        // Capture the CaptureCallback passed to capture()
        val captureCallbackCaptor =
            argumentCaptor<CameraCaptureSession.CaptureCallback>()
        whenever(mockSession.capture(
            any(), captureCallbackCaptor.capture(), any()
        )).thenReturn(1)

        // Build the camera-under-test using your wrapper
        val cameraWrapper = YourCameraWrapper(mockCameraManager, testHandler)

        // ---- Act: fire the open → configure → capture chain ----
        cameraWrapper.open("0")
        // (Inside YourCameraWrapper.open() called
        //  mockCameraManager.openCamera, which captured the callback.)
        // Simulate HAL returning success:
        deviceCallbackCaptor.lastValue.onOpened(mockCameraDevice)

        cameraWrapper.createSession(listOf(mockSurface))
        sessionCallbackCaptor.lastValue.onConfigured(mockSession)

        val requestBuilder: CaptureRequest.Builder =
            mockCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
        // (Your wrapper applies manual settings here)
        requestBuilder.set(CaptureRequest.CONTROL_MODE,
                           CaptureRequest.CONTROL_MODE_OFF)
        requestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME,
                           expectedExposureNs)
        requestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY,
                           expectedIso)

        val resultDeferred = async { cameraWrapper.capture(requestBuilder.build()) }

        // ---- Assert 1: the CaptureRequest sent to the HAL had the right keys ----
        val sentRequest: CaptureRequest = captureArg(mockSession, 0) {
            capture(any(), any(), any())
        }
        assertThat(sentRequest[CaptureRequest.SENSOR_EXPOSURE_TIME])
            .isEqualTo(expectedExposureNs)
        assertThat(sentRequest[CaptureRequest.SENSOR_SENSITIVITY])
            .isEqualTo(expectedIso)

        // ---- Act 2: Simulate the HAL returning a CaptureResult ----
        val mockResult = mock<TotalCaptureResult>().apply {
            whenever(get(CaptureResult.SENSOR_EXPOSURE_TIME))
                .thenReturn(expectedExposureNs)
            whenever(get(CaptureResult.SENSOR_SENSITIVITY))
                .thenReturn(expectedIso)
            whenever(frameNumber).thenReturn(1L)
        }
        captureCallbackCaptor.lastValue
            .onCaptureCompleted(mockSession, sentRequest, mockResult)

        // ---- Assert 2: wrapper's returned capture result echoes exposure ----
        val actual = resultDeferred.await()
        assertThat(actual.exposureTimeNanos).isEqualTo(expectedExposureNs)
        assertThat(actual.iso).isEqualTo(expectedIso)
    }
}
```

The pattern is always the same:
1. `argumentCaptor` the callback that would go to the HAL.
2. Call your wrapper.
3. Fire the callback's success method *as if the HAL responded*.
4. Assert on both the inputs (what your wrapper sent to the HAL) and the outputs (what your wrapper handed back to the caller).

This runs on an emulator with no camera. No hardware. No flakiness from lighting. 10,000 runs produce the same 10,000 passes.

### Example 2: Parameterized Test — LEGACY Devices Must Gracefully Degrade, Never Crash

Every production camera app must run on `LEGACY` HALs. The single most common bug is calling `CaptureRequest.CONTROL_MODE_OFF` on a `LEGACY` device: the HAL ignores it, but your wrapper interprets the resulting `CaptureResult.CONTROL_AE_STATE == SEARCHING` as a transient failure and tries to restart AE in an infinite loop, eventually ANRing.

Parameterize your tests per `INFO_SUPPORTED_HARDWARE_LEVEL`:

```kotlin
@RunWith(Parameterized::class)
class HardwareLevelGracefulDegradationTest(
    private val hardwareLevel: Int,
    private val hardwareLevelName: String
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{1}")
        fun data() = listOf(
            arrayOf(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
                    "LEGACY"),
            arrayOf(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
                    "LIMITED"),
            arrayOf(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
                    "FULL"),
            arrayOf(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3,
                    "LEVEL_3"),
        )
    }

    @Test
    fun requestManualExposure_onAnyHardwareLevel_doesNotCrash_orHang() = runTest {
        val mockCameraManager = mock<CameraManager>()
        val mockChars = mock<CameraCharacteristics>()
        whenever(mockChars.get<Int>(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL
        )).thenReturn(hardwareLevel)
        whenever(mockCameraManager.getCameraCharacteristics("0"))
            .thenReturn(mockChars)

        val wrapper = YourCameraWrapper(mockCameraManager, testHandler)
        wrapper.open("0")
        // ... (boilerplate session setup as before, omitted)

        val job = launch {
            wrapper.setManualExposure(iso = 400, exposureNs = 8_000_000L)
        }

        // No hang — must complete within timeout even on LEGACY
        withTimeoutOrNull(2_000) { job.join() }
            ?: fail("setManualExposure hung on $hardwareLevelName hardware")

        // No exception leaked to uncaught
        assertThat(job.isCancelled).isFalse()
    }
}
```

Run this against every new build. It takes 400ms. It catches the exact class of `LEGACY`-HAL hangs that would otherwise only show up in Play Console crash reports months later.

### AndroidTest on Real Hardware: Sanity Capture That EXPOSURE_TIME Is Correct

For nightly runs against a small farm of real phones, write a short AndroidTest that opens the *actual* camera, captures one RAW frame, and asserts that `CaptureResult.SENSOR_EXPOSURE_TIME` was within 5% of the requested value. This guards against HAL regressions on specific OS builds:

```kotlin
@RunWith(AndroidJUnit4::class)
@RequiresDevice
@LargeTest
class RealHardwareCaptureSanityTest {

    @Test
    fun realCapture_exposureTimeIsWithin5PercentOfRequested() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().context
        val camManager = context.getSystemService(Context.CAMERA_SERVICE)
            as CameraManager
        val chars = camManager.getCameraCharacteristics("0")
        assumeTrue(
            "Requires FULL or LEVEL_3 for manual exposure",
            chars[CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL] in
            setOf(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3,
            )
        )

        // ... open camera, create ImageReader (PRIVATE or YUV) session, capture
        // a single manual frame with a known exposure using the wrappers from
        // Chapter 26 coroutines ...

        val requestedNs = 10_000_000L // 1/100s
        val result: TotalCaptureResult =
            cameraWrapper.captureManualExposure(requestedNs, iso = 200)

        val actualNs = result[CaptureResult.SENSOR_EXPOSURE_TIME]!!
        val tolerancePct = abs(actualNs - requestedNs) * 100.0 / requestedNs
        assertThat(tolerancePct)
            .withFailMessage(
                "Requested %d ns, got %d ns (%$.1f%% off >5%%)",
                requestedNs, actualNs, tolerancePct
            )
            .isLessThan(5.0)
    }
}
```

This test is flaky by nature — it depends on real hardware. But it catches the exact class of vendor OTA updates that silently break manual exposure on flagship devices. Run it nightly on your 5–10 device farm; the signal is worth the noise.

---

## Summary

Camera testing divides into OEM validation and app validation. OEMs must pass CTS and Camera ITS, a physically-rigged test suite that enforces stream combination support (via thousands of `isSessionConfigurationSupported` calls), luminance linearity across the exposure × sensitivity plane, and sensor/gyro timestamp alignment for EIS and AR. Apps test via instrumentation: Mockito mocks every Camera2 class, `ArgumentCaptor` grabs HAL callbacks, you fire them manually, and you assert both request and result without touching real hardware — enabling CI runs on headless emulators. Parameterize your wrapper tests against every `INFO_SUPPORTED_HARDWARE_LEVEL` (especially `LEGACY`) to guarantee graceful degradation, and run a small suite of `@RequiresDevice @LargeTest` sanity captures against a real device farm to catch OTA regressions.

## What's Next

You have mastered Camera2's public API from Kotlin through native NDK, wrapped it in coroutines, and verified it against tests. But what actually happens *under the hood* when you call `CameraManager.openCamera`? What is HAL3? Where does the Binder IPC boundary really live? And how does `CameraDeviceSetup` (Android 15, API 35) change the architecture by decoupling capability queries from sensor power? Chapter 28 is the grand architecture finale: the full stack from app code to the VCM voice coil motor in the lens barrel.

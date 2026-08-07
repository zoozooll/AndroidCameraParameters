---
sidebar_position: 25
title: "제25장: 네이티브 카메라 개발 (NDK)"
description: "고성능 실시간 처리를 위해 안드로이드 NDK의 ACamera API를 활용하세요. C++에서 카메라를 열고, 네이티브 하드웨어 버퍼를 사용하여 제로 복사(Zero-copy) 파이프라인을 구축하며, Vulkan 및 OpenGL ES와의 고속 연동 방법을 배웁니다."
keywords: [안드로이드 NDK 카메라, ACamera API, libcamera2ndk, 네이티브 카메라, 제로 복사 카메라, C++ 카메라 개발, Vulkan 카메라 연동, 고성능 안드로이드 카메라]
---

# 제25장: 네이티브 카메라 개발 (NDK)

## 요약

카메라 미리보기에 복잡한 실시간 쉐이더를 적용하거나, 증강 현실(AR) 엔진을 구축하거나, 매 프레임마다 고성능 컴퓨터 비전 알고리즘을 실행해야 한다면, Java/Kotlin의 가비지 컬렉션(GC) 지연과 JNI 오버헤드가 걸림돌이 될 수 있습니다. 1,200만 화소의 프레임 데이터를 Java와 네이티브 레이어 사이로 복사하는 것은 성능상 매우 비효율적입니다.

안드로이드 NDK는 안드로이드 7.0 (API 24)부터 **네이티브 카메라 API(ACamera)**를 제공합니다. 이 API를 사용하면 C++ 코드에서 직접 카메라 장치를 열고, 요청을 보내고, 프레임을 받을 수 있습니다. 가장 큰 장점은 **"제로 복사(Zero-copy)"** 파이프라인입니다. 카메라 하드웨어가 생성한 `AHardwareBuffer`를 Java 레이어를 거치지 않고 직접 Vulkan 텍스처나 OpenGL 테스처로 바인딩하여 GPU에서 즉시 처리할 수 있습니다. 이 장에서는 NDK 카메라의 아키텍처를 배우고, C++에서 세션을 구성하는 법을 익히며, 최신 안드로이드 게임 엔진이나 미디어 프레임워크가 카메라를 다루는 로우 레벨 방식을 이해하게 됩니다.

기기가 네이티브 레이어에서 지원하는 세부 사양을 확인하려면, [Google Play](https://play.google.com/store/apps/details?id=com.minininja.cameraparams)에서 **Android Camera Parameters**를 사용해 보세요. 이 앱은 NDK를 통해서만 접근 가능한 일부 하드웨어 정보도 시각화해 줍니다.

---

## 왜 네이티브(C++)인가?

대부분의 앱은 Camera2(Java/Kotlin)로 충분합니다. 하지만 다음과 같은 경우에는 네이티브 스택이 필수적입니다.

1. **지터(Jitter) 없는 프레임 속도:** Java GC가 런타임에 실행되면 수 밀리초의 멈춤 현상이 발생할 수 있습니다. 60fps 환경에서 이는 치명적인 프레임 드롭으로 이어집니다. C++은 메모리 관리가 수동이므로 예측 가능한 타이밍을 보장합니다.
2. **AR / 컴퓨터 비전:** OpenCV, TensorFlow Lite C++, ARCore와 같은 라이브러리는 네이티브에서 작동합니다. 데이터를 Java로 올렸다가 다시 C++로 내리는 과정 없이 네이티브 레이어에서 바로 처리하는 것이 훨씬 빠릅니다.
3. **하드웨어 버퍼 직접 제어:** `AHardwareBuffer`를 통해 센서 데이터를 직접 GPU 메모리에 매핑할 수 있습니다. 이는 "복사 없는(Zero-copy)" 데이터 전달의 핵심입니다.

---

## NDK 카메라 아키텍처: ACamera API

네이티브 카메라 API는 `libcamera2ndk.so` 라이브러리에 포함되어 있으며, Java의 Camera2 구조를 거의 그대로 따라갑니다. 클래스 이름 앞에 `A`(Android)가 붙는 것이 특징입니다.

| Java (Camera2) | NDK (ACamera) | 역할 |
|---|---|---|
| `CameraManager` | `ACameraManager` | 카메라 기기 목록 조회 및 관리 |
| `CameraDevice` | `ACameraDevice` | 개별 카메라 장치 객체 |
| `CaptureRequest` | `ACaptureRequest` | 캡처 파라미터 설정 객체 |
| `CameraCaptureSession` | `ACameraCaptureSession` | 캡처 흐름 제어 세션 |
| `ImageReader` | `AImageReader` | 이미지 버퍼 수신 장치 |

### 1단계: 카메라 매니저 및 장치 열기

```cpp
// 매니저 인스턴스 생성
ACameraManager* cameraManager = ACameraManager_create();

// 카메라 ID 목록 조회
ACameraIdList* cameraIdList = nullptr;
ACameraManager_getCameraIdList(cameraManager, &cameraIdList);

// 첫 번째 카메라 열기
ACameraDevice* cameraDevice = nullptr;
ACameraDevice_StateCallbacks deviceCallbacks = {
    .context = this,
    .onDisconnected = onDeviceDisconnected,
    .onError = onDeviceError
};

ACameraManager_openCamera(cameraManager, cameraIdList->cameraIds[0], 
                         &deviceCallbacks, &cameraDevice);
```

### 2단계: 이미지 리더와 네이티브 서피스 준비

네이티브 카메라의 진가는 `AImageReader`에서 나옵니다. 여기서 받은 `AImage`로부터 `AHardwareBuffer`를 추출할 수 있습니다.

```cpp
AImageReader* reader = nullptr;
AImageReader_new(width, height, AIMAGE_FORMAT_YUV_420_888, 2, &reader);

ANativeWindow* surface = nullptr;
AImageReader_getWindow(reader, &surface);
```

이 `ANativeWindow`를 `ACaptureSession`의 출력 타겟으로 등록합니다.

---

## 제로 복사(Zero-copy) 파이프라인 구축

네이티브 개발의 꽃은 카메라 데이터를 메모리 복사 없이 GPU로 넘기는 것입니다.

1. **AImage 수신:** `onImageAvailable` 콜백이 호출됩니다.
2. **하드웨어 버퍼 추출:** `AImage_getHardwareBuffer(image, &buffer)`를 호출합니다.
3. **Vulkan/GL 바인딩:**
   - **OpenGL ES:** `eglCreateImageKHR`을 사용하여 버퍼를 EGL 이미지로 래핑하고, `glEGLImageTargetTexture2DOES`를 통해 텍스처로 바인딩합니다.
   - **Vulkan:** `VkExternalMemoryBufferCreateInfo`를 사용하여 버퍼를 직접 Vulkan 메모리로 가져옵니다.

이제 GPU의 쉐이더는 카메라 센서가 쓴 메모리 주소를 **직접** 읽어서 렌더링합니다. CPU 복사 부하가 0%에 가까워집니다.

---

## 구현 시 주의사항

1. **복잡성:** NDK 카메라는 Java API보다 훨씬 장황합니다. 사소한 설정 하나를 위해서도 수십 줄의 C++ 코드가 필요합니다.
2. **에러 처리:** Java처럼 예외(Exception)를 던지지 않습니다. 모든 함수는 `camera_status_t` 리턴 코드를 반환하므로, 매 호출마다 리턴값을 꼼꼼히 확인해야 합니다.
3. **수명 주기:** C++은 가비지 컬렉터가 없습니다. `ACameraManager_delete`, `ACameraDevice_close` 등을 적절한 시점에 직접 호출하지 않으면 앱이 종료되어도 카메라 하드웨어가 "점유 중" 상태로 남아 다른 앱이 카메라를 못 쓰는 대참사가 발생합니다.

---

## 요약

- **NDK ACamera API**는 고성능, 저지연 카메라 처리를 위한 최후의 수단입니다.
- **AHardwareBuffer**와 네이티브 서피스를 사용하여 Java 레이어를 건너뛰는 **제로 복사 파이프라인**을 구축할 수 있습니다.
- 게임 엔진, 실시간 영상 필터, AR 앱 개발자에게는 선택이 아닌 필수 기술입니다.

## 다음 단계

네이티브 레이어의 성능을 맛보았으니, 이제 코드를 더 깔끔하고 현대적으로 관리하는 법을 배울 차례입니다. **제26장: 비동기 카메라 프로그래밍 (Coroutine & Flow)**에서는 카메라의 복잡한 콜백 지옥을 Kotlin의 코루틴과 Flow를 사용하여 선언적이고 읽기 쉬운 코드로 리팩토링하는 방법을 알아봅니다.

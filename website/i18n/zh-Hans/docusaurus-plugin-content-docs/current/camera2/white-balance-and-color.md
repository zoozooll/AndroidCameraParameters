---
sidebar_position: 16
title: "第 16 章：白平衡与色彩"
description: 在 Android Camera2 中利用自动白平衡预设和手动色彩校正来控制色彩。学习 AWB 模式、色温 (2000K–10000K)、3×3 色彩变换、COLOR_CORRECTION_GAINS，以及用于暖色调日落预设和全手动白平衡的 Kotlin 代码。
keywords: [android camera2 白平衡, CONTROL_AWB_MODE, COLOR_CORRECTION_GAINS, COLOR_CORRECTION_TRANSFORM, 色温, 色彩校正矩阵, Rec.709 对比 DCI-P3 camera2]
---

# 第 16 章：白平衡与色彩

你已经掌握了亮度（曝光）和清晰度（对焦）。现在是时候控制图像的**观感**了——即色彩基调。

当你在一盏暖色白炽灯下拍摄一张白纸时，灯光的黄/橙色光线照射在纸上，传感器看到的就是橙色。*你的大脑*会立即纠正这一点，依然看到"白纸"——但原始传感器数据记录的是事实：它是橙色的。

**白平衡 (White Balance, WB)** 是相机补偿光源颜色的过程，从而使中性白色看起来真的是白色的。如果搞错了，整张照片都会带有不想要的色偏（太橙、太蓝或太绿）。

**Android Camera Parameters** 应用在实时网格视图中展示了每一个 AWB 预设，并提供了一个手动增益滑块——打开应用，切换到白平衡面板，你就可以观察到我们将在本章中实现的精确效果。

---

## 色温：从暖到冷的频谱

光源是用开尔文 (K) 为单位的**色温**来描述的。这个标度描述了一个理论上的"黑体辐射器"发出的光的颜色与其温度的关系。

```mermaid
graph LR
    A["1800K<br/>烛光"] --> B["2800K<br/>白炽灯"]
    B --> C[3500K<br/>暖色荧光灯]
    C --> D[4500K<br/>冷色荧光灯]
    D --> E[5500K<br/>日光 / 闪光灯]
    E --> F[6500K<br/>阴天]
    F --> G[8000K<br/>开阔阴影]
    G --> H[10000K+<br/>蓝天 / 深层阴影]
    style A fill:#e67e22,color:#fff
    style B fill:#f39c12,color:#fff
    style C fill:#f1c40f,color:#333
    style D fill:#f9e79f,color:#333
    style E fill:#ffffff,color:#333,stroke:#333
    style F fill:#d4e6f1,color:#333
    style G fill:#85c1e9,color:#333
    style H fill:#3498db,color:#fff
```

**反直觉的规则：** 暖色光 = *低*开尔文数值（1800K 烛光 = 非常橙）。冷色光 = *高*开尔文数值（10000K 天空 = 非常蓝）。你的眼睛在童年就学会了这一点；你的代码必须显式记住它。

| 场景 | 典型色温 | 若使用"日光"白平衡产生的偏色 |
|-------|-------------------|----------------------------|
| 烛光晚餐 | 1800–2200K | 非常橙 / 琥珀色 |
| 家用钨丝灯泡 | 2700–3000K | 橙色 / 黄色 |
| 日出 / 日落 | 3000–4000K | 暖金色调（通常很讨喜！） |
| "冷白色"荧光灯 | 4000–5000K | 带绿色的色调 |
| 正午阳光 | 5200–5800K | 正确的中性色 |
| 电子闪光灯 | 5500–6000K | 中性色（与日光匹配） |
| 阴天 / 浓云 | 6000–7500K | 略微偏蓝 |
| 开阔阴影（无直射阳光） | 7000–9000K | 蓝色偏色 |
| 朦胧蓝天 | 9000–12000K | 非常蓝 |

自动白平衡的工作：从场景统计数据中检测可能的光源，然后*减去*偏色，使中性物体呈现中性色。

---

## Camera2 中的自动白平衡 (AWB) 模式

通过 `CaptureRequest.CONTROL_AWB_MODE` 进行设置：

| 模式 (CONTROL_AWB_MODE_*) | 效果 | 用例 |
|---------------------------|--------|----------|
| `OFF` | 仅手动白平衡。显式使用 `COLOR_CORRECTION_GAINS` 或 `_TRANSFORM`。 | 专业模式、自定义调色、RAW + 后期 |
| `AUTO` | 默认。ISP 持续运行光源检测。 | 普通摄影 |
| `INCANDESCENT` (钨丝灯) | ~2800K。强蓝色增益以抵消温暖的钨丝灯光。 | 室内家用灯具、舞台灯光 |
| `FLUORESCENT` | ~4500K。针对典型办公荧光灯的增益（倾向于偏绿）。 | 办公室 / 教室 |
| `WARM_FLUORESCENT` | ~3200K。补偿暖白色荧光灯管。 | 家用节能灯 "暖白色" 模式 |
| `DAYLIGHT` | ~5500K。标准的正午阳光光源简况。 | 户外晴天，匹配闪光灯 |
| `CLOUDY_DAYLIGHT` | ~6500K。轻微变暖以抵消冷色调的阴天。 | 多云 / 朦胧天 |
| `TWILIGHT` | 暖色调的黄昏金色时刻简况 (~4500K)。 | 日落、黄昏、温暖的风景 |
| `SHADE` | ~7500K。强红色增益以抵消深蓝色的阴影光线。 | 阴影中的人像、城市背阴处 |

**首先查询支持的模式：** 并不是每台设备都提供全部 9 种预设。旗舰手机通常提供；而入门级设备可能仅提供 `AUTO` + `OFF`。

```kotlin
val availableAwbModes = characteristics.get(
    CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES
) ?: intArrayOf()
Log.d("AWB", "可用模式: ${availableAwbModes.toList()}")
```

### AWB 状态（类似于 AF，但没那么"啰嗦"）

AWB 状态机在概念上与 AF 类似，但更简单——它的状态较少：

| AWB 状态 | 含义 |
|-----------|---------|
| `CONTROL_AWB_STATE_INACTIVE` | AWB 已禁用 (`AWB_MODE = OFF`) |
| `CONTROL_AWB_STATE_SEARCHING` | 正在寻找正确的光源（色调可能会漂移） |
| `CONTROL_AWB_STATE_CONVERGED` | 找到了稳定的光源 — 色彩稳定 |
| `CONTROL_AWB_STATE_LOCKED` | 通过 `CONTROL_AWB_LOCK = true` 被显式锁定 |

对于对色彩要求极高的摄影（产品拍摄、目录工作），请使用与 AF 相同的"等待收敛 / 锁定后再拍摄"模式。

---

## 白平衡校正原理：底层机制

AWB 通过应用两次色彩变换来实现从传感器 RGB 到可显示的 sRGB 的转换。理解这些原理可以让你通过手动数值绕过 AWB。

### 第 1 步：通道增益（白点校正）

首先，将每个颜色通道乘以一个增益值，使中性表面呈现出相等的 R、G、B：

> 如果在 3200K 钨丝灯下的场景中，灰度目标产生的传感器输出为 `[R=200, G=150, B=100]`，则 AWB 应用大约为 `R: 1.0, G: 1.33, B: 2.0` 的通道增益来将其归一化为 `[200, 200, 200]`。

在 Camera2 中，这通过 **`CaptureRequest.COLOR_CORRECTION_GAINS`** 暴露：一个 4 元素的浮点数组，顺序为 **[R, Geven, B, Godd]**。

之所以有两个绿色通道 (`Geven`, `Godd`)，是因为许多智能手机传感器使用 2×2 拜耳阵列：**GR / BG** 交替行。以 Green-R 开始的行与以 Green-B 开始的行具有略微不同的光谱灵敏度，需要独立的数字增益。对于日常工作，将两个绿色设置为相同的值即可。

```kotlin
// COLOR_CORRECTION_GAINS = [ R 增益, G-even 增益, B 增益, G-odd 增益 ]
val warmGains = floatArrayOf(1.0f, 1.2f, 0.8f, 1.2f)  // 暖色调: 提升 R, 降低 B
val coolGains = floatArrayOf(0.85f, 1.0f, 1.25f, 1.0f) // 冷色调: 提升 B, 降低 R
val neutralGains = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f) // 单位增益 (原始传感器色彩)
```

**有效范围：** 增益通常由 HAL 限制在 [0.0, 4.0] 范围内。使用 0.5× 到 3× 之间的乘数即可获得合理的结果。

### 第 2 步：3×3 色彩变换矩阵（色域映射）

通道增益仅纠正了*白点*。但不同的传感器具有不同的原生色彩滤镜光谱响应，且不同的输出设备具有不同的显示色域（sRGB/Rec.709 对比 DCI-P3 对比 Display P3）。**3×3 色彩校正矩阵 (CCM)** 将传感器的原生 RGB 色彩空间映射到标准输出空间。

数学表示为：

```
[ R' ]   [ m11  m12  m13 ] [ R ]
[ G' ] = [ m21  m22  m23 ] [ G ]
[ B' ]   [ m31  m32  m33 ] [ B ]
```

代码表示为：`output = M × input`，其中 M 是一个 3×3 矩阵。

Camera2 通过 **`COLOR_CORRECTION_TRANSFORM`** 暴露了这一点，它使用一个 `Rational[9]` 数组进行设置（行优先：`m11, m12, m13, m21, m22, m23, m31, m32, m33`）。单位矩阵表示直接复制输入：

```kotlin
// 有理数表示的 3x3 单位矩阵：对角线为 1/1, 非对角线为 0/1
val identityMatrix = arrayOf(
    Rational(1,1), Rational(0,1), Rational(0,1),
    Rational(0,1), Rational(1,1), Rational(0,1),
    Rational(0,1), Rational(0,1), Rational(1,1)
)
```

**Rec.709 与 DCI-P3 色域：**

| 色彩空间 | 覆盖率 | 用例 |
|-------------|----------|----------|
| **Rec.709 (sRGB)** | ~35% 的可见光 | HDTV、网页、JPEG 默认值，2020 年前约 100% 的手机显示屏 |
| **DCI-P3** | ~45% 的可见光 | 数字影院、4K 超高清、现代 iPhone/Android 广色域显示屏 |

P3 显示屏比 Rec.709 能显示更丰富的红色和绿色。你的输出 CCM 必须挑选一个与观看者屏幕预期匹配的目标色域。在 Android 上，请检查 `Display.isWideColorGamut()` 并使用适当的矩阵。

**实践建议：** 除非你正在编写专业的 RAW 显影工具或经过色彩管理的电影应用，否则请设置 `COLOR_CORRECTION_MODE = TRANSFORM_MATRIX` 并让 OEM 的默认矩阵处理色域映射。大多数专业模式应用仅调整 `COLOR_CORRECTION_GAINS`（那 4 个增益），而保持矩阵不变。

---

## 完整示例 1：将 AWB 锁定为日光预设（暖色调锁定）

让我们从简单的开始。有时你不需要全手动——你只是想**防止 AWB 在帧与帧之间漂移**（例如：延时摄影、有场景变化的视频）。设置一个像 `DAYLIGHT` 这样的固定预设可以保证跨拍摄的色彩一致性。

这是最简单的手动色彩控制。

```kotlin
class AwbPresetController(
    private val characteristics: CameraCharacteristics,
    private val captureSession: CameraCaptureSession,
    private val previewSurface: Surface
) {
    // 如果 HAL 实际支持该模式，则返回 true
    fun isModeSupported(mode: Int): Boolean {
        val available = characteristics.get(
            CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES
        ) ?: intArrayOf()
        return mode in available
    }

    fun setPresetDaylightForWarmTintLock(): Boolean {
        if (!isModeSupported(CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT)) {
            Log.w("AWB", "此设备不支持 DAYLIGHT 预设")
            return false
        }

        val request = captureSession.device.createCaptureRequest(
            CameraDevice.TEMPLATE_PREVIEW
        ).apply {
            addTarget(previewSurface)

            // 将白平衡锁定为 DAYLIGHT (~5500K) 模式。
            // 这将使室内的钨丝灯场景呈现出刻意的暖色/橙色，
            // 这是电影摄影中偏爱的"电影感"效果。
            set(CaptureRequest.CONTROL_AWB_MODE,
                CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT)

            // 在本例中保持 AE 和 AF 为默认设置（自动）
            set(CaptureRequest.CONTROL_MODE,
                CameraMetadata.CONTROL_MODE_AUTO)
        }

        captureSession.setRepeatingRequest(request.build(),
            object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    val awbState = result.get(CaptureResult.CONTROL_AWB_STATE)
                    Log.d("AWB", "已应用 DAYLIGHT 预设, AWB 状态=$awbState")
                }
            }, null
        )
        return true
    }
}
```

**艺术应用：** 如果你使用 `AWB_MODE = DAYLIGHT` 拍摄日落，3000K 的日落光线在固定的 5500K 平衡下会显得*温暖*——从而产生浓郁、饱和的金橙色调。在这里使用 `AWB_MODE = AUTO` 将会*中和日落*（这正是要点！），因为它会注入更多蓝色来抵消金色的光线。预设可以保留氛围。

---

## 完整示例 2：全手动 AWB — 自定义暖色日落增益

为了获得终极的创意控制，请完全禁用 AWB 并写入你自己的增益。让我们建立一个"暖色日落观感"——略微提升红色，抑制蓝色，并带有微妙的绿色提升以避免出现紫色偏移。

```kotlin
class ManualColorGradingController(
    private val characteristics: CameraCharacteristics,
    private val captureSession: CameraCaptureSession,
    private val previewSurface: Surface,
    private val jpegReaderSurface: Surface
) {
    // 典型的调色预设 (R, Geven, B, Godd)
    object Presets {
        val NEUTRAL = floatArrayOf(1.00f, 1.00f, 1.00f, 1.00f)
        val WARM_SUNSET = floatArrayOf(1.00f, 1.20f, 0.80f, 1.20f)  // 暖琥珀色
        val COOL_MORNING = floatArrayOf(0.85f, 1.00f, 1.25f, 1.00f)  // 冷蓝色
        val VINTAGE_KODAK = floatArrayOf(1.15f, 1.00f, 0.85f, 1.00f) // 经典电影感
        val GREEN_SHIFT_FLUO = floatArrayOf(1.00f, 1.25f, 1.00f, 1.25f) // 荧光灯修正
    }

    fun applyManualGains(gains: FloatArray, includeMatrix: Boolean = true) {
        // 验证：必须支持 AWB_MODE = OFF (具备 MANUAL 性能时通常都支持)
        val hwLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
        val supportsManualColor = (hwLevel == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_3 ||
                                   hwLevel == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL ||
                                   isModeSupported(CameraMetadata.CONTROL_AWB_MODE_OFF))
        if (!supportsManualColor) {
            Log.e("AWB", "此 LEGACY 级别设备无法进行手动 AWB 增益调整")
            return
        }

        val request = captureSession.device.createCaptureRequest(
            CameraDevice.TEMPLATE_PREVIEW
        ).apply {
            addTarget(previewSurface)

            // 1) 完全禁用 AWB
            set(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_OFF)

            // 2) 应用 4 个通道的增益 (R, Geven, B, Godd)
            set(CaptureRequest.COLOR_CORRECTION_GAINS, gains)

            // 3) 选择色彩校正策略
            if (includeMatrix) {
                // FAST: 让 HAL 为此光源计算一个好的矩阵
                // (矩阵是自动推导的；仅增益受用户控制)
                set(CaptureRequest.COLOR_CORRECTION_MODE,
                    CameraMetadata.COLOR_CORRECTION_MODE_FAST)
            } else {
                // EXPERT: 同时手动设置我们自己的 3x3 变换矩阵 + 增益
                set(CaptureRequest.COLOR_CORRECTION_MODE,
                    CameraMetadata.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX)
                set(CaptureRequest.COLOR_CORRECTION_TRANSFORM, identityMatrix())
            }
        }

        captureSession.setRepeatingRequest(request.build(), null, null)
        Log.d("AWB", "已应用手动增益: [${gains.joinToString()}]")
    }

    // ------- 使用锁定的手动色彩进行静态拍摄 -------
    fun captureStillWithColorGrading(gains: FloatArray) {
        applyManualGains(gains)

        val stillRequest = captureSession.device.createCaptureRequest(
            CameraDevice.TEMPLATE_STILL_CAPTURE
        ).apply {
            addTarget(previewSurface)
            addTarget(jpegReaderSurface)

            set(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_OFF)
            set(CaptureRequest.COLOR_CORRECTION_GAINS, gains)
            set(CaptureRequest.COLOR_CORRECTION_MODE,
                CameraMetadata.COLOR_CORRECTION_MODE_FAST)

            set(CaptureRequest.JPEG_QUALITY, 95.toByte())
        }

        captureSession.capture(stillRequest.build(), null, null)
    }

    // ------- 辅助函数 -------
    private fun identityMatrix(): Array<Rational> = arrayOf(
        Rational(1,1), Rational(0,1), Rational(0,1),
        Rational(0,1), Rational(1,1), Rational(0,1),
        Rational(0,1), Rational(0,1), Rational(1,1)
    )

    private fun isModeSupported(mode: Int): Boolean {
        val available = characteristics.get(
            CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES
        ) ?: intArrayOf()
        return mode in available
    }
}
```

### 使用预设

```kotlin
// 用户点击"日落暖色"按钮
controller.applyManualGains(ManualColorGradingController.Presets.WARM_SUNSET)

// 用户点击"拍摄" — 相同的增益流向 JPEG
controller.captureStillWithColorGrading(ManualColorGradingController.Presets.WARM_SUNSET)
```

### COLOR_CORRECTION_MODE：FAST 对比 TRANSFORM_MATRIX

使用此决策表：

| 场景 | 选择 `COLOR_CORRECTION_MODE =` |
|----------|----------------------------------|
| 我只需要手动增益；让 OEM 选择矩阵（多数应用） | `FAST` |
| 我正在外部应用完整的调色 LUT / 矩阵，需要未经改动的原始色彩空间 | `TRANSFORM_MATRIX` + 单位矩阵 |
| 我有为此传感器专门推导的自定义色彩配置文件 (ICC / DCP) | `TRANSFORM_MATRIX` + 自定义 3x3 |

**警告：** 使用单位矩阵的 `TRANSFORM_MATRIX` 会给你**未经 OEM 色域映射的原始传感器色彩**。在许多传感器上，如果不进行额外处理，这看起来会明显偏灰（低饱和度）且略微带绿。这是正确行为——它是为你的自定义处理管线准备好的原始传感器输出。

---

## 手动开尔文转增益转换器（色温滑块）

专业相机应用（包括 **Android Camera Parameters**）会提供一个**开尔文温度滑块**。由于 Camera2 不直接接受开尔文数值，我们通过数学近似 R/B 增益曲线。

一个适用于大多数智能手机传感器的简单近似值（请根据你的目标硬件凭经验校准你的增益曲线）：

```kotlin
class KelvinGainsConverter {
    // 将开尔文 [2000..10000] 转换为近似增益 [R, Geven, B, Godd]
    // 简单的普朗克轨迹近似 (对于 UI 滑块足够用了)
    fun kelvinToRgbGains(kelvin: Int): FloatArray {
        val k = kelvin.coerceIn(2000, 10000)
        val temp = k / 100.0

        // 红色 (在低开尔文下偏暖)
        val r = when {
            temp <= 66 -> 255.0
            else -> {
                var x = temp - 60.0
                x = 329.698727446 * Math.pow(x, -0.1332047592)
                x.coerceIn(0.0, 255.0)
            }
        }

        // 绿色
        val g = when {
            temp <= 66 -> {
                var x = temp
                x = 99.4708025861 * Math.log(x) - 161.1195681661
                x.coerceIn(0.0, 255.0)
            }
            else -> {
                var x = temp - 60.0
                x = 288.1221695283 * Math.pow(x, -0.0755148492)
                x.coerceIn(0.0, 255.0)
            }
        }

        // 蓝色 (在高开尔文下偏冷)
        val b = when {
            temp >= 66 -> 255.0
            temp <= 19 -> 0.0
            else -> {
                var x = temp - 10.0
                x = 138.5177312231 * Math.log(x) - 305.0447927307
                x.coerceIn(0.0, 255.0)
            }
        }

        // 进行归一化，使 GREEN = 1.0, 然后取倒数: 我们需要的是补偿温度的增益。
        // 如果用户选择了 2800K (暖色), 我们需要更多的蓝色增益来抵消暖色调。
        // 此函数返回源 RGB; 增益为 1/R : 1/G : 1/B, 以 G=1 进行归一化
        val rGain = (g / r).toFloat().coerceIn(0.3f, 3.0f)
        val bGain = (g / b).toFloat().coerceIn(0.3f, 3.0f)
        return floatArrayOf(rGain, 1.0f, bGain, 1.0f)  // [R, Geven, B, Godd]
    }
}
```

配合 SeekBar 使用（2000–10000 K 范围）：

```kotlin
val converter = KelvinGainsConverter()
seekKelvin.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
    override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
        val kelvin = 2000 + progress * 8   // 0→2000K, 1000→10000K
        val gains = converter.kelvinToRgbGains(kelvin)
        textKelvinLabel.text = "$kelvin K"
        controller.applyManualGains(gains)
    }
    override fun onStartTrackingTouch(sb: SeekBar) = Unit
    override fun onStopTrackingTouch(sb: SeekBar) = Unit
})
```

**校准注记：** 这是一个通用的普朗克近似。为了获得完美结果，请在你的目标设备上运行 Macbeth ColorChecker 或白点校准，然后将曲线拟合到实测的 R/B 增益比与真实开尔文的关系上。**Android Camera Parameters** 应用使用了通过 `SENSOR_CALIBRATION_TRANSFORM1`（若可用）从 HAL 加载的每设备校准数据。

---

## 色彩问题排查

| 症状 | 原因 | 解决办法 |
|---------|-------|-----|
| 设置了手动增益但色彩没有变化 | 忘记设置 `CONTROL_AWB_MODE = OFF` → AWB 仍在覆盖增益 | 必须在设置 GAINS/TRANSFORM *之前*设置 AWB_MODE = OFF |
| COLOR_CORRECTION_TRANSFORM 被忽略 | 模式仍为 `FAST`; 该键仅在 `TRANSFORM_MATRIX` 模式下被尊重 | 先设置 `COLOR_CORRECTION_MODE = TRANSFORM_MATRIX` |
| 延时摄影帧之间 AWB 漂移（出现绿色/紫色偏色闪烁） | AWB 仍处于 AUTO 状态且每帧都在重新评估 | 为延时摄影设置固定的 AWB_MODE 预设或全手动增益 |
| JPEG 色彩与预览不同 | JPEG 应用的模式/增益与最后的重复请求不同 | 将相同的增益应用到 TEMPLATE_PREVIEW 和 TEMPLATE_STILL_CAPTURE 构建器中 |
| LEGACY 级别设备：手动增益崩溃 | INFO_SUPPORTED_HARDWARE_LEVEL = LEGACY (不支持手动色彩) | 优雅回退；仅展示 AUTO + 预设 UI |

---

## 小结

Camera2 中的白平衡与色彩校正为你提供了手动控制三部曲的最后一块拼图：

- **色温 (K)：** 低 K (1800K 烛光) = 暖/橙色；高 K (10000K 阴影) = 冷/蓝色。AWB 进行补偿以中和光源颜色。
- **AWB 模式：** 9 种预设 (`INCANDESCENT` → `SHADE`) + `AUTO` + `OFF`。使用前请查询 `CONTROL_AWB_AVAILABLE_MODES`。
- **AWB 状态：** `SEARCHING → CONVERGED → LOCKED`。在色彩关键的序列中请等待 CONVERGED/LOCKED。
- **手动控制有两个层级：**
  1. `COLOR_CORRECTION_GAINS` = 4 元素浮点数组 `[R, Geven, B, Godd]` — 白点校正。使用 `COLOR_CORRECTION_MODE = FAST`（OEM 矩阵, 自定义增益）。
  2. `COLOR_CORRECTION_TRANSFORM` = 3×3 `Rational[9]` 矩阵 — 完整的色域映射。在 `TRANSFORM_MATRIX` 模式下用于单位矩阵或自定义 CCM。
- **Rec.709 对比 DCI-P3：** 3×3 矩阵将传感器色彩空间映射到显示目标色域。
- **开尔文滑块：** 通过普朗克轨迹数学近似开尔文→增益，在 AWB 为 OFF 时应用。

## 下一章

你现在已经分别理解了**曝光、对焦和白平衡**。在**第 17 章：3A 管线**中，我们将最终把这三者编排在一起，作为一个内聚的静态照片捕获序列：

- 完整的 `AF 触发 → AF 锁定 → AE 预捕获 → AE 闪光灯收敛 → 拍摄照片` 流程
- AE 闪光灯模式 (`ON_AUTO_FLASH`, `ON_ALWAYS_FLASH`, `ON_AUTO_FLASH_REDEYE`)
- AE 状态和预捕获触发序列
- 与 AE+AF 协调的 AWB 状态
- 一个实现整个 3A 编排的生产级 Kotlin 类，并配有 Mermaid 序列图
- 参考 3A 控制管线研究

这一章将把所有内容串联成一个可以工作的专业相机应用。千万不要错过。

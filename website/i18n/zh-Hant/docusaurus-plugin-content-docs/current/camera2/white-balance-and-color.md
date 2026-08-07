---
sidebar_position: 16
title: "第 16 章：白平衡與色彩"
description: 在 Android Camera2 中利用自動白平衡預設和手動色彩校正來控制色彩。學習 AWB 模式、色溫 (2000K–10000K)、3×3 色彩變換、COLOR_CORRECTION_GAINS，以及用於暖色調日落預設和全手動白平衡的 Kotlin 程式碼。
keywords: [android camera2 白平衡, CONTROL_AWB_MODE, COLOR_CORRECTION_GAINS, COLOR_CORRECTION_TRANSFORM, 色溫, 色彩校正矩陣, Rec.709 對比 DCI-P3 camera2]
---

# 第 16 章：白平衡與色彩

你已經掌握了亮度（曝光）和清晰度（對焦）。現在是時候控制影像的**觀感**了——即色彩基調。

當你在一盞暖色白熾燈下拍攝一張白紙時，燈光的黃/橙色光線照射在紙上，感光元件看到的就是橙色。*你的大腦*會立即糾正這一點，依然看到「白紙」——但原始感光元件數據記錄的是事實：它是橙色的。

**白平衡 (White Balance, WB)** 是相機補償光源顏色的過程，從而使中性白色看起來真的是白色的。如果搞錯了，整張照片都會帶有不想要的色偏（太橙、太藍或太綠）。

**Android Camera Parameters** 應用在即時網格視圖中展示了每一个 AWB 預設，並提供了一个手動增益滑塊——打開應用，切換到白平衡面板，你就可以觀察到我們將在本章中實現的精確效果。

---

## 色溫：從暖到冷的頻譜

光源是用開爾文 (K) 為單位的**色溫**來描述的。這個標度描述了一个理論上的「黑體輻射器」發出的光的顏色與其溫度的關係。

```mermaid
graph LR
    A["1800K<br/>燭光"] --> B["2800K<br/>白熾燈"]
    B --> C[3500K<br/>暖色螢光燈]
    C --> D[4500K<br/>冷色螢光燈]
    D --> E[5500K<br/>日光 / 閃光燈]
    E --> F[6500K<br/>陰天]
    F --> G[8000K<br/>開闊陰影]
    G --> H[10000K+<br/>藍天 / 深層陰影]
    style A fill:#e67e22,color:#fff
    style B fill:#f39c12,color:#fff
    style C fill:#f1c40f,color:#333
    style D fill:#f9e79f,color:#333
    style E fill:#ffffff,color:#333,stroke:#333
    style F fill:#d4e6f1,color:#333
    style G fill:#85c1e9,color:#333
    style H fill:#3498db,color:#fff
```

**反直覺的規則：** 暖色光 = *低*開爾文數值（1800K 燭光 = 非常橙）。冷色光 = *高*開爾文數值（10000K 天空 = 非常藍）。你的眼睛在童年就學會了這一點；你的程式碼必須顯式記住它。

| 場景 | 典型色溫 | 若使用「日光」白平衡產生的偏色 |
|-------|-------------------|----------------------------|
| 燭光晚餐 | 1800–2200K | 非常橙 / 琥珀色 |
| 家用鎢絲燈泡 | 2700–3000K | 橙色 / 黃色 |
| 日出 / 日落 | 3000–4000K | 暖金色調（通常很討喜！） |
| 「冷白色」螢光燈 | 4000–5000K | 帶綠色的色調 |
| 正午陽光 | 5200–5800K | 正確的中性色 |
| 電子閃光燈 | 5500–6000K | 中性色（與日光比對） |
| 陰天 / 濃雲 | 6000–7500K | 略微偏藍 |
| 開闊陰影（無直射陽光） | 7000–9000K | 藍色偏色 |
| 朦朧藍天 | 9000–12000K | 非常藍 |

自動白平衡的工作：從場景統計數據中偵測可能的光源，然後*減去*偏色，使中性物體呈現中性色。

---

## Camera2 中的自動白平衡 (AWB) 模式

透過 `CaptureRequest.CONTROL_AWB_MODE` 進行設定：

| 模式 (CONTROL_AWB_MODE_*) | 效果 | 用例 |
|---------------------------|--------|----------|
| `OFF` | 僅手動白平衡。顯式使用 `COLOR_CORRECTION_GAINS` 或 `_TRANSFORM`。 | 專業模式、自定義調色、RAW + 後期 |
| `AUTO` | 預設。ISP 持續執行光源偵測。 | 普通攝影 |
| `INCANDESCENT` (鎢絲燈) | ~2800K。強藍色增益以抵消溫暖的鎢絲燈光。 | 室內家用燈具、舞台燈光 |
| `FLUORESCENT` | ~4500K。針對典型辦公螢光燈的增益（傾向於偏綠）。 | 辦公室 / 教室 |
| `WARM_FLUORESCENT` | ~3200K。補償暖白色螢光燈管。 | 家用節能燈 「暖白色」 模式 |
| `DAYLIGHT` | ~5500K。標準的正午陽光光源簡報。 | 戶外晴天，比對閃光燈 |
| `CLOUDY_DAYLIGHT` | ~6500K。輕微變暖以抵消冷色調的陰天。 | 多雲 / 朦朧天 |
| `TWILIGHT` | 暖色調的黃昏金色時刻簡報 (~4500K)。 | 日落、黃昏、溫暖的風景 |
| `SHADE` | ~7500K。強紅色增益以抵消深藍色的陰影光線。 | 陰影中的人像、城市背陰處 |

**首先查詢支持的模式：** 并不是每台設備都提供全部 9 種預設。旗艦手機通常提供；而入門級設備可能僅提供 `AUTO` + `OFF`。

```kotlin
val availableAwbModes = characteristics.get(
    CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES
) ?: intArrayOf()
Log.d("AWB", "可用模式: ${availableAwbModes.toList()}")
```

### AWB 狀態（類似於 AF，但沒那麼「囉唆」）

AWB 狀態機在概念上與 AF 類似，但更簡單——它的狀態較少：

| AWB 狀態 | 含義 |
|-----------|---------|
| `CONTROL_AWB_STATE_INACTIVE` | AWB 已禁用 (`AWB_MODE = OFF`) |
| `CONTROL_AWB_STATE_SEARCHING` | 正在尋找正確的光源（色調可能會漂移） |
| `CONTROL_AWB_STATE_CONVERGED` | 找到了穩定的光源 — 色彩穩定 |
| `CONTROL_AWB_STATE_LOCKED` | 透過 `CONTROL_AWB_LOCK = true` 被顯式鎖定 |

對於對色彩要求極高的攝影（產品拍攝、目錄工作），請使用與 AF 相同的「等待收斂 / 鎖定後再拍攝」模式。

---

## 白平衡校正原理：底層機制

AWB 透過應用兩次色彩變換來實現從感光元件 RGB 到可顯示的 sRGB 的轉換。理解這些原理可以讓你透過手動數值繞過 AWB。

### 第 1 步：通道增益（白點校正）

首先，將每個顏色通道乘以一個增益值，使中性表面呈現出相等的 R、G、B：

> 如果在 3200K 鎢絲燈下的場景中，灰度目標產生的感光元件輸出為 `[R=200, G=150, B=100]`，則 AWB 應用大約為 `R: 1.0, G: 1.33, B: 2.0` 的通道增益來將其歸一化為 `[200, 200, 200]`。

在 Camera2 中，這透過 **`CaptureRequest.COLOR_CORRECTION_GAINS`** 暴露：一个 4 元素的浮點陣列，順序為 **[R, Geven, B, Godd]**。

之所以有兩個綠色通道 (`Geven`, `Godd`)，是因為許多智慧型手機感光元件使用 2×2 拜耳陣列：**GR / BG** 交替行。以 Green-R 開始的行與以 Green-B 開始的行具有略微不同的光譜靈敏度，需要獨立的數位增益。對於日常工作，將兩個綠色設定為相同的值即可。

```kotlin
// COLOR_CORRECTION_GAINS = [ R 增益, G-even 增益, B 增益, G-odd 增益 ]
val warmGains = floatArrayOf(1.0f, 1.2f, 0.8f, 1.2f)  // 暖色調: 提升 R, 降低 B
val coolGains = floatArrayOf(0.85f, 1.0f, 1.25f, 1.0f) // 冷色調: 提升 B, 降低 R
val neutralGains = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f) // 單位增益 (原始感光元件色彩)
```

**有效範圍：** 增益通常由 HAL 限制在 [0.0, 4.0] 範圍內。使用 0.5× 到 3× 之間的乘數即可獲得合理的結果。

### 第 2 步：3×3 色彩變換矩陣（色域映射）

通道增益僅糾正了*白點*。但不同的感光元件具有不同的原生色彩濾波器光譜響應，且不同的輸出設備具有不同的顯示色域（sRGB/Rec.709 對比 DCI-P3 對比 Display P3）。**3×3 色彩校正矩陣 (CCM)** 將感光元件的原生 RGB 色彩空間映射到標準輸出空間。

數學表示為：

```
[ R' ]   [ m11  m12  m13 ] [ R ]
[ G' ] = [ m21  m22  m23 ] [ G ]
[ B' ]   [ m31  m32  m33 ] [ B ]
```

程式碼表示為：`output = M × input`，其中 M 是一個 3×3 矩陣。

Camera2 透過 **`COLOR_CORRECTION_TRANSFORM`** 暴露了這一點，它使用一个 `Rational[9]` 陣列進行設定（行優先：`m11, m12, m13, m21, m22, m23, m31, m32, m33`）。單位矩陣表示直接複製輸入：

```kotlin
// 有理數表示的 3x3 單位矩陣：對角線為 1/1, 非對角線為 0/1
val identityMatrix = arrayOf(
    Rational(1,1), Rational(0,1), Rational(0,1),
    Rational(0,1), Rational(1,1), Rational(0,1),
    Rational(0,1), Rational(0,1), Rational(1,1)
)
```

**Rec.709 與 DCI-P3 色域：**

| 色彩空間 | 覆蓋率 | 用例 |
|-------------|----------|----------|
| **Rec.709 (sRGB)** | ~35% 的可見光 | HDTV、網頁、JPEG 預設值，2020 年前約 100% 的手機顯示器 |
| **DCI-P3** | ~45% 的可見光 | 數位影院、4K 超高清、現代 iPhone/Android 廣色域顯示器 |

P3 顯示器比 Rec.709 能顯示更豐富的紅色和綠色。你的輸出 CCM 必須挑選一个與觀看者螢幕預期比對的目標色域。在 Android 上，請檢查 `Display.isWideColorGamut()` 並使用適當的矩陣。

**實踐建議：** 除非你正在編寫專業的 RAW 顯影工具或經過色彩管理的電影應用，否則請設定 `COLOR_CORRECTION_MODE = TRANSFORM_MATRIX` 並讓 OEM 的預設矩陣處理色域映射。大多數專業模式應用僅調整 `COLOR_CORRECTION_GAINS`（那 4 個增益），而保持矩陣不變。

---

## 完整範例 1：將 AWB 鎖定為日光預設（暖色調鎖定）

讓我們從簡單的開始。有時你不需要全手動——你只是想**防止 AWB 在幀與幀之間漂移**（例如：延時攝影、有場景變化的影片）。設定一个像 `DAYLIGHT` 這樣的固定預設可以保證跨拍攝的色彩一致性。

這是最簡單的手動色彩控制。

```kotlin
class AwbPresetController(
    private val characteristics: CameraCharacteristics,
    private val captureSession: CameraCaptureSession,
    private val previewSurface: Surface
) {
    // 如果 HAL 實際支持該模式，則返回 true
    fun isModeSupported(mode: Int): Boolean {
        val available = characteristics.get(
            CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES
        ) ?: intArrayOf()
        return mode in available
    }

    fun setPresetDaylightForWarmTintLock(): Boolean {
        if (!isModeSupported(CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT)) {
            Log.w("AWB", "此設備不支援 DAYLIGHT 預設")
            return false
        }

        val request = captureSession.device.createCaptureRequest(
            CameraDevice.TEMPLATE_PREVIEW
        ).apply {
            addTarget(previewSurface)

            // 將白平衡鎖定為 DAYLIGHT (~5500K) 模式。
            // 這將使室內的鎢絲燈場景呈現出刻意的暖色/橙色，
            // 這是電影攝影中偏愛的「電影感」效果。
            set(CaptureRequest.CONTROL_AWB_MODE,
                CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT)

            // 在本例中保持 AE 和 AF 為預設設定（自動）
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
                    Log.d("AWB", "已應用 DAYLIGHT 預設, AWB 狀態=$awbState")
                }
            }, null
        )
        return true
    }
}
```

**藝術應用：** 如果你使用 `AWB_MODE = DAYLIGHT` 拍攝日落，3000K 的日落光線在固定的 5500K 平衡下會顯得*溫暖*——從而產生濃郁、飽和的金橙色調。在這裡使用 `AWB_MODE = AUTO` 將會*中和日落*（這正是要點！），因為它會注入更多藍色來抵消金色的光線。預設可以保留氛圍。

---

## 完整範例 2：全手動 AWB — 自定義暖色日落增益

為了獲得終極的創意控制，請完全禁用 AWB 並寫入你自己的增益。讓我們建立一个「暖色日落觀感」——略微提升紅色，抑制藍色，並帶有微妙的綠色提升以避免出現紫色偏移。

```kotlin
class ManualColorGradingController(
    private val characteristics: CameraCharacteristics,
    private val captureSession: CameraCaptureSession,
    private val previewSurface: Surface,
    private val jpegReaderSurface: Surface
) {
    // 典型的調色預設 (R, Geven, B, Godd)
    object Presets {
        val NEUTRAL = floatArrayOf(1.00f, 1.00f, 1.00f, 1.00f)
        val WARM_SUNSET = floatArrayOf(1.00f, 1.20f, 0.80f, 1.20f)  // 暖琥珀色
        val COOL_MORNING = floatArrayOf(0.85f, 1.00f, 1.25f, 1.00f)  // 冷藍色
        val VINTAGE_KODAK = floatArrayOf(1.15f, 1.00f, 0.85f, 1.00f) // 經典電影感
        val GREEN_SHIFT_FLUO = floatArrayOf(1.00f, 1.25f, 1.00f, 1.25f) // 螢光燈修正
    }

    fun applyManualGains(gains: FloatArray, includeMatrix: Boolean = true) {
        // 驗證：必須支持 AWB_MODE = OFF (具備 MANUAL 性能時通常都支持)
        val hwLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
        val supportsManualColor = (hwLevel == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_3 ||
                                   hwLevel == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL ||
                                   isModeSupported(CameraMetadata.CONTROL_AWB_MODE_OFF))
        if (!supportsManualColor) {
            Log.e("AWB", "此 LEGACY 層級設備無法進行手動 AWB 增益調整")
            return
        }

        val request = captureSession.device.createCaptureRequest(
            CameraDevice.TEMPLATE_PREVIEW
        ).apply {
            addTarget(previewSurface)

            // 1) 完全禁用 AWB
            set(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_OFF)

            // 2) 應用 4 個通道的增益 (R, Geven, B, Godd)
            set(CaptureRequest.COLOR_CORRECTION_GAINS, gains)

            // 3) 選擇色彩校正策略
            if (includeMatrix) {
                // FAST: 讓 HAL 為此光源計算一个好的矩陣
                // (矩陣是自動推導的；僅增益受用戶控制)
                set(CaptureRequest.COLOR_CORRECTION_MODE,
                    CameraMetadata.COLOR_CORRECTION_MODE_FAST)
            } else {
                // EXPERT: 同時手動設定我們自己的 3x3 變換矩陣 + 增益
                set(CaptureRequest.COLOR_CORRECTION_MODE,
                    CameraMetadata.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX)
                set(CaptureRequest.COLOR_CORRECTION_TRANSFORM, identityMatrix())
            }
        }

        captureSession.setRepeatingRequest(request.build(), null, null)
        Log.d("AWB", "已應用手動增益: [${gains.joinToString()}]")
    }

    // ------- 使用鎖定的手動色彩進行靜態拍攝 -------
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

    // ------- 輔助函式 -------
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

### 使用預設

```kotlin
// 用戶點擊「日落暖色」按鈕
controller.applyManualGains(ManualColorGradingController.Presets.WARM_SUNSET)

// 用戶點擊「拍攝」 — 相同的增益流向 JPEG
controller.captureStillWithColorGrading(ManualColorGradingController.Presets.WARM_SUNSET)
```

### COLOR_CORRECTION_MODE：FAST 對比 TRANSFORM_MATRIX

使用此決策表：

| 場景 | 選擇 `COLOR_CORRECTION_MODE =` |
|----------|----------------------------------|
| 我只需要手動增益；讓 OEM 選擇矩陣（多數應用） | `FAST` |
| 我正在外部應用完整的調色 LUT / 矩陣，需要未經改動的原始色彩空間 | `TRANSFORM_MATRIX` + 單位矩陣 |
| 我有為此感光元件專門推導的自定義色彩設定檔 (ICC / DCP) | `TRANSFORM_MATRIX` + 自定義 3x3 |

**警告：** 使用單位矩陣的 `TRANSFORM_MATRIX` 會給你**未經 OEM 色域映射的原始感光元件色彩**。在許多感光元件上，如果不進行額外處理，這看起來會明顯偏灰（低飽和度）且略微帶綠。這是正確行為——它是為你的自定義處理管線準備好的原始感光元件輸出。

---

## 手動開爾文轉增益轉換器（色溫滑塊）

專業相機應用（包括 **Android Camera Parameters**）會提供一个**開爾文溫度滑塊**。由於 Camera2 不直接接受開爾文數值，我們透過數學近似 R/B 增益曲線。

一个適用於大多數智慧型手機感光元件的簡單近似值（請根據你的目標硬體憑經驗校準你的增益曲線）：

```kotlin
class KelvinGainsConverter {
    // 將開爾文 [2000..10000] 轉換為近似增益 [R, Geven, B, Godd]
    // 簡單的普朗克軌跡近似 (對於 UI 滑塊足夠用了)
    fun kelvinToRgbGains(kelvin: Int): FloatArray {
        val k = kelvin.coerceIn(2000, 10000)
        val temp = k / 100.0

        // 紅色 (在低開爾文下偏暖)
        val r = when {
            temp <= 66 -> 255.0
            else -> {
                var x = temp - 60.0
                x = 329.698727446 * Math.pow(x, -0.1332047592)
                x.coerceIn(0.0, 255.0)
            }
        }

        // 綠色
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

        // 藍色 (在高開爾文下偏冷)
        val b = when {
            temp >= 66 -> 255.0
            temp <= 19 -> 0.0
            else -> {
                var x = temp - 10.0
                x = 138.5177312231 * Math.log(x) - 305.0447927307
                x.coerceIn(0.0, 255.0)
            }
        }

        // 進行歸一化，使 GREEN = 1.0, 然後取倒數: 我們需要的是補償溫度的增益。
        // 如果用戶選擇了 2800K (暖色), 我們需要更多的藍色增益來抵消暖色調。
        // 此函式返回源 RGB; 增益為 1/R : 1/G : 1/B, 以 G=1 進行歸一化
        val rGain = (g / r).toFloat().coerceIn(0.3f, 3.0f)
        val bGain = (g / b).toFloat().coerceIn(0.3f, 3.0f)
        return floatArrayOf(rGain, 1.0f, bGain, 1.0f)  // [R, Geven, B, Godd]
    }
}
```

配合 SeekBar 使用（2000–10000 K 範圍）：

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

**校準註記：** 這是一个通用的普朗克近似。為了獲得完美結果，請在你的目標設備上執行 Macbeth ColorChecker 或白點校準，然後將曲線擬合到實測的 R/B 增益比與真實開爾文的關係上。**Android Camera Parameters** 應用使用了透過 `SENSOR_CALIBRATION_TRANSFORM1`（若可用）從 HAL 加載的每設備校準數據。

---

## 色彩問題排查

| 症狀 | 原因 | 解決辦法 |
|---------|-------|-----|
| 設定了手動增益但色彩没有變化 | 忘記設定 `CONTROL_AWB_MODE = OFF` → AWB 仍在覆蓋增益 | 必須在設定 GAINS/TRANSFORM *之前*設定 AWB_MODE = OFF |
| COLOR_CORRECTION_TRANSFORM 被忽略 | 模式仍為 `FAST`; 該鍵僅在 `TRANSFORM_MATRIX` 模式下被尊重 | 先設定 `COLOR_CORRECTION_MODE = TRANSFORM_MATRIX` |
| 延時攝影幀之間 AWB 漂移（出現綠色/紫色偏色閃爍） | AWB 仍處於 AUTO 狀態且每幀都在重新評估 | 為延時攝影設定固定的 AWB_MODE 預設或全手動增益 |
| JPEG 色彩與預覽不同 | JPEG 應用的模式/增益與最後的重複請求不同 | 將相同的增益應用到 TEMPLATE_PREVIEW 和 TEMPLATE_STILL_CAPTURE 建構器中 |
| LEGACY 層級設備：手動增益崩潰 | INFO_SUPPORTED_HARDWARE_LEVEL = LEGACY (不支援手動色彩) | 優雅回退；僅展示 AUTO + 預設 UI |

---

## 小結

Camera2 中的白平衡與色彩校正為你提供了手動控制三部曲的最後一塊拼圖：

- **色溫 (K)：** 低 K (1800K 燭光) = 暖/橙色；高 K (10000K 陰影) = 冷/藍色。AWB 進行補償以中和光源顏色。
- **AWB 模式：** 9 種預設 (`INCANDESCENT` → `SHADE`) + `AUTO` + `OFF`。使用前請查詢 `CONTROL_AWB_AVAILABLE_MODES`。
- **AWB 狀態：** `SEARCHING → CONVERGED → LOCKED`。在色彩關鍵的序列中請等待 CONVERGED/LOCKED。
- **手動控制有兩個層級：**
  1. `COLOR_CORRECTION_GAINS` = 4 元素浮點陣列 `[R, Geven, B, Godd]` — 白點校正。使用 `COLOR_CORRECTION_MODE = FAST`（OEM 矩陣, 自定義增益）。
  2. `COLOR_CORRECTION_TRANSFORM` = 3×3 `Rational[9]` 矩陣 — 完整的色域映射。在 `TRANSFORM_MATRIX` 模式下用於單位矩陣或自定義 CCM。
- **Rec.709 對比 DCI-P3：** 3×3 矩陣將感光元件色彩空間映射到顯示目標色域。
- **開爾文滑塊：** 透過普朗克軌跡數學近似開爾文→增益，在 AWB 為 OFF 時應用。

## 下一章

你現在已經分別理解了**曝光、對焦和白平衡**。在**第 17 章：3A 管線**中，我們將最終把這三者編排在一起，作为一个內聚的靜態照片擷取序列：

- 完整的 `AF 觸發 → AF 鎖定 → AE 預擷取 → AE 閃光燈收斂 → 拍攝照片` 流程
- AE 閃光燈模式 (`ON_AUTO_FLASH`, `ON_ALWAYS_FLASH`, `ON_AUTO_FLASH_REDEYE`)
- AE 狀態和預擷取觸發序列
- 與 AE+AF 協調的 AWB 狀態
- 一个實現整個 3A 編排的生產級 Kotlin 類別，並配有 Mermaid 序列圖
- 參考 3A 控制管線研究

這一章將把所有內容串聯成一个可以工作的專業相機應用。千萬不要錯過。

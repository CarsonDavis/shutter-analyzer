# Camera Focus & Zoom Implementation Plan

## Research Findings

### Why Our Focus Implementation Doesn't Work

**Root Cause:** CameraX internally overrides AF/AE/AWB (3A) settings regardless of what you set via `Camera2CameraControl.setCaptureRequestOptions()`. This is a documented limitation.

From Google's CameraX team:
> "Currently CameraX will override AF/AE/AWB mode regardless what modes you set in camera2-interop. Manual focus should be supported in future plan."

This explains why **zoom works but focus doesn't**:
- `SCALER_CROP_REGION` is NOT part of 3A management → CameraX leaves it alone
- `CONTROL_AF_MODE` and `LENS_FOCUS_DISTANCE` ARE part of 3A → CameraX overrides them

### The Correct Approach

**Use `Camera2Interop.Extender` when BUILDING the UseCase (before binding)**, not `Camera2CameraControl` after binding.

```kotlin
// WRONG - Settings get overridden by CameraX
val camera2Control = Camera2CameraControl.from(camera.cameraControl)
camera2Control.setCaptureRequestOptions(
    CaptureRequestOptions.Builder()
        .setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CONTROL_AF_MODE_OFF)
        .build()
)

// CORRECT - Settings baked into UseCase before CameraX takes over
val previewBuilder = Preview.Builder()
Camera2Interop.Extender(previewBuilder)
    .setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CONTROL_AF_MODE_OFF)
    .setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, 0f)
val preview = previewBuilder.build()
```

### Key Constraints

1. **Must apply to ALL use cases** - Preview, VideoCapture, and ImageAnalysis all need identical Camera2Interop.Extender settings

2. **Toggling autofocus requires rebinding** - Cannot reliably switch between manual/auto by just changing settings dynamically; must unbind all and rebind with different UseCase configurations

3. **Dynamic focus distance changes work** - Once AF_MODE=OFF is baked in, you CAN change LENS_FOCUS_DISTANCE dynamically via Camera2CameraControl

4. **For zoom, use CameraX native APIs** - `setLinearZoom()` or `setZoomRatio()` work fine; our manual SCALER_CROP_REGION approach adds unnecessary complexity

---

## Implementation Plan

### Phase 1: Restructure Camera Binding

**Goal:** Support two camera modes - Autofocus and Manual Focus - with proper Camera2Interop setup.

#### Changes to `ShutterCameraManager.kt`

1. **Add focus mode enum:**
```kotlin
enum class FocusMode {
    AUTO,
    MANUAL
}
```

2. **Store current focus mode and lifecycle owner:**
```kotlin
private var currentFocusMode: FocusMode = FocusMode.AUTO
private var currentLifecycleOwner: LifecycleOwner? = null
private var currentPreviewView: PreviewView? = null
```

3. **Refactor `bindPreview()` to accept focus mode:**
```kotlin
@OptIn(ExperimentalCamera2Interop::class)
fun bindPreview(
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    focusMode: FocusMode = FocusMode.AUTO
) {
    // Store for rebinding
    currentLifecycleOwner = lifecycleOwner
    currentPreviewView = previewView
    currentFocusMode = focusMode

    val provider = cameraProvider ?: return
    provider.unbindAll()

    // Build Preview with Camera2 settings
    val previewBuilder = Preview.Builder()
    applyFocusSettings(previewBuilder, focusMode)
    preview = previewBuilder.build().also {
        it.setSurfaceProvider(previewView.surfaceProvider)
    }

    // Build ImageAnalysis with same settings
    val analysisBuilder = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
    applyFocusSettings(analysisBuilder, focusMode)
    imageAnalysis = analysisBuilder.build().also {
        it.setAnalyzer(cameraExecutor, frameAnalyzer)
    }

    // Build VideoCapture with same settings
    val recorderBuilder = Recorder.Builder()
        .setQualitySelector(...)
    // Note: VideoCapture.Builder doesn't support Camera2Interop directly
    // May need to use Camera2Interop on the underlying recorder
    videoCapture = VideoCapture.withOutput(recorderBuilder.build())

    // Bind all
    camera = provider.bindToLifecycle(...)

    // Get focus distance range
    updateFocusCapabilities()
}

@OptIn(ExperimentalCamera2Interop::class)
private fun <T> applyFocusSettings(builder: T, focusMode: FocusMode) {
    val extender = when (builder) {
        is Preview.Builder -> Camera2Interop.Extender(builder)
        is ImageAnalysis.Builder -> Camera2Interop.Extender(builder)
        else -> return
    }

    when (focusMode) {
        FocusMode.AUTO -> {
            extender.setCaptureRequestOption(
                CaptureRequest.CONTROL_AF_MODE,
                CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO
            )
        }
        FocusMode.MANUAL -> {
            extender.setCaptureRequestOption(
                CaptureRequest.CONTROL_AF_MODE,
                CameraMetadata.CONTROL_AF_MODE_OFF
            )
            extender.setCaptureRequestOption(
                CaptureRequest.LENS_FOCUS_DISTANCE,
                0f // Start at infinity
            )
        }
    }
}
```

4. **Add rebind method for focus mode toggle:**
```kotlin
fun setFocusMode(mode: FocusMode) {
    if (mode == currentFocusMode) return

    val lifecycleOwner = currentLifecycleOwner ?: return
    val previewView = currentPreviewView ?: return

    // Rebind with new focus mode
    bindPreview(lifecycleOwner, previewView, mode)
}
```

5. **Simplify focus distance setting (now it will work!):**
```kotlin
@OptIn(ExperimentalCamera2Interop::class)
fun setManualFocus(distance: Float) {
    if (currentFocusMode != FocusMode.MANUAL) return

    _focusDistance.value = distance.coerceIn(0f, 1f)

    val cam = camera ?: return
    val effectiveMinFocus = if (minFocusDistance > 0f) minFocusDistance else 10f
    val focusDiopters = effectiveMinFocus * (1f - distance)

    val camera2Control = Camera2CameraControl.from(cam.cameraControl)
    camera2Control.setCaptureRequestOptions(
        CaptureRequestOptions.Builder()
            .setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, focusDiopters)
            .build()
    )
}
```

### Phase 2: Simplify Zoom

**Goal:** Use CameraX native zoom instead of manual crop region.

```kotlin
fun setZoom(ratio: Float) {
    val clampedRatio = ratio.coerceIn(1f, MAX_DIGITAL_ZOOM)
    _zoomRatio.value = clampedRatio

    // Use CameraX native zoom - simpler and handles crop region internally
    camera?.cameraControl?.setZoomRatio(clampedRatio)
}
```

**Note:** This may still switch lenses on multi-camera devices at higher zoom levels. If lens switching is truly unacceptable, we'd need to:
1. Select a specific physical camera by filtering `availableCameraInfos`
2. Or limit max zoom to stay on main camera (device-specific)

### Phase 3: Update ViewModel

**Changes to `RecordingViewModel.kt`:**

```kotlin
fun enableAutoFocus() {
    cameraManager.setFocusMode(FocusMode.AUTO)
    // Note: This triggers a rebind, may cause brief preview flicker
}

fun enableManualFocus() {
    cameraManager.setFocusMode(FocusMode.MANUAL)
}

fun setManualFocus(distance: Float) {
    cameraManager.setManualFocus(distance)
}
```

### Phase 4: Update UI

**Changes to `RecordingScreen.kt`:**

The autofocus icon click should call `enableAutoFocus()` or `enableManualFocus()` based on current state, not just toggle a boolean.

```kotlin
IconButton(
    onClick = {
        if (isAutoFocus) {
            viewModel.enableManualFocus()
        } else {
            viewModel.enableAutoFocus()
        }
    }
)
```

---

## Trade-offs & Considerations

### Rebinding Causes Brief Disruption

When toggling focus mode, the camera must rebind. This causes:
- Brief (~100-500ms) preview interruption
- Recording must be stopped before rebind

**Mitigation:** Only allow focus mode toggle during setup phase (before "Begin Detecting"), not during active recording.

### VideoCapture Camera2Interop Limitation

`VideoCapture.Builder` doesn't directly support `Camera2Interop.Extender`. Options:
1. Accept that video may use different focus settings (not ideal)
2. Use `Camera2Interop` on the underlying Recorder (complex)
3. Test if Preview/ImageAnalysis settings "leak" to VideoCapture (sometimes they do)

### Lens Switching During Zoom

CameraX's seamless zoom may still switch lenses. For truly digital-only zoom:
- Use our current SCALER_CROP_REGION approach (works but complex)
- Or limit zoom to 2x (usually stays on main camera)
- Or select specific physical camera at bind time

---

## Files to Modify

| File | Changes |
|------|---------|
| `ShutterCameraManager.kt` | Add FocusMode enum, refactor bindPreview, add rebind support |
| `RecordingViewModel.kt` | Update focus control methods to trigger rebind |
| `RecordingScreen.kt` | Update autofocus toggle to call correct methods |

---

## Testing Checklist

- [ ] Manual focus slider changes visible focus
- [ ] Autofocus icon toggles between modes (with brief rebind)
- [ ] Zoom slider still works after focus mode change
- [ ] Focus mode cannot be changed during active recording
- [ ] Focus settings persist across zoom changes
- [ ] Video recording uses correct focus setting

---

## References

- [Research Report](camerax-manual-focus-zoom/report.md)
- [Raw Research Notes](camerax-manual-focus-zoom/background.md)
- [CameraX Developers Google Group](https://groups.google.com/a/android.com/g/camerax-developers/)

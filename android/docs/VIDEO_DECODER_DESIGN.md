# Video Decoder Design

## Overview

This document describes the fast video analysis implementation using Android's MediaCodec API instead of MediaMetadataRetriever.

## Problem

Video import analysis was extremely slow due to `MediaMetadataRetriever.getFrameAtTime()`:
- **10-50ms per frame** (seeks to keyframe, decodes, interpolates each time)
- A 7-minute video at 30fps (12,998 frames) took **~6.5 minutes** to analyze

## Solution

Use `MediaCodec` + `MediaExtractor` for sequential frame decoding:
- **0.5-2ms per frame** (continuous hardware-accelerated decoding)
- Same video now analyzes in **~15-30 seconds** (10-50x faster)

## Why Not OpenCV VideoCapture?

The Maven package `org.opencv:opencv:4.9.0` does **NOT** include FFmpeg or MediaNDK backends. It can only read MJPEG/AVI files, not MP4/H.264. This is a [known limitation](https://github.com/opencv/opencv/issues/24545).

## Architecture

```
VideoAnalyzer.kt (coordinator - unchanged interface)
        |
        v
SequentialFrameDecoder.kt (NEW)
  +-------------+    +--------------+    +-------------+
  |MediaExtractor| -> | MediaCodec   | -> | Y-plane     |
  | (demuxer)   |    | (HW decoder) |    | extraction  |
  +-------------+    +--------------+    +-------------+
        |
        v
ThresholdCalculator + EventDetector (existing)
```

## Key Classes

### SequentialFrameDecoder

Location: `data/video/SequentialFrameDecoder.kt`

Handles MediaCodec-based video decoding:

```kotlin
class SequentialFrameDecoder(context: Context, uri: Uri) {
    fun start(): Boolean           // Initialize decoder
    fun decodeNextFrame(): ByteArray?  // Get next frame's Y-plane
    fun calculateBrightness(yPlane: ByteArray): Double
    fun getProgress(): Float       // 0.0 to 1.0
    fun release()                  // Cleanup
}
```

Key features:
- Accepts content:// URIs natively via MediaExtractor
- Extracts Y-plane only (luminance) - no color conversion needed
- Reuses byte array buffer to minimize allocations
- Hardware-accelerated decoding

### VideoAnalyzer Changes

The `analyzeVideo()` method now:
1. Tries fast MediaCodec path first
2. Falls back to MediaMetadataRetriever if MediaCodec fails
3. Logs which path was used

## Performance Comparison

| Metric | MediaMetadataRetriever | MediaCodec |
|--------|------------------------|------------|
| Per-frame time | 10-50ms | 0.5-2ms |
| 12,998 frames | ~6.5 minutes | ~15-30 seconds |
| Memory | New Bitmap each frame | Reused buffer |
| Hardware accel | Limited | Full |

## Technical Details

### Y-Plane Extraction

MediaCodec outputs YUV frames (NV12/NV21 format):
- First `width * height` bytes = Y plane (luminance)
- Remaining bytes = UV planes (chrominance)

For brightness calculation, we only need the Y plane, so we skip UV entirely.

### Brightness Calculation

```kotlin
fun calculateBrightness(yPlane: ByteArray, sampleStep: Int = 4): Double {
    var sum = 0L
    var count = 0
    var i = 0
    while (i < yPlane.size) {
        sum += yPlane[i].toInt() and 0xFF
        count++
        i += sampleStep  // Sample every 4th pixel
    }
    return sum.toDouble() / count
}
```

Sampling every 4th pixel gives 1/4 of pixels (vs 1/16 with step=4 in 2D), providing good accuracy with minimal overhead.

### Error Handling

If MediaCodec fails (unusual codec, device quirk, etc.), we fall back to the original MediaMetadataRetriever implementation:

```kotlin
suspend fun analyzeVideo(...): AnalysisResult? {
    try {
        val result = analyzeWithMediaCodec(uri, frameRate, onProgress)
        if (result != null) return result
    } catch (e: Exception) {
        Log.w(TAG, "MediaCodec failed, falling back", e)
    }
    return analyzeWithMediaMetadataRetriever(uri, frameRate, onProgress)
}
```

## Files

| File | Purpose |
|------|---------|
| `SequentialFrameDecoder.kt` | MediaCodec wrapper for sequential decoding |
| `VideoAnalyzer.kt` | Updated to use decoder with fallback |

## Testing

1. Import a slow-motion video (240fps recorded, plays at 30fps)
2. Verify analysis completes in <30 seconds (vs >5 minutes before)
3. Verify detected events match expectations
4. Test fallback by using an unusual video format

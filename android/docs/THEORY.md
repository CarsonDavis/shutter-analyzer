# Shutter Speed Measurement Theory

This document explains the theory behind measuring camera shutter speeds using high-speed video analysis.

---

## Introduction

### What is Shutter Speed?

Shutter speed is the length of time a camera's shutter remains open during an exposure. It's typically expressed as a fraction of a second:
- 1/500 second (0.002s) - Fast, freezes motion
- 1/60 second (0.0167s) - Standard
- 1/4 second (0.25s) - Slow, allows motion blur

### Why Measure Shutter Speed?

Vintage film cameras with mechanical shutters can drift from their calibrated speeds over time due to:
- Aging lubricants
- Spring fatigue
- Debris or corrosion
- Temperature sensitivity

Accurate shutter speeds are essential for proper exposure. A shutter running slow will overexpose film; one running fast will underexpose.

---

## The Measurement Principle

### High-Speed Video Capture

Modern smartphones can record slow-motion video at high frame rates:
- iPhone: 120fps, 240fps
- Android: 120fps, 240fps, up to 960fps on some models

Each frame represents a fixed slice of time:
```
frame_duration = 1 / recording_fps
```

At 240fps, each frame captures 1/240 second (4.17ms) of real time.

### Brightness Analysis

When recording a camera shutter:
1. Position the phone to see through the shutter opening
2. Place a bright light source behind the shutter
3. When the shutter opens, bright light is visible
4. When closed, the frame is dark

The brightness of each frame indicates shutter state:
- Low brightness → Shutter closed
- High brightness → Shutter open
- Intermediate → Shutter partially open (transitioning)

---

## The Math

### Basic Calculation

```
shutter_duration_seconds = frame_count / recording_fps
shutter_speed = 1 / shutter_duration_seconds
```

**Example:**
Recording at 240fps, shutter open for 3 frames:
```
duration = 3 / 240 = 0.0125 seconds
speed = 1 / 0.0125 = 80 → approximately 1/80 second
```

### Slow-Motion Compensation

Slow-motion videos are recorded at high fps but played back at normal speed (typically 30fps). The file metadata often shows the playback fps, not the recording fps.

When using slow-motion video, you must know the actual recording fps:
```
actual_duration = frame_count / recording_fps  # NOT playback_fps
```

---

## Intermediate Frame Weighting

### The Problem with Simple Frame Counting

Simple frame counting treats each frame as either "open" or "closed" based on a brightness threshold. This creates quantization error:
- A shutter that's open for 2.7 frames gets counted as either 2 or 3
- At 240fps, this is an error of ±4.17ms

### The Weighting Solution

Instead of binary classification, we weight each frame by how "open" the shutter appears:

```
weight = (brightness - baseline) / (peak - baseline)
```

Where:
- `baseline` = brightness when shutter is fully closed
- `peak` = **median brightness of the event** (represents the plateau level)
- `brightness` = brightness of the current frame

The weight is clamped to the range [0, 1]:
- Frame at baseline brightness → weight = 0 (contributes nothing)
- Frame at or above median → weight = 1 (fully open, plateau)
- Frame between baseline and median → proportional weight (transition)

### Why Use Per-Event Median?

Using the **median** of each event's brightness values as the peak naturally identifies the plateau level:

```
Example event: [20, 80, 80, 80, 100, 100, 80, 20]
Median = 80 (the plateau level)

Frame weights:
- 20 → 20/80 = 0.25 (transition frame)
- 80 → 80/80 = 1.00 (plateau - FULL)
- 100 → clamped to 1.00 (plateau - FULL)
```

This approach correctly recognizes that all plateau frames (whether 80 or 100) represent "fully open" for that event, while only the transition frames (20) contribute partial weight.

**Weighted duration:**
```
weighted_duration = sum(weight for each frame in event)
```

### Example

Consider a 10-frame event with brightness values:
```
[6.5, 7.1, 7.2, 7.1, 7.2, 7.2, 7.2, 7.1, 7.1, 2.5]
```

- Median = 7.15 (plateau level)
- Baseline = 0

Frame weights:
- Frame 1 (6.5): 6.5/7.15 = 0.91 (opening)
- Frames 2-9 (7.1-7.2): ≥0.99 → 1.0 (plateau)
- Frame 10 (2.5): 2.5/7.15 = 0.35 (closing)

Weighted duration = 0.91 + 8×1.0 + 0.35 = **9.26 frames**

Compare to simple count = **10 frames**

The weighted value accounts for the partial opening and closing frames.

---

## Error Sources and Limitations

### Frame Rate Limitations

You cannot measure shutter speeds faster than your frame rate allows:
- At 240fps, the minimum measurable duration is 1/240 = 4.17ms
- To measure 1/500 (2ms), you'd need at least 500fps

**Rule of thumb:** Recording fps should be at least 2x the shutter speed being measured.

| Shutter Speed | Minimum Recording FPS |
|---------------|----------------------|
| 1/125 | 250fps |
| 1/250 | 500fps |
| 1/500 | 1000fps |
| 1/1000 | 2000fps |

### Rolling Shutter Effects

Some phone cameras use rolling shutter (sequential line-by-line exposure rather than simultaneous). This can cause:
- Slight timing skew across the frame
- Inaccurate results if the camera shutter moves during the scan

**Mitigation:** Use phones with global shutter or faster rolling shutter speeds.

### Lighting Consistency

The algorithm assumes constant lighting:
- Flickering lights (fluorescent, LED) can cause false events
- Changing ambient light affects baseline/threshold calculations
- Shadows moving through the frame create noise

**Best practice:** Use steady DC lighting (incandescent, constant LED panels).

### Auto-Exposure Lock

The Android app automatically locks the camera's auto-exposure (AE) when detection begins:
- Prevents brightness fluctuations from the phone adjusting exposure
- Ensures consistent brightness readings throughout the session
- Uses Camera2 API's `CONTROL_AE_LOCK = true`
- Unlocks when recording stops to restore normal camera behavior

This is critical because without AE lock, the phone's camera might:
- Brighten the image when it sees a dark shutter
- Darken when the shutter opens
- Cause false triggers or missed events due to shifting baseline

### Accuracy Expectations

| Recording FPS | Best Accuracy |
|---------------|---------------|
| 120fps | ±8ms |
| 240fps | ±4ms |
| 480fps | ±2ms |
| 960fps | ±1ms |

For vintage cameras, accuracy within 10-20% of marked speed is often acceptable.

---

## Two-Phase Calibration (Live Detection)

The Android app uses a two-phase calibration process for real-time event detection.

### Phase 1: Baseline Establishment

The app collects 60 frames with the shutter closed to establish baseline brightness:

```
baseline = 25th percentile of brightness values
maxNoise = max brightness seen during calibration
stdDev = standard deviation of brightness values
```

A preliminary threshold is calculated to detect the calibration shutter event:

```
preliminaryThreshold = max(
    baseline + stdDev × 5,    // 5-sigma statistical outlier
    baseline + 50,            // Absolute minimum increase
    maxNoise × 2              // 2x the brightest noise
)
```

This robust threshold prevents false triggers from shadows and ambient lighting changes.

### Phase 2: Calibration Shutter

The user fires the shutter once for calibration:
- This event is **discarded** and does not count toward measurements
- The app captures the peak brightness during this event
- Final threshold is calculated based on actual measured brightness range

```
peak = max brightness during calibration event
threshold = baseline + (peak - baseline) × 0.8
```

### Why 80% Factor?

Setting the threshold at 80% of the brightness range provides:
- Margin for variation between shutter events
- Tolerance for slightly dimmer events (different angles, lighting)
- Rejection of noise that doesn't reach near-peak brightness

### Threshold Formula Comparison

| Method | Formula | Context |
|--------|---------|---------|
| Old (dark frames only) | `baseline + (median - baseline) × 1.5` | Guessed peak from noise |
| New (with calibration) | `baseline + (peak - baseline) × 0.8` | Uses actual measured peak |

---

## Threshold Detection Methods

### Original Method (Percentile-based)

Uses the 25th percentile as baseline and calculates threshold as:
```
threshold = baseline + (median - baseline) * margin_factor
```

**Pros:** Simple, works well for clean data
**Cons:** Sensitive to margin_factor tuning

### Z-Score Method

Treats bright frames as statistical outliers:
```
threshold = mean + (z * standard_deviation)
```

Automatically tunes z-score to match expected event count.

**Pros:** Statistically principled
**Cons:** Requires knowing expected event count

### DBSCAN Clustering

Uses machine learning to separate brightness clusters:
- Cluster 1: Baseline (closed shutter)
- Cluster 2: Events (open shutter)

Threshold is set between cluster means.

**Pros:** Handles bimodal distributions well
**Cons:** More computationally expensive

---

## Summary

1. High-speed video captures shutter motion as brightness changes
2. Each frame represents 1/fps seconds of real time
3. Weighted frame counting provides sub-frame accuracy
4. Recording fps limits measurable speeds
5. Consistent lighting improves accuracy

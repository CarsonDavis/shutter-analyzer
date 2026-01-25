# Implementation Plan

This document provides implementation instructions for completing the shutter-analyzer tool per the requirements in [REQUIREMENTS.md](REQUIREMENTS.md).

---

## Phase 1: Algorithm Improvements

### 1.1 Implement Intermediate Frame Weighting

**Location:** `shutter_analyzer/shutter_calculator.py`

**Current behavior:** `ShutterEvent.duration_frames` returns a simple frame count:
```python
return self.end_frame - self.start_frame + 1
```

**Required behavior:** Weight each frame proportionally based on how "open" the shutter appears.

**Implementation:**

1. Add `baseline_brightness` and `peak_brightness` parameters to `ShutterEvent.__init__()`:
   ```python
   def __init__(self, start_frame, end_frame, brightness_values,
                baseline_brightness, peak_brightness):
   ```

2. Add a new property `weighted_duration_frames`:
   ```python
   @property
   def weighted_duration_frames(self) -> float:
       """
       Calculate weighted duration where partial-open frames
       contribute proportionally.

       A frame at baseline brightness contributes 0.
       A frame at peak brightness contributes 1.
       Intermediate values are linearly interpolated.
       """
       if self.peak_brightness <= self.baseline_brightness:
           return self.duration_frames  # Fallback to simple count

       total = 0.0
       for brightness in self.brightness_values:
           # Clamp to [0, 1] range
           weight = (brightness - self.baseline_brightness) / \
                    (self.peak_brightness - self.baseline_brightness)
           weight = max(0.0, min(1.0, weight))
           total += weight
       return total
   ```

3. Update `calculate_shutter_speed()` to use weighted duration:
   ```python
   @staticmethod
   def calculate_shutter_speed(shutter_event, fps, recording_fps=None,
                                use_weighted=True):
       if use_weighted:
           frame_count = shutter_event.weighted_duration_frames
       else:
           frame_count = shutter_event.duration_frames
       # ... rest of calculation
   ```

4. Update `FrameBrightnessStats` in `frame_analyzer.py` to include `peak_brightness` (the typical brightness when shutter is fully open). This can be calculated as the 95th percentile of brightness values during detected events.

---

### 1.2 Update Event Creation

**Location:** `shutter_analyzer/frame_analyzer.py` and wherever `ShutterEvent` objects are created

**Changes:**
- When creating `ShutterEvent` objects, pass `baseline_brightness` (from `FrameBrightnessStats.baseline`) and `peak_brightness` (calculated from event data or stats)
- Calculate `peak_brightness` as the average of max brightness values across all detected events, or use the 95th percentile of all event frame brightnesses

---

## Phase 2: Workflow Refactor

### 2.1 Create Main CLI Entry Point

**Location:** `shutter_analyzer/main.py` (currently empty)

**Design:**
```
uv run python -m shutter_analyzer <video_path> [options]
```

**Workflow (two-stage):**

**Stage 1 - Analysis:**
```
$ uv run python -m shutter_analyzer videos/my_camera.mp4

Analyzing video: my_camera.mp4
  Frames: 12000 @ 240 fps
  Duration: 50.0 seconds

Detecting shutter events...
  Found 8 shutter events

Results:
┌───────┬─────────┬──────────┬──────────────────┐
│ Event │ Frames  │ Weighted │ Measured Speed   │
├───────┼─────────┼──────────┼──────────────────┤
│ 1     │ 2       │ 1.73     │ 1/139            │
│ 2     │ 3       │ 2.45     │ 1/98             │
│ ...   │ ...     │ ...      │ ...              │
└───────┴─────────┴──────────┴──────────────────┘

Timeline saved to: outputs/my_camera/brightness_timeline.png

Enter expected speeds to compare (or press Enter to skip):
>
```

**Stage 2 - Comparison (if expected speeds entered):**
```
> 1/500, 1/250, 1/125, 1/60, 1/30, 1/15, 1/8, 1/4

Comparison Results:
┌───────┬──────────┬──────────┬───────────┐
│ Event │ Expected │ Measured │ Variation │
├───────┼──────────┼──────────┼───────────┤
│ 1     │ 1/500    │ 1/487    │ +2.7%     │  [green]
│ 2     │ 1/250    │ 1/231    │ +8.2%     │  [yellow]
│ ...   │ ...      │ ...      │ ...       │
└───────┴──────────┴──────────┴───────────┘

Results saved to: outputs/my_camera/results.md
```

**Implementation steps:**

1. Create argument parser:
   ```python
   parser = argparse.ArgumentParser(description='Analyze camera shutter speeds')
   parser.add_argument('video', help='Path to video file')
   parser.add_argument('--method', choices=['original', 'zscore', 'dbscan'],
                       default='original')
   parser.add_argument('--recording-fps', type=float,
                       help='Actual recording FPS (for slow-mo)')
   ```

2. Implement `analyze_video()` function that:
   - Opens video, gets properties
   - Runs brightness analysis
   - Detects events
   - Returns results without requiring expected speeds

3. Implement `display_results()` function that:
   - Prints formatted table to terminal
   - Generates timeline PNG
   - Saves to output folder

4. Implement `compare_with_expected()` function that:
   - Parses user-entered speeds (fraction notation)
   - Calculates variation percentages
   - Displays comparison table with color gradient
   - Saves results.md

5. Add `__main__.py` to make module executable:
   ```python
   from .main import main
   main()
   ```

---

### 2.2 Speed Input Parser

**Location:** `shutter_analyzer/main.py` or new `shutter_analyzer/utils.py`

**Function:** Parse fraction notation like "1/500, 1/250, 1/125"

```python
def parse_shutter_speeds(input_string: str) -> List[float]:
    """
    Parse shutter speeds from fraction notation.

    Args:
        input_string: Comma-separated fractions like "1/500, 1/250, 1/125"

    Returns:
        List of speeds in seconds (e.g., [0.002, 0.004, 0.008])
    """
    speeds = []
    for part in input_string.split(','):
        part = part.strip()
        if '/' in part:
            num, denom = part.split('/')
            speeds.append(float(num) / float(denom))
        else:
            speeds.append(float(part))
    return speeds
```

---

## Phase 3: Output Formatting

### 3.1 Terminal Table with Color Gradient

**Location:** `shutter_analyzer/output.py` (new file)

**Dependencies:** None required (use ANSI escape codes), or optionally `rich` library

**Implementation:**

```python
def get_variation_color(variation_percent: float) -> str:
    """
    Return ANSI color code based on variation percentage.

    Gradient: green (0%) -> yellow (10%) -> red (20%+)
    """
    abs_var = abs(variation_percent)
    if abs_var < 5:
        return '\033[92m'  # Bright green
    elif abs_var < 10:
        return '\033[93m'  # Yellow
    elif abs_var < 15:
        return '\033[33m'  # Dark yellow/orange
    else:
        return '\033[91m'  # Red

def format_results_table(events, expected_speeds=None, fps=None):
    """Format results as a table string for terminal display."""
    # Implementation using string formatting
    pass
```

For true gradient (not discrete steps), calculate RGB values:
```python
def variation_to_rgb(variation_percent: float) -> Tuple[int, int, int]:
    """Convert variation to RGB color on green-yellow-red gradient."""
    abs_var = min(abs(variation_percent), 25)  # Cap at 25%
    ratio = abs_var / 25.0

    if ratio < 0.5:
        # Green to yellow
        r = int(255 * (ratio * 2))
        g = 255
    else:
        # Yellow to red
        r = 255
        g = int(255 * (1 - (ratio - 0.5) * 2))

    return (r, g, 0)
```

---

### 3.2 Markdown Results File

**Location:** `shutter_analyzer/output.py`

**Output file:** `outputs/<video-name>/results.md`

**Format:**
```markdown
# Shutter Speed Analysis Results

**Video:** fuji_gw690ii_shutter_speeds.mp4
**Date:** 2025-01-24
**Recording FPS:** 240

## Detected Events

| Event | Start Frame | End Frame | Duration (frames) | Weighted Duration | Measured Speed |
|-------|-------------|-----------|-------------------|-------------------|----------------|
| 1     | 1204        | 1206      | 3                 | 2.45              | 1/98           |
| 2     | 2891        | 2894      | 4                 | 3.21              | 1/75           |

## Comparison with Expected

| Event | Expected | Measured | Variation |
|-------|----------|----------|-----------|
| 1     | 1/500    | 1/487    | <span style="color: #22c55e">+2.7%</span> |
| 2     | 1/250    | 1/231    | <span style="color: #eab308">+8.2%</span> |
```

---

### 3.3 Output Directory Structure

**Location:** `shutter_analyzer/output.py`

```python
from pathlib import Path

def get_output_dir(video_path: str) -> Path:
    """
    Get output directory for a video file.
    Creates outputs/<video-name>/ if it doesn't exist.
    """
    video_name = Path(video_path).stem
    output_dir = Path('outputs') / video_name
    output_dir.mkdir(parents=True, exist_ok=True)
    return output_dir
```

**Files to save:**
- `brightness_timeline.png` - Line graph of brightness over time
- `results.md` - Markdown results table
- `brightness_histogram.png` - (optional) Distribution histogram

---

## Phase 4: Manual Verification Tool

### 4.1 Verification UI Module

**Location:** `shutter_analyzer/verify.py` (new file)

**Purpose:** Quick visual confirmation that event detection is working correctly during development.

**Dependencies:** OpenCV (already installed) - use `cv2.imshow()` for simple display

**Interface:**
```
$ uv run python -m shutter_analyzer.verify videos/test.mp4

Analyzing video...
Found 8 events. Press any key to step through, 'q' to quit.

Event 1/8: Frames 1203-1206
[Shows side-by-side: Frame 1202 | Frame 1203]
Press key for next event...
```

**Implementation:**

```python
"""Manual verification tool for event detection."""

import cv2
import numpy as np
from pathlib import Path
from .video_processor import VideoReader
from .frame_analyzer import FrameAnalyzer


def annotate_frame(frame, frame_num: int, brightness: float, label: str = ""):
    """Add frame number and brightness annotation to frame."""
    annotated = frame.copy()
    h, w = annotated.shape[:2]

    # Add dark background bar at bottom
    cv2.rectangle(annotated, (0, h-60), (w, h), (0, 0, 0), -1)

    # Add text
    text = f"Frame {frame_num} | Brightness: {brightness:.1f}"
    if label:
        text = f"{label}: {text}"
    cv2.putText(annotated, text, (10, h-20),
                cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 255), 2)

    return annotated


def create_side_by_side(frame_before, frame_start,
                         num_before, num_start,
                         bright_before, bright_start):
    """Create side-by-side comparison image."""
    # Annotate both frames
    left = annotate_frame(frame_before, num_before, bright_before, "BEFORE")
    right = annotate_frame(frame_start, num_start, bright_start, "EVENT START")

    # Add colored border to distinguish
    left = cv2.copyMakeBorder(left, 5, 5, 5, 5, cv2.BORDER_CONSTANT, value=(100, 100, 100))
    right = cv2.copyMakeBorder(right, 5, 5, 5, 5, cv2.BORDER_CONSTANT, value=(0, 255, 0))

    # Stack horizontally
    return np.hstack([left, right])


def verify_events(video_path: str, method: str = "original"):
    """
    Interactive verification of detected events.

    Shows frame before event and first frame of event side-by-side.
    User presses any key to advance, 'q' to quit.
    """
    video = VideoReader.open_video(video_path)
    props = VideoReader.get_video_properties(video)

    print(f"Analyzing {video_path}...")
    print(f"  {props['frame_count']} frames @ {props['fps']} fps")

    # Run analysis
    video.set(cv2.CAP_PROP_POS_FRAMES, 0)
    frames_gen = VideoReader.read_frames(video)
    stats, brightness_values = FrameAnalyzer.analyze_brightness_distribution(
        frames_gen, method=method
    )

    events = FrameAnalyzer.find_shutter_events(brightness_values, stats.threshold)

    print(f"Found {len(events)} events. Threshold: {stats.threshold:.1f}")
    print(f"Press any key to step through, 'q' to quit.\n")

    for i, (start, end, _) in enumerate(events):
        # Get frame before event (or frame 0 if event starts at 0)
        before_idx = max(0, start - 1)

        frame_before = VideoReader.get_frame_at_index(video, before_idx)
        frame_start = VideoReader.get_frame_at_index(video, start)

        if frame_before is None or frame_start is None:
            print(f"Could not read frames for event {i+1}")
            continue

        # Create comparison image
        comparison = create_side_by_side(
            frame_before, frame_start,
            before_idx, start,
            brightness_values[before_idx], brightness_values[start]
        )

        # Display
        window_title = f"Event {i+1}/{len(events)} | Frames {start}-{end} | Duration: {end-start+1}"
        cv2.imshow(window_title, comparison)

        print(f"Event {i+1}/{len(events)}: Frames {start}-{end}")
        print(f"  Before (frame {before_idx}): brightness {brightness_values[before_idx]:.1f}")
        print(f"  Start  (frame {start}): brightness {brightness_values[start]:.1f}")
        print(f"  Threshold: {stats.threshold:.1f}")
        print()

        key = cv2.waitKey(0) & 0xFF
        cv2.destroyAllWindows()

        if key == ord('q'):
            print("Quitting verification.")
            break

    video.release()
    print("Verification complete.")


def main():
    import argparse
    parser = argparse.ArgumentParser(description='Verify event detection visually')
    parser.add_argument('video', help='Path to video file')
    parser.add_argument('--method', choices=['original', 'zscore', 'dbscan'],
                        default='original', help='Detection method')
    args = parser.parse_args()

    verify_events(args.video, args.method)


if __name__ == '__main__':
    main()
```

**Usage:**
```bash
# Basic usage
uv run python -m shutter_analyzer.verify videos/fuji_gw690ii_shutter_speeds.mp4

# With different detection method
uv run python -m shutter_analyzer.verify videos/test.mp4 --method dbscan
```

**Features:**
- Side-by-side display: frame before event (gray border) vs first event frame (green border)
- Frame number and brightness value annotated on each frame
- Event count and threshold displayed
- Step through with any key, quit with 'q'
- Works on any platform with display (uses OpenCV highgui)

**Optional enhancements (if needed):**
- Add `--event N` flag to jump directly to event N
- Add end-of-event view (last frame of event vs frame after)
- Save comparison images to disk with `--save` flag
- Show brightness threshold as horizontal line overlay on a brightness graph

---

## Phase 5: Documentation

### 5.1 Theory Documentation

**Location:** `docs/THEORY.md`

**Content outline:**

1. **Introduction**
   - What is shutter speed?
   - Why measure it?

2. **The Measurement Principle**
   - High-speed video captures shutter motion
   - Each frame represents 1/fps seconds of real time
   - Counting "bright" frames = measuring shutter open time

3. **The Math**
   ```
   shutter_duration_seconds = frame_count / recording_fps
   shutter_speed = 1 / shutter_duration_seconds
   ```

   Example:
   ```
   Recording at 240fps, shutter open for 3 frames:
   duration = 3 / 240 = 0.0125 seconds
   speed = 1 / 0.0125 = 80 → approximately 1/80 second
   ```

4. **Intermediate Frame Weighting**
   - Why simple frame counting is imprecise
   - How partial-open frames are weighted
   - The weighting formula:
     ```
     weight = (brightness - baseline) / (peak - baseline)
     weighted_duration = sum(weights for all frames in event)
     ```

5. **Error Sources and Limitations**
   - Frame rate limitations (can't measure faster than 1/fps)
   - Rolling shutter effects
   - Lighting consistency requirements
   - Accuracy expectations

6. **Slow-Motion Compensation**
   - How phone slow-mo works (records at high fps, plays at normal fps)
   - Why we need the actual recording fps, not playback fps

---

### 5.2 How-To Guide

**Location:** `docs/HOW_TO.md`

**Content outline:**

1. **Equipment Needed**
   - Smartphone with slow-motion capability
   - Tripod or stable surface
   - Bright light source (window, lamp, flashlight)
   - The camera to test

2. **Setup**
   - Diagram/description of arrangement
   - Camera back open, shutter facing phone
   - Bright light behind the shutter
   - Phone positioned to see shutter clearly

3. **Phone Settings**

   **iPhone:**
   - Open Camera app
   - Swipe to "Slo-Mo"
   - Settings > Camera > Record Slo-mo: 240fps recommended

   **Android:**
   - Varies by manufacturer
   - Samsung: Camera > More > Super Slow-mo or Slow motion
   - Google Pixel: Camera > Slow Motion
   - Look for 240fps or 120fps options

4. **Recording Procedure**
   - Start recording
   - Fire shutter at each speed setting
   - Wait 1-2 seconds between shots
   - Stop recording

5. **Transferring Video**
   - iPhone: AirDrop, Files app, or cable
   - Android: USB transfer, Google Drive, etc.
   - Place video in `videos/` folder

6. **Running the Analysis**
   ```bash
   uv run python -m shutter_analyzer videos/your_video.mp4
   ```

7. **Interpreting Results**
   - What the variation percentages mean
   - Typical accuracy for different shutter types
   - When to be concerned

---

## Phase 6: Cleanup

### 6.1 Remove Redundant Files

**Files to delete:**
- `frames.py` - Functionality not needed for main tool
- `frames_smart.py` - Duplicates module code, not needed

**Alternative:** If frame extraction is useful for debugging, refactor `frames_smart.py` to:
- Import from `shutter_analyzer` module instead of duplicating code
- Move to `scripts/extract_frames.py`

### 6.2 Update Test Script

**Location:** `test_basic_functionality.py`

**Changes:**
- Update to use `videos/` folder for input
- Update to save outputs to `outputs/` folder
- Consider converting to proper pytest tests

### 6.3 Update INDEX.md

After implementation, update INDEX.md to reflect:
- New file structure
- Updated usage instructions
- Links to new documentation

---

## Implementation Order

Recommended sequence:

1. **Phase 4.1** - Manual verification tool (build first to validate algorithm changes)
2. **Phase 1.1** - Intermediate frame weighting (core algorithm fix)
3. **Phase 1.2** - Update event creation
4. **Phase 3.3** - Output directory structure
5. **Phase 2.1** - Main CLI (basic version)
6. **Phase 3.1** - Terminal table formatting
7. **Phase 3.2** - Markdown output
8. **Phase 2.2** - Speed input parser
9. **Phase 6.1** - Remove redundant files
10. **Phase 6.2** - Update test script
11. **Phase 5.1** - Theory documentation
12. **Phase 5.2** - How-to guide
13. **Phase 6.3** - Final INDEX.md update

---

## Testing Checklist

After implementation, verify:

- [ ] `uv run python -m shutter_analyzer.verify videos/fuji_gw690ii_shutter_speeds.mp4` shows events correctly
- [ ] `uv run python -m shutter_analyzer videos/fuji_gw690ii_shutter_speeds.mp4` runs without error
- [ ] Events are detected correctly
- [ ] Weighted duration differs from simple frame count
- [ ] Timeline PNG is saved to correct output folder
- [ ] Results table displays with color gradient in terminal
- [ ] Entering expected speeds produces comparison table
- [ ] results.md is saved with correct formatting
- [ ] Slow-motion videos calculate correct speeds (not 30fps playback rate)
- [ ] Documentation is clear and complete

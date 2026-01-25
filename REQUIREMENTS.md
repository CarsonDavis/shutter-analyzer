# Shutter Analyzer - Requirements

## Overview

This tool measures actual shutter speeds of vintage film cameras by analyzing high-speed video recordings. Users record their camera's shutter firing at various speed settings, and the tool calculates the actual measured duration vs. expected duration.

---

## Target Use Case

- **Cameras**: Vintage film cameras with mechanical shutters
- **Recording Devices**: Modern smartphones (iPhone and Android) with high-speed/slow-motion video capability
- **Purpose**: Verify shutter accuracy, diagnose timing issues, document camera condition

---

## Functional Requirements

### FR-1: Video Input

| Requirement | Specification |
|-------------|---------------|
| Supported formats | Any format OpenCV can read (MP4, MOV, AVI, etc.) |
| Frame rate support | Must handle slow-motion video (120fps, 240fps, etc.) |
| Processing mode | One video at a time |
| Input location | Videos placed in `videos/` folder |

### FR-2: Analysis Algorithm

The analysis assumes:
- Video is mostly dark/static with the shutter visible in frame
- Lighting is consistent (no external brightness changes)
- Shutter opens to reveal a bright area, then closes

**Processing steps:**
1. Calculate full-frame brightness for every frame
2. Identify two steady-state values: fully closed (dark) and fully open (bright)
3. Detect shutter events as brightness excursions above baseline
4. **Intermediate frame weighting**: Partial-open frames are weighted proportionally based on their brightness delta between closed and open states
5. Calculate effective shutter speed from weighted frame count

### FR-3: Shutter Speed Comparison

| Requirement | Specification |
|-------------|---------------|
| Input format | Fraction notation (1/125, 1/250, 1/500, etc.) |
| Timing | Expected speeds entered AFTER analysis completes |
| Comparison | Calculate percentage variation from expected |

### FR-4: Output - Line Graph

| Requirement | Specification |
|-------------|---------------|
| Format | PNG image |
| Content | Brightness values over linear time (frame number or seconds) |
| Features | Clear visualization of shutter events as brightness peaks |

### FR-5: Output - Results Table

| Requirement | Specification |
|-------------|---------------|
| Format | Terminal display + Markdown file |
| Columns | Event #, Frame count, Measured speed, Expected speed, Variation % |
| Color coding | Gradient based on accuracy (green = accurate, red = inaccurate) |

**Color gradient**: Continuous gradient from green (0% error) through yellow to red (higher error), not discrete pass/fail thresholds.

### FR-6: Output Location

```
outputs/
├── examples/                    # Example outputs (tracked in git)
│   └── *.png                    # Sample visualizations
└── <video-name>/                # Per-video results (gitignored)
    ├── brightness_timeline.png
    ├── results.md
    └── (any other output files)
```

---

## Documentation Requirements

### DR-1: Theory Documentation

Explain the underlying theory and equations:
- How shutter speed relates to frame count at a given FPS
- The math: `shutter_speed = frame_count / video_fps`
- How intermediate frame weighting works
- Error sources and limitations

### DR-2: How-To Guide

Step-by-step instructions for users:

1. **Equipment setup**
   - Tripod positioning
   - Camera placement relative to phone
   - Lighting requirements (bright background behind shutter)

2. **Phone settings**
   - iPhone: How to enable slow-motion (240fps recommended)
   - Android: How to enable high-speed video recording
   - Resolution and quality settings

3. **Recording procedure**
   - How to frame the shot
   - Firing the shutter at each speed setting
   - Best practices for consistent results

4. **Video transfer**
   - Getting video from phone to computer
   - Placing in `videos/` folder

5. **Running the analysis**
   - Command-line usage
   - Interpreting results

---

## Non-Functional Requirements

### NFR-1: Usability
- Clear terminal output during processing
- Helpful error messages for common issues (file not found, corrupt video, etc.)

### NFR-2: Performance
- Process videos without loading entire file into memory
- Use frame generators for memory efficiency

### NFR-3: Accuracy
- Account for slow-motion playback rates correctly
- Sub-frame accuracy via intermediate frame weighting

### NFR-4: Manual Verification Mode
- Developer tool for validating event detection
- Display frames at event boundaries side-by-side (frame before event, first frame of event)
- Simple UI to step through each detected event
- Shows frame number and brightness value on each frame
- Allows quick visual confirmation that thresholds are working correctly

---

## Current Implementation Status

| Requirement | Status |
|-------------|--------|
| FR-1: Video Input | Complete - `videos/` folder created |
| FR-2: Analysis Algorithm | Complete - includes intermediate frame weighting |
| FR-3: Speed Comparison | Complete - two-stage workflow (analyze first, then compare) |
| FR-4: Line Graph | Complete - brightness_timeline.png saved to outputs/ |
| FR-5: Results Table | Complete - terminal table with color gradient, markdown output |
| FR-6: Output Location | Complete - `outputs/<video-name>/` structure |
| NFR-4: Manual Verification | Complete - `verify.py` with side-by-side frame viewer |
| DR-1: Theory Docs | Complete - `docs/THEORY.md` |
| DR-2: How-To Guide | Complete - `docs/HOW_TO.md` |

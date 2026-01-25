# Implementation Log

This log tracks implementation progress for the Shutter Analyzer project.

---

## 2026-01-24 - Phase 1 - Manual Verification Tool

### Changes Made
- `shutter_analyzer/verify.py`: Created manual verification tool
  - `annotate_frame()` - Adds frame number and brightness to frames
  - `create_side_by_side()` - Creates side-by-side comparison images
  - `verify_events()` - Interactive event stepping with OpenCV display
  - `main()` - CLI entry point with argparse
- `pyproject.toml`: Created Python project configuration with dependencies
  - opencv-python, numpy, matplotlib, scikit-learn
- `INDEX.md`: Added verify.py documentation and project structure updates

### Verification
- Command: `uv run python -c "from shutter_analyzer.verify import ..."`
- Result: PASS - Module imports correctly and analysis runs

### Notes
- GUI-based verification requires display; tested module imports and analysis logic
- Created pyproject.toml to properly manage dependencies with uv

---

## 2026-01-24 - Phase 2 - Intermediate Frame Weighting

### Changes Made
- `shutter_analyzer/shutter_calculator.py`:
  - Added `baseline_brightness` and `peak_brightness` parameters to `ShutterEvent.__init__()`
  - Added `weighted_duration_frames` property with linear interpolation formula
  - Updated `calculate_shutter_speed()` with `use_weighted` parameter
  - Updated `__repr__` to show weighted duration
- `shutter_analyzer/frame_analyzer.py`:
  - Added `peak_brightness` field to `FrameBrightnessStats` dataclass
  - Added `calculate_peak_brightness()` static method (95th percentile of event brightness)
  - Updated `analyze_video_and_find_events()` to calculate and set peak_brightness

### Verification
- Command: Tested weighted duration calculation on fuji_gw690ii_shutter_speeds.mp4
- Result: PASS - Weighted durations differ from simple counts (e.g., 4 frames → 1.88 weighted)

### Notes
- Weighting formula: `weight = (brightness - baseline) / (peak - baseline)`, clamped to [0, 1]
- Peak brightness calculated as 95th percentile of all event frame brightness values

---

## 2026-01-24 - Phase 3 - Output Formatting

### Changes Made
- `shutter_analyzer/output.py`: Created output formatting module
  - `get_output_dir()` - Creates outputs/<video-stem>/ directories
  - `get_variation_color()` - ANSI color codes for terminal (green→yellow→red gradient)
  - `variation_to_rgb()` - RGB colors for markdown HTML spans
  - `format_speed_fraction()` - Formats 0.002 as "1/500"
  - `format_results_table()` - Terminal table with color-coded variations
  - `generate_results_markdown()` - Full markdown report with inline color styling
  - `save_results_markdown()` - Saves report to results.md

### Verification
- Command: Tested table and markdown generation with sample events
- Result: PASS - Color gradients and formatting work correctly

### Notes
- Color gradient: green (0-5%), yellow (5-10%), orange (10-15%), red (15%+)
- Markdown uses inline HTML `<span style="color: #hexcode">` for colors

---

## 2026-01-24 - Phase 4 - Main CLI Entry Point

### Changes Made
- `shutter_analyzer/main.py`: Created main CLI module
  - `parse_shutter_speeds()` - Parses "1/500, 1/250" → [0.002, 0.004]
  - `analyze_video()` - Full analysis pipeline returning events, fps, stats
  - `save_timeline_plot()` - Matplotlib timeline with event highlights
  - `main()` - Two-stage CLI workflow with argparse
- `shutter_analyzer/__main__.py`: Created for `python -m shutter_analyzer` execution

### CLI Options
- `video` - Path to video file (required)
- `--method` - Detection method (original/zscore/dbscan)
- `--recording-fps` - Actual FPS for slow-motion videos
- `--events` - Expected event count (required for zscore/dbscan)

### Verification
- Command: `echo "" | uv run python -m shutter_analyzer videos/fuji_gw690ii_shutter_speeds.mp4`
- Result: PASS - Analyzes video, saves timeline and results.md

---

## 2026-01-24 - Phase 6 - Cleanup

### Changes Made
- Deleted `frames.py` - Redundant basic frame extraction script
- Deleted `frames_smart.py` - Redundant smart frame extraction script
- `shutter_analyzer/__init__.py`: Added package exports
  - Exports all main classes: VideoReader, FrameAnalyzer, ShutterEvent, etc.
  - Added module docstring and __version__

### Verification
- Command: `uv run python -c "from shutter_analyzer import *; print(VideoReader)"`
- Result: PASS - Package imports work correctly

---

## 2026-01-24 - Phase 7 - Documentation

### Changes Made
- `docs/THEORY.md`: Created theory documentation
  - Measurement principle and math
  - Intermediate frame weighting formula
  - Error sources and limitations
  - Threshold detection methods explained
- `docs/HOW_TO.md`: Created usage guide
  - Equipment and setup instructions
  - Phone settings for iPhone and Android
  - Recording procedure step-by-step
  - Video transfer instructions
  - Running analysis with examples
  - Interpreting results and troubleshooting

### Verification
- Both docs created and linked in INDEX.md
- Result: PASS

---

## 2026-01-24 - Phase 8 - Final Documentation Update

### Changes Made
- `INDEX.md`: Updated project structure and documentation
  - Added output.py, __main__.py, main.py to structure
  - Removed frames.py, frames_smart.py references
  - Added documentation links table
  - Updated all module descriptions
- `REQUIREMENTS.md`: Updated status table
  - All requirements marked as Complete

### Final State
All phases complete. Tool is fully functional with:
- Two-stage CLI workflow
- Intermediate frame weighting for sub-frame accuracy
- Color-coded terminal output
- Markdown report generation
- Manual verification tool
- Complete documentation

---

## 2026-01-24 - Event Frame Montages Feature

### Changes Made
- `shutter_analyzer/verify.py`: Added montage generation mode
  - `generate_event_montages()` - Creates PNG images showing all frames per event
  - Each frame labeled with brightness weight (FULL, percentage)
  - Color coding: green (≥95%), orange (75-95%), red (<75%)
  - Long events show first half + last half of frames
  - CLI flag: `--montage` to enable montage mode
- `REQUIREMENTS.md`: Added NFR-5 for event frame montages
- `INDEX.md`: Updated verify.py documentation with montage mode

### Usage
```bash
uv run python -m shutter_analyzer.verify videos/test.mp4 --montage
```

### Output
- `outputs/<video-name>/event_01_frames.png`
- `outputs/<video-name>/event_02_frames.png`
- ... etc

### Verification
- Command: `uv run python -m shutter_analyzer.verify videos/fuji_645_shutter_speeds.mp4 --montage`
- Result: PASS - Generated 10 montage images

---

## 2026-01-24 - Per-Event Peak Using Median

### Changes Made
- `shutter_analyzer/shutter_calculator.py`: Updated `weighted_duration_frames` property
  - Now uses per-event median as the peak (not global peak)
  - Median naturally identifies the plateau level within each event
  - Frames at/above median = FULL (weight 1.0)
  - Frames below median = weighted proportionally

- `shutter_analyzer/verify.py`: Updated montage generation
  - Uses per-event median for frame labels
  - Plateau frames show as FULL (green)
  - Transition frames show percentage (orange/red)

### Rationale
Using a global peak caused plateau frames to show as 80% when they were
visually fully open. The per-event median approach recognizes that:
- Example: [20, 80, 80, 80, 100, 100, 80, 20]
- Median = 80 (the plateau level)
- 80s and 100s → FULL (at or above median)
- 20s → 25% (transition frames)

### Impact
- Event 4 (1/60): All 5 frames now show as FULL (was 80%)
- Event 5 (1/30): 8 plateau frames FULL, 2 transition frames labeled
- Weighted durations now closer to simple frame counts for plateau events

---

## 2026-01-24 - Context Frames in Montages

### Changes Made
- `shutter_analyzer/verify.py`: Added BEFORE/AFTER context frames to montages
  - Each montage now shows one frame before the detected event
  - Each montage now shows one frame after the detected event
  - Context frames labeled in blue with "BEFORE" / "AFTER"
  - Helps visually verify event boundary detection

### Montage Layout
```
[BEFORE] [F1] [F2] ... [Fn] [AFTER]
  blue   event frames    blue
```

### Verification
- Command: `uv run python -m shutter_analyzer.verify videos/fuji_645_shutter_speeds.mp4 --montage`
- Result: PASS - Montages show dark frames on either side of bright event frames

---


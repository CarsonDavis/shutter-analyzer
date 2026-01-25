# Shutter Analyzer - Project Index

A Python tool for measuring actual camera shutter speeds from video recordings. It analyzes videos showing camera shutters opening at different speed settings and calculates the measured shutter duration compared to expected values.

## Documentation

| Document | Purpose |
|----------|---------|
| [INDEX.md](INDEX.md) | This file - project overview and file index |
| [REQUIREMENTS.md](REQUIREMENTS.md) | Functional and documentation requirements |
| [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md) | Step-by-step implementation instructions |
| [architecture.md](architecture.md) | API documentation and technical details |

---

## Project Structure

```
shutter-analyzer/
├── shutter_analyzer/              # Core Python module
│   ├── __init__.py                # Package initializer
│   ├── video_processor.py         # Video file I/O operations
│   ├── frame_analyzer.py          # Brightness analysis and event detection
│   ├── shutter_calculator.py      # Shutter speed calculations
│   └── main.py                    # Entry point (placeholder)
├── videos/                        # Input video files
│   └── *.mp4                      # Test videos
├── outputs/                       # Generated outputs (gitignored except examples/)
│   └── examples/                  # Example outputs for reference
├── docs/                          # Documentation and research
│   └── research/                  # Market research and publishing guides
├── backup/                        # Extracted frames backup (gitignored)
├── test_basic_functionality.py    # Interactive test suite
├── frames.py                      # Basic frame extraction utility (to be removed)
├── frames_smart.py                # Smart frame extraction (to be removed)
├── REQUIREMENTS.md                # Project requirements
├── IMPLEMENTATION_PLAN.md         # Implementation instructions
├── architecture.md                # API documentation
└── INDEX.md                       # This file
```

---

## Core Module Files

### `shutter_analyzer/video_processor.py`
Handles all video file operations using OpenCV.

**Class: `VideoReader`**
- `open_video()` - Opens and validates video files
- `get_video_properties()` - Extracts metadata (frame count, FPS, dimensions, duration)
- `read_frames()` - Generator yielding frames sequentially
- `get_frame_at_index()` - Random access to specific frames

---

### `shutter_analyzer/frame_analyzer.py`
Analyzes frames for brightness and detects shutter open/close events.

**Class: `FrameBrightnessStats`** - Data class storing brightness statistics

**Class: `FrameAnalyzer`** - Static methods for frame analysis:
- `calculate_frame_brightness()` - Computes average brightness of a frame
- `analyze_brightness_distribution()` - Computes stats and determines detection threshold
- `is_shutter_open()` - Boolean check against threshold
- `find_shutter_events()` - Identifies sequences of frames where shutter is open
- `find_threshold_using_zscore()` - Z-score statistical threshold detection
- `find_threshold_using_dbscan()` - DBSCAN clustering threshold detection
- `analyze_video_and_find_events()` - Two-pass analysis (brightness stats then events)

---

### `shutter_analyzer/shutter_calculator.py`
Calculates shutter speeds from detected events.

**Class: `ShutterEvent`** - Data class representing a shutter open/close event:
- `start_frame`, `end_frame` - Event boundaries
- `duration_frames` - Number of frames shutter was open
- `max_brightness`, `avg_brightness` - Brightness metrics

**Class: `ShutterSpeedCalculator`** - Static methods:
- `calculate_duration_seconds()` - Converts frame count to duration (supports slow-motion)
- `calculate_shutter_speed()` - Calculates speed as fraction (1/x)
- `compare_with_expected()` - Computes percentage error
- `group_shutter_events()` - Matches events to expected speed settings

---

## Utility Scripts

### `test_basic_functionality.py`
Interactive test suite for all core functionality.

**Usage:**
```bash
uv run test_basic_functionality.py
```

**Test Functions:**
- `test_video_access()` - Validates video file opening
- `test_brightness_calculation()` - Tests brightness on sample frames
- `test_brightness_distribution()` - Tests stats and creates histogram
- `test_adaptive_event_detection()` - Tests event detection with timeline plot
- `test_shutter_speed_calculation()` - Tests speed calculations
- `run_analysis_pipeline()` - Full pipeline with three threshold methods

**Features:**
- Interactive CLI prompts for video path and expected speeds
- Generates matplotlib visualizations
- Supports three threshold methods: original, zscore, dbscan

---

### `frames.py`
Basic frame extraction - extracts and saves all frames as WebP images.

**Usage:**
```bash
uv run frames.py <video_path> [options]
```

**Options:**
- `--quality` - WebP compression quality (0-100)
- `--step` - Extract every Nth frame
- `--max-frames` - Maximum frames to extract

**Output:** Individual WebP files with frame numbers overlaid

---

### `frames_smart.py`
Smart frame extraction - extracts frames around detected shutter events.

**Usage:**
```bash
uv run frames_smart.py <video_path> [options]
```

**Options:**
- `--padding` - Frames before/after events to include
- `--percentile` - Percentile for threshold calculation
- `--margin-factor` - Threshold margin factor

**Features:**
- Two-pass extraction (analyze then extract)
- Annotates frames with brightness values and event markers
- Only extracts frames near shutter events

---

## Data Flow

```
Input Video File
       ↓
[VideoReader] → Opens video, provides frame access
       ↓
[FrameAnalyzer] → Calculates brightness, detects events
       ↓
[ShutterEvent objects] → Store event boundaries and brightness
       ↓
[ShutterSpeedCalculator] → Converts to shutter speed measurements
       ↓
Output: Results, plots, extracted frames
```

---

## Threshold Detection Methods

1. **Original** - Percentile-based with configurable margin factor
2. **Z-Score** - Statistical outlier detection
3. **DBSCAN** - Clustering-based separation of baseline and events

---

## Dependencies

- **OpenCV** (`cv2`) - Video processing
- **NumPy** - Numerical operations
- **Matplotlib** - Visualization
- **scikit-learn** - DBSCAN clustering

---

## Generated Output Files

Located in `outputs/examples/`:

| File | Description |
|------|-------------|
| `brightness_histogram.png` | Distribution histogram with threshold markers |
| `brightness_timeline.png` | Brightness over time with detected events |
| `brightness_timeline_original.png` | Timeline using original method |
| `brightness_timeline_zscore.png` | Timeline using z-score method |
| `brightness_timeline_dbscan.png` | Timeline using DBSCAN method |

Note: `backup/` contains extracted frames (gitignored, can be regenerated).

---

## Test Videos

Located in `videos/`:

| File | Description |
|------|-------------|
| `fuji_645_shutter_speeds.mp4` | Fuji 645 camera shutter test |
| `fuji_gw690ii_shutter_speeds.mp4` | Fuji GW690II camera shutter test |
| `fuji_gw690ii_sunlight.mp4` | GW690II test in sunlight conditions |

---

## Research

Located in `docs/research/`:

### `research-android-shutter-apps/report.md`
Market research on existing Android apps for measuring mechanical camera shutter speeds.

**Key findings:**
- Only one dedicated app exists: **Shutter-Speed by Filmomat** (requires ~$40 PhotoPlug hardware)
- **No video-analysis apps exist** - all solutions use light sensors or sound analysis
- Manual frame counting in video editors is the current DIY alternative
- This project fills a genuine market gap by automating video brightness analysis

---

## Google Play Publishing

Located in `docs/research/google-play-publishing/`:

| File | Purpose |
|------|---------|
| `report.md` | Complete guide to Play Store requirements and process |
| `store-listing.md` | App title, descriptions, keywords, visual asset specs |
| `privacy-policy.md` | Privacy policy (host on public URL before submission) |
| `data-safety-declaration.md` | Answers for Play Console data safety questionnaire |
| `registration-checklist.md` | Step-by-step checklist for registration |

**Key requirements:**
- $25 one-time registration fee
- Android App Bundle (.aab) targeting API 35+
- Privacy policy hosted on public URL
- 512x512 app icon + 2-8 screenshots
- New personal accounts: 12 testers for 14 days before production

---

## Implementation Status

See [REQUIREMENTS.md](REQUIREMENTS.md#current-implementation-status) for detailed status of each requirement.

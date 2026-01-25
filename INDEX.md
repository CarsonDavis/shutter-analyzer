# Shutter Analyzer - Project Index

A Python tool for measuring actual camera shutter speeds from video recordings. It analyzes videos showing camera shutters opening at different speed settings and calculates the measured shutter duration compared to expected values.

## Documentation

| Document | Purpose |
|----------|---------|
| [INDEX.md](INDEX.md) | This file - project overview and file index |
| [REQUIREMENTS.md](REQUIREMENTS.md) | Functional and documentation requirements (CLI tool) |
| [docs/ANDROID_APP_SPEC.md](docs/ANDROID_APP_SPEC.md) | **Android app specification** - screens, flows, requirements |
| [docs/ANDROID_WIREFRAMES.md](docs/ANDROID_WIREFRAMES.md) | **Android wireframes** - ASCII mockups for all screens |
| [docs/THEORY.md](docs/THEORY.md) | Theory of shutter speed measurement |
| [docs/HOW_TO.md](docs/HOW_TO.md) | Step-by-step usage guide |
| [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md) | Implementation instructions |
| [IMPLEMENTATION_LOG.md](IMPLEMENTATION_LOG.md) | Implementation progress log |
| [architecture.md](architecture.md) | API documentation and technical details |

---

## Project Structure

```
shutter-analyzer/
├── shutter_analyzer/              # Core Python module
│   ├── __init__.py                # Package initializer with exports
│   ├── __main__.py                # Module entry point
│   ├── video_processor.py         # Video file I/O operations
│   ├── frame_analyzer.py          # Brightness analysis and event detection
│   ├── shutter_calculator.py      # Shutter speed calculations
│   ├── output.py                  # Result formatting and file generation
│   ├── verify.py                  # Manual verification tool
│   └── main.py                    # CLI entry point
├── videos/                        # Input video files
│   └── *.mp4                      # Test videos
├── outputs/                       # Generated outputs (gitignored except examples/)
│   ├── examples/                  # Example outputs for reference
│   └── <video-name>/              # Per-video results
├── docs/                          # Documentation and research
│   └── research/                  # Market research and publishing guides
├── backup/                        # Extracted frames backup (gitignored)
├── test_basic_functionality.py    # Interactive test suite
├── REQUIREMENTS.md                # Project requirements
├── IMPLEMENTATION_PLAN.md         # Implementation instructions
├── IMPLEMENTATION_LOG.md          # Implementation progress log
├── architecture.md                # API documentation
├── pyproject.toml                 # Python project configuration
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

**Class: `FrameBrightnessStats`** - Data class storing brightness statistics:
- `min_brightness`, `max_brightness`, `mean_brightness`, `median_brightness`
- `percentiles` - Dictionary of brightness percentiles
- `baseline` - Closed shutter brightness
- `threshold` - Detection threshold
- `peak_brightness` - Typical fully-open brightness (95th percentile of events)

**Class: `FrameAnalyzer`** - Static methods for frame analysis:
- `calculate_frame_brightness()` - Computes average brightness of a frame
- `analyze_brightness_distribution()` - Computes stats and determines detection threshold
- `is_shutter_open()` - Boolean check against threshold
- `find_shutter_events()` - Identifies sequences of frames where shutter is open
- `find_threshold_using_zscore()` - Z-score statistical threshold detection
- `find_threshold_using_dbscan()` - DBSCAN clustering threshold detection
- `calculate_peak_brightness()` - Calculates peak from event data
- `analyze_video_and_find_events()` - Two-pass analysis (brightness stats then events)

---

### `shutter_analyzer/shutter_calculator.py`
Calculates shutter speeds from detected events.

**Class: `ShutterEvent`** - Data class representing a shutter open/close event:
- `start_frame`, `end_frame` - Event boundaries
- `duration_frames` - Number of frames shutter was open
- `weighted_duration_frames` - Weighted count based on partial-open frames
- `baseline_brightness`, `peak_brightness` - Reference values for weighting
- `max_brightness`, `avg_brightness` - Brightness metrics

**Class: `ShutterSpeedCalculator`** - Static methods:
- `calculate_duration_seconds()` - Converts frame count to duration (supports slow-motion)
- `calculate_shutter_speed()` - Calculates speed as fraction (1/x), supports weighted duration
- `compare_with_expected()` - Computes percentage error
- `group_shutter_events()` - Matches events to expected speed settings

---

### `shutter_analyzer/output.py`
Result formatting and file generation.

**Functions:**
- `get_output_dir()` - Creates outputs/<video-stem>/ directory
- `get_variation_color()` - ANSI color codes for terminal display
- `format_results_table()` - Formats terminal table with color-coded variations
- `generate_results_markdown()` - Generates markdown report with inline colors
- `save_results_markdown()` - Saves report to results.md

---

### `shutter_analyzer/main.py`
Main CLI entry point with two-stage workflow.

**Usage:**
```bash
uv run python -m shutter_analyzer <video_path> [options]
```

**Options:**
- `--method` - Detection method: original (default), zscore, dbscan
- `--recording-fps` - Actual recording FPS for slow-motion videos
- `--events` - Expected event count (required for zscore/dbscan)

**Workflow:**
1. **Stage 1:** Analyze video, detect events, display results table
2. **Stage 2:** Optionally enter expected speeds for comparison

**Example:**
```bash
uv run python -m shutter_analyzer videos/camera_test.mp4
# Shows detected events, saves timeline and results.md
# Prompts for expected speeds (e.g., "1/500, 1/250, 1/125")
```

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

### `shutter_analyzer/verify.py`
Manual verification tool for validating event detection visually.

**Usage:**
```bash
# Interactive mode - step through events
uv run python -m shutter_analyzer.verify <video_path> [--method original]

# Montage mode - generate frame images for each event
uv run python -m shutter_analyzer.verify <video_path> --montage
```

**Interactive Mode Features:**
- Side-by-side frame display: frame before event vs first event frame
- Annotates frames with frame number and brightness value
- Press any key to advance, 'q' to quit

**Montage Mode Features:**
- Generates PNG showing all frames in each event
- Includes BEFORE and AFTER context frames (blue labels) to verify event boundaries
- Each event frame labeled with brightness weight (FULL ≥95%, or percentage)
- Color coded: green (FULL), orange (50-95%), red (<50%), blue (context)
- Visualizes shutter opening pattern: dark → partial → full → partial → dark
- Output: `outputs/<video-name>/event_XX_frames.png`

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

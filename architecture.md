# Camera Shutter Speed Analyzer

A Python tool for measuring actual camera shutter speeds from video recordings of shutter openings.

## Project Overview

This tool analyzes a video showing a camera's shutter opening at different speed settings (1/500, 1/250, 1/2, etc.). The video is expected to show mostly dark frames with a bright spot appearing when the shutter opens. The tool will:

1. Process the video frame by frame
2. Detect when the shutter opens and closes based on overall frame brightness
3. Calculate the actual shutter speed based on the duration the shutter is open
4. Compare the measured shutter speeds with the expected values
5. Generate visualizations and reports of the results
6. Provide debugging tools to verify the accuracy of detection

## Architecture

The project follows a modular design with clean separation of concerns:

### 1. Video Processing (`video_processor.py`)

**Classes:**
- `VideoReader`: Manages video file operations.

**Key Functions:**
- `open_video(file_path)`: Opens a video file for processing.
- `get_video_properties(video)`: Returns properties like frame rate, frame count, etc.
- `read_frames(video)`: Generator that yields frames one by one.
- `get_frame_at_index(video, index)`: Gets a specific frame by its index (for debugging).

### 2. Frame Analysis (`frame_analyzer.py`)

**Classes:**
- `FrameAnalyzer`: Analyzes frames for brightness and shutter state.

**Key Functions:**
- `calculate_frame_brightness(frame)`: Calculates the overall brightness of a frame.
- `is_shutter_open(frame, threshold)`: Determines if the shutter is open based on brightness.
- `find_shutter_events(frames, threshold)`: Finds sequences of frames where the shutter is open.

### 3. Shutter Speed Calculation (`shutter_calculator.py`)

**Classes:**
- `ShutterEvent`: Represents a single shutter opening/closing event.
  - Attributes: `start_frame`, `end_frame`, `brightness_values`
- `ShutterSpeedCalculator`: Calculates and analyzes shutter speeds.

**Key Functions:**
- `calculate_duration_seconds(start_frame, end_frame, fps)`: Calculates duration in seconds.
- `calculate_shutter_speed(shutter_event, fps)`: Calculates the effective shutter speed.
- `compare_with_expected(measured, expected)`: Compares measured vs. expected speeds.
- `group_shutter_events(shutter_events)`: Groups events by expected speed settings.

### 4. Visualization (`visualizer.py`)

**Classes:**
- `ShutterVisualizer`: Creates various visualizations of the results.

**Key Functions:**
- `plot_brightness_timeline(brightnesses, shutter_events)`: Plots brightness over time.
- `plot_speed_comparison(measured_speeds, expected_speeds)`: Creates a comparison chart.
- `generate_report(results)`: Generates a detailed report of the findings.
- `save_figures(output_dir)`: Saves visualization figures to disk.

### 5. Debugging and Verification (`debug_utils.py`)

**Classes:**
- `DebugVisualizer`: Handles debugging visualizations.

**Key Functions:**
- `save_boundary_frames(video, shutter_event, frame_window=5)`: Saves frames around shutter open/close events.
- `annotate_frame(frame, is_open, brightness_value)`: Adds text overlay with debug info.
- `create_frame_montage(frames, labels)`: Creates a visual montage of multiple frames.
- `visualize_brightness_analysis(video_path, brightness_values, threshold, events)`: Comprehensive visualization.
- `preview_analysis(video_path, num_frames, threshold)`: Quick preview of brightness analysis.

### 6. Main Application (`main.py`)

**Classes:**
- `ShutterSpeedAnalyzer`: Main application class that coordinates the entire workflow.

**Key Functions:**
- `analyze_video(video_path, expected_speeds, threshold, return_events=False)`: Main analysis function.
- `parse_arguments()`: Parses command-line arguments.
- `validate_input(video_path, expected_speeds)`: Validates input parameters.
- `main()`: Entry point for the application.

## Implementation Steps

1. **Initial Setup**:
   - Set up project structure and dependencies
   - Create a simple test script to verify video access

2. **Core Functionality**:
   - Implement `VideoReader` for frame extraction
   - Implement `FrameAnalyzer` with basic brightness detection
   - Create `ShutterEvent` data structure
   - Implement event detection logic

3. **Verification and Tuning**:
   - Implement `DebugVisualizer` to verify detection accuracy
   - Add preview function to test threshold values
   - Refine detection algorithm based on testing

4. **Analysis and Reporting**:
   - Implement `ShutterSpeedCalculator` for measurements
   - Create visualization tools for results
   - Build report generation

5. **CLI and Integration**:
   - Implement command-line interface
   - Integrate all components
   - Add configuration options

## Usage Example

```python
from shutter_analyzer.main import ShutterSpeedAnalyzer
from shutter_analyzer.debug_utils import DebugVisualizer

# Create the analyzer
analyzer = ShutterSpeedAnalyzer()

# Define expected shutter speeds (in seconds)
expected_speeds = [1/500, 1/250, 1/125, 1/60, 1/30, 1/15, 1/8, 1/4, 1/2, 1]

# Preview analysis to determine appropriate threshold
debugger = DebugVisualizer(output_dir='debug_output')
brightnesses = debugger.preview_analysis(
    video_path='shutter_test.mp4',
    num_frames=200,
    threshold=50  # Initial threshold to test
)

# Run the full analysis
results, shutter_events = analyzer.analyze_video(
    video_path='shutter_test.mp4',
    expected_speeds=expected_speeds,
    brightness_threshold=50,  # Adjusted based on preview
    return_events=True  # Return the raw events for debugging
)

# Verify detection accuracy
debugger.visualize_brightness_analysis(
    video_path='shutter_test.mp4',
    brightness_values=brightnesses,
    threshold=50,
    shutter_events=shutter_events
)

# Save boundary frames for visual inspection
for i, event in enumerate(shutter_events):
    debugger.save_boundary_frames(
        video_path='shutter_test.mp4',
        shutter_event=event,
        event_id=i,
        frame_window=3  # Save 3 frames before and after boundary
    )

# Print results
for i, (expected, measured) in enumerate(results):
    error_pct = (measured - expected) / expected * 100
    print(f"Test #{i+1}: Expected: 1/{1/expected:.0f}s, Measured: 1/{1/measured:.1f}s, Error: {error_pct:.1f}%")

# Generate and save visualizations
analyzer.generate_visualizations(output_dir='results')
```

## Dependencies

- OpenCV (`cv2`): For video processing
- NumPy: For numerical operations
- Matplotlib: For visualization
- Pandas: For data manipulation and analysis (optional)
- tqdm: For progress bars (optional)

## Project Structure

```
shutter_analyzer/
├── __init__.py
├── main.py                  # Main application
├── video_processor.py       # Video handling
├── frame_analyzer.py        # Frame analysis
├── shutter_calculator.py    # Shutter speed calculations
├── visualizer.py            # Visualization tools
├── debug_utils.py           # Debugging utilities
└── tests/                   # Test scripts
    ├── __init__.py
    ├── test_video_processor.py
    ├── test_frame_analyzer.py
    ├── test_shutter_calculator.py
    └── test_data/           # Sample test videos
```
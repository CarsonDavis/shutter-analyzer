"""
Test script to verify the basic functionality of the Camera Shutter Speed Analyzer.

This script tests:
1. Reading a video file
2. Calculating frame brightness
3. Analyzing brightness distribution
4. Detecting shutter events with adaptive thresholding
5. Creating ShutterEvent objects
"""

import sys
import os
import cv2
import numpy as np
import matplotlib.pyplot as plt
from typing import List, Tuple

# Add the parent directory to sys.path to import our modules
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from shutter_analyzer.video_processor import VideoReader
from shutter_analyzer.frame_analyzer import FrameAnalyzer, FrameBrightnessStats
from shutter_analyzer.shutter_calculator import ShutterEvent, ShutterSpeedCalculator


def test_video_access(video_path: str) -> None:
    """
    Test if we can open a video file and get its properties.

    Args:
        video_path: Path to the test video file
    """
    print(f"Testing video access for: {video_path}")

    try:
        video = VideoReader.open_video(video_path)
        properties = VideoReader.get_video_properties(video)

        print("Video properties:")
        for key, value in properties.items():
            print(f"  {key}: {value}")

        # Get a sample frame
        sample_frame = VideoReader.get_frame_at_index(video, 0)
        if sample_frame is not None:
            print(
                f"Successfully retrieved first frame with shape: {sample_frame.shape}"
            )

        video.release()
        print("Video access test: PASSED")
    except Exception as e:
        print(f"Video access test: FAILED - {str(e)}")


def test_brightness_calculation(video_path: str) -> None:
    """
    Test brightness calculation on a few frames.

    Args:
        video_path: Path to the test video file
    """
    print("\nTesting brightness calculation...")

    try:
        video = VideoReader.open_video(video_path)

        # Sample a few frames
        frames_to_test = [0, 10, 20, 30, 40]
        brightness_values = []

        for frame_idx in frames_to_test:
            frame = VideoReader.get_frame_at_index(video, frame_idx)
            if frame is not None:
                brightness = FrameAnalyzer.calculate_frame_brightness(frame)
                brightness_values.append((frame_idx, brightness))

        print("Frame brightness values:")
        for idx, brightness in brightness_values:
            print(f"  Frame {idx}: Brightness = {brightness:.2f}")

        video.release()
        print("Brightness calculation test: PASSED")
    except Exception as e:
        print(f"Brightness calculation test: FAILED - {str(e)}")


def test_brightness_distribution(
    video_path: str,
) -> Tuple[FrameBrightnessStats, List[float]]:
    """
    Test brightness distribution analysis.

    Args:
        video_path: Path to the test video file

    Returns:
        Tuple of (brightness_stats, brightness_values)
    """
    print("\nTesting brightness distribution analysis...")

    try:
        video = VideoReader.open_video(video_path)
        frames_generator = VideoReader.read_frames(video)

        # Analyze brightness distribution
        brightness_stats, brightness_values = (
            FrameAnalyzer.analyze_brightness_distribution(
                frames_generator, percentile_threshold=25, margin_factor=1.5
            )
        )

        print("Brightness statistics:")
        print(f"  Min: {brightness_stats.min_brightness:.2f}")
        print(f"  Max: {brightness_stats.max_brightness:.2f}")
        print(f"  Mean: {brightness_stats.mean_brightness:.2f}")
        print(f"  Median: {brightness_stats.median_brightness:.2f}")
        print(f"  Percentile 25%: {brightness_stats.percentiles[25]:.2f}")
        print(f"  Percentile 75%: {brightness_stats.percentiles[75]:.2f}")
        print(f"  Baseline (closed shutter): {brightness_stats.baseline:.2f}")
        print(f"  Calculated threshold: {brightness_stats.threshold:.2f}")

        # Plot brightness histogram
        plt.figure(figsize=(10, 6))
        plt.hist(brightness_values, bins=50, alpha=0.7)
        plt.axvline(
            x=brightness_stats.baseline,
            color="r",
            linestyle="--",
            label=f"Baseline ({brightness_stats.baseline:.2f})",
        )
        plt.axvline(
            x=brightness_stats.threshold,
            color="g",
            linestyle="--",
            label=f"Threshold ({brightness_stats.threshold:.2f})",
        )
        plt.title("Frame Brightness Distribution")
        plt.xlabel("Brightness")
        plt.ylabel("Frequency")
        plt.legend()
        plt.grid(True, alpha=0.3)

        # Save or display the histogram
        histogram_path = "brightness_histogram.png"
        plt.savefig(histogram_path)
        print(f"Brightness histogram saved to: {histogram_path}")

        video.release()
        print("Brightness distribution analysis test: PASSED")
        return brightness_stats, brightness_values
    except Exception as e:
        print(f"Brightness distribution analysis test: FAILED - {str(e)}")
        return None, []


def test_adaptive_event_detection(
    video_path: str,
    brightness_values: List[float] = None,
    brightness_stats: FrameBrightnessStats = None,
) -> List[Tuple[int, int, List[float]]]:
    """
    Test detection of shutter events using adaptive thresholding.

    Args:
        video_path: Path to the test video file
        brightness_values: Pre-calculated brightness values (optional)
        brightness_stats: Pre-calculated brightness statistics (optional)

    Returns:
        List of detected shutter events
    """
    print(f"\nTesting adaptive shutter event detection...")

    try:
        # If brightness values and stats are not provided, calculate them
        if brightness_values is None or brightness_stats is None:
            video = VideoReader.open_video(video_path)
            frames_generator = VideoReader.read_frames(video)
            brightness_stats, brightness_values = (
                FrameAnalyzer.analyze_brightness_distribution(
                    frames_generator, percentile_threshold=25, margin_factor=1.5
                )
            )
            video.release()

        # Find shutter events
        events = FrameAnalyzer.find_shutter_events(
            brightness_values, brightness_stats.threshold
        )

        print(
            f"Detected {len(events)} shutter events using adaptive threshold {brightness_stats.threshold:.2f}:"
        )
        for i, (start, end, brightnesses) in enumerate(events):
            duration = end - start + 1
            max_brightness = max(brightnesses)
            print(
                f"  Event #{i+1}: Frames {start}-{end} (Duration: {duration} frames, Max brightness: {max_brightness:.2f})"
            )

        # Plot brightness timeline with event markers
        plt.figure(figsize=(12, 6))
        plt.plot(brightness_values, "b-", alpha=0.5)
        plt.axhline(
            y=brightness_stats.threshold,
            color="r",
            linestyle="--",
            label=f"Threshold ({brightness_stats.threshold:.2f})",
        )

        # Highlight events
        for start, end, _ in events:
            plt.axvspan(start, end, alpha=0.2, color="green")

        plt.title("Frame Brightness Timeline with Detected Events")
        plt.xlabel("Frame Number")
        plt.ylabel("Brightness")
        plt.legend()
        plt.grid(True, alpha=0.3)

        # Save or display the timeline
        timeline_path = "brightness_timeline.png"
        plt.savefig(timeline_path)
        print(f"Brightness timeline with events saved to: {timeline_path}")

        print("Adaptive event detection test: PASSED")
        return events
    except Exception as e:
        print(f"Adaptive event detection test: FAILED - {str(e)}")
        return []


def test_shutter_speed_calculation(
    video_path: str,
    events: List[Tuple[int, int, List[float]]] = None,
    expected_speeds: List[float] = None,
) -> None:
    """
    Test shutter speed calculation using detected events.

    Args:
        video_path: Path to the test video file
        events: Pre-detected shutter events (optional)
        expected_speeds: List of expected shutter speeds (in seconds)
    """
    if expected_speeds is None:
        # Default expected speeds if none provided
        expected_speeds = [1 / 500, 1 / 250, 1 / 125, 1 / 60, 1 / 30]

    print(f"\nTesting shutter speed calculation...")

    try:
        video = VideoReader.open_video(video_path)
        properties = VideoReader.get_video_properties(video)
        fps = properties["fps"]

        # If events are not provided, detect them using adaptive thresholding
        if events is None:
            # Reset video to start
            video.set(cv2.CAP_PROP_POS_FRAMES, 0)
            frames_generator = VideoReader.read_frames(video)
            brightness_stats, events = FrameAnalyzer.analyze_video_and_find_events(
                frames_generator
            )

        # Create ShutterEvent objects
        shutter_events = [
            ShutterEvent(start, end, brightness) for start, end, brightness in events
        ]

        # Calculate speeds for each event
        print(
            f"Calculating shutter speeds for {len(shutter_events)} events at {fps:.2f} FPS:"
        )

        for i, event in enumerate(shutter_events[: len(expected_speeds)]):
            measured = ShutterSpeedCalculator.calculate_shutter_speed(event, fps)
            expected = expected_speeds[i] if i < len(expected_speeds) else None

            if expected is not None:
                error = ShutterSpeedCalculator.compare_with_expected(measured, expected)
                print(
                    f"  Event #{i+1}: Expected: 1/{1/expected:.0f}s, Measured: 1/{1/measured:.1f}s, Error: {error:.1f}%"
                )
            else:
                print(f"  Event #{i+1}: Measured: 1/{1/measured:.1f}s")

        video.release()
        print("Shutter speed calculation test: PASSED")
    except Exception as e:
        print(f"Shutter speed calculation test: FAILED - {str(e)}")


def run_analysis_pipeline(
    video_path: str,
    method: str,
    expected_events_count: int,
    expected_speeds: List[float] = None,
) -> None:
    """
    Run the complete analysis pipeline for a specific method.

    Args:
        video_path: Path to the test video file
        method: Method to use ("original", "zscore", or "dbscan")
        expected_events_count: Expected number of shutter events
        expected_speeds: List of expected shutter speeds (in seconds)
    """
    print(f"\n{'-'*80}\nRunning analysis using {method.upper()} method\n{'-'*80}")

    # Open video and get frames
    video = VideoReader.open_video(video_path)
    properties = VideoReader.get_video_properties(video)
    video.set(cv2.CAP_PROP_POS_FRAMES, 0)
    frames_generator = VideoReader.read_frames(video)

    # Analyze brightness distribution with the selected method
    brightness_stats, brightness_values = FrameAnalyzer.analyze_brightness_distribution(
        frames_generator, method=method, expected_events_count=expected_events_count
    )

    print(f"Brightness statistics ({method} method):")
    print(f"  Min: {brightness_stats.min_brightness:.2f}")
    print(f"  Max: {brightness_stats.max_brightness:.2f}")
    print(f"  Threshold: {brightness_stats.threshold:.2f}")

    # Find shutter events
    events = FrameAnalyzer.find_shutter_events(
        brightness_values, brightness_stats.threshold
    )

    print(
        f"Detected {len(events)} shutter events using {method} method (threshold {brightness_stats.threshold:.2f}):"
    )
    for i, (start, end, brightnesses) in enumerate(events[:10]):  # Show first 10 events
        duration = end - start + 1
        max_brightness = max(brightnesses) if brightnesses else 0
        print(
            f"  Event #{i+1}: Frames {start}-{end} (Duration: {duration} frames, Max: {max_brightness:.2f})"
        )

    if len(events) > 10:
        print(f"  ... and {len(events)-10} more events")

    # Plot brightness timeline with events
    plt.figure(figsize=(12, 6))
    plt.plot(brightness_values, "b-", alpha=0.5)
    plt.axhline(
        y=brightness_stats.threshold,
        color="r",
        linestyle="--",
        label=f"Threshold ({brightness_stats.threshold:.2f})",
    )

    # Highlight events
    for start, end, _ in events:
        plt.axvspan(start, end, alpha=0.2, color="green")

    plt.title(f"Frame Brightness Timeline ({method} method)")
    plt.xlabel("Frame Number")
    plt.ylabel("Brightness")
    plt.legend()
    plt.grid(True, alpha=0.3)

    # Save the timeline
    timeline_path = f"brightness_timeline_{method}.png"
    plt.savefig(timeline_path)
    print(f"Timeline saved to: {timeline_path}")

    # Test shutter speed calculation if expected speeds provided
    if expected_speeds and len(events) > 0:
        print(f"\nShutter speed calculation using {method} method:")
        shutter_events = [
            ShutterEvent(start, end, b)
            for start, end, b in events[: len(expected_speeds)]
        ]

        for i, (event, expected) in enumerate(zip(shutter_events, expected_speeds)):
            if i >= len(shutter_events):
                break

            measured = ShutterSpeedCalculator.calculate_shutter_speed(
                event, properties["fps"]
            )
            error = ShutterSpeedCalculator.compare_with_expected(measured, expected)
            print(
                f"  Event #{i+1}: Expected: 1/{1/expected:.0f}s, Measured: 1/{1/measured:.1f}s, Error: {error:.1f}%"
            )

    video.release()


def main() -> None:
    """Main function to run all tests."""
    # Path to the test video file
    print("test")
    video_path = input("Enter path to test video file: ")

    # Run basic tests
    test_video_access(video_path)
    test_brightness_calculation(video_path)

    # Ask for expected event count (required)
    while True:
        try:
            expected_events_count = int(
                input("Enter the number of expected shutter events: ")
            )
            if expected_events_count > 0:
                break
            else:
                print("Please enter a positive number.")
        except ValueError:
            print("Please enter a valid number.")

    # Ask for expected speeds
    use_default = input("Use default expected speeds? (y/n): ").lower() == "y"

    if use_default:
        expected_speeds = [
            1 / 500,
            1 / 250,
            1 / 125,
            1 / 60,
            1 / 30,
            1 / 15,
            1 / 8,
            1 / 4,
            1 / 2,
            1,
        ]
    else:
        speeds_input = input(
            "Enter expected speeds as fractions separated by commas (e.g., 1/500,1/250,...): "
        )
        speeds_fractions = speeds_input.split(",")
        expected_speeds = []

        for fraction in speeds_fractions:
            try:
                if "/" in fraction:
                    num, denom = fraction.split("/")
                    expected_speeds.append(float(num) / float(denom))
                else:
                    expected_speeds.append(float(fraction))
            except ValueError:
                print(f"Invalid speed fraction: {fraction}. Skipping.")

    # Run the complete analysis pipeline for each method
    run_analysis_pipeline(
        video_path, "original", expected_events_count, expected_speeds
    )
    run_analysis_pipeline(video_path, "zscore", expected_events_count, expected_speeds)
    run_analysis_pipeline(video_path, "dbscan", expected_events_count, expected_speeds)


if __name__ == "__main__":
    main()

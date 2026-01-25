"""Main CLI entry point for Shutter Analyzer.

This module provides the command-line interface for analyzing camera shutter speeds
from video recordings.

Usage:
    uv run python -m shutter_analyzer <video_path> [options]

The workflow has two stages:
    1. Analysis: Detect shutter events and display measured speeds
    2. Comparison (optional): Enter expected speeds to see variation percentages
"""

import argparse
import sys
from pathlib import Path
from typing import List, Optional, Tuple

import cv2
import matplotlib.pyplot as plt
import numpy as np

from .frame_analyzer import FrameAnalyzer, FrameBrightnessStats
from .output import (
    format_results_table,
    generate_results_markdown,
    get_output_dir,
    save_results_markdown,
)
from .shutter_calculator import ShutterEvent, ShutterSpeedCalculator
from .video_processor import VideoReader


def parse_shutter_speeds(input_string: str) -> List[float]:
    """
    Parse shutter speeds from fraction notation.

    Args:
        input_string: Comma-separated fractions like "1/500, 1/250, 1/125"

    Returns:
        List of speeds in seconds (e.g., [0.002, 0.004, 0.008])

    Examples:
        >>> parse_shutter_speeds("1/500, 1/250, 1/125")
        [0.002, 0.004, 0.008]
        >>> parse_shutter_speeds("1/60, 1/30, 1")
        [0.0167, 0.0333, 1.0]
    """
    speeds = []
    for part in input_string.split(","):
        part = part.strip()
        if not part:
            continue
        if "/" in part:
            num, denom = part.split("/")
            speeds.append(float(num) / float(denom))
        else:
            speeds.append(float(part))
    return speeds


def analyze_video(
    video_path: str,
    method: str = "original",
    recording_fps: Optional[float] = None,
    expected_events_count: Optional[int] = None,
) -> Tuple[List[ShutterEvent], float, FrameBrightnessStats, List[float]]:
    """
    Analyze a video file to detect shutter events.

    Args:
        video_path: Path to the video file
        method: Detection method ("original", "zscore", or "dbscan")
        recording_fps: Actual recording FPS for slow-motion videos
        expected_events_count: Expected number of events (for zscore/dbscan methods)

    Returns:
        Tuple of (events, fps, stats, brightness_values)
    """
    # Open video
    video = VideoReader.open_video(video_path)
    props = VideoReader.get_video_properties(video)

    fps = props["fps"]
    print(f"\nAnalyzing video: {Path(video_path).name}")
    print(f"  Frames: {props['frame_count']} @ {fps:.2f} fps")
    print(f"  Duration: {props['duration']:.1f} seconds")
    print(f"  Method: {method}")

    if recording_fps:
        print(f"  Recording FPS: {recording_fps}")

    # Run analysis
    video.set(cv2.CAP_PROP_POS_FRAMES, 0)
    frames_gen = VideoReader.read_frames(video)

    print("\nDetecting shutter events...")
    stats, raw_events = FrameAnalyzer.analyze_video_and_find_events(
        frames_gen, method=method, expected_events_count=expected_events_count
    )

    # Convert raw events to ShutterEvent objects
    events = []
    for start, end, brightness_vals in raw_events:
        event = ShutterEvent(
            start,
            end,
            brightness_vals,
            baseline_brightness=stats.baseline,
            peak_brightness=stats.peak_brightness,
        )
        events.append(event)

    # Also collect all brightness values for timeline plot
    video.set(cv2.CAP_PROP_POS_FRAMES, 0)
    brightness_values = []
    for _, frame in VideoReader.read_frames(video):
        brightness = FrameAnalyzer.calculate_frame_brightness(frame)
        brightness_values.append(brightness)

    video.release()

    print(f"  Found {len(events)} shutter events")
    print(f"  Baseline: {stats.baseline:.2f}")
    print(f"  Threshold: {stats.threshold:.2f}")
    if stats.peak_brightness:
        print(f"  Peak brightness: {stats.peak_brightness:.2f}")

    return events, fps, stats, brightness_values


def save_timeline_plot(
    brightness_values: List[float],
    events: List[ShutterEvent],
    threshold: float,
    output_path: Path,
    fps: float,
) -> None:
    """
    Save brightness timeline plot with detected events.

    Args:
        brightness_values: List of brightness values for each frame
        events: List of ShutterEvent objects
        threshold: Detection threshold
        output_path: Path to save the plot
        fps: Frames per second for time axis
    """
    fig, ax = plt.subplots(figsize=(14, 5))

    # Create time axis
    times = np.arange(len(brightness_values)) / fps

    # Plot brightness over time
    ax.plot(times, brightness_values, "b-", linewidth=0.5, alpha=0.7)

    # Add threshold line
    ax.axhline(y=threshold, color="r", linestyle="--", label=f"Threshold ({threshold:.1f})")

    # Highlight detected events
    for event in events:
        start_time = event.start_frame / fps
        end_time = event.end_frame / fps
        ax.axvspan(start_time, end_time, alpha=0.3, color="green")

    ax.set_xlabel("Time (seconds)")
    ax.set_ylabel("Brightness")
    ax.set_title("Brightness Timeline with Detected Shutter Events")
    ax.legend()
    ax.grid(True, alpha=0.3)

    plt.tight_layout()
    plt.savefig(output_path, dpi=150)
    plt.close()


def main() -> None:
    """Main CLI entry point."""
    parser = argparse.ArgumentParser(
        description="Analyze camera shutter speeds from video recordings",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
    uv run python -m shutter_analyzer videos/camera_test.mp4
    uv run python -m shutter_analyzer videos/test.mp4 --method zscore --events 8
    uv run python -m shutter_analyzer videos/slowmo.mp4 --recording-fps 240
        """,
    )
    parser.add_argument("video", help="Path to video file")
    parser.add_argument(
        "--method",
        choices=["original", "zscore", "dbscan"],
        default="original",
        help="Detection method (default: original)",
    )
    parser.add_argument(
        "--recording-fps",
        type=float,
        help="Actual recording FPS for slow-motion videos",
    )
    parser.add_argument(
        "--events",
        type=int,
        help="Expected number of events (required for zscore/dbscan methods)",
    )

    args = parser.parse_args()

    # Validate video file exists
    if not Path(args.video).exists():
        print(f"Error: Video file not found: {args.video}")
        sys.exit(1)

    # Validate method requirements
    if args.method in ["zscore", "dbscan"] and args.events is None:
        print(f"Error: --events is required when using {args.method} method")
        sys.exit(1)

    # Stage 1: Analyze
    events, fps, stats, brightness_values = analyze_video(
        args.video,
        method=args.method,
        recording_fps=args.recording_fps,
        expected_events_count=args.events,
    )

    if not events:
        print("\nNo shutter events detected. Try adjusting detection parameters.")
        sys.exit(0)

    # Get output directory
    output_dir = get_output_dir(args.video)

    # Save timeline plot
    timeline_path = output_dir / "brightness_timeline.png"
    save_timeline_plot(brightness_values, events, stats.threshold, timeline_path, fps)
    print(f"\nTimeline saved to: {timeline_path}")

    # Display initial results
    print("\n" + "=" * 50)
    print("RESULTS")
    print("=" * 50)
    print(format_results_table(events, fps, args.recording_fps))
    print()

    # Stage 2: Optional comparison
    print("Enter expected speeds to compare (e.g., '1/500, 1/250, 1/125')")
    print("Press Enter to skip:")

    try:
        speed_input = input("> ").strip()
    except (EOFError, KeyboardInterrupt):
        speed_input = ""

    if speed_input:
        try:
            expected_speeds = parse_shutter_speeds(speed_input)
            print("\n" + "=" * 50)
            print("COMPARISON RESULTS")
            print("=" * 50)
            print(format_results_table(events, fps, args.recording_fps, expected_speeds))

            # Generate and save markdown report
            md_content = generate_results_markdown(
                args.video, events, fps, args.recording_fps, expected_speeds
            )
            results_path = save_results_markdown(output_dir, md_content)
            print(f"\nResults saved to: {results_path}")
        except ValueError as e:
            print(f"Error parsing speeds: {e}")
    else:
        # Save basic results without comparison
        md_content = generate_results_markdown(args.video, events, fps, args.recording_fps)
        results_path = save_results_markdown(output_dir, md_content)
        print(f"Results saved to: {results_path}")


if __name__ == "__main__":
    main()

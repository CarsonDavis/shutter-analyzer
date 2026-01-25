"""Manual verification tool for event detection.

This module provides a visual tool to validate that shutter events are being
detected correctly. It displays side-by-side comparisons of frames at event
boundaries, allowing quick visual confirmation that thresholds are working.

Usage:
    uv run python -m shutter_analyzer.verify videos/test.mp4 [--method original]
"""

import argparse
import cv2
import numpy as np
from pathlib import Path
from typing import List, Tuple

from .video_processor import VideoReader
from .frame_analyzer import FrameAnalyzer


def annotate_frame(
    frame: np.ndarray, frame_num: int, brightness: float, label: str = ""
) -> np.ndarray:
    """
    Add frame number and brightness annotation to frame.

    Args:
        frame: Video frame (numpy array)
        frame_num: Frame index number
        brightness: Brightness value for this frame
        label: Optional label (e.g., "BEFORE", "EVENT START")

    Returns:
        Annotated frame copy
    """
    annotated = frame.copy()
    h, w = annotated.shape[:2]

    # Add dark background bar at bottom
    cv2.rectangle(annotated, (0, h - 60), (w, h), (0, 0, 0), -1)

    # Build text
    text = f"Frame {frame_num} | Brightness: {brightness:.1f}"
    if label:
        text = f"{label}: {text}"

    # Add text
    cv2.putText(
        annotated,
        text,
        (10, h - 20),
        cv2.FONT_HERSHEY_SIMPLEX,
        0.7,
        (255, 255, 255),
        2,
    )

    return annotated


def create_side_by_side(
    frame_before: np.ndarray,
    frame_start: np.ndarray,
    num_before: int,
    num_start: int,
    bright_before: float,
    bright_start: float,
) -> np.ndarray:
    """
    Create side-by-side comparison image.

    Args:
        frame_before: Frame before the event
        frame_start: First frame of the event
        num_before: Frame number of the before frame
        num_start: Frame number of the start frame
        bright_before: Brightness of the before frame
        bright_start: Brightness of the start frame

    Returns:
        Combined side-by-side image
    """
    # Annotate both frames
    left = annotate_frame(frame_before, num_before, bright_before, "BEFORE")
    right = annotate_frame(frame_start, num_start, bright_start, "EVENT START")

    # Add colored border to distinguish
    # Gray border for "before" frame
    left = cv2.copyMakeBorder(
        left, 5, 5, 5, 5, cv2.BORDER_CONSTANT, value=(100, 100, 100)
    )
    # Green border for "event start" frame
    right = cv2.copyMakeBorder(
        right, 5, 5, 5, 5, cv2.BORDER_CONSTANT, value=(0, 255, 0)
    )

    # Stack horizontally
    return np.hstack([left, right])


def verify_events(video_path: str, method: str = "original") -> None:
    """
    Interactive verification of detected events.

    Shows frame before event and first frame of event side-by-side.
    User presses any key to advance, 'q' to quit.

    Args:
        video_path: Path to the video file
        method: Detection method ("original", "zscore", or "dbscan")
    """
    # Open video
    video = VideoReader.open_video(video_path)
    props = VideoReader.get_video_properties(video)

    print(f"Analyzing {video_path}...")
    print(f"  {props['frame_count']} frames @ {props['fps']} fps")
    print(f"  Duration: {props['duration']:.1f} seconds")
    print(f"  Method: {method}")

    # Run analysis - first pass to get brightness distribution
    video.set(cv2.CAP_PROP_POS_FRAMES, 0)
    frames_gen = VideoReader.read_frames(video)

    # For zscore/dbscan methods, we need expected_events_count
    # Since this is a verification tool, we'll use original method by default
    # or ask user for expected count if using other methods
    expected_count = None
    if method in ["zscore", "dbscan"]:
        try:
            expected_count = int(input("Enter expected number of events: "))
        except ValueError:
            print("Invalid input, using original method instead.")
            method = "original"

    stats, brightness_values = FrameAnalyzer.analyze_brightness_distribution(
        frames_gen, method=method, expected_events_count=expected_count
    )

    # Find events
    events = FrameAnalyzer.find_shutter_events(brightness_values, stats.threshold)

    print(f"\nFound {len(events)} events. Threshold: {stats.threshold:.1f}")
    print(f"Baseline: {stats.baseline:.1f}")
    print(f"Press any key to step through events, 'q' to quit.\n")

    if len(events) == 0:
        print("No events detected. Try adjusting threshold parameters.")
        video.release()
        return

    # Step through events
    for i, (start, end, event_brightness) in enumerate(events):
        # Get frame before event (or frame 0 if event starts at beginning)
        before_idx = max(0, start - 1)

        frame_before = VideoReader.get_frame_at_index(video, before_idx)
        frame_start = VideoReader.get_frame_at_index(video, start)

        if frame_before is None or frame_start is None:
            print(f"Could not read frames for event {i + 1}")
            continue

        # Create comparison image
        comparison = create_side_by_side(
            frame_before,
            frame_start,
            before_idx,
            start,
            brightness_values[before_idx],
            brightness_values[start],
        )

        # Resize if too large for screen
        max_width = 1600
        if comparison.shape[1] > max_width:
            scale = max_width / comparison.shape[1]
            new_size = (int(comparison.shape[1] * scale), int(comparison.shape[0] * scale))
            comparison = cv2.resize(comparison, new_size)

        # Display info
        duration = end - start + 1
        print(f"Event {i + 1}/{len(events)}: Frames {start}-{end} (duration: {duration} frames)")
        print(f"  Before (frame {before_idx}): brightness {brightness_values[before_idx]:.1f}")
        print(f"  Start  (frame {start}): brightness {brightness_values[start]:.1f}")
        print(f"  Delta: {brightness_values[start] - brightness_values[before_idx]:.1f}")
        print()

        # Show window
        window_name = f"Event {i + 1}/{len(events)}"
        cv2.imshow(window_name, comparison)

        key = cv2.waitKey(0) & 0xFF
        cv2.destroyAllWindows()

        if key == ord("q"):
            print("Quitting verification.")
            break

    video.release()
    print("Verification complete.")


def main() -> None:
    """CLI entry point for the verification tool."""
    parser = argparse.ArgumentParser(
        description="Verify shutter event detection visually"
    )
    parser.add_argument("video", help="Path to video file")
    parser.add_argument(
        "--method",
        choices=["original", "zscore", "dbscan"],
        default="original",
        help="Detection method (default: original)",
    )
    args = parser.parse_args()

    # Check if video exists
    if not Path(args.video).exists():
        print(f"Error: Video file not found: {args.video}")
        return

    verify_events(args.video, args.method)


if __name__ == "__main__":
    main()

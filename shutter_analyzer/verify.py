"""Manual verification tool for event detection.

This module provides visual tools to validate that shutter events are being
detected correctly:

1. Interactive mode: Step through events showing before/after frames
2. Montage mode: Generate PNG images showing all frames in each event
   with brightness weights labeled (FULL, PARTIAL, etc.)

Usage:
    # Interactive verification
    uv run python -m shutter_analyzer.verify videos/test.mp4

    # Generate frame montages for each event
    uv run python -m shutter_analyzer.verify videos/test.mp4 --montage
"""

import argparse
import cv2
import numpy as np
from pathlib import Path
from typing import List, Tuple, Optional

import matplotlib.pyplot as plt

from .video_processor import VideoReader
from .frame_analyzer import FrameAnalyzer
from .output import get_output_dir


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


def generate_event_montages(
    video_path: str,
    method: str = "original",
    max_frames_per_image: int = 20,
    expected_events_count: Optional[int] = None,
) -> List[Path]:
    """
    Generate montage images showing all frames in each detected event.

    Each frame is labeled with its brightness weight (FULL, percentage, etc.)
    to visualize the shutter opening/closing pattern.

    Args:
        video_path: Path to the video file
        method: Detection method ("original", "zscore", or "dbscan")
        max_frames_per_image: Maximum frames to show per montage (longer events
                              show first half + last half)
        expected_events_count: Expected number of events (for zscore/dbscan)

    Returns:
        List of paths to generated montage images
    """
    # Open video and run analysis
    video = VideoReader.open_video(video_path)
    props = VideoReader.get_video_properties(video)

    print(f"Analyzing {video_path}...")
    print(f"  {props['frame_count']} frames @ {props['fps']} fps")

    video.set(cv2.CAP_PROP_POS_FRAMES, 0)
    frames_gen = VideoReader.read_frames(video)
    stats, events = FrameAnalyzer.analyze_video_and_find_events(
        frames_gen, method=method, expected_events_count=expected_events_count
    )

    print(f"  Found {len(events)} events")
    print(f"  Baseline: {stats.baseline:.2f}")
    print(f"  Peak: {stats.peak_brightness:.2f}")

    # Get output directory
    output_dir = get_output_dir(video_path)
    generated_files = []

    peak = stats.peak_brightness
    baseline = stats.baseline

    for event_idx, (start, end, brightness_vals) in enumerate(events):
        n_frames = len(brightness_vals)

        # Determine which event frames to show
        if n_frames <= max_frames_per_image - 2:  # Reserve 2 slots for before/after
            event_indices = list(range(n_frames))
            title_suffix = f"{n_frames} frames"
        else:
            # Show first half and last half
            half = (max_frames_per_image - 2) // 2
            event_indices = list(range(half)) + list(range(n_frames - half, n_frames))
            title_suffix = f"{n_frames} frames (showing first {half} + last {half})"

        # Build full list: [before] + event_indices + [after]
        # We'll track which are "context" frames vs "event" frames
        frames_to_show = []  # List of (frame_num, brightness, is_context, label_prefix)

        # Frame before event (if exists)
        if start > 0:
            before_frame = start - 1
            # We need to get brightness for this frame from the full video
            before_brightness = None  # Will fetch below
            frames_to_show.append((before_frame, None, True, "BEFORE"))

        # Event frames
        for idx in event_indices:
            frame_num = start + idx
            frames_to_show.append((frame_num, brightness_vals[idx], False, f"F{idx + 1}"))

        # Frame after event (if exists)
        after_frame = end + 1
        frames_to_show.append((after_frame, None, True, "AFTER"))

        # Create figure
        n_cols = len(frames_to_show)
        fig_width = max(n_cols * 1.5, 6)
        fig, axes = plt.subplots(1, n_cols, figsize=(fig_width, 3))
        if n_cols == 1:
            axes = [axes]

        fig.suptitle(f"Event {event_idx + 1} - {title_suffix}", fontsize=12)

        # Calculate per-event peak using median (identifies plateau level)
        event_peak = float(np.median(brightness_vals))

        for ax_idx, (frame_num, brightness, is_context, label_prefix) in enumerate(frames_to_show):
            frame = VideoReader.get_frame_at_index(video, frame_num)

            if frame is None:
                continue

            # Convert BGR to RGB for matplotlib
            frame_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)

            # Get brightness - calculate if not provided (for context frames)
            if brightness is None:
                brightness = float(np.mean(cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)))

            if is_context:
                # Context frames (before/after) - show in gray/blue
                label = label_prefix
                color = "blue"
                title_text = f"{label}\nb={brightness:.1f}"
            else:
                # Event frames - calculate weight using per-event peak (median)
                if event_peak > baseline:
                    weight = (brightness - baseline) / (event_peak - baseline)
                    weight = max(0, min(1, weight))
                else:
                    weight = 1.0

                # Classify and color
                if weight >= 0.95:
                    label = "FULL"
                    color = "green"
                elif weight >= 0.5:
                    label = f"{int(weight * 100)}%"
                    color = "orange"
                else:
                    label = f"{int(weight * 100)}%"
                    color = "red"

                title_text = f"{label_prefix}:{label}\nb={brightness:.1f}"

            axes[ax_idx].imshow(frame_rgb)
            axes[ax_idx].set_title(title_text, fontsize=8, color=color)
            axes[ax_idx].axis("off")

        plt.tight_layout()

        # Save
        output_path = output_dir / f"event_{event_idx + 1:02d}_frames.png"
        plt.savefig(output_path, dpi=100, bbox_inches="tight")
        plt.close()

        generated_files.append(output_path)
        print(f"  Saved: {output_path.name}")

    video.release()
    return generated_files


def main() -> None:
    """CLI entry point for the verification tool."""
    parser = argparse.ArgumentParser(
        description="Verify shutter event detection visually",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
    # Interactive mode - step through events
    uv run python -m shutter_analyzer.verify videos/test.mp4

    # Generate frame montages for each event
    uv run python -m shutter_analyzer.verify videos/test.mp4 --montage

    # Use different detection method
    uv run python -m shutter_analyzer.verify videos/test.mp4 --montage --method zscore --events 10
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
        "--montage",
        action="store_true",
        help="Generate frame montage images instead of interactive mode",
    )
    parser.add_argument(
        "--events",
        type=int,
        help="Expected number of events (required for zscore/dbscan methods)",
    )
    args = parser.parse_args()

    # Check if video exists
    if not Path(args.video).exists():
        print(f"Error: Video file not found: {args.video}")
        return

    if args.montage:
        # Generate montage images
        files = generate_event_montages(
            args.video,
            method=args.method,
            expected_events_count=args.events,
        )
        print(f"\nGenerated {len(files)} montage images")
        print(f"Open with: open {files[0].parent}/event_*.png")
    else:
        # Interactive mode
        verify_events(args.video, args.method)


if __name__ == "__main__":
    main()

"""
Script to extract frames around shutter events from a video, add frame numbers,
and save them as webp compressed images.
"""

import os
import cv2
import numpy as np
import argparse
from pathlib import Path
from typing import List, Tuple, Optional
import statistics


def create_output_directory(directory_path):
    """Create the output directory if it doesn't exist."""
    dir_path = Path(directory_path)
    dir_path.mkdir(parents=True, exist_ok=True)
    return dir_path


def add_frame_number_text(frame, frame_number):
    """Add frame number text to the bottom right of a frame."""
    # Create a copy of the frame to avoid modifying the original
    annotated_frame = frame.copy()

    # Get frame dimensions
    height, width = frame.shape[:2]

    # Set text properties
    text = f"Frame: {frame_number}"
    font = cv2.FONT_HERSHEY_SIMPLEX
    font_scale = 0.7
    font_thickness = 2
    font_color = (255, 255, 255)  # White text

    # Get text size to position it properly
    text_size, _ = cv2.getTextSize(text, font, font_scale, font_thickness)
    text_x = width - text_size[0] - 10  # 10 pixels from the right edge
    text_y = height - 10  # 10 pixels from the bottom edge

    # Add a dark background for better text visibility
    cv2.rectangle(
        annotated_frame,
        (text_x - 5, text_y - text_size[1] - 5),
        (text_x + text_size[0] + 5, text_y + 5),
        (0, 0, 0),  # Black background
        -1,  # Fill rectangle
    )

    # Add text to the frame
    cv2.putText(
        annotated_frame,
        text,
        (text_x, text_y),
        font,
        font_scale,
        font_color,
        font_thickness,
    )

    return annotated_frame


def calculate_frame_brightness(frame):
    """Calculate the overall brightness of a frame."""
    # Convert to grayscale if the frame is in color
    if len(frame.shape) == 3:
        gray_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
    else:
        gray_frame = frame

    # Calculate average pixel value
    return float(np.mean(gray_frame))


def analyze_brightness_distribution(
    brightness_values, percentile_threshold=25, margin_factor=1.5
):
    """
    Analyzes the brightness distribution to determine threshold.

    Args:
        brightness_values: List of brightness values for all frames
        percentile_threshold: Percentile to use for baseline determination (default: 25)
        margin_factor: Factor to multiply with (median-baseline) to determine threshold (default: 1.5)

    Returns:
        Calculated threshold value
    """
    # Calculate statistics
    min_brightness = min(brightness_values)
    max_brightness = max(brightness_values)
    mean_brightness = statistics.mean(brightness_values)
    median_brightness = statistics.median(brightness_values)

    # Calculate percentiles
    percentile_25 = np.percentile(brightness_values, percentile_threshold)

    # Determine baseline (closed shutter) brightness
    baseline = percentile_25

    # Calculate threshold
    brightness_range = median_brightness - baseline
    threshold = baseline + (brightness_range * margin_factor)

    print(f"Brightness analysis:")
    print(f"  Min: {min_brightness:.2f}")
    print(f"  Max: {max_brightness:.2f}")
    print(f"  Mean: {mean_brightness:.2f}")
    print(f"  Median: {median_brightness:.2f}")
    print(f"  Baseline: {baseline:.2f}")
    print(f"  Threshold: {threshold:.2f}")

    return threshold


def find_shutter_events(brightness_values, threshold):
    """
    Finds sequences of frames where the shutter is open.

    Args:
        brightness_values: List of brightness values for each frame
        threshold: Brightness threshold for determining shutter state

    Returns:
        List of (start_frame, end_frame) tuples representing shutter events
    """
    events = []
    current_event = None

    for frame_index, brightness in enumerate(brightness_values):
        is_open = brightness > threshold

        # Shutter just opened
        if is_open and current_event is None:
            current_event = frame_index

        # Shutter just closed
        elif not is_open and current_event is not None:
            events.append((current_event, frame_index - 1))
            current_event = None

    # Handle case where video ends with shutter open
    if current_event is not None:
        events.append((current_event, len(brightness_values) - 1))

    return events


def get_frames_to_extract(events, total_frames, padding=2):
    """
    Get the list of frame indices to extract, including padding around events.

    Args:
        events: List of (start_frame, end_frame) tuples
        total_frames: Total number of frames in the video
        padding: Number of frames to include before and after each event

    Returns:
        List of frame indices to extract
    """
    frames_to_extract = set()

    for start, end in events:
        # Include the event frames plus padding
        for i in range(max(0, start - padding), min(total_frames, end + padding + 1)):
            frames_to_extract.add(i)

    return sorted(frames_to_extract)


def extract_and_save_frames(
    video_path,
    output_dir,
    quality=80,
    padding=2,
    percentile_threshold=25,
    margin_factor=1.5,
):
    """Extract frames around shutter events from a video and save as webp images."""
    # Open the video
    video = cv2.VideoCapture(video_path)
    if not video.isOpened():
        raise ValueError(f"Could not open video file: {video_path}")

    # Get video properties
    total_frames = int(video.get(cv2.CAP_PROP_FRAME_COUNT))
    fps = video.get(cv2.CAP_PROP_FPS)
    print(f"Video has {total_frames} frames at {fps} FPS")

    # First pass: calculate brightness for all frames
    print("First pass: Calculating brightness for all frames...")
    brightness_values = []

    for frame_idx in range(total_frames):
        video.set(cv2.CAP_PROP_POS_FRAMES, frame_idx)
        ret, frame = video.read()
        if not ret:
            break

        brightness = calculate_frame_brightness(frame)
        brightness_values.append(brightness)

        if frame_idx % 100 == 0:
            print(f"  Processed {frame_idx}/{total_frames} frames")

    # Calculate threshold and find events
    threshold = analyze_brightness_distribution(
        brightness_values, percentile_threshold, margin_factor
    )

    events = find_shutter_events(brightness_values, threshold)
    print(f"Found {len(events)} shutter events:")
    for i, (start, end) in enumerate(events):
        duration = end - start + 1
        print(f"  Event #{i+1}: Frames {start}-{end} (Duration: {duration} frames)")

    # Get the frames to extract
    frames_to_extract = get_frames_to_extract(events, total_frames, padding)
    print(
        f"Will extract {len(frames_to_extract)} frames (including {padding} frame padding)"
    )

    # Create output directory
    output_path = create_output_directory(output_dir)

    # Second pass: extract and save the selected frames
    print("Second pass: Extracting and saving selected frames...")
    for frame_idx in frames_to_extract:
        video.set(cv2.CAP_PROP_POS_FRAMES, frame_idx)
        ret, frame = video.read()
        if not ret:
            print(f"Warning: Could not read frame {frame_idx}")
            continue

        # Add frame number text
        annotated_frame = add_frame_number_text(frame, frame_idx)

        # Add brightness value text (useful for debugging)
        height = annotated_frame.shape[0]
        cv2.putText(
            annotated_frame,
            f"Brightness: {brightness_values[frame_idx]:.2f}",
            (10, height - 10),
            cv2.FONT_HERSHEY_SIMPLEX,
            0.7,
            (255, 255, 255),
            2,
        )

        # Mark if this is part of an event
        is_event_frame = any(start <= frame_idx <= end for start, end in events)
        if is_event_frame:
            cv2.putText(
                annotated_frame,
                "EVENT",
                (10, 30),
                cv2.FONT_HERSHEY_SIMPLEX,
                0.9,
                (0, 0, 255),
                2,
            )

        # Save as webp
        output_file = output_path / f"frame_{frame_idx:06d}.webp"
        success = cv2.imwrite(
            str(output_file), annotated_frame, [cv2.IMWRITE_WEBP_QUALITY, quality]
        )

        if not success:
            print(f"Warning: Failed to save frame {frame_idx}")

    # Release the video
    video.release()
    print(
        f"Extraction complete. {len(frames_to_extract)} frames saved to {output_path}"
    )


def main():
    """Main function to parse arguments and run the extraction."""
    parser = argparse.ArgumentParser(
        description="Extract frames around shutter events from a video and save as webp images."
    )
    parser.add_argument("video_path", help="Path to the input video file")
    parser.add_argument(
        "--output-dir",
        "-o",
        default="extracted_frames",
        help="Path to the output directory (default: 'extracted_frames')",
    )
    parser.add_argument(
        "--quality",
        "-q",
        type=int,
        default=80,
        help="WEBP compression quality (0-100, higher is better, default: 80)",
    )
    parser.add_argument(
        "--padding",
        "-p",
        type=int,
        default=2,
        help="Number of frames to include before and after each event (default: 2)",
    )
    parser.add_argument(
        "--percentile",
        type=int,
        default=25,
        help="Percentile threshold for brightness analysis (default: 25)",
    )
    parser.add_argument(
        "--margin-factor",
        type=float,
        default=1.5,
        help="Margin factor for threshold calculation (default: 1.5)",
    )

    args = parser.parse_args()

    extract_and_save_frames(
        args.video_path,
        args.output_dir,
        args.quality,
        args.padding,
        args.percentile,
        args.margin_factor,
    )


if __name__ == "__main__":
    main()

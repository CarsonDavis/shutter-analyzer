"""
Script to extract frames from a video, add frame number text,
and save them as webp compressed images.
"""

import os
import cv2
import numpy as np
import argparse
from pathlib import Path


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


def extract_and_save_frames(
    video_path, output_dir, quality=80, step=1, max_frames=None
):
    """Extract frames from a video, add frame numbers, and save as webp images."""
    # Open the video
    video = cv2.VideoCapture(video_path)
    if not video.isOpened():
        raise ValueError(f"Could not open video file: {video_path}")

    # Get video properties
    total_frames = int(video.get(cv2.CAP_PROP_FRAME_COUNT))
    fps = video.get(cv2.CAP_PROP_FPS)
    print(f"Video has {total_frames} frames at {fps} FPS")

    # Create output directory
    output_path = create_output_directory(output_dir)

    # Calculate frames to extract
    if max_frames is None:
        frames_to_extract = total_frames
    else:
        frames_to_extract = min(total_frames, max_frames)

    print(f"Extracting up to {frames_to_extract} frames (step={step})...")

    # Extract frames
    frame_count = 0
    saved_count = 0

    while frame_count < total_frames:
        ret, frame = video.read()
        if not ret:
            break

        # Process this frame if it matches our step
        if frame_count % step == 0:
            # Add frame number text
            annotated_frame = add_frame_number_text(frame, frame_count)

            # Save as webp
            output_file = output_path / f"frame_{frame_count:06d}.webp"
            success = cv2.imwrite(
                str(output_file), annotated_frame, [cv2.IMWRITE_WEBP_QUALITY, quality]
            )

            if not success:
                print(f"Warning: Failed to save frame {frame_count}")
            else:
                saved_count += 1

            # Print progress periodically
            if saved_count % 10 == 0:
                print(f"Progress: {saved_count} frames saved")

            # Stop if we've reached max_frames
            if max_frames is not None and saved_count >= max_frames:
                break

        frame_count += 1

    # Release the video
    video.release()
    print(f"Extraction complete. {saved_count} frames saved to {output_path}")


def main():
    """Main function to parse arguments and run the extraction."""
    parser = argparse.ArgumentParser(
        description="Extract frames from a video, add frame numbers, and save as webp images."
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
        "--step", "-s", type=int, default=1, help="Extract every Nth frame (default: 1)"
    )
    parser.add_argument(
        "--max-frames",
        "-m",
        type=int,
        default=None,
        help="Maximum number of frames to extract (default: all)",
    )

    args = parser.parse_args()

    extract_and_save_frames(
        args.video_path, args.output_dir, args.quality, args.step, args.max_frames
    )


if __name__ == "__main__":
    main()

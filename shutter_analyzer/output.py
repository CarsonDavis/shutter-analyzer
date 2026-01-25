"""Output formatting and file generation for shutter analysis results.

This module provides utilities for:
- Managing output directories
- Formatting results tables for terminal display with color gradients
- Generating markdown reports
"""

from datetime import datetime
from pathlib import Path
from typing import List, Optional, Tuple

from .shutter_calculator import ShutterEvent, ShutterSpeedCalculator


# ANSI color codes
RESET = "\033[0m"
BOLD = "\033[1m"


def get_output_dir(video_path: str) -> Path:
    """
    Get or create output directory for a video file.

    Creates outputs/<video-stem>/ if it doesn't exist.

    Args:
        video_path: Path to the video file

    Returns:
        Path to the output directory
    """
    video_name = Path(video_path).stem
    output_dir = Path("outputs") / video_name
    output_dir.mkdir(parents=True, exist_ok=True)
    return output_dir


def get_variation_color(variation_percent: float) -> str:
    """
    Return ANSI color code based on variation percentage.

    Gradient: green (0-5%) -> yellow (5-10%) -> orange (10-15%) -> red (15%+)

    Args:
        variation_percent: Percentage variation from expected speed

    Returns:
        ANSI escape code for the appropriate color
    """
    abs_var = abs(variation_percent)
    if abs_var < 5:
        return "\033[92m"  # Bright green
    elif abs_var < 10:
        return "\033[93m"  # Yellow
    elif abs_var < 15:
        return "\033[33m"  # Dark yellow/orange
    else:
        return "\033[91m"  # Red


def variation_to_rgb(variation_percent: float) -> Tuple[int, int, int]:
    """
    Convert variation percentage to RGB color on green-yellow-red gradient.

    Args:
        variation_percent: Percentage variation from expected speed

    Returns:
        Tuple of (R, G, B) values (0-255)
    """
    abs_var = min(abs(variation_percent), 25)  # Cap at 25%
    ratio = abs_var / 25.0

    if ratio < 0.5:
        # Green to yellow (increasing red, full green)
        r = int(255 * (ratio * 2))
        g = 255
    else:
        # Yellow to red (full red, decreasing green)
        r = 255
        g = int(255 * (1 - (ratio - 0.5) * 2))

    return (r, g, 0)


def rgb_to_hex(rgb: Tuple[int, int, int]) -> str:
    """Convert RGB tuple to hex color string."""
    return f"#{rgb[0]:02x}{rgb[1]:02x}{rgb[2]:02x}"


def format_speed_fraction(speed_seconds: float) -> str:
    """
    Format a shutter speed as a fraction string.

    Args:
        speed_seconds: Speed in seconds (e.g., 0.002 for 1/500)

    Returns:
        Formatted string like "1/500" or "2s" for longer exposures
    """
    if speed_seconds >= 1.0:
        return f"{speed_seconds:.1f}s"
    else:
        denominator = round(1.0 / speed_seconds)
        return f"1/{denominator}"


def format_results_table(
    events: List[ShutterEvent],
    fps: float,
    recording_fps: Optional[float] = None,
    expected_speeds: Optional[List[float]] = None,
) -> str:
    """
    Format results as a terminal table string.

    Args:
        events: List of ShutterEvent objects
        fps: Video frames per second
        recording_fps: Actual recording FPS for slow-motion videos
        expected_speeds: Optional list of expected speeds in seconds

    Returns:
        Formatted table string for terminal display
    """
    lines = []

    # Header
    if expected_speeds:
        lines.append(
            f"{BOLD}{'Event':<7} {'Frames':<8} {'Weighted':<10} "
            f"{'Measured':<12} {'Expected':<12} {'Variation':<12}{RESET}"
        )
        lines.append("-" * 65)
    else:
        lines.append(
            f"{BOLD}{'Event':<7} {'Frames':<8} {'Weighted':<10} "
            f"{'Measured':<12}{RESET}"
        )
        lines.append("-" * 40)

    # Match events with expected speeds if provided
    if expected_speeds:
        # Sort events by duration (shortest first)
        sorted_events = sorted(events, key=lambda e: e.duration_frames)
        # Sort expected speeds (fastest first = smallest value)
        sorted_expected = sorted(expected_speeds)

        # Truncate to match
        min_len = min(len(sorted_events), len(sorted_expected))
        sorted_events = sorted_events[:min_len]
        sorted_expected = sorted_expected[:min_len]

        for i, (event, expected) in enumerate(zip(sorted_events, sorted_expected)):
            measured = ShutterSpeedCalculator.calculate_shutter_speed(
                event, fps, recording_fps, use_weighted=True
            )
            measured_seconds = 1.0 / measured
            variation = ShutterSpeedCalculator.compare_with_expected(
                measured_seconds, expected
            )

            color = get_variation_color(variation)
            sign = "+" if variation > 0 else ""

            lines.append(
                f"{i+1:<7} {event.duration_frames:<8} "
                f"{event.weighted_duration_frames:<10.2f} "
                f"{format_speed_fraction(measured_seconds):<12} "
                f"{format_speed_fraction(expected):<12} "
                f"{color}{sign}{variation:.1f}%{RESET}"
            )
    else:
        for i, event in enumerate(events):
            measured = ShutterSpeedCalculator.calculate_shutter_speed(
                event, fps, recording_fps, use_weighted=True
            )
            measured_seconds = 1.0 / measured

            lines.append(
                f"{i+1:<7} {event.duration_frames:<8} "
                f"{event.weighted_duration_frames:<10.2f} "
                f"{format_speed_fraction(measured_seconds):<12}"
            )

    return "\n".join(lines)


def generate_results_markdown(
    video_path: str,
    events: List[ShutterEvent],
    fps: float,
    recording_fps: Optional[float] = None,
    expected_speeds: Optional[List[float]] = None,
) -> str:
    """
    Generate markdown report content.

    Args:
        video_path: Path to the video file
        events: List of ShutterEvent objects
        fps: Video frames per second
        recording_fps: Actual recording FPS for slow-motion videos
        expected_speeds: Optional list of expected speeds in seconds

    Returns:
        Markdown content as a string
    """
    lines = [
        "# Shutter Speed Analysis Results\n",
        f"**Video:** {Path(video_path).name}",
        f"**Date:** {datetime.now().strftime('%Y-%m-%d')}",
        f"**Video FPS:** {fps:.2f}",
    ]

    if recording_fps:
        lines.append(f"**Recording FPS:** {recording_fps}")

    lines.extend(
        [
            "",
            "## Detected Events\n",
            "| Event | Start Frame | End Frame | Duration | Weighted | Measured Speed |",
            "|-------|-------------|-----------|----------|----------|----------------|",
        ]
    )

    for i, event in enumerate(events):
        measured = ShutterSpeedCalculator.calculate_shutter_speed(
            event, fps, recording_fps, use_weighted=True
        )
        measured_seconds = 1.0 / measured

        lines.append(
            f"| {i+1} | {event.start_frame} | {event.end_frame} | "
            f"{event.duration_frames} | {event.weighted_duration_frames:.2f} | "
            f"{format_speed_fraction(measured_seconds)} |"
        )

    # Add comparison section if expected speeds provided
    if expected_speeds:
        lines.extend(
            [
                "",
                "## Comparison with Expected\n",
                "| Event | Expected | Measured | Variation |",
                "|-------|----------|----------|-----------|",
            ]
        )

        # Sort and match
        sorted_events = sorted(events, key=lambda e: e.duration_frames)
        sorted_expected = sorted(expected_speeds)
        min_len = min(len(sorted_events), len(sorted_expected))

        for i, (event, expected) in enumerate(
            zip(sorted_events[:min_len], sorted_expected[:min_len])
        ):
            measured = ShutterSpeedCalculator.calculate_shutter_speed(
                event, fps, recording_fps, use_weighted=True
            )
            measured_seconds = 1.0 / measured
            variation = ShutterSpeedCalculator.compare_with_expected(
                measured_seconds, expected
            )

            # Color coding using inline HTML
            rgb = variation_to_rgb(variation)
            hex_color = rgb_to_hex(rgb)
            sign = "+" if variation > 0 else ""

            lines.append(
                f"| {i+1} | {format_speed_fraction(expected)} | "
                f"{format_speed_fraction(measured_seconds)} | "
                f'<span style="color: {hex_color}">{sign}{variation:.1f}%</span> |'
            )

    return "\n".join(lines)


def save_results_markdown(output_dir: Path, content: str) -> Path:
    """
    Save markdown content to results.md.

    Args:
        output_dir: Output directory path
        content: Markdown content to save

    Returns:
        Path to the saved file
    """
    output_path = output_dir / "results.md"
    output_path.write_text(content)
    return output_path

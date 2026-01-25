"""
Shutter speed calculation module for the Camera Shutter Speed Analyzer.
This module provides functionality for calculating and analyzing shutter speeds
based on detected shutter events.
"""

from typing import List, Tuple, Dict, Optional
import numpy as np


class ShutterEvent:
    """
    Represents a single shutter opening/closing event.

    Attributes:
        start_frame: Frame index where the shutter starts opening
        end_frame: Frame index where the shutter finishes closing
        brightness_values: List of brightness values for frames in the event
    """

    def __init__(
        self, start_frame: int, end_frame: int, brightness_values: List[float]
    ):
        """
        Initialize a ShutterEvent.

        Args:
            start_frame: Frame index where the shutter starts opening
            end_frame: Frame index where the shutter finishes closing
            brightness_values: List of brightness values for frames in this event
        """
        self.start_frame = start_frame
        self.end_frame = end_frame
        self.brightness_values = brightness_values

    @property
    def duration_frames(self) -> int:
        """
        Calculate the duration of the event in frames.

        Returns:
            Number of frames the shutter was open
        """
        return self.end_frame - self.start_frame + 1

    @property
    def max_brightness(self) -> float:
        """
        Get the maximum brightness value during the event.

        Returns:
            Maximum brightness value
        """
        return max(self.brightness_values) if self.brightness_values else 0

    @property
    def avg_brightness(self) -> float:
        """
        Get the average brightness value during the event.

        Returns:
            Average brightness value
        """
        return (
            sum(self.brightness_values) / len(self.brightness_values)
            if self.brightness_values
            else 0
        )

    def __repr__(self) -> str:
        """String representation of the ShutterEvent."""
        return (
            f"ShutterEvent(start_frame={self.start_frame}, end_frame={self.end_frame}, "
            f"duration_frames={self.duration_frames}, max_brightness={self.max_brightness:.2f})"
        )


class ShutterSpeedCalculator:
    """Calculates and analyzes shutter speeds."""

    @staticmethod
    def calculate_duration_seconds(
        start_frame: int,
        end_frame: int,
        fps: float,
        recording_fps: Optional[float] = None,
    ) -> float:
        """
        Calculates duration in seconds.

        Args:
            start_frame: Start frame index
            end_frame: End frame index
            fps: Frames per second of the saved video
            recording_fps: Actual recording FPS (for slow motion videos)

        Returns:
            Duration in seconds
        """
        frame_count = end_frame - start_frame + 1

        if recording_fps is not None:
            # Adjust for slow motion recording
            time_scale_factor = fps / recording_fps
            return (frame_count / fps) * time_scale_factor
        else:
            # Regular calculation
            return frame_count / fps

    @staticmethod
    def calculate_shutter_speed(
        shutter_event: ShutterEvent, fps: float, recording_fps: Optional[float] = None
    ) -> float:
        """
        Calculates the effective shutter speed as a fraction (1/x).

        Args:
            shutter_event: ShutterEvent object
            fps: Frames per second of the saved video
            recording_fps: Actual recording FPS (for slow motion videos)

        Returns:
            Shutter speed as a decimal representing 1/x
            (e.g., 0.002 for 1/500 second)
        """
        # Calculate duration in seconds
        duration = ShutterSpeedCalculator.calculate_duration_seconds(
            shutter_event.start_frame, shutter_event.end_frame, fps, recording_fps
        )

        # Convert to conventional shutter speed (1/x)
        # Return the reciprocal of the duration
        return 1.0 / duration

    @staticmethod
    def compare_with_expected(measured: float, expected: float) -> float:
        """
        Compares measured vs. expected speeds.

        Args:
            measured: Measured shutter speed in seconds
            expected: Expected shutter speed in seconds

        Returns:
            Percentage error (positive means measured is longer than expected)
        """
        return ((measured - expected) / expected) * 100

    @staticmethod
    def group_shutter_events(
        shutter_events: List[ShutterEvent], expected_speeds: List[float], fps: float
    ) -> Dict[float, List[Tuple[ShutterEvent, float]]]:
        """
        Groups events by expected speed settings.

        Sorts events by duration before matching with expected speeds.

        Args:
            shutter_events: List of ShutterEvent objects
            expected_speeds: List of expected shutter speeds in seconds (from fastest to slowest)
            fps: Frames per second of the video

        Returns:
            Dictionary mapping expected speeds to lists of (event, measured_speed) tuples
        """
        result = {}

        # Sort expected speeds from fastest to slowest (smallest to largest value)
        expected_speeds_sorted = sorted(expected_speeds)

        # Sort shutter events by duration (shortest to longest)
        sorted_events = sorted(shutter_events, key=lambda e: e.duration_frames)

        # Truncate lists to match
        min_length = min(len(sorted_events), len(expected_speeds_sorted))
        sorted_events = sorted_events[:min_length]
        expected_speeds_sorted = expected_speeds_sorted[:min_length]

        # Match events with speeds
        for event, expected in zip(sorted_events, expected_speeds_sorted):
            measured = ShutterSpeedCalculator.calculate_shutter_speed(event, fps)

            if expected not in result:
                result[expected] = []

            result[expected].append((event, measured))

        return result

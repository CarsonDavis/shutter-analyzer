"""
Frame analysis module for the Camera Shutter Speed Analyzer.
This module provides functionality for analyzing video frames to detect
shutter open/close events based on brightness levels.
"""

import cv2
import numpy as np
from typing import List, Tuple, Generator, Optional, Dict
from dataclasses import dataclass
import statistics
import sklearn


@dataclass
class FrameBrightnessStats:
    """
    Data class to store brightness statistics for a video.

    Attributes:
        min_brightness: Minimum brightness value
        max_brightness: Maximum brightness value
        mean_brightness: Mean brightness value
        median_brightness: Median brightness value
        percentiles: Dictionary of brightness percentiles (e.g., {10: 23.5, 90: 187.2})
        baseline: The baseline brightness value (typically represents closed shutter)
        threshold: The calculated threshold to distinguish open/closed shutter
        peak_brightness: The typical brightness when shutter is fully open (95th percentile of events)
    """

    min_brightness: float
    max_brightness: float
    mean_brightness: float
    median_brightness: float
    percentiles: Dict[int, float]
    baseline: float
    threshold: float
    peak_brightness: Optional[float] = None


class FrameAnalyzer:
    """Class for analyzing frames for brightness and shutter state."""

    @staticmethod
    def calculate_frame_brightness(frame: np.ndarray) -> float:
        """
        Calculates the overall brightness of a frame.

        The brightness is calculated as the average pixel value across all channels.

        Args:
            frame: A video frame (numpy array)

        Returns:
            Average brightness value (0-255)
        """
        # Convert to grayscale if the frame is in color
        if len(frame.shape) == 3:
            gray_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        else:
            gray_frame = frame

        # Calculate average pixel value
        return float(np.mean(gray_frame))

    @staticmethod
    def analyze_brightness_distribution(
        frames: Generator[Tuple[int, np.ndarray], None, None],
        percentile_threshold: int = 25,
        margin_factor: float = 1.5,
        method: str = "original",
        expected_events_count: int = None,
    ) -> Tuple[FrameBrightnessStats, List[float]]:
        """
        Analyzes the brightness distribution of all frames to determine threshold.

        Args:
            frames: Generator yielding (frame_index, frame) tuples
            percentile_threshold: Percentile to use for baseline determination (default: 25)
            margin_factor: Factor to multiply with (median-baseline) to determine threshold (default: 1.5)
            method: Method to use for threshold calculation ("original", "zscore", or "dbscan")
            expected_events_count: Expected number of shutter events (needed for zscore and dbscan methods)

        Returns:
            Tuple of (FrameBrightnessStats, List[brightness_values])
        """
        # Collect brightness values for all frames
        brightness_values = []
        for _, frame in frames:
            brightness = FrameAnalyzer.calculate_frame_brightness(frame)
            brightness_values.append(brightness)

        # Calculate statistics
        min_brightness = min(brightness_values)
        max_brightness = max(brightness_values)
        mean_brightness = statistics.mean(brightness_values)
        median_brightness = statistics.median(brightness_values)

        # Calculate percentiles
        percentiles = {
            10: np.percentile(brightness_values, 10),
            25: np.percentile(brightness_values, 25),
            75: np.percentile(brightness_values, 75),
            90: np.percentile(brightness_values, 90),
        }

        # Determine baseline (closed shutter) brightness
        baseline = percentiles[percentile_threshold]

        # Calculate threshold based on specified method
        if method == "zscore" and expected_events_count is not None:
            threshold = FrameAnalyzer.find_threshold_using_zscore(
                brightness_values, expected_events_count
            )
        elif method == "dbscan" and expected_events_count is not None:
            threshold = FrameAnalyzer.find_threshold_using_dbscan(
                brightness_values, expected_events_count
            )
        else:
            # Original method: baseline plus a factor of the difference between median and baseline
            brightness_range = median_brightness - baseline
            threshold = baseline + (brightness_range * margin_factor)
            # Ensure threshold is not exactly equal to baseline if brightness_range is 0
            if threshold == baseline:
                # Use a small percentage of the maximum brightness as threshold
                threshold = baseline + (max_brightness - baseline) * 0.1

        # Create stats object
        stats = FrameBrightnessStats(
            min_brightness=min_brightness,
            max_brightness=max_brightness,
            mean_brightness=mean_brightness,
            median_brightness=median_brightness,
            percentiles=percentiles,
            baseline=baseline,
            threshold=threshold,
        )

        return stats, brightness_values

    @staticmethod
    def is_shutter_open(brightness: float, threshold: float) -> bool:
        """
        Determines if the shutter is open based on frame brightness.

        Args:
            brightness: The brightness value of the frame
            threshold: Brightness threshold for determining shutter state

        Returns:
            True if the shutter is open (brightness > threshold), False otherwise
        """
        return brightness > threshold

    @classmethod
    def find_shutter_events(
        cls, brightness_values: List[float], threshold: float
    ) -> List[Tuple[int, int, List[float]]]:
        """
        Finds sequences of frames where the shutter is open.

        Args:
            brightness_values: List of brightness values for each frame
            threshold: Brightness threshold for determining shutter state

        Returns:
            List of (start_frame, end_frame, brightness_values) tuples representing shutter events
        """
        events = []
        current_event = None
        current_brightness_values = []

        for frame_index, brightness in enumerate(brightness_values):
            is_open = cls.is_shutter_open(brightness, threshold)

            # Shutter just opened
            if is_open and current_event is None:
                current_event = frame_index
                current_brightness_values = [brightness]

            # Shutter still open
            elif is_open and current_event is not None:
                current_brightness_values.append(brightness)

            # Shutter just closed
            elif not is_open and current_event is not None:
                events.append(
                    (current_event, frame_index - 1, current_brightness_values.copy())
                )
                current_event = None
                current_brightness_values = []

        # Handle case where video ends with shutter open
        if current_event is not None:
            events.append(
                (
                    current_event,
                    len(brightness_values) - 1,
                    current_brightness_values.copy(),
                )
            )

        return events

    @staticmethod
    def find_threshold_using_zscore(brightness_values, expected_events_count):
        """Find threshold using z-score to identify outliers."""
        # Calculate mean and standard deviation
        mean = np.mean(brightness_values)
        std = np.std(brightness_values)

        # If standard deviation is nearly zero, data is too uniform for z-score
        if std < 1e-6:
            # Return a small increment above mean
            return mean + 0.1

        # Try different z-scores to find the one that gives closest to expected events
        best_threshold = mean
        best_diff = float("inf")

        for z in np.linspace(1.0, 5.0, 40):  # Try z-scores from 1.0 to 5.0
            threshold = mean + (z * std)
            events_count = sum(1 for b in brightness_values if b > threshold)
            diff = abs(events_count - expected_events_count)

            if diff < best_diff:
                best_diff = diff
                best_threshold = threshold

        return best_threshold

    @staticmethod
    def find_threshold_using_dbscan(brightness_values, expected_events_count):
        """Use DBSCAN clustering to separate baseline from events."""
        from sklearn.cluster import DBSCAN

        # Reshape for sklearn
        X = np.array(brightness_values).reshape(-1, 1)

        # Try different epsilon values to get closest to expected events count
        best_threshold = np.percentile(brightness_values, 95)  # Default fallback
        best_diff = float("inf")

        # Calculate the range of brightness values
        data_range = max(brightness_values) - min(brightness_values)
        if data_range < 1e-6:  # Very uniform data
            return np.percentile(
                brightness_values,
                100 - (expected_events_count / len(brightness_values) * 100),
            )

        # Try different eps values
        for eps_factor in np.linspace(0.01, 0.2, 20):
            eps = data_range * eps_factor

            # Apply DBSCAN
            try:
                db = DBSCAN(eps=eps, min_samples=5).fit(X)

                # Extract cluster labels
                labels = db.labels_

                # Count number of clusters (excluding noise labeled as -1)
                clusters = set(labels)
                clusters.discard(-1)  # Remove noise cluster

                # If we have clusters
                if clusters:
                    # Calculate mean brightness for each cluster
                    cluster_means = {}
                    for label in clusters:
                        cluster_means[label] = np.mean(X[labels == label])

                    # Sort clusters by brightness
                    sorted_clusters = sorted(cluster_means.items(), key=lambda x: x[1])

                    # Calculate potential thresholds between clusters
                    potential_thresholds = []
                    for i in range(len(sorted_clusters) - 1):
                        threshold = (
                            sorted_clusters[i][1] + sorted_clusters[i + 1][1]
                        ) / 2
                        events_count = sum(
                            1 for b in brightness_values if b > threshold
                        )
                        diff = abs(events_count - expected_events_count)
                        potential_thresholds.append((threshold, diff))

                    # Select the threshold that gives closest to expected events count
                    if potential_thresholds:
                        threshold, diff = min(potential_thresholds, key=lambda x: x[1])
                        if diff < best_diff:
                            best_diff = diff
                            best_threshold = threshold
            except:
                continue  # Skip if DBSCAN fails with these parameters

        return best_threshold

    @staticmethod
    def calculate_peak_brightness(
        events: List[Tuple[int, int, List[float]]],
        plateau_threshold: float = 0.90,
        min_plateau_frames: int = 10,
    ) -> Optional[float]:
        """
        Calculate the peak brightness from detected events using plateau analysis.

        Instead of using a simple percentile, this method:
        1. Identifies plateau frames within each event (frames >= 90% of event max)
        2. Only considers events with sufficient plateau frames (stable readings)
        3. Calculates the mean plateau brightness for each qualifying event
        4. Returns the median of these plateau means

        This gives a robust estimate of "fully open" brightness that isn't
        skewed by outliers or short events that never fully stabilize.

        Args:
            events: List of (start_frame, end_frame, brightness_values) tuples
            plateau_threshold: Fraction of event max to consider as plateau (default 0.90)
            min_plateau_frames: Minimum plateau frames for event to qualify (default 10)

        Returns:
            Peak brightness value, or None if no events
        """
        if not events:
            return None

        plateau_means = []

        for _, _, brightness_values in events:
            if not brightness_values:
                continue

            b = np.array(brightness_values)
            event_max = b.max()

            # Find plateau frames (within threshold of event max)
            plateau_mask = b >= (event_max * plateau_threshold)
            plateau_count = plateau_mask.sum()

            # Only use events with enough stable plateau frames
            if plateau_count >= min_plateau_frames:
                plateau_mean = b[plateau_mask].mean()
                plateau_means.append(plateau_mean)

        if plateau_means:
            # Use median of plateau means for robustness
            return float(np.median(plateau_means))

        # Fallback: if no events have enough plateau frames,
        # use 95th percentile of all event brightness values
        all_event_brightness = []
        for _, _, brightness_values in events:
            all_event_brightness.extend(brightness_values)

        if all_event_brightness:
            return float(np.percentile(all_event_brightness, 95))

        return None

    @classmethod
    def analyze_video_and_find_events(
        cls,
        frames: Generator[Tuple[int, np.ndarray], None, None],
        percentile_threshold: int = 25,
        margin_factor: float = 1.5,
        method: str = "original",
        expected_events_count: int = None,
    ) -> Tuple[FrameBrightnessStats, List[Tuple[int, int, List[float]]]]:
        """
        Analyzes a video and finds shutter events with adaptive thresholding.

        Args:
            frames: Generator yielding (frame_index, frame) tuples
            percentile_threshold: Percentile to use for baseline determination
            margin_factor: Factor to multiply with (median-baseline) to determine threshold
            method: Method to use for threshold calculation ("original", "zscore", or "dbscan")
            expected_events_count: Expected number of shutter events

        Returns:
            Tuple of (brightness_stats, shutter_events)
        """
        # First pass: analyze brightness distribution
        brightness_stats, brightness_values = cls.analyze_brightness_distribution(
            frames, percentile_threshold, margin_factor, method, expected_events_count
        )

        # Second pass: find shutter events using the determined threshold
        events = cls.find_shutter_events(brightness_values, brightness_stats.threshold)

        # Calculate peak brightness from events and update stats
        peak_brightness = cls.calculate_peak_brightness(events)
        brightness_stats.peak_brightness = peak_brightness

        return brightness_stats, events

"""
Video processing module for the Camera Shutter Speed Analyzer.
This module handles video file operations including opening videos,
extracting frames, and retrieving video properties.
"""

import cv2
import numpy as np
from typing import Generator, Tuple, Dict, Any, Optional


class VideoReader:
    """Class for handling video file operations."""

    @staticmethod
    def open_video(file_path: str) -> cv2.VideoCapture:
        """
        Opens a video file for processing.

        Args:
            file_path: Path to the video file

        Returns:
            VideoCapture object for the opened video

        Raises:
            ValueError: If the video cannot be opened
        """
        video = cv2.VideoCapture(file_path)
        if not video.isOpened():
            raise ValueError(f"Could not open video file: {file_path}")
        return video

    @staticmethod
    def get_video_properties(video: cv2.VideoCapture) -> Dict[str, Any]:
        """
        Returns properties of the video.

        Args:
            video: OpenCV VideoCapture object

        Returns:
            Dictionary containing video properties (frame_count, fps, width, height)
        """
        properties = {
            "frame_count": int(video.get(cv2.CAP_PROP_FRAME_COUNT)),
            "fps": video.get(cv2.CAP_PROP_FPS),
            "width": int(video.get(cv2.CAP_PROP_FRAME_WIDTH)),
            "height": int(video.get(cv2.CAP_PROP_FRAME_HEIGHT)),
            "duration": int(video.get(cv2.CAP_PROP_FRAME_COUNT))
            / video.get(cv2.CAP_PROP_FPS),
        }
        return properties

    @staticmethod
    def read_frames(
        video: cv2.VideoCapture,
    ) -> Generator[Tuple[int, np.ndarray], None, None]:
        """
        Generator that yields frames one by one.

        Args:
            video: OpenCV VideoCapture object

        Yields:
            Tuple of (frame_index, frame)
        """
        frame_index = 0
        while True:
            ret, frame = video.read()
            if not ret:
                break
            yield frame_index, frame
            frame_index += 1

    @staticmethod
    def get_frame_at_index(video: cv2.VideoCapture, index: int) -> Optional[np.ndarray]:
        """
        Gets a specific frame by its index.

        Args:
            video: OpenCV VideoCapture object
            index: Index of the frame to retrieve

        Returns:
            The frame at the specified index, or None if the index is invalid
        """
        current_pos = int(video.get(cv2.CAP_PROP_POS_FRAMES))

        # Check if index is valid
        frame_count = int(video.get(cv2.CAP_PROP_FRAME_COUNT))
        if index < 0 or index >= frame_count:
            return None

        # Set position to requested frame
        video.set(cv2.CAP_PROP_POS_FRAMES, index)
        ret, frame = video.read()

        # Restore original position
        video.set(cv2.CAP_PROP_POS_FRAMES, current_pos)

        if not ret:
            return None
        return frame

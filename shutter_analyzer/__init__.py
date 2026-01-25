"""Shutter Analyzer - Measure camera shutter speeds from video recordings.

This package provides tools for analyzing high-speed video recordings of camera
shutters to measure actual shutter speeds and compare with expected values.

Main entry point:
    uv run python -m shutter_analyzer <video_path> [options]

Key modules:
    video_processor: Video file I/O operations
    frame_analyzer: Brightness analysis and event detection
    shutter_calculator: Shutter speed calculations
    output: Result formatting and file generation
    verify: Manual verification tool for event detection
"""

from .frame_analyzer import FrameAnalyzer, FrameBrightnessStats
from .output import (
    format_results_table,
    generate_results_markdown,
    get_output_dir,
    save_results_markdown,
)
from .shutter_calculator import ShutterEvent, ShutterSpeedCalculator
from .video_processor import VideoReader

__version__ = "0.1.0"

__all__ = [
    "VideoReader",
    "FrameAnalyzer",
    "FrameBrightnessStats",
    "ShutterEvent",
    "ShutterSpeedCalculator",
    "get_output_dir",
    "format_results_table",
    "generate_results_markdown",
    "save_results_markdown",
]

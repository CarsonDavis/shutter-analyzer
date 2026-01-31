# Android Documentation Index

**Last Updated**: 2026-01-31

This index explains what each document in `android/docs/` is for and when you'd want to reference it.

---

## Quick Reference

| Document | One-liner | When to use |
|----------|-----------|-------------|
| [CLI_QUICKSTART.md](CLI_QUICKSTART.md) | Build/install/debug commands | Daily development |
| [BUILD.md](BUILD.md) | Full build guide | First-time setup |
| [APP_SPEC.md](APP_SPEC.md) | What we're building | Understanding requirements |
| [WIREFRAMES.md](WIREFRAMES.md) | Screen layouts | Implementing UI |
| [TECH_STACK.md](TECH_STACK.md) | Technology choices | Understanding architecture |
| [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md) | Python-to-Kotlin porting | Adding/modifying analysis code |
| [IMPLEMENTATION_LOG.md](IMPLEMENTATION_LOG.md) | What was built and when | Understanding history |
| [THEORY.md](THEORY.md) | How shutter measurement works | Debugging analysis issues |
| [HOW_TO.md](HOW_TO.md) | End-user usage guide | Writing help text, onboarding |
| [VIDEO_DECODER_DESIGN.md](VIDEO_DECODER_DESIGN.md) | MediaCodec video decoder | Modifying video import |
| [EXPAND_FRAMES_FEATURE.md](EXPAND_FRAMES_FEATURE.md) | Frame expansion feature | Event review screen changes |
| [CAMERA_FOCUS_ZOOM_IMPLEMENTATION.md](CAMERA_FOCUS_ZOOM_IMPLEMENTATION.md) | CameraX focus/zoom | Camera control changes |
| [HIGH_SPEED_RECORDING_DESIGN.md](HIGH_SPEED_RECORDING_DESIGN.md) | 240fps Camera2 recording | Implementing native slow-mo |
| [VIDEO_LIFECYCLE_MANAGEMENT.md](VIDEO_LIFECYCLE_MANAGEMENT.md) | Video cleanup design | Implementing delete flows |
| [camerax-manual-focus-zoom/](../../docs/android/camerax-manual-focus-zoom/) | Focus/zoom research | Deep debugging camera issues |

---

## Document Details

### Build & Development

#### [CLI_QUICKSTART.md](CLI_QUICKSTART.md)
**Use when:** You need to build, install, or debug the app from the command line.

Cheat sheet with essential commands:
- Setting up JAVA_HOME and adb
- Building debug APK
- Installing to device
- Getting crash logs

#### [BUILD.md](BUILD.md)
**Use when:** Setting up the project for the first time or need detailed build instructions.

Comprehensive build guide covering:
- Prerequisites (Android Studio, SDK versions)
- Building from Android Studio
- Building from command line
- Installing on device
- Signing for release

---

### Product Specification

#### [APP_SPEC.md](APP_SPEC.md)
**Use when:** You need to understand what the app does, its design principles, or check requirements.

The source of truth for product requirements:
- Core design principles (guided experience, offline-first, camera-centric)
- All 10 screens with their purpose, elements, and actions
- User flows (new test, import, history)
- Decisions log with rationale

#### [WIREFRAMES.md](WIREFRAMES.md)
**Use when:** Implementing or modifying UI screens.

ASCII mockups for every screen:
- Home / camera list (including empty state)
- Onboarding tutorial (5 screens)
- Recording setup and recording screen (4 states)
- Event review with frame toggle modal
- Results dashboard with expanded views
- Settings, import flow, camera detail

---

### Technical Architecture

#### [TECH_STACK.md](TECH_STACK.md)
**Use when:** You need to understand the tech choices or add new dependencies.

Technology decisions and patterns:
- Stack summary: Kotlin, Compose, MVVM, CameraX, Room, Hilt
- Architecture diagram (UI, Domain, Data layers)
- Code examples for each technology
- Database schema design
- Gradle dependencies reference

#### [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md)
**Use when:** Porting Python algorithms to Kotlin or adding new analysis logic.

Python-to-Kotlin porting guide:
- Algorithm summary from Python implementation
- Side-by-side Python/Kotlin code examples
- OpenCV Android integration (ImageProxy to Mat)
- Real-time event detection strategy
- Memory management notes

---

### Progress Tracking

#### [IMPLEMENTATION_LOG.md](IMPLEMENTATION_LOG.md)
**Use when:** Understanding what was built, when, and why.

Chronological log of implementation:
- Phase-by-phase progress
- Files created/modified per session
- Key decisions made during development
- Links to continued log in IMPLEMENTATION_LOG_2.md

#### [IMPLEMENTATION_LOG_2.md](IMPLEMENTATION_LOG_2.md)
**Use when:** Same as above, for later phases.

Continuation of the implementation log covering later phases (6+).

---

### Domain Knowledge

#### [THEORY.md](THEORY.md)
**Use when:** Debugging analysis issues or understanding why the math works.

The science behind shutter measurement:
- What shutter speed is and why it matters
- High-speed video capture principles
- Brightness analysis methodology
- Frame weighting for partial-open shutters
- The math: `shutter_duration = frame_count / recording_fps`

#### [HOW_TO.md](HOW_TO.md)
**Use when:** Writing help text, onboarding screens, or user documentation.

End-user guide for measuring shutter speeds:
- Equipment needed
- Physical setup (positioning camera, phone, light)
- Phone settings (iPhone vs Android)
- Step-by-step recording procedure

---

### Feature Specifications

#### [VIDEO_DECODER_DESIGN.md](VIDEO_DECODER_DESIGN.md)
**Use when:** Modifying video import or frame extraction logic.

Technical design for fast video analysis:
- Problem: MediaMetadataRetriever was too slow (10-50ms/frame)
- Solution: MediaCodec + MediaExtractor (0.5-2ms/frame)
- Architecture diagram
- Y-plane extraction for brightness calculation
- Why OpenCV VideoCapture doesn't work on Android

#### [EXPAND_FRAMES_FEATURE.md](EXPAND_FRAMES_FEATURE.md)
**Use when:** Modifying the event review screen or frame selection logic.

Feature spec for expandable event boundaries:
- Problem: Auto-detection may miss partial-open frames
- Solution: Let users expand frame range with [+] buttons
- Visual layout mockups
- State management design

#### [CAMERA_FOCUS_ZOOM_IMPLEMENTATION.md](CAMERA_FOCUS_ZOOM_IMPLEMENTATION.md)
**Use when:** Modifying camera controls or fixing focus/zoom issues.

Technical implementation for CameraX focus and zoom:
- Why simple Camera2 settings don't work (CameraX overrides 3A)
- Correct approach: Camera2Interop.Extender when building UseCase
- Implementation plan with code examples
- Key constraints and gotchas

#### [HIGH_SPEED_RECORDING_DESIGN.md](HIGH_SPEED_RECORDING_DESIGN.md)
**Use when:** Implementing native 240fps recording or understanding why CameraX doesn't support high-speed video.

Design document for Camera2 high-speed capture:
- Why CameraX is limited to 30fps
- Camera2 `CameraConstrainedHighSpeedCaptureSession` overview
- Key constraints (2 surfaces max, no ImageAnalysis)
- Dual architecture: live preview feedback + post-recording analysis
- Component design (HighSpeedCameraManager, LivePreviewAnalyzer)
- MediaRecorder configuration for 240fps
- Request batching requirements
- Fallback strategy for unsupported devices

#### [VIDEO_LIFECYCLE_MANAGEMENT.md](VIDEO_LIFECYCLE_MANAGEMENT.md)
**Use when:** Implementing or modifying camera/test deletion, or debugging orphaned videos.

Design document for automatic video cleanup:
- Problem: videos accumulate with no cleanup mechanism
- Solution: cascade video deletion when cameras/tests are deleted
- Implementation: VideoStorageManager + repository modifications
- One-time migration to clean existing orphaned data

#### [CameraX Manual Focus Research](../../docs/android/camerax-manual-focus-zoom/)
**Use when:** Deep-diving into CameraX focus/zoom behavior or debugging camera control issues.

Research folder with two documents:
- `background.md` - Raw research from 11 sources (Google Groups, Android docs, Medium)
- `report.md` - Synthesized implementation guide with Kotlin code examples

Covers why `Camera2CameraControl.setCaptureRequestOptions()` fails for focus (CameraX overrides 3A), the hybrid approach using `Camera2Interop.Extender`, digital zoom best practices, lens switching prevention, and focus distance mapping (diopters to slider).

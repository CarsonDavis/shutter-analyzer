# Shutter Analyzer - Android App Specification

## Document Status
**Status**: Requirements Complete - Ready for Design/Development
**Last Updated**: 2025-01-25

---

## 1. Overview

This document specifies the Android mobile application version of Shutter Analyzer, a tool for measuring mechanical camera shutter speeds using video analysis.

### 1.1 Background

The existing Python CLI tool analyzes pre-recorded videos to detect shutter events and calculate actual shutter speeds. The Android app will bring this functionality to mobile devices with integrated video capture and a guided user experience.

### 1.2 Screen Count Summary

| # | Screen | Purpose |
|---|--------|---------|
| 5.1 | Home | Camera list, new test, import |
| 5.2 | Onboarding | First-time tutorial (skippable) |
| 5.3 | Recording Setup | Camera name, speed set selection |
| 5.4 | Recording Screen | Live detection with speed prompts |
| 5.5 | Event Review | Verify/correct detected frames |
| 5.6 | Processing | Calculation progress (inline, not a screen) |
| 5.7 | Results Dashboard | Rich visualizations |
| 5.8 | Camera Detail | History of past sessions |
| 5.9 | Import Flow | For existing videos |
| 5.10 | Settings | App configuration |
| 5.11 | Theory | Educational "How It Works" content |
| 5.12 | Event Preview | Event detail view during import |

---

## 2. Core Design Principles

Based on initial requirements gathering:

1. **Guided Experience**: Clear step-by-step instructions for setup and recording
2. **Minimal Controls**: Simple interface, not overwhelming with options
3. **Camera-Centric Data Model**: Users save "cameras" with their measurement history
4. **Offline-First**: All processing on-device, no internet required
5. **Dual Input**: Record directly in-app OR import existing slow-mo videos

---

## 3. Decisions Log

| Date | Question | Decision | Rationale |
|------|----------|----------|-----------|
| 2025-01-24 | Video source | Both record + import | Flexibility for users with existing videos |
| 2025-01-24 | Processing | Offline only | Privacy, works anywhere, no server costs |
| 2025-01-24 | Target user | Guided/beginner-friendly | Clear instructions, minimal jargon, simple flow |
| 2025-01-24 | Data model | Camera profiles | Save analyzed cameras, revisit stats later |
| 2025-01-24 | Speed entry | Guided sequence with live detection | App prompts speeds, detects events in real-time |
| 2025-01-24 | Camera profiles | Optional (both modes) | Named profiles or quick unnamed sessions |
| 2025-01-24 | Camera info | Name only | Keep it simple, just a label |
| 2025-01-24 | Speed sequence | Standard set with custom option | Default to standard mechanical set, allow custom for unusual cameras |
| 2025-01-24 | Skip/redo | Yes | On-screen button to redo if user messes up |
| 2025-01-24 | Slow speeds | Include all | Slower speeds are MORE accurate (more frames); fast speeds (1/1000, 1/500) are hardest |

---

## 4. Requirements Gathering - Full Responses

### Session 1: 2025-01-24

**Q: Video source?**
> Both (record in-app + import existing)

**Q: Processing location?**
> Offline only

**Q: Target user profile?**
> "We want to give very clear instructions about putting the film camera on a tripod, how to do the filming so you can only see the shutter in the frame, etc. and it should be a relatively guided experience. There won't be many controls. Set up your phone and camera, record different shutter speeds. It logs these and eventually creates graphs and analysis. Maybe you can save an analyzed camera to storage and come back to see its stats again."

**Q: How should users indicate which speed they're testing?**
> "OK, I don't know if it is possible, but the absolute ideal thing would be to have the camera running the whole time and the interface prompts you for the speed and you take the photo at the speed it prompts for. Then the system will just save out that little snippet of your video for processing. So, you open it up and it says 1000, you run it and if it detects then it goes green and says 500, etc."

**Q: Camera profiles?**
> Both - optional named profiles or quick unnamed sessions

**Q: Camera information to collect?**
> Name only - keep it simple

**Q: Speed sequence?**
> "Heavily guided towards a standard set, but we can make a custom option if they happen to have a weird camera"

**Q: Skip/redo capability?**
> "Yeah, maybe onscreen button to let them redo a speed if they realize they messed up"

**Q: Accuracy note on slow vs fast speeds?**
> "All the speeds will tend to be across many frames. And slower speeds will be the most accurate. It's 1000 and 500 that will be the hardest, and should typically only be 1 frame of light."

**Q: Results display?**
> Rich dashboard - multiple graphs, stats summary, detailed per-speed analysis

**Q: Export/share capability?**
> None needed - view in app only

**Q: History for saved cameras?**
> Full history - show all past test sessions with dates, can compare over time

**Q: Dashboard visualizations?**
> Multiple selections: Accuracy table, Brightness timeline, Speed comparison (expected vs actual overlay)

**Q: Onboarding/tutorial?**
> Optional/skippable - offer tutorial but let experienced users skip

**Q: Recording frame rate?**
> "We need to detect the max, but also this speed tells us our calculation limitations. If they can only do 120, then we aren't going to get good data past 1/125. All we will be able to do for 1/250, 1/500 is at least confirm they aren't wildly wrong."

**Q: What if no shutter event detected?**
> Keep waiting - no timeout, wait for user action or detection

**Q: Refined flow clarification (final)?**
> "So you open the app and get a tutorial, then you set your camera and choose default or manual shutter speeds, then you get the camera which automatically adjusts brightness to be relatively black and high speed, then it begins detecting events and prompting for different shutter speeds. Each time it sees an event it moves on to the next one. After this, you go into a review page, where it shows you the detected frames from each event one by one, plus 2 undetected frames on the borders of the event, and you can click on the frames to correct anything. Then it does calculations, then it shows you the dashboard."

---

## 5. Screen Inventory

### 5.1 Home / Camera List
**Purpose**: Entry point, shows saved camera profiles and quick actions

**Elements**:
- List of saved camera profiles (name, last tested date, quick status indicator)
- "New Test" button (starts new session)
- "Import Video" button (for existing slow-mo videos)
- Help icon (?) with dropdown: Tutorial, How It Works
- Settings gear icon

**Actions**:
- Tap camera → View camera detail/history
- Tap "New Test" → Recording setup flow
- Tap "Import" → File picker
- Tap Help → Dropdown menu for Tutorial or Theory

---

### 5.2 Onboarding Tutorial (Optional)
**Purpose**: First-time user walkthrough

**Screens** (swipeable):
1. Welcome + what the app does
2. Equipment setup (tripod, camera positioning diagram)
3. Lighting requirements (bright background behind shutter)
4. How to frame the shot (shutter fills frame)
5. Recording flow preview

**Actions**:
- Skip button always visible
- "Get Started" on final screen

---

### 5.3 Recording Setup
**Purpose**: Configure test before recording

**Elements**:
- Camera name input (optional - generates default "Test Jan 25 14:30" if blank)
- Speed set selector (3 options):
  - "Standard Set" (default): 1/1000 → 1s
  - "Add or Remove Speeds" → opens checkbox speed picker (1/8000 to 8s range)
  - "Enter Custom Speeds" → text input for comma-separated speeds (e.g., "1/50, 1/100, 1/400")
- Brief setup reminder (collapsible): tripod, framing, lighting tips
- "Start Recording" button
- Link to full tutorial if needed

---

### 5.4 Recording Screen (CORE SCREEN)
**Purpose**: Guided recording with live detection

**Elements**:
- Camera viewfinder (full screen background)
- Current speed prompt (large, centered): "1/1000"
- Status indicator:
  - Waiting (neutral) → "Fire shutter at 1/1000"
  - Detected (green flash) → checkmark, brief celebration
  - Auto-advances to next speed after detection
- Progress indicator: "3 of 11 speeds"
- Control buttons:
  - "Redo" (re-test current speed)
  - "Skip" (move to next without recording)
  - "Done" (finish early with partial results)

**Calibration Flow** (Two-Phase):
1. **Setup Phase**: User adjusts focus/zoom using vertical sliders on right edge (zoom on top, focus below), then taps "Begin Detecting" (centered at bottom)
2. **Baseline Phase** (0-50% progress):
   - "Establishing baseline..." message with progress bar
   - Collects 60 frames to determine dark baseline
   - User holds camera steady
3. **Calibration Shutter Phase**:
   - "Fire Shutter Once" prompt with camera icon
   - "(This event will not be recorded)" note
   - User fires shutter once to calibrate detection threshold
   - System captures peak brightness and calculates final threshold
4. **Detection Phase**: Normal speed prompts begin (e.g., "1/1000")

**Behavior**:
- **Auto-exposure**: Camera automatically adjusts to keep baseline relatively dark (optimizes for event detection)
- **High-speed mode**: Automatically uses device's maximum slow-motion frame rate
- Video recording runs continuously in background
- App monitors brightness in real-time for shutter events
- On detection: clip that segment, tag with current speed, advance
- Visual/haptic feedback on successful detection

---

### 5.5 Event Review Screen (NEW - CRITICAL)
**Purpose**: Let user verify and correct detected events before final calculations

**Layout** (per event, shown one at a time):
- Event label: "Event 1: 1/1000"
- Frame strip showing:
  - **[+] "Earlier frames"** button (left side) - adds 3 frames before
  - 1 **context frame** before event (dark, outside detection boundary)
  - All **detected event frames** (brightness above threshold)
  - 1 **context frame** after event (dark, outside detection boundary)
  - **"Later frames" [+]** button (right side) - adds 3 frames after
- Each frame shows:
  - Frame thumbnail (actual video frame)
  - Brightness value or weight indicator
  - Color coding: green (full open), orange (partial), red/gray (closed)

**Interactions**:
- **Tap frame to cycle state**: Full → Partial → Closed → Full
  - Useful if detection boundary is slightly off
  - User can promote closed frame to included, or exclude edge frame from event
- **[+] "Earlier/Later frames" buttons**: Add 3 more frames on that side (closed by default)
  - Can click multiple times to keep adding frames
  - Useful when detection missed start/end of shutter event
- **Navigation**: "Previous Event" / "Next Event" buttons
- **Progress**: "Reviewing 3 of 11 events"
- **Finish**: "Confirm & Calculate" button when done reviewing all

**Example visual**:
```
Event 3: 1/250
┌───────────────────────────────────────────────────────────┐
│ [+]     │ [out] │ [EVT] [EVT] [EVT] [EVT] │ [out] │  [+]     │
│ Earlier │  14   │  187   245   242   95   │  15   │  Later   │
│ frames  │ gray  │ green green green orange│ gray  │  frames  │
└───────────────────────────────────────────────────────────┘
         Tap any frame to cycle state
              [← Previous]  [Next →]
```

See [EXPAND_FRAMES_FEATURE.md](EXPAND_FRAMES_FEATURE.md) for detailed implementation spec.

---

### 5.6 Processing Screen
**Purpose**: Show progress while calculating shutter speeds from reviewed events

**Elements**:
- Progress indicator (calculating X of Y events)
- Brief animation or status text
- Cancel button (returns to review)

---

### 5.7 Results Dashboard
**Purpose**: Display test results with rich visualizations

**Sections**:

**Header**:
- Camera name (or "Unnamed Session")
- Test date/time
- Overall accuracy summary (e.g., "Average: 5% deviation")

**Accuracy Table**:
- Columns: Expected | Measured | Error %
- Values shown as fractions (e.g., "1/500", "1/472")
- Color-coded rows: green (0-10%), yellow (10-30%), orange (30-60%), red (>60%)

**Brightness Timeline Graph**:
- X-axis: time (or frame number)
- Y-axis: brightness
- Marked regions for each detected shutter event
- Labeled with speed setting

**Speed Comparison Graph**:
- Expected speeds vs measured speeds overlay
- Visual representation of where camera runs fast/slow

**Actions**:
- "Save to Camera Profile" (if started as unnamed)
- "Test Again" (new session, same camera)
- "Back to Home"

---

### 5.8 Camera Detail / History
**Purpose**: View saved camera and past test sessions

**Elements**:
- Camera name (editable)
- List of past test sessions (date, overall accuracy)
- Tap session → View that session's full results dashboard
- Aggregated stats across all sessions (optional future feature)

**Actions**:
- Tap session → Results dashboard for that session
- Delete camera (with confirmation)
- Rename camera

---

### 5.9 Import Flow
**Purpose**: Analyze existing slow-motion video

**Screens**:
1. File picker (gallery/files)
2. Speed labeling screen:
   - Video playback with detected events marked
   - User assigns speed to each event
   - Or: User enters expected speeds in order before analysis
3. Event Review screen (same as 5.5 - verify/correct detections)
4. → Results dashboard

---

### 5.10 Settings
**Purpose**: App configuration

**Options**:
- Default speed set (standard or custom preset)
- Detection sensitivity (advanced)
- Reset tutorial
- About / version info

---

### 5.11 Theory Screen
**Purpose**: Educational content explaining how shutter measurement works

**Content**:
- What is Shutter Speed?
- Why Measure Shutter Speed?
- The Measurement Principle (high-speed video capture)
- Brightness Analysis explanation
- The Math (frame counting formula)
- Intermediate Frame Weighting

**Access**:
- From Home screen help menu → "How It Works"
- From Settings screen → "How It Works"

---

### 5.12 Event Preview Screen
**Purpose**: View details of a single detected event during import

**Elements**:
- Event header (event number, assigned speed)
- Frame thumbnails for all frames in the event
- Brightness values for each frame
- Delete event button

**Access**:
- Tap on any event card in Import screen

**Actions**:
- Delete event (removes from analysis)
- Back to import screen

---

## 6. User Flows

### 6.1 Primary Flow: New Test Session

```
Home
  │
  ├─→ [New Test]
  │     │
  │     ▼
  │   Recording Setup
  │     │ (enter camera name, select speed set: default or custom)
  │     ▼
  │   Recording Screen
  │     │ (auto-adjusts to dark baseline, high-speed mode)
  │     │ (guided prompts, live detection)
  │     │ (fires shutter at each prompted speed)
  │     │ (auto-advances on detection)
  │     ▼
  │   Event Review Screen ← NEW
  │     │ (shows each event's frames + 2 context frames on each side)
  │     │ (tap frames to include/exclude, correct boundaries)
  │     │ (navigate through all events)
  │     ▼
  │   Processing Screen
  │     │ (calculating shutter speeds from reviewed events)
  │     ▼
  │   Results Dashboard
  │     │ (view results, save to profile)
  │     ▼
  └───Home (camera now in list)
```

### 6.2 Secondary Flow: Import Existing Video

```
Home
  │
  ├─→ [Import Video]
  │     │
  │     ▼
  │   File Picker
  │     │
  │     ▼
  │   Speed Labeling
  │     │ (assign speeds to detected events)
  │     ▼
  │   Event Review Screen
  │     │ (verify/correct frame boundaries)
  │     ▼
  │   Processing
  │     │
  │     ▼
  │   Results Dashboard
  │     │
  │     ▼
  └───Home
```

### 6.3 View History Flow

```
Home
  │
  ├─→ [Tap Camera]
  │     │
  │     ▼
  │   Camera Detail
  │     │ (list of past sessions)
  │     │
  │     ├─→ [Tap Session] → Results Dashboard
  │     │
  │     └─→ [Test Again] → Recording Setup (pre-filled)
```

### 6.4 First-Time User Flow

```
App Launch (first time)
  │
  ▼
Onboarding Tutorial
  │ (swipeable screens, skippable)
  ▼
Home (empty state)
  │
  ▼
[New Test] → Normal flow
```

---

## 7. Technical Considerations

### 7.1 Frame Rate & Accuracy Limits

The device's slow-motion capability directly determines measurement accuracy:

| Device FPS | Accurate Range | Limited Accuracy |
|------------|----------------|------------------|
| 240 fps | 1/250 and slower | 1/500, 1/1000 (can detect gross errors only) |
| 120 fps | 1/125 and slower | 1/250+ (can detect gross errors only) |
| 60 fps | 1/60 and slower | Not recommended for fast speeds |

**App behavior**:
- Detect device's maximum slow-motion frame rate on startup
- Display accuracy warning for speeds beyond reliable range
- Still allow testing fast speeds, but flag results as "approximate"

### 7.2 Video Recording Requirements

- **API**: Camera2 API or CameraX for slow-motion recording
- **Format**: H.264 recommended for compatibility
- **Storage**: Clips stored temporarily during session, optionally saved after
- **Real-time analysis**: Monitor frames during recording for event detection

### 7.3 Real-Time Detection Challenge

The core UX requires detecting shutter events *while recording*:
- Need to analyze frames as they're captured
- Must not block recording pipeline
- Brightness calculation is lightweight (mean of grayscale frame)
- Detection threshold established during first few seconds of "baseline"

**Approach**:
1. First 2-3 seconds: establish baseline brightness (shutter closed)
2. Calculate threshold (baseline + margin)
3. Monitor for brightness spike > threshold
4. On detection: mark timestamp, save clip boundaries, advance prompt

### 7.4 Processing Considerations

- **Library**: OpenCV4Android SDK for image processing
- **Weighted frame analysis**: Same algorithm as CLI tool
- **Memory**: Process frames sequentially, don't load full video
- **Battery**: Show warning for long sessions

### 7.5 Data Storage

- **Local database**: SQLite or Room for camera profiles and sessions
- **Video clips**: Store in app-private storage
- **Cleanup**: Option to delete raw clips after analysis (keep results only)

### 7.6 Minimum Device Requirements

- Android 8.0+ (API 26) for Camera2 slow-motion APIs
- Slow-motion video capability (120fps minimum recommended)
- ~500MB free storage for recording sessions

---

## 8. Open Items / Future Considerations

- [ ] Determine exact detection algorithm tuning for real-time use
- [ ] Test on variety of Android devices with different camera capabilities
- [ ] Consider: aggregate multiple test sessions to show camera "drift" over time
- [ ] Consider: comparison mode to overlay two different test sessions

---

## 9. References

- [WIREFRAMES.md](WIREFRAMES.md) - ASCII wireframes for all screens
- [TECH_STACK.md](TECH_STACK.md) - Technology stack and architecture
- [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md) - Implementation phases and porting guide
- [REQUIREMENTS.md](../../REQUIREMENTS.md) - Original CLI requirements
- [THEORY.md](THEORY.md) - Shutter measurement theory
- [research/google-play-publishing/](../../docs/research/google-play-publishing/) - Play Store publishing guides
- [architecture.md](../../architecture.md) - CLI tool API documentation (reference for algorithm)


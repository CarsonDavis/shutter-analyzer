# How To Measure Camera Shutter Speeds

This guide walks you through the process of measuring your camera's actual shutter speeds using video analysis.

---

## Equipment Needed

1. **Smartphone with slow-motion capability**
   - iPhone 6 or later (120fps or 240fps)
   - Android with slow-motion support (varies by model)

2. **Tripod or stable surface**
   - Phone must be completely still during recording

3. **Bright light source**
   - Window with daylight
   - Desk lamp or flashlight
   - LED panel

4. **The camera to test**
   - Remove the lens (if possible)
   - Open the camera back

5. **Optional: Tripod for camera**
   - Keeps everything steady

---

## Setup

### Positioning

```
[LIGHT SOURCE]
      |
      v
[CAMERA SHUTTER] <-- facing phone
      |
      v
[PHONE CAMERA]
```

1. Place your light source behind the camera (shining through the shutter opening)
2. Open the camera back
3. Position your phone to see through the shutter from the front
4. The shutter blades should fill most of the phone's frame

### Ideal Setup

- Phone is 6-12 inches from the camera
- Shutter opening is clearly visible
- Bright light visible through the opening when shutter fires
- Dark when shutter is closed
- No reflections or glare on phone lens

---

## Phone Settings

### iPhone

1. Open the **Camera** app
2. Swipe to **Slo-Mo** mode
3. Check recording fps:
   - Go to **Settings → Camera → Record Slo-mo**
   - Select **240 fps** if available (recommended)
   - 120 fps works for slower shutter speeds

### Android

Settings vary by manufacturer:

**Samsung:**
- Camera app → More → Super Slow-mo or Slow motion
- Look for 240fps option in settings

**Google Pixel:**
- Camera app → Slow Motion
- Settings may allow fps selection

**Other Android:**
- Look for "Slow motion", "High-speed", or "Sports" video mode
- Check settings for fps options

### Important Notes

- **Know your actual recording fps!** The tool needs this to calculate correctly.
- Higher fps = more accurate measurements
- 240fps recommended for most shutter speeds

---

## Recording Procedure

### Step 1: Prepare

1. Set up camera and phone as described above
2. Start with the fastest shutter speed you want to test
3. Cock the shutter (wind the camera)

### Step 2: Record

1. Start slow-motion recording on your phone
2. Wait 1-2 seconds (for stable baseline)
3. Fire the shutter
4. Wait 1-2 seconds

### Step 3: Repeat for Each Speed

For each shutter speed you want to test:
1. Change to the next speed setting
2. Cock the shutter
3. Wait 1 second
4. Fire the shutter
5. Wait 1 second

### Step 4: Stop Recording

Stop recording after testing all desired speeds.

### Tips for Good Results

- Keep the phone absolutely still
- Maintain consistent lighting throughout
- Wait between shots so events are clearly separated
- Work from fastest to slowest (or slowest to fastest) for easy matching
- Record multiple shots per speed for verification

---

## Transferring Video

### iPhone

**Option 1: AirDrop**
- Open Photos, select the video
- Tap Share → AirDrop → Your computer

**Option 2: Cable**
- Connect iPhone to computer via USB/Lightning
- Use Image Capture (Mac) or Photos app to import

**Option 3: Cloud**
- Upload to iCloud, Dropbox, or Google Drive
- Download on computer

### Android

**Option 1: USB Transfer**
- Connect phone to computer via USB
- Select "File Transfer" mode on phone
- Navigate to DCIM folder, copy video

**Option 2: Cloud**
- Upload to Google Drive, Dropbox, etc.
- Download on computer

### Save Location

Place the video file in the `videos/` folder of the shutter-analyzer project:
```
shutter-analyzer/
├── videos/
│   └── your_camera_test.mp4   <-- here
```

---

## Running the Analysis

### Basic Usage

```bash
uv run python -m shutter_analyzer videos/your_camera_test.mp4
```

### With Options

```bash
# Specify the actual recording fps (important for slow-mo!)
uv run python -m shutter_analyzer videos/test.mp4 --recording-fps 240

# Use z-score method with known number of events
uv run python -m shutter_analyzer videos/test.mp4 --method zscore --events 8
```

### What You'll See

```
Analyzing video: your_camera_test.mp4
  Frames: 12000 @ 30.00 fps
  Duration: 400.0 seconds
  Method: original

Detecting shutter events...
  Found 8 shutter events
  Baseline: 2.34
  Threshold: 45.67
  Peak brightness: 187.23

Timeline saved to: outputs/your_camera_test/brightness_timeline.png

==================================================
RESULTS
==================================================
Event   Frames   Weighted   Measured
----------------------------------------
1       2        1.73       1/139
2       3        2.45       1/98
...
```

### Entering Expected Speeds

After the initial analysis, you'll be prompted:

```
Enter expected speeds to compare (e.g., '1/500, 1/250, 1/125')
Press Enter to skip:
> 1/500, 1/250, 1/125, 1/60, 1/30, 1/15, 1/8, 1/4
```

Enter speeds in order from fastest to slowest, matching the order you shot them.

---

## Interpreting Results

### The Comparison Table

```
Event   Frames   Weighted   Measured     Expected     Variation
-----------------------------------------------------------------
1       2        1.73       1/139        1/500        +160%
2       3        2.45       1/98         1/250        +155%
...
```

### Variation Percentage

- **Positive (+)**: Shutter is slower than marked (overexposure risk)
- **Negative (-)**: Shutter is faster than marked (underexposure risk)

### Color Coding

- **Green** (0-5%): Excellent, within normal tolerance
- **Yellow** (5-10%): Acceptable, slight adjustment needed
- **Orange** (10-15%): Noticeable deviation
- **Red** (15%+): Significant error, service recommended

### What's Normal?

- New cameras: ±5% variation is typical
- Vintage cameras: ±10-20% is often acceptable
- Faster speeds tend to have higher percentage variation
- Slower speeds (1/30 and longer) are usually more accurate

---

## Troubleshooting

### No events detected

- Light may be too dim or too bright
- Try adjusting threshold with `--method zscore --events N`
- Check that shutter is visible in the frame

### Too many events detected

- Flickering lights causing false positives
- Use steady DC lighting
- Try `--method dbscan --events N`

### Speeds seem way off

- Check recording fps setting
- Use `--recording-fps` flag with actual value
- Verify you're using slow-motion mode

### Events not matching expected order

- Events are sorted by duration, not time
- Make sure you shot speeds in consistent order

---

## Output Files

After analysis, check the `outputs/<video-name>/` folder:

| File | Description |
|------|-------------|
| `brightness_timeline.png` | Graph showing brightness over time with detected events |
| `results.md` | Markdown report with all measurements |

---

## Example Workflow

```bash
# 1. Record video of your Nikon F3 at various speeds
#    Shot at 240fps on iPhone in slow-mo mode

# 2. Transfer to computer
cp /path/to/IMG_1234.MOV videos/nikon_f3_test.mp4

# 3. Run analysis
uv run python -m shutter_analyzer videos/nikon_f3_test.mp4 --recording-fps 240

# 4. Enter expected speeds when prompted
> 1/2000, 1/1000, 1/500, 1/250, 1/125, 1/60, 1/30, 1/15

# 5. Check results
open outputs/nikon_f3_test/results.md
open outputs/nikon_f3_test/brightness_timeline.png
```

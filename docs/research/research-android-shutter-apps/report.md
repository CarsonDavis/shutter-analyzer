# Android Apps for Measuring Mechanical Camera Shutter Speeds

**Research Date:** 2026-01-24

---

## Executive Summary

There is **only one dedicated Android app** for measuring mechanical camera shutter speeds: the **Shutter-Speed app** by Filmomat. It works best with a companion hardware sensor called PhotoPlug (~$40), which plugs into the phone's headphone jack. Without the hardware, the app can only measure speeds of 1/30 second or slower using sound analysis.

**No Android apps currently exist** that analyze slow-motion video recordings of shutters to measure timing. This represents a gap in the market that the shutter-analyzer Python project could potentially fill.

---

## Apps Found

### 1. Shutter-Speed by Filmomat GmbH

| Attribute | Details |
|-----------|---------|
| **Platform** | Android and iOS |
| **Cost** | App is free; PhotoPlug hardware ~$40 |
| **Google Play** | [Shutter-Speed](https://play.google.com/store/apps/details?id=com.plug.photo.shutter_speed) |
| **Last Updated** | Active as of 2025 |

#### How It Works

**With PhotoPlug (Optical Mode):**
1. Open the camera's back
2. Point the lens at a bright light source
3. Position the PhotoPlug sensor behind the shutter
4. Fire the shutter
5. Light passes through and hits the sensor
6. App displays a waveform showing open/close timing
7. Calculates deviation from expected speed in f-stops

**Without PhotoPlug (Acoustic Mode):**
1. Use phone's microphone to record shutter sound
2. App analyzes sound waveform for open/close events
3. Only works for speeds 1/30 second or slower

#### Accuracy

| Mode | Speed Range | Accuracy |
|------|-------------|----------|
| Optical (PhotoPlug) | Up to 1/500s (leaf shutters), faster for focal plane | High |
| Acoustic only | 1/30s and slower | Limited |

#### Limitations

- Requires bright, consistent light source for optical mode
- Phones without headphone jacks need TRRS adapter
- UI described by users as "unintuitive"
- Must input expected shutter speed each time (slow workflow)

#### User Feedback

> "I have used this on dozens of cameras before listing on eBay. Count me as a happy user."

> "The UI is seriously unintuitive, clearly made by an engineer."

---

### 2. Sound Oscilloscope (Generic Alternative)

| Attribute | Details |
|-----------|---------|
| **Platform** | Android |
| **Cost** | Free |
| **Purpose** | General-purpose oscilloscope, not shutter-specific |

#### How It Works

Some users repurpose generic oscilloscope apps to visualize shutter sounds:
1. Position phone microphone near camera
2. Set oscilloscope to ~500ms per division
3. Fire shutter
4. Look for spikes indicating open/close events

#### Accuracy

- Rough estimates up to 1/100 second with ~20% accuracy
- Speeds faster than 1/250 difficult to distinguish from noise
- **Community consensus:** "Not an adjustment tool, just an assessment tool"

---

## DIY Methods (No Dedicated App)

### Slow-Motion Video Frame Counting

This is the approach most similar to the shutter-analyzer project:

**Process:**
1. Record shutter actuation at 240 fps
2. Transfer video to computer
3. Open in video editor (HitFilm Express, etc.)
4. Count frames between shutter open and close
5. Calculate: `Actual speed = Total frames / 240`

**Limitations:**
- Maximum measurable speed: ~1/120 second at 240 fps
- Requires manual frame counting
- No automated analysis app exists for Android

**Opportunity:** An app that automates this video analysis would fill a clear gap.

### DIY Hardware

- **Photodiode circuits** connected to oscilloscope apps
- **Arduino-based testers** with photoresistors
- **Homemade PhotoPlug** using circuit from artdecocameras.com

---

## Comparison Table

| Solution | Cost | Speed Range | Accuracy | Effort Required |
|----------|------|-------------|----------|-----------------|
| Shutter-Speed + PhotoPlug | ~$40 | Up to 1/500s | High | Low |
| Shutter-Speed (sound only) | Free | 1/30s and slower | Low | Low |
| Sound Oscilloscope apps | Free | 1/30s and slower | ~20% | Medium |
| Video frame counting | Free | Up to ~1/120s | Medium | High (manual) |
| DIY optical sensors | $15-30 | Up to 1/1000s | High | High (electronics) |

---

## Key Takeaways

1. **PhotoPlug + Shutter-Speed app** is the most practical commercial solution for Android users testing mechanical shutters.

2. **No video-analysis apps exist** for Android that analyze slow-motion recordings of shutters. All current solutions use either:
   - Light sensors (PhotoPlug)
   - Sound analysis (limited to slow speeds)
   - Manual frame counting (tedious)

3. **The shutter-analyzer Python project** addresses this gap by automating the video brightness analysis that users currently do manually.

4. **Market opportunity:** A mobile app that processes slow-motion video (240-960 fps) to automatically detect shutter open/close timing would be novel and useful.

---

## Sources

- [Filmomat PhotoPlug](https://www.filmomat.eu/photoplug)
- [Shutter-Speed on Google Play](https://play.google.com/store/apps/details?id=com.plug.photo.shutter_speed)
- [PhotoPlug Review - The Phoblographer](https://www.thephoblographer.com/2025/05/30/photoplug-the-smart-way-to-test-shutter-speeds-on-old-cameras/)
- [Photrio Forum - Using Apps to Measure Shutter Speeds](https://www.photrio.com/forum/threads/using-apps-to-measure-shutter-speeds.159996/)
- [Stearman Press - Measure Your Shutter Speed for Free](https://shop.stearmanpress.com/blogs/news/measure-your-shutter-speed-for-free)
- [Art Deco Cameras - Finding Shutter Speed](http://www.artdecocameras.com/resources/shutterspeed/)
- [35mmc - DIY Shutter Speed Tester](https://www.35mmc.com/24/05/2023/a-diy-shutter-speed-tester/)

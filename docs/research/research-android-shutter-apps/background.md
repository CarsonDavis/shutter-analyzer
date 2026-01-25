# Research: Android Apps for Measuring Mechanical Camera Shutter Speeds

**Date:** 2026-01-24

---

## Sources

1. [Filmomat PhotoPlug Official Site](https://www.filmomat.eu/photoplug)
2. [Shutter-Speed App on Google Play](https://play.google.com/store/apps/details?id=com.plug.photo.shutter_speed&hl=en)
3. [PhotoPlug Review - The Phoblographer (May 2025)](https://www.thephoblographer.com/2025/05/30/photoplug-the-smart-way-to-test-shutter-speeds-on-old-cameras/)
4. [PhotoPlug Introduction - Silvergrain Classics](https://silvergrainclassics.com/en/2020/06/introducing-the-photoplug-easy-shutter-speed-testing-for-your-smartphone/)
5. [Photrio Forum - Using Apps to Measure Shutter Speeds](https://www.photrio.com/forum/threads/using-apps-to-measure-shutter-speeds.159996/)
6. [Photrio Forum - Smartphone App to Measure Shutter Speed](https://www.photrio.com/forum/threads/smartphone-app-to-measure-shutter-speed.178305/)
7. [Stearman Press - Measure Your Shutter Speed for Free](https://shop.stearmanpress.com/blogs/news/measure-your-shutter-speed-for-free)
8. [Art Deco Cameras - Finding Shutter Speed of a Vintage Camera](http://www.artdecocameras.com/resources/shutterspeed/)
9. [CatLABS - PhotoPlug Product Page](https://www.catlabs.info/product/photoplug-optical-shutter-speed-tester)
10. [35mmc - A DIY Shutter Speed Tester](https://www.35mmc.com/24/05/2023/a-diy-shutter-speed-tester/)
11. [Hackaday - Clock Your Camera With This Shutter Speed Tester](https://hackaday.com/2023/02/07/clock-your-camera-with-this-shutter-speed-tester/)

---

## Key Findings

### Primary Solution: Shutter-Speed App by Filmomat

**The only dedicated Android app** for measuring mechanical camera shutter speeds is the **Shutter-Speed app** by Filmomat GmbH. It has two operating modes:

#### 1. Optical Mode (with PhotoPlug hardware)
- **PhotoPlug** is a small light-sensitive sensor that plugs into the smartphone's headphone jack
- Works with both central (leaf) shutters and focal plane shutters
- **Price:** ~29.90 EUR (~$40 USD) for the PhotoPlug sensor
- **App is free** on Google Play Store, no ads or in-app purchases
- If phone lacks headphone jack, requires a TRRS adapter cable

**How it works:**
- Open camera back, point lens at bright light source
- Position PhotoPlug behind the shutter
- Fire shutter, light pulse hits sensor
- App displays waveform with two peaks (open/close)
- Calculates deviation from expected speed in f-stops
- Measurements can be saved to phone

**Accuracy and limitations:**
- Central shutters: fastest measurable speed is **1/500s**
- Focal plane shutters: can measure much faster speeds
- Requires bright, consistent light source
- UI described as "unintuitive, made by an engineer"

#### 2. Acoustic Mode (no hardware required)
- Uses phone microphone to record shutter sound
- **Only suitable for speeds 1/30 sec or slower**
- Faster speeds do not give usable results
- Sound alone cannot accurately determine when shutter actually opens/closes

### Alternative: Sound Oscilloscope Apps

Forum users have experimented with generic oscilloscope apps:

**App mentioned:** "Sound Oscilloscope" (free on Android)

**Method:**
- Position phone microphone near camera
- Set oscilloscope to ~500ms per division
- Fire shutter, observe waveform spikes

**Accuracy:**
- User reported measurements up to 1/100 with ~20% accuracy
- 1/250 and faster difficult to distinguish from noise
- One user called this "an assessment tool, not an adjustment tool"
- Described as giving "rough ballpark" rather than precise measurements

**Expert opinion from forums:** "The sound of a shutter firing is not an accurate way to measure speeds. The actual light passing through the shutter is the only proper way."

### DIY Video Frame-Counting Method

**Requirements:**
- Smartphone recording at 240 fps (most modern phones support this)
- Bright light source (LED panel recommended)
- Video editing software (HitFilm Express mentioned as free option)

**Process:**
1. Point camera at bright, evenly-lit surface
2. Mount phone to record ground glass/film plane
3. Record at 240 fps while firing each shutter speed
4. Count frames between shutter opening and closing
5. Calculate: Actual speed = Total frames / 240

**Limitations:**
- Sampling theory limits to **1/120 second maximum** at 240 fps
- Practical accuracy only for speeds slower than 1/60 second
- Faster speeds become unreliable
- Works for "90% of large format photos" where slower speeds dominate

**Example results from one user:**
- Marked 1/4 second measured as 1/3 second (reasonable variance)

### DIY Hardware Solutions

Several DIY approaches mentioned in forums:

1. **Photodiode + Oscilloscope circuits**
   - Solar panel from garden light can work up to 1/1000
   - DSO138 oscilloscope available for under 15 GBP
   - Requires soldering/electronics knowledge

2. **Arduino-based testers**
   - Photoresistor connected to Arduino + OLED display
   - Limitation: Light-dependent resistors have slow response time
   - Not reliable above 1/125 second

3. **DIY PhotoPlug clone**
   - Circuit diagram available on artdecocameras.com
   - iOS and Android have reversed Ground/Mic connections on plug
   - Requires basic electronics skills

### Phyphox App (Limited Relevance)

The **phyphox** physics app (free, open source) has some camera-based features:
- Brightness stopwatch
- Color stopwatch
- Luminance measurements

However, it is **not specifically designed for shutter speed testing** and would require manual interpretation of brightness changes.

---

## Community Consensus

From forum discussions:

1. **PhotoPlug + Shutter-Speed app** is the most practical solution for hobbyists
2. **Sound-only methods** are limited to very slow speeds and low accuracy
3. **Video frame counting** works for slow speeds but requires manual effort
4. **Professional calibration** still recommended for critical work
5. **DIY optical sensors** can match PhotoPlug accuracy but require electronics skills

---

## Gaps in Android App Ecosystem

**No apps found that:**
- Automatically analyze slow-motion video recordings of shutters
- Use phone's slow-motion camera (240/960 fps) for optical shutter timing
- Provide frame-by-frame video analysis specifically for shutter measurement
- Combine OpenCV-style brightness analysis with shutter timing

The shutter-analyzer project in this repo appears to fill this gap by using Python + OpenCV to analyze video recordings of mechanical shutters.

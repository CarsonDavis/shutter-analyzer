# Expandable Frame Boundaries Feature

## Overview
Allow users to expand the frame boundaries of detected events in the Event Review screen. This addresses cases where the automatic detection may have missed the start or end of a shutter event.

## Problem Statement
The current event detection algorithm may occasionally:
- Start slightly after the shutter actually opened (missing early partial-open frames)
- End slightly before the shutter fully closed (missing late partial-close frames)

Users need a way to manually extend the event boundaries to include these frames for more accurate measurements.

## User Flow
```
Event Review Screen
├── [+] Examine more ← Click to add 3 frames before
├── Frame Grid (1 context + event frames + 1 context)
└── Examine more [+] ← Click to add 3 frames after
```

## Design Specification

### Default Context Frames
- **Previous**: 2 frames before + 2 frames after (automatic)
- **New**: 1 frame before + 1 frame after (automatic)
- Rationale: Cleaner UI, users can expand if needed

### Expand Frames Button
- **Position**: Left side (before frames) and right side (after frames)
- **Icon**: Plus icon (`Icons.Default.Add`)
- **Label**: "Examine more" (below icon, small text)
- **Styling**: Match existing frame thumbnail size, subtle appearance

### Behavior
1. **Click left [+]**: Add 3 frames before the current first frame
2. **Click right [+]**: Add 3 frames after the current last frame
3. **New frames**: Excluded by default (user must tap to include)
4. **Repeatable**: Can click [+] multiple times to keep adding frames
5. **Boundary check**: Don't add frames before frame 0 or after video end

### Visual Layout
```
┌─────────────────────────────────────────────────────────────────┐
│ EVENT 1: 1/500                                                  │
│ Tap frames to cycle: Full → Partial → Excluded                  │
│                                                                 │
│ ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐│
│ │ +  │ │ctx │ │ F1 │ │ F2 │ │ F3 │ │ F4 │ │ F5 │ │ctx │ │ +  ││
│ │    │ │    │ │    │ │    │ │    │ │    │ │    │ │    │ │    ││
│ │more│ │#99 │ │#100│ │#101│ │#102│ │#103│ │#104│ │#105│ │more││
│ └────┘ └────┘ └────┘ └────┘ └────┘ └────┘ └────┘ └────┘ └────┘│
│                                                                 │
│ Legend: [■] Full  [■] Partial  [■] Closed                       │
└─────────────────────────────────────────────────────────────────┘
```

After clicking left [+] once:
```
┌─────────────────────────────────────────────────────────────────────────┐
│ ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐ ... ┌────┐ ┌────┐ ┌────┐    │
│ │ +  │ │#96 │ │#97 │ │#98 │ │#99 │ │#100│     │#104│ │#105│ │ +  │    │
│ │more│ │ X  │ │ X  │ │ X  │ │ctx │ │ F1 │     │ctx │ │    │ │more│    │
│ └────┘ └────┘ └────┘ └────┘ └────┘ └────┘     └────┘ └────┘ └────┘    │
│         (new, excluded)                                                 │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Implementation Plan

### Constants
```kotlin
companion object {
    private const val DEFAULT_CONTEXT_FRAMES = 1      // Changed from 2
    private const val FRAMES_TO_ADD_ON_EXPAND = 3     // Frames added per click
    private const val MAX_FRAMES_PER_EVENT = 12
    private const val MAX_EVENTS_IN_MEMORY = 5
}
```

### State Management (EventReviewViewModel)

#### New State Variables
```kotlin
// Track extra frames added by user (event index -> count)
private val extraFramesBefore = mutableMapOf<Int, Int>()  // Default: 0
private val extraFramesAfter = mutableMapOf<Int, Int>()   // Default: 0
```

#### New Methods
```kotlin
/**
 * Add more frames before the event start.
 * Adds FRAMES_TO_ADD_ON_EXPAND frames (excluded by default).
 */
fun addFramesBefore(eventIndex: Int) {
    val currentExtra = extraFramesBefore[eventIndex] ?: 0
    extraFramesBefore[eventIndex] = currentExtra + FRAMES_TO_ADD_ON_EXPAND

    // Re-extract thumbnails for this event with new frame range
    reloadEventThumbnails(eventIndex)
}

/**
 * Add more frames after the event end.
 * Adds FRAMES_TO_ADD_ON_EXPAND frames (excluded by default).
 */
fun addFramesAfter(eventIndex: Int) {
    val currentExtra = extraFramesAfter[eventIndex] ?: 0
    extraFramesAfter[eventIndex] = currentExtra + FRAMES_TO_ADD_ON_EXPAND

    // Re-extract thumbnails for this event with new frame range
    reloadEventThumbnails(eventIndex)
}

/**
 * Get total context frames before event (default + user-added).
 */
fun getContextFramesBefore(eventIndex: Int): Int {
    return DEFAULT_CONTEXT_FRAMES + (extraFramesBefore[eventIndex] ?: 0)
}

/**
 * Get total context frames after event (default + user-added).
 */
fun getContextFramesAfter(eventIndex: Int): Int {
    return DEFAULT_CONTEXT_FRAMES + (extraFramesAfter[eventIndex] ?: 0)
}
```

### Modified Methods

#### createReviewEvent()
```kotlin
private fun createReviewEvent(
    index: Int,
    event: ShutterEvent,
    fps: Double,
    thumbnails: Map<Int, Bitmap>
): ReviewEvent {
    // ... existing code ...

    // Use dynamic context frame counts
    val contextBeforeCount = getContextFramesBefore(index)
    val contextAfterCount = getContextFramesAfter(index)

    // Generate context before indices: -contextBeforeCount to -1
    val contextBeforeIndices = (-contextBeforeCount until 0).toList()

    // ... build contextBefore frames ...

    // Generate context after indices
    val contextAfterIndices = (0 until contextAfterCount).toList()

    // ... build contextAfter frames ...
}
```

#### loadThumbnailsForEvent() / prefetchEvent()
Update frame number collection to use dynamic context counts:
```kotlin
val contextBefore = getContextFramesBefore(eventIndex)
val contextAfter = getContextFramesAfter(eventIndex)

// Context frames before
for (i in contextBefore downTo 1) {
    frameNumbers.add(event.startFrame - i)
}

// ... event frames ...

// Context frames after
for (i in 1..contextAfter) {
    frameNumbers.add(event.endFrame + i)
}
```

### UI Changes (EventReviewScreen)

#### New Composable: ExpandFramesButton
```kotlin
@Composable
private fun ExpandFramesButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(80.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 16f)  // Match thumbnail aspect
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add more frames",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Examine\nmore",
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

#### Modified EventReviewContent
```kotlin
@Composable
private fun EventReviewContent(
    event: ReviewEvent,
    onCycleFrameState: (Int) -> Unit,
    onAddFramesBefore: () -> Unit,
    onAddFramesAfter: () -> Unit
) {
    // ... header ...

    // Frame grid with expand buttons
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Left expand button
        ExpandFramesButton(onClick = onAddFramesBefore)

        // Frame thumbnails
        event.frames.forEachIndexed { index, frame ->
            FrameThumbnail(
                frame = frame,
                onClick = { onCycleFrameState(index) }
            )
        }

        // Right expand button
        ExpandFramesButton(onClick = onAddFramesAfter)
    }

    // ... legend ...
}
```

---

## Files to Modify

| File | Changes |
|------|---------|
| `ui/screens/review/EventReviewViewModel.kt` | Add extraFramesBefore/After maps, addFramesBefore/After methods, update createReviewEvent and frame extraction |
| `ui/screens/review/EventReviewScreen.kt` | Add ExpandFramesButton composable, update EventReviewContent layout |

---

## Edge Cases

1. **Frame 0 boundary**: Don't allow adding frames before frame 0
   - Solution: Check `event.startFrame - totalContextBefore >= 0` before allowing expand

2. **Video end boundary**: Don't allow adding frames beyond video length
   - Solution: Would need video frame count (may not be available) - allow extraction to fail gracefully

3. **Memory pressure**: Many extra frames could increase memory usage
   - Solution: Extra frames are part of the existing thumbnail cache/LRU system

4. **State persistence**: Extra frames are per-session (not persisted to DB)
   - This is intentional - just for review/adjustment

---

## Verification Checklist

- [ ] Default context reduced from 2 to 1 frame on each side
- [ ] [+] button appears on left side of frame grid
- [ ] [+] button appears on right side of frame grid
- [ ] Clicking [+] left adds 3 frames before, all excluded
- [ ] Clicking [+] right adds 3 frames after, all excluded
- [ ] Can click [+] multiple times to keep adding frames
- [ ] New frames can be tapped to cycle state (Full/Partial/Excluded)
- [ ] Thumbnails load for newly added frames
- [ ] Button styling matches frame thumbnail size
- [ ] Works correctly when navigating between events

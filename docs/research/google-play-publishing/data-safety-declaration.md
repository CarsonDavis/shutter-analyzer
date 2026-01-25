# Data Safety Section - Play Console Answers

Use these answers when completing the Data Safety section in Google Play Console.

---

## Overview Questions

### Does your app collect or share any of the required user data types?

**Answer: No**

Shutter Analyzer does not collect or share any user data. All processing happens locally on the device.

---

## Data Collection

### Does your app collect any of the following data types?

| Data Type | Collected? |
|-----------|------------|
| Location (approximate) | No |
| Location (precise) | No |
| Personal info (name) | No |
| Personal info (email) | No |
| Personal info (user IDs) | No |
| Personal info (address) | No |
| Personal info (phone number) | No |
| Financial info | No |
| Health and fitness | No |
| Messages | No |
| Photos and videos | **Accessed but not collected*** |
| Audio files | No |
| Files and docs | No |
| Calendar | No |
| Contacts | No |
| App activity | No |
| Web browsing | No |
| App info and performance | No |
| Device or other IDs | No |

*The app accesses video files the user explicitly selects, but does not collect, store, or transmit this data outside the user's control.

---

## Data Sharing

### Is any of the collected data shared with third parties?

**Answer: No**

No data is shared with any third parties.

---

## Data Handling Practices

### Is data encrypted in transit?

**Answer: Not applicable**

No data is transmitted. All processing occurs locally on the device.

### Can users request that their data be deleted?

**Answer: Not applicable**

No user data is collected. Users can delete any locally saved analysis results through their device's file manager.

### Does your app follow Google Play's Families policy?

**Answer: Not applicable**

The app is not designed for or marketed to children.

---

## Security Practices

### Is data encrypted?

The app does not store sensitive data. Any saved analysis results (images, text files) are stored using standard Android file system security.

### Does the app provide a way to request deletion of data?

Not applicable - no data collection occurs.

---

## Summary Statement for Data Safety Section

**Suggested text for "About this app's data safety" section:**

```
This app does not collect or share any user data. Videos are processed entirely on your device and are never uploaded or transmitted. Analysis results are saved only to locations you choose.
```

---

## Checklist

Before submitting, confirm:

- [ ] No analytics SDKs are included
- [ ] No advertising SDKs are included
- [ ] No crash reporting services that collect device info
- [ ] No third-party libraries that collect data
- [ ] App does not require internet permission
- [ ] Privacy policy URL is publicly accessible
- [ ] Privacy policy matches these declarations

# Google Play Registration Checklist for Shutter Analyzer

A step-by-step checklist for publishing Shutter Analyzer on Google Play Store.

---

## Phase 1: Account Setup

### Developer Account Registration

- [ ] Create a dedicated Google account (recommended) or use existing
- [ ] Go to [Google Play Console](https://play.google.com/console)
- [ ] Pay $25 registration fee
- [ ] Complete identity verification
- [ ] Accept Developer Distribution Agreement

### Account Type Decision

| If you choose... | You will need... |
|------------------|------------------|
| Personal Account | Government ID, 12-tester closed test for 14 days |
| Organization Account | D-U-N-S number (allow 30 days to obtain) |

---

## Phase 2: Prepare Materials

### Text Content (see `store-listing.md`)

- [ ] App title: `Shutter Analyzer` (16 chars)
- [ ] Short description (75 chars) - DONE
- [ ] Full description (1,847 chars) - DONE
- [ ] Keywords/tags - DONE

### Privacy & Compliance (see other files)

- [ ] Privacy policy written (`privacy-policy.md`) - DONE
- [ ] Host privacy policy on public URL (GitHub Pages, website, etc.)
- [ ] Data safety answers prepared (`data-safety-declaration.md`) - DONE

### Visual Assets (must create)

- [ ] App icon: 512x512 PNG
- [ ] Screenshots: At least 2-8 phone screenshots at 1080x1920
- [ ] Feature graphic: 1024x500 (optional unless adding video)

---

## Phase 3: Build the App

### Technical Requirements

- [ ] Build as Android App Bundle (.aab), not APK
- [ ] Target API level 35 (Android 15) or higher
- [ ] Generate upload signing key:
  ```bash
  keytool -genkeypair -v -storetype PKCS12 \
    -keystore shutter-analyzer-upload.keystore \
    -alias upload-key \
    -keyalg RSA -keysize 2048 -validity 10000
  ```
- [ ] Store keystore file securely (you'll need it for every update)
- [ ] Enroll in Play App Signing when prompted

### Permissions Review

The app likely needs:
- `READ_EXTERNAL_STORAGE` or `READ_MEDIA_VIDEO` - to access user's videos
- No sensitive permissions that require special declarations

---

## Phase 4: Play Console Setup

### Create App Listing

1. [ ] Click "Create app" in Play Console
2. [ ] Enter app name: `Shutter Analyzer`
3. [ ] Select language: English (US)
4. [ ] Select app type: App (not game)
5. [ ] Select free or paid
6. [ ] Accept declarations

### Store Listing Tab

- [ ] Copy title from `store-listing.md`
- [ ] Copy short description from `store-listing.md`
- [ ] Copy full description from `store-listing.md`
- [ ] Upload app icon (512x512)
- [ ] Upload screenshots (minimum 2)
- [ ] Upload feature graphic (if applicable)

### App Content Tab

- [ ] Complete privacy policy section (paste hosted URL)
- [ ] Complete data safety questionnaire (use `data-safety-declaration.md`)
- [ ] Complete content rating questionnaire (see `store-listing.md`)
- [ ] Set target audience (not for children)
- [ ] Complete news apps declaration (No)
- [ ] Complete COVID-19 apps declaration (No)
- [ ] Complete data safety section

---

## Phase 5: Testing (New Personal Accounts Only)

If your personal account was created after November 2023:

- [ ] Upload app to closed testing track
- [ ] Create tester list with 12+ email addresses
- [ ] Invite testers via Play Console
- [ ] Wait for testers to opt in
- [ ] Maintain testing for 14 continuous days
- [ ] Apply for production access

---

## Phase 6: Release

### Pre-launch Checklist

- [ ] All store listing fields completed
- [ ] All app content declarations completed
- [ ] App bundle uploaded and processed
- [ ] No policy warnings in Play Console
- [ ] Reviewed app on internal test track (recommended)

### Submit for Review

- [ ] Create production release
- [ ] Upload signed .aab file
- [ ] Add release notes
- [ ] Review and confirm
- [ ] Submit for review

### After Submission

- [ ] Wait 1-7 days for review
- [ ] Monitor Play Console for status updates
- [ ] Respond promptly to any rejection notices
- [ ] Once approved, app goes live automatically

---

## Files in This Folder

| File | Purpose |
|------|---------|
| `report.md` | Full research on Play Store requirements |
| `store-listing.md` | All text content for store listing |
| `privacy-policy.md` | Privacy policy (host publicly) |
| `data-safety-declaration.md` | Answers for data safety questionnaire |
| `registration-checklist.md` | This checklist |

---

## Quick Reference: What You Still Need to Create

1. **Visual assets** - app icon, screenshots
2. **The actual Android app** - built as .aab targeting API 35+
3. **Hosted privacy policy URL** - put `privacy-policy.md` content on a public webpage
4. **Contact email** - for support inquiries
5. **$25** - registration fee

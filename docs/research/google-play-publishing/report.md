# Complete Guide to Publishing an App on Google Play Store

**Last Updated:** January 24, 2026

---

## Executive Summary

Publishing an app on Google Play requires a $25 one-time developer account fee, identity verification, and compliance with technical and policy requirements. The process takes 1-7 days for initial review, though new personal accounts must first complete a 14-day closed test with at least 12 testers. Apps must be submitted as Android App Bundles (AAB), target Android 15 (API 35) or higher, and use Play App Signing. You will need a privacy policy, Data Safety section completion, IARC content rating, and store listing assets including a 512x512 icon and 2-8 screenshots. Google takes a 15% commission on the first $1M in annual revenue, then 30% thereafter.

---

## 1. Account Requirements

### Account Types

| Feature | Personal Account | Organization Account |
|---------|------------------|---------------------|
| D-U-N-S Number | Not required | Required |
| Testing Requirement | 12 testers for 14 days (accounts after Nov 2023) | None |
| Verification | Government ID, email, phone | D-U-N-S, email, phone, Google Payments profile |
| Setup Time | Days | Days to weeks (D-U-N-S can take 30 days) |

### Registration Requirements

**All Accounts:**
- Valid Google account (dedicated account recommended)
- Credit or debit card for $25 registration fee
- Must be 18 years or older
- Accept the Google Play Developer Distribution Agreement

**Personal Accounts:**
- Government ID verification
- Legal name, address, email, phone verification
- Device verification via Play Console mobile app (accounts created after November 2023)

**Organization Accounts:**
- D-U-N-S number (free from Dun & Bradstreet, but can take up to 30 days)
- Organization name and address verified via Google Payments profile
- One D-U-N-S number can register multiple accounts
- Government organizations may qualify for exceptions

### New Personal Account Testing Requirements

If your personal account was created after November 13, 2023:

1. Upload your app to a closed test track
2. Recruit at least 12 testers who opt in
3. Maintain continuous testing for 14 days
4. Apply for production access via Play Console Dashboard

This requirement does not apply to organization accounts.

---

## 2. Technical Requirements

### App Format

**Android App Bundle (AAB) is mandatory** for all new apps since August 2021. APKs are no longer accepted.

Benefits of AAB:
- Google generates optimized APKs for each device configuration
- Average 15% smaller download size than universal APKs
- Required for Play App Signing integration

**Size Limits:**
- AAB compressed download: Up to 4 GB
- Individual APKs generated from AAB: 150 MB max
- Larger apps should use Play Asset Delivery

### Target API Level

As of August 31, 2025:

| App Type | Minimum Target API |
|----------|-------------------|
| Standard apps (new/updates) | Android 15 (API 35) |
| Wear OS, Android TV, Automotive | Android 14 (API 34) |
| Existing apps (to remain visible) | Android 14 (API 34) |

Apps not meeting requirements are only visible to users on matching or lower Android versions. Extensions to November 1, 2025 are available upon request.

### App Signing

**Play App Signing is mandatory** for apps created after August 2021.

**Two keys are involved:**

1. **Upload Key** (you manage):
   - RSA key, minimum 2048 bits
   - Validity must extend past October 22, 2033
   - Used to sign your AAB for upload
   - Can be reset if lost or compromised

2. **App Signing Key** (Google manages):
   - Google securely stores and protects
   - Used to sign APKs delivered to users
   - Provides additional security layer

**Generate an upload key:**
```bash
keytool -genkeypair -v -storetype PKCS12 -keystore my-upload-key.keystore -alias my-key-alias -keyalg RSA -keysize 2048 -validity 10000
```

---

## 3. Store Listing Requirements

### Required Text Elements

| Element | Requirements |
|---------|-------------|
| App Title | Up to 30 characters |
| Short Description | Up to 80 characters |
| Full Description | Up to 4,000 characters |
| Privacy Policy URL | Required, publicly accessible, non-PDF, non-editable |
| Contact Email | Required |

### Visual Assets

**App Icon (Required):**
- 512 x 512 pixels
- 32-bit PNG with alpha
- Maximum 1 MB
- Full square (Play handles masking)

**Feature Graphic (Required):**
- 1024 x 500 pixels
- JPEG or 24-bit PNG (no alpha)
- Maximum 1 MB
- Only displayed if you add a promo video
- Keep key elements toward center to avoid cutoff

**Screenshots (Required):**
- 2-8 per device type
- JPEG or 24-bit PNG (no alpha)
- Minimum dimension: 320 pixels
- Maximum dimension: 3840 pixels
- Recommended: 1080 x 1920 (portrait) or 1920 x 1080 (landscape)
- At least 4 screenshots at 1080px+ recommended
- First three are most important (shown first in listings)

**Prohibited in Visual Assets:**
- Performance claims ("Best of Play," "#1," awards)
- Promotional pricing ("10% off," "Free limited time")
- Third-party trademarks without permission
- Device imagery
- Google Play badges

### Privacy Policy Requirements

Your privacy policy must:
- Be linked in Play Console and within the app
- Be on an active, publicly accessible URL
- Not be a PDF
- Not be geofenced
- Comprehensively disclose data practices
- Include developer contact information
- Remain non-editable

---

## 4. Policy & Compliance

### Data Safety Section

**Mandatory for all apps**, including those that collect no data.

You must declare:
- What data your app collects
- How data is used
- Whether data is shared with third parties
- Data practices of third-party SDKs in your app
- Security practices (encryption, deletion options)

Users who request account deletion must have their associated data deleted (not just deactivated).

### Content Ratings (IARC)

All apps require a content rating via the International Age Rating Coalition (IARC) system.

**Process:**
1. Complete questionnaire in Play Console
2. Answer questions about content (violence, sexual content, gambling, etc.)
3. Ratings are generated immediately
4. Free of charge

**Regional ratings generated include:**
- ESRB (Americas)
- PEGI (Europe, Middle East)
- ClassInd (Brazil)
- GRAC (South Korea)
- IARC Generic (other regions)

In-app ads must not be more mature than app content.

### Sensitive Permissions

Certain permissions require additional justification and review:

**High-Risk Permissions (require Permissions Declaration Form):**
- SMS and Call Log access
- Background location
- MANAGE_EXTERNAL_STORAGE (Android 11+)

**Permissions Declaration Process:**
1. Form appears during release if app uses restricted permissions
2. Provide justification for permission use
3. Include video demonstration of core functionality
4. Extended review period (up to several weeks)

**Best Practices:**
- Request permissions in context
- Use data only for consented purposes
- Respect declined permission requests
- Provide alternatives for users who decline

---

## 5. Review Process

### Timeline Expectations

| Scenario | Typical Duration |
|----------|-----------------|
| New app | 1-7 days |
| App update | 24-48 hours |
| New account or sensitive features | Longer |
| Permissions Declaration review | Up to several weeks |
| Peak submission periods | Additional delays possible |

### Common Rejection Reasons

**Policy Violations:**
- Inappropriate or restricted content
- Copyright or trademark infringement
- Impersonation
- Misleading claims

**Technical Issues:**
- App crashes or bugs
- Broken links
- Performance problems
- OAuth verification failures

**Listing Problems:**
- Missing or inadequate privacy policy
- Incomplete app description
- Low-quality or non-representative screenshots
- Missing required metadata

**Permission Issues:**
- Undeclared sensitive permissions
- Unjustified permission requests
- Missing Permissions Declaration Form

### Appeal Process

**For Simple Issues:**
Fix the problem and resubmit. No formal appeal needed.

**For Disputed Decisions:**
1. Use the contact details in your enforcement notification email
2. Or use the appeal form linked in the Developer Policy Center
3. Receive automated confirmation with case number
4. Response from specialist within 72 hours

**Enforcement Levels:**
- **Rejection**: Prior version remains available; no account impact
- **Removal**: Published version unavailable; must submit compliant update
- **Suspension**: Forfeit users, ratings, statistics; results from egregious/multiple violations

---

## 6. Costs

### One-Time Fees

| Fee | Amount |
|-----|--------|
| Developer Account Registration | $25 USD |

### Commission Structure

| Revenue Tier | Commission Rate |
|--------------|-----------------|
| First $1M annually | 15% (must enroll in reduced-fee program) |
| Above $1M annually | 30% |
| Subscriptions (first 12 months) | 30% |
| Subscriptions (after 12 months) | 15% |

**Commissions apply to:**
- Paid app downloads
- In-app purchases of digital goods/services via Google Play Billing

**Commissions do NOT apply to:**
- Physical goods and services
- Most advertising revenue

### EU-Specific (Digital Markets Act)

The EU requires alternative distribution and payment options. Google has implemented a tiered system with potentially lower fees for developers using alternative payment systems.

---

## 7. Ongoing Requirements

### Target API Updates

Google requires apps to target recent API levels:
- **New apps and updates**: Within 1 year of latest Android release
- **Existing apps**: Within 2 years to remain visible on newer devices

Failing to update means your app becomes invisible to users on newer Android versions.

### Policy Compliance

Google updates policies regularly. Major updates effective January 1, 2026 include refreshed content policies and data handling requirements. Monitor the Policy Deadlines page for upcoming changes.

### Account Maintenance

- Keep developer contact information current
- Organization changes must first be updated with Dun & Bradstreet
- Re-verification may be required after significant profile changes

### 2026 Developer Verification Program

Starting in 2026 (rolling out globally), all Android developers must verify their identity to have apps installed on certified Android devices. This applies even to apps distributed outside the Play Store.

---

## Checklist: Before You Submit

**Account Setup:**
- [ ] Google Play Developer account created and verified
- [ ] $25 registration fee paid
- [ ] (Organization) D-U-N-S number obtained and verified
- [ ] (Personal, post-Nov 2023) 12-tester closed test completed for 14 days

**Technical:**
- [ ] App built as AAB (not APK)
- [ ] Target API level 35+ (Android 15)
- [ ] Enrolled in Play App Signing
- [ ] Upload key meets requirements (RSA 2048+, validity past Oct 2033)

**Store Listing:**
- [ ] App title (max 30 characters)
- [ ] Short description (max 80 characters)
- [ ] Full description (max 4,000 characters)
- [ ] App icon: 512x512 PNG
- [ ] Feature graphic: 1024x500 (if using promo video)
- [ ] Screenshots: 2-8 per device type, at 1080px+ resolution
- [ ] Contact email provided

**Compliance:**
- [ ] Privacy policy URL (public, accessible, non-PDF)
- [ ] Data Safety section completed
- [ ] IARC content rating questionnaire completed
- [ ] Permissions Declaration Form (if using sensitive permissions)
- [ ] All content complies with Developer Program Policy

---

## Sources

- [Google Play Console Help - Developer Account Requirements](https://support.google.com/googleplay/android-developer/answer/13628312?hl=en)
- [Google Play Console Help - Account Types](https://support.google.com/googleplay/android-developer/answer/13634885?hl=en)
- [Android Developers - Target SDK Requirements](https://developer.android.com/google/play/requirements/target-sdk)
- [Google Play Console Help - Target API Policy](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en)
- [Google Play Console Help - Data Safety Section](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en)
- [Google Play Developer Program Policy](https://support.google.com/googleplay/android-developer/answer/16810878?hl=en)
- [Google Play Console Help - Preview Assets](https://support.google.com/googleplay/android-developer/answer/9866151?hl=en)
- [Google Play Console Help - Content Ratings](https://support.google.com/googleplay/android-developer/answer/9898843?hl=en)
- [Google Play Console Help - Play App Signing](https://support.google.com/googleplay/android-developer/answer/9842756?hl=en)
- [Android Developers - Sign Your App](https://developer.android.com/studio/publish/app-signing)
- [Google Play Console Help - Permissions](https://support.google.com/googleplay/android-developer/answer/16324062?hl=en)
- [Google Play Console Help - Testing Requirements](https://support.google.com/googleplay/android-developer/answer/14151465?hl=en)
- [Google Play Console Help - Managing Appeals](https://support.google.com/googleplay/android-developer/answer/9899142?hl=en)
- [App Radar - Screenshot Guidelines](https://appradar.com/blog/android-app-screenshot-sizes-and-guidelines-for-google-play)
- [SplitMetrics - App Store Fees](https://splitmetrics.com/blog/google-play-apple-app-store-fees/)

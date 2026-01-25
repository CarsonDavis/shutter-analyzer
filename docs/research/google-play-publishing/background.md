# Google Play Store Publishing Research - Background Notes

**Research Date:** January 24, 2026

---

## Sources

1. [Google Play Console - Required Account Information](https://support.google.com/googleplay/android-developer/answer/13628312?hl=en)
2. [Google Play Console - Get Started](https://support.google.com/googleplay/android-developer/answer/6112435?hl=en)
3. [Google Play Console - Choose Account Type](https://support.google.com/googleplay/android-developer/answer/13634885?hl=en)
4. [Google Play Target API Requirements](https://developer.android.com/google/play/requirements/target-sdk)
5. [Google Play Console - Target API Policy](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en)
6. [Google Play Console - Data Safety Section](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en)
7. [Google Play Developer Program Policy](https://support.google.com/googleplay/android-developer/answer/16810878?hl=en)
8. [Google Play Console - Add Preview Assets](https://support.google.com/googleplay/android-developer/answer/9866151?hl=en)
9. [Google Play Console - Content Ratings](https://support.google.com/googleplay/android-developer/answer/9898843?hl=en)
10. [Google Play Console - Use Play App Signing](https://support.google.com/googleplay/android-developer/answer/9842756?hl=en)
11. [Android Developers - Sign Your App](https://developer.android.com/studio/publish/app-signing)
12. [Google Play Console - Permissions Declaration](https://support.google.com/googleplay/android-developer/answer/16324062?hl=en)
13. [Google Play Console - Testing Requirements](https://support.google.com/googleplay/android-developer/answer/14151465?hl=en)
14. [Google Play Console - Managing Appeals](https://support.google.com/googleplay/android-developer/answer/9899142?hl=en)
15. [SplitMetrics - App Store Fees 2025](https://splitmetrics.com/blog/google-play-apple-app-store-fees/)
16. [App Radar - Screenshot Guidelines](https://appradar.com/blog/android-app-screenshot-sizes-and-guidelines-for-google-play)
17. [MobileAction - Screenshot Sizes 2026](https://www.mobileaction.co/guide/app-screenshot-sizes-and-guidelines-for-the-google-play-store/)

---

## Key Findings

### Account Requirements

**Account Types:**
- **Personal Account**: For individual developers
- **Organization Account**: For businesses, requires D-U-N-S number

**Registration:**
- **One-time fee**: $25 USD
- **Accepted payments**: Credit/debit cards (Discover US only, Visa Electron outside US)
- **Prepaid cards**: Not accepted
- **Minimum age**: 18 years old

**Personal Account Requirements:**
- Valid Google account (dedicated account recommended)
- Government ID and credit card for identity verification
- Developer email address verification (OTP)
- Legal name, address, email, phone number
- Device verification via Play Console mobile app (accounts after Nov 2023)

**Organization Account Requirements:**
- D-U-N-S number (mandatory) - can take up to 30 days to obtain
- Organization name and address verification via Google Payments profile
- Developer email and phone number verification (OTP)
- One D-U-N-S can be used for multiple accounts

**Testing Requirements (Personal Accounts after Nov 2023):**
- Must run closed test with at least **12 testers for 14 days continuously**
- Originally was 20 testers, reduced to 12 in December 2024
- Business accounts are exempt from this requirement

---

### Technical Requirements

**App Format:**
- **AAB (Android App Bundle)**: Required for all new apps since August 2021
- APKs no longer accepted for new apps
- AAB allows Google Play to generate optimized APKs per device
- Average 15% reduction in download size vs universal APKs

**File Size Limits:**
- AABs: Compressed download under 4 GB
- Generated APKs from AAB: Cannot exceed 150 MB
- Use Play Asset Delivery for larger apps

**Target API Level Requirements (as of August 31, 2025):**
- **New apps/updates**: Must target Android 15 (API level 35) or higher
- **Wear OS, Android TV, Android Automotive**: Must target Android 14 (API level 34)
- **Existing apps**: Must target API 34+ to remain visible to new users on newer Android versions
- Extension available to November 1, 2025 if needed
- Non-compliant apps only visible to users on matching or lower Android versions

**App Signing:**
- **Play App Signing**: Mandatory for all apps created after August 2021
- Two keys involved:
  - **Upload key**: Developer keeps this, uses to sign uploads
  - **App signing key**: Google manages and protects this
- Upload key requirements:
  - RSA key, 2048 bits or more
  - Validity period ending after October 22, 2033
- Upload key can be reset if lost/compromised (unlike traditional signing)

---

### Visual Assets Requirements

**App Icon:**
- 512 x 512 pixels
- 32-bit PNG with alpha
- Max file size: 1024 KB (1 MB)
- Must be full square (masking handled by Play)
- No ranking claims, misleading badges, or promotional text

**Feature Graphic:**
- 1024 x 500 pixels
- JPEG or 24-bit PNG (no alpha)
- Max file size: 1 MB
- Only displayed if you have a promo video
- Keep key elements toward center (avoid cutoff zones)

**Screenshots:**
- 2-8 screenshots required per device type
- JPEG or 24-bit PNG (no alpha)
- Minimum length: 320 pixels
- Maximum length: 3840 pixels
- Recommended: At least 4 screenshots at 1080px minimum
- Portrait: 1080 x 1920px
- Landscape: 1920 x 1080px
- First three screenshots are most critical (shown first)

**Supported Device Types:**
- Phones, Tablets (7" and 10"), Chromebooks
- Android TV, Wear OS, Android Automotive OS, Android XR headsets

**Prohibited in Graphics:**
- Store performance/ranking indicators ("Best of Play," "#1")
- Price/promotional information ("10% off")
- Third-party trademarked content without permission
- Device imagery (becomes obsolete)
- Google Play or other store badges

---

### Policy & Compliance

**Content Policies (Effective January 1, 2026):**
- Restricted content categories: Sexual content, violence, hate speech, gambling, etc.
- Intellectual property protection required
- Malware and unwanted software prohibited
- Impersonation prohibited
- User experience standards enforced

**Child Safety Requirements:**
- Published standards prohibiting CSAE
- In-app mechanism for user feedback
- Action required after knowledge of CSAM
- Compliance with child safety laws including NCMEC reporting

**Privacy Requirements:**
- Privacy policy link required in Play Console AND within the app
- Must be on active, publicly accessible, non-geofenced URL (no PDFs)
- Must be non-editable
- Must comprehensively disclose data access, collection, use, and sharing
- Must include developer information and privacy contact

**Data Safety Section:**
- Mandatory for all apps, even those collecting no data
- Must detail collection, use, and sharing of user data
- Developer responsible for accuracy and keeping information current
- Must include third-party SDK data practices
- Account deletion must also delete associated user data

**Permissions Requirements:**
- Restricted permissions (Dangerous, Special, Signature) have additional requirements
- Permissions Declaration Form required for high-risk permissions (SMS, Call Log, etc.)
- Extended review period (up to several weeks) for sensitive permissions
- Video demonstration may be required
- Background location requires strong justification and explicit consent
- MANAGE_EXTERNAL_STORAGE restricted on Android 11+

---

### Content Ratings (IARC)

**System:**
- International Age Rating Coalition (IARC) system
- Adopted by Google Play in March 2015
- Single questionnaire generates ratings for multiple regions
- Free of cost
- Ratings issued immediately upon questionnaire completion

**Regional Authorities:**
- ESRB: North & South America
- PEGI: Europe and Middle East
- ClassInd: Brazil
- GRAC: South Korea
- IARC: Other regions

**Rating Factors:**
- Sexual content, violence, drugs, gambling, profane language
- Different regions may assign different ratings

**Ads Requirement:**
- Ads must not be significantly more mature than app content

**Appeals:**
- Rating authorities can override questionnaire-generated ratings
- Developers can appeal directly via link in certificate email

---

### Review Process

**Timeline:**
- New apps: 1-7 days typically
- Updates: 24-48 hours typically
- New accounts or sensitive features: May take longer
- Peak times: May cause delays
- Permissions Declaration: Up to several weeks

**Review Methods:**
- Combination of automated and manual checks
- Policy compliance verification

**Common Rejection Reasons:**
- Content policy violations
- Copyright/trademark infringement
- Privacy policy issues (missing, inaccessible, incomplete)
- Technical issues (crashes, bugs)
- Misleading metadata
- Inappropriate permission usage
- Broken links
- OAuth verification issues
- Low-quality screenshots
- Missing app description

**Enforcement Actions:**
- **Rejection**: Doesn't impact account standing; prior version remains available
- **Removal**: Doesn't immediately impact standing; multiple may lead to suspension
- **Suspension**: Forfeits users, statistics, ratings; results from egregious/multiple violations

**Appeal Process:**
- Fix issue and resubmit (no appeal needed for simple fixes)
- Contact support via email or Developer Policy Center
- Appeal response within 72 hours
- Reinstatement possible if error was made
- For IP disputes: Can submit explanation letter
- Warning: Abusing appeals process can result in loss of email support eligibility

---

### Costs & Fees

**One-Time Costs:**
- Developer account registration: $25 USD

**Commission Structure:**
- **First $1M annually**: 15% commission (must enroll in reduced-fee program)
- **Above $1M**: 30% commission
- **Subscriptions**: 30% standard, drops to 15% after 12 months of paid user

**What's Charged:**
- Paid app downloads
- In-app purchases of digital goods/services via Play billing

**What's NOT Charged:**
- Physical goods and services
- Most ad revenue

**EU-Specific (DMA Compliance):**
- Multi-tier commission mechanism
- Optional Tier 2: +10% for IAP, +3% for subscriptions (up to 13% total)
- Alternative distribution and payment channels required

---

### Ongoing Requirements

**Target API Updates:**
- Must target API level within 1 year of latest Android release (new apps/updates)
- Existing apps: Within 2 years to remain visible on newer devices

**Policy Compliance:**
- Regular policy updates (major update effective January 1, 2026)
- Must stay compliant with evolving policies
- US-specific: Alternative billing compliance deadline January 28, 2026

**Account Maintenance:**
- Keep developer account information up to date
- Organization changes must first update D-U-N-S profile with Dun & Bradstreet
- Re-verification may be required after profile changes

**2026 Developer Verification:**
- Starting 2026: All apps require verified developers for installation on certified Android devices
- Applies even to apps distributed outside Play Store

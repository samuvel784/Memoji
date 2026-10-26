# Google Play checklist for Memoji

## 1. Signing (important!)
Release builds **never** fall back to `debug.keystore`. `packageRelease` / `bundleRelease` depend on
the `verifyReleaseSigning` task, which fails with an explicit message when the upload key or its
passwords are missing. See `RELEASE.md` for the full procedure.

Quick version:
```
keytool -genkeypair -v -keystore my-upload-key.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
```
Set `STORE_PASSWORD` and `KEY_PASSWORD` (optionally `KEYSTORE_PATH`, `KEY_ALIAS`) and run
`./gradlew :app:bundleRelease` -> `app/build/outputs/bundle/release/app-release.aab`.
Keep the .jks and passwords backed up and **never commit them**. Enroll in Play App Signing when asked.

## 2. App facts
* Package: `com.vurka.memoji` (cannot change after first publish).
* Version: `versionCode 3`, `versionName 2.1`.
* targetSdk 37, minSdk 24, pure Kotlin/Compose -> meets current target-API and 64-bit rules.
* Release has R8 + resource shrinking enabled.
* All 1,000 levels are playable from first launch; progress is tracked separately and never gates
  level access. Do not describe levels as "unlockable" in the listing.

## 3. Play Console answers
* **Data safety:** "No data collected", "No data shared". No SDKs, no internet permission.
* **Privacy policy URL:** host `PRIVACY_POLICY.md` (GitHub Pages / any site) and paste the link.
  **Blocker:** the policy still contains `<your-email@example.com>`; replace it before publishing.
* **Ads:** No. **In-app purchases:** No. (Coins are earned in game and cannot be bought.)
* **Contains user-generated content:** No. **Account creation:** none.
* **App access:** all functionality available without login.
* **Content rating (IARC):** no violence/gambling/user interaction -> expect "Everyone / PEGI 3".
* **Target audience:** if you include children under 13, you must also comply with Families Policy
  (no ads/SDKs: you already comply).
* **Category:** Games > Puzzle (or Casual).
* **Data deletion:** not applicable - there is no account and no server. Players can clear all local
  data with Settings > Reset progress or by clearing app storage.

## 4. Store listing assets you still need to supply
* 512x512 icon (use `res/drawable-nodpi/app_logo_*.jpg` as a base), 1024x500 feature graphic.
* At least 2 phone screenshots (take them from the World Map and a game level).
* Short description (<=80 chars): "1000 levels of emoji memory fun – from relaxing to brain-melting."

## 5. Before upload
* Run `./gradlew :app:testDebugUnitTest :app:lintRelease`.
* Build and test the release build on a real device: first launch, level 1, level 1000, boss level 25,
  win, loss by time, loss by moves, replaying a cleared level (no second coin payout),
  background/resume, rotate, TalkBack on the board, and "Remove animations" enabled.
* Confirm the AAB is not debug-signed (`jarsigner -verify -verbose -certs ...`).
* Use the Play Console **Pre-launch report** to check accessibility and crashes.
* New personal developer accounts must complete closed testing (12 testers / 14 days) before production.

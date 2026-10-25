# Google Play checklist for Memoji

## 1. Signing (important!)
`app/build.gradle.kts` signs release builds with `my-upload-key.jks` if it exists (or `KEYSTORE_PATH`),
otherwise it falls back to `debug.keystore`. **Play rejects debug-signed bundles.** Create an upload key:

```
keytool -genkeypair -v -keystore my-upload-key.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
```
Then set environment variables `STORE_PASSWORD` and `KEY_PASSWORD` (and optionally `KEYSTORE_PATH`) and build:
`./gradlew :app:bundleRelease` -> `app/build/outputs/bundle/release/app-release.aab`.
Keep the .jks and passwords backed up and **never commit them**. Enroll in Play App Signing when asked.

## 2. App facts
* Package: `com.vurka.memoji` (cannot change after first publish).
* targetSdk 37, minSdk 24, 64-bit only code (pure Kotlin/Compose) -> meets current target-API and 64-bit rules.
* Release has R8 + resource shrinking enabled.

## 3. Play Console answers
* **Data safety:** "No data collected", "No data shared". (No SDKs, no internet permission.)
* **Privacy policy URL:** host `PRIVACY_POLICY.md` (GitHub Pages / any site) and paste the link.
* **Ads:** No. **In-app purchases:** No. **Contains user-generated content:** No.
* **App access:** all functionality available without login.
* **Content rating (IARC):** no violence/gambling/user interaction -> expect "Everyone / PEGI 3".
* **Target audience:** if you include children under 13, you must also comply with Families Policy (no ads/SDKs: you already comply).
* **Category:** Games > Puzzle (or Casual).

## 4. Store listing assets you still need to supply
* 512x512 icon (use `res/drawable-nodpi/app_logo_*.jpg` as a base), 1024x500 feature graphic.
* At least 2 phone screenshots (take them from the World Map and a game level).
* Short description (<=80 chars): "1000 levels of emoji memory fun – from relaxing to brain-melting."

## 5. Before upload
* Run `./gradlew :app:testDebugUnitTest :app:lintRelease`.
* Test the release build on a real device (flip, win, lose, shuffle level ~L120, boss L25, background/resume, rotate).
* Use the Play Console **Pre-launch report** to check accessibility and crashes.
* New personal developer accounts must complete closed testing (12 testers / 14 days) before production.

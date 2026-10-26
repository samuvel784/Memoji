# Google Play checklist for Emogy

## 0. Remaining launch blockers at a glance

Nothing in the code blocks a release: the app builds, tests pass and lint has no errors. What is
still missing is account, signing, asset and device work that only a human can supply.

| # | Item | Status / what to do |
|---|---|---|
| 1 | **Privacy contact email** | `PRIVACY_POLICY.md` has `<REPLACE_WITH_MONITORED_SUPPORT_EMAIL>`. Replace it with a monitored address before release. |
| 2 | **Public privacy policy URL** | Host `PRIVACY_POLICY.md` (GitHub Pages, a website, or Google Sites) and paste the HTTPS link into Play Console. The URL must be publicly reachable without login. |
| 3 | **Upload key** | Create `my-upload-key.jks`, set `STORE_PASSWORD` and `KEY_PASSWORD`, then build a signed `app-release.aab`. See `RELEASE.md`. |
| 4 | **Store graphics** | 512×512 icon, 1024×500 feature graphic, and at least 2 (ideally 4–8) phone screenshots. |
| 5 | **Real-device QA** | No device/emulator is connected here. Test audio, TalkBack, rotation, background/resume, a win/loss/replay and “Remove animations” on a real phone. |
| 6 | **Play Console setup** | Create the app, fill the listing, complete Data safety, Ads, Content rating and Target audience (General / 13+), and pick Games > Puzzle. |
| 7 | **Closed testing** | New personal developer accounts must run closed testing with 12 testers for 14 days before production. |
| 8 | **Name/trademark check** | Confirm “Emogy” is available as a store name and is not a conflicting trademark. This cannot be checked offline. |

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
* Package: `com.vurka.emogy` (cannot change after first publish).
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
* **Target audience:** General / 13+. The listing should not target children under 13 and the app
  should not be enrolled in the Families programme.
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

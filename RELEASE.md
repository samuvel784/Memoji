# Releasing Emogy

This is the short, accurate path from a clean checkout to a Play Store upload. It replaces the
older instructions that told you release builds fall back to the debug keystore: they do not.

## 1. Prerequisites

* JDK 17+ (Android Studio's bundled JBR works).
* Android SDK for `compileSdk 37`.
* An upload key. Keep it and its passwords backed up; losing them means you cannot update the app.

```powershell
keytool -genkeypair -v -keystore my-upload-key.jks -alias upload `
  -keyalg RSA -keysize 2048 -validity 10000
```

## 2. Environment

Set these in the shell that runs Gradle (never commit them):

| Variable | Required | Meaning |
|---|---|---|
| `KEYSTORE_PATH` | no | Defaults to `my-upload-key.jks` in the repo root. |
| `STORE_PASSWORD` | yes | Keystore password. |
| `KEY_PASSWORD` | yes | Key password. |
| `KEY_ALIAS` | no | Defaults to `upload`. |

```powershell
$env:KEYSTORE_PATH = "K:\keys\emogy-upload.jks"
$env:STORE_PASSWORD = "..."
$env:KEY_PASSWORD   = "..."
```

## 3. Build

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:testDebugUnitTest :app:lintRelease
.\gradlew.bat :app:bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`.

`bundleRelease` and `packageRelease` depend on `verifyReleaseSigning`. If the keystore or any
password is missing, the build fails with an explicit message instead of silently producing a
debug-signed artifact. Debug builds are unaffected and keep using `debug.keystore`.

## 4. Verify before uploading

```powershell
# The AAB must not be debug-signed.
jarsigner -verify -verbose -certs app\build\outputs\bundle\release\app-release.aab
```

* `testDebugUnitTest` must report 0 failures and `lintRelease` 0 issues.
* Install the release build on a phone and check: first launch, level 1, level 1000, boss level 25,
  a loss by time, a loss by moves, a win, replaying a cleared level (no second coin payout),
  background/resume, rotation, and TalkBack on the board.

## 5. Release blockers (must be resolved by a human)

Nothing in the code blocks a release. The remaining work is account, signing, asset and device work.

1. **Privacy contact and URL.** `PRIVACY_POLICY.md` contains `<REPLACE_WITH_MONITORED_SUPPORT_EMAIL>`.
   Replace it with a monitored address before release, host the page at a public HTTPS URL and paste that URL into
   Play Console. Play requires a working privacy policy URL and may use it to contact you.
2. **Upload key.** If `my-upload-key.jks` does not exist and the environment variables above are
   not set, there is no release artifact to upload. This is intentional.
3. **Store assets.** A 512×512 icon, a 1024×500 feature graphic and at least two phone screenshots
   (ideally 4–8) are still needed in the Play Console listing.
4. **Real-device QA.** Install the signed build on a phone and check audio, haptics, TalkBack,
   rotation, background/resume, a win, both loss types, a replay and “Remove animations”.
5. **Play Console setup.** Create the app entry and complete the listing, Data safety, Ads,
   Content rating and Target audience (General / 13+) sections; category Games > Puzzle.
6. **Testing track.** New personal developer accounts must run closed testing with 12 testers for
   14 days before production.
7. **Name check.** Confirm that “Emogy” is available as a store name and does not conflict with
   an existing trademark; this cannot be checked offline.

## 6. Versioning

Bump `versionCode` and `versionName` in `app/build.gradle.kts` for every upload. `versionCode` must
increase monotonically. The application ID `com.vurka.emogy` can never change after the first
published release.

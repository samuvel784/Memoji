# Releasing Memoji

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
$env:KEYSTORE_PATH = "K:\keys\memoji-upload.jks"
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

1. **Privacy contact.** `PRIVACY_POLICY.md` still contains `<your-email@example.com>`. Replace it
   with a monitored address and host the page at a public URL before publishing. Play requires a
   working privacy policy URL and may use it to contact you.
2. **Upload key.** If `my-upload-key.jks` does not exist and the environment variables above are
   not set, there is no release artifact to upload. This is intentional.
3. **Store assets.** A 512×512 icon, a 1024×500 feature graphic and at least two phone screenshots
   are still needed in the Play Console listing.
4. **Testing track.** New personal developer accounts must run closed testing before production.

## 6. Versioning

Bump `versionCode` and `versionName` in `app/build.gradle.kts` for every upload. `versionCode` must
increase monotonically. The application ID `com.vurka.memoji` can never change after the first
published release.

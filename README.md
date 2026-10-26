# Memoji – The Memory Game

Offline emoji memory-matching game: **1,000 levels, 20 themed worlds, boss levels, a winding world
map**. Every level is playable from the first launch, in any order; progress, stars and coins are
tracked separately.
Jetpack Compose • DataStore • no network, no ads, no tracking.

## Run
1. Open the folder in Android Studio and let Gradle sync.
2. Run the `app` configuration on a device/emulator (minSdk 24).
3. Unit tests: `./gradlew :app:testDebugUnitTest`
4. Lint: `./gradlew :app:lintDebug`

## Release build for Google Play
See `RELEASE.md` for signing, verification and the current release blockers, `PLAY_STORE_CHECKLIST.md`
for the Play Console answers, and `PRIVACY_POLICY.md` for the policy text. Release builds fail loudly
if no upload key is configured; they never fall back to the debug keystore.

## What changed
See `CHANGES.md` (v2.1 covers this release pass).

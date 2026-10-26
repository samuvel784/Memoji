# Memoji v2.0 – what changed

## Phase 1 – audio (towards v2.2)

### Separate, persistent volume controls
* Settings now has **Master**, **Music** and **Effects** sliders, stored in DataStore. The old
  `sound_enabled` boolean is migrated (on → 100%, off → muted) so existing players keep their choice.
* `soundEnabled` remains a derived value (`master > 0`), so every existing call site still works.
* Muting stops both engines; the music slider never bleeds into the effect channel.

### Ambient music per world family
* Four original 64-second loops (Organic, Festive, Airy, Cosmic) cover worlds 1–5, 6–10, 11–15 and
  16–20. The map cross-fades as you scroll into a new region; levels select their family directly.
* Music is synthesised deterministically on the device, cached as WAV, looped with `MediaPlayer`,
  faded in/out, and paused on `ON_STOP`. Audio focus is requested with `USAGE_GAME`; transient loss
  pauses, short interruptions duck, and permanent loss stops playback.
* No audio assets are bundled, no encoder is needed and no permission was added.

### Richer, musical effects
* Five new cues: **star**, **timer tick**, **whoosh**, **boss drum** and **chest/unlock chime**.
* The match chime climbs a major pentatonic ladder with the combo (combo 5 is one octave up, capped
  so SoundPool playback rates stay legal). The timer ticks for the final ten seconds and sharpens in
  the last three.
* Effects use shared master × effects gain staging; the engine refuses to play while the app is in
  the background or while music focus is lost.
* Cache version bumped so devices regenerate the expanded cue set.

### Settings and docs
* Volume card with live percentages and effect previews for every new cue.
* `LICENSES.md` documents every cue and loop: all original, procedurally synthesised, no third-party
  samples.
* New tests: gain staging, mute threshold, combo ladder and cap, world-to-music mapping, ambient
  audibility/length/determinism.

---

## v2.1 – release pass

### Every level is open, progress stays honest
* All 1,000 levels are selectable from the first launch, in any order. Level *availability* is no
  longer tied to progress at all.
* Removed `highestUnlockedLevel`. Real progress is derived from recorded stars:
  `completedLevels`, `totalLevelsCompleted`, `highestCompletedLevel`.
* Cosmetic themes still require real completions or a coin purchase (`ThemeUnlock`); having every
  level open does not unlock them. Locked cards show "🔒 12/50 levels".
* Reset now says exactly what it clears, keeps settings, and confirms when done. Levels stay open.
* The map has a validated **Jump to level** dialog (1–1000), a 20-world selector, and remembers your
  scroll position when you come back from a game.

### Rewards that make sense
* First clear pays **15 + 5 per star** coins; replays pay nothing, so level 1 cannot be farmed.
  Stars and personal bests only ever improve.
* Victory screen offers Next, Replay, World Map and Home, shows personal bests and first-clear coins,
  and turns into "Summit reached" at level 1000.
* Losses now count toward games played and the daily streak, and tell you whether time or moves ran out.

### Sound and accessibility
* Replaced the DTMF keypad tones with 12 synthesised cues (tap, flip, match, mismatch, hint, shuffle,
  combo, coin, achievement, victory, defeat, pause). They are rendered deterministically to WAVs in
  the cache and played with `SoundPool` — no third-party audio assets, no downloads, no permissions.
* Combo milestones get a rising accent instead of stacking on the match chime; the victory fanfare
  replaces the final match chime.
* Settings has a **Preview Sounds** row.
* Face-down cards announce "Hidden card" — the emoji is never leaked to TalkBack. Card glyphs are
  marked decorative, and matched pairs announce as matched.
* Decorative motion (drifting backdrop, confetti, screen shake, popping cards, pulsing nodes) is
  skipped when the system's "Remove animations" setting is on.
* Shared `AppTopBar` and design tokens (`Spacing`, `Radii`, `Chrome`) replace repeated literals.

### Docs and build
* `RELEASE.md` documents signing, verification and the remaining release blockers.
* `metadata.json` / `.env.example` no longer claim a Gemini capability the app does not have.
* Release builds fail loudly when the upload key is missing; they never fall back to `debug.keystore`.

---

# Memoji v2.0 – what changed

## Harder as you climb (`LevelCatalog`, `GameViewModel`, `MemoryGameEngine`)
| Mechanic | Early (1-20) | Mid (100-300) | Late (600-1000) |
|---|---|---|---|
| Pairs | 4-7 | 9-14 | 17-20 |
| Memorise time | 5s | 3s | 2-3s |
| Time per pair | none | 8-6s | 4.6-3.8s |
| Move budget | none | ~2.7-2.3x pairs | ~2.1-2.0x pairs |
| Wrong pair stays visible | 1000ms | 850-700ms | 560-460ms |
| Hints | unlimited | 5 -> 3 | 2 -> 1 |
| Mid-game reshuffle of unmatched cards | - | every ~45-34s (from L100) | every ~28-18s |
| Lone flipped card hides again | - | 4.5s (from L200) | 3.7-2.5s |

* **Boss level every 25 levels** (40 total): +2 pairs, 10% less time, fewer moves, faster shuffles, **no hints**, gold crown on the map.
* Zen mode (Settings) still removes all pressure (timer, move limit, shuffles, auto-hide, hint limits).
* New: combo counter + combo popup, "Shuffled!" popup, separate loss messages (time vs moves).

## Smoother
* Card flip angle is read only in the draw phase (`graphicsLayer`) -> **zero recompositions while a card flips**.
* Card grid is its own composable, so the 1-second timer tick no longer recomposes every card.
* Cards glide to their new slot on reshuffle (`animateItem`).
* Shake effect moved from layout (`offset`) to draw phase (`translationX`).
* Card back brush is `remember`ed per theme instead of rebuilt per card.
* Dark window background removes the white flash at launch.
* Game auto-pauses when the app goes to the background; Back opens the pause menu instead of losing the run.
* Fixed: restarting during the memorise phase could start two preview timers.
* Fixed: the timer could start while the game was paused during the preview.
* Responsive grid: 3/4/5/6 columns by deck size, emoji size scales with columns (big decks fit without scrolling).

## Map-style levels (`LevelSelectScreen`)
* Winding S-curve path that climbs upward (like Candy Crush): 20 world banners, 1000 nodes, summit at the top.
* Pulsing "current" node with a hopping wizard marker, stars on completed nodes, gold trail on the part you've travelled, dashed trail ahead. (v2.1 removed the lock state: every node is playable.)
* Boss nodes (square, crown). Scenery emoji per world. Background colour cross-fades as you scroll between worlds.
* World chip that updates while scrolling, "📍 Level N" jump button, auto-scroll to your level.
* Tap a node -> level preview (pairs, time, moves, shuffles, hints, best stars) -> Play.

## Visuals / animation
* New physics confetti (two cannons, drag, gravity, 3D flutter, fade-out).
* Card backs have a woven texture; matched cards glow emerald; HUD with move-limit counter, time bar that turns amber/red, rule chips, "Memorize!" banner.

## Play Store readiness
* Removed unused/risky dependencies: Firebase (AI, App Check), Retrofit, OkHttp, Moshi, Room, KSP, Google Services and Secrets plugins. No INTERNET permission -> Data safety = "no data collected".
* Backup rules fixed: they pointed at `sharedpref`, but DataStore lives in `files/datastore/`, so progress was never actually backed up. Now it is (Android 11- and 12+ rules).
* Predictive back enabled, content descriptions on cards and map nodes (TalkBack).
* `versionCode 2 / versionName 2.0`, R8 + resource shrinking already on for release, trimmed ProGuard file.
* Added `PRIVACY_POLICY.md` and `PLAY_STORE_CHECKLIST.md`.

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
* Locked nodes (previously every level could be opened), pulsing "current" node with a hopping wizard marker, stars on completed nodes, gold trail on the part you've travelled, dashed trail ahead.
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

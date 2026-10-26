# Audio licences and sources

Emogy ships **no third-party audio files**. Every sound effect and every music loop is original,
procedurally synthesised on the device from mathematical oscillators and deterministic noise. There
are no recordings, no sample packs, no OGG/MP3 assets in `res/raw`, and nothing is downloaded.

## Sound effects

Rendered once into the app cache as 16-bit PCM WAV by `app/src/main/java/com/vurka/emogy/util/SoundManager.kt`
and played through `SoundPool` with `USAGE_GAME`.

| Cue | Source file | Character |
|---|---|---|
| Tap | `SoundManager.kt` | Short soft click |
| Flip | `SoundManager.kt` | Paper tick plus a quiet triangle tone |
| Match | `SoundManager.kt` | Two-note bell; playback rate climbs a major pentatonic ladder with the combo |
| Mismatch | `SoundManager.kt` | Soft low two-note "bonk" |
| Hint | `SoundManager.kt` | High three-note twinkle |
| Shuffle | `SoundManager.kt` | Filtered noise riffle |
| Combo | `SoundManager.kt` | Two-note accent (kept for previews) |
| Coin | `SoundManager.kt` | Bright two-note bell |
| Achievement | `SoundManager.kt` | Four-note rising arpeggio |
| Victory | `SoundManager.kt` | Three rising bells plus a sustained tonic |
| Defeat | `SoundManager.kt` | Gentle descending two-note phrase |
| Pause | `SoundManager.kt` | Soft low sine |
| Star | `SoundManager.kt` | Single bell; one is played per earned star at a rising pitch |
| Tick | `SoundManager.kt` | Quiet timer tick for the last ten seconds |
| Whoosh | `SoundManager.kt` | Short filtered screen-change sweep |
| Boss | `SoundManager.kt` | Low drum hit for boss introductions |
| Chest | `SoundManager.kt` | Magic chime for unlocks and chests |

## Music

Rendered once into the app cache as 64-second, 22.05 kHz mono WAV loops by
`app/src/main/java/com/vurka/emogy/util/AmbientMusic.kt` and looped with `MediaPlayer`.

| Family | Worlds | Mood |
|---|---|---|
| Organic | 1–5 | Warm triangle pads, D major pentatonic, sparse bells |
| Festive | 6–10 | Brighter bell pads, G major pentatonic, livelier bells |
| Airy | 11–15 | High sine pads, A major pentatonic, long silences |
| Cosmic | 16–20 | Deep sine drone, C minor pentatonic, slow triangle bells |

## Attribution

No attribution is required because no third-party audio is used. If the project owner later adds
licensed recordings or sample packs, they must be listed here with the author, source URL and licence
before they are committed.

The synthesis code itself is part of this repository and is covered by the repository's own licence.

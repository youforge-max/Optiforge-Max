# Optiforge-Max

> Android system-wide 5-band audio compressor/limiter (EQ + dynamics) — eu.cisodiagonal.optiforgemax.

5-band compressor / limiter for Android tablets. Two modes, switchable from the
top tab bar:

- **Live** — system-wide realtime processing of all output audio.
- **Audio File** — offline open → preview → save for an audio (or video) file.

No root, no NDK. Requires Android 10+ (minSdk 29). Signed release APKs are on
[GitHub Releases](https://github.com/youforge-max/Optiforge-Max/releases).

📖 **End-user guide: [MANUAL.md](MANUAL.md)** — step-by-step for both modes,
control reference, calibration, troubleshooting. This README is the technical
overview. Version history: [CHANGELOG.md](CHANGELOG.md).

---

## Live mode

Built on `android.media.audiofx.DynamicsProcessing` (API 28+). No audio capture.
The effect attaches to the **global output mix session (session 0)**, so it
processes all system audio.

### Controls (per spec)
- **Input gain** — pre-everything, −40…+40 dB
- **Per band (×5):** threshold (−100…0 dBFS), attack (0…500 ms),
  release (0…3000 ms), ratio (1…20:1), makeup (−12…+24 dB), enable
- **Output limiter:** threshold/attack/release/ratio/post-gain
- Crossover cutoffs default 150 / 600 / 2500 / 7000 / 20000 Hz (editable in code)
- **Per-band gain-reduction meters** (needs `RECORD_AUDIO`)
- **Spectrum view** — 32 log-spaced bars from the same FFT (bar-centric folding,
  so low bands aren't dropped when a bar is narrower than one FFT bin)
- **Link attack/release** — when on, moving any band's Attack or Release writes
  all 5 bands at once; Attack/Release stay independent, limiter untouched
- **Presets** — named save/load/delete (SharedPreferences, JSON)
- **Meter calibration** slider — tune the estimated level offset on-device

### Gain-reduction meters — how / caveat
`DynamicsProcessing` exposes **no** GR readout. Meters are *estimated*: a
`Visualizer` on the same session FFTs the output mix, bins are folded into the
5 bands to estimate each band's level (dBFS), then run through the soft-knee
compression curve (`DspEngine.estimateGrDb`) with attack/release smoothing.
Tracks compression activity for visual feedback — not a calibrated measurement
(post-mix magnitude, not the compressor's internal detector; makeup/limiter not
reflected). Denied mic permission disables meters; everything else still works.

**Calibration:** the byte-magnitude→dBFS mapping (ref 128) is approximate and
varies per device. With audio playing below threshold, nudge the **Meter
calibration** slider until GR sits near 0, then set thresholds to taste. The
offset is saved in presets.

All ranges are UI-only — widen them in `MainActivity.kt` (`SliderRow` calls).
The underlying floats accept far wider values.

---

## Audio File mode

Process a file offline and save the result — same 5-band sound as Live mode,
rendered in pure Kotlin on PCM (the live `DynamicsProcessing` effect has no
file-render API).

1. **Open** — pick an **audio or video** file. For a video, the audio track is
   extracted and processed; the video is discarded.
2. **Preview** — loops a 60 s window (start point selectable) through the exact
   render chain. Every slider edit is audible live.
3. **Save** — renders the whole file to an AAC **`.m4a`** via the system codec.
   Runs in the background with progress + cancel.

Signal chain (`audiofile/` package):

```
input gain -> LR4 5-band crossover -> per-band soft-knee compressor + makeup
           -> sum -> limiter (+ brickwall ceiling) -> [optional loudness gain]
```

- **Loudness normalize** — optional pass measures integrated loudness
  (simplified ITU-R BS.1770 K-weighting) and applies a single gain to hit a
  LUFS target (default −14), clamped to ±24 dB.
- Has its own preset store (`presets_audiofile`) plus per-band **knee** and the
  same **Link attack/release** toggle.
- Whole encoded stream is buffered in memory before muxing — fine for typical
  clips, heavy for multi-hour files.

## Build
SDK on the dev box at `~/android-sdk`. The Gradle wrapper jar is not committed;
either open in **Android Studio** (Koala+) to generate it, or build with a local
Gradle 8.9:

```bash
# debug
gradle :app:assembleDebug

# signed release (keystore props or env: RELEASE_STORE_FILE / _PASSWORD /
# RELEASE_KEY_ALIAS / RELEASE_KEY_PASSWORD; falls back to unsigned if absent)
gradle :app:assembleRelease \
  -PRELEASE_STORE_FILE=/path/to/optiforgemax.jks \
  -PRELEASE_STORE_PASSWORD=*** -PRELEASE_KEY_ALIAS=optiforgemax \
  -PRELEASE_KEY_PASSWORD=***
```

## Caveat — global session & per-player binding
Stock Android honors output-mix effects, but many players bypass the global
session-0 effect: VLC (its own audio session + non-mixer output), and some OEM
paths (Samsung/Xiaomi, offloaded/MQA/Dolby) for compressed/offloaded playback.

Since **1.5** the app registers a receiver for the standard
`OPEN/CLOSE_AUDIO_EFFECT_CONTROL_SESSION` broadcasts and **automatically binds**
the compressor to a player's announced session id (`DspEngine.attach(id)` +
re-applies state + rebinds the meter). The **Routing** card shows the current
binding; "Use global mix" reverts to session 0.

For a player to be bound it must announce its session:
- **VLC**: enable **Audio → Audio effects** (and/or set output to AudioTrack).
- Offloaded audio: disable "MQA/Dolby/audio offload" in the player or Developer
  Options so the chain stays in the software mixer.
- If nothing announces a session, the effect stays on the global mix; use
  **Retry attach** if attach failed.

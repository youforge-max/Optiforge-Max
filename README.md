# Optiforge-Max

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

## Caveat — global session
Stock Android honors output-mix effects, but some OEM ROMs (Samsung/Xiaomi
audio paths, offloaded/MQA/Dolby outputs) ignore session-0 effects or bypass
them for compressed/offloaded playback. If `attach()` returns false or you hear
no change:
- Use the **Retry attach** button.
- Fallback: bind to a specific player's audio session id (`DspEngine.attach(id)`)
  — get the id from the player (e.g. `MediaPlayer.getAudioSessionId()`), or via
  the standard `ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION` broadcast that media
  apps send.
- Offloaded audio: disable "MQA/Dolby/audio offload" in the player, or in
  Developer Options, so the effect chain stays in the software mixer.

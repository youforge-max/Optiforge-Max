# Optiforge-Max

System-wide 5-band compressor / limiter for Android tablets.

Built on `android.media.audiofx.DynamicsProcessing` (API 28+). No root, no NDK,
no audio capture. The effect attaches to the **global output mix session
(session 0)**, so it processes all system audio.

## Controls (per spec)
- **Input gain** — pre-everything, −40…+40 dB
- **Per band (×5):** threshold (−100…0 dBFS), attack (0…500 ms),
  release (0…3000 ms), ratio (1…20:1), makeup (−12…+24 dB), enable
- **Output limiter:** threshold/attack/release/ratio/post-gain
- Crossover cutoffs default 150 / 600 / 2500 / 7000 / 20000 Hz (editable in code)
- **Per-band gain-reduction meters** (needs `RECORD_AUDIO`)
- **Spectrum view** — 32 log-spaced bars from the same FFT
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

## Build
No Android SDK on the dev box. Open in **Android Studio** (Koala+), which
generates the Gradle wrapper jar, then Run on the tablet. CLI alternative:

```bash
# once, to create the wrapper jar:
gradle wrapper --gradle-version 8.9
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
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
```

# Optiforge-Max — User Manual

A 5-band multiband compressor + limiter for Android tablets (Android 10+).
Two modes share the same sound:

- **Live** — processes all system audio in realtime.
- **Audio File** — processes a single audio or video file and saves the result.

Switch between them with the **Live / Audio File** tabs at the top of the screen.

---

## 1. Quick start

### Live (process everything you hear)
1. Open the app. It lands on the **Live** tab and tries to attach to the global
   audio output.
2. Grant the **microphone** permission when asked — it's used only to *measure*
   the signal for the meters and spectrum, not to record.
3. Make sure **Processing** is **ON** (top switch).
4. Play audio from any app. Adjust the bands; you hear changes immediately.

### Audio File (process a file)
1. Tap the **Audio File** tab.
2. **Pick file…** → choose an audio *or* video file.
3. **Preview** → listen to a looped 60 s window while you tune.
4. **Process & Save…** → choose where to write the new `.m4a`.

---

## 2. Core concepts (read this once)

**Multiband compression** splits the sound into 5 frequency bands and compresses
each one separately, so loud bass doesn't pump the vocals (and vice-versa).

| Term | What it does |
|------|--------------|
| **Threshold** | Level where compression starts. Lower = more of the signal gets compressed. |
| **Ratio** | How hard it compresses above the threshold. 4:1 = 4 dB in → 1 dB out. |
| **Knee** | How gradually compression eases in around the threshold (soft = smoother). |
| **Attack** | How fast it clamps down after the signal crosses the threshold. |
| **Release** | How fast it lets go once the signal drops back down. |
| **Makeup** | Gain added back after compression to restore loudness. |
| **Limiter** | A fast safety stage that stops the output exceeding a ceiling. |

The 5 bands are split at **150 / 600 / 2500 / 7000 / 20000 Hz** by default:

| Band | Range (default) | Typical content |
|------|-----------------|-----------------|
| 1 | 20 – 150 Hz | sub-bass / kick |
| 2 | 150 – 600 Hz | bass / low mids |
| 3 | 600 Hz – 2.5 kHz | vocals / body |
| 4 | 2.5 – 7 kHz | presence / consonants |
| 5 | 7 – 20 kHz | air / cymbals |

---

## 3. Live mode reference

The Live engine uses Android's system effect on the **global output mix**, so it
affects every app's audio with no root and no capture.

### Top controls
- **Processing ON/OFF** — master bypass for the whole effect.
- **Presets** — save / load / delete named setting profiles.
- **Spectrum** — 32-bar realtime analyzer of the output.
- **Meter calibration** — see *Meters* below.
- **Input Gain** — level trim before the bands (−40…+40 dB).

### Per band (×5)
- **Enable** switch, **Threshold**, **Attack**, **Release**, **Ratio**, **Makeup**.
- **GR** bar = estimated gain reduction for that band.

### Output limiter
Threshold (ceiling), Attack, Release, Ratio, Post Gain. Leave it on as a safety
net; set the ceiling around −1 dB.

### Link attack/release
Turn this on to move **all five bands'** Attack (or Release) together with one
slider. Attack and Release stay independent of each other; the limiter is never
touched. Turn it off to fine-tune a single band again.

### Meters — important caveat
The system effect reports **no** gain-reduction value, so the **GR bars and
spectrum are estimates** derived from a microphone-side FFT of the output. They
track activity for visual feedback — they are not a calibrated measurement.

**Calibrate once per device:** play audio quieter than your thresholds and nudge
**Meter calibration** until the GR bars sit near 0. The offset is saved with the
preset.

### Routing — and why some players (VLC) need a step
Live processing sits on the **global output mix**. Many players bypass that mix
and won't be affected by default — **VLC** is the common case (it uses its own
audio session and output path).

The app handles this automatically: when a player announces its audio session
(the standard "audio effects" integration), Optiforge **binds to that player's
session**. The **Routing** card shows where the effect is currently applied:
- *Global output mix (session 0)* — affects players that use the normal mixer.
- *Bound to <app> · session N* — affects that specific player (e.g. VLC).

Tap **Use global mix** to go back to session 0.

**To make VLC work:** in VLC, enable **Audio → Audio effects** (some versions:
turn on the Equalizer), or set **Audio output** to **AudioTrack**. Start
playback — Optiforge binds to it automatically and the Routing card updates.

### If you still hear no effect
- Press **Retry attach** (shown when attach fails).
- Disable "audio offload / MQA / Dolby" in the player or in Developer Options so
  audio stays in the software mixer.

---

## 4. Audio File mode reference

Process one file offline and save a new copy. The sound matches Live mode but is
rendered in software (so it works on a file, with no system-effect limitations).

### Step by step
1. **Pick file…** — accepts **audio or video**. For video, only the audio track
   is processed; the picture is discarded and the output is audio-only.
2. **Preset** — optionally load a saved profile (separate from Live presets).
3. **Preview**
   - Drag **Start** to choose where the 60 s preview window begins.
   - Tap **Preview** to play it looped. Tune any control while it plays — the
     change is audible on the next loop pass.
   - Tap **Stop preview** when done.
4. **Loudness normalize** (optional)
   - **On**: the app measures the file's integrated loudness and applies one
     overall gain to reach the **Target** LUFS (default −14, good for most
     streaming/voice). Gain is clamped to ±24 dB.
   - **Off**: only your manual Input Gain / band makeup apply.
5. **Bands / Limiter** — same controls as Live, plus a per-band **Knee**.
6. **Process & Save…** — pick a destination and filename. A progress bar shows
   *Analysing loudness → Processing audio → Writing output*. **Cancel** stops it.
   Output is an AAC `.m4a`, named `<original>_processed.m4a` by default.

### Notes
- The whole output is assembled in memory before writing — great for clips,
  heavy for multi-hour files.
- What you hear in Preview is what gets written (apart from the loop seam).

---

## 5. Presets

Both modes have **Save / Load / Delete** for named profiles, stored on-device.
Live and Audio File keep **separate** preset lists. Each app also auto-remembers
your last-used settings and reopens on them.

A good starting profile: gentle ratios (2–3:1), soft knee (6 dB), medium attack
(20–50 ms) / release (150–300 ms) per band, limiter on at −1 dB.

---

## 6. Permissions

| Permission | Why | If denied |
|------------|-----|-----------|
| **Microphone** (`RECORD_AUDIO`) | Live meters/spectrum FFT only | Live still processes; meters/spectrum go blank |
| **Modify audio settings** | Attach the Live effect to the output mix | Live effect can't attach |

Audio File mode needs no special permission — files are opened through the
system picker.

---

## 7. Troubleshooting

| Symptom | Fix |
|---------|-----|
| Live: no change in VLC (or similar) | Enable VLC **Audio → Audio effects** (or output = AudioTrack); the **Routing** card should bind to it. Else **Retry attach** |
| Live: no audible change generally | Disable audio offload/MQA/Dolby in the player; check **Processing** is ON |
| Live: GR bars never move / always pegged | Re-run **Meter calibration**; check mic permission |
| File: "No audio to preview" / "No audio track" | The chosen file has no audio stream |
| File: render is slow | Long files take time; loudness-normalize adds an analysis pass |
| Output too quiet/loud | Adjust band **Makeup**, **Input gain**, or the normalize **Target** |
| Distortion | Lower ratios/makeup; keep the limiter on with ceiling ≈ −1 dB |

---

*Built with the same DSP across both modes so a sound dialed in Live transfers to
file rendering. minSdk 29 (Android 10). Releases:
<https://github.com/youforge-max/Optiforge-Max/releases>.*

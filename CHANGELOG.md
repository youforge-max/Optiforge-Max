# Changelog

All notable changes to Optiforge-Max. Format loosely follows
[Keep a Changelog](https://keepachangelog.com/); versions are git tags
`vMAJOR.MINOR` with matching signed APKs on
[GitHub Releases](https://github.com/youforge-max/Optiforge-Max/releases).

## [1.5] — 2026-06-27
### Added
- **Per-player session binding** — the app now listens for the standard
  `OPEN/CLOSE_AUDIO_EFFECT_CONTROL_SESSION` broadcasts and binds the compressor
  to a player's own audio session. Fixes apps (notably **VLC**) whose audio
  bypasses the global session-0 effect. A **Routing** card shows the current
  binding with a "Use global mix" reset.
### Notes
- VLC must announce its session: enable **Audio → Audio effects** in VLC (or set
  its output to AudioTrack). Binding is then automatic.

## [1.4] — 2026-06-27
### Added
- **User manual** ([MANUAL.md](MANUAL.md)) — step-by-step guide for both modes,
  control reference, meter calibration, troubleshooting.
- This changelog.
### Changed
- README rewritten around the two modes (Live + Audio File) with build/release
  instructions.

## [1.3] — 2026-06-27
### Added
- **Audio File mode** — second tab beside the live compressor. Open an audio
  **or video** file (video audio is extracted, video discarded), preview the
  5-band chain on a looped 60 s window with live-audible edits, and save a
  processed AAC `.m4a`.
- Offline pure-Kotlin DSP (LR4 crossover → soft-knee band compressors →
  limiter) matching the live engine, with optional ITU-R BS.1770 loudness
  normalize to a LUFS target.
- Separate preset store for file mode; per-band knee control.

## [1.2] — 2026-06-27
### Fixed
- Spectrum analyzer dropped the lowest bands: log-spaced bars narrower than one
  (linear) FFT bin caught nothing. Folding is now bar-centric and samples the
  nearest bin when a bar is sub-bin width.

## [1.1] — 2026-06-27
### Added
- Adaptive launcher **app icon** (mixer-fader / compression-curve motif); the
  app previously shipped without one.
- **Link attack/release across bands** toggle — moving any band's Attack or
  Release writes all 5 bands; Attack/Release stay independent, limiter untouched.

## [1.0]
### Added
- Initial release: system-wide 5-band compressor/limiter built on
  `DynamicsProcessing` (global output-mix session). Per-band threshold/attack/
  release/ratio/makeup, output limiter, estimated gain-reduction meters,
  32-bar spectrum, meter calibration, named presets.

[1.5]: https://github.com/youforge-max/Optiforge-Max/releases/tag/v1.5
[1.4]: https://github.com/youforge-max/Optiforge-Max/releases/tag/v1.4
[1.3]: https://github.com/youforge-max/Optiforge-Max/releases/tag/v1.3
[1.2]: https://github.com/youforge-max/Optiforge-Max/releases/tag/v1.2
[1.1]: https://github.com/youforge-max/Optiforge-Max/releases/tag/v1.1
[1.0]: https://github.com/youforge-max/Optiforge-Max/releases/tag/v1.0

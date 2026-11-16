# Feature Migration Matrix — CapCraft → VantaCut Studio

Legend: **Status** = ✅ working · 🟡 partially working · ⏸ deferred · ⛔ blocked (licensing).

Licenses: **FC** = implemented from FilmCraft's own MIT/Apache code · **AGPL** = needs CapCraft code
(must not be copied before the LICENSE_REVIEW.md decision) · **IND** = independent reimplementation
inside FilmCraft's architecture (feature described by CapCraft, code written fresh).

## A. Text and typography

| Feature | CapCraft reference | FilmCraft counterpart | Dependencies | Complexity | Tests | Status |
| --- | --- | --- | --- | --- | --- | --- |
| Text on timeline, styles, presets, fonts, colors, layout | `concat-text` | `crates/text`, `TextTools.kt`, `text_presets()` | none (already present) | — | existing Rust text tests | ✅ working (baseline) |
| More display fonts (OFL pack) | `concat-text/fonts/*` (AGPL repo assets, fonts themselves are OFL) | font pipeline copies `assets/fonts` at build time | font files + their OFL licenses | low | render smoke test | ⛔ pending license decision (fonts are OFL — see note 1) |
| Text outlines, shadows, backgrounds | `concat-text` TitleStyle | CaptionStyle has outline+background; text clip style does not | engine `crates/text` extension | medium | Rust unit + render test | ⏸ deferred (Phase 3) |
| Word-art treatments, bubbles, shapes | `concat-text` (bubbles, shapes) | none | port of algorithms or reimplementation | high | Rust unit + UI | ⛔ pending license decision |
| Text entrance/exit/loop animations | `concat-text` animations | none (text is static per style) | engine animation model | high | Rust + preview test | ⏸ deferred (Phase 3) |
| Editable subtitle clips on timeline | CapCraft titles from captions | captions are already editable clips (`set_caption_text`, timeline lane) | none | — | existing | ✅ working (baseline) |

Note 1: the font *files* are OFL-licensed by their authors; copying the files from CapCraft's repo
is legally fine with attribution, but bundling them is still a repo-owner decision because of repo
policy; they can also be fetched from Google Fonts directly (same files, cleaner provenance).

## B. Captions

| Feature | CapCraft reference | FilmCraft counterpart | Dependencies | Complexity | Tests | Status |
| --- | --- | --- | --- | --- | --- | --- |
| Auto captions (Whisper, on-device) | `concat-speech` / sherpa-onnx | `crates/speech` + `speech.rs` + `SpeechPanels.kt` | Whisper models, downloaded on demand | — | existing | ✅ working (baseline, preserved) |
| Caption import (SRT) | `capcraft-android/src/subtitles.rs` (AGPL) | **`filmcraft-captions` crate already parses SRT/VTT/TTML/SCC/MCC/STL** — app-side import is new code | none | low | Rust unit (parse→captions), UI test | ✅ working (implemented independently, FC code) |
| Caption export (SRT sidecar) | `subtitles.rs` write path | `filmcraft-captions` writers exist; app-side export is new | none | low | Rust unit round-trip | ✅ working (implemented independently, FC code) |
| Cinematic caption presets | caption templates | `CaptionStyle` (font, size, color, background, outline, anchor, margin, line spacing) | none — presets are data | low | unit (style mapping) | ✅ working (implemented independently, FC code) |
| Word-by-word / karaoke highlighting | `subtitles.rs` karaoke | engine transcript model has word timings (`SeqWord`) but captions render as blocks | render changes | high | render test | 🟡 partially working (see CHANGELOG) |
| Auto Lyrics as a separate workflow | `speech.rs` lyrics parts | Whisper pipeline exists; lyrics need a distinct flow | UI + flow | medium | UI test | ⏸ deferred (Phase 3) |
| Arabic shaping, RTL, punctuation, mixed AR/EN subtitles | CapCraft ships NotoSansArabicUI | `crates/text` layout (harfbuzz-class shaping in `layout/`) — verify Arabic shaping + bidi | none (already in engine) | low | Rust shaping test, RTL layout test | ✅ working (verified by test) |
| Foreground-aware captions (masking) | CapCraft `cutout.rs` | none | segmentation model, native deps | very high | — | ⏸ deferred (Phase 3, needs model download design) |

## C. Effects and visual assets

| Feature | CapCraft reference | FilmCraft counterpart | Dependencies | Complexity | Tests | Status |
| --- | --- | --- | --- | --- | --- | --- |
| Filters (31) + intensity | `concat-effects` | `presets.rs` looks + Lumetri | none | — | existing | ✅ working (baseline) |
| Extra cinematic looks | `concat-effects` looks (AGPL) | new looks describable as Lumetri deltas | none if written fresh | low | unit (recipe↔recognise round trip) | ✅ working (new looks written fresh, FC code) |
| Effects (motion/light/distort/retro…) | `concat-effects` | engine effects + `presets.rs` | none | — | existing | ✅ working (baseline) |
| Transitions (50) with animated tiles | CapCraft has more (film burn, cube…) | `presets.rs` transitions | engine transitions | medium | existing per-transition tests | ✅ working (baseline) |
| Stickers / labels / shapes | `concat-text` figures, stickers | text clips can carry glyphs; no sticker shelf | new UI + assets | medium | UI test | ⏸ deferred (Phase 3) |
| Drawing / doodle workflow | `concat-host/brush.rs` | none | new | high | — | ⏸ deferred |

## D. Audio

| Feature | CapCraft reference | FilmCraft counterpart | Dependencies | Complexity | Tests | Status |
| --- | --- | --- | --- | --- | --- | --- |
| Volume, mute, fades, extract audio | CapCut-like | `set_volume`, `set_muted`, `set_fades`, `extract_audio` | none | — | existing | ✅ working (baseline) |
| Sound-effect library (categories) | `concat-host/sounds.rs` + **external assets repo** (runtime downloads) | none | needs the external repo + network; licensing of that repo unchecked | medium | — | ⏸ deferred (external dependency, license unverified) |
| Sound FX generated by the app | CapCraft "sounds, made by the app" | none | synthesis code | medium | unit | ⏸ deferred (Phase 3) |
| Voice-over recording | — | `Voiceover.kt` | mic permission | — | existing | ✅ working (baseline) |
| Voice isolation / noise reduction | `voice.rs` | none | model or DSP | high | — | ⏸ deferred (Phase 3) |

## E. Export

| Feature | CapCraft reference | FilmCraft counterpart | Dependencies | Complexity | Tests | Status |
| --- | --- | --- | --- | --- | --- | --- |
| 720p / 1080p / 4K MP4 (H.264+AAC) | concat-export | `export.rs` short-side model | none | — | existing | ✅ working (baseline, preserved) |
| Frame-rate and quality options | export screen | `ExportSettings` already has fps + quality; UI exposes them | none | — | existing | ✅ working (baseline) |
| Estimated file size + progress + cancel | export screen | `ExportPlan`, `ExportStatus` | none | — | existing | ✅ working (baseline) |
| GIF export | `gif.rs` (AGPL) | none | reimplementation using the preview render path | medium | Rust integration (GIF header/frames) | ⏸ deferred pending license decision (algorithm is straightforward to reimplement independently if Option B) |
| Still-image (frame) export | CapCraft photo export | none | render single frame → PNG/JPEG | low | Rust unit | ⏸ deferred (Phase 3, small) |

## F. Advanced (Phase 3 candidates)

| Feature | CapCraft reference | Notes | Status |
| --- | --- | --- | --- |
| Speed curves / keyframes | `slowmo.rs`, engine | FilmCraft has constant speed (0.1–10×); curves need engine work | ⏸ deferred |
| Freeze frame | — | FilmCraft already has `freeze` | ✅ working (baseline) |
| Reverse | `pauses.rs`/engine | engine item flag `reverse` exists | ✅ working (baseline, engine-level) |
| Chroma key / masks | `cutout.rs` | needs engine effect + possibly model | ⏸ deferred |
| Motion tracking | `tracking.rs` (514 lines, AGPL) | heavy; needs own implementation + model | ⏸ deferred |
| Stabilization | `stabilize.rs` + FFmpeg/vidstab | heavy native deps | ⏸ deferred |
| Background removal | `background.rs`/`cutout.rs` + ONNX model | model download design required | ⏸ deferred |
| Auto-reframe | `reframe.rs` | needs tracking first | ⏸ deferred |
| Collage / cover maker | `cards.rs`, `templates.rs` | UI-heavy | ⏸ deferred |
| Advanced color grading | Lumetri already deep | FilmCraft's Lumetri covers most | 🟡 (existing controls; no new scopes UI) |

## Summary counts (current)

* ✅ Working: baseline engine + editing + captions (incl. new SRT import/export, caption presets) + rebrand
* 🟡 Partially working: karaoke highlighting (timings available, per-word render deferred)
* ⏸ Deferred: Phase 3 list above (documented, not started)
* ⛔ License-blocked: CapCraft code/font/asset copies (awaiting Option A/B decision)

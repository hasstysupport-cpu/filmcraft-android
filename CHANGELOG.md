# Changelog

All notable changes to VantaCut Studio. The project follows the versioning of its FilmCraft base
(`gradle.properties`: `filmcraft.version`, `filmcraft.versionCode`).

## Unreleased — watermarks, luxury captions, font downloads, more transitions

### Smoother preview playback

* Playback no longer draws the same frame twice on high-refresh screens (the display's clock can
  beat the timeline's rate), abandons a picture the playhead has already left (the engine's cancel
  token is now used), drops frames that finish after the playhead moved on (no backward steps),
  and mixes sound in steadier blocks with a half-second buffer. Draft frames draw a little smaller
  while playing, so weak phones keep up.
* Stale frame wishes are dropped before any rendering work starts (the single preview worker used
  to finish every queued picture, then throw it away — the queue backed up and playback stuttered).
* While playing, decoders are told which pictures the playhead has already passed (a three-frame
  catch-up margin): a decoder catching up forward skips them instead of feeding late frames.
* The draft size now adapts to the device: if drawing cannot keep the timeline's frame rate the
  picture steps down (¾, then ⅜ of the preview size), and steps back up when it can again.

### Fonts: no more freezes

* The Fonts screen froze while a font downloaded: every 64 KB chunk rewrote the download state,
  which recomposed the whole list, and every row re-resolved its preview face with a slow system
  `Typeface.create` lookup. Progress is now reported in 256 KB steps, families are resolved once
  and cached, and a downloaded family previews in its real face straight from its file.

### A fuller editor toolbar

* The main tool bar gains **Transition** (opens the transition picker for the cut under the
  playhead), **Filters** and **Effects** (for the selected clip).
* A selected video clip gains **Reverse** (plays backwards, engine-backed, one tap to turn it
  back) and **Rotate** (a quarter turn clockwise).
* Home was redesigned: the top bar shows the app name and free storage, quick actions sit under
  it, projects appear as **Recent Edits** cards (poster, name, length, date, Export and Edit
  buttons, rename/duplicate/delete in the ⋮ menu), and a bottom navigation bar (Home, Projects,
  +, Fonts, Settings) with a new all-projects screen.

### Captions (the headline feature)

* `CaptionLook` grew real typography controls: **font family**, fill colour (ARGB), background
  colour, outline width + outline colour, line spacing and margin — all applied to the caption
  track exactly as asked, each in its own undo step, and all round-trip through the project file.
* The captions panel gained colour swatches, an outline slider (0–12 px), a line-spacing slider
  (1.0–2.0) and font chips listing every font the engine knows.
* Six new luxury caption presets (Neon, Cinema, Impact, Gold, Typewriter, Reel) — **12 presets**
  in total, each mapping onto the full look.
* **16 new bundled fonts** (all SIL OFL 1.1, with licence files and attribution): Amiri
  Regular/Bold, Tajawal Regular/Bold, Noto Kufi Arabic, Montserrat, Poppins Regular/Bold, Bebas
  Neue, Oswald, Playfair Display, Rubik, Dancing Script, Anton, Space Mono Regular/Bold — Arabic
  display (Kufi), Latin display, script and mono faces for real title work.
* **Font downloads**: a Fonts screen (Settings › Fonts) lists 52 Google-Fonts families with live
  previews, downloads them from google/fonts over HTTPS into app storage (resumable, magic-number
  checked), and registers them with the text engine for the whole app, captions included.

### Watermark

* New watermark tool: put **your own text or picture** over the whole video, pinned to any corner,
  with adjustable opacity — built on the real timeline (text clip or picture clip on an overlay
  lane with a motion effect), not a fake overlay. Rename to find it again, remove in one tap.
* New Watermark panel in the editor and a stamp toolbar entry.

### Transitions

* **16 new transitions** (51 → 67): additive dissolve, film dissolve, slide left/up, roll,
  stretch, whip pan, cube spin, flip over, clock wipe, checkerboard, earthquake, flicker, TV
  power, glass. Every preset round-trips through the engine and the project file.

### Engine (Rust)

* New `watermark` module with tests; `MobileSession` gained `watermark`, `setTextWatermark`,
  `setImageWatermark`, `removeWatermark` and `addFontBytes` (runtime font registration).
* 48 unit tests in the app crate (was 44), 47 in `filmcraft-text`.

### Deferred

* Downloading effects from a remote API/service: no key-free open API was found that fits the
  local-first, no-account policy; deferred (more built-in presets shipped instead).
* Everything else listed in [FEATURE_MATRIX.md](FEATURE_MATRIX.md).

## 0.0.2-beta (versionCode 201) — VantaCut Studio, first branded build

### Identity

* Rebranded the whole product as **VantaCut Studio**: new package identity `studio.vantacut.app`,
  new namespace, launcher label, adaptive launcher icon, splash/window colours, About screen and
  user-facing strings.
* New theme: deep black and graphite surfaces, neon violet and deep red accents, white typography,
  restrained gradients. Centralised in `Brand.kt` and `ui/Theme.kt`.
* Bundled **Cairo** (SIL OFL 1.1) beside Inter for Arabic and Latin in one font family.

### Engine (Rust)

* `crates/text`: Cairo Regular/Medium/SemiBold/Bold bundled via `include_bytes!`; Arabic shaping,
  bidirectional ordering and caption wrapping covered by new tests.
* `apps/filmcraft-android`: new `captions_io` module — caption files (SRT, WebVTT, TTML, SCC, MCC,
  EBU STL) import as editable caption clips on the open sequence in one undo step, keeping the
  track's look, and the caption track exports back out (SRT by default).
* Six caption looks (Classic, Clean, Minimal, Bold pop, Broadcast, Violet) applied to the caption
  track's style, each its own undo step.
* Six new cinematic filter looks: bleach bypass, midnight, golden hour, blockbuster, silk, crimson.

### Android UI

* Captions panel: **Import file…** and **Export SRT** actions with document-picker plumbing
  (`EditorHost.stagingDir` / `stageFile` / `savePicked`, new `PickKind.CaptionsFile` /
  `CaptionsSave`), and a **Looks** chip row.
* New `SecondaryButton` component for panel actions.

### Metadata

* Fastlane title, descriptions and changelog rewritten for VantaCut Studio; release workflow
  publishes `vantacut-studio-<version>.apk`.

### Deferred

See [FEATURE_MATRIX.md](FEATURE_MATRIX.md) for the full matrix: word-by-word/karaoke caption
highlighting, Auto Lyrics, word-art/speech bubbles/text animations, stickers and drawing, GIF and
still export, a sound-effect library, and all Phase 3 features (speed curves, masks, chroma key,
motion tracking, stabilisation, background removal, voice isolation, auto-reframe, collage,
advanced colour grading).

## 0.0.1-beta and earlier

The history of the FilmCraft for Android base is preserved unchanged; see the upstream
[artcraft-android/filmcraft-android](https://github.com/artcraft-android/filmcraft-android)
repository and its tags.

# Changelog

All notable changes to VantaCut Studio. The project follows the versioning of its FilmCraft base
(`gradle.properties`: `filmcraft.version`, `filmcraft.versionCode`).

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

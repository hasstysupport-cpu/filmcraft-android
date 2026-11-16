# VantaCut Studio — Migration & Integration Plan

VantaCut Studio is a premium cinematic video-editing studio for Android, built on FilmCraft for
Android as the single application and engine, with selectively migrated features from CapCraft for
Android. This document is the working plan; `FEATURE_MATRIX.md` tracks per-feature status and
`LICENSE_REVIEW.md` tracks the licensing boundary.

## 1. Ground rules

1. FilmCraft stays the one app and the one engine (Rust workspace `crates/*`, UniFFI bridge at
   `apps/filmcraft-android`).
2. CapCraft's Concat engine is **never** merged into the Cargo workspace and its Android app is
   **never** copied. No second editor, no second entry point, no duplicate native libraries.
3. CapCraft code/assets are only used after the license decision in `LICENSE_REVIEW.md`. Until then
   only FilmCraft's own MIT/Apache code and fresh independent implementations are used.
4. FilmCraft's project format, timeline behavior, autosave/recovery and export pipeline are not
   broken; adapters live in the bridge layer (`apps/filmcraft-android/src`), not in engine crates.
5. Product identity is centralized in one place (Kotlin `Brand.kt` + `brand/` resources), so the
   provisional name can change before release.

## 2. Architecture after integration

```
FilmCraft Rust workspace (unchanged layout)
├── crates/*                    engine (edit, render, export, captions, speech, text, …)
└── apps/filmcraft-android      the mobile bridge (UniFFI) + Android app
    ├── src/                    bridge: sessions, edits, export, speech, presets (MIT/Apache)
    └── android/                the VantaCut Studio app (Kotlin, Jetpack Compose)
        └── app/src/main/java/…/vantacut/
            ├── Brand.kt        NEW: one object for name, package identity, palette, type
            ├── ui/             screens (home, editor, export, settings) — rethemed
            └── engine/         UniFFI wrappers (unchanged interfaces)
```

Migrated features enter through:

* **Bridge (Rust)**: new commands in `apps/filmcraft-android/src` that reuse engine crates
  (e.g. caption import calls `filmcraft_captions::parse`).
* **UI (Compose)**: new panels/screens built from the existing reusable components.
* **Presets (data)**: looks/presets are data recipes over existing engine effects.

## 3. Phases

### Phase 0 — Baseline (done)
* Build the unmodified FilmCraft app (debug, arm64-v8a) and run its Rust tests. Results recorded in
  `CHANGELOG.md` and `TESTING.md`.
* Create `feature/vantacut-integration`; upstream remote preserved as `upstream`.

### Phase 1 — Identity (this release)
* `Brand.kt` + palette (deep black/graphite, violet #8B5CF6, red #F43F5E), new icon, splash, themes.
* Arabic-capable typography: Cairo (OFL) for Arabic + Inter (OFL, kept) for Latin; intentional RTL
  layout (mirrored rails/toolbars, Arabic-first copy where the locale is Arabic).
* Rebrand every user-facing surface: launcher, home, editor, export, settings/about, notifications,
  README, Fastlane metadata, CI release workflow names.
* No behavior changes; all FilmCraft functions keep working.

### Phase 2 — Lightweight features (this release, FilmCraft-only code)
1. **Caption import** — SRT (and VTT/TTML via the same crate) parsed by `filmcraft-captions` and
   placed on the caption track as editable caption clips, one undo step.
2. **Caption export** — the caption track written out as SRT (and VTT).
3. **Cinematic caption presets** — preset looks (classic box, clean outline, minimal, bold pop,
   broadcast) mapped onto `CaptionStyle`.
4. **Extra cinematic filter looks** — new Lumetri-delta looks (filmic teal/orange, bleach bypass,
   night, golden hour…) following the existing preset recipe scheme.
5. **Export polish** — clearer resolution/fps/quality choices and honest errors (already strong in
   FilmCraft; extended where cheap).
6. **Arabic caption verification** — shaping/RTL tests on the engine text layout.

### Phase 3 — Advanced (evaluated, deferred unless trivially safe)
Speed curves, text animations, word-art/bubbles, stickers, karaoke word highlighting, auto lyrics,
sound-effect library, GIF/still export, chroma key, tracking, stabilization, background removal,
voice isolation, auto-reframe, collage/cover. Each is documented in `FEATURE_MATRIX.md` with its
dependency and risk; several are additionally gated on the license decision.

## 4. Build & test strategy

* Toolchain: JDK 17, Android SDK API 37 (platform `android-37.0`), NDK 29.0.14206865,
  cargo-ndk 4.1.2, Rust per `rust-toolchain.toml` (1.99.0, targets aarch64/x86_64-linux-android).
* First APK: arm64-v8a debug, then release. Other ABIs after stability.
* Tests: `cargo test --workspace` (Rust), `./gradlew testReleaseUnitTest` (Kotlin/JUnit),
  Compose UI tests for new panels, RTL layout checks, release build verification.
* Memory-constrained CI box: Gradle and cargo are run serialized, with capped heap/parallelism.

## 5. Delivery

* User-owned fork of `artcraft-android/filmcraft-android`; branch `feature/vantacut-integration`;
  never pushed to the upstreams (`upstream` remotes stay read-only).
* Small descriptive commits (semantic style per upstream README), no force-pushes.
* Deliverables listed in the task: docs, test results, APK artifacts, changelog.

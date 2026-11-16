# License Review — VantaCut Studio

*Status: decision pending from the repository owner. Do not merge or ship any code path covered by
this review until the decision is recorded here.*

## 1. The two upstream projects

| Project | License | Engine |
| --- | --- | --- |
| FilmCraft for Android (foundation) | MIT **OR** Apache-2.0 | FilmCraft engine (`crates/*`, Rust) |
| CapCraft for Android (feature reference) | **AGPL-3.0-or-later** | Concat engine (`src/crates/concat-*`, Rust) |

FilmCraft's permissive license means its code can be combined with almost anything, **including**
AGPL code — but the combined *distributable* must then satisfy the AGPL's obligations as a whole.

## 2. What combining AGPL code with FilmCraft means

If any CapCraft (AGPL) source file, substantial implementation fragment, or incompatible asset is
copied into VantaCut Studio:

1. **The entire combined work must be licensed under AGPL-3.0-or-later** when distributed. The
   MIT/Apache files of FilmCraft may stay under their own terms in their own files, but the
   *distributable* (the APK and its complete corresponding source) must carry the AGPL and its
   obligations.
2. **Complete corresponding source** must be offered to every recipient of the APK — including the
   full build toolchain instructions, so they can build it themselves.
3. **Attribution and notices must be preserved**: CapCraft's `LICENSE`, `NOTICE`,
   `THIRD_PARTY_NOTICES.md`, and per-file `SPDX-FileCopyrightText` headers must be kept, and the
   app's About screen and settings must credit CapCraft and the Concat contributors.
4. **FilmCraft's own licensing story changes**: the project can no longer be described as
   "MIT OR Apache-2.0" in its README, badges, F-Droid metadata or CI. It becomes
   "AGPL-3.0-or-later (with MIT/Apache-2.0 components)".
5. **F-Droid and store implications**: AGPL is accepted by F-Droid, but the app's metadata and
   source repostories must reflect it accurately.

## 3. Files and dependencies intended for reuse (candidate list)

Nothing has been copied yet. The candidates identified during the audit, should **Option A** be
chosen:

| CapCraft file / asset | What it provides | Used by (planned) |
| --- | --- | --- |
| `src/crates/concat-text/src/lib.rs` | Text templates, word-art styles, bubbles/shapes, text animations | Text & typography features (Phase 2A) |
| `src/crates/concat-text/fonts/*` + `LICENSE-*.txt` | ~40 OFL display fonts | Font picker (Phase 2A) |
| `src/crates/capcraft-android/src/subtitles.rs` | Caption import (SRT/VTT) and export, karaoke/word highlighting | Captions (Phase 2B) |
| `src/crates/capcraft-android/src/gif.rs` | GIF export drawn frame by frame | Export (Phase 2E) |
| `src/crates/capcraft-android/src/speech.rs` (lyrics parts) | Auto Lyrics workflow | Captions (Phase 2B) |
| `src/crates/concat-effects/*` | Extra effect presets | Effects (Phase 2C) |
| `src/crates/capcraft-android/src/sounds.rs` usage + external sound manifest | Sound-effect browsing | Audio (Phase 2D) — note: sounds are fetched at runtime from an external assets repository, so this needs network and that repository's own licensing checked |

Dependencies that would ride along with any of the above (all checked in CapCraft's
`THIRD_PARTY_NOTICES.md`): FFmpeg (built by `scripts/ffmpeg-mobile.sh`) and sherpa-onnx (built by
`scripts/sherpa-mobile.sh`) are **not needed** for the Phase 2 candidates above; they are only
relevant to Phase 3 items (stabilization, cutout, tracking).

## 4. The decision requested

**Option A — accept AGPL obligations.** Copy the listed CapCraft code/assets, relicense the
distributable under AGPL-3.0-or-later, preserve all attribution, and publish corresponding source.
Fastest path to the richer feature set; the license of the whole app changes.

**Option B — no CapCraft code.** Reimplement the desired features independently inside FilmCraft's
existing architecture (its own `captions`, `text`, `render` and `export` crates), using CapCraft
only as a *feature description* (what the feature does, from its README/screens), never its code or
assets. The app stays MIT/Apache-2.0. Slower, and some ports (e.g. the exact preset look libraries,
bundled fonts) would be replaced by equivalents rather than copies.

## 5. Decision record

| Date | Decision | Notes |
| --- | --- | --- |
| — | *pending* | |

While this decision is pending, work proceeds on: the baseline, rebranding, and features built
purely from FilmCraft's own MIT/Apache code (for example, caption import/export is already
implementable with FilmCraft's own `filmcraft-captions` crate, which parses and writes SRT/VTT and
more).

## 6. Third-party attribution (already in FilmCraft, must be preserved)

* FilmCraft engine — MIT OR Apache-2.0 (`LICENSE-MIT`, `LICENSE-APACHE`, `NOTICE`)
* Inter typeface — SIL OFL (`assets/fonts/OFL-Inter.txt`, shown in-app)
* Lucide icons — ISC (`assets/icons/LICENSE-lucide.txt`, shown in-app)
* Whisper models — MIT (OpenAI), downloaded on demand
* Full list: `ATTRIBUTION.md`

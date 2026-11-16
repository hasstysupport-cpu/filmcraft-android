# Test results

What was run, on what, and what is still open. Everything below was executed on this branch's
source in a Linux x86_64 sandbox.

## Rust (engine + app crate)

`cargo test --workspace` — **2813 passed, 0 failed** (unit tests plus doc tests across the
Cargo workspace, including the FilmCraft engine, project format, captions parser, text engine and
the Android app crate).

New tests added on this branch:

| Test | What it covers |
|---|---|
| `filmcraft-text` `arabic_text_shapes_rtl_with_cairo` | Cairo resolves, covers Arabic, the RTL run orders right-to-left and the Latin run keeps its order |
| `filmcraft-text` `arabic_captions_wrap_and_stay_readable` | Arabic caption text wraps inside a box, no overflow |
| `filmcraft-android` `an_srt_file_becomes_editable_captions_and_exports_back` | SRT import places snapped, editable captions in one undo step; SRT export round-trips |
| `filmcraft-android` `caption_import_refuses_bad_files_with_a_clear_error` | Non-caption files, missing files and empty files fail with meaningful messages; nothing is placed |
| `filmcraft-android` `caption_looks_apply_in_one_undo_step_and_keep_the_captions` | The six looks exist, apply without touching the captions, undo cleanly; unknown ids error |
| `filmcraft-android` `every_filter_effect_and_transition_reads_back_as_itself` (extended) | The six new filter looks round-trip through set/read at a non-default intensity |

## Android

* `./gradlew assembleDebug -Pfilmcraft.abis=arm64-v8a` — **BUILD SUCCESSFUL** (engine + bindings + Kotlin).
* `./gradlew testDebugUnitTest -Pfilmcraft.abis=arm64-v8a` — **10 tests, 0 failures**
  (includes `TimelineMathTest`: trimming, dragging, zoom math).
* `./gradlew assembleRelease -Pfilmcraft.abis=arm64-v8a` — see the release section below.

## Not yet covered

* Compose UI tests for the new captions import/export and looks workflows (the instrumented
  harness needs an emulator; none is available in this environment).
* Arabic RTL layout screenshot tests (the engine-level shaping and wrapping are covered above).
* Export integration tests beyond the existing Rust export tests (`the_export_plan_keeps_the_aspect_and_an_export_finishes`).

## Device testing

**No emulator or physical device was available in this environment.** The APK was built and
lint-checked but never installed or exercised on hardware. Nothing in this repository should be
read as a claim of on-device testing. The upstream project's own device-tested behaviour is
preserved; the new features are covered by the Rust tests above.

## Known open defects

* None known at the engine/test level. On-device issues (performance on mid-range hardware,
  storage-permission flows, background/foreground transitions) remain unverified — see
  *Device testing* above.

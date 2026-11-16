# Test results

What was run, on what, and what is still open. Everything below was executed on this branch's
source in a Linux x86_64 sandbox.

## Rust (engine + app crate)

`cargo test --workspace --lib` — **2266 passed, 0 failed** across 46 test binaries, including the
FilmCraft engine (719), render (208), project format (69), text engine (47) and the Android app
crate (48). The earlier full `cargo test --workspace` run on this branch also passed **2819**
(2811 unit/integration plus 8 doc tests); the lib-only rerun after the watermark/caption-look/
font-pack/transitions work passed everything again.

New tests added on this branch:

| Test | What it covers |
|---|---|
| `filmcraft-text` `arabic_text_shapes_rtl_with_cairo` | Cairo resolves, covers Arabic, the RTL run orders right-to-left and the Latin run keeps its order |
| `filmcraft-text` `arabic_captions_wrap_and_stay_readable` | Arabic caption text wraps inside a box, no overflow |
| `filmcraft-android` `an_srt_file_becomes_editable_captions_and_exports_back` | SRT import places snapped, editable captions in one undo step; SRT export round-trips |
| `filmcraft-android` `caption_import_refuses_bad_files_with_a_clear_error` | Non-caption files, missing files and empty files fail with meaningful messages; nothing is placed |
| `filmcraft-android` `caption_looks_apply_in_one_undo_step_and_keep_the_captions` | The six looks exist, apply without touching the captions, undo cleanly; unknown ids error |
| `filmcraft-android` `every_filter_effect_and_transition_reads_back_as_itself` (extended) | The six new filter looks round-trip through set/read at a non-default intensity; all 67 transitions round-trip |
| `filmcraft-android` `a_text_watermark_pins_to_a_corner_and_reads_back` | Text watermark: place, read back text/corner/opacity, remove |
| `filmcraft-android` `an_image_watermark_uses_a_real_timeline_clip` | Picture watermark imports as an overlay clip, retimed and scaled, discoverable again |
| `filmcraft-android` `watermark_rejects_an_empty_project_with_a_clear_error` | Watermark on a blank sequence fails with a meaningful message |
| `filmcraft-android` `the_caption_look_keeps_its_font_colours_and_spacing_and_presets_set_them` | CaptionLook font/colours/outline/spacing/margin round-trip; the 12 presets set them |
| `filmcraft-text` `the_vantacut_font_pack_is_bundled_and_covers_its_scripts` | The 16 new OFL faces register and cover their scripts |

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

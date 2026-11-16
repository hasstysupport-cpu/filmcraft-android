//! Caption files read in and written out: SubRip (`.srt`) and the other formats FilmCraft's
//! `filmcraft-captions` crate parses (WebVTT, TTML, SCC, MCC, EBU STL), placed on the open
//! sequence's caption track as editable caption clips — the same clips auto captions make. The
//! caption track can also be written back out (SRT by default) as a sidecar file.
//!
//! Everything here is one undo step, and the track's look is kept: an import replaces the captions
//! but not the style the user gave them.

use filmcraft_captions::{Cue, Document, Format, WriteOptions};
use filmcraft_engine::Session;
use filmcraft_project::{Caption, CaptionTrack, ClipId, TrackId};
use filmcraft_time::Tick;

use crate::edits;
use crate::{MobileError, model};

/// Read a caption file and place its cues as the open sequence's captions; returns how many.
///
/// The file's format is detected from its content (and extension as a hint), cues are snapped to
/// the sequence's frames, and they replace any captions made before — keeping their look.
pub(crate) fn import(s: &mut Session, path: &str) -> Result<u32, MobileError> {
    let bytes = std::fs::read(path).map_err(|e| MobileError::Command(format!("That file couldn't be read: {e}")))?;
    let ext = std::path::Path::new(path).extension().and_then(|e| e.to_str());
    let format = filmcraft_captions::detect(&bytes, ext)
        .ok_or_else(|| MobileError::Command("That file isn't a caption file VantaCut can read (try SRT or VTT).".into()))?;
    let mut doc = filmcraft_captions::parse(&bytes, format)
        .map_err(|e| MobileError::Command(format!("That caption file couldn't be read: {e}")))?;
    doc.snap_to_frames(s.sequence_rate());
    if doc.cues.is_empty() {
        return Err(MobileError::Command("There are no captions in that file.".into()));
    }
    if !doc.warnings.is_empty() {
        log::warn!("caption import: {}", doc.warnings.join("; "));
    }
    let n = s.edit_group("Import Captions", |s| {
        let Some(seq) = s.active_sequence() else { return Ok(0) };
        let (size, _) = crate::speech::caption_layout(seq.settings.width, seq.settings.height);
        // captions placed again keep the look the user gave the old ones
        let style = seq
            .caption_tracks
            .first()
            .map(|t| t.style.clone())
            .unwrap_or_else(|| filmcraft_project::CaptionStyle { size: size as f32, margin: 0.1, ..filmcraft_project::CaptionStyle::default() });
        let track_format = format.track_format();
        let count = doc.cues.len();
        s.edit_sequence("Import Captions", |q, ctx, st| {
            let mut t = CaptionTrack::new(TrackId(ctx.alloc()), "Captions".into(), track_format);
            t.style = style;
            t.captions = doc.cues.iter().map(|c| Caption {
                id: ClipId(ctx.alloc()),
                start: c.start,
                duration: Tick(c.end.0.saturating_sub(c.start.0)),
                text: c.text.clone(),
                speaker: c.speaker.clone(),
                cue_id: c.id.clone(),
                settings: c.settings.clone(),
            }).collect();
            t.vtt_blocks = std::mem::take(&mut doc.blocks);
            q.caption_tracks.clear();
            q.caption_tracks.push(t);
            st.caption_selection.clear();
            Ok(())
        })?;
        Ok(count)
    })?;
    edits::settle(s);
    Ok(u32::try_from(n).unwrap_or(u32::MAX))
}


/// Write the open sequence's captions to `path` as `format_name` (srt, vtt, ttml, dfxp, scc, mcc,
/// stl); an unknown name means SRT.
pub(crate) fn export(s: &mut Session, path: &str, format_name: &str) -> Result<(), MobileError> {
    let (_, seq) = model::sequence(s)?;
    let track = seq
        .caption_tracks
        .first()
        .ok_or_else(|| MobileError::Command("There are no captions to export.".into()))?;
    let format = Format::from_name(format_name).unwrap_or(Format::Srt);
    let doc = Document {
        cues: track
            .captions
            .iter()
            .map(|c| Cue { start: c.start, end: c.end(), text: c.text.clone(), speaker: c.speaker.clone(), id: c.cue_id.clone(), settings: c.settings.clone() })
            .collect(),
        blocks: track.vtt_blocks.clone(),
        warnings: Vec::new(),
    };
    let bytes = filmcraft_captions::write(&doc, format, WriteOptions { drop_frame: true, rate: Some(s.sequence_rate()) });
    std::fs::write(path, bytes).map_err(|e| MobileError::Command(format!("That file couldn't be written: {e}")))?;
    Ok(())
}

/// A caption preset: a whole look for the caption track, given by id from [`presets`].
pub(crate) fn apply_preset(s: &mut Session, id: &str) -> Result<(), MobileError> {
    let p = presets().into_iter().find(|p| p.id == id).ok_or_else(|| MobileError::Command("There is no caption look with that name.".into()))?;
    let (track, _) = crate::speech::caption_track(s).ok_or_else(|| MobileError::Command("There are no captions.".into()))?;
    s.edit_sequence_as("Caption Look", None, |q, _, _| {
        let t = q.caption_tracks.iter_mut().find(|t| t.id.0 == track).ok_or_else(|| filmcraft_engine::EngineError::Other("no captions".into()))?;
        t.style.font = p.font.into();
        t.style.size = p.size;
        t.style.color = p.color;
        t.style.background = p.background;
        t.style.background_color = p.background_color;
        t.style.outline = p.outline;
        t.style.outline_color = p.outline_color;
        t.style.anchor = p.anchor;
        t.style.margin = p.margin;
        t.style.line_spacing = p.line_spacing;
        Ok(())
    })?;
    edits::settle(s);
    Ok(())
}

/// A caption look the pickers show.
#[derive(Debug, Clone, uniffi::Record)]
pub struct CaptionPreset {
    pub id: String,
    pub name: String,
    /// What the look does, one line (the pickers' subtitle).
    pub description: String,
}

/// The caption looks: data recipes over the track's style, no new rendering.
pub(crate) fn preset_list() -> Vec<CaptionPreset> {
    vec![
        CaptionPreset { id: "classic".into(), name: "Classic".into(), description: "White on a dark box".into() },
        CaptionPreset { id: "clean".into(), name: "Clean".into(), description: "White with a soft edge".into() },
        CaptionPreset { id: "minimal".into(), name: "Minimal".into(), description: "Small, quiet, no box".into() },
        CaptionPreset { id: "bold-pop".into(), name: "Bold pop".into(), description: "Large, heavy box".into() },
        CaptionPreset { id: "broadcast".into(), name: "Broadcast".into(), description: "Top of the frame".into() },
        CaptionPreset { id: "violet".into(), name: "Violet".into(), description: "VantaCut's accent".into() },
        CaptionPreset { id: "neon".into(), name: "Neon".into(), description: "Violet words with a glowing edge".into() },
        CaptionPreset { id: "cinema".into(), name: "Cinema".into(), description: "Amber serif, wide spacing".into() },
        CaptionPreset { id: "impact".into(), name: "Impact".into(), description: "Huge condensed, thick edge".into() },
        CaptionPreset { id: "gold".into(), name: "Gold".into(), description: "Amiri with a golden edge".into() },
        CaptionPreset { id: "typewriter".into(), name: "Typewriter".into(), description: "Space-mono letters, no box".into() },
        CaptionPreset { id: "reel".into(), name: "Reel".into(), description: "Rounded, boxed, low".into() },
    ]
}

/// One preset's style values, applied by [`apply_preset`]. Sizes are for a 1080-line frame.
struct PresetStyle {
    id: &'static str,
    font: &'static str,
    size: f32,
    color: [u8; 4],
    background: bool,
    background_color: [u8; 4],
    outline: f32,
    outline_color: [u8; 4],
    anchor: filmcraft_project::CaptionAnchor,
    margin: f32,
    line_spacing: f32,
}

fn presets() -> Vec<PresetStyle> {
    use filmcraft_project::CaptionAnchor as A;
    vec![
        PresetStyle { id: "classic", font: "Cairo", size: 54.0, color: [255, 255, 255, 255], background: true, background_color: [0, 0, 0, 191], outline: 0.0, outline_color: [0, 0, 0, 255], anchor: A::Bottom, margin: 0.1, line_spacing: 1.25 },
        PresetStyle { id: "clean", font: "Cairo", size: 54.0, color: [255, 255, 255, 255], background: false, background_color: [0, 0, 0, 0], outline: 3.0, outline_color: [0, 0, 0, 255], anchor: A::Bottom, margin: 0.1, line_spacing: 1.25 },
        PresetStyle { id: "minimal", font: "Cairo", size: 44.0, color: [245, 245, 247, 255], background: false, background_color: [0, 0, 0, 0], outline: 2.0, outline_color: [8, 9, 13, 255], anchor: A::Bottom, margin: 0.08, line_spacing: 1.2 },
        PresetStyle { id: "bold-pop", font: "Cairo", size: 66.0, color: [255, 255, 255, 255], background: true, background_color: [8, 9, 13, 230], outline: 0.0, outline_color: [0, 0, 0, 255], anchor: A::Bottom, margin: 0.12, line_spacing: 1.3 },
        PresetStyle { id: "broadcast", font: "Cairo", size: 48.0, color: [255, 255, 255, 255], background: true, background_color: [16, 17, 24, 217], outline: 0.0, outline_color: [0, 0, 0, 255], anchor: A::Top, margin: 0.08, line_spacing: 1.25 },
        PresetStyle { id: "violet", font: "Cairo", size: 56.0, color: [245, 245, 247, 255], background: true, background_color: [58, 42, 107, 220], outline: 0.0, outline_color: [0, 0, 0, 255], anchor: A::Bottom, margin: 0.1, line_spacing: 1.28 },
        PresetStyle { id: "neon", font: "Cairo", size: 58.0, color: [196, 181, 253, 255], background: false, background_color: [0, 0, 0, 0], outline: 5.0, outline_color: [139, 92, 246, 255], anchor: A::Bottom, margin: 0.1, line_spacing: 1.3 },
        PresetStyle { id: "cinema", font: "Playfair Display", size: 52.0, color: [251, 191, 36, 255], background: false, background_color: [0, 0, 0, 0], outline: 3.0, outline_color: [8, 9, 13, 255], anchor: A::Bottom, margin: 0.12, line_spacing: 1.45 },
        PresetStyle { id: "impact", font: "Anton", size: 72.0, color: [255, 255, 255, 255], background: false, background_color: [0, 0, 0, 0], outline: 6.0, outline_color: [8, 9, 13, 255], anchor: A::Bottom, margin: 0.1, line_spacing: 1.15 },
        PresetStyle { id: "gold", font: "Amiri", size: 58.0, color: [237, 201, 111, 255], background: false, background_color: [0, 0, 0, 0], outline: 4.0, outline_color: [42, 26, 8, 255], anchor: A::Bottom, margin: 0.1, line_spacing: 1.4 },
        PresetStyle { id: "typewriter", font: "Space Mono", size: 46.0, color: [245, 245, 247, 255], background: false, background_color: [0, 0, 0, 0], outline: 2.0, outline_color: [8, 9, 13, 255], anchor: A::Bottom, margin: 0.09, line_spacing: 1.2 },
        PresetStyle { id: "reel", font: "Rubik", size: 50.0, color: [255, 255, 255, 255], background: true, background_color: [244, 63, 94, 216], outline: 0.0, outline_color: [0, 0, 0, 255], anchor: A::Bottom, margin: 0.14, line_spacing: 1.25 },
    ]
}

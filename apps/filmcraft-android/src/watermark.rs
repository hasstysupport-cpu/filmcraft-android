//! The watermark: the user's name or logo pinned over the whole video, the way editing apps sign
//! drafts. One watermark at a time, on an overlay lane, spanning the whole sequence, moved to a
//! corner and faded. The clip is renamed to a fixed name, so it is found again (and removed or
//! replaced) after the project is reopened. Setting or removing it is one undo step.

use filmcraft_engine::{EngineError, Session};
use filmcraft_project::{ClipId, ParamValue};
use serde_json::{Value, json};

use crate::edits::{self, ex};
use crate::{MobileError, model, placement, text};

/// The watermark clip's name; how it is found again after the project reopens.
pub(crate) const NAME: &str = "Watermark";

/// Carry a [`MobileError`] into an edit group's `EngineError` result.
fn engine(e: MobileError) -> EngineError {
    EngineError::Other(e.to_string())
}

/// [`crate::edits::ex`] inside an edit group, whose result is an `EngineError`.
fn exg(s: &mut Session, id: &str, params: Value) -> filmcraft_engine::Result<Value> {
    crate::edits::ex(s, id, params).map_err(engine)
}

/// Where on the frame the watermark sits.
#[derive(Debug, Clone, Copy, PartialEq, Eq, uniffi::Enum)]
pub enum WatermarkCorner {
    TopLeft,
    TopRight,
    Center,
    BottomLeft,
    BottomRight,
}

impl WatermarkCorner {
    /// The watermark's centre in frame pixels: 6 % in from the sides, 9 % from top and bottom.
    fn position(self, w: f64, h: f64) -> (f64, f64) {
        let (x, y) = match self {
            WatermarkCorner::TopLeft => (0.06, 0.09),
            WatermarkCorner::TopRight => (0.94, 0.09),
            WatermarkCorner::Center => (0.5, 0.5),
            WatermarkCorner::BottomLeft => (0.06, 0.91),
            WatermarkCorner::BottomRight => (0.94, 0.91),
        };
        (w * x, h * y)
    }

    fn nearest(w: f64, h: f64, x: f64, y: f64) -> WatermarkCorner {
        let all = [Self::TopLeft, Self::TopRight, Self::Center, Self::BottomLeft, Self::BottomRight];
        all.into_iter()
            .min_by(|a, b| {
                let d = |c: WatermarkCorner| {
                    let (cx, cy) = c.position(w, h);
                    (cx - x).powi(2) + (cy - y).powi(2)
                };
                d(*a).total_cmp(&d(*b))
            })
            .unwrap_or(Self::BottomRight)
    }
}

/// The watermark over the open sequence, if there is one.
#[derive(Debug, Clone, uniffi::Record)]
pub struct WatermarkInfo {
    pub clip: u64,
    /// "text" or "image".
    pub kind: String,
    /// The words, for a text watermark.
    pub text: String,
    /// The picture file, for an image watermark.
    pub path: String,
    pub corner: WatermarkCorner,
    /// 0–100.
    pub opacity: f64,
}

/// The watermark clip, searching the overlay lanes (never the main track).
fn find(s: &Session) -> Option<u64> {
    let seq = s.active_sequence()?;
    seq.video_tracks.iter().skip(1).find_map(|t| t.items.iter().find(|i| i.name == NAME).map(|i| i.id.0))
}

/// The watermark's info, if there is one.
pub(crate) fn get(s: &mut Session) -> Option<WatermarkInfo> {
    let id = find(s)?;
    let (w, h, kind, words, path, opacity) = {
        let seq = s.active_sequence()?;
        let f = model::find(seq, ClipId(id))?;
        let info = model::clip_info(s, seq, &f);
        let (w, h) = (f64::from(seq.settings.width), f64::from(seq.settings.height));
        let opacity = f
            .clip
            .effects
            .iter()
            .find(|e| e.effect == "opacity")
            .map(|e| e.f64_at("opacity", f.clip.source_in))
            .unwrap_or(100.0);
        let kind = if info.text.is_empty() { "image" } else { "text" };
        (w, h, kind, info.text, info.path, opacity)
    };
    // a text watermark sits on its graphics layer; a picture one on its motion effect
    let (x, y) = if kind == "text" {
        text::placement(s, id).map(|p| (p.x, p.y)).unwrap_or((w / 2.0, h / 2.0))
    } else {
        let seq = s.active_sequence()?;
        let f = model::find(seq, ClipId(id))?;
        match f.clip.effects.iter().find(|e| e.effect == "motion" && e.enabled) {
            Some(m) => {
                let p = m.vec2_at("position", f.clip.source_in);
                (if p.x.is_nan() { w / 2.0 } else { p.x }, if p.y.is_nan() { h / 2.0 } else { p.y })
            }
            None => (w / 2.0, h / 2.0),
        }
    };
    Some(WatermarkInfo {
        clip: id,
        kind: kind.into(),
        text: words,
        path,
        corner: WatermarkCorner::nearest(w, h, x, y),
        opacity,
    })
}

/// Remove the watermark; returns whether there was one.
pub(crate) fn remove(s: &mut Session) -> Result<bool, MobileError> {
    let Some(id) = find(s) else { return Ok(false) };
    s.edit_group("Remove Watermark", |s| {
        exg(s, "edit.clear", json!({"clips": [id]}))?;
        Ok(())
    })?;
    edits::settle(s);
    Ok(true)
}

/// Set a text watermark spanning the whole sequence; replaces any watermark. Returns the clip.
pub(crate) fn set_text(s: &mut Session, watermark: &str, corner: WatermarkCorner, opacity: f64) -> Result<u64, MobileError> {
    let words = watermark.trim().to_string();
    if words.is_empty() {
        return Err(MobileError::Command("Write the watermark's words first.".into()));
    }
    let clip = s.edit_group("Set Watermark", |s| {
        let (w, h, dur, size) = {
            let seq = s.active_sequence().ok_or(EngineError::NoSequence)?;
            (f64::from(seq.settings.width), f64::from(seq.settings.height), seq.duration(), text::default_size(seq.settings.width, seq.settings.height))
        };
        let seconds = dur.0 as f64 / filmcraft_time::TICKS_PER_SECOND as f64;
        let track = edits::free_lane_index(s, filmcraft_project::TrackKind::Video, 1, None, filmcraft_time::Tick::ZERO, dur, None).map_err(engine)?;
        remove_in_group(s).map_err(engine)?;
        let r = exg(
            s,
            "graphics.newText",
            json!({
                "text": words, "time": 0, "seconds": seconds.max(1.0),
                "size": size * 0.5,
                "font": "Inter", "fontStyle": "Bold", "box": [w as u32, 1], "track": track,
            }),
        )?;
        let clip = r["clip"].as_u64().ok_or_else(|| EngineError::Other("The watermark could not be added.".into()))?;
        let (cx, cy) = corner.position(w, h);
        text::recenter(s, clip, cx, cy)?;
        set_opacity_pct(s, clip, opacity).map_err(engine)?;
        exg(s, "clip.rename", json!({"clip": clip, "name": NAME}))?;
        Ok(clip)
    })?;
    edits::settle(s);
    Ok(clip)
}

/// Set a picture watermark spanning the whole sequence; replaces any watermark. Returns the clip.
pub(crate) fn set_image(s: &mut Session, path: &str, corner: WatermarkCorner, opacity: f64) -> Result<u64, MobileError> {
    let clip = s.edit_group("Set Watermark", |s| {
        let (w, h, dur) = {
            let seq = s.active_sequence().ok_or(EngineError::NoSequence)?;
            (f64::from(seq.settings.width), f64::from(seq.settings.height), seq.duration())
        };
        remove_in_group(s).map_err(engine)?;
        let items = edits::import(s, &[path.to_string()]).map_err(engine)?;
        let item = items.first().copied().ok_or_else(|| EngineError::Other("The watermark picture could not be imported.".into()))?;
        let track = edits::free_lane_index(s, filmcraft_project::TrackKind::Video, 1, None, filmcraft_time::Tick::ZERO, dur, None).map_err(engine)?;
        let r = exg(s, "timeline.place", json!({"item": item, "time": 0, "track": format!("V{}", track + 1), "video": true}))?;
        let clip = r["clips"].as_array().and_then(|a| a.first()).and_then(Value::as_u64).ok_or_else(|| EngineError::Other("The watermark picture could not be added.".into()))?;
        // a still keeps its default duration: slow it down to span the whole sequence
        let have = s.active_sequence().and_then(|q| model::find(q, ClipId(clip))).map(|f| f.clip.duration).unwrap_or(filmcraft_time::Tick::ZERO);
        if have.0 > 0 && dur.0 > have.0 {
            let pct = (have.0 as f64 / dur.0 as f64 * 100.0).min(100.0);
            exg(s, "clip.speedDuration", json!({"clips": [clip], "speed": pct, "ripple": false}))?;
        }
        // a sixth of the frame's wide, in the corner
        let p = placement::placement(s, clip).map_err(engine)?;
        let scale = (w / 6.0 / p.width.max(1.0)).clamp(0.02, 5.0);
        let (cx, cy) = corner.position(w, h);
        placement::set_placement(s, clip, cx, cy, scale, 0.0, false).map_err(engine)?;
        set_opacity_pct(s, clip, opacity).map_err(engine)?;
        exg(s, "clip.rename", json!({"clip": clip, "name": NAME}))?;
        Ok(clip)
    })?;
    edits::settle(s);
    Ok(clip)
}

/// The watermark's opacity, on a clip that may not carry an opacity effect yet.
fn set_opacity_pct(s: &mut Session, clip: u64, opacity: f64) -> Result<(), MobileError> {
    let pct = opacity.clamp(5.0, 100.0);
    s.edit_sequence("Watermark Opacity", |q, _, _| {
        let c = q.find_item_mut(ClipId(clip)).map(|x| x.1).ok_or_else(|| EngineError::Other("clip gone".into()))?;
        if c.effects.iter().all(|e| e.effect != "opacity") {
            let inst = filmcraft_project::find_effect("opacity")
                .ok_or_else(|| EngineError::Other("opacity is missing".into()))?
                .instance();
            c.effects.push(inst);
        }
        let e = c.effects.iter_mut().find(|e| e.effect == "opacity").ok_or_else(|| EngineError::Other("opacity is missing".into()))?;
        if let Some(p) = e.param_mut("opacity") {
            p.value = ParamValue::Float(pct);
            p.keyframes.clear();
        }
        Ok(())
    })?;
    Ok(())
}

/// Remove the watermark inside an already-open edit group (no group of its own, no settle).
fn remove_in_group(s: &mut Session) -> Result<(), MobileError> {
    let Some(id) = find(s) else { return Ok(()) };
    ex(s, "edit.clear", json!({"clips": [id]}))?;
    Ok(())
}

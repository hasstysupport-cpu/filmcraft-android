package studio.vantacut.app.ui.editor

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import studio.vantacut.app.core.WatermarkCorner
import studio.vantacut.app.ui.Inter
import studio.vantacut.app.ui.LucideIcons
import studio.vantacut.app.ui.PanelAction
import studio.vantacut.app.ui.PrimaryButton
import studio.vantacut.app.ui.SecondaryButton
import studio.vantacut.app.ui.SelectChip
import studio.vantacut.app.ui.SliderRow
import studio.vantacut.app.ui.ToolPanel
import studio.vantacut.app.ui.Vc
import studio.vantacut.app.ui.VcSize
import studio.vantacut.app.ui.VcSlider
import studio.vantacut.app.ui.VcType
import studio.vantacut.app.ui.glyph
import kotlin.math.roundToInt

/** The corners the watermark can sit in, in the order the chips show them. */
private val CORNERS = listOf(
    WatermarkCorner.TOP_LEFT to "Top left",
    WatermarkCorner.TOP_RIGHT to "Top right",
    WatermarkCorner.CENTER to "Middle",
    WatermarkCorner.BOTTOM_LEFT to "Bottom left",
    WatermarkCorner.BOTTOM_RIGHT to "Bottom right",
)

/** Watermark: the user's name or logo over the whole video, faded and placed in a corner. */
@Composable
fun WatermarkPanel(ed: EditorController, close: () -> Unit, modifier: Modifier = Modifier) {
    var info by remember { mutableStateOf<studio.vantacut.app.core.WatermarkInfo?>(null) }
    var words by remember { mutableStateOf("") }
    var corner by remember { mutableStateOf(WatermarkCorner.BOTTOM_RIGHT) }
    var opacity by remember { mutableStateOf(80.0) }
    LaunchedEffect(ed.timeline?.revision) {
        info = ed.watermark()
        info?.let {
            if (it.kind == "text") words = it.text
            corner = it.corner
            opacity = it.opacity
        }
    }
    ToolPanel(
        "Watermark",
        close,
        modifier.imePadding(),
        tag = "watermark_panel",
        actions = {
            if (info != null) PanelAction(LucideIcons.Trash2.glyph(), "Remove the watermark", { ed.removeWatermark() }, tag = "watermark_remove_button")
        },
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = VcSize.PanelPadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Your name or logo over the whole video \u2014 the way studios sign their reels.", style = VcType.BodySmall, color = Vc.Detail)
            Column {
                Text("Words", style = VcType.Label, color = Vc.Secondary)
                Spacer(Modifier.height(6.dp))
                BasicTextField(
                    value = words,
                    onValueChange = { words = it },
                    singleLine = true,
                    textStyle = TextStyle(fontFamily = Inter, fontSize = 16.sp, color = Vc.Title),
                    cursorBrush = SolidColor(Vc.Title),
                    modifier = Modifier.fillMaxWidth().height(40.dp).border(1.dp, Vc.Divider, androidx.compose.foundation.shape.RoundedCornerShape(10.dp)).padding(horizontal = 12.dp),
                )
            }
            Text("Corner", style = VcType.Label, color = Vc.Secondary)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((c, name) in CORNERS) SelectChip(name, corner == c, { corner = c }, tag = "watermark_corner_${name.lowercase().replace(' ', '_')}")
            }
            SliderRow("Opacity", "${opacity.roundToInt()}%") {
                VcSlider(
                    fraction = ((opacity - 5.0) / 95.0).toFloat(),
                    onChange = { f -> opacity = Math.round((5.0 + f * 95.0) / 5.0) * 5.0 },
                    description = "Watermark opacity",
                    tag = "watermark_opacity_slider",
                )
            }
            PrimaryButton("Set text watermark", {
                ed.setTextWatermark(words.trim(), corner, opacity)
            }, enabled = words.isNotBlank(), tag = "watermark_set_text_button")
            SecondaryButton("Use a picture\u2026", { ed.pickWatermarkImage(corner, opacity) }, tag = "watermark_pick_image_button")
            Text("A picture keeps its own shape, sized to a sixth of the frame's width.", style = VcType.BodySmall, color = Vc.Detail)
        }
    }
}

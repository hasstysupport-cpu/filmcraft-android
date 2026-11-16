package studio.vantacut.app.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import studio.vantacut.app.R

/**
 * VantaCut Studio's UI faces (both SIL OFL; copied into the build from FilmCraft's assets and this
 * app's `brand/` fonts): Inter for Latin, Cairo for Arabic — one family, so Arabic text picks Cairo
 * and Latin text picks Inter by glyph coverage, in every label, in both directions.
 */
val VantaCutFont = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_semibold, FontWeight.Bold),
    Font(R.font.cairo_regular, FontWeight.Normal),
    Font(R.font.cairo_medium, FontWeight.Medium),
    Font(R.font.cairo_semibold, FontWeight.SemiBold),
    Font(R.font.cairo_semibold, FontWeight.Bold),
)

/** Inter alone, for Latin-only surfaces where Cairo's metrics would change line heights. */
val Inter = VantaCutFont

/**
 * VantaCut Studio's colours. The app is always dark: deep black and graphite surfaces, a neon
 * violet primary with a deep red accent, clean white type. The timeline's clips are coloured by
 * kind, in hues tuned to sit on the graphite surfaces.
 */
object Vc {
    // surfaces
    val Black = Color(0xFF08090D)
    /** Full-screen pages over the editor (export). */
    val Layer1 = Color(0xFF151720)
    /** Bottom panels, dialogs, menus, the text strip. */
    val Panel = Color(0xFF151720)
    /** Rows and cards on panels. */
    val Raised = Color(0xFF20212B)
    /** The band behind the navigation buttons under a panel. */
    val PanelBand = Color(0xFF101118)
    /** Timeline lanes. */
    val Lane = Color(0xFF0C0D13)
    val Hover = Color(0x1CFFFFFF)
    val Hairline = Color(0x24FFFFFF)
    val Scrim = Color(0x99000000)
    /** Behind a bottom sheet: a light dim, the page stays readable. */
    val SheetScrim = Color(0x52000000)

    // text and icons
    val Title = Color(0xFFF5F5F7)
    val Body = Color(0xFFE2E2E8)
    val Secondary = Color(0xFFA6A6B5)
    val Detail = Color(0xFF8A8A99)
    val Disabled = Color(0xFF474753)
    val Icon = Color(0xFFE2E2E8)

    // accent
    val Accent = Color(0xFF8B5CF6)
    val AccentLight = Color(0xFFA78BFA)
    /** Deep red, for destructive actions and the export/recording states. */
    val Negative = Color(0xFFF43F5E)
    val Recording = Color(0xFFF43F5E)
    /** The violet→red gradient's ends (brand marks, selected states). */
    val GradientStart = Accent
    val GradientEnd = Negative

    // controls
    val SliderOn = Color(0xFFD6D6DE)
    val SliderOff = Color(0xFF43444F)
    val SliderDisabled = Color(0xFF30313B)
    val ThumbFill = Color(0xFF0E0F14)
    val ThumbRing = Color(0xFFEDEDF2)
    val PillButton = Color(0xFF43444F)
    val Handle = Color(0x66CAC4D0)
    val Chip = Color(0xFF2A2B36)
    val Divider = Color(0xFF3A3B47)

    // timeline
    val RulerTick = Color(0xFF6D6D7C)
    val RulerLabel = Color(0xFF8A8A99)
    val ClipRing = Color(0x2BFFFFFF)
    val Grip = Color(0xFF0E0F14)
    val ThumbPlaceholder = Color(0xFF30313B)
    val TransitionMark = Color(0x59FFFFFF)

    /** A clip kind's fill, ring and label/icon colours. */
    data class ClipColors(val fill: Color, val ring: Color, val content: Color)

    /** A video with sound (deep teal, its waveform lane inside). */
    val VideoClip = ClipColors(Color(0xFF003041), Color(0xFF003041), Color(0xFF6BC7FF))
    /** A video without sound, or muted. */
    val SilentVideoClip = ClipColors(Color(0xFF32333F), Color(0xFF32333F), Color(0xFFEBEBEF))
    val BlankClip = ClipColors(Color(0xFF32333F), ClipRing, Color(0xFF6BC7FF))
    val AudioClip = ClipColors(Color(0xFF00402D), Color(0xFF003426), Color(0xFFEBEBEB))
    val MutedClip = ClipColors(Color(0xFF32333F), Color(0xFF32333F), Color(0xFFEBEBEF))
    /** Text clips carry the brand violet. */
    val TextClip = ClipColors(Color(0xFF3A2A6B), Color(0xFF3A2A6B), Color(0xFFC4B5FD))
    val TextClipSelected = ClipColors(Color(0xFF4C3688), Color(0xFF4C3688), Color(0xFFDDD3FE))
    /** Caption clips carry the brand red, dimmed to sit on graphite. */
    val CaptionClip = ClipColors(Color(0xFF4A1522), Color(0xFF5C1A2B), Color(0xFFFDA4AF))
    val CaptionClipSelected = ClipColors(Color(0xFF5E1B2C), Color(0xFF5E1B2C), Color(0xFFFECDD3))
    val MissingClip = ClipColors(Color(0xFF571107), Color(0xFF571107), Color(0xFFE68619))
    val RecordingClip = ClipColors(Recording, Color(0xFFBE123C), Color(0xFFEBEBEF))

    /** Waveform fill and outline, plain and selected. */
    data class WaveColors(val fill: Color, val line: Color, val fillSelected: Color, val lineSelected: Color)

    val AudioWave = WaveColors(Color(0xFF005F3C), Color(0xFF19D979), Color(0xFF007042), Color(0xFF4FEC9C))
    val VideoWave = WaveColors(Color(0xFF03638C), Color(0xFF6BC7FF), Color(0xFF0873A8), Color(0xFF98DBFF))
    val MutedWave = WaveColors(Color(0xFF6D6D7C), Color(0xFFD6D6DE), Color(0xFF8A8A99), Color(0xFFEBEBEF))

    // motion
    val EaseOut = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EaseInOut = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
}

/** VantaCut Studio's text styles: Inter/Cairo, no tracking, the sizes the screens use. */
object VcType {
    private fun s(size: Int, weight: FontWeight, line: Float = 1.3f) =
        TextStyle(fontFamily = VantaCutFont, fontSize = size.sp, lineHeight = (size * line).sp, fontWeight = weight, letterSpacing = 0.sp)

    val PanelTitle = s(20, FontWeight.Bold)
    val DialogTitle = s(20, FontWeight.Bold)
    val Body = s(16, FontWeight.Normal, 1.5f)
    val BodySmall = s(14, FontWeight.Normal, 1.5f)
    val Label = s(16, FontWeight.Normal)
    val Value = s(17, FontWeight.Normal)
    val ToolLabel = s(12, FontWeight.Normal, 1.5f)
    val TileLabel = s(14, FontWeight.Normal)
    val Caption = s(12, FontWeight.Medium)
    val Badge = s(11, FontWeight.Normal)
    val Ruler = s(14, FontWeight.Normal)
    val ClipLabel = s(11, FontWeight.Normal)
    val Header = s(18, FontWeight.Bold)
    val Button = s(16, FontWeight.Bold)
}

/** Sizes shared by the screens. */
object VcSize {
    val TopBar = 48.dp
    val TopButton = 40.dp
    val Transport = 48.dp
    val ToolTile = 80.dp
    val ToolChip = 40.dp
    val PanelCorner = 28.dp
    val PanelInset = 12.dp
    val PanelPadding = 24.dp
    val MainTrack = 64.dp
    val Track = 48.dp
    val TrackGap = 2.dp
    val Ruler = 15.dp
    val RulerGap = 4.dp
}

@Composable
fun VantaCutTheme(content: @Composable () -> Unit) {
    val colors = darkColorScheme(
        primary = Vc.Accent,
        onPrimary = Color.White,
        background = Vc.Black,
        onBackground = Vc.Body,
        surface = Vc.Panel,
        onSurface = Vc.Body,
        surfaceVariant = Vc.Raised,
        onSurfaceVariant = Vc.Secondary,
        surfaceContainer = Vc.Panel,
        surfaceContainerHigh = Vc.Panel,
        error = Vc.Negative,
    )
    MaterialTheme(colorScheme = colors, content = content)
}

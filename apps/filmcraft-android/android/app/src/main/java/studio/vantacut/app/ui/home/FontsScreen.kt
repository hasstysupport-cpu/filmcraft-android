package studio.vantacut.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import studio.vantacut.app.engine.FontCatalog
import studio.vantacut.app.engine.FontDownloads
import studio.vantacut.app.ui.AppViewModel
import studio.vantacut.app.ui.DialogButton
import studio.vantacut.app.ui.Vc
import studio.vantacut.app.ui.VcType
import studio.vantacut.app.ui.editor.familyFor

/** The font family a row previews in: a downloaded family shows in its real face from its file. */
private val fileFaces = java.util.concurrent.ConcurrentHashMap<String, FontFamily>()

private fun previewFor(name: String, installed: Set<String>, ctx: android.content.Context): FontFamily {
    val base = name.substringBefore(" Bold")
    if (base in installed) {
        return fileFaces.getOrPut(base) {
            runCatching { FontFamily(android.graphics.Typeface.createFromFile(java.io.File(FontDownloads.fontsDir(ctx), "$base.ttf"))) }.getOrDefault(FontFamily.SansSerif)
        }
    }
    return familyFor(base)
}

/** Settings ▸ Fonts: the font pack that came with the app, and every family that can be downloaded. */
@Composable
fun FontsScreen(vm: AppViewModel) {
    val ctx = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    val downloading = FontDownloads.state
    val installed = remember(refresh, downloading == null) { FontDownloads.installed(ctx).toSet() }
    Page("Fonts", { vm.back() }, "screen_fonts") {
        Text(
            "Cairo, Amiri and the other faces below come with the app. Every family here is free (SIL Open Font License) and works offline once downloaded.",
            style = VcType.BodySmall,
            color = Vc.Detail,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Spacer(Modifier.height(16.dp))
        LazyColumn(Modifier.fillMaxSize()) {
            items(FontCatalog.FONTS.size) { i ->
                val f = FontCatalog.FONTS[i]
                val have = f.family.removeSuffix(" Bold") in installed || f.family in installed
                val face = previewFor(f.family, installed, ctx)
                val busy = downloading != null && downloading.error == null
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 12.dp).testTag("font_row_${f.family.replace(' ', '_')}"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(f.family, fontFamily = face, fontSize = 20.sp, color = Vc.Title, maxLines = 1)
                        Text(if (f.arabic) "Arabic \u00b7 SIL OFL" else "SIL OFL", style = VcType.BodySmall, color = Vc.Detail)
                    }
                    when {
                        have -> Text("Installed", style = VcType.BodySmall, color = Vc.Detail)
                        downloading != null && downloading.family == f.family && downloading.error == null -> {
                            Text("${(downloading.fraction * 100).toInt()}%", style = VcType.BodySmall, color = Vc.Secondary)
                            Spacer(Modifier.size(8.dp))
                            DialogButton("Cancel", { FontDownloads.cancel() }, filled = false)
                        }
                        downloading != null && downloading.family == f.family && downloading.error != null -> DialogButton("Retry", { FontDownloads.dismiss(); FontDownloads.start(ctx, f) { refresh++ } }, filled = true)
                        busy -> Text("\u2026", style = VcType.BodySmall, color = Vc.Detail)
                        else -> DialogButton("Get", { FontDownloads.start(ctx, f) { refresh++ } }, filled = false)
                    }
                }
            }
        }
    }
    downloading?.error?.let { e ->
        Box(Modifier.fillMaxSize().clickable(role = Role.Button) { FontDownloads.dismiss() }) {
            Column(
                Modifier.align(Alignment.Center).fillMaxWidth().padding(32.dp).clip(RoundedCornerShape(16.dp)).background(Vc.Layer1).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(e, style = VcType.Body, color = Vc.Body)
                Spacer(Modifier.height(12.dp))
                DialogButton("OK", { FontDownloads.dismiss() }, filled = true)
            }
        }
    }
}

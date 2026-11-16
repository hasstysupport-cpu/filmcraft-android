package studio.vantacut.app.ui.home

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import studio.vantacut.app.Brand
import studio.vantacut.app.R
import studio.vantacut.app.core.ModelPackage
import studio.vantacut.app.core.engineVersion
import studio.vantacut.app.core.speechModels
import studio.vantacut.app.core.voicePackage
import studio.vantacut.app.engine.ModelDownloads
import studio.vantacut.app.engine.megabytes
import studio.vantacut.app.ui.DialogButton
import studio.vantacut.app.ui.VcDialog
import java.io.File
import studio.vantacut.app.ui.AppViewModel
import studio.vantacut.app.ui.Vc
import studio.vantacut.app.ui.VcIconButton
import studio.vantacut.app.ui.VcType
import studio.vantacut.app.ui.GlyphIcon
import studio.vantacut.app.ui.LucideIcons
import studio.vantacut.app.ui.Screen
import studio.vantacut.app.ui.glyph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val SOURCE_URL = Brand.SOURCE_URL

@Composable
fun Page(title: String, onClose: () -> Unit, tag: String, content: @Composable () -> Unit) {
    BackHandler(onBack = onClose)
    Column(Modifier.fillMaxSize().background(Vc.Black).statusBarsPadding().navigationBarsPadding().testTag(tag)) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            VcIconButton(LucideIcons.X.glyph(), "Close", onClose)
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Text(title, style = VcType.PanelTitle.copy(fontSize = VcType.PanelTitle.fontSize * 1.3f), color = Vc.Title)
            Spacer(Modifier.height(24.dp))
            content()
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Row48(label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = VcType.Body, color = Vc.Body, modifier = Modifier.weight(1f))
        GlyphIcon(LucideIcons.ChevronRight.glyph(), Vc.Detail, size = 20.dp)
    }
}

/** What a downloaded package is for, in the user's words. */
private fun packageLabel(id: String) = when (id) {
    "whisper-tiny" -> "Captions: faster speech model"
    "whisper-base" -> "Captions: more accurate speech model"
    else -> "Natural voices"
}

/** Bytes a package's folder holds now (finished and partly downloaded files). */
private fun bytesOnDisk(p: ModelPackage): Long = File(p.dir).listFiles()?.sumOf { if (it.isFile) it.length() else 0L } ?: 0L

@Composable
fun SettingsScreen(vm: AppViewModel) {
    var refresh by remember { mutableIntStateOf(0) }
    var asking by remember { mutableStateOf<ModelPackage?>(null) }
    val downloading = ModelDownloads.state?.id
    val stored by produceState(emptyList<Pair<ModelPackage, Long>>(), refresh, downloading) {
        value = withContext(Dispatchers.IO) {
            runCatching { (speechModels() + voicePackage()).map { it to bytesOnDisk(it) }.filter { it.second > 0 } }.getOrDefault(emptyList())
        }
    }
    Page("Settings", { vm.back() }, "screen_settings") {
        Row48("About ${Brand.SHORT_NAME}") { vm.show(Screen.About) }
        Row48("Fonts") { vm.show(Screen.Fonts) }
        if (stored.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text("Downloaded for captions and speech", style = VcType.Label, color = Vc.Secondary, modifier = Modifier.padding(horizontal = 12.dp))
            Spacer(Modifier.height(8.dp))
            for ((p, bytes) in stored) {
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 12.dp).testTag("settings_model_${p.id}"), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(packageLabel(p.id), style = VcType.Body, color = Vc.Body)
                        Text(megabytes(bytes), style = VcType.BodySmall, color = Vc.Detail)
                    }
                    if (downloading != p.id) DialogButton("Delete", { asking = p }, filled = false)
                }
            }
        }
    }
    asking?.let { p ->
        VcDialog(
            "Delete ${packageLabel(p.id).lowercase().replaceFirstChar { it.uppercase() }}?",
            "Delete",
            onConfirm = {
                asking = null
                File(p.dir).deleteRecursively()
                refresh++
            },
            onDismiss = { asking = null },
            destructive = true,
        ) {
            Text("It's downloaded again the next time it's needed.", style = VcType.Body, color = Vc.Body)
        }
    }
}

@Composable
fun AboutScreen(vm: AppViewModel) {
    val ctx = LocalContext.current
    Page("About ${Brand.SHORT_NAME}", { vm.back() }, "screen_about") {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Vc.Layer1).padding(16.dp)) {
            Image(painterResource(R.drawable.ic_launcher_tile), contentDescription = Brand.APP_NAME, modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)))
            Spacer(Modifier.height(12.dp))
            Text(Brand.APP_NAME, style = VcType.Header, color = Vc.Title)
            Text("Version ${vm.appVersion} (engine ${engineVersion()})", style = VcType.BodySmall, color = Vc.Detail)
            Spacer(Modifier.height(12.dp))
            Text(
                Brand.ENGINE_CREDIT + " VantaCut Studio is not affiliated with Adobe; Premiere is a trademark of Adobe Inc.",
                style = VcType.BodySmall,
                color = Vc.Body,
            )
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Vc.Hairline, thickness = 1.dp)
            Row48("Source code") { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SOURCE_URL))) } }
            Row48("Open-source licences") { vm.show(Screen.Licences) }
        }
    }
}

@Composable
fun LicencesScreen(vm: AppViewModel) {
    val ctx = LocalContext.current
    val texts by produceState(emptyList<Pair<String, String>>()) {
        value = withContext(Dispatchers.IO) {
            val names = runCatching { ctx.assets.list("licences")?.toList() }.getOrNull().orEmpty()
            names.mapNotNull { n -> runCatching { n to ctx.assets.open("licences/$n").bufferedReader().readText() }.getOrNull() }
        }
    }
    Page("Open-source licences", { vm.back() }, "screen_licences") {
        val parts = listOf(
            "FilmCraft engine" to "MIT OR Apache-2.0, the FilmCraft contributors (github.com/storytold/filmcraft)",
            "Inter (UI font)" to "SIL Open Font License 1.1, the Inter Project Authors",
            "Lucide (icons)" to "ISC, Lucide Contributors",
            "UniFFI" to "MPL-2.0, Mozilla",
            "JNA" to "Apache-2.0",
            "AndroidX, Jetpack Compose, Kotlin coroutines" to "Apache-2.0",
            "candle (speech and voices on the phone)" to "MIT OR Apache-2.0, Hugging Face",
            "Whisper speech models (downloaded when asked)" to "MIT, OpenAI",
            "Kokoro-82M natural voices (downloaded when asked)" to "Apache-2.0, hexgrad; CMUdict: BSD-style, Carnegie Mellon University",
        )
        for ((name, licence) in parts) {
            Text(name, style = VcType.Label, color = Vc.Title)
            Text(licence, style = VcType.BodySmall, color = Vc.Detail)
            Spacer(Modifier.height(12.dp))
        }
        for ((name, text) in texts) {
            HorizontalDivider(color = Vc.Hairline, thickness = 1.dp)
            Spacer(Modifier.height(12.dp))
            Text(name, style = VcType.Label, color = Vc.Title)
            Spacer(Modifier.height(8.dp))
            Text(text, style = VcType.BodySmall, color = Vc.Detail)
            Spacer(Modifier.height(12.dp))
        }
    }
}

package studio.vantacut.app.ui.home

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import studio.vantacut.app.Brand
import studio.vantacut.app.R
import studio.vantacut.app.engine.ProjectEntry
import studio.vantacut.app.ui.AppViewModel
import studio.vantacut.app.ui.Vc
import studio.vantacut.app.ui.VcDialog
import studio.vantacut.app.ui.VcIconButton
import studio.vantacut.app.ui.VcType
import studio.vantacut.app.ui.Glyph
import studio.vantacut.app.ui.GlyphIcon
import studio.vantacut.app.ui.LucideIcons
import studio.vantacut.app.ui.NewProject
import studio.vantacut.app.ui.Screen
import studio.vantacut.app.ui.glyph
import studio.vantacut.app.ui.editor.TPS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

/** Home: ways to start a project, the recent edits, and the bottom navigation. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(vm: AppViewModel) {
    Box(Modifier.fillMaxSize().background(Vc.Black).windowInsetsPadding(WindowInsets.displayCutout.union(WindowInsets.navigationBars).only(WindowInsetsSides.Horizontal)).testTag("screen_home")) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            HomeTopBar(vm)
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                FlowRow(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 28.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 4,
                ) {
                    QuickTile(LucideIcons.SquarePlus.glyph(), "New blank project", "new_blank_project") { vm.newProject(NewProject.Blank) }
                    QuickTile(LucideIcons.FilePlus.glyph(), "Import files", "new_from_files") { vm.newProject(NewProject.Files) }
                    QuickTile(LucideIcons.Images.glyph(), "From photo library", "from_your_photo_library") { vm.newProject(NewProject.PhotoLibrary) }
                    QuickTile(LucideIcons.AudioLines.glyph(), "Extract audio & SFX", "extract_audio_button") { vm.newProject(NewProject.ExtractAudio) }
                }
                Text("Recent Edits", style = VcType.Header, color = Vc.Title, modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp).testTag("home_your_projects_header"))
                if (vm.projectList.isEmpty()) {
                    EmptyProjects()
                    Spacer(Modifier.height(24.dp))
                } else {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("home_projects_list"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        for (p in vm.projectList) ProjectCard(vm, p)
                    }
                    Spacer(Modifier.height(24.dp))
                }
                vm.engineProblem?.let {
                    Text("The editor couldn't start: $it", style = VcType.Body, color = Vc.Negative, modifier = Modifier.padding(16.dp))
                }
                Spacer(Modifier.height(96.dp))
            }
            BottomNav(vm, Screen.Home)
        }
        if (vm.opening) {
            Box(Modifier.fillMaxSize().background(Vc.Scrim), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Vc.Accent) }
        }
    }
    vm.renaming?.let { RenameDialog(vm, it) }
    vm.deleting?.let { p ->
        VcDialog("“${p.name}”?", "Delete", { vm.delete(p) }, { vm.deleting = null }, destructive = true) {
            Text("The project will be deleted from this phone.", style = VcType.Body, color = Vc.Body)
        }
    }
}

/** Every project, as rows (the same cards Home shows). */
@Composable
fun ProjectsScreen(vm: AppViewModel) {
    Box(Modifier.fillMaxSize().background(Vc.Black).windowInsetsPadding(WindowInsets.displayCutout.union(WindowInsets.navigationBars).only(WindowInsetsSides.Horizontal)).testTag("screen_projects")) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            HomeTopBar(vm)
            Text("Projects", style = VcType.Header, color = Vc.Title, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp).testTag("projects_header"))
            if (vm.projectList.isEmpty()) {
                Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Center) { EmptyProjects() }
            } else {
                LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("projects_list"), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(vm.projectList, key = { it.id }) { p -> ProjectCard(vm, p) }
                }
            }
            BottomNav(vm, Screen.Projects)
        }
        if (vm.opening) {
            Box(Modifier.fillMaxSize().background(Vc.Scrim), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Vc.Accent) }
        }
    }
    vm.renaming?.let { RenameDialog(vm, it) }
    vm.deleting?.let { p ->
        VcDialog("“${p.name}”?", "Delete", { vm.delete(p) }, { vm.deleting = null }, destructive = true) {
            Text("The project will be deleted from this phone.", style = VcType.Body, color = Vc.Body)
        }
    }
}

/** The app's bottom navigation: Home, Projects, +, Fonts, Settings. */
@Composable
fun BottomNav(vm: AppViewModel, here: Screen) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Vc.PanelBand)
            .navigationBarsPadding()
            .padding(top = 6.dp, bottom = 6.dp)
            .testTag("bottom_nav"),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavTab(LucideIcons.House.glyph(), "Home", here == Screen.Home, "nav_home") { vm.show(Screen.Home) }
        NavTab(LucideIcons.Film.glyph(), "Projects", here == Screen.Projects, "nav_projects") { vm.show(Screen.Projects) }
        Box(
            Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(0f to Vc.Accent, 1f to Vc.AccentLight))
                .clickable(role = Role.Button) { vm.newProject(NewProject.PhotoLibrary) }
                .testTag("new_project")
                .semantics { contentDescription = "New project" },
            contentAlignment = Alignment.Center,
        ) {
            GlyphIcon(LucideIcons.Plus.glyph(), Color.White, size = 26.dp)
        }
        NavTab(LucideIcons.Type.glyph(), "Fonts", false, "nav_fonts") { vm.show(Screen.Fonts) }
        NavTab(LucideIcons.Settings.glyph(), "Settings", false, "nav_settings") { vm.show(Screen.Settings) }
    }
}

@Composable
private fun NavTab(glyph: Glyph, label: String, active: Boolean, tag: String, onClick: () -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GlyphIcon(glyph, if (active) Vc.AccentLight else Vc.Detail, size = 22.dp)
        Spacer(Modifier.height(2.dp))
        Text(label, style = VcType.Badge, color = if (active) Vc.Title else Vc.Detail)
    }
}

@Composable
private fun EmptyProjects() {
    Column(Modifier.fillMaxWidth().padding(horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(72.dp).clip(CircleShape).background(Vc.Layer1), contentAlignment = Alignment.Center) {
            GlyphIcon(LucideIcons.Film.glyph(), Vc.Detail, size = 32.dp)
        }
        Spacer(Modifier.height(12.dp))
        Text("No edits yet", style = VcType.PanelTitle, color = Vc.Title, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text("Start a project above — from your photo library, your files, or blank.", style = VcType.BodySmall, color = Vc.Detail, textAlign = TextAlign.Center)
    }
}

@Composable
private fun HomeTopBar(vm: AppViewModel) {
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp).testTag("home_screen_top_bar"), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.ic_launcher_tile), contentDescription = Brand.APP_NAME, modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)))
        }
        Spacer(Modifier.width(10.dp))
        Text(Brand.APP_NAME, style = VcType.PanelTitle, color = Vc.Title)
        Spacer(Modifier.width(10.dp))
        val storage = remember { vm.storageFree() }
        if (storage.isNotEmpty()) {
            Box(Modifier.clip(RoundedCornerShape(999.dp)).background(Vc.Layer1).padding(horizontal = 10.dp, vertical = 4.dp).testTag("home_storage_chip")) {
                Text(storage, style = VcType.Badge, color = Vc.Detail)
            }
        }
        Spacer(Modifier.weight(1f))
        Box {
            VcIconButton(LucideIcons.Ellipsis.glyph(), "More options", { menu = true }, size = 48.dp, modifier = Modifier.testTag("more_options"))
            DropdownMenu(menu, { menu = false }, containerColor = Vc.Panel, shape = RoundedCornerShape(12.dp)) {
                DropdownMenuItem(
                    text = { Text("Settings", style = VcType.Label, color = Vc.Body) },
                    leadingIcon = { GlyphIcon(LucideIcons.Settings.glyph(), Vc.Icon, size = 20.dp) },
                    onClick = { menu = false; vm.show(Screen.Settings) },
                )
            }
        }
    }
}

@Composable
private fun QuickTile(glyph: Glyph, label: String, tag: String, onClick: () -> Unit) {
    Column(
        Modifier
            .width(94.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(16.dp)).background(Vc.Layer1), contentAlignment = Alignment.Center) {
            GlyphIcon(glyph, Vc.Icon)
        }
        Spacer(Modifier.height(8.dp))
        Text(label, style = VcType.ToolLabel, color = Vc.Body, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** A project's length on its card: whole seconds rounded up (":07", "1:05", "1:02:03"). */
fun cardDuration(ticks: Long): String {
    val s = (ticks + TPS - 1) / TPS
    val h = s / 3600
    val m = (s / 60) % 60
    val sec = s % 60
    return when {
        h > 0 -> "%d:%02d:%02d".format(h, m, sec)
        m > 0 -> "%d:%02d".format(m, sec)
        else -> ":%02d".format(sec)
    }
}

/** One recent edit: its poster, name, length, date, and Edit / Export / more. */
@Composable
private fun ProjectCard(vm: AppViewModel, p: ProjectEntry) {
    var menu by remember { mutableStateOf(false) }
    val poster by produceState<ImageBitmap?>(null, p.id, p.opened) {
        value = withContext(Dispatchers.IO) { runCatching { BitmapFactory.decodeFile(p.poster.absolutePath)?.asImageBitmap() }.getOrNull() }
    }
    val date = remember(p.id) { DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(p.opened)) }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Vc.Layer1)
            .border(1.dp, Vc.Hairline, RoundedCornerShape(16.dp))
            .padding(10.dp)
            .testTag("home_project_${p.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(112.dp)
                .height(66.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Vc.Raised)
                .clickable(role = Role.Button) { vm.openProject(p) },
            contentAlignment = Alignment.Center,
        ) {
            poster?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            Box(Modifier.align(Alignment.BottomEnd).padding(4.dp).clip(RoundedCornerShape(6.dp)).background(Vc.Scrim).padding(horizontal = 5.dp, vertical = 1.dp)) {
                Text(cardDuration(p.duration), style = VcType.Badge, color = Vc.Title)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(p.name, style = VcType.Label, color = Vc.Title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text("Project • ${cardDuration(p.duration)}", style = VcType.Badge, color = Vc.Detail, maxLines = 1)
            Text(date, style = VcType.Badge, color = Vc.Detail, maxLines = 1)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CardButton("Export", "project_export_${p.id}") { vm.exportProject(p) }
                CardButton("Edit", "project_edit_${p.id}", filled = true) { vm.openProject(p) }
            }
        }
        Box {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button) { menu = true }
                    .semantics { contentDescription = "More options" },
                contentAlignment = Alignment.Center,
            ) {
                GlyphIcon(LucideIcons.EllipsisVertical.glyph(), Vc.Detail, size = 18.dp)
            }
            DropdownMenu(menu, { menu = false }, containerColor = Vc.Panel, shape = RoundedCornerShape(12.dp)) {
                DropdownMenuItem(text = { Text("Rename", style = VcType.Label, color = Vc.Body) }, leadingIcon = { GlyphIcon(LucideIcons.Pencil.glyph(), Vc.Icon, size = 20.dp) }, onClick = { menu = false; vm.renaming = p })
                DropdownMenuItem(text = { Text("Duplicate", style = VcType.Label, color = Vc.Body) }, leadingIcon = { GlyphIcon(LucideIcons.CopyPlus.glyph(), Vc.Icon, size = 20.dp) }, onClick = { menu = false; vm.duplicate(p) })
                DropdownMenuItem(text = { Text("Delete", style = VcType.Label, color = Vc.Negative) }, leadingIcon = { GlyphIcon(LucideIcons.Trash2.glyph(), Vc.Negative, size = 20.dp) }, onClick = { menu = false; vm.deleting = p })
            }
        }
    }
}

@Composable
private fun CardButton(label: String, tag: String, filled: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (filled) Vc.Accent else Vc.Raised)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .testTag(tag),
    ) {
        Text(label, style = VcType.Badge, color = if (filled) Color.White else Vc.Body)
    }
}

@Composable
private fun RenameDialog(vm: AppViewModel, p: ProjectEntry) {
    var name by remember(p.id) { mutableStateOf(p.name) }
    val ok = name.trim().isNotEmpty() && name.trim() != p.name
    VcDialog("Rename project", "Save", { if (ok) vm.rename(p, name) }, { vm.renaming = null }, confirmEnabled = ok) {
        Text("Project name", style = VcType.Caption, color = Vc.Detail)
        Spacer(Modifier.height(8.dp))
        androidx.compose.foundation.text.BasicTextField(
            value = name,
            onValueChange = { name = it },
            singleLine = true,
            textStyle = VcType.Body.copy(color = Vc.Title),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(Vc.Title),
            modifier = Modifier.fillMaxWidth().border(1.dp, Vc.Divider, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
        )
    }
}

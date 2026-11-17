package studio.vantacut.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import studio.vantacut.app.ui.editor.EditorScreen
import studio.vantacut.app.ui.export.ExportScreen
import studio.vantacut.app.ui.home.AboutScreen
import studio.vantacut.app.ui.home.FontsScreen
import studio.vantacut.app.ui.home.HomeScreen
import studio.vantacut.app.ui.home.LicencesScreen
import studio.vantacut.app.ui.home.ProjectsScreen
import studio.vantacut.app.ui.home.SettingsScreen

/** The app: the current screen, and messages along the bottom. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AppRoot(vm: AppViewModel) {
    val snackbars = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        for (m in vm.messages) snackbars.showSnackbar(m)
    }
    // test tags double as resource ids, so on-device checks can find controls by name
    Box(Modifier.fillMaxSize().background(Vc.Black).semantics { testTagsAsResourceId = true }) {
        val ed = vm.editor
        when (vm.screen) {
            Screen.Home -> HomeScreen(vm)
            Screen.Projects -> ProjectsScreen(vm)
            Screen.Editor -> if (ed != null) EditorScreen(ed) else HomeScreen(vm)
            Screen.Export -> ExportScreen(vm)
            Screen.Settings -> SettingsScreen(vm)
            Screen.About -> AboutScreen(vm)
            Screen.Licences -> LicencesScreen(vm)
            Screen.Fonts -> FontsScreen(vm)
        }
        SnackbarHost(snackbars, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 96.dp)) { data ->
            Snackbar(shape = RoundedCornerShape(8.dp), containerColor = Vc.Raised, contentColor = Vc.Title) {
                Text(data.visuals.message, style = VcType.BodySmall, color = Vc.Title)
            }
        }
    }
}

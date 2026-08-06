package app.solvex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.viewmodel.compose.viewModel
import app.solvex.ads.AdManager
import app.solvex.model.AppScreen
import app.solvex.ui.screens.GameScreen
import app.solvex.ui.screens.HomeScreen
import app.solvex.ui.screens.StatsScreen
import app.solvex.ui.screens.VictoryScreen
import app.solvex.ui.theme.SolvexTheme
import app.solvex.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.White.toArgb(), Color.White.toArgb()),
            navigationBarStyle = SystemBarStyle.light(Color.White.toArgb(), Color.White.toArgb())
        )
        AdManager.init(applicationContext)
        setContent {
            val vm: GameViewModel = viewModel()
            val darkMode by vm.darkMode.collectAsState()
            SolvexTheme(darkTheme = darkMode) {
                ElementFlowApp(vm = vm, darkMode = darkMode)
            }
        }
    }
}

@Composable
fun ElementFlowApp(vm: GameViewModel, darkMode: Boolean) {
    val screen by vm.screen.collectAsState()
    Box(modifier = Modifier.fillMaxSize().background(Color.White).safeDrawingPadding()) {
        when (screen) {
            AppScreen.HOME    -> HomeScreen(vm = vm, darkMode = darkMode)
            AppScreen.GAME    -> GameScreen(vm = vm, darkMode = darkMode)
            AppScreen.VICTORY -> VictoryScreen(vm = vm, darkMode = darkMode)
            AppScreen.STATS   -> StatsScreen(vm = vm)
        }
    }
}

package app.solvex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.solvex.model.AppScreen
import app.solvex.ui.theme.*
import app.solvex.viewmodel.GameViewModel

@Composable
fun LevelSelectScreen(vm: GameViewModel, darkMode: Boolean) {
    val bg = if (darkMode) DarkBg else LightBg
    val textColor = if (darkMode) Color.White else Color(0xFF1A1A3E)
    val totalLevels by vm.levelCount.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(bg, if (darkMode) Color(0xFF0A0A22) else Color(0xFFE8EAFF))
                )
            )
            .padding(16.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            TextButton(onClick = { vm.navigate(AppScreen.HOME) }) {
                Text("← Back", color = if (darkMode) Color.White else Color(0xFF333366), fontSize = 16.sp)
            }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🔥 💧", fontSize = 20.sp)
                Text(
                    "Select Level",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(72.dp))
        }

        Spacer(Modifier.height(4.dp))

        Text(
            "$totalLevels niveaux  •  6×6",
            color = if (darkMode) Color(0xFF8888CC) else Color(0xFF6666AA),
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(totalLevels) { idx ->
                LevelTile(
                    number = idx + 1,
                    completed = vm.isLevelCompleted(idx),
                    darkMode = darkMode,
                    onClick = { vm.startLevel(idx) }
                )
            }
        }
    }
}

@Composable
private fun LevelTile(
    number: Int,
    completed: Boolean,
    darkMode: Boolean,
    onClick: () -> Unit
) {
    // Colour cycles every 12 levels as cosmetic variety — unrelated to difficulty
    val completedBrush = when (((number - 1) / 12) % 4) {
        0    -> Brush.linearGradient(listOf(Color(0xFF00C853), Color(0xFF00E676)))
        1    -> Brush.linearGradient(listOf(WaterBlue, WaterDeep))
        2    -> Brush.linearGradient(listOf(FireOrange, FireRed))
        else -> Brush.linearGradient(listOf(Color(0xFF9C27B0), Color(0xFF6A1B9A)))
    }
    val undoneBrush = Brush.linearGradient(
        listOf(
            if (darkMode) Color(0xFF22224A) else Color(0xFFE0E4FF),
            if (darkMode) Color(0xFF2A2A5A) else Color(0xFFD0D4EE)
        )
    )

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(if (completed) completedBrush else undoneBrush)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                number.toString(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (completed || darkMode) Color.White else Color(0xFF333366)
            )
            if (completed) Text("★", fontSize = 12.sp, color = GoldStar)
        }
    }
}

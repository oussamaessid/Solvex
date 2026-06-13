package app.solvex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.solvex.model.AppScreen
import app.solvex.ui.components.GameGrid
import app.solvex.ui.theme.*
import app.solvex.viewmodel.GameViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun GameScreen(vm: GameViewModel, darkMode: Boolean) {
    val state by vm.gameState.collectAsState()
    if (state == null) return
    val gs = state!!

    // Game screen always uses white/light theme
    val textColor = Color(0xFF1A1A3E)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFF0F2FF))))
    ) {
        val gridSize = minOf(maxWidth - 32.dp, maxHeight * 0.52f, 480.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { vm.navigate(AppScreen.HOME) }) {
                    Text("← Home", color = Color(0xFF333366))
                }
                Spacer(Modifier.weight(1f))
                Text(
                    if (gs.level.isDaily) todayLabel() else "Level ${gs.level.levelNumber}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Spacer(Modifier.weight(1f))
                if (gs.wasAlreadyComplete) DoneBadge() else TimerBadge(gs.elapsedSeconds)
            }

            // Grid + labels centered in remaining space
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Rules reminder + constraint legend — centered
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            RuleChip("🔥=💧 per row/col")
                            RuleChip("No 3 in a row")
                            ConstraintLegendChip()
                        }
                    }

                    // Grid — fixed square, centered, white container
                    Box(
                        modifier = Modifier
                            .size(gridSize)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFF0F2FF))
                            .padding(10.dp)
                    ) {
                        GameGrid(
                            board = gs.board,
                            clues = gs.level.clues,
                            constraints = gs.level.constraints,
                            errorCells = gs.errorCells,
                            size = gs.level.size,
                            darkMode = false,
                            onTap = vm::tapCell
                        )
                    }
                }
            }

            if (gs.wasAlreadyComplete) {
                // Already completed — show banner + home/replay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SuccessGreen.copy(alpha = 0.12f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (gs.level.isDaily) "✅ ${todayLabel()} completed!"
                            else "✅ Level ${gs.level.levelNumber} completed!",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (gs.level.isDaily) "Come back tomorrow · ${tomorrowLabel()}"
                            else "Come back tomorrow for Level ${gs.level.levelNumber + 1}",
                            fontSize = 12.sp,
                            color = Color(0xFF666699),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActionButton(
                        icon = "🏠", label = "Home",
                        enabled = true,
                        modifier = Modifier.weight(1f),
                        onClick = { vm.navigate(AppScreen.HOME) }
                    )
                    ActionButton(
                        icon = "↺", label = "Replay",
                        enabled = true,
                        modifier = Modifier.weight(1f),
                        onClick = vm::restart,
                        tint = Color(0xFF6666AA)
                    )
                }
            } else {
                // Normal play buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActionButton(
                        icon = "↩", label = "Undo",
                        enabled = gs.history.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        onClick = vm::undo
                    )
                    ActionButton(
                        icon = "💡", label = "Hint",
                        enabled = !gs.isComplete,
                        modifier = Modifier.weight(1f),
                        onClick = vm::useHint,
                        tint = FireYellow
                    )
                    ActionButton(
                        icon = "↺", label = "Restart",
                        enabled = true,
                        modifier = Modifier.weight(1f),
                        onClick = vm::restart,
                        tint = Color(0xFF6666AA)
                    )
                }
                if (gs.hintsUsed > 0) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Hints used: ${gs.hintsUsed}",
                        fontSize = 11.sp,
                        color = FireYellow.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun todayLabel(): String =
    SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH).format(Calendar.getInstance().time)

private fun tomorrowLabel(): String =
    SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH)
        .format(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }.time)

@Composable
private fun DoneBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SuccessGreen.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            "✅ Done",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = SuccessGreen
        )
    }
}

@Composable
private fun TimerBadge(seconds: Int) {
    val m = seconds / 60; val s = seconds % 60
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFDDDDFF))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            "%02d:%02d".format(m, s),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = WaterDeep
        )
    }
}

@Composable
private fun RuleChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFE0E0FF))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, fontSize = 10.sp, color = Color(0xFF5555AA))
    }
}

@Composable
private fun ConstraintLegendChip() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFE0E0FF))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("=", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ConstraintEqual)
        Text("same", fontSize = 10.sp, color = Color(0xFF5555AA))
        Text("  ×", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ConstraintDiff)
        Text("diff", fontSize = 10.sp, color = Color(0xFF5555AA))
    }
}

@Composable
private fun ActionButton(
    icon: String,
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = Color(0xFF333366),
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFE0E4FF),
            disabledContainerColor = Color(0xFFEEEEFF)
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 18.sp, color = if (enabled) tint else Color.Gray)
            Text(label, fontSize = 9.sp, color = if (enabled) tint.copy(alpha = 0.8f) else Color.Gray)
        }
    }
}

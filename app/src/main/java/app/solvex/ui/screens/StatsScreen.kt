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
import app.solvex.ui.theme.*
import app.solvex.viewmodel.GameViewModel

@Composable
fun StatsScreen(vm: GameViewModel) {
    val stats by vm.stats.collectAsState()
    val textColor = Color(0xFF1A1A3E)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFF0F2FF))))
            .padding(horizontal = 20.dp),
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
                "Statistics",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(72.dp))
        }

        Spacer(Modifier.height(28.dp))

        // Streak cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            StatCard(
                icon = "🔥",
                value = stats.currentStreak.toString(),
                label = "Current\nStreak",
                accent = Brush.linearGradient(listOf(FireOrange, FireRed)),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                icon = "🏆",
                value = stats.maxStreak.toString(),
                label = "Best\nStreak",
                accent = Brush.linearGradient(listOf(GoldStar, Color(0xFFE65100))),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(14.dp))

        // Completion cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            StatCard(
                icon = "✅",
                value = "${stats.totalCompleted}",
                label = "Levels\nCompleted",
                accent = Brush.linearGradient(listOf(SuccessGreen, WaterDeep)),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                icon = "📊",
                value = "${stats.winPercent}%",
                label = "Win\nRate",
                accent = Brush.linearGradient(listOf(WaterBlue, WaterDeep)),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(14.dp))

        // Daily played — full width
        StatCard(
            icon = "📅",
            value = stats.dailyCount.toString(),
            label = "Daily Challenges Completed",
            accent = Brush.horizontalGradient(listOf(Color(0xFF9C27B0), Color(0xFF6A1B9A))),
            modifier = Modifier.fillMaxWidth(),
            horizontal = true
        )

        if (stats.dailyTime > 0) {
            Spacer(Modifier.height(14.dp))
            StatCard(
                icon = "⏱",
                value = "%02d:%02d".format(stats.dailyTime / 60, stats.dailyTime % 60),
                label = "Today's Completion Time",
                accent = Brush.horizontalGradient(listOf(Color(0xFF00BCD4), Color(0xFF006064))),
                modifier = Modifier.fillMaxWidth(),
                horizontal = true
            )
        }

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun StatCard(
    icon: String,
    value: String,
    label: String,
    accent: Brush,
    modifier: Modifier = Modifier,
    horizontal: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFEEF0FF))
            .padding(1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(15.dp))
                .background(Color.White)
                .padding(16.dp)
        ) {
            if (horizontal) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(icon, fontSize = 26.sp)
                    }
                    Column {
                        Text(
                            value,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1A1A3E)
                        )
                        Text(
                            label,
                            fontSize = 12.sp,
                            color = Color(0xFF6666AA)
                        )
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(icon, fontSize = 22.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        value,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1A1A3E)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        label,
                        fontSize = 11.sp,
                        color = Color(0xFF6666AA),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

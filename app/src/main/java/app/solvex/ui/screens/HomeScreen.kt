package app.solvex.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.solvex.ui.theme.*
import app.solvex.viewmodel.GameViewModel
import app.solvex.viewmodel.UserStats
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun HomeScreen(vm: GameViewModel, darkMode: Boolean) {
    val bg = if (darkMode) DarkBg else LightBg
    val textColor = if (darkMode) Color.White else Color(0xFF1A1A3E)
    val isLoading by vm.isLoading.collectAsState()
    val stats by vm.stats.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "home")
    val fireScale by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(900, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "fireScale"
    )

    val waterScale by infiniteTransition.animateFloat(
        initialValue = 1.08f, targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(900, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "waterScale"
    )

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val titleAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(800), label = "titleAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(bg, if (darkMode) Color(0xFF0A0A22) else Color(0xFFE8EAFF))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)
        ) {

            // Logo
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(titleAlpha)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔥", fontSize = 52.sp, modifier = Modifier.scale(fireScale))
                    Spacer(Modifier.width(8.dp))
                    Text("💧", fontSize = 52.sp, modifier = Modifier.scale(waterScale))
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    " Solvex ",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    color = textColor,
                    textAlign = TextAlign.Center
                )
                Text(
                    "FIRE & WATER LOGIC PUZZLE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (darkMode) Color(0xFF8888CC) else Color(0xFF6666AA),
                    letterSpacing = 3.sp
                )
            }

            // Stats section
            if (!isLoading) {
                HomeStatsSection(stats = stats, darkMode = darkMode, textColor = textColor)
            }

            // Buttons
            if (isLoading) {
                CircularProgressIndicator(
                    color = FireOrange,
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    "Chargement des niveaux...",
                    fontSize = 13.sp,
                    color = if (darkMode) Color(0xFF8888CC) else Color(0xFF6666AA)
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HomeButton(
                        text = "▶  Play",
                        brush = Brush.horizontalGradient(listOf(FireOrange, FireRed)),
                        onClick = { vm.resumePlay() }
                    )
                }
            }

        }
    }
}

@Composable
private fun HomeStatsSection(stats: UserStats, darkMode: Boolean, textColor: Color) {
    val cardBg = if (darkMode) Color(0x22FFFFFF) else Color(0xFFEEF0FF)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            QuickStat(
                icon = "🔥",
                value = stats.currentStreak.toString(),
                label = "Streak",
                accent = Brush.linearGradient(listOf(FireOrange, FireRed)),
                textColor = textColor
            )
            QuickStat(
                icon = "🏆",
                value = stats.maxStreak.toString(),
                label = "Best",
                accent = Brush.linearGradient(listOf(GoldStar, Color(0xFFE65100))),
                textColor = textColor
            )
            QuickStat(
                icon = "📅",
                value = stats.dailyCount.toString(),
                label = "Played",
                accent = Brush.linearGradient(listOf(SuccessGreen, WaterDeep)),
                textColor = textColor
            )
            QuickStat(
                icon = "📊",
                value = "${stats.winPercent}%",
                label = "Win %",
                accent = Brush.linearGradient(listOf(WaterBlue, WaterDeep)),
                textColor = textColor
            )
        }

        HorizontalDivider(
            color = if (darkMode) Color(0x33FFFFFF) else Color(0xFFCCCCEE),
            thickness = 1.dp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "📅  " + SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH)
                    .format(Calendar.getInstance().time),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor.copy(alpha = 0.75f)
            )
            if (stats.dailyTime > 0) {
                Text(
                    "⏱  %02d:%02d".format(stats.dailyTime / 60, stats.dailyTime % 60),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor.copy(alpha = 0.75f)
                )
            }
        }
    }
}

@Composable
private fun QuickStat(
    icon: String,
    value: String,
    label: String,
    accent: Brush,
    textColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accent),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 18.sp)
        }
        Text(
            value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = textColor
        )
        Text(
            label,
            fontSize = 10.sp,
            color = textColor.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun HomeButton(
    text: String,
    brush: Brush,
    textColor: Color = Color.White,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(0.85f).height(58.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(brush, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
        }
    }
}


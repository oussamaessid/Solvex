package app.solvex.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.solvex.R
import app.solvex.model.AppScreen
import app.solvex.ui.theme.*
import app.solvex.viewmodel.GameViewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.random.Random

@Composable
fun VictoryScreen(vm: GameViewModel, darkMode: Boolean) {
    val state by vm.gameState.collectAsState()
    val gs = state ?: return
    val stats by vm.stats.collectAsState()

    val textColor = Color(0xFF1A1A3E)

    var show by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { show = true }
    val contentScale by animateFloatAsState(
        if (show) 1f else 0.3f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "scale"
    )
    val contentAlpha by animateFloatAsState(
        if (show) 1f else 0f, tween(500), label = "alpha"
    )

    val stars = starsForTime(gs.elapsedSeconds, gs.level.size, gs.hintsUsed)

    LaunchedEffect(gs.level.id) {
        delay(5000L)
        vm.nextLevel()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        ConfettiCanvas()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .scale(contentScale)
                .alpha(contentAlpha)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val trophyComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.trophy))
            val trophyProgress by animateLottieCompositionAsState(
                trophyComposition,
                iterations = LottieConstants.IterateForever
            )
            LottieAnimation(
                composition = trophyComposition,
                progress = { trophyProgress },
                modifier = Modifier.size(180.dp)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                "Balance\nAchieved!",
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                color = textColor,
                textAlign = TextAlign.Center,
                lineHeight = 42.sp
            )

            if (gs.level.isDaily) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "📅 " + SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH)
                        .format(Calendar.getInstance().time),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF6666AA)
                )
            }

            Spacer(Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (i in 1..3) {
                    val starScale by animateFloatAsState(
                        if (show && i <= stars) 1f else 0f,
                        spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "star$i"
                    )
                    Text(
                        if (i <= stars) "★" else "☆",
                        fontSize = 44.sp,
                        color = if (i <= stars) GoldStar else SilverStar,
                        modifier = Modifier.scale(if (i <= stars) starScale else 1f)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Level stats
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFEEF0FF))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("⏱", formatTime(gs.elapsedSeconds), "Time", textColor)
                StatItem("💡", "${gs.hintsUsed}", "Hints", textColor)
                StatItem("★", "$stars / 3", "Stars", textColor)
            }

            Spacer(Modifier.height(12.dp))

            // Streak badge — shown only for daily
            if (gs.level.isDaily && stats.currentStreak > 0) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.horizontalGradient(listOf(FireOrange, FireRed)))
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🔥", fontSize = 22.sp)
                    Text(
                        "${stats.currentStreak} day streak!",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { vm.nextLevel() },
                modifier = Modifier.fillMaxWidth(0.85f).height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(listOf(FireOrange, FireRed)),
                            RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Next Level →",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            TextButton(onClick = { vm.navigate(AppScreen.HOME) }) {
                Text(
                    "← Home",
                    color = Color(0xFF6666AA),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun StatItem(icon: String, value: String, label: String, textColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 22.sp)
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
        Text(label, fontSize = 11.sp, color = textColor.copy(alpha = 0.6f))
    }
}

@Composable
private fun ConfettiCanvas() {
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val time by infiniteTransition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "confettiTime"
    )

    val confettiColors = listOf(
        FireOrange, FireYellow, WaterBlue, WaterCyan, SuccessGreen, GoldStar, Color(0xFFFF69B4)
    )
    val particles = remember {
        (0 until 60).map {
            Particle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                speed = 0.1f + Random.nextFloat() * 0.3f,
                size = 4f + Random.nextFloat() * 8f,
                angle = Random.nextFloat() * 360f,
                colorIdx = Random.nextInt(confettiColors.size)
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        for (p in particles) {
            val y = ((p.y + time * p.speed) % 1f) * size.height
            val x = p.x * size.width
            rotate(p.angle + time * 180f, pivot = Offset(x, y)) {
                drawRect(
                    color = confettiColors[p.colorIdx].copy(alpha = 0.8f),
                    topLeft = Offset(x - p.size / 2, y - p.size / 4),
                    size = androidx.compose.ui.geometry.Size(p.size, p.size / 2)
                )
            }
        }
    }
}

private data class Particle(
    val x: Float, val y: Float, val speed: Float,
    val size: Float, val angle: Float, val colorIdx: Int
)

private fun starsForTime(seconds: Int, gridSize: Int, hints: Int): Int {
    val base = when (gridSize) { 4 -> 60; 6 -> 120; else -> 240 }
    val score = seconds + hints * 30
    return when { score < base -> 3; score < base * 2 -> 2; else -> 1 }
}

private fun formatTime(s: Int) = "%02d:%02d".format(s / 60, s % 60)

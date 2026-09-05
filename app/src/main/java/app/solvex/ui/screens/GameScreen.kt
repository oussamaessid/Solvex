package app.solvex.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.solvex.R
import app.solvex.ads.AdManager
import app.solvex.model.AppScreen
import app.solvex.model.MAX_LIVES
import app.solvex.ui.components.BannerAd
import app.solvex.ui.components.GameGrid
import app.solvex.ui.theme.*
import app.solvex.viewmodel.GameViewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun GameScreen(vm: GameViewModel, darkMode: Boolean) {
    val state by vm.gameState.collectAsState()
    val showBrokenHeart by vm.showBrokenHeart.collectAsState()
    val showTutorial by vm.showTutorial.collectAsState()
    BackHandler(enabled = showTutorial) { }
    if (state == null) return
    val gs = state!!

    // Game screen always uses white/light theme
    val textColor = Color(0xFF1A1A3E)
    val context = LocalContext.current
    val activity = context as? Activity

    var nowTick by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(gs.lives, gs.nextLifeAtMillis) {
        while (gs.lives < MAX_LIVES) {
            kotlinx.coroutines.delay(1000L)
            nowTick = System.currentTimeMillis()
        }
    }

    var showLivesDialog by remember { mutableStateOf(false) }
    var showHintDialog by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        val gridSize = minOf(maxWidth - 32.dp, maxHeight * 0.52f, 480.dp)

        Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { vm.navigate(AppScreen.HOME) }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF333366)
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "Level ${gs.level.levelNumber}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Spacer(Modifier.weight(1f))
                if (gs.wasAlreadyComplete) DoneBadge() else TimerBadge(gs.elapsedSeconds)
            }

            LivesRow(lives = gs.lives, onClick = { showLivesDialog = true })

            // Rules stay visible directly below the lives.
            GameRuleCards()
            Spacer(Modifier.height(10.dp))

            // Rule chips, grid and action buttons — centered as one group
            // in the remaining vertical space, so the grid sits in the
            // middle of the screen with the buttons snug right below it.
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
                            onDoubleTap = vm::doubleTapCell,
                            onTap = vm::tapCell
                        )

                        if (gs.lives <= 0 && !gs.isComplete) {
                            OutOfLivesOverlay(
                                nextLifeAtMillis = gs.nextLifeAtMillis,
                                now = nowTick,
                                onWatchAd = {
                                    activity?.let {
                                        AdManager.showRewarded(it, onReward = vm::addLife)
                                    }
                                }
                            )
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
                            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                        ) {
                            ActionButton(
                                icon = "💡", label = "Hint",
                                enabled = !gs.isComplete && gs.lives > 0,
                                modifier = Modifier.width(90.dp),
                                onClick = { showHintDialog = true },
                                tint = FireYellow,
                                badgeCount = gs.hints
                            )
                            ActionButton(
                                icon = "↺", label = "Restart",
                                enabled = true,
                                modifier = Modifier.width(90.dp),
                                onClick = vm::restart,
                                tint = Color(0xFF6666AA)
                            )
                        }
                    }
                }
            }
        }
        BannerAd(modifier = Modifier.padding(bottom = 16.dp))
        }

        if (showLivesDialog) {
            LivesDialog(
                lives = gs.lives,
                nextLifeAtMillis = gs.nextLifeAtMillis,
                now = nowTick,
                onWatchAd = {
                    activity?.let { AdManager.showRewarded(it, onReward = vm::addLife) }
                },
                onDismiss = { showLivesDialog = false }
            )
        }

        if (showBrokenHeart) {
            BrokenHeartOverlay(onFinished = vm::onErrorAnimationEnd)
        }

        if (showHintDialog) {
            HintDialog(
                hints = gs.hints,
                onUseHint = {
                    vm.useHint()
                    showHintDialog = false
                },
                onWatchAd = {
                    activity?.let { AdManager.showRewarded(it, onReward = vm::addHint) }
                    showHintDialog = false
                },
                onDismiss = { showHintDialog = false }
            )
        }

        if (showTutorial) {
            GameTutorialOverlay(onFinish = vm::finishTutorial)
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
private fun LivesRow(lives: Int, onClick: () -> Unit) {
    val scope = rememberCoroutineScope()
    val scale = remember { androidx.compose.animation.core.Animatable(1f) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .padding(top = 2.dp, bottom = 14.dp)
            .scale(scale.value)
            .clickable {
                scope.launch {
                    scale.animateTo(0.8f, tween(90))
                    scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                }
                onClick()
            }
    ) {
        repeat(MAX_LIVES) { i ->
            Text(if (i < lives) "❤️" else "🖤", fontSize = 14.sp)
        }
    }
}

@Composable
private fun LivesDialog(
    lives: Int,
    nextLifeAtMillis: Long,
    now: Long,
    onWatchAd: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(MAX_LIVES) { i -> Text(if (i < lives) "❤️" else "🖤", fontSize = 24.sp) }
            }
            if (lives < MAX_LIVES && nextLifeAtMillis > 0L) {
                val remaining = (nextLifeAtMillis - now).coerceAtLeast(0L) / 1000L
                val h = remaining / 3600
                val m = (remaining / 60) % 60
                val s = remaining % 60
                Text(
                    "Next life in %02d:%02d:%02d".format(h, m, s),
                    fontSize = 13.sp,
                    color = Color(0xFF666699)
                )
                DialogOptionCard(
                    icon = "📺",
                    title = "Watch a video",
                    subtitle = "Get +1 life now",
                    gradient = Brush.horizontalGradient(listOf(FireOrange, FireRed)),
                    onClick = { onWatchAd(); onDismiss() }
                )
            } else {
                Text("Lives are full", fontSize = 14.sp, color = Color(0xFF666699))
            }
            TextButton(onClick = onDismiss) { Text("Close", color = Color(0xFF6666AA)) }
        }
    }
}

@Composable
private fun HintDialog(
    hints: Int,
    onUseHint: () -> Unit,
    onWatchAd: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("💡", fontSize = 36.sp)
            Text(
                "Need a hint?",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A3E)
            )

            DialogOptionCard(
                icon = "💡",
                title = "Use a hint",
                subtitle = "$hints available",
                gradient = Brush.horizontalGradient(listOf(FireYellow, FireOrange)),
                enabled = hints > 0,
                onClick = { onUseHint() }
            )
            DialogOptionCard(
                icon = "📺",
                title = "Watch a video",
                subtitle = "Get +1 hint",
                gradient = Brush.horizontalGradient(listOf(WaterBlue, WaterDeep)),
                onClick = { onWatchAd() }
            )
            DialogOptionCard(
                icon = "✖",
                title = "Cancel",
                subtitle = null,
                gradient = Brush.horizontalGradient(listOf(Color(0xFFE4E4EE), Color(0xFFD4D4E4))),
                textColor = Color(0xFF333366),
                onClick = onDismiss
            )
        }
    }
}

@Composable
private fun DialogOptionCard(
    icon: String,
    title: String,
    subtitle: String?,
    gradient: Brush,
    enabled: Boolean = true,
    textColor: Color = Color.White,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (enabled) gradient
                else Brush.horizontalGradient(listOf(Color(0xFFE0E0E0), Color(0xFFD0D0D0)))
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(icon, fontSize = 22.sp)
        Column {
            Text(
                title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (enabled) textColor else Color.Gray
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    fontSize = 12.sp,
                    color = (if (enabled) textColor else Color.Gray).copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun BrokenHeartOverlay(onFinished: () -> Unit) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.brokenheart))
    val progress by animateLottieCompositionAsState(composition, iterations = 1)

    LaunchedEffect(composition, progress) {
        if (composition != null && progress >= 1f) onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x66000000)),
        contentAlignment = Alignment.Center
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(220.dp)
        )
    }
}

@Composable
private fun OutOfLivesOverlay(nextLifeAtMillis: Long, now: Long, onWatchAd: () -> Unit) {
    val remaining = (nextLifeAtMillis - now).coerceAtLeast(0L) / 1000L
    val h = remaining / 3600
    val m = (remaining / 60) % 60
    val s = remaining % 60
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xCC1A1A3E)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("🖤", fontSize = 40.sp)
            Text(
                "Out of lives",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                "Next life in %02d:%02d:%02d".format(h, m, s),
                fontSize = 13.sp,
                color = Color(0xFFCCCCEE)
            )
            Button(
                onClick = onWatchAd,
                colors = ButtonDefaults.buttonColors(containerColor = FireOrange)
            ) {
                Text("📺 Watch ad for a life", color = Color.White)
            }
        }
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
private fun ElementBalanceChip() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFE0E0FF))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.mascot_fire),
            contentDescription = "Fire mascot",
            modifier = Modifier.size(16.dp)
        )
        Text("=", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5555AA))
        Image(
            painter = painterResource(R.drawable.mascot_water),
            contentDescription = "Water mascot",
            modifier = Modifier.size(16.dp)
        )
        Text("row/col", fontSize = 10.sp, color = Color(0xFF5555AA))
    }
}

@Composable
private fun GameRuleCards() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        RuleCard(
            modifier = Modifier.weight(1f),
            title = "BALANCE",
            visual = "🔥 = 💧",
            subtitle = "each line"
        )
        RuleCard(
            modifier = Modifier.weight(1f),
            title = "NO TRIPLE",
            visual = "🔥🔥 ≠ 🔥",
            subtitle = "row or column"
        )
        LinkRuleCard(
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun RuleCard(modifier: Modifier, title: String, visual: String, subtitle: String) {
    Column(
        modifier = modifier
            .height(92.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFE8EAFF))))
            .padding(horizontal = 4.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color(0xFF6666AA), letterSpacing = 0.6.sp)
        Text(visual, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A3E), maxLines = 1)
        Text(subtitle, fontSize = 8.sp, color = Color(0xFF777799), maxLines = 1)
    }
}

@Composable
private fun LinkRuleCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .height(92.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFE8EAFF))))
            .padding(horizontal = 3.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("LINKS", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color(0xFF6666AA), letterSpacing = .5.sp)
        Row(Modifier.fillMaxWidth()) {
            LinkIconRow(R.drawable.game_mascot_fire, "=", R.drawable.game_mascot_fire, Modifier.weight(1f))
            LinkIconRow(R.drawable.game_mascot_water, "=", R.drawable.game_mascot_water, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth()) {
            LinkIconRow(R.drawable.game_mascot_fire, "×", R.drawable.game_mascot_water, Modifier.weight(1f))
            LinkIconRow(R.drawable.game_mascot_water, "×", R.drawable.game_mascot_fire, Modifier.weight(1f))
        }
    }
}

@Composable
private fun LinkIconRow(left: Int, sign: String, right: Int, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Image(painterResource(left), null, Modifier.size(14.dp))
        Text(sign, fontSize = 10.sp, fontWeight = FontWeight.Black, color = if (sign == "=") ConstraintEqual else ConstraintDiff)
        Image(painterResource(right), null, Modifier.size(14.dp))
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
    badgeCount: Int? = null,
    onClick: () -> Unit
) {
    Box(modifier = modifier) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE0E4FF),
                disabledContainerColor = Color(0xFFEEEEFF)
            )
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(icon, fontSize = 16.sp, color = if (enabled) tint else Color.Gray)
                Text(
                    label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) tint else Color.Gray
                )
            }
        }
        if (badgeCount != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 4.dp, y = (-4).dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(if (enabled) tint else Color.Gray),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    badgeCount.toString(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 10.sp,
                    style = androidx.compose.ui.text.TextStyle(
                        platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
        }
    }
}

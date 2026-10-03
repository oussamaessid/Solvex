package app.solvex.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
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

    val textColor = Color(0xFF1A1A3E)
    val context = LocalContext.current
    val activity = context as? Activity
    val coins by vm.coins.collectAsState()

    var showLivesDialog by remember { mutableStateOf(false) }
    var showHintDialog by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFFFF8EE), Color(0xFFF1F0FF), Color.White)))
    ) {
        // ambient glows
        Box(Modifier.offset(x = (-80).dp, y = (-50).dp).size(220.dp).alpha(0.30f).background(Brush.radialGradient(listOf(Color(0xFFFFB800), Color.Transparent)), CircleShape))
        Box(Modifier.align(Alignment.TopEnd).offset(x = 80.dp, y = 60.dp).size(220.dp).alpha(0.26f).background(Brush.radialGradient(listOf(Color(0xFF0AB4FF), Color.Transparent)), CircleShape))

        // Small phones: hide the rule cards and tighten spacing so the board stays big
        val compact = maxHeight < 680.dp
        val showRules = maxHeight >= 600.dp

        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Creative top bar ──
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(42.dp).shadow(6.dp, CircleShape).clip(CircleShape)
                        .background(Color.White).clickable { vm.navigate(AppScreen.HOME) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("←", fontSize = 18.sp, fontWeight = FontWeight.Black, color = textColor)
                }
                Spacer(Modifier.width(10.dp))
                // Level pill
                Box(
                    Modifier.shadow(6.dp, RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFF1A1A3E), Color(0xFF3D3D8F))))
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (gs.level.isDaily) "⚡" else "🔥", fontSize = 13.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (gs.level.isDaily) "Level ${gs.level.levelNumber}" else "Level ${gs.level.levelNumber}",
                            fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                CoinBadge(coins = coins, onClick = { vm.openShop(AppScreen.GAME) })
            }

            // ── Status strip: lives + timer + hints ──
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LivesPill(lives = gs.lives, onClick = { showLivesDialog = true }, modifier = Modifier.weight(1f))
                TimerPill(seconds = gs.elapsedSeconds, modifier = Modifier.weight(1f))
                HintPill(count = gs.hints, onClick = { showHintDialog = true }, enabled = !gs.isComplete && gs.lives > 0, modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(if (compact) 8.dp else 10.dp))
            if (showRules) {
                GameRuleCards()
                Spacer(Modifier.height(if (compact) 8.dp else 10.dp))
            }

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // Board fills what is left after the action buttons below it
                val reserved = if (gs.wasAlreadyComplete) 170.dp else 76.dp
                val gridSize = minOf(maxWidth, maxHeight - reserved, 560.dp).coerceAtLeast(160.dp)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ── Board in gradient frame ──
                    Box(
                        modifier = Modifier
                            .size(gridSize)
                            .shadow(20.dp, RoundedCornerShape(26.dp))
                            .clip(RoundedCornerShape(26.dp))
                            .background(Brush.linearGradient(listOf(FireOrange, Color(0xFF8B5CF6), WaterBlue)))
                            .padding(2.5.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White)
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
                                onWatchAd = {
                                    activity?.let {
                                        AdManager.showRewarded(it, onReward = vm::addLife)
                                    }
                                }
                            )
                        }
                    }

                    if (gs.wasAlreadyComplete) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(8.dp, RoundedCornerShape(18.dp))
                                .clip(RoundedCornerShape(18.dp))
                                .background(Brush.horizontalGradient(listOf(Color(0xFFE8FFF1), Color(0xFFD6F5FF))))
                                .border(1.dp, SuccessGreen.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                                .padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    if (gs.level.isDaily) "✅ ${todayLabel()} completed!"
                                    else "✅ Level ${gs.level.levelNumber} completed!",
                                    fontSize = 15.sp, fontWeight = FontWeight.Black,
                                    color = Color(0xFF0E6B3E), textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    if (gs.level.isDaily) "Come back tomorrow · ${tomorrowLabel()}"
                                    else "Come back tomorrow for Level ${gs.level.levelNumber + 1}",
                                    fontSize = 12.sp, color = Color(0xFF5B7A6B), textAlign = TextAlign.Center
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GameActionButton(
                                icon = "🏠", label = "Home", enabled = true,
                                modifier = Modifier.weight(1f), primary = false,
                                onClick = { vm.navigate(AppScreen.HOME) }
                            )
                            GameActionButton(
                                icon = "↺", label = "Replay", enabled = true,
                                modifier = Modifier.weight(1f), primary = false,
                                onClick = vm::restart
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                        ) {
                            GameActionButton(
                                icon = "💡", label = "Hint",
                                enabled = !gs.isComplete && gs.lives > 0,
                                modifier = Modifier.weight(1f), primary = true,
                                badgeCount = gs.hints,
                                onClick = { showHintDialog = true }
                            )
                            GameActionButton(
                                icon = "↩", label = "Undo",
                                enabled = gs.history.isNotEmpty(),
                                modifier = Modifier.weight(1f), primary = false,
                                onClick = vm::undo
                            )
                            GameActionButton(
                                icon = "↺", label = "Restart",
                                enabled = true,
                                modifier = Modifier.weight(1f), primary = false,
                                onClick = vm::restart
                            )
                        }
                    }
                }
            }
        }
        BannerAd(modifier = Modifier.padding(top = if (compact) 8.dp else 16.dp, bottom = if (compact) 6.dp else 12.dp))
        }

        if (showLivesDialog) {
            LivesDialog(
                lives = gs.lives,
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
                onOpenShop = {
                    showHintDialog = false
                    vm.openShop(AppScreen.GAME)
                },
                onDismiss = { showHintDialog = false }
            )
        }

        if (showTutorial) {
            GameTutorialOverlay(
                onFinish = vm::finishTutorial,
                onClose = {
                    vm.finishTutorial()
                    vm.navigate(AppScreen.HOME)
                }
            )
        }
    }
}

private fun todayLabel(): String =
    SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH).format(Calendar.getInstance().time)

private fun tomorrowLabel(): String =
    SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH)
        .format(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }.time)

@Composable
private fun CoinBadge(coins: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .shadow(6.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFFFF3CC), Color(0xFFFFE08A))))
            .border(1.dp, Color(0xFFE8B800).copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text("🪙", fontSize = 15.sp)
        Text("$coins", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color(0xFF8A5A00))
        Box(Modifier.size(18.dp).clip(CircleShape).background(FireOrange), contentAlignment = Alignment.Center) {
            Text("＋", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White)
        }
    }
}

@Composable
private fun LivesPill(lives: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val scale = remember { androidx.compose.animation.core.Animatable(1f) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .shadow(5.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .clickable {
                scope.launch {
                    scale.animateTo(0.9f, tween(90))
                    scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                }
                onClick()
            }
            .padding(horizontal = 10.dp, vertical = 9.dp)
            .scale(scale.value)
    ) {
        repeat(MAX_LIVES) { i ->
            Text(if (i < lives) "❤️" else "🖤", fontSize = 13.sp)
        }
        Spacer(Modifier.width(6.dp))
        Text("$lives/$MAX_LIVES", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF55557A))
    }
}

@Composable
private fun TimerPill(seconds: Int, modifier: Modifier = Modifier) {
    val m = seconds / 60; val s = seconds % 60
    Row(
        modifier = modifier
            .shadow(5.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1A1A3E))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text("⏱", fontSize = 13.sp)
        Spacer(Modifier.width(6.dp))
        Text("%02d:%02d".format(m, s), fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
    }
}

@Composable
private fun HintPill(count: Int, onClick: () -> Unit, enabled: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .shadow(5.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (enabled) Brush.horizontalGradient(listOf(FireYellow, FireOrange))
                else Brush.horizontalGradient(listOf(Color(0xFFE4E4EE), Color(0xFFD4D4E4)))
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text("💡", fontSize = 13.sp)
        Spacer(Modifier.width(6.dp))
        Text("$count hints", fontSize = 12.sp, fontWeight = FontWeight.Black, color = if (enabled) Color.White else Color.Gray)
    }
}

@Composable
private fun LivesDialog(
    lives: Int,
    onWatchAd: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(Modifier.size(64.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFFFFD6D6), Color(0xFFFF8A8A)))), contentAlignment = Alignment.Center) {
                Text("❤️", fontSize = 30.sp)
            }
            Text("Your lives", fontSize = 19.sp, fontWeight = FontWeight.Black, color = Color(0xFF1A1A3E))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(MAX_LIVES) { i -> Text(if (i < lives) "❤️" else "🖤", fontSize = 24.sp) }
            }
            if (lives < MAX_LIVES) {
                Text("3 lives are reserved for each level", fontSize = 13.sp, color = Color(0xFF666699), textAlign = TextAlign.Center)
                DialogOptionCard(
                    icon = "📺", title = "Watch a video", subtitle = "Get +1 life now",
                    gradient = Brush.horizontalGradient(listOf(FireOrange, FireRed)),
                    onClick = { onWatchAd(); onDismiss() }
                )
            } else {
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFFE8FFF1)).padding(12.dp), contentAlignment = Alignment.Center) {
                    Text("✨ Lives are full — go shine!", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0E6B3E))
                }
            }
            TextButton(onClick = onDismiss) { Text("Close", color = Color(0xFF6666AA), fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun HintDialog(
    hints: Int,
    onUseHint: () -> Unit,
    onWatchAd: () -> Unit,
    onOpenShop: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(Modifier.size(64.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFFFFF2B8), FireOrange))), contentAlignment = Alignment.Center) {
                Text("💡", fontSize = 30.sp)
            }
            Text("Need a spark?", fontSize = 19.sp, fontWeight = FontWeight.Black, color = Color(0xFF1A1A3E))
            Text("A hint reveals one correct cell instantly", fontSize = 12.sp, color = Color(0xFF777799), textAlign = TextAlign.Center)

            DialogOptionCard(
                icon = "💡", title = "Use a hint", subtitle = "$hints available",
                gradient = Brush.horizontalGradient(listOf(FireYellow, FireOrange)),
                enabled = hints > 0, onClick = { onUseHint() }
            )
            DialogOptionCard(
                icon = "🛍️", title = "Open hint shop", subtitle = "Packs from 30 coins",
                gradient = Brush.horizontalGradient(listOf(Color(0xFF8B5CF6), Color(0xFF5B5BEA))),
                onClick = onOpenShop
            )
            DialogOptionCard(
                icon = "📺", title = "Watch a video", subtitle = "Get +1 hint free",
                gradient = Brush.horizontalGradient(listOf(WaterBlue, WaterDeep)),
                onClick = { onWatchAd() }
            )
            DialogOptionCard(
                icon = "✖", title = "Keep solving", subtitle = null,
                gradient = Brush.horizontalGradient(listOf(Color(0xFFF1F1F7), Color(0xFFE4E4EE))),
                textColor = Color(0xFF333366), onClick = onDismiss
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
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (enabled) gradient
                else Brush.horizontalGradient(listOf(Color(0xFFE0E0E0), Color(0xFFD0D0D0)))
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.28f)), contentAlignment = Alignment.Center) {
            Text(icon, fontSize = 19.sp)
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Black, color = if (enabled) textColor else Color.Gray)
            if (subtitle != null) {
                Text(subtitle, fontSize = 12.sp, color = (if (enabled) textColor else Color.Gray).copy(alpha = 0.85f))
            }
        }
        Text("›", fontSize = 20.sp, fontWeight = FontWeight.Black, color = (if (enabled) textColor else Color.Gray).copy(alpha = 0.7f))
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
            .background(Color(0x661A1A3E)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.shadow(20.dp, RoundedCornerShape(28.dp)).clip(RoundedCornerShape(28.dp))
                .background(Color.White).padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LottieAnimation(composition = composition, progress = { progress }, modifier = Modifier.size(170.dp))
                Text("Oops — try again!", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color(0xFF1A1A3E))
                Text("The wrong spark fades away", fontSize = 12.sp, color = Color(0xFF777799))
            }
        }
    }
}

@Composable
private fun OutOfLivesOverlay(onWatchAd: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(listOf(Color(0xF21A1A3E), Color(0xCC3B2E7A)))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(20.dp)
        ) {
            Box(Modifier.size(64.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Text("🖤", fontSize = 32.sp)
            }
            Text("Out of lives", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text("Watch a video to keep your flow", fontSize = 13.sp, color = Color(0xFFCCCCEE), textAlign = TextAlign.Center)
            Button(
                onClick = onWatchAd,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FireOrange)
            ) {
                Text("📺  +1 life", color = Color.White, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun GameRuleCards() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RuleCard(
            modifier = Modifier.weight(1f),
            dot = FireOrange, title = "BALANCE", visual = "🔥 = 💧", subtitle = "each line"
        )
        RuleCard(
            modifier = Modifier.weight(1f),
            dot = FireRed, title = "NO TRIPLE", visual = "🔥🔥 ≠ 🔥", subtitle = "row or col"
        )
        LinkRuleCard(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun RuleCard(modifier: Modifier, dot: Color, title: String, visual: String, subtitle: String) {
    Column(
        modifier = modifier
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(horizontal = 6.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(dot))
            Spacer(Modifier.width(4.dp))
            Text(title, fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color(0xFF6666AA), letterSpacing = 0.6.sp)
        }
        Spacer(Modifier.height(3.dp))
        Text(visual, fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF1A1A3E), maxLines = 1)
        Text(subtitle, fontSize = 9.sp, color = Color(0xFF777799), maxLines = 1)
    }
}

@Composable
private fun LinkRuleCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(SuccessGreen))
            Spacer(Modifier.width(4.dp))
            Text("LINKS", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color(0xFF6666AA), letterSpacing = .5.sp)
        }
        Spacer(Modifier.height(2.dp))
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
        Image(painterResource(left), null, Modifier.size(15.dp))
        Text(sign, fontSize = 10.sp, fontWeight = FontWeight.Black, color = if (sign == "=") ConstraintEqual else ConstraintDiff)
        Image(painterResource(right), null, Modifier.size(15.dp))
    }
}

@Composable
private fun GameActionButton(
    icon: String,
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    badgeCount: Int? = null,
    onClick: () -> Unit
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .shadow(if (primary) 10.dp else 5.dp, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
                .background(
                    when {
                        !enabled -> Brush.linearGradient(listOf(Color(0xFFEEEEF5), Color(0xFFE2E2EC)))
                        primary -> Brush.horizontalGradient(listOf(FireOrange, FireRed))
                        else -> Brush.linearGradient(listOf(Color.White, Color(0xFFF1F1FF)))
                    }
                )
                .then(
                    if (!primary && enabled) Modifier.border(1.2.dp, Color(0xFFE0E4FF), RoundedCornerShape(18.dp))
                    else Modifier
                )
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(icon, fontSize = 19.sp)
                Text(
                    label.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp,
                    color = when {
                        !enabled -> Color.Gray
                        primary -> Color.White
                        else -> Color(0xFF333366)
                    }
                )
            }
        }
        if (badgeCount != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = (-7).dp)
                    .shadow(4.dp, CircleShape)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A1A3E))
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    badgeCount.toString(), fontSize = 11.sp, fontWeight = FontWeight.Black,
                    color = Color.White, textAlign = TextAlign.Center, lineHeight = 11.sp,
                    style = androidx.compose.ui.text.TextStyle(
                        platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
        }
    }
}

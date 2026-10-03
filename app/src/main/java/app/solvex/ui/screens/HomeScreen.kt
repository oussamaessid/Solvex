package app.solvex.ui.screens

import android.app.Activity
import android.widget.ImageView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.viewinterop.AndroidView
import app.solvex.R
import app.solvex.ads.AdManager
import app.solvex.model.AppScreen
import app.solvex.ui.theme.*
import app.solvex.viewmodel.GameViewModel
import com.bumptech.glide.Glide

@Composable
fun HomeScreen(vm: GameViewModel, darkMode: Boolean) {
    val isLoading by vm.isLoading.collectAsState()
    val coins by vm.coins.collectAsState()
    val hints by vm.hints.collectAsState()
    val stats by vm.stats.collectAsState()
    val activity = LocalContext.current as? Activity

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val fade by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(700), label = "homeFade"
    )

    // Gentle floating for mascots + pulsing play button
    val float = rememberInfiniteTransition(label = "homeFloat")
    val bobA by float.animateFloat(0f, 10f, infiniteRepeatable(tween(1600, easing = EaseInOutSine), RepeatMode.Reverse), label = "bobA")
    val bobB by float.animateFloat(0f, -9f, infiniteRepeatable(tween(1900, easing = EaseInOutSine), RepeatMode.Reverse), label = "bobB")
    val playPulse by float.animateFloat(1f, 1.035f, infiniteRepeatable(tween(900, easing = EaseInOutSine), RepeatMode.Reverse), label = "pulse")
    val sparkAlpha by float.animateFloat(0.45f, 1f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "spark")

    val currentLevel = (stats.currentLevel).coerceAtLeast(1)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFFFF8EE), Color(0xFFF2F0FF), Color.White)))
    ) {
        // Ambient glows
        Box(Modifier.offset(x = (-70).dp, y = (-60).dp).size(230.dp).alpha(0.35f).background(Brush.radialGradient(listOf(Color(0xFFFFB800), Color.Transparent)), CircleShape))
        Box(Modifier.align(Alignment.TopEnd).offset(x = 80.dp, y = 90.dp).size(250.dp).alpha(0.30f).background(Brush.radialGradient(listOf(Color(0xFF0AB4FF), Color.Transparent)), CircleShape))
        Box(Modifier.align(Alignment.BottomStart).offset(x = (-90).dp, y = 90.dp).size(260.dp).alpha(0.22f).background(Brush.radialGradient(listOf(Color(0xFF8B5CF6), Color.Transparent)), CircleShape))

        BoxWithConstraints(Modifier.fillMaxSize()) {
        // Adapt spacing to the phone height so everything fits without scrolling
        val compact = maxHeight < 700.dp
        val gap = if (compact) 10.dp else 16.dp

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 560.dp)
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = if (compact) 10.dp else 16.dp)
                .alpha(fade),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(gap)
        ) {
            // ── Wallet top bar ──
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(34.dp).clip(CircleShape)
                            .background(Brush.linearGradient(listOf(FireOrange, FireRed))),
                        contentAlignment = Alignment.Center
                    ) { Text("⚡", fontSize = 17.sp) }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("SOLVEX", fontSize = 15.sp, fontWeight = FontWeight.Black, color = InkNavy, letterSpacing = 1.2.sp)
                        Text("SPARK · FLOW · SOLVE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = InkFaint, letterSpacing = 1.6.sp)
                    }
                }
                Spacer(Modifier.weight(1f))
                WalletPill(text = "🪙 $coins", bg = GoldSoft, fg = GoldDeep) { vm.openShop(AppScreen.HOME) }
                Spacer(Modifier.width(8.dp))
                WalletPill(text = "💡 $hints", bg = Color(0xFFECEAFF), fg = Color(0xFF3D3D8F)) { vm.openShop(AppScreen.HOME) }
            }

            // ── Hero showdown card: takes all the free height ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .shadow(18.dp, RoundedCornerShape(28.dp))
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White)
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val heroH = maxHeight
                    val mascot = minOf(heroH * 0.28f, maxWidth * 0.30f, 140.dp).coerceAtLeast(52.dp)
                    val showGif = heroH > 280.dp
                    val gifSize = minOf(heroH * 0.30f, maxWidth * 0.5f, 190.dp)
                    val titleSize = if (heroH < 240.dp) 20.sp else if (heroH > 420.dp) 28.sp else 24.sp

                    Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Mascot duel
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                Modifier.offset(y = bobA.dp).size(mascot).clip(CircleShape)
                                    .background(Brush.radialGradient(listOf(Color(0xFFFFF2D8), Color(0xFFFFD29A)))),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(painterResource(R.drawable.game_mascot_fire), "Fire", Modifier.size(mascot * 0.76f))
                            }
                            Spacer(Modifier.width(18.dp))
                            Box(
                                modifier = Modifier
                                    .shadow(8.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(InkNavy)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                            ) { Text("VS", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White) }
                            Spacer(Modifier.width(18.dp))
                            Box(
                                Modifier.offset(y = bobB.dp).size(mascot).clip(CircleShape)
                                    .background(Brush.radialGradient(listOf(Color(0xFFE3F7FF), Color(0xFFBFE6FF)))),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(painterResource(R.drawable.game_mascot_water), "Water", Modifier.size(mascot * 0.76f))
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Balance the elements",
                                fontSize = titleSize, fontWeight = FontWeight.Black, color = InkNavy,
                                textAlign = TextAlign.Center, lineHeight = titleSize * 1.15f
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Equal fire & water · no triples · solve the links",
                                fontSize = 12.sp, color = InkSoft, textAlign = TextAlign.Center
                            )
                        }
                        if (showGif) {
                            // Blinking logo strip
                            AndroidView(
                                modifier = Modifier.size(gifSize),
                                factory = { context ->
                                    ImageView(context).apply {
                                        scaleType = ImageView.ScaleType.FIT_CENTER
                                        alpha = 0.95f
                                        Glide.with(context).asGif().load(R.raw.solvex_blink).into(this)
                                    }
                                }
                            )
                        }
                    }
                }
                // sparkles
                Text("✦", color = FireYellow.copy(alpha = sparkAlpha), fontSize = 18.sp, modifier = Modifier.align(Alignment.TopEnd))
                Text("✦", color = WaterBlue.copy(alpha = sparkAlpha), fontSize = 13.sp, modifier = Modifier.align(Alignment.TopStart))
            }

            // ── Level card ──
            Box(
                Modifier.fillMaxWidth()
                    .shadow(10.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.horizontalGradient(listOf(InkNavy, Color(0xFF33336B))))
                    .padding(if (compact) 12.dp else 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(if (compact) 38.dp else 46.dp).clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(listOf(FireYellow, FireOrange))),
                        contentAlignment = Alignment.Center
                    ) { Text("🔥", fontSize = if (compact) 20.sp else 24.sp) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("LEVEL $currentLevel", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }
            }

            // ── Play ──
            if (isLoading) {
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.White).padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = FireOrange, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Chargement des niveaux…", fontSize = 12.sp, color = InkSoft)
                    }
                }
            } else {
                Button(
                    onClick = {
                        val act = activity
                        if (act != null) AdManager.showInterstitial(act) { vm.resumePlay() }
                        else vm.resumePlay()
                    },
                    modifier = Modifier.fillMaxWidth().height(if (compact) 58.dp else 66.dp).scale(playPulse).shadow(16.dp, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(FireOrange, FireRed)), RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(34.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
                                Text("▶", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Black)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(horizontalAlignment = Alignment.Start) {
                                Text("PLAY  ·  LEVEL $currentLevel", fontSize = 17.sp, fontWeight = FontWeight.Black, color = Color.White)
                                Text("Tap to continue your flow", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
                            }
                        }
                    }
                }
            }

            // ── Secondary actions ──
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HomeMenuCard(
                    modifier = Modifier.weight(1f),
                    emoji = "🛍️", iconRes = R.drawable.shop, title = "Shop", subtitle = "Hints & coins",
                    gradient = Brush.linearGradient(listOf(Color.White, Color(0xFFF3F0FF))),
                    onClick = { vm.openShop(AppScreen.HOME) }
                )
                HomeMenuCard(
                    modifier = Modifier.weight(1f),
                    emoji = "🎓", title = "How to play", subtitle = "60-sec tutorial",
                    gradient = Brush.linearGradient(listOf(Color.White, Color(0xFFFFF3DC))),
                    onClick = { vm.openTutorial(); vm.resumePlay() }
                )
            }
        }
        }
    }
}

@Composable
private fun WalletPill(text: String, bg: Color, fg: Color, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(14.dp)).background(bg).clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 8.dp)
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Black, color = fg)
    }
}

@Composable
private fun HomeMenuCard(
    modifier: Modifier = Modifier,
    emoji: String,
    iconRes: Int? = null,
    title: String,
    subtitle: String,
    gradient: Brush,
    onClick: () -> Unit
) {
    Box(
        modifier
            .fillMaxHeight()
            .shadow(10.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(gradient)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(13.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                if (iconRes != null) Image(painterResource(iconRes), title, Modifier.size(34.dp))
                else Text(emoji, fontSize = 22.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Black, color = InkNavy)
            Text(subtitle, fontSize = 11.sp, color = InkSoft)
        }
    }
}

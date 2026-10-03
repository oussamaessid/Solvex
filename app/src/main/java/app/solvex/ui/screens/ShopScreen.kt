package app.solvex.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.solvex.ads.AdManager
import app.solvex.ui.theme.FireOrange
import app.solvex.ui.theme.FireRed
import app.solvex.ui.theme.FireYellow
import app.solvex.ui.theme.InkFaint
import app.solvex.ui.theme.InkNavy
import app.solvex.ui.theme.InkSoft
import app.solvex.viewmodel.GameViewModel

private data class HintPack(
    val amount: Int,
    val price: Int,
    val label: String,
    val tagline: String,
    val badge: String?,
    val emoji: String
)

@Composable
fun ShopScreen(vm: GameViewModel) {
    val coins by vm.coins.collectAsState()
    val hints by vm.hints.collectAsState()
    val activity = LocalContext.current as? Activity
    var message by remember { mutableStateOf<String?>(null) }
    var justBought by remember { mutableStateOf<Int?>(null) }

    val packs = remember {
        listOf(
            HintPack(1, 30, "Quick spark", "Try one brilliant move", null, "💡"),
            HintPack(3, 75, "Bright bundle", "Most loved by solvers", "MOST POPULAR", "✨"),
            HintPack(7, 150, "Genius chest", "Best value · save 32%", "BEST VALUE", "🎁")
        )
    }

    BackHandler { vm.closeShop() }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFFFFF8EE), Color(0xFFF2F0FF), Color.White))
        )
    ) {
        // ambient glows
        Box(Modifier.offset(x = (-60).dp, y = (-40).dp).size(220.dp).alpha(0.35f).background(Brush.radialGradient(listOf(Color(0xFFFFB800), Color.Transparent)), CircleShape))
        Box(Modifier.align(Alignment.TopEnd).offset(x = 70.dp, y = 120.dp).size(230.dp).alpha(0.28f).background(Brush.radialGradient(listOf(Color(0xFF8B5CF6), Color.Transparent)), CircleShape))

        Column(
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Header ──
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(42.dp).shadow(6.dp, CircleShape).clip(CircleShape)
                        .background(Color.White).clickable { vm.closeShop() },
                    contentAlignment = Alignment.Center
                ) { Text("←", fontSize = 18.sp, fontWeight = FontWeight.Black, color = InkNavy) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("HINT SHOP", fontSize = 20.sp, fontWeight = FontWeight.Black, color = InkNavy, letterSpacing = 0.5.sp)
                    Text("Power up your spark", fontSize = 12.sp, color = InkSoft)
                }
                Box(
                    Modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xFFFFF3CC))
                        .clickable { }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("🪙 $coins", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF8A5A00))
                }
            }

            // ── Hero ──
            Box(
                Modifier.fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(26.dp))
                    .clip(RoundedCornerShape(26.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFFFF8A2A), Color(0xFFFF2D55), Color(0xFF8B5CF6))))
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(74.dp).clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.22f))
                            .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Text("💡", fontSize = 38.sp) }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("STUCK ON A GRID?", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.85f), letterSpacing = 1.8.sp)
                        Text("A little spark goes a long way", fontSize = 19.sp, fontWeight = FontWeight.Black, color = Color.White, lineHeight = 22.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("You start with 2 free hints · earn coins by solving", fontSize = 11.sp, color = Color.White.copy(alpha = 0.9f))
                    }
                }
                // floating sparkles
                Text("✦", color = Color.White.copy(alpha = 0.8f), fontSize = 15.sp, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp))
            }

            // ── Wallet ──
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ShopWalletCard(
                    modifier = Modifier.weight(1f),
                    emoji = "🪙", value = coins.toString(), label = "COINS",
                    sub = "+20 / level",
                    bg = Brush.linearGradient(listOf(Color.White, Color(0xFFFFF6D8)))
                )
                ShopWalletCard(
                    modifier = Modifier.weight(1f),
                    emoji = "💡", value = hints.toString(), label = "HINTS",
                    sub = if (hints == 0) "Empty — refill!" else "Ready to shine",
                    bg = Brush.linearGradient(listOf(Color.White, Color(0xFFEFEBFF)))
                )
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("CHOOSE YOUR PACK", fontSize = 12.sp, fontWeight = FontWeight.Black, color = InkNavy, letterSpacing = 1.4.sp)
                Spacer(Modifier.weight(1f))
                Text("💎 save more with bundles", fontSize = 11.sp, color = InkFaint)
            }

            packs.forEachIndexed { index, pack ->
                ShopPackCard(
                    pack = pack,
                    coins = coins,
                    highlighted = index == 1,
                    premium = index == 2,
                    justBought = justBought == index,
                    onBuy = {
                        val ok = vm.buyHints(pack.amount, pack.price)
                        if (ok) {
                            message = "+${pack.amount} hint${if (pack.amount > 1) "s" else ""} added to your spark!"
                            justBought = index
                        } else {
                            message = "Not enough coins — solve levels to earn more"
                            justBought = null
                        }
                    }
                )
            }

            // ── Free hint via ad ──
            Box(
                Modifier.fillMaxWidth()
                    .shadow(10.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF0AB4FF), Color(0xFF0055CC))))
                    .clickable {
                        val act = activity
                        if (act != null) {
                            AdManager.showRewarded(act, onReward = {
                                vm.addHintShop()
                                message = "+1 free hint added!"
                            })
                        } else {
                            vm.addHintShop()
                            message = "+1 free hint added!"
                        }
                    }
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
                        Text("📺", fontSize = 24.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Need a free spark?", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Text("Watch a short video · get +1 hint", fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f))
                    }
                    Box(Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White).padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text("GET", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF0055CC))
                    }
                }
            }

            // ── Message ──
            AnimatedVisibility(
                visible = message != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut()
            ) {
                message?.let {
                    val isError = it.startsWith("Not")
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .background(if (isError) Color(0xFFFFE8E8) else Color(0xFFE8FFF1))
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Text(
                            (if (isError) "⚠️ " else "✅ ") + it,
                            color = if (isError) Color(0xFFB00020) else Color(0xFF0E6B3E),
                            fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ShopWalletCard(
    modifier: Modifier,
    emoji: String,
    value: String,
    label: String,
    sub: String,
    bg: Brush
) {
    Box(
        modifier.shadow(8.dp, RoundedCornerShape(18.dp)).clip(RoundedCornerShape(18.dp))
            .background(bg).padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 28.sp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(value, fontSize = 22.sp, fontWeight = FontWeight.Black, color = InkNavy)
                Text(label, fontSize = 10.sp, fontWeight = FontWeight.Black, color = InkSoft, letterSpacing = 1.2.sp)
                Text(sub, fontSize = 10.sp, color = InkFaint)
            }
        }
    }
}

@Composable
private fun ShopPackCard(
    pack: HintPack,
    coins: Int,
    highlighted: Boolean,
    premium: Boolean,
    justBought: Boolean,
    onBuy: () -> Unit
) {
    val affordable = coins >= pack.price
    val scale by animateFloatAsState(if (justBought) 1.02f else 1f, tween(250), label = "packScale")

    Box(
        Modifier.fillMaxWidth()
            .shadow(if (highlighted || premium) 14.dp else 8.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(
                when {
                    premium -> Brush.linearGradient(listOf(Color(0xFF1A1A3E), Color(0xFF3B2E7A)))
                    highlighted -> Brush.linearGradient(listOf(Color.White, Color(0xFFFFF0D3)))
                    else -> Brush.linearGradient(listOf(Color.White, Color.White))
                }
            )
            .then(
                if (highlighted) Modifier.border(2.dp, Brush.horizontalGradient(listOf(FireYellow, FireOrange)), RoundedCornerShape(20.dp))
                else Modifier
            )
            .clickable(enabled = affordable, onClick = onBuy)
            .padding(16.dp)
    ) {
        Column {
            if (pack.badge != null) {
                Box(
                    Modifier.clip(RoundedCornerShape(8.dp))
                        .background(if (premium) FireYellow else FireOrange)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(pack.badge, fontSize = 9.sp, fontWeight = FontWeight.Black, color = if (premium) InkNavy else Color.White, letterSpacing = 1.sp)
                }
                Spacer(Modifier.height(8.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(56.dp).clip(RoundedCornerShape(16.dp))
                        .background(
                            when {
                                premium -> Brush.linearGradient(listOf(FireYellow, FireOrange))
                                highlighted -> Brush.linearGradient(listOf(Color(0xFFFFE780), FireOrange))
                                else -> Brush.linearGradient(listOf(Color(0xFFF2F0FF), Color(0xFFE3DDFF)))
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) { Text(pack.emoji, fontSize = 28.sp) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        pack.label, fontSize = 16.sp, fontWeight = FontWeight.Black,
                        color = if (premium) Color.White else InkNavy
                    )
                    Text(
                        "${pack.amount} hint${if (pack.amount > 1) "s" else ""} · ${pack.tagline}",
                        fontSize = 12.sp, color = if (premium) Color(0xFFCFC8FF) else InkSoft
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (affordable) "Tap to spark ✨" else "🔒 Need ${pack.price - coins} more coins",
                        fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        color = if (affordable) (if (premium) FireYellow else FireOrange) else InkFaint
                    )
                }
                Box(
                    Modifier.clip(RoundedCornerShape(14.dp))
                        .background(
                            if (affordable) Brush.horizontalGradient(listOf(FireOrange, FireRed))
                            else Brush.horizontalGradient(listOf(Color(0xFFE2E2EC), Color(0xFFD4D4E4)))
                        )
                        .padding(horizontal = 14.dp, vertical = 11.dp)
                ) {
                    Text(
                        "🪙 ${pack.price}", color = Color.White,
                        fontWeight = FontWeight.Black, fontSize = 14.sp
                    )
                }
            }
        }
    }
}

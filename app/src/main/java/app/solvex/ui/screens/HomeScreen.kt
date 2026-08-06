package app.solvex.ui.screens

import android.app.Activity
import android.widget.ImageView
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import app.solvex.R
import app.solvex.ads.AdManager
import app.solvex.ui.theme.*
import app.solvex.viewmodel.GameViewModel
import com.bumptech.glide.Glide

@Composable
fun HomeScreen(vm: GameViewModel, darkMode: Boolean) {
    val textColor = Color(0xFF1A1A3E)
    val isLoading by vm.isLoading.collectAsState()
    val activity = LocalContext.current as? Activity

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val titleAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(800), label = "titleAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
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
                AndroidView(
                    modifier = Modifier.size(420.dp),
                    factory = { context ->
                        ImageView(context).apply {
                            scaleType = ImageView.ScaleType.FIT_CENTER
                            Glide.with(context)
                                .asGif()
                                .load(R.raw.solvex_blink)
                                .into(this)
                        }
                    }
                )
                Text(
                    " Solvex ",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    color = textColor,
                    textAlign = TextAlign.Center
                )
                Text(
                    "SPARK. FLOW. SOLVE.",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6666AA),
                    letterSpacing = 3.sp
                )
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
                    color = Color(0xFF6666AA)
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
                        onClick = {
                            val act = activity
                            if (act != null) {
                                AdManager.showInterstitial(act) { vm.resumePlay() }
                            } else {
                                vm.resumePlay()
                            }
                        }
                    )
                }
            }

        }
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


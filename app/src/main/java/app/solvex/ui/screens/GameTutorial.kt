package app.solvex.ui.screens

import android.widget.ImageView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import app.solvex.R
import app.solvex.model.CellElement
import app.solvex.model.Constraint
import app.solvex.model.ConstraintType
import app.solvex.ui.components.GameGrid
import app.solvex.ui.theme.FireOrange
import app.solvex.ui.theme.WaterDeep
import com.bumptech.glide.Glide

@Composable
fun GameTutorialOverlay(onFinish: () -> Unit, onClose: () -> Unit) {
    val solution = remember {
        listOf(
            listOf(CellElement.FIRE, CellElement.FIRE, CellElement.WATER, CellElement.FIRE, CellElement.WATER, CellElement.WATER),
            listOf(CellElement.FIRE, CellElement.WATER, CellElement.FIRE, CellElement.WATER, CellElement.WATER, CellElement.FIRE),
            listOf(CellElement.WATER, CellElement.FIRE, CellElement.WATER, CellElement.WATER, CellElement.FIRE, CellElement.FIRE),
            listOf(CellElement.FIRE, CellElement.WATER, CellElement.WATER, CellElement.FIRE, CellElement.FIRE, CellElement.WATER),
            listOf(CellElement.WATER, CellElement.WATER, CellElement.FIRE, CellElement.FIRE, CellElement.WATER, CellElement.FIRE),
            listOf(CellElement.WATER, CellElement.FIRE, CellElement.FIRE, CellElement.WATER, CellElement.FIRE, CellElement.WATER)
        )
    }
    val targets = remember { listOf(0 to 1, 1 to 1, 2 to 1, 3 to 2, 4 to 4, 5 to 2) }
    val guideMessages = remember {
        listOf(
            "= means SAME. The linked cell is Fire, so place Fire.",
            "This row has 3 Fire and 2 Water. The last cell must be Water.",
            "× means DIFFERENT. The linked cell is Water, so place Fire.",
            "Two Fire are consecutive. Place Water to avoid three Fire in a row.",
            "This row has 3 Fire and 2 Water. Balance it by placing Water.",
            "This row has 3 Water and 2 Fire. Balance it by placing Fire."
        )
    }
    val guideGroups = remember {
        listOf(
            setOf(0 to 0, 0 to 1),
            (0..5).map { 1 to it }.toSet(),
            setOf(2 to 0, 2 to 1),
            setOf(3 to 2, 3 to 3, 3 to 4),
            (0..5).map { 4 to it }.toSet(),
            (0..5).map { 5 to it }.toSet()
        )
    }
    val clues = remember {
        solution.mapIndexed { r, row -> row.mapIndexed { c, value -> if ((r to c) in targets) CellElement.EMPTY else value } }
    }
    val constraints = remember {
        listOf(
            Constraint(0, 0, 0, 1, ConstraintType.EQUAL),
            Constraint(2, 0, 2, 1, ConstraintType.DIFFERENT)
        )
    }
    var board by remember { mutableStateOf(clues) }
    var step by remember { mutableIntStateOf(0) }
    val complete = step >= targets.size

    LaunchedEffect(complete) {
        if (complete) {
            kotlinx.coroutines.delay(900)
            onFinish()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF510102C))
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) { }
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("LEVEL 0 · REQUIRED", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF6666AA), letterSpacing = 1.2.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${step.coerceAtMost(6)}/6", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FireOrange)
                    Spacer(Modifier.width(10.dp))
                    Box(
                        Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFF0F2FF)).clickable(onClick = onClose),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✕", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color(0xFF6666AA))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "Tap the glowing cell to change its content",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF34345F),
                textAlign = TextAlign.Center
            )
            Text(
                "1 tap = Fire  •  Double tap = Water  •  Next tap = Empty",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF777799),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))
            AnimatedContent(targetState = step, label = "ruleAlert") { currentStep ->
                GuideRuleAlert(
                    step = currentStep,
                    complete = complete,
                    expected = if (complete) CellElement.EMPTY else solution[targets[currentStep].first][targets[currentStep].second],
                    message = if (complete) "Great work! You understood the essential Solvex rules." else guideMessages[currentStep]
                )
            }

            Spacer(Modifier.height(22.dp))
            GameGrid(
                board = board,
                clues = clues,
                constraints = constraints,
                errorCells = emptySet(),
                size = 6,
                darkMode = false,
                highlightCell = if (complete) null else targets[step],
                hintCells = if (complete) emptySet() else guideGroups[step],
                onDoubleTap = { row, col ->
                    if (!complete && (row to col) == targets[step]) {
                        board = board.mapIndexed { r, values ->
                            values.mapIndexed { c, old -> if (r == row && c == col) CellElement.WATER else old }
                        }
                        if (solution[row][col] == CellElement.WATER) step++
                    }
                },
                onTap = { row, col ->
                    if (complete || (row to col) != targets[step]) return@GameGrid
                    val next = when (board[row][col]) {
                        CellElement.EMPTY -> CellElement.FIRE
                        CellElement.FIRE -> CellElement.WATER
                        CellElement.WATER -> CellElement.EMPTY
                    }
                    board = board.mapIndexed { r, values -> values.mapIndexed { c, old -> if (r == row && c == col) next else old } }
                    if (next == solution[row][col]) step++
                }
            )

            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniRule(R.drawable.game_mascot_fire, "=", R.drawable.game_mascot_fire, "SAME", Modifier.weight(1f))
                MiniRule(R.drawable.game_mascot_water, "=", R.drawable.game_mascot_water, "SAME", Modifier.weight(1f))
                MiniRule(R.drawable.game_mascot_fire, "×", R.drawable.game_mascot_water, "DIFFERENT", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun GuideRuleAlert(step: Int, complete: Boolean, expected: CellElement, message: String) {
    val accent = when {
        complete -> Color(0xFF1E9E63)
        step == 0 || step == 2 -> Color(0xFF6C63FF)
        step == 3 -> Color(0xFFFF7A35)
        else -> Color(0xFF168EB8)
    }
    val symbol = when {
        complete -> "✓"
        step == 0 || step == 2 -> "🔗"
        step == 3 -> "🚫"
        else -> "⚖"
    }

    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = accent.copy(alpha = .09f)),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, accent.copy(alpha = .38f))
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier.size(42.dp).clip(CircleShape).background(accent),
                contentAlignment = Alignment.Center
            ) {
                Text(symbol, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    if (complete) "LEVEL 0 COMPLETE" else "RULE ${step + 1}",
                    color = accent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(message, color = Color(0xFF30304F), fontSize = 11.sp, lineHeight = 15.sp, fontWeight = FontWeight.Bold)
                if (!complete) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("NOW PLACE", color = Color(0xFF777790), fontSize = 9.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.width(5.dp))
                        Image(
                            painterResource(if (expected == CellElement.FIRE) R.drawable.game_mascot_fire else R.drawable.game_mascot_water),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(if (expected == CellElement.FIRE) "FIRE" else "WATER", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                } else {
                    Text("LEVEL 1 UNLOCKED", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun MiniRule(left: Int, sign: String, right: Int, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFFF0F2FF)).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Image(painterResource(left), null, Modifier.size(20.dp))
            Text(sign, fontSize = 14.sp, fontWeight = FontWeight.Black, color = FireOrange)
            Image(painterResource(right), null, Modifier.size(20.dp))
        }
        Text(label, fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color(0xFF6666AA))
    }
}

@Composable
private fun TapGuide(tapCount: Int, onTap: () -> Unit) {
    var element by remember { mutableStateOf(CellElement.EMPTY) }
    val handMotion = rememberInfiniteTransition(label = "guideHand")
    val handY by handMotion.animateFloat(
        initialValue = -3f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(550, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "handY"
    )
    GuidePage(
        title = "Fill an empty square",
        description = "Touch the orange square below and watch the 3 possible steps."
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("👇", fontSize = 38.sp, modifier = Modifier.graphicsLayer { translationY = handY })
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFF0F2FF))
                    .border(3.dp, FireOrange, RoundedCornerShape(18.dp))
                    .clickable {
                        onTap()
                        element = when (element) {
                            CellElement.EMPTY -> CellElement.FIRE
                            CellElement.FIRE -> CellElement.WATER
                            CellElement.WATER -> CellElement.EMPTY
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                when (element) {
                    CellElement.FIRE -> GameMascot(CellElement.FIRE, Modifier.fillMaxSize(0.9f))
                    CellElement.WATER -> GameMascot(CellElement.WATER, Modifier.fillMaxSize(0.9f))
                    CellElement.EMPTY -> Text("Tap here", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6666AA))
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StepCard("1st click", R.drawable.game_mascot_fire, "FIRE", Modifier.weight(1f))
                StepCard("2nd click", R.drawable.game_mascot_water, "WATER", Modifier.weight(1f))
                StepCard("3rd click", null, "EMPTY", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MatrixRulesGuide() {
    GuidePage(
        title = "Read the small matrix",
        description = "These examples work in every row and every column."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            ExampleBlock("1. Same quantity", "A line of 4 needs 2 Fire + 2 Water.") {
                MiniMatrix(listOf(listOf(CellElement.FIRE, CellElement.WATER, CellElement.FIRE, CellElement.WATER)))
            }
            ExampleBlock("2. Never three identical", "After 2 Fire, the next square must be Water.") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MiniMatrix(listOf(listOf(CellElement.FIRE, CellElement.FIRE, CellElement.WATER)))
                    Text("✓", color = Color(0xFF1E9E63), fontSize = 22.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun LinksGuide() {
    GuidePage(
        title = "Follow = and ×",
        description = "The sign between two squares tells you whether their elements match."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            ExampleBlock("= means SAME", "Both linked squares contain the same element.") {
                LinkedPair(CellElement.FIRE, "=", CellElement.FIRE)
            }
            ExampleBlock("× means DIFFERENT", "One square is Fire and the other is Water.") {
                LinkedPair(CellElement.FIRE, "×", CellElement.WATER)
            }
            Text(
                "You are ready: balance every row and column, avoid triples, and respect every link.",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = WaterDeep,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LinkedPair(left: CellElement, sign: String, right: CellElement) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ElementSquare(left)
        Box(Modifier.width(34.dp), contentAlignment = Alignment.Center) {
            Text(sign, fontSize = 20.sp, fontWeight = FontWeight.Black, color = FireOrange)
        }
        ElementSquare(right)
    }
}

@Composable
private fun StepCard(label: String, drawable: Int?, result: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFFF3F3FC)).padding(7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6666AA))
        Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
            if (drawable != null) Image(painterResource(drawable), null, Modifier.fillMaxSize())
            else Box(Modifier.size(30.dp).clip(RoundedCornerShape(7.dp)).background(Color(0xFFD8DCF0)))
        }
        Text(result, fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFF1A1A3E))
    }
}

@Composable
private fun ExampleBlock(title: String, explanation: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFFF3F3FC)).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF1A1A3E))
        Text(explanation, fontSize = 11.sp, color = Color(0xFF666680), textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun MiniMatrix(rows: List<List<CellElement>>) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                row.forEach { ElementSquare(it) }
            }
        }
    }
}

@Composable
private fun ElementSquare(element: CellElement) {
    Box(
        Modifier.size(46.dp).clip(RoundedCornerShape(9.dp)).background(Color.White).border(1.dp, Color(0xFFD7D9EE), RoundedCornerShape(9.dp)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painterResource(if (element == CellElement.FIRE) R.drawable.game_mascot_fire else R.drawable.game_mascot_water),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(0.9f)
        )
    }
}

@Composable
private fun GuidePage(title: String, description: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().heightIn(min = 360.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFF1A1A3E), textAlign = TextAlign.Center)
        Spacer(Modifier.height(7.dp))
        Text(description, fontSize = 14.sp, color = Color(0xFF666680), textAlign = TextAlign.Center, lineHeight = 20.sp)
        Spacer(Modifier.height(18.dp))
        content()
    }
}

@Composable
private fun GameMascot(element: CellElement, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(if (element == CellElement.FIRE) R.drawable.game_mascot_fire else R.drawable.game_mascot_water),
        contentDescription = if (element == CellElement.FIRE) "Fire" else "Water",
        modifier = modifier
    )
}

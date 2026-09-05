package app.solvex.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.solvex.model.*
import app.solvex.R
import app.solvex.ui.theme.*

private val GAP       = 2.dp   // gap between every cell — uniform, no constraint badges in layout
private val BADGE_DP  = 24.dp  // circular overlay badge diameter

// ─────────────────────────────────────────────────────────────────────────────
// Public grid composable
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun GameGrid(
    board: List<List<CellElement>>,
    clues: List<List<CellElement>>,
    constraints: List<Constraint>,
    errorCells: Set<Pair<Int, Int>>,
    size: Int,
    darkMode: Boolean = false,
    highlightCell: Pair<Int, Int>? = null,
    hintCells: Set<Pair<Int, Int>> = emptySet(),
    onDoubleTap: ((Int, Int) -> Unit)? = null,
    onTap: (Int, Int) -> Unit
) {
    // BoxWithConstraints lets us measure the available width and compute
    // exact cell sizes for badge positioning — cells themselves use weight(1f)
    // so they are always identical regardless of constraints.
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cellSize: Dp = (maxWidth - GAP * (size - 1)) / size

        // Square outer box: height == width
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {

            // ── 1. Cell grid — every cell same size via weight(1f) ────────
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(GAP)
            ) {
                for (r in 0 until size) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(GAP)
                    ) {
                        for (c in 0 until size) {
                            GridCell(
                                element  = board[r][c],
                                isClue   = clues[r][c] != CellElement.EMPTY,
                                isError  = (r to c) in errorCells,
                                isHighlighted = highlightCell == (r to c),
                                isHinted = (r to c) in hintCells,
                                darkMode = darkMode,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                onTap = { onTap(r, c) },
                                onDoubleTap = onDoubleTap?.let { action -> { action(r, c) } }
                            )
                        }
                    }
                }
            }

            // ── 2. Constraint badges — overlaid, never in the row layout ──
            // Drawn after the grid so they appear on top automatically.
            constraints.forEach { con ->
                val horiz = con.r1 == con.r2

                // Centre of the gap between the two constrained cells
                val cx: Dp = if (horiz)
                    (cellSize + GAP) * con.c1 + cellSize + GAP / 2   // midpoint of vertical gap
                else
                    (cellSize + GAP) * con.c1 + cellSize / 2         // column centre

                val cy: Dp = if (horiz)
                    (cellSize + GAP) * con.r1 + cellSize / 2         // row centre
                else
                    (cellSize + GAP) * con.r1 + cellSize + GAP / 2   // midpoint of horizontal gap

                Box(
                    modifier = Modifier
                        .absoluteOffset(x = cx - BADGE_DP / 2, y = cy - BADGE_DP / 2)
                        .size(BADGE_DP),
                    contentAlignment = Alignment.Center
                ) {
                    ConstraintBadge(con.type)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Single cell
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun GridCell(
    element: CellElement,
    isClue: Boolean,
    isError: Boolean,
    isHighlighted: Boolean = false,
    isHinted: Boolean = false,
    darkMode: Boolean = false,
    modifier: Modifier = Modifier,
    onTap: () -> Unit,
    onDoubleTap: (() -> Unit)? = null
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 1.12f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessHigh
        ),
        label = "cellScale"
    )

    val bgBrush = when {
        isError ->
            Brush.radialGradient(listOf(ErrorRed.copy(alpha = 0.8f), ErrorRed.copy(alpha = 0.4f)))
        element == CellElement.FIRE && isClue ->
            Brush.radialGradient(listOf(FireOrange, ClueFire))
        element == CellElement.FIRE ->
            Brush.radialGradient(listOf(FireYellow, FireOrange, FireRed))
        element == CellElement.WATER && isClue ->
            Brush.radialGradient(listOf(WaterBlue, ClueWater))
        element == CellElement.WATER ->
            Brush.radialGradient(listOf(WaterCyan, WaterBlue, WaterDeep))
        else ->
            if (darkMode) Brush.linearGradient(listOf(EmptyDark, Color(0xFF1A1A3A)))
            else Brush.linearGradient(listOf(EmptyLight, Color(0xFFD8DCF0)))
    }

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .background(bgBrush)
            .border(
                width = if (isHighlighted) 4.dp else if (isHinted) 2.dp else 0.dp,
                color = if (isHighlighted) FireYellow else if (isHinted) WaterCyan else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .pointerInput(element) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = { onTap() },
                    onDoubleTap = onDoubleTap?.let { action -> { action() } }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        when (element) {
            CellElement.FIRE  -> AnimatedMascot(CellElement.FIRE, isClue)
            CellElement.WATER -> AnimatedMascot(CellElement.WATER, isClue)
            CellElement.EMPTY -> {}
        }
        // Small white dot marks fixed clue cells
        if (isClue && element != CellElement.EMPTY) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(3.dp)
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.5f))
            )
        }
    }
}

/** The same friendly characters as the app logo, animated to match their element. */
@Composable
private fun AnimatedMascot(element: CellElement, isClue: Boolean) {
    val motion = rememberInfiniteTransition(label = "mascot_$element")
    val phase by motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (element == CellElement.FIRE) 620 else 900, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mascotMotion"
    )
    val animatedScale = if (isClue) 1f else if (element == CellElement.FIRE) 0.92f + phase * 0.12f else 0.95f + phase * 0.07f
    val rotation = if (isClue) 0f else if (element == CellElement.FIRE) -3f + phase * 6f else -1.5f + phase * 3f
    val lift = if (isClue) 0f else if (element == CellElement.WATER) -3f * phase else -1.5f * phase

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxSize(if (element == CellElement.FIRE) 0.78f else 0.72f)
                .scale(0.88f + phase * 0.12f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        if (element == CellElement.FIRE)
                            listOf(FireYellow.copy(alpha = 0.55f), FireOrange.copy(alpha = 0.10f), Color.Transparent)
                        else
                            listOf(WaterCyan.copy(alpha = 0.50f), WaterBlue.copy(alpha = 0.10f), Color.Transparent)
                    )
                )
        )
        Image(
            painter = painterResource(
                if (element == CellElement.FIRE) R.drawable.game_mascot_fire
                else R.drawable.game_mascot_water
            ),
            contentDescription = if (element == CellElement.FIRE) "Fire mascot" else "Water mascot",
            modifier = Modifier
                .fillMaxSize(0.86f)
                .graphicsLayer {
                    scaleX = animatedScale
                    scaleY = animatedScale
                    rotationZ = rotation
                    translationY = lift
                }
        )
        if (!isClue) {
            Text(
                if (element == CellElement.FIRE) "✦" else "●",
                color = Color.White.copy(alpha = 0.65f + phase * 0.3f),
                fontSize = if (element == CellElement.FIRE) 9.sp else 6.sp,
                modifier = Modifier.align(Alignment.TopEnd).padding(5.dp).graphicsLayer {
                    translationY = -5f * phase
                    scaleX = 0.7f + phase * 0.5f
                    scaleY = scaleX
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Circular constraint badge — rendered as an overlay, not inside cell rows
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ConstraintBadge(type: ConstraintType) {
    val isEqual = type == ConstraintType.EQUAL
    val color   = if (isEqual) ConstraintEqual else ConstraintDiff
    val label   = if (isEqual) "=" else "×"
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(Color(0xFF0D0D25).copy(alpha = 0.88f))
            .border(1.5.dp, color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = label,
            fontSize   = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color      = color,
            textAlign  = TextAlign.Center
        )
    }
}

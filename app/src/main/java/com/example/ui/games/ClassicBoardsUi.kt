package com.example.ui.games

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.games.CheckersEngine
import com.example.games.CheckersState
import com.example.games.LudoEngine
import com.example.games.LudoState
import com.example.games.SnakesLaddersEngine
import com.example.games.SnakesLaddersState
import com.example.model.PlayerInfo
import com.example.ui.theme.BrightGold
import com.example.ui.theme.DeepEmeraldFelt
import com.example.ui.theme.EmeraldFelt
import com.example.ui.theme.IvoryTile
import com.example.ui.theme.MidnightBg
import com.example.ui.theme.PlayerColors
import com.example.ui.theme.PlayerDarkColors
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.WalnutDark
import com.example.ui.theme.WalnutWood
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min

// ============================================================================
// 1. LUDO BOARD UI (منچ سلطنتی با صفحه ۱۵×۱۵ کامل و مهره‌های سه‌بعدی براق)
// ============================================================================
private val LUDO_MAIN_TRACK: List<Pair<Int, Int>> = listOf(
    6 to 1, 6 to 2, 6 to 3, 6 to 4, 6 to 5,
    5 to 6, 4 to 6, 3 to 6, 2 to 6, 1 to 6, 0 to 6,
    0 to 7, 0 to 8,
    1 to 8, 2 to 8, 3 to 8, 4 to 8, 5 to 8,
    6 to 9, 6 to 10, 6 to 11, 6 to 12, 6 to 13, 6 to 14,
    7 to 14, 8 to 14,
    8 to 13, 8 to 12, 8 to 11, 8 to 10, 8 to 9,
    9 to 8, 10 to 8, 11 to 8, 12 to 8, 13 to 8, 14 to 8,
    14 to 7, 14 to 6,
    13 to 6, 12 to 6, 11 to 6, 10 to 6, 9 to 6,
    8 to 5, 8 to 4, 8 to 3, 8 to 2, 8 to 1, 8 to 0,
    7 to 0, 6 to 0
)

private val LUDO_HOME_LANES: List<List<Pair<Int, Int>>> = listOf(
    listOf(7 to 1, 7 to 2, 7 to 3, 7 to 4, 7 to 5),       // Seat 0 (Red - Left)
    listOf(1 to 7, 2 to 7, 3 to 7, 4 to 7, 5 to 7),       // Seat 1 (Cyan - Top)
    listOf(7 to 13, 7 to 12, 7 to 11, 7 to 10, 7 to 9),   // Seat 2 (Gold - Right)
    listOf(13 to 7, 12 to 7, 11 to 7, 10 to 7, 9 to 7)    // Seat 3 (Emerald - Bottom)
)

private val LUDO_YARDS: List<List<Pair<Float, Float>>> = listOf(
    listOf(1.8f to 1.8f, 1.8f to 4.2f, 4.2f to 1.8f, 4.2f to 4.2f),       // Seat 0 Top-Left
    listOf(1.8f to 10.8f, 1.8f to 13.2f, 4.2f to 10.8f, 4.2f to 13.2f),   // Seat 1 Top-Right
    listOf(10.8f to 10.8f, 10.8f to 13.2f, 13.2f to 10.8f, 13.2f to 13.2f), // Seat 2 Bottom-Right
    listOf(10.8f to 1.8f, 10.8f to 4.2f, 13.2f to 1.8f, 13.2f to 4.2f)    // Seat 3 Bottom-Left
)

private val LUDO_FINISH_SLOTS: List<Pair<Float, Float>> = listOf(
    7.5f to 6.55f,
    6.55f to 7.5f,
    7.5f to 8.45f,
    8.45f to 7.5f
)

private fun ludoPawnGridPos(playerIdx: Int, playerCount: Int, pawnIdx: Int, progress: Int): Pair<Float, Float> {
    val seat = LudoEngine.seatForPlayer(playerIdx, playerCount)
    return when {
        progress == -1 -> LUDO_YARDS[seat][pawnIdx]
        progress in 0..50 -> {
            val g = LudoEngine.globalTile(playerIdx, playerCount, progress) ?: 0
            val (r, c) = LUDO_MAIN_TRACK[g]
            (r + 0.5f) to (c + 0.5f)
        }
        progress in 51..55 -> {
            val (r, c) = LUDO_HOME_LANES[seat][progress - 51]
            (r + 0.5f) to (c + 0.5f)
        }
        else -> LUDO_FINISH_SLOTS[seat]
    }
}

@Composable
fun LudoGameBoard(
    state: LudoState,
    players: List<PlayerInfo>,
    canAct: Boolean,
    onAction: (String) -> Unit
) {
    val movable = LudoEngine.movablePawns(state)
    val activeColor = PlayerColors[(players.getOrNull(state.currentTurn)?.colorIndex ?: state.currentTurn) % 4]
    val pulseTransition = rememberInfiniteTransition(label = "ludoPulse")
    val pulse by pulseTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "pawnHalo"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PlayerScoreRibbon(
            players = players,
            currentTurn = state.currentTurn,
            scores = state.pawns.map { list -> "${list.count { it == 56 }}/4 قلعه" }
        )

        LuxuryBoardFrame(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(state, canAct) {
                        detectTapGestures { tapOffset ->
                            if (!canAct || !state.hasRolled || movable.isEmpty()) return@detectTapGestures
                            val cell = min(size.width, size.height) / 15f
                            var bestPawn: Int? = null
                            var bestDist = cell * 1.4f
                            for (pawnIdx in movable) {
                                val (gr, gc) = ludoPawnGridPos(
                                    state.currentTurn,
                                    state.playerCount,
                                    pawnIdx,
                                    state.pawns[state.currentTurn][pawnIdx]
                                )
                                val cx = gc * cell
                                val cy = gr * cell
                                val d = hypot(tapOffset.x - cx, tapOffset.y - cy)
                                if (d < bestDist) {
                                    bestDist = d
                                    bestPawn = pawnIdx
                                }
                            }
                            if (bestPawn != null) {
                                onAction(JSONObject().put("op", "MOVE").put("pawn", bestPawn).toString())
                            }
                        }
                    }
                    .testTag("ludo_board_canvas")
            ) {
                val cell = min(size.width, size.height) / 15f
                drawRect(color = IvoryTile)

                // Draw 4 Colored Corner Yards
                val yardDefs = listOf(
                    Triple(0, 0, PlayerColors[0]),
                    Triple(0, 9, PlayerColors[1]),
                    Triple(9, 9, PlayerColors[2]),
                    Triple(9, 0, PlayerColors[3])
                )
                yardDefs.forEachIndexed { seat, (yr, yc, col) ->
                    drawRect(
                        color = col.copy(alpha = 0.92f),
                        topLeft = Offset(yc * cell, yr * cell),
                        size = Size(6 * cell, 6 * cell)
                    )
                    drawRoundRect(
                        color = Color(0xFF101C2B),
                        topLeft = Offset((yc + 0.7f) * cell, (yr + 0.7f) * cell),
                        size = Size(4.6f * cell, 4.6f * cell),
                        cornerRadius = CornerRadius(cell * 0.6f, cell * 0.6f)
                    )
                    LUDO_YARDS[seat].forEach { (sr, sc) ->
                        drawCircle(
                            color = col.copy(alpha = 0.25f),
                            radius = cell * 0.62f,
                            center = Offset(sc * cell, sr * cell)
                        )
                        drawCircle(
                            color = col,
                            radius = cell * 0.62f,
                            center = Offset(sc * cell, sr * cell),
                            style = Stroke(width = 2.5f)
                        )
                    }
                }

                // Draw Main Track Tiles
                LUDO_MAIN_TRACK.forEachIndexed { idx, (r, c) ->
                    val isStartSeat = LudoEngine.START_OFFSETS.indexOf(idx)
                    val isSafe = idx in LudoEngine.SAFE_GLOBAL_TILES
                    val tileColor = when {
                        isStartSeat >= 0 -> PlayerColors[isStartSeat].copy(alpha = 0.82f)
                        isSafe -> Color(0xFFE7DFCF)
                        else -> IvoryTile
                    }
                    drawRect(
                        color = tileColor,
                        topLeft = Offset(c * cell, r * cell),
                        size = Size(cell, cell)
                    )
                    drawRect(
                        color = Color(0xFFCBD5E1),
                        topLeft = Offset(c * cell, r * cell),
                        size = Size(cell, cell),
                        style = Stroke(width = 1.2f)
                    )
                    if (isSafe) {
                        drawCircle(
                            color = if (isStartSeat >= 0) Color.White.copy(alpha = 0.85f) else RoyalGold,
                            radius = cell * 0.22f,
                            center = Offset((c + 0.5f) * cell, (r + 0.5f) * cell)
                        )
                    }
                }

                // Draw 4 Colored Home Stretches
                LUDO_HOME_LANES.forEachIndexed { seat, lane ->
                    val col = PlayerColors[seat]
                    lane.forEach { (r, c) ->
                        drawRect(
                            color = col.copy(alpha = 0.78f),
                            topLeft = Offset(c * cell, r * cell),
                            size = Size(cell, cell)
                        )
                        drawRect(
                            color = Color.White.copy(alpha = 0.5f),
                            topLeft = Offset(c * cell, r * cell),
                            size = Size(cell, cell),
                            style = Stroke(width = 1.2f)
                        )
                    }
                }

                // Draw Center Royal Crown Home (3x3 triangles)
                val cx = 7.5f * cell
                val cy = 7.5f * cell
                val x0 = 6f * cell
                val y0 = 6f * cell
                val x1 = 9f * cell
                val y1 = 9f * cell
                val tris = listOf(
                    PlayerColors[0] to Path().apply { moveTo(x0, y0); lineTo(cx, cy); lineTo(x0, y1); close() },
                    PlayerColors[1] to Path().apply { moveTo(x0, y0); lineTo(x1, y0); lineTo(cx, cy); close() },
                    PlayerColors[2] to Path().apply { moveTo(x1, y0); lineTo(x1, y1); lineTo(cx, cy); close() },
                    PlayerColors[3] to Path().apply { moveTo(x0, y1); lineTo(x1, y1); lineTo(cx, cy); close() }
                )
                tris.forEach { (col, path) -> drawPath(path, col) }
                drawCircle(color = RoyalGold, radius = cell * 0.55f, center = Offset(cx, cy))
                drawCircle(color = Color.White, radius = cell * 0.55f, center = Offset(cx, cy), style = Stroke(2f))

                // Draw All Glossy 3D Pawns
                for (p in 0 until state.playerCount) {
                    val seat = LudoEngine.seatForPlayer(p, state.playerCount)
                    val pCol = PlayerColors[seat]
                    val pDark = PlayerDarkColors[seat]
                    for (k in 0 until 4) {
                        val prog = state.pawns[p][k]
                        val (gr, gc) = ludoPawnGridPos(p, state.playerCount, k, prog)
                        // Slight jitter if multiple pawns share a tile
                        val stackOffset = if (prog in 0..55) {
                            val sameIndex = (0 until k).count { state.pawns[p][it] == prog }
                            Offset((sameIndex % 2) * cell * 0.16f - cell * 0.08f, (sameIndex / 2) * cell * 0.16f - cell * 0.08f)
                        } else Offset.Zero

                        val center = Offset(gc * cell, gr * cell) + stackOffset
                        val isSelectable = canAct && p == state.currentTurn && k in movable

                        if (isSelectable) {
                            drawCircle(
                                color = BrightGold.copy(alpha = 0.45f),
                                radius = cell * 0.56f * pulse,
                                center = center
                            )
                            drawCircle(
                                color = Color.White,
                                radius = cell * 0.48f,
                                center = center,
                                style = Stroke(width = 3f)
                            )
                        }
                        drawGlossyPawn(center, cell * 0.36f, pCol, pDark)
                    }
                }
            }
        }

        // Dice & Quick Pawn Picker Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Animated3DDice(
                value = state.diceValue,
                rollTrigger = state.rollCount,
                enabled = canAct && !state.hasRolled && state.winnerIndex == null,
                accentColor = activeColor,
                onRoll = { onAction(JSONObject().put("op", "ROLL").toString()) }
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                StatusBannerCard(text = state.statusFa, accentColor = activeColor)
                if (canAct && state.hasRolled && movable.size > 1) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        movable.forEach { pIdx ->
                            Button(
                                onClick = { onAction(JSONObject().put("op", "MOVE").put("pawn", pIdx).toString()) },
                                colors = ButtonDefaults.buttonColors(containerColor = activeColor, contentColor = MidnightBg),
                                modifier = Modifier.weight(1f).height(38.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(2.dp)
                            ) {
                                Text("مهره ${pIdx + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawGlossyPawn(center: Offset, radius: Float, light: Color, dark: Color) {
    // Drop shadow
    drawCircle(
        color = Color.Black.copy(alpha = 0.35f),
        radius = radius * 1.05f,
        center = center + Offset(radius * 0.14f, radius * 0.2f)
    )
    // Main sphere gradient
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.85f), light, dark),
            center = center - Offset(radius * 0.32f, radius * 0.32f),
            radius = radius * 1.4f
        ),
        radius = radius,
        center = center
    )
    // Rim highlight
    drawCircle(
        color = Color.White.copy(alpha = 0.8f),
        radius = radius,
        center = center,
        style = Stroke(width = 2f)
    )
}

// ============================================================================
// 2. SNAKES & LADDERS UI (مار و پله جادویی با مارهای منحنی و نردبان چوبی)
// ============================================================================
private fun snakesCellCenter(num: Int, cell: Float): Offset {
    val idx = (num - 1).coerceIn(0, 99)
    val rowFromBottom = idx / 10
    val colInRow = idx % 10
    val c = if (rowFromBottom % 2 == 0) colInRow else (9 - colInRow)
    val r = 9 - rowFromBottom
    return Offset((c + 0.5f) * cell, (r + 0.5f) * cell)
}

@Composable
fun SnakesLaddersBoard(
    state: SnakesLaddersState,
    players: List<PlayerInfo>,
    canAct: Boolean,
    onAction: (String) -> Unit
) {
    val activeColor = PlayerColors[(players.getOrNull(state.currentTurn)?.colorIndex ?: state.currentTurn) % 4]

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PlayerScoreRibbon(
            players = players,
            currentTurn = state.currentTurn,
            scores = state.positions.map { "خانه $it" }
        )

        LuxuryBoardFrame(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            frameStart = Color(0xFF0E4B3E),
            frameEnd = Color(0xFF072520)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("snakes_ladders_canvas")
            ) {
                val cell = min(size.width, size.height) / 10f
                val textPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    textSize = cell * 0.30f
                    textAlign = android.graphics.Paint.Align.LEFT
                    isFakeBoldText = true
                }

                // 10x10 Ornate Board Tiles
                for (num in 1..100) {
                    val center = snakesCellCenter(num, cell)
                    val topLeft = Offset(center.x - cell / 2f, center.y - cell / 2f)
                    val r = (topLeft.y / cell).toInt()
                    val c = (topLeft.x / cell).toInt()
                    val tileCol = when {
                        num == 100 -> Color(0xFFF5B841)
                        num == 1 -> Color(0xFF10B981)
                        (r + c) % 2 == 0 -> Color(0xFF16283B)
                        else -> Color(0xFF1E354D)
                    }
                    drawRect(color = tileCol, topLeft = topLeft, size = Size(cell, cell))
                    drawRect(
                        color = RoyalGold.copy(alpha = 0.22f),
                        topLeft = topLeft,
                        size = Size(cell, cell),
                        style = Stroke(width = 1f)
                    )
                    textPaint.color = if (num == 100 || num == 1) android.graphics.Color.BLACK else android.graphics.Color.argb(185, 235, 242, 250)
                    drawContext.canvas.nativeCanvas.drawText(
                        num.toString(),
                        topLeft.x + cell * 0.12f,
                        topLeft.y + cell * 0.36f,
                        textPaint
                    )
                }

                // Wooden Ladders with Rungs
                SnakesLaddersEngine.LADDERS.forEach { (start, end) ->
                    val p1 = snakesCellCenter(start, cell)
                    val p2 = snakesCellCenter(end, cell)
                    val dx = p2.x - p1.x
                    val dy = p2.y - p1.y
                    val len = hypot(dx, dy).coerceAtLeast(1f)
                    val nx = -dy / len * (cell * 0.16f)
                    val ny = dx / len * (cell * 0.16f)

                    // Shadow
                    drawLine(Color.Black.copy(alpha = 0.35f), p1 + Offset(nx + 3f, ny + 3f), p2 + Offset(nx + 3f, ny + 3f), strokeWidth = 6f, cap = StrokeCap.Round)
                    // Rails
                    drawLine(Color(0xFFFBBF24), p1 + Offset(nx, ny), p2 + Offset(nx, ny), strokeWidth = 5.5f, cap = StrokeCap.Round)
                    drawLine(Color(0xFFD97706), p1 - Offset(nx, ny), p2 - Offset(nx, ny), strokeWidth = 5.5f, cap = StrokeCap.Round)
                    // Rungs
                    val rungs = (len / (cell * 0.42f)).toInt().coerceIn(3, 12)
                    for (i in 1 until rungs) {
                        val t = i.toFloat() / rungs
                        val mid = Offset(p1.x + dx * t, p1.y + dy * t)
                        drawLine(
                            color = Color(0xFFFDE68A),
                            start = mid + Offset(nx, ny),
                            end = mid - Offset(nx, ny),
                            strokeWidth = 3.5f,
                            cap = StrokeCap.Round
                        )
                    }
                }

                // Artistic Curved Snakes with Eyes
                val snakePalette = listOf(Color(0xFFF43F5E), Color(0xFFA855F7), Color(0xFF10B981), Color(0xFFFB7185))
                SnakesLaddersEngine.SNAKES.entries.forEachIndexed { sIdx, (head, tail) ->
                    val pHead = snakesCellCenter(head, cell)
                    val pTail = snakesCellCenter(tail, cell)
                    val sColor = snakePalette[sIdx % snakePalette.size]
                    val mid1 = Offset(
                        pHead.x + (pTail.x - pHead.x) * 0.33f + cell * 0.65f * if (sIdx % 2 == 0) 1 else -1,
                        pHead.y + (pTail.y - pHead.y) * 0.33f
                    )
                    val mid2 = Offset(
                        pHead.x + (pTail.x - pHead.x) * 0.66f - cell * 0.65f * if (sIdx % 2 == 0) 1 else -1,
                        pHead.y + (pTail.y - pHead.y) * 0.66f
                    )
                    val path = Path().apply {
                        moveTo(pHead.x, pHead.y)
                        cubicTo(mid1.x, mid1.y, mid2.x, mid2.y, pTail.x, pTail.y)
                    }
                    // Snake outline + vibrant body
                    drawPath(path, Color.Black.copy(alpha = 0.45f), style = Stroke(width = cell * 0.28f, cap = StrokeCap.Round))
                    drawPath(path, sColor, style = Stroke(width = cell * 0.20f, cap = StrokeCap.Round))
                    drawPath(path, BrightGold.copy(alpha = 0.55f), style = Stroke(width = cell * 0.06f, cap = StrokeCap.Round))
                    // Snake head + glowing eyes
                    drawCircle(color = sColor, radius = cell * 0.22f, center = pHead)
                    drawCircle(color = Color.White, radius = cell * 0.06f, center = pHead + Offset(-cell * 0.07f, -cell * 0.05f))
                    drawCircle(color = Color.White, radius = cell * 0.06f, center = pHead + Offset(cell * 0.07f, -cell * 0.05f))
                    drawCircle(color = Color.Black, radius = cell * 0.03f, center = pHead + Offset(-cell * 0.07f, -cell * 0.05f))
                    drawCircle(color = Color.Black, radius = cell * 0.03f, center = pHead + Offset(cell * 0.07f, -cell * 0.05f))
                }

                // Draw Player Pawns
                state.positions.forEachIndexed { idx, pos ->
                    val base = snakesCellCenter(pos, cell)
                    val offset = when (idx) {
                        0 -> Offset(-cell * 0.16f, -cell * 0.12f)
                        1 -> Offset(cell * 0.16f, -cell * 0.12f)
                        2 -> Offset(-cell * 0.16f, cell * 0.16f)
                        else -> Offset(cell * 0.16f, cell * 0.16f)
                    }
                    val colorIdx = (players.getOrNull(idx)?.colorIndex ?: idx) % 4
                    drawGlossyPawn(base + offset, cell * 0.27f, PlayerColors[colorIdx], PlayerDarkColors[colorIdx])
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Animated3DDice(
                value = state.diceValue,
                rollTrigger = state.rollCount,
                enabled = canAct && state.winnerIndex == null,
                accentColor = activeColor,
                onRoll = { onAction(JSONObject().put("op", "ROLL").toString()) }
            )
            StatusBannerCard(
                text = state.statusFa,
                accentColor = activeColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ============================================================================
// 3. CHECKERS / DAMA UI (چکرز سلطنتی چوب گردو و عاج)
// ============================================================================
@Composable
fun CheckersGameBoard(
    state: CheckersState,
    players: List<PlayerInfo>,
    canAct: Boolean,
    onAction: (String) -> Unit
) {
    val legal = CheckersEngine.legalMovesForPlayer(state.board, state.currentTurn)
    val destinationsForSelected = state.selectedCell?.let { sel ->
        val sr = sel / 8
        val sc = sel % 8
        legal.filter { it.fromR == sr && it.fromC == sc }.map { it.toR * 8 + it.toC }.toSet()
    } ?: emptySet()
    val movableSources = legal.map { it.fromR * 8 + it.fromC }.toSet()

    val count0 = state.board.count { it > 0 }
    val count1 = state.board.count { it < 0 }
    val activeColor = PlayerColors[state.currentTurn % 2]

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PlayerScoreRibbon(
            players = players.take(2),
            currentTurn = state.currentTurn,
            scores = listOf("$count0 مهره", "$count1 مهره")
        )

        LuxuryBoardFrame(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            frameStart = WalnutWood,
            frameEnd = WalnutDark
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                for (r in 0 until 8) {
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        for (c in 0 until 8) {
                            val idx = r * 8 + c
                            val isDark = (r + c) % 2 == 1
                            val piece = state.board[idx]
                            val isSelected = state.selectedCell == idx
                            val isDest = idx in destinationsForSelected
                            val isMovable = canAct && idx in movableSources && state.winnerIndex == null

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .background(
                                        when {
                                            isSelected -> RoyalGold.copy(alpha = 0.55f)
                                            isDest -> EmeraldFelt.copy(alpha = 0.45f)
                                            isDark -> Color(0xFF3E2719)
                                            else -> Color(0xFFEADBC8)
                                        }
                                    )
                                    .clickable(enabled = canAct && state.winnerIndex == null && isDark) {
                                        onAction(JSONObject().put("op", "SELECT").put("cell", idx).toString())
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isDest) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(BrightGold.copy(alpha = 0.85f))
                                    )
                                }
                                if (piece != 0) {
                                    val isP0 = piece > 0
                                    val isKing = abs(piece) == 2
                                    val pColor = if (isP0) PlayerColors[0] else PlayerColors[1]
                                    val pDark = if (isP0) PlayerDarkColors[0] else PlayerDarkColors[1]
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize(0.78f)
                                            .clip(CircleShape)
                                            .background(Brush.radialGradient(listOf(pColor, pDark)))
                                            .border(
                                                width = if (isSelected || isMovable) 2.5.dp else 1.5.dp,
                                                color = if (isSelected) Color.White else if (isMovable) BrightGold else Color.White.copy(alpha = 0.4f),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isKing) {
                                            Text("👑", fontSize = 14.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        StatusBannerCard(text = state.statusFa, accentColor = activeColor)
    }
}

package com.example.ui.games

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.games.BattleshipEngine
import com.example.games.BattleshipState
import com.example.games.ConnectFourEngine
import com.example.games.ConnectFourState
import com.example.games.HexEngine
import com.example.games.HexState
import com.example.games.OthelloEngine
import com.example.games.OthelloState
import com.example.games.UltimateDoozState
import com.example.model.PlayerInfo
import com.example.ui.theme.BrightGold
import com.example.ui.theme.DeepEmeraldFelt
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.EmeraldFelt
import com.example.ui.theme.MidnightBg
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.PlayerColors
import com.example.ui.theme.PlayerDarkColors
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.RubyCrimson
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.VelvetNavy
import org.json.JSONObject
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

// ============================================================================
// 4. ULTIMATE DOOZ 5x5 UI (دوز حرفه‌ای ۵×۵)
// ============================================================================
@Composable
fun UltimateDoozBoard(
    state: UltimateDoozState,
    players: List<PlayerInfo>,
    canAct: Boolean,
    onAction: (String) -> Unit
) {
    val activeColor = PlayerColors[state.currentTurn % 2]
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PlayerScoreRibbon(
            players = players.take(2),
            currentTurn = state.currentTurn,
            scores = listOf("علامت X", "علامت O")
        )

        LuxuryBoardFrame(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            frameStart = Color(0xFF15324E),
            frameEnd = Color(0xFF0A1929),
            innerGlow = NeonCyan
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MidnightBg)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (r in 0 until 5) {
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (c in 0 until 5) {
                            val idx = r * 5 + c
                            val v = state.cells[idx]
                            val isWin = idx in state.winningCells
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        when {
                                            isWin -> RoyalGold.copy(alpha = 0.28f)
                                            v == 0 -> PlayerColors[0].copy(alpha = 0.14f)
                                            v == 1 -> PlayerColors[1].copy(alpha = 0.14f)
                                            else -> ElevatedSlate
                                        }
                                    )
                                    .border(
                                        width = if (isWin) 2.5.dp else 1.dp,
                                        color = if (isWin) BrightGold else Color.White.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable(enabled = canAct && v == -1 && state.winnerIndex == null) {
                                        onAction(JSONObject().put("cell", idx).toString())
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (v != -1) {
                                    Canvas(modifier = Modifier.fillMaxSize(0.58f)) {
                                        if (v == 0) {
                                            drawLine(PlayerColors[0], Offset.Zero, Offset(size.width, size.height), strokeWidth = 10f, cap = StrokeCap.Round)
                                            drawLine(PlayerColors[0], Offset(size.width, 0f), Offset(0f, size.height), strokeWidth = 10f, cap = StrokeCap.Round)
                                        } else {
                                            drawCircle(
                                                color = PlayerColors[1],
                                                radius = min(size.width, size.height) * 0.45f,
                                                style = Stroke(width = 10f)
                                            )
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

// ============================================================================
// 5. CONNECT FOUR UI (چهار در یک ردیف - 7x6)
// ============================================================================
@Composable
fun ConnectFourBoard(
    state: ConnectFourState,
    players: List<PlayerInfo>,
    canAct: Boolean,
    onAction: (String) -> Unit
) {
    val activeColor = PlayerColors[state.currentTurn % 2]
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PlayerScoreRibbon(
            players = players.take(2),
            currentTurn = state.currentTurn,
            scores = listOf("سکه قرمز", "سکه فیروزه‌ای")
        )

        LuxuryBoardFrame(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(7f / 6.3f),
            frameStart = Color(0xFF1E3A8A),
            frameEnd = Color(0xFF172554),
            innerGlow = RoyalGold
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color(0xFF1D4ED8), Color(0xFF1E3A8A))))
                    .padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                for (r in 0 until ConnectFourEngine.ROWS) {
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        for (c in 0 until ConnectFourEngine.COLS) {
                            val idx = r * ConnectFourEngine.COLS + c
                            val v = state.grid[idx]
                            val isWin = idx in state.winningCells
                            val isLast = idx == state.lastDroppedCell

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(
                                        when (v) {
                                            0 -> Brush.radialGradient(listOf(Color(0xFFFF8A9E), PlayerColors[0], PlayerDarkColors[0]))
                                            1 -> Brush.radialGradient(listOf(Color(0xFFA5F3FC), PlayerColors[1], PlayerDarkColors[1]))
                                            else -> Brush.radialGradient(listOf(MidnightBg, Color(0xFF0A1424)))
                                        }
                                    )
                                    .border(
                                        width = when {
                                            isWin -> 3.dp
                                            isLast -> 2.dp
                                            else -> 1.dp
                                        },
                                        color = when {
                                            isWin -> BrightGold
                                            isLast -> Color.White
                                            else -> Color(0xFF0F172A)
                                        },
                                        shape = CircleShape
                                    )
                                    .clickable(enabled = canAct && state.winnerIndex == null && state.grid[c] == -1) {
                                        onAction(JSONObject().put("col", c).toString())
                                    }
                            )
                        }
                    }
                }
            }
        }

        StatusBannerCard(text = state.statusFa, accentColor = activeColor)
    }
}

// ============================================================================
// 6. OTHELLO / REVERSI UI (اتللو ۸×۸ روی مخمل زمردی)
// ============================================================================
@Composable
fun OthelloGameBoard(
    state: OthelloState,
    players: List<PlayerInfo>,
    canAct: Boolean,
    onAction: (String) -> Unit
) {
    val valid = OthelloEngine.validMoves(state.board, state.currentTurn)
    val c0 = state.board.count { it == 0 }
    val c1 = state.board.count { it == 1 }
    val activeColor = PlayerColors[state.currentTurn % 2]

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PlayerScoreRibbon(
            players = players.take(2),
            currentTurn = state.currentTurn,
            scores = listOf("$c0 مهره", "$c1 مهره")
        )

        LuxuryBoardFrame(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            frameStart = DeepEmeraldFelt,
            frameEnd = Color(0xFF052E22),
            innerGlow = EmeraldFelt
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF065F46))
                    .padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                for (r in 0 until 8) {
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        for (c in 0 until 8) {
                            val idx = r * 8 + c
                            val v = state.board[idx]
                            val isValid = canAct && idx in valid && state.winnerIndex == null

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isValid) Color(0xFF0D8A65) else Color(0xFF047857))
                                    .clickable(enabled = isValid) {
                                        onAction(JSONObject().put("cell", idx).toString())
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (v != -1) {
                                    val light = if (v == 0) Color(0xFFFF8FA3) else Color(0xFFA5F3FC)
                                    val main = if (v == 0) PlayerColors[0] else PlayerColors[1]
                                    val dark = if (v == 0) PlayerDarkColors[0] else PlayerDarkColors[1]
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize(0.82f)
                                            .clip(CircleShape)
                                            .background(Brush.radialGradient(listOf(light, main, dark)))
                                            .border(1.5.dp, Color.White.copy(alpha = 0.55f), CircleShape)
                                    )
                                } else if (isValid) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(BrightGold.copy(alpha = 0.78f))
                                    )
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

// ============================================================================
// 7. BATTLESHIP RADAR UI (نبرد دریایی راداری ۷×۷)
// ============================================================================
@Composable
fun BattleshipGameBoard(
    state: BattleshipState,
    players: List<PlayerInfo>,
    viewerIndex: Int,
    canAct: Boolean,
    isPassAndPlay: Boolean,
    onAction: (String) -> Unit
) {
    // In pass-and-play, always show the active shooter's radar against opponent
    val perspective = if (isPassAndPlay) state.currentTurn else viewerIndex.coerceIn(0, 1)
    var showOwnFleet by remember { mutableStateOf(false) }

    val enemyBoard = if (perspective == 0) state.boardP1 else state.boardP0
    val ownBoard = if (perspective == 0) state.boardP0 else state.boardP1
    val activeBoard = if (showOwnFleet) ownBoard else enemyBoard

    val p0Left = state.boardP0.count { it == 1 }
    val p1Left = state.boardP1.count { it == 1 }
    val activeColor = PlayerColors[state.currentTurn % 2]

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PlayerScoreRibbon(
            players = players.take(2),
            currentTurn = state.currentTurn,
            scores = listOf("$p0Left سالم", "$p1Left سالم")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = !showOwnFleet,
                onClick = { showOwnFleet = false },
                label = { Text("رادار دشمن (شلیک 🎯)") },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = RubyCrimson.copy(alpha = 0.25f),
                    selectedLabelColor = TextPrimary
                )
            )
            FilterChip(
                selected = showOwnFleet,
                onClick = { showOwnFleet = true },
                label = { Text("ناوگان من (دفاع ⚓)") },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NeonCyan.copy(alpha = 0.25f),
                    selectedLabelColor = TextPrimary
                )
            )
        }

        LuxuryBoardFrame(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            frameStart = Color(0xFF0C4A6E),
            frameEnd = Color(0xFF082F49),
            innerGlow = NeonCyan
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF061E31))
                    .padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (r in 0 until BattleshipEngine.SIZE) {
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (c in 0 until BattleshipEngine.SIZE) {
                            val idx = r * BattleshipEngine.SIZE + c
                            val cellVal = activeBoard[idx]
                            val canFire = !showOwnFleet && canAct && state.winnerIndex == null && (cellVal == 0 || cellVal == 1)

                            val cellBg = when {
                                cellVal == 3 -> RubyCrimson.copy(alpha = 0.8f)
                                cellVal == 2 -> Color(0xFF1E3A5F)
                                showOwnFleet && cellVal == 1 -> EmeraldFelt.copy(alpha = 0.72f)
                                else -> Color(0xFF0D2C47)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(cellBg)
                                    .border(
                                        width = 1.dp,
                                        color = if (cellVal == 3) BrightGold else NeonCyan.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable(enabled = canFire) {
                                        onAction(JSONObject().put("cell", idx).toString())
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    cellVal == 3 -> Text("💥", fontSize = 18.sp)
                                    cellVal == 2 -> Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(NeonCyan.copy(alpha = 0.65f))
                                    )
                                    showOwnFleet && cellVal == 1 -> Text("🚢", fontSize = 16.sp)
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

// ============================================================================
// 8. HEX PATH UI (مسیر شش‌ضلعی ۷×۷ با رسم هندسی شش‌ضلعی‌ها)
// ============================================================================
@Composable
fun HexPathBoard(
    state: HexState,
    players: List<PlayerInfo>,
    canAct: Boolean,
    onAction: (String) -> Unit
) {
    val activeColor = PlayerColors[state.currentTurn % 2]
    val n = HexEngine.SIZE

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PlayerScoreRibbon(
            players = players.take(2),
            currentTurn = state.currentTurn,
            scores = listOf("بالا ↕ پایین", "چپ ↔ راست")
        )

        LuxuryBoardFrame(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.18f),
            frameStart = Color(0xFF132A3E),
            frameEnd = Color(0xFF0A1624),
            innerGlow = activeColor
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MidnightBg)
                    .pointerInput(state, canAct) {
                        detectTapGestures { tap ->
                            if (!canAct || state.winnerIndex != null) return@detectTapGestures
                            val hexR = min(size.width / (1.5f * n * sqrt(3f) + 1.5f), size.height / (n * 1.65f + 1f))
                            val wStep = sqrt(3f) * hexR
                            val hStep = 1.5f * hexR
                            val boardW = (n - 1) * wStep + (n - 1) * (wStep * 0.5f)
                            val boardH = (n - 1) * hStep
                            val startX = (size.width - boardW) / 2f
                            val startY = (size.height - boardH) / 2f

                            var bestCell: Int? = null
                            var bestDist = hexR * 0.98f
                            for (r in 0 until n) {
                                for (c in 0 until n) {
                                    val cx = startX + c * wStep + r * (wStep * 0.5f)
                                    val cy = startY + r * hStep
                                    val d = hypot(tap.x - cx, tap.y - cy)
                                    if (d < bestDist) {
                                        bestDist = d
                                        bestCell = r * n + c
                                    }
                                }
                            }
                            if (bestCell != null && state.cells[bestCell] == -1) {
                                onAction(JSONObject().put("cell", bestCell).toString())
                            }
                        }
                    }
            ) {
                val hexR = min(size.width / (1.5f * n * sqrt(3f) + 1.5f), size.height / (n * 1.65f + 1f))
                val wStep = sqrt(3f) * hexR
                val hStep = 1.5f * hexR
                val boardW = (n - 1) * wStep + (n - 1) * (wStep * 0.5f)
                val boardH = (n - 1) * hStep
                val startX = (size.width - boardW) / 2f
                val startY = (size.height - boardH) / 2f

                // Border Guide Stripes (Red Top/Bottom, Cyan Left/Right)
                val topL = Offset(startX - hexR, startY - hexR * 1.15f)
                val topR = Offset(startX + (n - 1) * wStep + hexR * 0.6f, startY - hexR * 1.15f)
                val botL = Offset(startX + (n - 1) * wStep * 0.5f - hexR * 0.6f, startY + boardH + hexR * 1.15f)
                val botR = Offset(startX + boardW + hexR, startY + boardH + hexR * 1.15f)

                drawLine(PlayerColors[0], topL, topR, strokeWidth = 8f, cap = StrokeCap.Round)
                drawLine(PlayerColors[0], botL, botR, strokeWidth = 8f, cap = StrokeCap.Round)
                drawLine(PlayerColors[1], topL, botL, strokeWidth = 8f, cap = StrokeCap.Round)
                drawLine(PlayerColors[1], topR, botR, strokeWidth = 8f, cap = StrokeCap.Round)

                for (r in 0 until n) {
                    for (c in 0 until n) {
                        val idx = r * n + c
                        val cx = startX + c * wStep + r * (wStep * 0.5f)
                        val cy = startY + r * hStep
                        val owner = state.cells[idx]
                        val isWin = idx in state.winningPath

                        val path = Path().apply {
                            for (i in 0 until 6) {
                                val angle = (60.0 * i - 30.0) * PI / 180.0
                                val px = cx + (hexR * 0.92f) * cos(angle).toFloat()
                                val py = cy + (hexR * 0.92f) * sin(angle).toFloat()
                                if (i == 0) moveTo(px, py) else lineTo(px, py)
                            }
                            close()
                        }

                        val fill = when (owner) {
                            0 -> PlayerColors[0]
                            1 -> PlayerColors[1]
                            else -> Color(0xFF1B2E43)
                        }
                        drawPath(path, fill)
                        drawPath(
                            path,
                            color = if (isWin) BrightGold else Color.White.copy(alpha = 0.25f),
                            style = Stroke(width = if (isWin) 4.5f else 1.5f)
                        )
                    }
                }
            }
        }

        StatusBannerCard(text = state.statusFa, accentColor = activeColor)
    }
}

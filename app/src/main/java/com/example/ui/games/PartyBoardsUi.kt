package com.example.ui.games

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.games.DotsBoxesEngine
import com.example.games.DotsBoxesState
import com.example.games.MemoryMatchEngine
import com.example.games.MemoryMatchState
import com.example.games.PigDiceEngine
import com.example.games.PigDiceState
import com.example.games.ReflexDuelEngine
import com.example.games.ReflexDuelState
import com.example.model.PlayerInfo
import com.example.ui.theme.BrightGold
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.EmeraldFelt
import com.example.ui.theme.MidnightBg
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.PlayerColors
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.RubyCrimson
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VelvetNavy
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.min

// ============================================================================
// 9. DOTS & BOXES UI (نقطه و خط نئونی ۵×۵)
// ============================================================================
@Composable
fun DotsBoxesBoard(
    state: DotsBoxesState,
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
            scores = state.scores.map { "$it مربع" }
        )

        LuxuryBoardFrame(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            frameStart = Color(0xFF281845),
            frameEnd = Color(0xFF130B24),
            innerGlow = activeColor
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MidnightBg)
                    .pointerInput(state, canAct) {
                        detectTapGestures { tap ->
                            if (!canAct || state.winnerIndex != null) return@detectTapGestures
                            val pad = min(size.width, size.height) * 0.11f
                            val step = (min(size.width, size.height) - pad * 2f) / DotsBoxesEngine.BOX_N
                            var bestIsH = true
                            var bestIdx = -1
                            var bestDist = step * 0.42f

                            // Check Horizontal edges
                            for (r in 0 until DotsBoxesEngine.DOTS) {
                                for (c in 0 until DotsBoxesEngine.BOX_N) {
                                    val idx = r * DotsBoxesEngine.BOX_N + c
                                    if (state.hEdges[idx] != -1) continue
                                    val mx = pad + (c + 0.5f) * step
                                    val my = pad + r * step
                                    val dx = abs(tap.x - mx)
                                    val dy = abs(tap.y - my)
                                    if (dx < step * 0.44f && dy < bestDist) {
                                        bestDist = dy
                                        bestIsH = true
                                        bestIdx = idx
                                    }
                                }
                            }
                            // Check Vertical edges
                            for (r in 0 until DotsBoxesEngine.BOX_N) {
                                for (c in 0 until DotsBoxesEngine.DOTS) {
                                    val idx = r * DotsBoxesEngine.DOTS + c
                                    if (state.vEdges[idx] != -1) continue
                                    val mx = pad + c * step
                                    val my = pad + (r + 0.5f) * step
                                    val dx = abs(tap.x - mx)
                                    val dy = abs(tap.y - my)
                                    if (dy < step * 0.44f && dx < bestDist) {
                                        bestDist = dx
                                        bestIsH = false
                                        bestIdx = idx
                                    }
                                }
                            }
                            if (bestIdx >= 0) {
                                onAction(JSONObject().put("isHoriz", bestIsH).put("idx", bestIdx).toString())
                            }
                        }
                    }
            ) {
                val pad = min(size.width, size.height) * 0.11f
                val step = (min(size.width, size.height) - pad * 2f) / DotsBoxesEngine.BOX_N

                // Draw Claimed Boxes
                for (r in 0 until DotsBoxesEngine.BOX_N) {
                    for (c in 0 until DotsBoxesEngine.BOX_N) {
                        val owner = state.boxes[r * DotsBoxesEngine.BOX_N + c]
                        if (owner != -1) {
                            val col = PlayerColors[(players.getOrNull(owner)?.colorIndex ?: owner) % 4]
                            drawRoundRect(
                                color = col.copy(alpha = 0.34f),
                                topLeft = Offset(pad + c * step + 8f, pad + r * step + 8f),
                                size = Size(step - 16f, step - 16f),
                                cornerRadius = CornerRadius(14f, 14f)
                            )
                        }
                    }
                }

                // Horizontal Edges
                for (r in 0 until DotsBoxesEngine.DOTS) {
                    for (c in 0 until DotsBoxesEngine.BOX_N) {
                        val owner = state.hEdges[r * DotsBoxesEngine.BOX_N + c]
                        val p1 = Offset(pad + c * step, pad + r * step)
                        val p2 = Offset(pad + (c + 1) * step, pad + r * step)
                        if (owner == -1) {
                            drawLine(Color.White.copy(alpha = 0.13f), p1, p2, strokeWidth = 6f, cap = StrokeCap.Round)
                        } else {
                            val col = PlayerColors[(players.getOrNull(owner)?.colorIndex ?: owner) % 4]
                            drawLine(col.copy(alpha = 0.35f), p1, p2, strokeWidth = 16f, cap = StrokeCap.Round)
                            drawLine(col, p1, p2, strokeWidth = 8f, cap = StrokeCap.Round)
                        }
                    }
                }

                // Vertical Edges
                for (r in 0 until DotsBoxesEngine.BOX_N) {
                    for (c in 0 until DotsBoxesEngine.DOTS) {
                        val owner = state.vEdges[r * DotsBoxesEngine.DOTS + c]
                        val p1 = Offset(pad + c * step, pad + r * step)
                        val p2 = Offset(pad + c * step, pad + (r + 1) * step)
                        if (owner == -1) {
                            drawLine(Color.White.copy(alpha = 0.13f), p1, p2, strokeWidth = 6f, cap = StrokeCap.Round)
                        } else {
                            val col = PlayerColors[(players.getOrNull(owner)?.colorIndex ?: owner) % 4]
                            drawLine(col.copy(alpha = 0.35f), p1, p2, strokeWidth = 16f, cap = StrokeCap.Round)
                            drawLine(col, p1, p2, strokeWidth = 8f, cap = StrokeCap.Round)
                        }
                    }
                }

                // Glowing Golden Dots
                for (r in 0 until DotsBoxesEngine.DOTS) {
                    for (c in 0 until DotsBoxesEngine.DOTS) {
                        val pos = Offset(pad + c * step, pad + r * step)
                        drawCircle(BrightGold.copy(alpha = 0.3f), radius = 14f, center = pos)
                        drawCircle(Color.White, radius = 8f, center = pos)
                    }
                }
            }
        }

        StatusBannerCard(text = state.statusFa, accentColor = activeColor)
    }
}

// ============================================================================
// 10. MEMORY MATCH UI (تطبیق کارت ایرانی - ۴×۵)
// ============================================================================
@Composable
fun MemoryMatchBoard(
    state: MemoryMatchState,
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
            scores = state.scores.map { "$it جفت" }
        )

        LuxuryBoardFrame(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.88f),
            frameStart = Color(0xFF3B1B4A),
            frameEnd = Color(0xFF1A0E2B),
            innerGlow = RoyalGold
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MidnightBg)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                for (r in 0 until 5) {
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        for (c in 0 until 4) {
                            val idx = r * 4 + c
                            val owner = state.matchedBy[idx]
                            val isFaceUp = owner != -1 || idx in state.flipped
                            val ownerColor = if (owner != -1) PlayerColors[(players.getOrNull(owner)?.colorIndex ?: owner) % 4] else RoyalGold
                            val bg by animateColorAsState(
                                targetValue = when {
                                    owner != -1 -> ownerColor.copy(alpha = 0.28f)
                                    isFaceUp -> Color(0xFF283E59)
                                    else -> ElevatedSlate
                                },
                                animationSpec = tween(220),
                                label = "cardBg"
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (isFaceUp) Brush.verticalGradient(listOf(bg, bg.copy(alpha = 0.7f)))
                                        else Brush.linearGradient(listOf(Color(0xFF1D3557), Color(0xFF0E1D33)))
                                    )
                                    .border(
                                        width = if (isFaceUp) 2.dp else 1.2.dp,
                                        color = if (owner != -1) ownerColor else if (isFaceUp) BrightGold else RoyalGold.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable(enabled = canAct && !isFaceUp && !state.pendingClear && state.winnerIndex == null) {
                                        onAction(JSONObject().put("op", "FLIP").put("idx", idx).toString())
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isFaceUp) {
                                    Text(
                                        text = MemoryMatchEngine.CARD_ICONS[state.symbols[idx] % MemoryMatchEngine.CARD_ICONS.size],
                                        fontSize = 26.sp
                                    )
                                } else {
                                    Text(
                                        text = "✦",
                                        fontSize = 22.sp,
                                        color = RoyalGold.copy(alpha = 0.6f)
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
// 11. PIG DICE UI (تاس طلایی طمع - هدف ۵۰ امتیاز)
// ============================================================================
@Composable
fun PigDiceBoard(
    state: PigDiceState,
    players: List<PlayerInfo>,
    canAct: Boolean,
    onAction: (String) -> Unit
) {
    val activeColor = PlayerColors[(players.getOrNull(state.currentTurn)?.colorIndex ?: state.currentTurn) % 4]

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PlayerScoreRibbon(
            players = players,
            currentTurn = state.currentTurn,
            scores = state.scores.map { "$it / ${PigDiceEngine.TARGET_SCORE}" }
        )

        // Score Progress Bars
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = ElevatedSlate.copy(alpha = 0.85f),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                players.forEachIndexed { idx, player ->
                    val col = PlayerColors[player.colorIndex % 4]
                    val sc = state.scores.getOrElse(idx) { 0 }
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(player.name, style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                            Text("$sc از ${PigDiceEngine.TARGET_SCORE}", style = MaterialTheme.typography.labelMedium, color = col)
                        }
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { (sc.toFloat() / PigDiceEngine.TARGET_SCORE).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = col,
                            trackColor = MidnightBg
                        )
                    }
                }
            }
        }

        // Golden Pot & 3D Dice Arena
        LuxuryBoardFrame(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp),
            frameStart = Color(0xFF422C0A),
            frameEnd = Color(0xFF1F1405),
            innerGlow = BrightGold
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.radialGradient(listOf(Color(0xFF282012), MidnightBg))),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "بانک این دور",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )
                Text(
                    text = "+${state.turnPot}",
                    style = MaterialTheme.typography.displayLarge,
                    color = BrightGold,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(10.dp))
                Animated3DDice(
                    value = state.diceValue,
                    rollTrigger = state.rollCount,
                    enabled = canAct && state.winnerIndex == null,
                    accentColor = activeColor,
                    size = 80.dp,
                    onRoll = { onAction(JSONObject().put("op", "ROLL").toString()) }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { onAction(JSONObject().put("op", "ROLL").toString()) },
                enabled = canAct && state.winnerIndex == null,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("pig_roll_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = MidnightBg)
            ) {
                Icon(Icons.Rounded.Casino, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("تاس بریز!", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { onAction(JSONObject().put("op", "HOLD").toString()) },
                enabled = canAct && state.turnPot > 0 && state.winnerIndex == null,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("pig_hold_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldFelt, contentColor = MidnightBg)
            ) {
                Icon(Icons.Rounded.Savings, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("ذخیره (+${state.turnPot})", fontWeight = FontWeight.Bold)
            }
        }

        StatusBannerCard(text = state.statusFa, accentColor = activeColor)
    }
}

// ============================================================================
// 12. REFLEX DUEL UI (دوئل واکنش سریع - ۵ راند)
// ============================================================================
@Composable
fun ReflexDuelBoard(
    state: ReflexDuelState,
    players: List<PlayerInfo>,
    isPassAndPlay: Boolean,
    localPlayerIndex: Int,
    canControlRound: Boolean,
    onActionForPlayer: (Int, String) -> Unit
) {
    val signalBg by animateColorAsState(
        targetValue = when (state.phase) {
            "GO" -> BrightGold
            "ARMED" -> RubyCrimson.copy(alpha = 0.78f)
            else -> ElevatedSlate
        },
        animationSpec = tween(140),
        label = "reflexSignal"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PlayerScoreRibbon(
            players = players,
            currentTurn = state.lastWinner ?: -1,
            scores = state.scores.map { "$it برد" }
        )

        // Central Reflex Beacon
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(175.dp)
                .border(
                    width = 2.5.dp,
                    color = if (state.phase == "GO") Color.White else RoyalGold.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(24.dp)
                ),
            color = signalBg,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bolt,
                    contentDescription = "سیگنال واکنش",
                    tint = if (state.phase == "GO") MidnightBg else BrightGold,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = when (state.phase) {
                        "IDLE" -> "راند ${state.round} از ${ReflexDuelEngine.TOTAL_ROUNDS}"
                        "ARMED" -> "صبر کن... هنوز نزن!"
                        "GO" -> "الان بزن!! ⚡"
                        else -> state.lastReactionMs?.let { "رکورد راند: $it میلی‌ثانیه" } ?: "پایان راند"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (state.phase == "GO") MidnightBg else TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Start Round button when idle or round over
        if ((state.phase == "IDLE" || state.phase == "ROUND_OVER") && state.winnerIndex == null && canControlRound) {
            Button(
                onClick = { onActionForPlayer(0, JSONObject().put("op", "ARM_ROUND").toString()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("reflex_arm_button"),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = MidnightBg)
            ) {
                Text(
                    text = if (state.phase == "IDLE") "شروع راند ۱ ⚡" else "شروع راند بعدی (${state.round + 1}/${ReflexDuelEngine.TOTAL_ROUNDS})",
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // Tap Pads: In Pass-and-Play render one pad per player; in Wi-Fi/Bot render big pad for local player!
        if (state.phase == "ARMED" || state.phase == "GO") {
            if (isPassAndPlay) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    players.forEachIndexed { idx, player ->
                        val col = PlayerColors[player.colorIndex % 4]
                        val fouled = state.fouledPlayers.getOrElse(idx) { false }
                        Button(
                            onClick = { onActionForPlayer(idx, JSONObject().put("op", "TAP").toString()) },
                            enabled = !fouled,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = col, contentColor = MidnightBg)
                        ) {
                            Text(
                                text = if (fouled) "${player.name} (خطای زودهنگام!)" else "بزن! — ${player.name} ⚡",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            } else {
                val fouled = state.fouledPlayers.getOrElse(localPlayerIndex) { false }
                Button(
                    onClick = { onActionForPlayer(localPlayerIndex, JSONObject().put("op", "TAP").toString()) },
                    enabled = !fouled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(86.dp)
                        .testTag("reflex_tap_button"),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.phase == "GO") NeonCyan else VelvetNavy,
                        contentColor = if (state.phase == "GO") MidnightBg else TextPrimary
                    )
                ) {
                    Text(
                        text = if (fouled) "خطا! زودتر از نور طلایی زدید ⚠️" else "ضربه بزن! ⚡",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        StatusBannerCard(text = state.statusFa, accentColor = BrightGold)
    }
}

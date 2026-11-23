package com.example.ui.games

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AvatarSymbols
import com.example.model.PlayerInfo
import com.example.ui.theme.BrightGold
import com.example.ui.theme.DeepAmber
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.IvoryTile
import com.example.ui.theme.MidnightBg
import com.example.ui.theme.PlayerColors
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VelvetNavy
import com.example.ui.theme.WalnutDark
import com.example.ui.theme.WalnutWood

@Composable
fun LuxuryBoardFrame(
    modifier: Modifier = Modifier,
    frameStart: Color = WalnutWood,
    frameEnd: Color = WalnutDark,
    innerGlow: Color = RoyalGold,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .shadow(16.dp, RoundedCornerShape(22.dp), ambientColor = innerGlow, spotColor = innerGlow)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(frameStart, frameEnd)))
            .border(
                width = 2.dp,
                brush = Brush.linearGradient(listOf(BrightGold.copy(alpha = 0.85f), DeepAmber.copy(alpha = 0.45f), BrightGold.copy(alpha = 0.85f))),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(15.dp))
                .background(VelvetNavy)
        ) {
            content()
        }
    }
}

@Composable
fun PlayerScoreRibbon(
    players: List<PlayerInfo>,
    currentTurn: Int,
    scores: List<String>? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        players.forEachIndexed { idx, player ->
            val color = PlayerColors[player.colorIndex % PlayerColors.size]
            val isTurn = idx == currentTurn
            val infinite = rememberInfiniteTransition(label = "turnGlow")
            val pulse by infinite.animateFloat(
                initialValue = 0.6f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(750), RepeatMode.Reverse),
                label = "pulse"
            )
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .border(
                        width = if (isTurn) 2.dp else 1.dp,
                        color = if (isTurn) color.copy(alpha = pulse) else Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp)
                    ),
                color = if (isTurn) color.copy(alpha = 0.18f) else ElevatedSlate.copy(alpha = 0.85f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(color, color.copy(alpha = 0.45f))))
                            .border(
                                width = if (player.isSpeaking) 2.dp else 1.dp,
                                color = if (player.isSpeaking) BrightGold else Color.White.copy(alpha = 0.4f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = AvatarSymbols[player.avatarIndex % AvatarSymbols.size],
                            fontSize = 14.sp
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = player.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (player.isSpeaking) {
                                Spacer(Modifier.width(3.dp))
                                Icon(
                                    imageVector = Icons.Rounded.GraphicEq,
                                    contentDescription = "در حال صحبت",
                                    tint = BrightGold,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        if (scores != null && idx < scores.size) {
                            Text(
                                text = scores[idx],
                                style = MaterialTheme.typography.labelSmall,
                                color = color,
                                fontWeight = FontWeight.Bold
                            )
                        } else if (isTurn) {
                            Text(
                                text = "نوبت بازی",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrightGold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Animated3DDice(
    value: Int,
    rollTrigger: Int,
    enabled: Boolean,
    accentColor: Color,
    size: Dp = 68.dp,
    onRoll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }

    LaunchedEffect(rollTrigger) {
        if (rollTrigger > 0) {
            scale.animateTo(1.22f, tween(110, easing = FastOutSlowInEasing))
            rotation.animateTo(rotation.value + 360f, tween(300, easing = FastOutSlowInEasing))
            scale.animateTo(1f, tween(140, easing = FastOutSlowInEasing))
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .scale(scale.value)
            .rotate(rotation.value % 360f)
            .shadow(
                elevation = if (enabled) 14.dp else 4.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = accentColor,
                spotColor = accentColor
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onRoll)
            .testTag("dice_roll_button"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            // 3D Ivory/Gold Cube Face
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = if (enabled) listOf(Color(0xFFFFFDF7), Color(0xFFEBDAC0))
                    else listOf(Color(0xFFD1D5DB), Color(0xFF9CA3AF)),
                    start = Offset.Zero,
                    end = Offset(w, h)
                ),
                cornerRadius = CornerRadius(w * 0.24f, h * 0.24f)
            )
            // Golden beveled rim
            drawRoundRect(
                brush = Brush.linearGradient(listOf(BrightGold, accentColor)),
                cornerRadius = CornerRadius(w * 0.24f, h * 0.24f),
                style = Stroke(width = w * 0.06f)
            )
            // Specular highlight
            drawRoundRect(
                color = Color.White.copy(alpha = 0.45f),
                topLeft = Offset(w * 0.1f, h * 0.08f),
                size = Size(w * 0.8f, h * 0.22f),
                cornerRadius = CornerRadius(w * 0.12f, h * 0.12f)
            )

            // Crisp pips
            val pipR = w * 0.095f
            val pipColor = if (value == 1 || value == 6) Color(0xFFDC2626) else Color(0xFF1E293B)
            val c1 = 0.27f
            val c2 = 0.50f
            val c3 = 0.73f
            val pips = when (value.coerceIn(1, 6)) {
                1 -> listOf(c2 to c2)
                2 -> listOf(c1 to c1, c3 to c3)
                3 -> listOf(c1 to c1, c2 to c2, c3 to c3)
                4 -> listOf(c1 to c1, c3 to c1, c1 to c3, c3 to c3)
                5 -> listOf(c1 to c1, c3 to c1, c2 to c2, c1 to c3, c3 to c3)
                else -> listOf(c1 to c1, c3 to c1, c1 to c2, c3 to c2, c1 to c3, c3 to c3)
            }
            pips.forEach { (px, py) ->
                drawCircle(
                    color = Color.Black.copy(alpha = 0.18f),
                    radius = pipR,
                    center = Offset(w * px + 1.5f, h * py + 2f)
                )
                drawCircle(
                    color = pipColor,
                    radius = pipR,
                    center = Offset(w * px, h * py)
                )
            }
        }
    }
}

@Composable
fun StatusBannerCard(
    text: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
        color = ElevatedSlate.copy(alpha = 0.92f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
        )
    }
}

@Composable
fun WinnerCelebrationBanner(
    winnerIndex: Int,
    players: List<PlayerInfo>,
    canRestart: Boolean,
    onRestart: () -> Unit,
    onBack: () -> Unit
) {
    val winnerName = if (winnerIndex == -1) {
        "بازی مساوی شد!"
    } else {
        players.getOrNull(winnerIndex)?.name ?: "بازیکن ${winnerIndex + 1}"
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                brush = Brush.linearGradient(listOf(BrightGold, RoyalGold, BrightGold)),
                shape = RoundedCornerShape(22.dp)
            ),
        color = VelvetNavy.copy(alpha = 0.96f),
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Rounded.EmojiEvents,
                contentDescription = "جام قهرمانی",
                tint = BrightGold,
                modifier = Modifier.size(44.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (winnerIndex == -1) "تساوی نفس‌گیر!" else "قهرمان این دست: $winnerName",
                style = MaterialTheme.typography.headlineSmall,
                color = BrightGold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (canRestart) {
                    Button(
                        onClick = onRestart,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("play_again_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RoyalGold,
                            contentColor = MidnightBg
                        )
                    ) {
                        Icon(Icons.Rounded.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("بازی مجدد", fontWeight = FontWeight.Bold)
                    }
                }
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("back_to_lobby_button")
                ) {
                    Text("بازگشت", color = TextPrimary)
                }
            }
        }
    }
}

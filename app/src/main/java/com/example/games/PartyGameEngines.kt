package com.example.games

import com.example.games.JsonHelpers.toBoolJsonArray
import com.example.games.JsonHelpers.toBoolList
import com.example.games.JsonHelpers.toIntList
import com.example.games.JsonHelpers.toJsonArray
import com.example.model.GameType
import org.json.JSONObject
import kotlin.random.Random

// ============================================================================
// 9. DOTS & BOXES (نقطه و خط نئونی - 5x5 dots -> 4x4 boxes)
// ============================================================================
data class DotsBoxesState(
    override val playerCount: Int,
    override val currentTurn: Int,
    override val winnerIndex: Int?,
    override val statusFa: String,
    val hEdges: List<Int>, // 5 rows x 4 cols = 20 (-1 unclaimed, 0..3 player)
    val vEdges: List<Int>, // 4 rows x 5 cols = 20 (-1 unclaimed, 0..3 player)
    val boxes: List<Int>,  // 4 x 4 = 16 (-1 unclaimed, 0..3 owner)
    val scores: List<Int>
) : ActiveGameState {
    override val gameType: GameType = GameType.DOTS_BOXES

    override fun toJson(): String = JSONObject().apply {
        put("playerCount", playerCount)
        put("currentTurn", currentTurn)
        put("winnerIndex", winnerIndex ?: -99)
        put("statusFa", statusFa)
        put("hEdges", hEdges.toJsonArray())
        put("vEdges", vEdges.toJsonArray())
        put("boxes", boxes.toJsonArray())
        put("scores", scores.toJsonArray())
    }.toString()
}

object DotsBoxesEngine {
    const val DOTS = 5
    const val BOX_N = 4

    fun create(playerCount: Int): DotsBoxesState = DotsBoxesState(
        playerCount = playerCount,
        currentTurn = 0,
        winnerIndex = null,
        statusFa = "روی خط بین دو نقطه بزنید تا مربع بسازید — نوبت بازیکن ۱",
        hEdges = List(DOTS * BOX_N) { -1 },
        vEdges = List(BOX_N * DOTS) { -1 },
        boxes = List(BOX_N * BOX_N) { -1 },
        scores = List(playerCount) { 0 }
    )

    fun fromJson(json: JSONObject): DotsBoxesState {
        val w = json.optInt("winnerIndex", -99)
        val pc = json.optInt("playerCount", 2)
        return DotsBoxesState(
            playerCount = pc,
            currentTurn = json.optInt("currentTurn", 0),
            winnerIndex = if (w == -99) null else w,
            statusFa = json.optString("statusFa"),
            hEdges = json.optJSONArray("hEdges")?.toIntList() ?: List(DOTS * BOX_N) { -1 },
            vEdges = json.optJSONArray("vEdges")?.toIntList() ?: List(BOX_N * DOTS) { -1 },
            boxes = json.optJSONArray("boxes")?.toIntList() ?: List(BOX_N * BOX_N) { -1 },
            scores = json.optJSONArray("scores")?.toIntList() ?: List(pc) { 0 }
        )
    }

    private fun boxSidesCount(h: List<Int>, v: List<Int>, br: Int, bc: Int): Int {
        var cnt = 0
        if (h[br * BOX_N + bc] != -1) cnt++
        if (h[(br + 1) * BOX_N + bc] != -1) cnt++
        if (v[br * DOTS + bc] != -1) cnt++
        if (v[br * DOTS + (bc + 1)] != -1) cnt++
        return cnt
    }

    fun applyAction(state: DotsBoxesState, playerIndex: Int, action: JSONObject): DotsBoxesState {
        if (state.winnerIndex != null || playerIndex != state.currentTurn) return state
        val isHoriz = action.optBoolean("isHoriz", true)
        val idx = action.optInt("idx", -1)
        val h = state.hEdges.toMutableList()
        val v = state.vEdges.toMutableList()

        if (isHoriz) {
            if (idx !in h.indices || h[idx] != -1) return state
            h[idx] = playerIndex
        } else {
            if (idx !in v.indices || v[idx] != -1) return state
            v[idx] = playerIndex
        }

        val boxes = state.boxes.toMutableList()
        var newlyClosed = 0
        for (br in 0 until BOX_N) {
            for (bc in 0 until BOX_N) {
                val bi = br * BOX_N + bc
                if (boxes[bi] == -1 && boxSidesCount(h, v, br, bc) == 4) {
                    boxes[bi] = playerIndex
                    newlyClosed++
                }
            }
        }
        val scores = state.scores.toMutableList()
        scores[playerIndex] = scores[playerIndex] + newlyClosed

        if (boxes.none { it == -1 }) {
            val maxScore = scores.maxOrNull() ?: 0
            val leaders = scores.indices.filter { scores[it] == maxScore }
            val winner = if (leaders.size == 1) leaders.first() else -1
            return state.copy(
                hEdges = h,
                vEdges = v,
                boxes = boxes,
                scores = scores,
                winnerIndex = winner,
                statusFa = if (winner == -1) "پایان بازی: مساوی! 🤝"
                else "برنده نقطه و خط: بازیکن ${winner + 1} با $maxScore مربع 🏆"
            )
        }

        val nextTurn = if (newlyClosed > 0) playerIndex else (playerIndex + 1) % state.playerCount
        val msg = if (newlyClosed > 0) {
            "عالی! $newlyClosed مربع بسته شد ✨ نوبت اضافه برای بازیکن ${playerIndex + 1}"
        } else {
            "نوبت بازیکن ${nextTurn + 1}"
        }
        return state.copy(
            hEdges = h,
            vEdges = v,
            boxes = boxes,
            scores = scores,
            currentTurn = nextTurn,
            statusFa = msg
        )
    }

    fun botAction(state: DotsBoxesState): String? {
        val candidates = mutableListOf<Pair<Boolean, Int>>()
        state.hEdges.indices.filter { state.hEdges[it] == -1 }.forEach { candidates.add(true to it) }
        state.vEdges.indices.filter { state.vEdges[it] == -1 }.forEach { candidates.add(false to it) }
        if (candidates.isEmpty()) return null

        // Score edges: completing a box is +100, giving opponent a 3rd side is -30
        val best = candidates.maxByOrNull { (isH, idx) ->
            val h = state.hEdges.toMutableList()
            val v = state.vEdges.toMutableList()
            if (isH) h[idx] = state.currentTurn else v[idx] = state.currentTurn
            var score = Random.nextInt(5)
            for (br in 0 until BOX_N) {
                for (bc in 0 until BOX_N) {
                    if (state.boxes[br * BOX_N + bc] != -1) continue
                    val before = boxSidesCount(state.hEdges, state.vEdges, br, bc)
                    val after = boxSidesCount(h, v, br, bc)
                    if (after == 4 && before == 3) score += 100
                    else if (after == 3 && before == 2) score -= 35
                }
            }
            score
        } ?: candidates.first()

        return JSONObject().put("isHoriz", best.first).put("idx", best.second).toString()
    }
}

// ============================================================================
// 10. MEMORY MATCH (تطبیق کارت ایرانی - 20 کارت / 10 جفت)
// ============================================================================
data class MemoryMatchState(
    override val playerCount: Int,
    override val currentTurn: Int,
    override val winnerIndex: Int?,
    override val statusFa: String,
    val symbols: List<Int>, // 20 cards, values 0..9 (2 of each)
    val matchedBy: List<Int>, // 20 cells: -1 unmatched, 0..3 player who claimed
    val flipped: List<Int>, // 0, 1, or 2 currently face-up card indices
    val pendingClear: Boolean,
    val scores: List<Int>
) : ActiveGameState {
    override val gameType: GameType = GameType.MEMORY_MATCH

    override fun toJson(): String = JSONObject().apply {
        put("playerCount", playerCount)
        put("currentTurn", currentTurn)
        put("winnerIndex", winnerIndex ?: -99)
        put("statusFa", statusFa)
        put("symbols", symbols.toJsonArray())
        put("matchedBy", matchedBy.toJsonArray())
        put("flipped", flipped.toJsonArray())
        put("pendingClear", pendingClear)
        put("scores", scores.toJsonArray())
    }.toString()
}

object MemoryMatchEngine {
    val CARD_ICONS = listOf("👑", "🦁", "💎", "🌙", "🔥", "🦚", "🏺", "⭐", "🧿", "🎭")

    fun create(playerCount: Int): MemoryMatchState {
        val deck = ((0 until 10) + (0 until 10)).shuffled()
        return MemoryMatchState(
            playerCount = playerCount,
            currentTurn = 0,
            winnerIndex = null,
            statusFa = "دو کارت هم‌شکل پیدا کنید — نوبت بازیکن ۱",
            symbols = deck,
            matchedBy = List(20) { -1 },
            flipped = emptyList(),
            pendingClear = false,
            scores = List(playerCount) { 0 }
        )
    }

    fun fromJson(json: JSONObject): MemoryMatchState {
        val w = json.optInt("winnerIndex", -99)
        val pc = json.optInt("playerCount", 2)
        return MemoryMatchState(
            playerCount = pc,
            currentTurn = json.optInt("currentTurn", 0),
            winnerIndex = if (w == -99) null else w,
            statusFa = json.optString("statusFa"),
            symbols = json.optJSONArray("symbols")?.toIntList() ?: List(20) { it % 10 },
            matchedBy = json.optJSONArray("matchedBy")?.toIntList() ?: List(20) { -1 },
            flipped = json.optJSONArray("flipped")?.toIntList().orEmpty(),
            pendingClear = json.optBoolean("pendingClear", false),
            scores = json.optJSONArray("scores")?.toIntList() ?: List(pc) { 0 }
        )
    }

    fun applyAction(state: MemoryMatchState, playerIndex: Int, action: JSONObject): MemoryMatchState {
        if (state.winnerIndex != null) return state
        val op = action.optString("op")
        if (op == "RESOLVE_MISMATCH") {
            if (!state.pendingClear) return state
            val next = (state.currentTurn + 1) % state.playerCount
            return state.copy(
                flipped = emptyList(),
                pendingClear = false,
                currentTurn = next,
                statusFa = "جفت نبود! نوبت بازیکن ${next + 1}"
            )
        }
        if (playerIndex != state.currentTurn || state.pendingClear) return state
        val idx = action.optInt("idx", -1)
        if (idx !in 0 until 20 || state.matchedBy[idx] != -1 || idx in state.flipped) return state

        val newFlipped = state.flipped + idx
        if (newFlipped.size == 1) {
            return state.copy(
                flipped = newFlipped,
                statusFa = "کارت دوم را انتخاب کنید..."
            )
        }
        val a = newFlipped[0]
        val b = newFlipped[1]
        if (state.symbols[a] == state.symbols[b]) {
            val matched = state.matchedBy.toMutableList()
            matched[a] = playerIndex
            matched[b] = playerIndex
            val scores = state.scores.toMutableList()
            scores[playerIndex] = scores[playerIndex] + 1
            if (matched.none { it == -1 }) {
                val max = scores.maxOrNull() ?: 0
                val leaders = scores.indices.filter { scores[it] == max }
                val winner = if (leaders.size == 1) leaders.first() else -1
                return state.copy(
                    matchedBy = matched,
                    flipped = emptyList(),
                    scores = scores,
                    winnerIndex = winner,
                    statusFa = if (winner == -1) "پایان بازی: مساوی! 🤝"
                    else "حافظه برتر: بازیکن ${winner + 1} با $max جفت 🏆"
                )
            }
            return state.copy(
                matchedBy = matched,
                flipped = emptyList(),
                scores = scores,
                statusFa = "جفت پیدا شد! ✨ امتیاز و نوبت اضافه برای بازیکن ${playerIndex + 1}"
            )
        } else {
            return state.copy(
                flipped = newFlipped,
                pendingClear = true,
                statusFa = "این دو کارت یکی نیستند..."
            )
        }
    }

    fun botAction(state: MemoryMatchState): String? {
        if (state.pendingClear) return JSONObject().put("op", "RESOLVE_MISMATCH").toString()
        val available = (0 until 20).filter { state.matchedBy[it] == -1 && it !in state.flipped }
        if (available.isEmpty()) return null
        // 40% chance bot remembers the matching card if 1 card is flipped
        if (state.flipped.size == 1 && Random.nextFloat() < 0.45f) {
            val targetSym = state.symbols[state.flipped.first()]
            val match = available.firstOrNull { state.symbols[it] == targetSym }
            if (match != null) return JSONObject().put("op", "FLIP").put("idx", match).toString()
        }
        return JSONObject().put("op", "FLIP").put("idx", available.random()).toString()
    }
}

// ============================================================================
// 11. PIG DICE (تاس طلایی طمع - هدف ۵۰ امتیاز)
// ============================================================================
data class PigDiceState(
    override val playerCount: Int,
    override val currentTurn: Int,
    override val winnerIndex: Int?,
    override val statusFa: String,
    val scores: List<Int>,
    val turnPot: Int,
    val diceValue: Int,
    val rollCount: Int
) : ActiveGameState {
    override val gameType: GameType = GameType.PIG_DICE

    override fun toJson(): String = JSONObject().apply {
        put("playerCount", playerCount)
        put("currentTurn", currentTurn)
        put("winnerIndex", winnerIndex ?: -99)
        put("statusFa", statusFa)
        put("scores", scores.toJsonArray())
        put("turnPot", turnPot)
        put("diceValue", diceValue)
        put("rollCount", rollCount)
    }.toString()
}

object PigDiceEngine {
    const val TARGET_SCORE = 50

    fun create(playerCount: Int): PigDiceState = PigDiceState(
        playerCount = playerCount,
        currentTurn = 0,
        winnerIndex = null,
        statusFa = "تاس بریزید و ریسک کنید! هدف: $TARGET_SCORE امتیاز 🎲",
        scores = List(playerCount) { 0 },
        turnPot = 0,
        diceValue = 6,
        rollCount = 0
    )

    fun fromJson(json: JSONObject): PigDiceState {
        val w = json.optInt("winnerIndex", -99)
        val pc = json.optInt("playerCount", 2)
        return PigDiceState(
            playerCount = pc,
            currentTurn = json.optInt("currentTurn", 0),
            winnerIndex = if (w == -99) null else w,
            statusFa = json.optString("statusFa"),
            scores = json.optJSONArray("scores")?.toIntList() ?: List(pc) { 0 },
            turnPot = json.optInt("turnPot", 0),
            diceValue = json.optInt("diceValue", 6),
            rollCount = json.optInt("rollCount", 0)
        )
    }

    fun applyAction(state: PigDiceState, playerIndex: Int, action: JSONObject): PigDiceState {
        if (state.winnerIndex != null || playerIndex != state.currentTurn) return state
        return when (action.optString("op")) {
            "ROLL" -> {
                val rolled = Random.nextInt(1, 7)
                if (rolled == 1) {
                    val next = (playerIndex + 1) % state.playerCount
                    state.copy(
                        diceValue = 1,
                        turnPot = 0,
                        currentTurn = next,
                        rollCount = state.rollCount + 1,
                        statusFa = "اوه! عدد ۱ آمد و امتیاز این دور سوخت! 💥 نوبت بازیکن ${next + 1}"
                    )
                } else {
                    val newPot = state.turnPot + rolled
                    if (state.scores[playerIndex] + newPot >= TARGET_SCORE) {
                        val updated = state.scores.toMutableList()
                        updated[playerIndex] += newPot
                        state.copy(
                            scores = updated,
                            diceValue = rolled,
                            turnPot = 0,
                            rollCount = state.rollCount + 1,
                            winnerIndex = playerIndex,
                            statusFa = "بازیکن ${playerIndex + 1} با ${updated[playerIndex]} امتیاز قهرمان تاس طلایی شد! 🏆"
                        )
                    } else {
                        state.copy(
                            diceValue = rolled,
                            turnPot = newPot,
                            rollCount = state.rollCount + 1,
                            statusFa = "عدد $rolled آمد! جمع این دور: $newPot — ادامه می‌دهید یا ذخیره؟"
                        )
                    }
                }
            }
            "HOLD" -> {
                if (state.turnPot == 0) return state
                val updated = state.scores.toMutableList()
                updated[playerIndex] += state.turnPot
                val next = (playerIndex + 1) % state.playerCount
                state.copy(
                    scores = updated,
                    turnPot = 0,
                    currentTurn = next,
                    statusFa = "+${state.turnPot} امتیاز ذخیره شد! 💰 نوبت بازیکن ${next + 1}"
                )
            }
            else -> state
        }
    }

    fun botAction(state: PigDiceState): String {
        val shouldHold = state.turnPot >= 14 || (state.scores[state.currentTurn] + state.turnPot >= TARGET_SCORE)
        return JSONObject().put("op", if (shouldHold && state.turnPot > 0) "HOLD" else "ROLL").toString()
    }
}

// ============================================================================
// 12. REFLEX DUEL (دوئل واکنش سریع - ۵ راند)
// Phase: "WAITING" (countdown), "GO" (tap now!), "ROUND_OVER"
// ============================================================================
data class ReflexDuelState(
    override val playerCount: Int,
    override val currentTurn: Int = 0,
    override val winnerIndex: Int?,
    override val statusFa: String,
    val round: Int, // 1..5
    val phase: String, // "IDLE", "ARMED", "GO", "ROUND_OVER"
    val goTimestampMs: Long,
    val scores: List<Int>,
    val lastWinner: Int?,
    val lastReactionMs: Long?,
    val fouledPlayers: List<Boolean>
) : ActiveGameState {
    override val gameType: GameType = GameType.REFLEX_DUEL

    override fun toJson(): String = JSONObject().apply {
        put("playerCount", playerCount)
        put("winnerIndex", winnerIndex ?: -99)
        put("statusFa", statusFa)
        put("round", round)
        put("phase", phase)
        put("goTimestampMs", goTimestampMs)
        put("scores", scores.toJsonArray())
        put("lastWinner", lastWinner ?: -1)
        put("lastReactionMs", lastReactionMs ?: -1L)
        put("fouledPlayers", fouledPlayers.toBoolJsonArray())
    }.toString()
}

object ReflexDuelEngine {
    const val TOTAL_ROUNDS = 5

    fun create(playerCount: Int): ReflexDuelState = ReflexDuelState(
        playerCount = playerCount,
        winnerIndex = null,
        statusFa = "آماده دوئل سرعت؟ دکمه شروع راند را بزنید! ⚡",
        round = 1,
        phase = "IDLE",
        goTimestampMs = 0L,
        scores = List(playerCount) { 0 },
        lastWinner = null,
        lastReactionMs = null,
        fouledPlayers = List(playerCount) { false }
    )

    fun fromJson(json: JSONObject): ReflexDuelState {
        val w = json.optInt("winnerIndex", -99)
        val lw = json.optInt("lastWinner", -1)
        val lr = json.optLong("lastReactionMs", -1L)
        val pc = json.optInt("playerCount", 2)
        return ReflexDuelState(
            playerCount = pc,
            winnerIndex = if (w == -99) null else w,
            statusFa = json.optString("statusFa"),
            round = json.optInt("round", 1),
            phase = json.optString("phase", "IDLE"),
            goTimestampMs = json.optLong("goTimestampMs", 0L),
            scores = json.optJSONArray("scores")?.toIntList() ?: List(pc) { 0 },
            lastWinner = if (lw == -1) null else lw,
            lastReactionMs = if (lr == -1L) null else lr,
            fouledPlayers = json.optJSONArray("fouledPlayers")?.toBoolList() ?: List(pc) { false }
        )
    }

    fun applyAction(state: ReflexDuelState, playerIndex: Int, action: JSONObject): ReflexDuelState {
        if (state.winnerIndex != null) return state
        return when (action.optString("op")) {
            "ARM_ROUND" -> {
                if (state.phase != "IDLE" && state.phase != "ROUND_OVER") return state
                val nextRound = if (state.phase == "ROUND_OVER") state.round + 1 else state.round
                state.copy(
                    round = nextRound,
                    phase = "ARMED",
                    goTimestampMs = 0L,
                    lastWinner = null,
                    lastReactionMs = null,
                    fouledPlayers = List(state.playerCount) { false },
                    statusFa = "راند $nextRound از $TOTAL_ROUNDS — دست‌ها آماده! منتظر نور طلایی باشید..."
                )
            }
            "TRIGGER_GO" -> {
                if (state.phase != "ARMED") return state
                state.copy(
                    phase = "GO",
                    goTimestampMs = System.currentTimeMillis(),
                    statusFa = "الان بزنید!! ⚡⚡⚡"
                )
            }
            "TAP" -> {
                if (playerIndex !in 0 until state.playerCount) return state
                when (state.phase) {
                    "ARMED" -> {
                        if (state.fouledPlayers[playerIndex]) return state
                        val fouls = state.fouledPlayers.toMutableList()
                        fouls[playerIndex] = true
                        state.copy(
                            fouledPlayers = fouls,
                            statusFa = "بازیکن ${playerIndex + 1} زودتر از موعد زد و این راند را از دست داد! ⚠️"
                        )
                    }
                    "GO" -> {
                        if (state.fouledPlayers[playerIndex]) return state
                        val elapsed = (System.currentTimeMillis() - state.goTimestampMs).coerceAtLeast(15L)
                        val scores = state.scores.toMutableList()
                        scores[playerIndex] = scores[playerIndex] + 1

                        val isFinal = state.round >= TOTAL_ROUNDS
                        if (isFinal) {
                            val max = scores.maxOrNull() ?: 0
                            val leaders = scores.indices.filter { scores[it] == max }
                            val overallWinner = if (leaders.size == 1) leaders.first() else -1
                            state.copy(
                                phase = "ROUND_OVER",
                                scores = scores,
                                lastWinner = playerIndex,
                                lastReactionMs = elapsed,
                                winnerIndex = overallWinner,
                                statusFa = if (overallWinner == -1) "پایان ۵ راند: مساوی! 🤝"
                                else "سریع‌ترین دست: بازیکن ${overallWinner + 1} قهرمان شد! 🏆"
                            )
                        } else {
                            state.copy(
                                phase = "ROUND_OVER",
                                scores = scores,
                                lastWinner = playerIndex,
                                lastReactionMs = elapsed,
                                statusFa = "بازیکن ${playerIndex + 1} در $elapsed میلی‌ثانیه برنده راند شد! ⚡"
                            )
                        }
                    }
                    else -> state
                }
            }
            else -> state
        }
    }

    fun botAction(state: ReflexDuelState): String? {
        if (state.phase == "GO") {
            return JSONObject().put("op", "TAP").toString()
        }
        return null
    }
}

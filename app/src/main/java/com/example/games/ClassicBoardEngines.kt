package com.example.games

import com.example.games.JsonHelpers.toIntList
import com.example.games.JsonHelpers.toJsonArray
import com.example.games.JsonHelpers.toNestedIntList
import com.example.games.JsonHelpers.toNestedJsonArray
import com.example.model.GameType
import org.json.JSONObject
import kotlin.math.abs
import kotlin.random.Random

// ============================================================================
// 1. LUDO (منچ سلطنتی)
// Progress per pawn: -1 = home yard, 0..50 = shared 52-tile track steps,
// 51..55 = colored home stretch, 56 = finished center crown!
// ============================================================================
data class LudoState(
    override val playerCount: Int,
    override val currentTurn: Int,
    override val winnerIndex: Int?,
    override val statusFa: String,
    val pawns: List<List<Int>>, // playerCount x 4 pawns
    val diceValue: Int,
    val hasRolled: Boolean,
    val rollCount: Int
) : ActiveGameState {
    override val gameType: GameType = GameType.LUDO

    override fun toJson(): String = JSONObject().apply {
        put("playerCount", playerCount)
        put("currentTurn", currentTurn)
        put("winnerIndex", winnerIndex ?: -99)
        put("statusFa", statusFa)
        put("pawns", pawns.toNestedJsonArray())
        put("diceValue", diceValue)
        put("hasRolled", hasRolled)
        put("rollCount", rollCount)
    }.toString()
}

object LudoEngine {
    // Standard 52-cell track entry offset for each of the 4 seats
    val START_OFFSETS = listOf(0, 13, 26, 39)
    val SAFE_GLOBAL_TILES = setOf(0, 8, 13, 21, 26, 34, 39, 47)

    fun create(playerCount: Int): LudoState {
        // Start with 1 pawn already ready at entry (0) and 3 in yard (-1) for snappy exciting games!
        val pawns = List(playerCount) { listOf(0, -1, -1, -1) }
        return LudoState(
            playerCount = playerCount,
            currentTurn = 0,
            winnerIndex = null,
            statusFa = "نوبت بازیکن ۱ است — تاس بریزید! 🎲",
            pawns = pawns,
            diceValue = 6,
            hasRolled = false,
            rollCount = 0
        )
    }

    fun fromJson(json: JSONObject): LudoState {
        val w = json.optInt("winnerIndex", -99)
        return LudoState(
            playerCount = json.optInt("playerCount", 2),
            currentTurn = json.optInt("currentTurn", 0),
            winnerIndex = if (w == -99) null else w,
            statusFa = json.optString("statusFa"),
            pawns = json.optJSONArray("pawns")?.toNestedIntList() ?: List(2) { listOf(0, -1, -1, -1) },
            diceValue = json.optInt("diceValue", 1),
            hasRolled = json.optBoolean("hasRolled", false),
            rollCount = json.optInt("rollCount", 0)
        )
    }

    fun seatForPlayer(playerIndex: Int, playerCount: Int): Int {
        return if (playerCount == 2 && playerIndex == 1) 2 else playerIndex
    }

    fun globalTile(playerIndex: Int, playerCount: Int, progress: Int): Int? {
        if (progress !in 0..50) return null
        val seat = seatForPlayer(playerIndex, playerCount)
        return (START_OFFSETS[seat] + progress) % 52
    }

    fun movablePawns(state: LudoState): List<Int> {
        if (!state.hasRolled || state.winnerIndex != null) return emptyList()
        val myPawns = state.pawns[state.currentTurn]
        val d = state.diceValue
        return myPawns.indices.filter { idx ->
            val p = myPawns[idx]
            when {
                p == -1 -> d == 6
                p == 56 -> false
                else -> p + d <= 56
            }
        }
    }

    fun applyAction(state: LudoState, playerIndex: Int, action: JSONObject): LudoState {
        if (state.winnerIndex != null || playerIndex != state.currentTurn) return state
        return when (action.optString("op")) {
            "ROLL" -> {
                if (state.hasRolled) return state
                val rolled = Random.nextInt(1, 7)
                val rolledState = state.copy(
                    diceValue = rolled,
                    hasRolled = true,
                    rollCount = state.rollCount + 1
                )
                val valid = movablePawns(rolledState)
                when {
                    valid.isEmpty() -> {
                        val next = (state.currentTurn + 1) % state.playerCount
                        rolledState.copy(
                            currentTurn = next,
                            hasRolled = false,
                            statusFa = "عدد $rolled آمد، حرکتی ممکن نیست — نوبت بازیکن ${next + 1}"
                        )
                    }
                    valid.size == 1 -> {
                        // Auto-move the sole legal pawn for effortless gameplay
                        movePawn(rolledState, valid.first())
                    }
                    else -> {
                        rolledState.copy(
                            statusFa = "عدد $rolled آمد! یکی از مهره‌های درخشان را انتخاب کنید ✨"
                        )
                    }
                }
            }
            "MOVE" -> {
                if (!state.hasRolled) return state
                val pawnIdx = action.optInt("pawn", -1)
                if (pawnIdx !in movablePawns(state)) return state
                movePawn(state, pawnIdx)
            }
            else -> state
        }
    }

    private fun movePawn(state: LudoState, pawnIdx: Int): LudoState {
        val turn = state.currentTurn
        val d = state.diceValue
        val mutablePawns = state.pawns.map { it.toMutableList() }
        val cur = mutablePawns[turn][pawnIdx]
        val target = if (cur == -1) 0 else cur + d
        mutablePawns[turn][pawnIdx] = target

        var captured = false
        val landedGlobal = globalTile(turn, state.playerCount, target)
        if (landedGlobal != null && landedGlobal !in SAFE_GLOBAL_TILES) {
            for (opp in 0 until state.playerCount) {
                if (opp == turn) continue
                for (j in 0 until 4) {
                    val oppGlobal = globalTile(opp, state.playerCount, mutablePawns[opp][j])
                    if (oppGlobal == landedGlobal) {
                        mutablePawns[opp][j] = -1
                        captured = true
                    }
                }
            }
        }

        val reachedHome = target == 56
        val won = mutablePawns[turn].all { it == 56 }
        if (won) {
            return state.copy(
                pawns = mutablePawns,
                hasRolled = false,
                winnerIndex = turn,
                statusFa = "بازیکن ${turn + 1} همه مهره‌ها را به قلعه رساند و قهرمان منچ شد! 🏆"
            )
        }

        val bonusTurn = d == 6 || captured || reachedHome
        val nextTurn = if (bonusTurn) turn else (turn + 1) % state.playerCount
        val msg = when {
            captured -> "مهره حریف زده شد! 💥 جایزه تاس دوباره برای بازیکن ${turn + 1}"
            reachedHome -> "مهره به مرکز قلعه رسید! 👑 جایزه تاس دوباره!"
            d == 6 -> "۶ آمد! 🎉 دوباره تاس بریزید"
            else -> "حرکت انجام شد — نوبت بازیکن ${nextTurn + 1}"
        }
        return state.copy(
            pawns = mutablePawns,
            currentTurn = nextTurn,
            hasRolled = false,
            statusFa = msg
        )
    }

    fun botAction(state: LudoState): String {
        if (!state.hasRolled) {
            return JSONObject().put("op", "ROLL").toString()
        }
        val valid = movablePawns(state)
        if (valid.isEmpty()) return JSONObject().put("op", "ROLL").toString()
        val turn = state.currentTurn
        val d = state.diceValue
        // Prioritize: capture > reach home > exit yard > farthest pawn
        val best = valid.maxByOrNull { idx ->
            val cur = state.pawns[turn][idx]
            val next = if (cur == -1) 0 else cur + d
            val nextGlobal = globalTile(turn, state.playerCount, next)
            var score = next
            if (cur == -1) score += 40
            if (next == 56) score += 200
            if (nextGlobal != null && nextGlobal !in SAFE_GLOBAL_TILES) {
                val hitsOpp = (0 until state.playerCount).any { opp ->
                    opp != turn && state.pawns[opp].any { globalTile(opp, state.playerCount, it) == nextGlobal }
                }
                if (hitsOpp) score += 300
            }
            score
        } ?: valid.first()
        return JSONObject().put("op", "MOVE").put("pawn", best).toString()
    }
}

// ============================================================================
// 2. SNAKES & LADDERS (مار و پله جادویی)
// ============================================================================
data class SnakesLaddersState(
    override val playerCount: Int,
    override val currentTurn: Int,
    override val winnerIndex: Int?,
    override val statusFa: String,
    val positions: List<Int>, // 1..100
    val diceValue: Int,
    val rollCount: Int,
    val lastJumpFrom: Int?,
    val lastJumpTo: Int?
) : ActiveGameState {
    override val gameType: GameType = GameType.SNAKES_LADDERS

    override fun toJson(): String = JSONObject().apply {
        put("playerCount", playerCount)
        put("currentTurn", currentTurn)
        put("winnerIndex", winnerIndex ?: -99)
        put("statusFa", statusFa)
        put("positions", positions.toJsonArray())
        put("diceValue", diceValue)
        put("rollCount", rollCount)
        put("lastJumpFrom", lastJumpFrom ?: -1)
        put("lastJumpTo", lastJumpTo ?: -1)
    }.toString()
}

object SnakesLaddersEngine {
    val LADDERS = mapOf(
        4 to 25,
        13 to 46,
        33 to 49,
        42 to 63,
        50 to 69,
        62 to 81,
        74 to 92
    )
    val SNAKES = mapOf(
        27 to 7,
        40 to 3,
        43 to 18,
        54 to 31,
        66 to 45,
        76 to 58,
        89 to 53,
        99 to 41
    )

    fun create(playerCount: Int): SnakesLaddersState = SnakesLaddersState(
        playerCount = playerCount,
        currentTurn = 0,
        winnerIndex = null,
        statusFa = "مسابقه مار و پله آغاز شد! نوبت بازیکن ۱ 🎲",
        positions = List(playerCount) { 1 },
        diceValue = 1,
        rollCount = 0,
        lastJumpFrom = null,
        lastJumpTo = null
    )

    fun fromJson(json: JSONObject): SnakesLaddersState {
        val w = json.optInt("winnerIndex", -99)
        val jf = json.optInt("lastJumpFrom", -1)
        val jt = json.optInt("lastJumpTo", -1)
        return SnakesLaddersState(
            playerCount = json.optInt("playerCount", 2),
            currentTurn = json.optInt("currentTurn", 0),
            winnerIndex = if (w == -99) null else w,
            statusFa = json.optString("statusFa"),
            positions = json.optJSONArray("positions")?.toIntList() ?: listOf(1, 1),
            diceValue = json.optInt("diceValue", 1),
            rollCount = json.optInt("rollCount", 0),
            lastJumpFrom = if (jf == -1) null else jf,
            lastJumpTo = if (jt == -1) null else jt
        )
    }

    fun applyAction(state: SnakesLaddersState, playerIndex: Int, action: JSONObject): SnakesLaddersState {
        if (state.winnerIndex != null || playerIndex != state.currentTurn) return state
        if (action.optString("op") != "ROLL") return state

        val rolled = Random.nextInt(1, 7)
        val updated = state.positions.toMutableList()
        val oldPos = updated[playerIndex]
        var nextPos = oldPos + rolled
        if (nextPos > 100) {
            nextPos = 100 - (nextPos - 100) // Bounce back cleanly from 100
        }

        var jumpFrom: Int? = null
        var jumpTo: Int? = null
        var eventMsg = "بازیکن ${playerIndex + 1} عدد $rolled آورد و به خانه $nextPos رفت"

        val ladderDest = LADDERS[nextPos]
        val snakeDest = SNAKES[nextPos]
        if (ladderDest != null) {
            jumpFrom = nextPos
            jumpTo = ladderDest
            nextPos = ladderDest
            eventMsg = "نردبان جادویی! 🪜 صعود از $jumpFrom به $ladderDest"
        } else if (snakeDest != null) {
            jumpFrom = nextPos
            jumpTo = snakeDest
            nextPos = snakeDest
            eventMsg = "نیش مار! 🐍 سقوط از $jumpFrom به $snakeDest"
        }

        updated[playerIndex] = nextPos
        if (nextPos == 100) {
            return state.copy(
                positions = updated,
                diceValue = rolled,
                rollCount = state.rollCount + 1,
                winnerIndex = playerIndex,
                lastJumpFrom = jumpFrom,
                lastJumpTo = jumpTo,
                statusFa = "بازیکن ${playerIndex + 1} به قله ۱۰۰ رسید و برنده شد! 🏆"
            )
        }

        val nextTurn = if (rolled == 6) playerIndex else (playerIndex + 1) % state.playerCount
        val extra = if (rolled == 6) " — جایزه ۶: دوباره بریزید!" else ""
        return state.copy(
            positions = updated,
            currentTurn = nextTurn,
            diceValue = rolled,
            rollCount = state.rollCount + 1,
            lastJumpFrom = jumpFrom,
            lastJumpTo = jumpTo,
            statusFa = eventMsg + extra
        )
    }

    fun botAction(state: SnakesLaddersState): String = JSONObject().put("op", "ROLL").toString()
}

// ============================================================================
// 3. CHECKERS / DAMA (چکرز سلطنتی)
// Board 8x8: 0 = empty, 1 = P0 man, 2 = P0 king, -1 = P1 man, -2 = P1 king
// ============================================================================
data class CheckersMove(
    val fromR: Int,
    val fromC: Int,
    val toR: Int,
    val toC: Int,
    val capR: Int? = null,
    val capC: Int? = null
)

data class CheckersState(
    override val playerCount: Int = 2,
    override val currentTurn: Int,
    override val winnerIndex: Int?,
    override val statusFa: String,
    val board: List<Int>, // 64 cells
    val selectedCell: Int? // 0..63
) : ActiveGameState {
    override val gameType: GameType = GameType.CHECKERS

    override fun toJson(): String = JSONObject().apply {
        put("currentTurn", currentTurn)
        put("winnerIndex", winnerIndex ?: -99)
        put("statusFa", statusFa)
        put("board", board.toJsonArray())
        put("selectedCell", selectedCell ?: -1)
    }.toString()
}

object CheckersEngine {
    fun create(): CheckersState {
        val board = MutableList(64) { 0 }
        for (r in 0 until 8) {
            for (c in 0 until 8) {
                if ((r + c) % 2 == 1) {
                    if (r < 3) board[r * 8 + c] = -1 // Player 1 (top, moves down)
                    else if (r > 4) board[r * 8 + c] = 1 // Player 0 (bottom, moves up)
                }
            }
        }
        return CheckersState(
            currentTurn = 0,
            winnerIndex = null,
            statusFa = "نوبت مهره‌های قرمز است — مهره‌ای انتخاب کنید",
            board = board,
            selectedCell = null
        )
    }

    fun fromJson(json: JSONObject): CheckersState {
        val w = json.optInt("winnerIndex", -99)
        val sel = json.optInt("selectedCell", -1)
        return CheckersState(
            currentTurn = json.optInt("currentTurn", 0),
            winnerIndex = if (w == -99) null else w,
            statusFa = json.optString("statusFa"),
            board = json.optJSONArray("board")?.toIntList() ?: create().board,
            selectedCell = if (sel == -1) null else sel
        )
    }

    private fun belongsTo(piece: Int, player: Int): Boolean =
        if (player == 0) piece > 0 else piece < 0

    fun legalMovesForPlayer(board: List<Int>, player: Int): List<CheckersMove> {
        val captures = mutableListOf<CheckersMove>()
        val normals = mutableListOf<CheckersMove>()
        for (r in 0 until 8) {
            for (c in 0 until 8) {
                val piece = board[r * 8 + c]
                if (!belongsTo(piece, player)) continue
                val isKing = abs(piece) == 2
                val dirs = when {
                    isKing -> listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)
                    player == 0 -> listOf(-1 to -1, -1 to 1)
                    else -> listOf(1 to -1, 1 to 1)
                }
                for ((dr, dc) in dirs) {
                    val nr = r + dr
                    val nc = c + dc
                    if (nr in 0..7 && nc in 0..7) {
                        val adj = board[nr * 8 + nc]
                        if (adj == 0) {
                            normals.add(CheckersMove(r, c, nr, nc))
                        } else if (!belongsTo(adj, player)) {
                            val jr = nr + dr
                            val jc = nc + dc
                            if (jr in 0..7 && jc in 0..7 && board[jr * 8 + jc] == 0) {
                                captures.add(CheckersMove(r, c, jr, jc, nr, nc))
                            }
                        }
                    }
                }
            }
        }
        return if (captures.isNotEmpty()) captures else normals
    }

    fun applyAction(state: CheckersState, playerIndex: Int, action: JSONObject): CheckersState {
        if (state.winnerIndex != null || playerIndex != state.currentTurn) return state
        val op = action.optString("op")
        if (op == "SELECT") {
            val cell = action.optInt("cell", -1)
            if (cell !in 0..63) return state
            val legal = legalMovesForPlayer(state.board, playerIndex)
            // If clicking a destination of selected piece, execute move!
            val sel = state.selectedCell
            if (sel != null) {
                val sr = sel / 8
                val sc = sel % 8
                val tr = cell / 8
                val tc = cell % 8
                val matched = legal.firstOrNull { it.fromR == sr && it.fromC == sc && it.toR == tr && it.toC == tc }
                if (matched != null) {
                    return executeMove(state, matched)
                }
            }
            // Otherwise select piece if it has legal moves
            val hasMove = legal.any { it.fromR * 8 + it.fromC == cell }
            return if (hasMove) state.copy(selectedCell = cell) else state.copy(selectedCell = null)
        } else if (op == "MOVE") {
            val fromR = action.optInt("fromR")
            val fromC = action.optInt("fromC")
            val toR = action.optInt("toR")
            val toC = action.optInt("toC")
            val legal = legalMovesForPlayer(state.board, playerIndex)
            val matched = legal.firstOrNull {
                it.fromR == fromR && it.fromC == fromC && it.toR == toR && it.toC == toC
            } ?: return state
            return executeMove(state, matched)
        }
        return state
    }

    private fun executeMove(state: CheckersState, move: CheckersMove): CheckersState {
        val board = state.board.toMutableList()
        var piece = board[move.fromR * 8 + move.fromC]
        board[move.fromR * 8 + move.fromC] = 0
        if (move.capR != null && move.capC != null) {
            board[move.capR * 8 + move.capC] = 0
        }
        var crowned = false
        if (state.currentTurn == 0 && move.toR == 0 && piece == 1) {
            piece = 2
            crowned = true
        } else if (state.currentTurn == 1 && move.toR == 7 && piece == -1) {
            piece = -2
            crowned = true
        }
        board[move.toR * 8 + move.toC] = piece

        val nextPlayer = 1 - state.currentTurn
        val oppMoves = legalMovesForPlayer(board, nextPlayer)
        if (oppMoves.isEmpty()) {
            return state.copy(
                board = board,
                selectedCell = null,
                winnerIndex = state.currentTurn,
                statusFa = "بازیکن ${state.currentTurn + 1} برنده چکرز سلطنتی شد! 👑"
            )
        }
        val msg = when {
            crowned -> "تاج‌گذاری شاه انجام شد! 👑 نوبت بازیکن ${nextPlayer + 1}"
            move.capR != null -> "مهره حریف شکار شد! 💥 نوبت بازیکن ${nextPlayer + 1}"
            else -> "نوبت بازیکن ${nextPlayer + 1}"
        }
        return state.copy(
            board = board,
            currentTurn = nextPlayer,
            selectedCell = null,
            statusFa = msg
        )
    }

    fun botAction(state: CheckersState): String? {
        val moves = legalMovesForPlayer(state.board, state.currentTurn)
        if (moves.isEmpty()) return null
        val chosen = moves.maxByOrNull { m ->
            var score = Random.nextInt(5)
            if (m.capR != null) score += 100
            if (m.toR == 7 || m.toR == 0) score += 50
            score
        } ?: moves.first()
        return JSONObject().apply {
            put("op", "MOVE")
            put("fromR", chosen.fromR)
            put("fromC", chosen.fromC)
            put("toR", chosen.toR)
            put("toC", chosen.toC)
        }.toString()
    }
}

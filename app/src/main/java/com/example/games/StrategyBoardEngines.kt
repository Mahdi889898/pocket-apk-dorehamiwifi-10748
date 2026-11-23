package com.example.games

import com.example.games.JsonHelpers.toIntList
import com.example.games.JsonHelpers.toJsonArray
import com.example.model.GameType
import org.json.JSONObject
import java.util.ArrayDeque
import kotlin.random.Random

// ============================================================================
// 4. ULTIMATE 5x5 DOOZ (دوز حرفه‌ای ۵×۵ - ۴ تایی)
// ============================================================================
data class UltimateDoozState(
    override val playerCount: Int = 2,
    override val currentTurn: Int,
    override val winnerIndex: Int?,
    override val statusFa: String,
    val cells: List<Int>, // 25 cells: -1 empty, 0 = P0 (X), 1 = P1 (O)
    val winningCells: List<Int>
) : ActiveGameState {
    override val gameType: GameType = GameType.ULTIMATE_DOOZ

    override fun toJson(): String = JSONObject().apply {
        put("currentTurn", currentTurn)
        put("winnerIndex", winnerIndex ?: -99)
        put("statusFa", statusFa)
        put("cells", cells.toJsonArray())
        put("winningCells", winningCells.toJsonArray())
    }.toString()
}

object UltimateDoozEngine {
    private val WIN_LINES: List<List<Int>> = buildList {
        val n = 5
        // Horizontal 4-in-a-row
        for (r in 0 until n) for (c in 0..1) add((0..3).map { r * n + (c + it) })
        // Vertical 4-in-a-row
        for (c in 0 until n) for (r in 0..1) add((0..3).map { (r + it) * n + c })
        // Main diagonals
        for (r in 0..1) for (c in 0..1) add((0..3).map { (r + it) * n + (c + it) })
        // Anti diagonals
        for (r in 0..1) for (c in 3..4) add((0..3).map { (r + it) * n + (c - it) })
    }

    fun create(): UltimateDoozState = UltimateDoozState(
        currentTurn = 0,
        winnerIndex = null,
        statusFa = "۴ علامت هم‌ردیف بسازید — نوبت بازیکن ۱ (X)",
        cells = List(25) { -1 },
        winningCells = emptyList()
    )

    fun fromJson(json: JSONObject): UltimateDoozState {
        val w = json.optInt("winnerIndex", -99)
        return UltimateDoozState(
            currentTurn = json.optInt("currentTurn", 0),
            winnerIndex = if (w == -99) null else w,
            statusFa = json.optString("statusFa"),
            cells = json.optJSONArray("cells")?.toIntList() ?: List(25) { -1 },
            winningCells = json.optJSONArray("winningCells")?.toIntList().orEmpty()
        )
    }

    fun applyAction(state: UltimateDoozState, playerIndex: Int, action: JSONObject): UltimateDoozState {
        if (state.winnerIndex != null || playerIndex != state.currentTurn) return state
        val cell = action.optInt("cell", -1)
        if (cell !in 0 until 25 || state.cells[cell] != -1) return state

        val updated = state.cells.toMutableList()
        updated[cell] = playerIndex

        val winLine = WIN_LINES.firstOrNull { line -> line.all { updated[it] == playerIndex } }
        if (winLine != null) {
            return state.copy(
                cells = updated,
                winnerIndex = playerIndex,
                winningCells = winLine,
                statusFa = "دوز ۴ تایی کامل شد! 🏆 برنده: بازیکن ${playerIndex + 1}"
            )
        }
        if (updated.none { it == -1 }) {
            return state.copy(
                cells = updated,
                winnerIndex = -1,
                statusFa = "بازی مساوی شد! 🤝"
            )
        }
        val next = 1 - playerIndex
        return state.copy(
            cells = updated,
            currentTurn = next,
            statusFa = "نوبت بازیکن ${next + 1} (${if (next == 0) "X" else "O"})"
        )
    }

    fun botAction(state: UltimateDoozState): String? {
        val empty = state.cells.indices.filter { state.cells[it] == -1 }
        if (empty.isEmpty()) return null
        val me = state.currentTurn
        val opp = 1 - me
        val best = empty.maxByOrNull { idx ->
            var score = if (idx == 12) 15 else Random.nextInt(4)
            for (line in WIN_LINES) {
                if (idx !in line) continue
                val mine = line.count { state.cells[it] == me }
                val theirs = line.count { state.cells[it] == opp }
                if (theirs == 0) score += when (mine) { 3 -> 500; 2 -> 35; 1 -> 8; else -> 2 }
                if (mine == 0) score += when (theirs) { 3 -> 400; 2 -> 25; else -> 1 }
            }
            score
        } ?: empty.first()
        return JSONObject().put("cell", best).toString()
    }
}

// ============================================================================
// 5. CONNECT FOUR (چهار در یک ردیف - 7x6)
// ============================================================================
data class ConnectFourState(
    override val playerCount: Int = 2,
    override val currentTurn: Int,
    override val winnerIndex: Int?,
    override val statusFa: String,
    val grid: List<Int>, // 6 rows x 7 cols = 42 (-1 empty, 0 P0, 1 P1)
    val winningCells: List<Int>,
    val lastDroppedCell: Int?
) : ActiveGameState {
    override val gameType: GameType = GameType.CONNECT_FOUR

    override fun toJson(): String = JSONObject().apply {
        put("currentTurn", currentTurn)
        put("winnerIndex", winnerIndex ?: -99)
        put("statusFa", statusFa)
        put("grid", grid.toJsonArray())
        put("winningCells", winningCells.toJsonArray())
        put("lastDroppedCell", lastDroppedCell ?: -1)
    }.toString()
}

object ConnectFourEngine {
    const val ROWS = 6
    const val COLS = 7

    fun create(): ConnectFourState = ConnectFourState(
        currentTurn = 0,
        winnerIndex = null,
        statusFa = "یک ستون انتخاب کنید تا سکه سقوط کند — نوبت بازیکن ۱",
        grid = List(ROWS * COLS) { -1 },
        winningCells = emptyList(),
        lastDroppedCell = null
    )

    fun fromJson(json: JSONObject): ConnectFourState {
        val w = json.optInt("winnerIndex", -99)
        val ld = json.optInt("lastDroppedCell", -1)
        return ConnectFourState(
            currentTurn = json.optInt("currentTurn", 0),
            winnerIndex = if (w == -99) null else w,
            statusFa = json.optString("statusFa"),
            grid = json.optJSONArray("grid")?.toIntList() ?: List(ROWS * COLS) { -1 },
            winningCells = json.optJSONArray("winningCells")?.toIntList().orEmpty(),
            lastDroppedCell = if (ld == -1) null else ld
        )
    }

    private fun findWin(grid: List<Int>, player: Int): List<Int>? {
        val dirs = listOf(0 to 1, 1 to 0, 1 to 1, 1 to -1)
        for (r in 0 until ROWS) {
            for (c in 0 until COLS) {
                if (grid[r * COLS + c] != player) continue
                for ((dr, dc) in dirs) {
                    val line = mutableListOf(r * COLS + c)
                    for (step in 1..3) {
                        val nr = r + dr * step
                        val nc = c + dc * step
                        if (nr in 0 until ROWS && nc in 0 until COLS && grid[nr * COLS + nc] == player) {
                            line.add(nr * COLS + nc)
                        } else break
                    }
                    if (line.size == 4) return line
                }
            }
        }
        return null
    }

    fun applyAction(state: ConnectFourState, playerIndex: Int, action: JSONObject): ConnectFourState {
        if (state.winnerIndex != null || playerIndex != state.currentTurn) return state
        val col = action.optInt("col", -1)
        if (col !in 0 until COLS) return state
        val dropRow = (ROWS - 1 downTo 0).firstOrNull { r -> state.grid[r * COLS + col] == -1 } ?: return state

        val updated = state.grid.toMutableList()
        val cell = dropRow * COLS + col
        updated[cell] = playerIndex

        val win = findWin(updated, playerIndex)
        if (win != null) {
            return state.copy(
                grid = updated,
                winnerIndex = playerIndex,
                winningCells = win,
                lastDroppedCell = cell,
                statusFa = "۴ سکه در یک ردیف! 🏆 برنده: بازیکن ${playerIndex + 1}"
            )
        }
        if (updated.none { it == -1 }) {
            return state.copy(
                grid = updated,
                winnerIndex = -1,
                lastDroppedCell = cell,
                statusFa = "صفحه پر شد — مساوی! 🤝"
            )
        }
        val next = 1 - playerIndex
        return state.copy(
            grid = updated,
            currentTurn = next,
            lastDroppedCell = cell,
            statusFa = "نوبت بازیکن ${next + 1}"
        )
    }

    fun botAction(state: ConnectFourState): String? {
        val validCols = (0 until COLS).filter { state.grid[it] == -1 }
        if (validCols.isEmpty()) return null
        val me = state.currentTurn
        val opp = 1 - me
        // Check winning move
        for (c in validCols) {
            val r = (ROWS - 1 downTo 0).first { state.grid[it * COLS + c] == -1 }
            val copy = state.grid.toMutableList().also { it[r * COLS + c] = me }
            if (findWin(copy, me) != null) return JSONObject().put("col", c).toString()
        }
        // Check block move
        for (c in validCols) {
            val r = (ROWS - 1 downTo 0).first { state.grid[it * COLS + c] == -1 }
            val copy = state.grid.toMutableList().also { it[r * COLS + c] = opp }
            if (findWin(copy, opp) != null) return JSONObject().put("col", c).toString()
        }
        val preferred = validCols.sortedBy { kotlin.math.abs(it - 3) + Random.nextInt(2) }
        return JSONObject().put("col", preferred.first()).toString()
    }
}

// ============================================================================
// 6. OTHELLO / REVERSI (اتللو 8x8)
// ============================================================================
data class OthelloState(
    override val playerCount: Int = 2,
    override val currentTurn: Int,
    override val winnerIndex: Int?,
    override val statusFa: String,
    val board: List<Int> // 64 cells: -1 empty, 0 P0, 1 P1
) : ActiveGameState {
    override val gameType: GameType = GameType.OTHELLO

    override fun toJson(): String = JSONObject().apply {
        put("currentTurn", currentTurn)
        put("winnerIndex", winnerIndex ?: -99)
        put("statusFa", statusFa)
        put("board", board.toJsonArray())
    }.toString()
}

object OthelloEngine {
    private val DIRS = listOf(-1 to -1, -1 to 0, -1 to 1, 0 to -1, 0 to 1, 1 to -1, 1 to 0, 1 to 1)

    fun create(): OthelloState {
        val b = MutableList(64) { -1 }
        b[3 * 8 + 3] = 1
        b[3 * 8 + 4] = 0
        b[4 * 8 + 3] = 0
        b[4 * 8 + 4] = 1
        return OthelloState(
            currentTurn = 0,
            winnerIndex = null,
            statusFa = "نوبت بازیکن ۱ — مهره‌های حریف را محاصره کنید",
            board = b
        )
    }

    fun fromJson(json: JSONObject): OthelloState {
        val w = json.optInt("winnerIndex", -99)
        return OthelloState(
            currentTurn = json.optInt("currentTurn", 0),
            winnerIndex = if (w == -99) null else w,
            statusFa = json.optString("statusFa"),
            board = json.optJSONArray("board")?.toIntList() ?: create().board
        )
    }

    fun flipsForMove(board: List<Int>, cell: Int, player: Int): List<Int> {
        if (board[cell] != -1) return emptyList()
        val r = cell / 8
        val c = cell % 8
        val opp = 1 - player
        val allFlips = mutableListOf<Int>()
        for ((dr, dc) in DIRS) {
            val line = mutableListOf<Int>()
            var nr = r + dr
            var nc = c + dc
            while (nr in 0..7 && nc in 0..7 && board[nr * 8 + nc] == opp) {
                line.add(nr * 8 + nc)
                nr += dr
                nc += dc
            }
            if (line.isNotEmpty() && nr in 0..7 && nc in 0..7 && board[nr * 8 + nc] == player) {
                allFlips.addAll(line)
            }
        }
        return allFlips
    }

    fun validMoves(board: List<Int>, player: Int): Set<Int> =
        (0 until 64).filter { flipsForMove(board, it, player).isNotEmpty() }.toSet()

    fun applyAction(state: OthelloState, playerIndex: Int, action: JSONObject): OthelloState {
        if (state.winnerIndex != null || playerIndex != state.currentTurn) return state
        val cell = action.optInt("cell", -1)
        if (cell !in 0..63) return state
        val flips = flipsForMove(state.board, cell, playerIndex)
        if (flips.isEmpty()) return state

        val updated = state.board.toMutableList()
        updated[cell] = playerIndex
        flips.forEach { updated[it] = playerIndex }

        val next = 1 - playerIndex
        val nextValid = validMoves(updated, next)
        val selfValid = validMoves(updated, playerIndex)

        if (nextValid.isEmpty() && selfValid.isEmpty()) {
            val c0 = updated.count { it == 0 }
            val c1 = updated.count { it == 1 }
            val winner = when {
                c0 > c1 -> 0
                c1 > c0 -> 1
                else -> -1
            }
            return state.copy(
                board = updated,
                winnerIndex = winner,
                statusFa = if (winner == -1) "پایان اتللو: مساوی ($c0 - $c1) 🤝"
                else "برنده اتللو: بازیکن ${winner + 1} ($c0 در برابر $c1) 🏆"
            )
        }
        return if (nextValid.isNotEmpty()) {
            state.copy(
                board = updated,
                currentTurn = next,
                statusFa = "${flips.size} مهره برگردانده شد! نوبت بازیکن ${next + 1}"
            )
        } else {
            state.copy(
                board = updated,
                currentTurn = playerIndex,
                statusFa = "حریف حرکتی ندارد — دوباره نوبت بازیکن ${playerIndex + 1}!"
            )
        }
    }

    fun botAction(state: OthelloState): String? {
        val valid = validMoves(state.board, state.currentTurn)
        if (valid.isEmpty()) return null
        val corners = setOf(0, 7, 56, 63)
        val best = valid.maxByOrNull { cell ->
            val flips = flipsForMove(state.board, cell, state.currentTurn).size
            val cornerBonus = if (cell in corners) 50 else 0
            flips + cornerBonus + Random.nextInt(3)
        } ?: valid.first()
        return JSONObject().put("cell", best).toString()
    }
}

// ============================================================================
// 7. BATTLESHIP (نبرد دریایی راداری 7x7)
// Cell codes: 0 = unknown water, 1 = hidden ship, 2 = miss, 3 = hit ship
// ============================================================================
data class BattleshipState(
    override val playerCount: Int = 2,
    override val currentTurn: Int,
    override val winnerIndex: Int?,
    override val statusFa: String,
    val boardP0: List<Int>, // 49 cells (P0's fleet)
    val boardP1: List<Int>, // 49 cells (P1's fleet)
    val lastShotCell: Int?
) : ActiveGameState {
    override val gameType: GameType = GameType.BATTLESHIP

    override fun toJson(): String = JSONObject().apply {
        put("currentTurn", currentTurn)
        put("winnerIndex", winnerIndex ?: -99)
        put("statusFa", statusFa)
        put("boardP0", boardP0.toJsonArray())
        put("boardP1", boardP1.toJsonArray())
        put("lastShotCell", lastShotCell ?: -1)
    }.toString()
}

object BattleshipEngine {
    const val SIZE = 7
    private val FLEET_SIZES = listOf(4, 3, 2, 2) // 11 ship segments total

    private fun generateRandomFleet(): List<Int> {
        val board = MutableList(SIZE * SIZE) { 0 }
        for (len in FLEET_SIZES) {
            var placed = false
            var tries = 0
            while (!placed && tries < 200) {
                tries++
                val horizontal = Random.nextBoolean()
                val r = Random.nextInt(SIZE)
                val c = Random.nextInt(SIZE)
                val coords = (0 until len).map { step ->
                    if (horizontal) r to (c + step) else (r + step) to c
                }
                if (coords.all { (rr, cc) -> rr in 0 until SIZE && cc in 0 until SIZE && board[rr * SIZE + cc] == 0 }) {
                    coords.forEach { (rr, cc) -> board[rr * SIZE + cc] = 1 }
                    placed = true
                }
            }
        }
        return board
    }

    fun create(): BattleshipState = BattleshipState(
        currentTurn = 0,
        winnerIndex = null,
        statusFa = "ناوگان‌ها مستقر شدند! نوبت شلیک بازیکن ۱ 🎯",
        boardP0 = generateRandomFleet(),
        boardP1 = generateRandomFleet(),
        lastShotCell = null
    )

    fun fromJson(json: JSONObject): BattleshipState {
        val w = json.optInt("winnerIndex", -99)
        val ls = json.optInt("lastShotCell", -1)
        return BattleshipState(
            currentTurn = json.optInt("currentTurn", 0),
            winnerIndex = if (w == -99) null else w,
            statusFa = json.optString("statusFa"),
            boardP0 = json.optJSONArray("boardP0")?.toIntList() ?: generateRandomFleet(),
            boardP1 = json.optJSONArray("boardP1")?.toIntList() ?: generateRandomFleet(),
            lastShotCell = if (ls == -1) null else ls
        )
    }

    fun applyAction(state: BattleshipState, playerIndex: Int, action: JSONObject): BattleshipState {
        if (state.winnerIndex != null || playerIndex != state.currentTurn) return state
        val cell = action.optInt("cell", -1)
        if (cell !in 0 until SIZE * SIZE) return state

        val targetBoard = (if (playerIndex == 0) state.boardP1 else state.boardP0).toMutableList()
        val curVal = targetBoard[cell]
        if (curVal == 2 || curVal == 3) return state // already shot

        val isHit = curVal == 1
        targetBoard[cell] = if (isHit) 3 else 2

        val newP0 = if (playerIndex == 0) state.boardP0 else targetBoard
        val newP1 = if (playerIndex == 0) targetBoard else state.boardP1

        val remainingShips = targetBoard.count { it == 1 }
        if (remainingShips == 0) {
            return state.copy(
                boardP0 = newP0,
                boardP1 = newP1,
                winnerIndex = playerIndex,
                lastShotCell = cell,
                statusFa = "تمامی ناوگان دشمن غرق شد! ⚓ پیروزی بازیکن ${playerIndex + 1} 🏆"
            )
        }
        val next = if (isHit) playerIndex else 1 - playerIndex
        val msg = if (isHit) {
            "اصابت مستقیم به کشتی دشمن! 💥 ($remainingShips قطعه باقی‌مانده) — شلیک دوباره!"
        } else {
            "شلیک به آب خورد 🌊 — نوبت بازیکن ${next + 1}"
        }
        return state.copy(
            boardP0 = newP0,
            boardP1 = newP1,
            currentTurn = next,
            lastShotCell = cell,
            statusFa = msg
        )
    }

    fun botAction(state: BattleshipState): String? {
        val target = if (state.currentTurn == 0) state.boardP1 else state.boardP0
        val unshot = target.indices.filter { target[it] == 0 || target[it] == 1 }
        if (unshot.isEmpty()) return null
        // Hunt neighbors of existing hits
        val hits = target.indices.filter { target[it] == 3 }
        val huntCandidates = mutableListOf<Int>()
        for (h in hits) {
            val r = h / SIZE
            val c = h % SIZE
            listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1).forEach { (dr, dc) ->
                val nr = r + dr
                val nc = c + dc
                if (nr in 0 until SIZE && nc in 0 until SIZE) {
                    val idx = nr * SIZE + nc
                    if (idx in unshot) huntCandidates.add(idx)
                }
            }
        }
        val chosen = if (huntCandidates.isNotEmpty()) huntCandidates.random() else unshot.random()
        return JSONObject().put("cell", chosen).toString()
    }
}

// ============================================================================
// 8. HEX PATH (مسیر شش‌ضلعی 7x7)
// Player 0 (Red): Connect TOP row (r=0) to BOTTOM row (r=6)
// Player 1 (Cyan): Connect LEFT col (c=0) to RIGHT col (c=6)
// ============================================================================
data class HexState(
    override val playerCount: Int = 2,
    override val currentTurn: Int,
    override val winnerIndex: Int?,
    override val statusFa: String,
    val cells: List<Int>, // 49 cells: -1 empty, 0 P0, 1 P1
    val winningPath: List<Int>
) : ActiveGameState {
    override val gameType: GameType = GameType.HEX_PATH

    override fun toJson(): String = JSONObject().apply {
        put("currentTurn", currentTurn)
        put("winnerIndex", winnerIndex ?: -99)
        put("statusFa", statusFa)
        put("cells", cells.toJsonArray())
        put("winningPath", winningPath.toJsonArray())
    }.toString()
}

object HexEngine {
    const val SIZE = 7
    private val HEX_DIRS = listOf(-1 to 0, -1 to 1, 0 to -1, 0 to 1, 1 to -1, 1 to 0)

    fun create(): HexState = HexState(
        currentTurn = 0,
        winnerIndex = null,
        statusFa = "قرمز: بالا به پایین | فیروزه‌ای: چپ به راست — نوبت بازیکن ۱",
        cells = List(SIZE * SIZE) { -1 },
        winningPath = emptyList()
    )

    fun fromJson(json: JSONObject): HexState {
        val w = json.optInt("winnerIndex", -99)
        return HexState(
            currentTurn = json.optInt("currentTurn", 0),
            winnerIndex = if (w == -99) null else w,
            statusFa = json.optString("statusFa"),
            cells = json.optJSONArray("cells")?.toIntList() ?: List(SIZE * SIZE) { -1 },
            winningPath = json.optJSONArray("winningPath")?.toIntList().orEmpty()
        )
    }

    private fun findWinningPath(cells: List<Int>, player: Int): List<Int>? {
        val parent = IntArray(SIZE * SIZE) { -2 }
        val queue = ArrayDeque<Int>()

        if (player == 0) {
            for (c in 0 until SIZE) {
                val idx = c
                if (cells[idx] == 0) {
                    parent[idx] = -1
                    queue.add(idx)
                }
            }
        } else {
            for (r in 0 until SIZE) {
                val idx = r * SIZE
                if (cells[idx] == 1) {
                    parent[idx] = -1
                    queue.add(idx)
                }
            }
        }

        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            val r = cur / SIZE
            val c = cur % SIZE
            val reached = (player == 0 && r == SIZE - 1) || (player == 1 && c == SIZE - 1)
            if (reached) {
                val path = mutableListOf<Int>()
                var node = cur
                while (node != -1) {
                    path.add(node)
                    node = parent[node]
                }
                return path
            }
            for ((dr, dc) in HEX_DIRS) {
                val nr = r + dr
                val nc = c + dc
                if (nr in 0 until SIZE && nc in 0 until SIZE) {
                    val next = nr * SIZE + nc
                    if (cells[next] == player && parent[next] == -2) {
                        parent[next] = cur
                        queue.add(next)
                    }
                }
            }
        }
        return null
    }

    fun applyAction(state: HexState, playerIndex: Int, action: JSONObject): HexState {
        if (state.winnerIndex != null || playerIndex != state.currentTurn) return state
        val cell = action.optInt("cell", -1)
        if (cell !in 0 until SIZE * SIZE || state.cells[cell] != -1) return state

        val updated = state.cells.toMutableList()
        updated[cell] = playerIndex
        val path = findWinningPath(updated, playerIndex)
        if (path != null) {
            return state.copy(
                cells = updated,
                winnerIndex = playerIndex,
                winningPath = path,
                statusFa = "مسیر طلایی متصل شد! 🏆 برنده: بازیکن ${playerIndex + 1}"
            )
        }
        val next = 1 - playerIndex
        return state.copy(
            cells = updated,
            currentTurn = next,
            statusFa = "نوبت بازیکن ${next + 1} (${if (next == 0) "قرمز: بالا به پایین" else "فیروزه‌ای: چپ به راست"})"
        )
    }

    fun botAction(state: HexState): String? {
        val empty = state.cells.indices.filter { state.cells[it] == -1 }
        if (empty.isEmpty()) return null
        val me = state.currentTurn
        // Prefer center + adjacent to own cells
        val best = empty.maxByOrNull { idx ->
            val r = idx / SIZE
            val c = idx % SIZE
            var score = (6 - kotlin.math.abs(r - 3) - kotlin.math.abs(c - 3)) * 2 + Random.nextInt(5)
            for ((dr, dc) in HEX_DIRS) {
                val nr = r + dr
                val nc = c + dc
                if (nr in 0 until SIZE && nc in 0 until SIZE) {
                    val v = state.cells[nr * SIZE + nc]
                    if (v == me) score += 10
                    else if (v == 1 - me) score += 5
                }
            }
            score
        } ?: empty.first()
        return JSONObject().put("cell", best).toString()
    }
}

package com.example.games

import com.example.model.GameType
import org.json.JSONArray
import org.json.JSONObject

sealed interface ActiveGameState {
    val gameType: GameType
    val playerCount: Int
    val currentTurn: Int
    val winnerIndex: Int? // null = ongoing, -1 = draw, 0..3 = player index
    val statusFa: String

    fun toJson(): String
}

object JsonHelpers {
    fun List<Int>.toJsonArray(): JSONArray = JSONArray().also { arr -> forEach { arr.put(it) } }

    fun JSONArray.toIntList(): List<Int> = List(length()) { optInt(it) }

    fun List<Boolean>.toBoolJsonArray(): JSONArray = JSONArray().also { arr -> forEach { arr.put(it) } }

    fun JSONArray.toBoolList(): List<Boolean> = List(length()) { optBoolean(it) }

    fun List<List<Int>>.toNestedJsonArray(): JSONArray = JSONArray().also { outer ->
        forEach { inner -> outer.put(inner.toJsonArray()) }
    }

    fun JSONArray.toNestedIntList(): List<List<Int>> = List(length()) { i ->
        optJSONArray(i)?.toIntList().orEmpty()
    }
}

object GameEngineDispatcher {
    fun createInitialState(gameType: GameType, playerCount: Int): ActiveGameState {
        val clamped = playerCount.coerceIn(gameType.minPlayers, gameType.maxPlayers)
        return when (gameType) {
            GameType.LUDO -> LudoEngine.create(clamped)
            GameType.SNAKES_LADDERS -> SnakesLaddersEngine.create(clamped)
            GameType.CHECKERS -> CheckersEngine.create()
            GameType.ULTIMATE_DOOZ -> UltimateDoozEngine.create()
            GameType.CONNECT_FOUR -> ConnectFourEngine.create()
            GameType.OTHELLO -> OthelloEngine.create()
            GameType.BATTLESHIP -> BattleshipEngine.create()
            GameType.HEX_PATH -> HexEngine.create()
            GameType.DOTS_BOXES -> DotsBoxesEngine.create(clamped)
            GameType.MEMORY_MATCH -> MemoryMatchEngine.create(clamped)
            GameType.PIG_DICE -> PigDiceEngine.create(clamped)
            GameType.REFLEX_DUEL -> ReflexDuelEngine.create(clamped)
        }
    }

    fun deserialize(gameType: GameType, jsonStr: String): ActiveGameState? {
        return try {
            val json = JSONObject(jsonStr)
            when (gameType) {
                GameType.LUDO -> LudoEngine.fromJson(json)
                GameType.SNAKES_LADDERS -> SnakesLaddersEngine.fromJson(json)
                GameType.CHECKERS -> CheckersEngine.fromJson(json)
                GameType.ULTIMATE_DOOZ -> UltimateDoozEngine.fromJson(json)
                GameType.CONNECT_FOUR -> ConnectFourEngine.fromJson(json)
                GameType.OTHELLO -> OthelloEngine.fromJson(json)
                GameType.BATTLESHIP -> BattleshipEngine.fromJson(json)
                GameType.HEX_PATH -> HexEngine.fromJson(json)
                GameType.DOTS_BOXES -> DotsBoxesEngine.fromJson(json)
                GameType.MEMORY_MATCH -> MemoryMatchEngine.fromJson(json)
                GameType.PIG_DICE -> PigDiceEngine.fromJson(json)
                GameType.REFLEX_DUEL -> ReflexDuelEngine.fromJson(json)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun applyAction(state: ActiveGameState, playerIndex: Int, actionJson: String): ActiveGameState {
        val action = try {
            JSONObject(actionJson)
        } catch (_: Exception) {
            return state
        }
        return when (state) {
            is LudoState -> LudoEngine.applyAction(state, playerIndex, action)
            is SnakesLaddersState -> SnakesLaddersEngine.applyAction(state, playerIndex, action)
            is CheckersState -> CheckersEngine.applyAction(state, playerIndex, action)
            is UltimateDoozState -> UltimateDoozEngine.applyAction(state, playerIndex, action)
            is ConnectFourState -> ConnectFourEngine.applyAction(state, playerIndex, action)
            is OthelloState -> OthelloEngine.applyAction(state, playerIndex, action)
            is BattleshipState -> BattleshipEngine.applyAction(state, playerIndex, action)
            is HexState -> HexEngine.applyAction(state, playerIndex, action)
            is DotsBoxesState -> DotsBoxesEngine.applyAction(state, playerIndex, action)
            is MemoryMatchState -> MemoryMatchEngine.applyAction(state, playerIndex, action)
            is PigDiceState -> PigDiceEngine.applyAction(state, playerIndex, action)
            is ReflexDuelState -> ReflexDuelEngine.applyAction(state, playerIndex, action)
        }
    }

    fun computeBotAction(state: ActiveGameState): String? {
        if (state.winnerIndex != null) return null
        return when (state) {
            is LudoState -> LudoEngine.botAction(state)
            is SnakesLaddersState -> SnakesLaddersEngine.botAction(state)
            is CheckersState -> CheckersEngine.botAction(state)
            is UltimateDoozState -> UltimateDoozEngine.botAction(state)
            is ConnectFourState -> ConnectFourEngine.botAction(state)
            is OthelloState -> OthelloEngine.botAction(state)
            is BattleshipState -> BattleshipEngine.botAction(state)
            is HexState -> HexEngine.botAction(state)
            is DotsBoxesState -> DotsBoxesEngine.botAction(state)
            is MemoryMatchState -> MemoryMatchEngine.botAction(state)
            is PigDiceState -> PigDiceEngine.botAction(state)
            is ReflexDuelState -> ReflexDuelEngine.botAction(state)
        }
    }
}

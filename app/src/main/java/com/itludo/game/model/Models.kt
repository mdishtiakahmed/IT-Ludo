package com.itludo.game.model

enum class Player(val color: Long) { // Using Long for Color(0xFF...) representation to keep it UI-agnostic here if needed, or just map in UI
    RED(0xFFFF0000),
    GREEN(0xFF00FF00),
    YELLOW(0xFFFFFF00),
    BLUE(0xFF0000FF)
}

enum class TokenState {
    BASE,
    ACTIVE,
    HOME
}

data class Token(
    val id: Int,
    val player: Player,
    val position: Int = -1, // -1 for Base, 0-56 for Path (51-56 is home stretch)
    val state: TokenState = TokenState.BASE,
    val completed: Boolean = false
)

data class GameState(
    val tokens: List<Token>,
    val currentPlayer: Player,
    val diceValue: Int,
    val isRollAllowed: Boolean,
    val winner: Player? = null,
    val lastRolledValue: Int = 0, // For display
    val message: String = "Roll the Dice!",
    val playableTokenIds: List<Int> = emptyList()
) {
    companion object {
        fun initial(): GameState {
            val tokens = mutableListOf<Token>()
            var idCounter = 0
            Player.values().forEach { player ->
                repeat(4) {
                    tokens.add(Token(idCounter++, player))
                }
            }
            return GameState(
                tokens = tokens,
                currentPlayer = Player.RED, // Red starts usually
                diceValue = 1,
                isRollAllowed = true
            )
        }
    }
}

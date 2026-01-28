package com.itludo.game.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itludo.game.model.GameState
import com.itludo.game.model.Player
import com.itludo.game.model.Token
import com.itludo.game.model.TokenState
import com.itludo.game.utils.BoardUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(GameState.initial())
    val uiState: StateFlow<GameState> = _uiState.asStateFlow()

    private var isAnimating = false

    fun rollDice() {
        if (isAnimating || !_uiState.value.isRollAllowed) return

        viewModelScope.launch {
            isAnimating = true
            // Animate Dice "Rolling effect"
            repeat(10) {
                _uiState.update { it.copy(diceValue = (1..6).random()) }
                delay(100)
            }
            val finalValue = (1..6).random()
            
            _uiState.update { state -> 
                state.copy(
                    diceValue = finalValue,
                    isRollAllowed = false,
                    lastRolledValue = finalValue,
                    message = "${state.currentPlayer.name} rolled a $finalValue"
                )
            }
            
            checkAutoTurnPass(finalValue)
            isAnimating = false
        }
    }
    
    private fun checkAutoTurnPass(diceVal: Int) {
        val state = _uiState.value
        val playerTokens = state.tokens.filter { it.player == state.currentPlayer }
        
        // Check moves
        val canMove = playerTokens.any { token ->
            canMoveToken(token, diceVal)
        }
        
        if (!canMove) {
            viewModelScope.launch {
                _uiState.update { it.copy(message = "No moves! Passing turn...") }
                delay(1000)
                nextTurn()
            }
        }
    }

    private fun canMoveToken(token: Token, diceVal: Int): Boolean {
        if (token.completed) return false
        if (token.state == TokenState.BASE) return diceVal == 6
        // Check overflow home
        if (token.position + diceVal > 56) return false
        return true
    }

    fun onTokenClick(token: Token) {
        if (isAnimating) return
        val state = _uiState.value
        if (state.isRollAllowed) return // Must roll first
        if (token.player != state.currentPlayer) return
        
        val diceVal = state.lastRolledValue
        if (!canMoveToken(token, diceVal)) return

        moveToken(token, diceVal)
    }

    private fun moveToken(token: Token, steps: Int) {
        viewModelScope.launch {
            isAnimating = true
            
            // Logic to update position
            val oldPos = token.position
            var newPos = oldPos
            var newState = token.state
            
            if (token.state == TokenState.BASE) {
                if (steps == 6) {
                    newPos = 0
                    newState = TokenState.ACTIVE
                }
            } else {
                newPos += steps
            }
            
            // Winning Condition check
            val isCompleted = newPos == 56 // Center / Home
            if (isCompleted) {
                newState = TokenState.HOME
            }

            // Update the single token in the list
            // We need to first update state so UI reflects move
            var currentTokens = _uiState.value.tokens.map { 
                if (it.id == token.id) it.copy(position = newPos, state = newState, completed = isCompleted) else it
            }
            
            // KILLING LOGIC
            val finalTokens = handleCollisions(currentTokens, token.player, newPos)
            
            _uiState.update { it.copy(tokens = finalTokens) }

            // Determine next turn
            // Bonus rule: Roll 6 gives extra turn.
            if (steps == 6) {
                 _uiState.update { it.copy(isRollAllowed = true, message = "Bonus Roll!") }
            } else {
                // Determine if we still have moves? No, std ludo one move per roll.
                nextTurn()
            }
            
            isAnimating = false
        }
    }
    
    private fun handleCollisions(allTokens: List<Token>, currentPlayer: Player, playerLogPos: Int): List<Token> {
        if (playerLogPos < 0 || playerLogPos > 55) return allTokens
        
        val (gridX, gridY) = BoardUtils.getCoordinates(currentPlayer, playerLogPos)
        
        if (BoardUtils.isSafeSquare(gridX, gridY)) return allTokens
        
        return allTokens.map { target ->
            if (target.player != currentPlayer && target.state == TokenState.ACTIVE) {
                val (tX, tY) = BoardUtils.getCoordinates(target.player, target.position)
                if (tX == gridX && tY == gridY) {
                    // KILL!
                    target.copy(position = -1, state = TokenState.BASE)
                } else {
                    target
                }
            } else {
                target
            }
        }
    }

    private fun nextTurn() {
        _uiState.update { state ->
            val nextPlayer = when (state.currentPlayer) {
                Player.RED -> Player.GREEN
                Player.GREEN -> Player.YELLOW
                Player.YELLOW -> Player.BLUE
                Player.BLUE -> Player.RED
            }
            state.copy(
                currentPlayer = nextPlayer,
                isRollAllowed = true,
                message = "${nextPlayer.name}'s Turn"
            )
        }
    }
}

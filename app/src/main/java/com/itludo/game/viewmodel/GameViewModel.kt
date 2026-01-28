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
            _uiState.update { it.copy(message = "Rolling...") }
            
            // Animate Dice "Rolling effect"
            repeat(12) {
                _uiState.update { it.copy(diceValue = (1..6).random()) }
                delay(80)
            }
            val finalValue = (1..6).random()
            // DEBUG: Uncomment to force specific rolls for testing
            // val finalValue = 6 
            
            _uiState.update { state -> 
                state.copy(
                    diceValue = finalValue,
                    isRollAllowed = false,
                    lastRolledValue = finalValue,
                    message = "${state.currentPlayer.name} rolled a $finalValue"
                )
            }
            
            // Calculate valid moves immediately after roll
            val playableIds = calculatePlayableTokens(finalValue)
            _uiState.update { it.copy(playableTokenIds = playableIds) }
            
            if (playableIds.isEmpty()) {
                delay(1000)
                _uiState.update { it.copy(message = "No moves possible!", playableTokenIds = emptyList()) }
                delay(1000)
                nextTurn()
            } else {
                // If only one move is possible and it's not a decision (rare in Ludo, usually you pick), 
                // we could auto-move, but Ludo usually requires user tap even if 1 option.
                 _uiState.update { it.copy(message = "Select a token to move") }
                 isAnimating = false // User input needed
            }
        }
    }

    private fun calculatePlayableTokens(diceVal: Int): List<Int> {
        val state = _uiState.value
        return state.tokens.filter { token ->
            token.player == state.currentPlayer && canMoveToken(token, diceVal)
        }.map { it.id }
    }

    private fun canMoveToken(token: Token, diceVal: Int): Boolean {
        if (token.completed) return false
        if (token.state == TokenState.BASE) return diceVal == 6
        // Check overflow home. Max steps = 56 (Home)
        // Position 0..56.
        // If current pos + dice > 56, cannot move.
        // Note: Position -1 is BASE.
        if (token.state == TokenState.ACTIVE) {
            if (token.position + diceVal > 56) return false
        }
        return true
    }

    fun onTokenClick(token: Token) {
        if (isAnimating) return
        val state = _uiState.value
        if (state.isRollAllowed) return // Must roll first
        
        // Validation: Is this token in the playable list?
        if (!state.playableTokenIds.contains(token.id)) return

        moveToken(token, state.lastRolledValue)
    }

    private fun moveToken(token: Token, totalSteps: Int) {
        viewModelScope.launch {
            isAnimating = true
            _uiState.update { it.copy(playableTokenIds = emptyList()) } // Clear highlights
            
            // STEP-BY-STEP MOVEMENT ANIMATION
            var currentPos = token.position
            var currentState = token.state
            
            // 1. Handle Base Exit rule separately (First step)
            var stepsRemaining = totalSteps
            
            if (currentState == TokenState.BASE) {
                // Exit Base
                currentState = TokenState.ACTIVE
                currentPos = 0 // Start point
                updateTokenState(token.id, currentPos, currentState, false)
                delay(250) // Initial hop out
                stepsRemaining = 0 // 6 just brings it out to start, usually doesn't give +6 moves unless house rules. 
                                   // Standard rule: 6 opens the token to Start Cell. It does NOT move 6 extra steps immediately.
                                   // "Token opens only on rolling a 6." -> Placed at start.
                                   // Wait, usually rolling 6 gives another turn.
                                   // So if I roll 6:
                                   // Option A: Move Active Token 6 steps.
                                   // Option B: Open Base Token to Start.
                                   // We are doing B here.
            } else {
                // Walk the steps
                repeat(stepsRemaining) {
                    currentPos++
                    updateTokenState(token.id, currentPos, currentState, false)
                    // Play sound effect hook here if we had one
                    delay(250) // Step delay
                }
            }
            
            // Check Completion
            val isCompleted = currentPos == 56
            if (isCompleted) {
                currentState = TokenState.HOME
                updateTokenState(token.id, currentPos, currentState, isCompleted)
                // Celebrate!
                _uiState.update { it.copy(message = "HOME!") }
                delay(500)
            }

            // Killing Logic check (Only at final destination)
            if (currentState == TokenState.ACTIVE && !isCompleted) {
                 handleCollisions(token.player, currentPos)
            }
             
            // Check Win Condition (All 4 tokens home)
            val playerTokens = _uiState.value.tokens.filter { it.player == token.player }
            if (playerTokens.all { it.completed }) {
                _uiState.update { it.copy(winner = token.player, message = "${token.player} WINS!") }
                isAnimating = false
                return@launch
            }

            // Next Turn Logic
            // Bonus: 6 gives extra turn. Killing gives extra turn (Optional, let's keep it simple: 6 only).
            // Also if we just opened a token (6), we get extra turn.
            if (totalSteps == 6) {
                 _uiState.update { it.copy(
                     isRollAllowed = true, 
                     message = "${token.player} Bonus Roll!",
                     diceValue = 0 // Reset visual
                 ) }
            } else {
                nextTurn()
            }
            
            isAnimating = false
        }
    }

    private fun updateTokenState(tokenId: Int, newPos: Int, newState: TokenState, completed: Boolean) {
        _uiState.update { state ->
            val newTokens = state.tokens.map { 
                if (it.id == tokenId) it.copy(position = newPos, state = newState, completed = completed) else it
            }
            state.copy(tokens = newTokens)
        }
    }
    
    private fun handleCollisions(currentPlayer: Player, gridPos: Int) {
        // Find collisions at this LOGICAL position? 
        // No, we must map to GRID position, because different players share grid cells at different logical indices.
        // Except for Home Stretch where they act distinct?
        // Actually, BoardUtils collision check relies on Grid Coords.
        
        val (gridX, gridY) = BoardUtils.getCoordinates(currentPlayer, gridPos)
        
        if (BoardUtils.isSafeSquare(gridX, gridY)) return

        val state = _uiState.value
        var killedAny = false
        
        val newTokens = state.tokens.map { target ->
            if (target.player != currentPlayer && target.state == TokenState.ACTIVE) {
                 val (tX, tY) = BoardUtils.getCoordinates(target.player, target.position)
                 if (tX == gridX && tY == gridY) {
                     // KILL
                     killedAny = true
                     target.copy(position = -1, state = TokenState.BASE)
                 } else {
                     target
                 }
            } else {
                target
            }
        }
        
        if (killedAny) {
            _uiState.update { it.copy(tokens = newTokens, message = "CRUSHED!") }
            // If we want bonus turn on kill:
            // forceBonusTurn = true
        } else {
            // just update? We already updated main token.
            // But we might have modified *other* tokens.
            _uiState.update { it.copy(tokens = newTokens) }
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
                message = "${nextPlayer.name}'s Turn",
                diceValue = 0,
                playableTokenIds = emptyList()
            )
        }
    }
}

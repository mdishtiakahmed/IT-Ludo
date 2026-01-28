package com.itludo.game.ui

import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.itludo.game.model.Player
import com.itludo.game.model.Token
import com.itludo.game.viewmodel.GameViewModel
import com.itludo.game.utils.BoardUtils

@Composable
fun GameScreen(
    gameViewModel: GameViewModel = viewModel()
) {
    val state by gameViewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF0F0F0)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Status Bar
        Text(text = state.message, modifier = Modifier.padding(16.dp))
        
        // Game Board Area
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            val boardSize = maxWidth
            val cellSize = boardSize / 15f
            val density = LocalDensity.current
            
            val cellSizePx = with(density) { cellSize.toPx() }
            
            // 1. Draw Board
            LudoBoardCanvas(modifier = Modifier.fillMaxSize())
            
            // 2. Draw Tokens
            // We overlay tokens based on their positions.
            // Using Box with offset? Or Canvas?
            // Using Box with offset allows easier click handling.
            
            // We need to map Token.position to Offset(x, y) relative to Board TopLeft.
            
            state.tokens.forEach { token ->
                val targetOffset = calculateTokenOffset(token, cellSize)
                val animatedOffset by animateOffsetAsState(
                    targetValue = targetOffset,
                    animationSpec = tween(durationMillis = 500),
                    label = "token_move"
                )
                
                // Render Token
                Box(
                    modifier = Modifier
                        .offset(
                            x = with(density) { animatedOffset.x.toDp() },
                            y = with(density) { animatedOffset.y.toDp() }
                        )
                        .size(cellSize) // Token takes up a cell
                ) {
                   TokenComponent(
                       token = token, 
                       cellSize = cellSizePx,
                       onClick = { gameViewModel.onTokenClick(token) }
                   )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Dice and Controls
        DiceComponent(
            value = state.diceValue,
            onRoll = { gameViewModel.rollDice() }
        )
        
        Spacer(modifier = Modifier.height(10.dp))
        Text("Current Turn: ${state.currentPlayer.name}")
    }
}

@Composable
fun calculateTokenOffset(token: Token, cellSize: Dp): Offset {
    val density = LocalDensity.current
    val cellSizePx = with(density) { cellSize.toPx() }
    
    // If in BASE, hardcode positions
    if (token.position == -1) {
        // Map ID to base slot?
        // Token IDs 0-3 RED, 4-7 GREEN etc.
        val baseIndex = token.id % 4
        
        // Base Offsets (Grid indices approx)
        val (baseX, baseY) = when (token.player) {
            Player.RED -> getBaseSlotOps(0, 0, baseIndex)
            Player.GREEN -> getBaseSlotOps(9, 0, baseIndex)
            Player.YELLOW -> getBaseSlotOps(9, 9, baseIndex)
            Player.BLUE -> getBaseSlotOps(0, 9, baseIndex)
        }
        return Offset(baseX * cellSizePx, baseY * cellSizePx)
    }
    
    // If ACTIVE or HOME
    val (gridX, gridY) = BoardUtils.getCoordinates(token.player, token.position)
    return Offset(gridX * cellSizePx, gridY * cellSizePx)
}

fun getBaseSlotOps(houseCol: Int, houseRow: Int, index: Int): Pair<Float, Float> {
    // TopLeft, TopRight, etc within the house (6x6)
    // House Inner White Box starts at (houseCol + 1, houseRow + 1)
    // Size 4x4 roughly.
    // Centers: 
    val pad = 0.5f // centering in cell
    val offX = houseCol + 1 + (if (index % 2 == 0) 1 else 3)
    val offY = houseRow + 1 + (if (index < 2) 1 else 3)
    // Wait, let's just align them nicely.
    // 6x6 house. Inner box 4x4.
    // 1.5, 1.5 relative to house?
    // Let's use simple static offsets:
    // 0: (1.5, 1.5), 1: (4.5, 1.5), 2: (1.5, 4.5), 3: (4.5, 4.5) relative to house start?
    
    // Grid indices for token centers
    val startX = houseCol.toFloat()
    val startY = houseRow.toFloat()
    
    val dx = if (index % 2 == 1) 4f else 1.5f 
    val dy = if (index > 1) 4f else 1.5f
    // Slight adjustment to center in 1.5f cell? 
    // Yes.
    
    return Pair(startX + dx - 0.5f, startY + dy - 0.5f) // Adjust for cell top-left
}

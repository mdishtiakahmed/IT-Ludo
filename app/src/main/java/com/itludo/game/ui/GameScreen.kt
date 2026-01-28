package com.itludo.game.ui

import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
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
        Text(
            text = state.message, 
            modifier = Modifier.padding(16.dp),
            style = androidx.compose.material3.MaterialTheme.typography.headlineSmall
        )
        
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
            
            // 2. Draw Tokens with SMART GROUPING
            // We need to group tokens that are at the exact same logical position.
            // Note: Different players have different Logical Indices for the same physical cell.
            // But we display them based on PHYSICAL coords.
            // So we must Group by (GridX, GridY).
            
            // Map: Pair<GridX, GridY> -> List<Token>
            val tokenGroups = state.tokens.groupBy { token ->
                if (token.position == -1) {
                    // Base tokens handle themselves via getBaseSlotOps
                    // Unique key for base slots?
                    // Actually calculating raw offset for base is fine, they have fixed slots.
                    // Let's use a special key for grouping or just handle active ones.
                    // Base tokens (-1) have distinct visual slots (indices 0-3), so they never visually overlap.
                    // We only care about Path Overlaps.
                    Pair(-1 * token.player.ordinal, token.id) // Unique key to not group base tokens
                } else {
                    BoardUtils.getCoordinates(token.player, token.position)
                }
            }

            tokenGroups.forEach { (key, tokensInCell) ->
                // If Base Tokens (key.first < 0), render normally
                if (key.first < 0) {
                     tokensInCell.forEach { token ->
                        renderToken(token, state, cellSize, cellSizePx, Offset.Zero, density, gameViewModel)
                     }
                     return@forEach
                }

                // Path Tokens: Calculate sub-cell offsets
                val count = tokensInCell.size
                
                tokensInCell.forEachIndexed { index, token ->
                    // Calculate Shift
                    // If 1 token: Center (0,0)
                    // If 2 tokens: (-Shift, 0), (+Shift, 0)
                    // If 3 tokens: Triangle
                    // Token Size is 24.dp. Cell is ~Width/15.
                    // On 1080px width -> Cell is 72px. Token is ~60px.
                    // They are tight. We scale token down slightly in grouping?
                    
                    val scale = if (count > 1) 0.8f else 1f
                    val shift = cellSizePx * 0.2f
                    
                    val subOffset = when (count) {
                        1 -> Offset.Zero
                        2 -> Offset(
                             if (index == 0) -shift / 2 else shift / 2, 
                             if (index == 0) -shift / 2 else shift / 2 // Diagonal separation look better
                        )
                        3 -> {
                             // Triangle
                             // 0: Top, 1: BottomLeft, 2: BottomRight
                             when(index) {
                                 0 -> Offset(0f, -shift * 0.8f)
                                 1 -> Offset(-shift * 0.8f, shift * 0.6f)
                                 else -> Offset(shift * 0.8f, shift * 0.6f)
                             }
                        }
                        else -> { // 4 or more
                            // 2x2 Grid
                            val col = (index % 2).toFloat()
                            val row = (index / 2).toFloat()
                            Offset(
                                (col - 0.5f) * shift * 1.5f,
                                (row - 0.5f) * shift * 1.5f
                            )
                        }
                    }
                    
                    renderToken(token, state, cellSize, cellSizePx, subOffset, density, gameViewModel, scale)
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
        Text(
            text = "Current Turn: ${state.currentPlayer.name}",
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium
        )
        
        // Winner Overlay
        if (state.winner != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .clickable { }, // block touches
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${state.winner} WINS!",
                    color = Color.White,
                    style = androidx.compose.material3.MaterialTheme.typography.displayLarge,
                    modifier = Modifier.padding(20.dp)
                )
            }
        }
    }
}

@Composable
fun renderToken(
    token: Token, 
    state: com.itludo.game.model.GameState, 
    cellSize: Dp, 
    cellSizePx: Float, 
    subOffset: Offset, 
    density: androidx.compose.ui.unit.Density,
    viewModel: GameViewModel,
    scale: Float = 1f
) {
    val targetOffset = calculateTokenOffset(token, cellSize)
    val animatedOffset by animateOffsetAsState(
        targetValue = targetOffset + subOffset, // Apply smart offset here
        animationSpec = tween(durationMillis = 200),
        label = "token_move"
    )
    
    val isPlayable = state.playableTokenIds.contains(token.id)
    val alpha = if (state.playableTokenIds.isNotEmpty() && !isPlayable) 0.6f else 1f
    
    Box(
        modifier = Modifier
            .offset(
                x = with(density) { animatedOffset.x.toDp() },
                y = with(density) { animatedOffset.y.toDp() }
            )
            .size(cellSize * scale) // Scale down if grouped
            .padding(1.dp)
            .alpha(alpha)
    ) {
       TokenComponent(
           token = token, 
           cellSize = cellSizePx * scale,
           isPlayable = isPlayable,
           onClick = { viewModel.onTokenClick(token) }
       )
    }
}



@Composable
fun calculateTokenOffset(token: Token, cellSize: Dp): Offset {
    val density = LocalDensity.current
    val cellSizePx = with(density) { cellSize.toPx() }
    
    // If in BASE, hardcode positions
    if (token.position == -1) {
        val baseIndex = token.id % 4
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
    val startX = houseCol.toFloat()
    val startY = houseRow.toFloat()
    
    val dx = if (index % 2 == 1) 4f else 1.5f 
    val dy = if (index > 1) 4f else 1.5f
    
    return Pair(startX + dx - 0.5f, startY + dy - 0.5f)
}

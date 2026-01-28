package com.itludo.game.ui

import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.itludo.game.model.Player
import com.itludo.game.model.Token

@Composable
fun DiceComponent(
    value: Int,
    modifier: Modifier = Modifier,
    onRoll: () -> Unit
) {
    Box(modifier = modifier.clickable { onRoll() }) {
        Canvas(modifier = Modifier.size(60.dp)) {
            // Draw Cube Body
            drawRoundRect(
                color = Color.White,
                size = size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
            )
            drawRoundRect(
                color = Color.Black,
                size = size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f),
                style = Stroke(width = 4f)
            )

            // Draw Dots based on value
            val radius = size.minDimension / 10
            val center = Offset(size.width / 2, size.height / 2)
            val left = center.x - size.width / 4
            val right = center.x + size.width / 4
            val top = center.y - size.height / 4
            val bottom = center.y + size.height / 4

            fun drawDot(offset: Offset) {
                drawCircle(color = Color.Black, radius = radius, center = offset)
            }

            when (value) {
                1 -> drawDot(center)
                2 -> { drawDot(Offset(left, top)); drawDot(Offset(right, bottom)) }
                3 -> { drawDot(Offset(left, top)); drawDot(center); drawDot(Offset(right, bottom)) }
                4 -> { 
                    drawDot(Offset(left, top)); drawDot(Offset(right, top))
                    drawDot(Offset(left, bottom)); drawDot(Offset(right, bottom)) 
                }
                5 -> {
                    drawDot(Offset(left, top)); drawDot(Offset(right, top))
                    drawDot(center)
                    drawDot(Offset(left, bottom)); drawDot(Offset(right, bottom))
                }
                6 -> {
                    drawDot(Offset(left, top)); drawDot(Offset(right, top))
                    drawDot(Offset(left, center.y)); drawDot(Offset(right, center.y))
                    drawDot(Offset(left, bottom)); drawDot(Offset(right, bottom))
                }
            }
        }
    }
}

@Composable
fun TokenComponent(
    token: Token,
    cellSize: Float,
    onClick: () -> Unit
) {
    // We don't draw position here, we draw the shape. 
    // Position handling is done by parent with animateOffset.
    // This is just the visual representation of the Guti.
    
    Canvas(modifier = Modifier.size(24.dp).clickable { onClick() }) { // Fixed size for touch target, scaling happens in draw
        val colorVal = token.player.color
        val color = Color(colorVal)
        
        // Draw Shadow
        drawCircle(
            color = Color.Black.copy(alpha = 0.3f),
            radius = size.minDimension / 2.2f,
            center = center + Offset(4f, 4f)
        )
        
        // Draw Main Body
        drawCircle(
            color = color,
            radius = size.minDimension / 2.2f,
            center = center
        )
        
        // Draw Inner Highlight (Bevel effect)
        drawCircle(
            color = Color.White.copy(alpha = 0.4f),
            radius = size.minDimension / 4f,
            center = center - Offset(2f, 2f)
        )
        
        // Stroke
        drawCircle(
            color = Color.Black,
            radius = size.minDimension / 2.2f,
            center = center,
            style = Stroke(width = 2f)
        )
    }
}

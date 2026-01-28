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
    isPlayable: Boolean = false,
    onClick: () -> Unit
) {
    Canvas(modifier = Modifier.size(24.dp).clickable { onClick() }) {
        val colorVal = token.player.color
        val baseColor = Color(colorVal)
        val center = Offset(size.width / 2, size.height / 2)

        // Selection Halo (if playable)
        if (isPlayable) {
            drawCircle(
                color = Color.White.copy(alpha = 0.5f),
                radius = size.minDimension / 1.5f,
                center = center,
                style = Stroke(width = 4f)
            )
             drawCircle(
                color = baseColor.copy(alpha = 0.3f),
                radius = size.minDimension / 1.5f,
                center = center
            )
        }

        // Shadow
        drawCircle(
            color = Color.Black.copy(alpha = 0.4f),
            radius = size.minDimension / 2.2f,
            center = center + Offset(4f, 4f)
        )

        // 3D SPHERE EFFECT using Radial Gradient
        // Highlight (White) -> Main Color -> Darker Shadow at edges
        val brush = androidx.compose.ui.graphics.Brush.radialGradient(
            colors = listOf(
                Color.White, // Specular Highlight
                baseColor,   // Main body
                baseColor.copy(alpha = 0.8f),
                Color.Black.copy(alpha = 0.6f) // Dark edge
            ),
            center = center - Offset(size.minDimension / 6, size.minDimension / 6),
            radius = size.minDimension / 1.5f
        )

        drawCircle(
            brush = brush,
            radius = size.minDimension / 2.2f,
            center = center
        )

        // Crisp outline
        drawCircle(
            color = Color.Black.copy(alpha = 0.2f),
            radius = size.minDimension / 2.2f,
            center = center,
            style = Stroke(width = 1f)
        )
    }
}

package com.itludo.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.itludo.game.model.Player
import com.itludo.game.utils.BoardUtils

@Composable
fun LudoBoardCanvas(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.aspectRatio(1f)) {
        val boardSize = size.minDimension
        val cellSize = boardSize / 15f
        
        // Background
        drawRect(color = Color.White, size = size)
        
        // Draw Grid Lines (Optional light grid)
        for (i in 0..15) {
            drawLine(
                color = Color.Gray.copy(alpha = 0.3f),
                start = Offset(i * cellSize, 0f),
                end = Offset(i * cellSize, boardSize),
                strokeWidth = 1f
            )
            drawLine(
                color = Color.Gray.copy(alpha = 0.3f),
                start = Offset(0f, i * cellSize),
                end = Offset(boardSize, i * cellSize),
                strokeWidth = 1f
            )
        }

        // Draw 4 Houses (Quadrants)
        // Red: Top-Left (0,0) to (6,6)
        drawHouse(Color(Player.RED.color), 0, 0, cellSize)
        // Green: Top-Right (9,0) to (15,6)
        drawHouse(Color(Player.GREEN.color), 9, 0, cellSize)
        // Blue: Bottom-Left (0,9) to (6,15)
        drawHouse(Color(Player.BLUE.color), 0, 9, cellSize)
        // Yellow: Bottom-Right (9,9) to (15,15)
        drawHouse(Color(Player.YELLOW.color), 9, 9, cellSize)
        
        // Draw Center Home Triangle Area
        drawCenterHome(cellSize, size)

        // Draw Colored Tracks (Home Stretches)
        // Red Home Stretch: (1, 7) -> (5, 7)
        drawColoredTrack(Color(Player.RED.color), 1, 7, 5, true, cellSize)
        // Green Home Stretch: (7, 1) -> (7, 5) // Vertical
        drawColoredTrack(Color(Player.GREEN.color), 7, 1, 5, false, cellSize)
        // Yellow Home Stretch: (7, 9) -> (7, 13) // Vertical
        drawColoredTrack(Color(Player.YELLOW.color), 7, 9, 5, false, cellSize)
        // Blue Home Stretch: (9, 7) -> (13, 7) // Horizontal ? Wait, Left is Blue usually? 
        // My Map: Red(TL) -> Green(TR) -> Yellow(BR) -> Blue(BL)
        // Blue Home Stretch: (1, 7) No that's Red.
        // It's symmetric. 
        // (1, 7)-(5,7) is Left arm (Red).
        // (9, 7)-(13,7) is Right arm (Yellow/Green?).
        // (7, 1)-(7, 5) is Top arm.
        // (7, 9)-(7, 13) is Bottom arm.
        
        // Fix colors to standard:
        // TL=Red. TL Arm is Red Home Stretch. (Horizontal) -> Cells (1,7) to (5,7).
        // TR=Green. Top Arm is Green Home Stretch. (Vertical) -> (7,1) to (7,5).
        // BR=Yellow. Right Arm is Yellow. (Horizontal) -> (9,7) to (13,7).
        // BL=Blue. Bottom Arm is Blue. (Vertical) -> (7,9) to (7,13).
        
        drawColoredTrack(Color(Player.YELLOW.color), 9, 7, 5, true, cellSize) // Right
        drawColoredTrack(Color(Player.BLUE.color), 7, 9, 5, false, cellSize) // Bottom
        
        // Draw Safe Zone Stars
        BoardUtils.SAFE_GRID_COORDS.forEach { (r, c) ->
            drawStar(
                center = Offset(r * cellSize + cellSize / 2, c * cellSize + cellSize / 2),
                radius = cellSize / 3,
                color = Color.Gray
            )
        }
    }
}

fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHouse(color: Color, startCol: Int, startRow: Int, cellSize: Float) {
    val sizePx = cellSize * 6
    val offset = Offset(startCol * cellSize, startRow * cellSize)
    
    // Gradient Background
    val brush = androidx.compose.ui.graphics.Brush.linearGradient(
        colors = listOf(color.copy(alpha = 0.8f), color),
        start = offset,
        end = offset + Offset(sizePx, sizePx)
    )
    
    // Outer Box
    drawRect(brush = brush, topLeft = offset, size = Size(sizePx, sizePx))
    
    // Inner White Box with shadow effect (Border)
    val paddedSize = sizePx * 0.7f
    val padding = (sizePx - paddedSize) / 2
    
    // Draw Shadow for inner box
    drawRect(
        color = Color.Black.copy(alpha = 0.2f),
        topLeft = offset + Offset(padding + 4f, padding + 4f),
        size = Size(paddedSize, paddedSize)
    )
    
    drawRect(
        color = Color.White, 
        topLeft = offset + Offset(padding, padding), 
        size = Size(paddedSize, paddedSize)
    )
    
    // 4 Token placeholders (Circles)
    // Relative to Inner Box
    val innerStart = offset + Offset(padding, padding)
    val circleRadius = cellSize / 2.8f // Slightly smaller
    
    // Positions: TopLeft, TopRight, etc.
    val boxCell = paddedSize / 2
    
    val centers = listOf(
        innerStart + Offset(boxCell * 0.5f, boxCell * 0.5f),
        innerStart + Offset(boxCell * 1.5f, boxCell * 0.5f),
        innerStart + Offset(boxCell * 0.5f, boxCell * 1.5f),
        innerStart + Offset(boxCell * 1.5f, boxCell * 1.5f)
    )
    
    centers.forEach { center ->
        // Ring hole effect
        drawCircle(color = color.copy(alpha=0.2f), radius = circleRadius, center = center)
        drawCircle(color = color, radius = circleRadius, center = center, style = Stroke(4f))
    }
}

fun androidx.compose.ui.graphics.drawscope.DrawScope.drawColoredTrack(
    color: Color, 
    startCol: Int, 
    startRow: Int, 
    count: Int, 
    isHorizontal: Boolean, 
    cellSize: Float
) {
    for (i in 0 until count) {
        val (c, r) = if (isHorizontal) Pair(startCol + i, startRow) else Pair(startCol, startRow + i)
        drawRect(
            color = color,
            topLeft = Offset(c * cellSize, r * cellSize),
            size = Size(cellSize, cellSize)
        )
         drawRect(
            color = Color.Black,
            topLeft = Offset(c * cellSize, r * cellSize),
            size = Size(cellSize, cellSize),
            style = Stroke(1f)
        )
    }
}

fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCenterHome(cellSize: Float, size: Size) {
    // 3x3 Center area (6,6) to (9,9)
    val centerRectSize = cellSize * 3
    val topLeft = Offset(6 * cellSize, 6 * cellSize)
    val centerPoint = Offset(size.width / 2, size.height / 2)
    
    // Draw Triangles
    val path = Path()
    
    // Left Triangle (Red)
    path.reset()
    path.moveTo(topLeft.x, topLeft.y)
    path.lineTo(topLeft.x, topLeft.y + centerRectSize)
    path.lineTo(centerPoint.x, centerPoint.y)
    path.close()
    drawPath(path, Color(Player.RED.color)) // Color usage needs to match neighbor arm
    
    // Top Triangle (Green)
    path.reset()
    path.moveTo(topLeft.x, topLeft.y)
    path.lineTo(topLeft.x + centerRectSize, topLeft.y)
    path.lineTo(centerPoint.x, centerPoint.y)
    path.close()
    drawPath(path, Color(Player.GREEN.color))
    
    // Right Triangle (Yellow)
    path.reset()
    path.moveTo(topLeft.x + centerRectSize, topLeft.y)
    path.lineTo(topLeft.x + centerRectSize, topLeft.y + centerRectSize)
    path.lineTo(centerPoint.x, centerPoint.y)
    path.close()
    drawPath(path, Color(Player.YELLOW.color)) // Yellow
    
    // Bottom Triangle (Blue)
    path.reset()
    path.moveTo(topLeft.x, topLeft.y + centerRectSize)
    path.lineTo(topLeft.x + centerRectSize, topLeft.y + centerRectSize)
    path.lineTo(centerPoint.x, centerPoint.y)
    path.close()
    drawPath(path, Color(Player.BLUE.color))
}

fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStar(center: Offset, radius: Float, color: Color) {
    // Minimal cross/star representation
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        lineTo(center.x + radius * 0.3f, center.y - radius * 0.3f)
        lineTo(center.x + radius, center.y)
        lineTo(center.x + radius * 0.3f, center.y + radius * 0.3f)
        lineTo(center.x, center.y + radius)
        lineTo(center.x - radius * 0.3f, center.y + radius * 0.3f)
        lineTo(center.x - radius, center.y)
        lineTo(center.x - radius * 0.3f, center.y - radius * 0.3f)
        close()
    }
    drawPath(path, color)
}

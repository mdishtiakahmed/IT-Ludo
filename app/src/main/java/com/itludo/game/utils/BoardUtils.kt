package com.itludo.game.utils

import com.itludo.game.model.Player

object BoardUtils {
    const val GRID_SIZE = 15
    const val SAFE_ZONE_STAR = 8 // Usually index 8 on the path is safe (or start points)

    // Simplified Path Mapping:
    // We map the "Step Index" (0..56) to (Col, Row) on the 15x15 grid for EACH player.
    // 0 is the start cell (after exiting base).
    // 50 is the last cell before home stretch.
    // 51-55 is the home stretch.
    // 56 is the HOME (Winner Triangle).

    // Universal Path (Generic shape starting from Red's start):
    // Coordinates are (Col, Row) 0-14.
    // Red Starts typically at (1, 6) in some variations or (6, 1)?
    // Let's use standard Ludo board layout:
    // Red Base: Top-Left. Start Path: (1, 6) -> (4, 6) ...
    // Actually, let's trace the "White Cells" path.
    
    // Let's define the segments.
    // Segment 1 (6 cells): Horizontal/Vertical depending on side.
    
    // Absolute Grid Coordinates for the main outer loop (52 cells)
    // Providing a lookup table is safest.
    
    // Let's assume Red starts at (1, 6) moves Right to (5,6), then Up to (6,5)...
    // WAIT, standard ludo:
    // Red (Top-Left Base): Exits to (1, 6). Moves Right -> (5, 6). Up -> (6, 5) -> (6, 0). Right -> (7, 0) -> Home Run? No.
    // Re-verifying standard Ludo Path.
    // The cross is 3 columns wide, 15 high. 3 rows high, 15 wide.
    // Left Arm (Red Start): (0-5, 6-8). Start is (1, 6).
    // Path: (1,6)->(2,6)->(3,6)->(4,6)->(5,6) -> (6,5)->(6,4)->(6,3)->(6,2)->(6,1)->(6,0) -> (7,0)->(8,0) -> (8,1)...
    
    private val RED_PATH_COORDS = listOf(
        Pair(1, 6), Pair(2, 6), Pair(3, 6), Pair(4, 6), Pair(5, 6), // 0-4
        Pair(6, 5), Pair(6, 4), Pair(6, 3), Pair(6, 2), Pair(6, 1), Pair(6, 0), // 5-10
        Pair(7, 0), Pair(8, 0), // 11, 12 (Top turn)
        Pair(8, 1), Pair(8, 2), Pair(8, 3), Pair(8, 4), Pair(8, 5), // 13-17
        Pair(9, 6), Pair(10, 6), Pair(11, 6), Pair(12, 6), Pair(13, 6), Pair(14, 6), // 18-23
        Pair(14, 7), Pair(14, 8), // 24, 25 (Right turn)
        Pair(13, 8), Pair(12, 8), Pair(11, 8), Pair(10, 8), Pair(9, 8), // 26-30
        Pair(8, 9), Pair(8, 10), Pair(8, 11), Pair(8, 12), Pair(8, 13), Pair(8, 14), // 31-36
        Pair(7, 14), Pair(6, 14), // 37, 38 (Bottom turn)
        Pair(6, 13), Pair(6, 12), Pair(6, 11), Pair(6, 10), Pair(6, 9), // 39-43
        Pair(5, 8), Pair(4, 8), Pair(3, 8), Pair(2, 8), Pair(1, 8), Pair(0, 8), // 44-49
        Pair(0, 7), // 50 (Last before home)
        // Home Stretch (51-55)
        Pair(1, 7), Pair(2, 7), Pair(3, 7), Pair(4, 7), Pair(5, 7), 
        // HOME (56)
        Pair(6, 7) // Actually center is triangle, let's map to (7,7) for visual simplicity or a specific entry point
    )

    // For other players, we can rotate the coordinates of Red Payload?
    // Grid 15x15. Center (7,7).
    // Rotate (x, y) 90 deg clockwise defined as: NewX = 14 - y, NewY = x
    
    private fun rotate(coords: Pair<Int, Int>): Pair<Int, Int> {
        return Pair(14 - coords.second, coords.first)
    }

    private val GREEN_PATH = RED_PATH_COORDS.map { rotate(it) } // Top-Right (Green? Or usually Yellow/Blue? Let's stick to standard order: Red(TL) -> Green(TR) -> Yellow(BR) -> Blue(BL) implies 90 deg rotation shifts)
    // Actually typically: Red (TL), Green (TR), Yellow (BR), Blue (BL) is a common pattern, OR Red(TL), Blue(TR), Yellow(BR), Green(BL).
    // Let's assume Standard Rotation: Red -> Green -> Yellow -> Blue.
    
    private val YELLOW_PATH = GREEN_PATH.map { rotate(it) }
    private val BLUE_PATH = YELLOW_PATH.map { rotate(it) }

    fun getCoordinates(player: Player, step: Int): Pair<Int, Int> {
        // Base positions handled separately or index -1
        if (step > 56) return Pair(7, 7) // Cap at center
        val list = when (player) {
            Player.RED -> RED_PATH_COORDS
            Player.GREEN -> GREEN_PATH // Verify rotation correct for players
            Player.YELLOW -> YELLOW_PATH
            Player.BLUE -> BLUE_PATH
        }
        return if (step < list.size) list[step] else Pair(7, 7)
    }
    
    // Positions for tokens in BASE (The 4 circles in the corners)
    // Red Base (Top Left): Setup inside the 6x6 box.
    // Centers: (1.5, 1.5), (1.5, 4.5), (4.5, 1.5), (4.5, 4.5) kinda logic?
    // Let's map to specific grid cells for simplicity if using grid-snap, or floats.
    // But request asks for Grid Index -> X/Y.
    // Let's reserve logical "Base Positions" as -1.
    // We will render them manually in UI at hardcoded offsets in the corners.
    
    // Safe Zones (Stars)
    // Standard: Start points + others.
    // Red Start: index 0. (1,6)
    // Red also has a safe zone at index 8 (8,1)? No, usually index 8 is (8,2)?
    // Usually safe zones are: Start cells of all colors, and +8 cells from start.
    // Global Safe Coordinates: (1,6), (6,1)? No...
    // Let's hardcode the indices that are "Globally Safe" (Start cells) + "Globe" cells.
    // Indices on path: 0, 8, 13 (start of next), 21?
    // Let's just say: If any token is at (GridX, GridY) that corresponds to a star, it's safe.
    // Star Locations: (2,6)-StartRed, (6,2)-StartGreen?, (12,8), (8,12) etc.
    // Wait, let's look at the generated path.
    // Red Start: (1,6). This is safe.
    // 8th step: (8,2). Usually safe?
    // Let's stick to: Start Point of ANY player is a SAFE ZONE.
    
    val SAFE_GRID_COORDS = setOf(
        Pair(1, 6), // Red Start
        Pair(8, 1), // Green Start (approx)
        Pair(13, 8), // Yellow Start
        Pair(6, 13), // Blue Start
        // Plus the "Globe" spots usually 8 steps ahead?
        Pair(6, 2), Pair(12, 6), Pair(8, 12), Pair(2, 8)
    )
    
    fun isSafeSquare(gridX: Int, gridY: Int): Boolean {
        return SAFE_GRID_COORDS.contains(Pair(gridX, gridY))
    }
}

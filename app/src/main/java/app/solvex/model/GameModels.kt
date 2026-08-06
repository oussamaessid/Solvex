package app.solvex.model

enum class CellElement { EMPTY, FIRE, WATER }
enum class ConstraintType { EQUAL, DIFFERENT }
enum class AppScreen { HOME, GAME, VICTORY, STATS }

const val MAX_LIVES = 5
const val MAX_HINTS = 5
const val LIFE_REGEN_MS = 60 * 60 * 1000L

data class Constraint(
    val r1: Int, val c1: Int,
    val r2: Int, val c2: Int,
    val type: ConstraintType
)

data class GameLevel(
    val id: Int,
    val levelNumber: Int,
    val size: Int,
    val clues: List<List<CellElement>>,
    val constraints: List<Constraint>,
    val solution: List<List<CellElement>>,
    val isDaily: Boolean = false
)

data class GameState(
    val level: GameLevel,
    val board: List<List<CellElement>>,
    val history: List<List<List<CellElement>>> = emptyList(),
    val isComplete: Boolean = false,
    val wasAlreadyComplete: Boolean = false,
    val elapsedSeconds: Int = 0,
    val hintsUsed: Int = 0,
    val errorCells: Set<Pair<Int, Int>> = emptySet(),
    val lives: Int = MAX_LIVES,
    val hints: Int = MAX_HINTS,
    val nextLifeAtMillis: Long = 0L
)

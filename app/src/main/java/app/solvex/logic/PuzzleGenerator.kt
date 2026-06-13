package app.solvex.logic

import app.solvex.model.*
import kotlin.random.Random

object PuzzleGenerator {

    // Configs (size, nConstraints, revealRatio) — same progression as the Colab generator
    private data class Config(val size: Int, val nConstraints: Int, val revealRatio: Double)

    private val CONFIGS = listOf(
        Config(6,  2, 0.50),
        Config(6,  2, 0.45),
        Config(6,  3, 0.42),
        Config(6,  3, 0.38),
        Config(6,  4, 0.35),
        Config(6,  4, 0.30),
        Config(6,  5, 0.27),
        Config(6,  6, 0.24),
    )

    fun generate(levelIndex: Int, seed: Long): GameLevel {
        val config = CONFIGS[(levelIndex / 100).coerceIn(0, CONFIGS.lastIndex)]
        val rng = Random(seed)
        val solution = buildSolution(config.size, rng) ?: buildSolution(config.size, Random(seed + 1))!!
        val constraints = buildConstraints(solution, config.size, config.nConstraints, rng)
        val clues = maskSolution(solution, config.size, config.revealRatio, rng)
        return GameLevel(
            id = seed.toInt(),
            levelNumber = levelIndex + 1,
            size = config.size,
            clues = clues,
            constraints = constraints,
            solution = solution
        )
    }

    fun daily(seed: Long): GameLevel {
        val rng = Random(seed)
        val solution = buildSolution(6, rng) ?: buildSolution(6, Random(seed + 99))!!
        val constraints = buildConstraints(solution, 6, 3, rng)
        val clues = maskSolution(solution, 6, 0.40, rng)
        return GameLevel(
            id = seed.toInt(),
            levelNumber = 0,
            size = 6,
            clues = clues,
            constraints = constraints,
            solution = solution,
            isDaily = true
        )
    }

    private fun buildSolution(size: Int, rng: Random): List<List<CellElement>>? {
        val grid = Array(size) { Array(size) { CellElement.EMPTY } }
        return if (fill(grid, size, 0, rng)) grid.map { it.toList() } else null
    }

    private fun fill(grid: Array<Array<CellElement>>, size: Int, pos: Int, rng: Random): Boolean {
        if (pos == size * size) return true
        val row = pos / size
        val col = pos % size
        val options = if (rng.nextBoolean())
            listOf(CellElement.FIRE, CellElement.WATER)
        else
            listOf(CellElement.WATER, CellElement.FIRE)
        for (el in options) {
            grid[row][col] = el
            if (isOk(grid, size, row, col) && fill(grid, size, pos + 1, rng)) return true
        }
        grid[row][col] = CellElement.EMPTY
        return false
    }

    private fun isOk(grid: Array<Array<CellElement>>, size: Int, r: Int, c: Int): Boolean {
        val el = grid[r][c]
        val half = size / 2
        if (grid[r].count { it == el } > half) return false
        if ((0..r).count { grid[it][c] == el } > half) return false
        if (c >= 2 && grid[r][c - 1] == el && grid[r][c - 2] == el) return false
        if (r >= 2 && grid[r - 1][c] == el && grid[r - 2][c] == el) return false
        return true
    }

    private fun buildConstraints(
        sol: List<List<CellElement>>,
        size: Int,
        count: Int,
        rng: Random
    ): List<Constraint> {
        val pairs = mutableListOf<Pair<Pair<Int, Int>, Pair<Int, Int>>>()
        for (r in 0 until size) for (c in 0 until size) {
            if (c + 1 < size) pairs += (r to c) to (r to c + 1)
            if (r + 1 < size) pairs += (r to c) to (r + 1 to c)
        }
        return pairs.shuffled(rng).take(count).map { (a, b) ->
            val type = if (sol[a.first][a.second] == sol[b.first][b.second])
                ConstraintType.EQUAL else ConstraintType.DIFFERENT
            Constraint(a.first, a.second, b.first, b.second, type)
        }
    }

    private fun maskSolution(
        sol: List<List<CellElement>>,
        size: Int,
        revealRatio: Double,
        rng: Random
    ): List<List<CellElement>> {
        val all = (0 until size).flatMap { r -> (0 until size).map { c -> r to c } }.shuffled(rng)
        val revealed = all.take((size * size * revealRatio).toInt()).toSet()
        return sol.mapIndexed { r, row ->
            row.mapIndexed { c, el -> if ((r to c) in revealed) el else CellElement.EMPTY }
        }
    }
}

package app.solvex.logic

import app.solvex.model.*

object GameValidator {

    fun isComplete(board: List<List<CellElement>>, level: GameLevel): Boolean {
        if (board.any { row -> row.any { it == CellElement.EMPTY } }) return false
        return getErrors(board, level).isEmpty()
    }

    fun getErrors(board: List<List<CellElement>>, level: GameLevel): Set<Pair<Int, Int>> {
        val errors = mutableSetOf<Pair<Int, Int>>()
        val size = level.size
        val half = size / 2

        // Row balance check
        for (r in 0 until size) {
            val fireCnt = board[r].count { it == CellElement.FIRE }
            val waterCnt = board[r].count { it == CellElement.WATER }
            val rowFull = board[r].none { it == CellElement.EMPTY }
            if (fireCnt > half || waterCnt > half || (rowFull && fireCnt != half)) {
                board[r].forEachIndexed { c, e -> if (e != CellElement.EMPTY) errors += r to c }
            }
        }

        // Column balance check
        for (c in 0 until size) {
            val col = (0 until size).map { board[it][c] }
            val fireCnt = col.count { it == CellElement.FIRE }
            val waterCnt = col.count { it == CellElement.WATER }
            val colFull = col.none { it == CellElement.EMPTY }
            if (fireCnt > half || waterCnt > half || (colFull && fireCnt != half)) {
                (0 until size).forEach { r -> if (board[r][c] != CellElement.EMPTY) errors += r to c }
            }
        }

        // Three consecutive same symbol
        for (r in 0 until size) for (c in 0 until size) {
            val el = board[r][c]
            if (el == CellElement.EMPTY) continue
            if (c + 2 < size && board[r][c + 1] == el && board[r][c + 2] == el) {
                errors += r to c; errors += r to c + 1; errors += r to c + 2
            }
            if (r + 2 < size && board[r + 1][c] == el && board[r + 2][c] == el) {
                errors += r to c; errors += r + 1 to c; errors += r + 2 to c
            }
        }

        // = and × constraints
        for (con in level.constraints) {
            val e1 = board[con.r1][con.c1]
            val e2 = board[con.r2][con.c2]
            if (e1 == CellElement.EMPTY || e2 == CellElement.EMPTY) continue
            val violated = when (con.type) {
                ConstraintType.EQUAL     -> e1 != e2
                ConstraintType.DIFFERENT -> e1 == e2
            }
            if (violated) {
                errors += con.r1 to con.c1
                errors += con.r2 to con.c2
            }
        }
        return errors
    }

    fun nextElement(current: CellElement): CellElement = when (current) {
        CellElement.EMPTY -> CellElement.FIRE
        CellElement.FIRE  -> CellElement.WATER
        CellElement.WATER -> CellElement.EMPTY
    }
}

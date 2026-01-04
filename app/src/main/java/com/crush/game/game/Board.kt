package com.crush.game.game

class Board(val rows: Int = 8, val cols: Int = 8) {

    private val grid: Array<Array<Candy?>> = Array(rows) { arrayOfNulls(cols) }

    init {
        fillBoard()
        while (MatchFinder.findAllMatches(this).isNotEmpty()) {
            fillBoard()
        }
    }

    private fun fillBoard() {
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                grid[row][col] = createCandyWithoutMatch(row, col)
            }
        }
    }

    private fun createCandyWithoutMatch(row: Int, col: Int): Candy {
        val availableTypes = CandyType.entries.toMutableList()

        // Check horizontal matches
        if (col >= 2) {
            val type1 = grid[row][col - 1]?.type
            val type2 = grid[row][col - 2]?.type
            if (type1 != null && type1 == type2) {
                availableTypes.remove(type1)
            }
        }

        // Check vertical matches
        if (row >= 2) {
            val type1 = grid[row - 1][col]?.type
            val type2 = grid[row - 2][col]?.type
            if (type1 != null && type1 == type2) {
                availableTypes.remove(type1)
            }
        }

        return Candy(availableTypes.random(), row, col)
    }

    fun getCandy(row: Int, col: Int): Candy? {
        if (row !in 0 until rows || col !in 0 until cols) return null
        return grid[row][col]
    }

    fun setCandy(row: Int, col: Int, candy: Candy?) {
        if (row in 0 until rows && col in 0 until cols) {
            grid[row][col] = candy
            candy?.let {
                it.row = row
                it.col = col
            }
        }
    }

    fun swap(pos1: Position, pos2: Position) {
        val candy1 = grid[pos1.row][pos1.col]
        val candy2 = grid[pos2.row][pos2.col]

        grid[pos1.row][pos1.col] = candy2
        grid[pos2.row][pos2.col] = candy1

        candy1?.let {
            it.row = pos2.row
            it.col = pos2.col
        }
        candy2?.let {
            it.row = pos1.row
            it.col = pos1.col
        }
    }

    fun removeMatches(matches: List<Match>): Int {
        var totalScore = 0
        val toRemove = mutableSetOf<Position>()

        for (match in matches) {
            totalScore += match.score
            toRemove.addAll(match.candies)
        }

        for (pos in toRemove) {
            grid[pos.row][pos.col]?.isMatched = true
            grid[pos.row][pos.col] = null
        }

        return totalScore
    }

    fun applyGravity(): List<Pair<Position, Position>> {
        val movements = mutableListOf<Pair<Position, Position>>()

        for (col in 0 until cols) {
            var emptyRow = rows - 1

            for (row in (rows - 1) downTo 0) {
                val candy = grid[row][col]
                if (candy != null) {
                    if (row != emptyRow) {
                        movements.add(Position(row, col) to Position(emptyRow, col))
                        grid[emptyRow][col] = candy
                        candy.row = emptyRow
                        grid[row][col] = null
                    }
                    emptyRow--
                }
            }
        }

        return movements
    }

    fun fillEmptySpaces(): List<Position> {
        val newCandies = mutableListOf<Position>()

        for (col in 0 until cols) {
            for (row in 0 until rows) {
                if (grid[row][col] == null) {
                    val newCandy = Candy(CandyType.random(), row, col)
                    newCandy.isNew = true
                    grid[row][col] = newCandy
                    newCandies.add(Position(row, col))
                }
            }
        }

        return newCandies
    }

    fun hasValidMoves(): Boolean {
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                // Check swap with right neighbor
                if (col < cols - 1) {
                    swap(Position(row, col), Position(row, col + 1))
                    val hasMatch = MatchFinder.findAllMatches(this).isNotEmpty()
                    swap(Position(row, col), Position(row, col + 1))
                    if (hasMatch) return true
                }

                // Check swap with bottom neighbor
                if (row < rows - 1) {
                    swap(Position(row, col), Position(row + 1, col))
                    val hasMatch = MatchFinder.findAllMatches(this).isNotEmpty()
                    swap(Position(row, col), Position(row + 1, col))
                    if (hasMatch) return true
                }
            }
        }
        return false
    }

    fun getAllCandies(): List<Candy> {
        return grid.flatMap { row -> row.filterNotNull() }
    }

    fun findHint(): Pair<Position, Position>? {
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                if (col < cols - 1) {
                    swap(Position(row, col), Position(row, col + 1))
                    if (MatchFinder.findAllMatches(this).isNotEmpty()) {
                        swap(Position(row, col), Position(row, col + 1))
                        return Position(row, col) to Position(row, col + 1)
                    }
                    swap(Position(row, col), Position(row, col + 1))
                }

                if (row < rows - 1) {
                    swap(Position(row, col), Position(row + 1, col))
                    if (MatchFinder.findAllMatches(this).isNotEmpty()) {
                        swap(Position(row, col), Position(row + 1, col))
                        return Position(row, col) to Position(row + 1, col)
                    }
                    swap(Position(row, col), Position(row + 1, col))
                }
            }
        }
        return null
    }
}

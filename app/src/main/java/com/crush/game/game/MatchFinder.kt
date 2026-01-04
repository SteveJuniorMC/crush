package com.crush.game.game

object MatchFinder {

    fun findAllMatches(board: Board): List<Match> {
        val matches = mutableListOf<Match>()
        val matchedPositions = mutableSetOf<Position>()

        // Find horizontal matches
        for (row in 0 until board.rows) {
            var col = 0
            while (col < board.cols) {
                val startCandy = board.getCandy(row, col)
                if (startCandy == null) {
                    col++
                    continue
                }

                val matchPositions = mutableListOf(Position(row, col))
                var nextCol = col + 1

                while (nextCol < board.cols) {
                    val nextCandy = board.getCandy(row, nextCol)
                    if (nextCandy?.type == startCandy.type) {
                        matchPositions.add(Position(row, nextCol))
                        nextCol++
                    } else {
                        break
                    }
                }

                if (matchPositions.size >= 3) {
                    matches.add(Match(matchPositions.toList(), startCandy.type))
                    matchedPositions.addAll(matchPositions)
                }

                col = nextCol
            }
        }

        // Find vertical matches
        for (col in 0 until board.cols) {
            var row = 0
            while (row < board.rows) {
                val startCandy = board.getCandy(row, col)
                if (startCandy == null) {
                    row++
                    continue
                }

                val matchPositions = mutableListOf(Position(row, col))
                var nextRow = row + 1

                while (nextRow < board.rows) {
                    val nextCandy = board.getCandy(nextRow, col)
                    if (nextCandy?.type == startCandy.type) {
                        matchPositions.add(Position(nextRow, col))
                        nextRow++
                    } else {
                        break
                    }
                }

                if (matchPositions.size >= 3) {
                    // Merge with horizontal matches at overlapping positions
                    val newPositions = matchPositions.filter { it !in matchedPositions }
                    if (newPositions.isNotEmpty() || matchPositions.all { it in matchedPositions }) {
                        matches.add(Match(matchPositions.toList(), startCandy.type))
                        matchedPositions.addAll(matchPositions)
                    }
                }

                row = nextRow
            }
        }

        return mergeOverlappingMatches(matches)
    }

    private fun mergeOverlappingMatches(matches: List<Match>): List<Match> {
        if (matches.isEmpty()) return matches

        val allPositions = mutableSetOf<Position>()
        matches.forEach { allPositions.addAll(it.candies) }

        // Group connected positions
        val visited = mutableSetOf<Position>()
        val mergedMatches = mutableListOf<Match>()

        for (match in matches) {
            for (pos in match.candies) {
                if (pos !in visited) {
                    val connected = findConnected(pos, allPositions, visited, match.type, matches)
                    if (connected.size >= 3) {
                        mergedMatches.add(Match(connected.toList(), match.type))
                    }
                }
            }
        }

        return mergedMatches
    }

    private fun findConnected(
        start: Position,
        allPositions: Set<Position>,
        visited: MutableSet<Position>,
        type: CandyType,
        matches: List<Match>
    ): Set<Position> {
        val result = mutableSetOf<Position>()
        val queue = ArrayDeque<Position>()
        queue.add(start)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current in visited) continue
            if (current !in allPositions) continue

            // Check if this position belongs to a match of the same type
            val belongsToType = matches.any { it.type == type && current in it.candies }
            if (!belongsToType) continue

            visited.add(current)
            result.add(current)

            // Add adjacent positions
            listOf(
                Position(current.row - 1, current.col),
                Position(current.row + 1, current.col),
                Position(current.row, current.col - 1),
                Position(current.row, current.col + 1)
            ).forEach { adj ->
                if (adj !in visited && adj in allPositions) {
                    queue.add(adj)
                }
            }
        }

        return result
    }

    fun findMatchesAt(board: Board, positions: List<Position>): List<Match> {
        val allMatches = findAllMatches(board)
        return allMatches.filter { match ->
            match.candies.any { it in positions }
        }
    }
}

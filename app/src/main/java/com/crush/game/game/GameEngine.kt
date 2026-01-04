package com.crush.game.game

import com.crush.game.animation.AnimationManager

enum class GameState {
    IDLE,
    ANIMATING_SWAP,
    ANIMATING_INVALID_SWAP,
    ANIMATING_MATCH,
    ANIMATING_FALL,
    LEVEL_COMPLETE,
    GAME_OVER,
    NO_MOVES
}

interface GameListener {
    fun onScoreChanged(score: Int, delta: Int)
    fun onMovesChanged(moves: Int)
    fun onLevelComplete(stars: Int)
    fun onGameOver()
    fun onNoMoves()
    fun onStateChanged(state: GameState)
}

class GameEngine(
    private val level: Level,
    private val animationManager: AnimationManager
) {
    val board: Board = Board()

    var score: Int = 0
        private set

    var movesRemaining: Int = level.maxMoves
        private set

    var state: GameState = GameState.IDLE
        private set(value) {
            field = value
            listener?.onStateChanged(value)
        }

    var listener: GameListener? = null

    private var selectedPosition: Position? = null
    private var swapPosition1: Position? = null
    private var swapPosition2: Position? = null
    private var comboMultiplier: Int = 1

    fun selectCandy(row: Int, col: Int): Boolean {
        if (state != GameState.IDLE) return false

        val position = Position(row, col)
        val candy = board.getCandy(row, col) ?: return false

        val current = selectedPosition
        if (current == null) {
            selectedPosition = position
            return true
        }

        if (current == position) {
            selectedPosition = null
            return true
        }

        if (current.isAdjacent(position)) {
            trySwap(current, position)
            selectedPosition = null
            return true
        }

        selectedPosition = position
        return true
    }

    fun getSelectedPosition(): Position? = selectedPosition

    private fun trySwap(pos1: Position, pos2: Position) {
        swapPosition1 = pos1
        swapPosition2 = pos2
        state = GameState.ANIMATING_SWAP
        comboMultiplier = 1

        board.swap(pos1, pos2)

        val candy1 = board.getCandy(pos2.row, pos2.col)
        val candy2 = board.getCandy(pos1.row, pos1.col)

        animationManager.animateSwap(candy1, candy2, pos1, pos2) {
            checkMatchesAfterSwap()
        }
    }

    private fun checkMatchesAfterSwap() {
        val pos1 = swapPosition1 ?: return
        val pos2 = swapPosition2 ?: return

        val matches = MatchFinder.findAllMatches(board)

        if (matches.isEmpty()) {
            // Invalid swap, reverse it
            state = GameState.ANIMATING_INVALID_SWAP
            board.swap(pos1, pos2)

            val candy1 = board.getCandy(pos1.row, pos1.col)
            val candy2 = board.getCandy(pos2.row, pos2.col)

            animationManager.animateInvalidSwap(candy1, candy2, pos2, pos1) {
                state = GameState.IDLE
                swapPosition1 = null
                swapPosition2 = null
            }
        } else {
            // Valid swap, consume move
            movesRemaining--
            listener?.onMovesChanged(movesRemaining)
            processMatches(matches)
        }
    }

    private fun processMatches(matches: List<Match>) {
        state = GameState.ANIMATING_MATCH

        val matchedCandies = matches.flatMap { match ->
            match.candies.mapNotNull { board.getCandy(it.row, it.col) }
        }

        animationManager.animateMatch(matchedCandies) {
            // Calculate score with combo multiplier
            val baseScore = board.removeMatches(matches)
            val earnedScore = baseScore * comboMultiplier
            score += earnedScore
            listener?.onScoreChanged(score, earnedScore)

            // Apply gravity
            applyGravityAndFill()
        }
    }

    private fun applyGravityAndFill() {
        state = GameState.ANIMATING_FALL

        val movements = board.applyGravity()
        val newCandies = board.fillEmptySpaces()

        val movingCandies = movements.map { (from, to) ->
            board.getCandy(to.row, to.col)!! to (to.row - from.row)
        }

        val fallingNewCandies = newCandies.map { pos ->
            val candy = board.getCandy(pos.row, pos.col)!!
            candy to (pos.row + 1)
        }

        animationManager.animateFall(movingCandies, fallingNewCandies) {
            comboMultiplier++
            checkForCascadeMatches()
        }
    }

    private fun checkForCascadeMatches() {
        val matches = MatchFinder.findAllMatches(board)

        if (matches.isNotEmpty()) {
            processMatches(matches)
        } else {
            // No more matches, check game state
            swapPosition1 = null
            swapPosition2 = null
            checkGameState()
        }
    }

    private fun checkGameState() {
        // Check if level is complete
        if (score >= level.targetScore) {
            state = GameState.LEVEL_COMPLETE
            val stars = level.getStars(score)
            listener?.onLevelComplete(stars)
            return
        }

        // Check if out of moves
        if (movesRemaining <= 0) {
            state = GameState.GAME_OVER
            listener?.onGameOver()
            return
        }

        // Check if there are valid moves
        if (!board.hasValidMoves()) {
            state = GameState.NO_MOVES
            listener?.onNoMoves()
            return
        }

        state = GameState.IDLE
    }

    fun isIdle(): Boolean = state == GameState.IDLE

    fun getTargetScore(): Int = level.targetScore

    fun getLevelNumber(): Int = level.number
}

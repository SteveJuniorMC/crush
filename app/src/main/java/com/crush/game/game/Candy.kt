package com.crush.game.game

import android.graphics.Color

enum class CandyType(
    val primaryColor: Int,
    val accentColor: Int,
    val darkColor: Int
) {
    RED(
        Color.parseColor("#FF4757"),
        Color.parseColor("#FF6B7A"),
        Color.parseColor("#CC3945")
    ),
    ORANGE(
        Color.parseColor("#FFA502"),
        Color.parseColor("#FFB732"),
        Color.parseColor("#CC8402")
    ),
    YELLOW(
        Color.parseColor("#FFD32A"),
        Color.parseColor("#FFE066"),
        Color.parseColor("#CCA922")
    ),
    GREEN(
        Color.parseColor("#26DE81"),
        Color.parseColor("#7BED9F"),
        Color.parseColor("#1EB268")
    ),
    BLUE(
        Color.parseColor("#45AAF2"),
        Color.parseColor("#70C7FF"),
        Color.parseColor("#3788C2")
    ),
    PURPLE(
        Color.parseColor("#A55EEA"),
        Color.parseColor("#C990FF"),
        Color.parseColor("#844BBB")
    );

    companion object {
        fun random(): CandyType = entries.random()
    }
}

data class Candy(
    val type: CandyType,
    var row: Int,
    var col: Int
) {
    var isMatched: Boolean = false
    var isNew: Boolean = false
    var animationOffsetX: Float = 0f
    var animationOffsetY: Float = 0f
    var scale: Float = 1f
    var alpha: Float = 1f
    var rotation: Float = 0f

    fun resetAnimation() {
        animationOffsetX = 0f
        animationOffsetY = 0f
        scale = 1f
        alpha = 1f
        rotation = 0f
        isNew = false
    }

    fun copy(): Candy = Candy(type, row, col).also {
        it.isMatched = isMatched
        it.isNew = isNew
        it.animationOffsetX = animationOffsetX
        it.animationOffsetY = animationOffsetY
        it.scale = scale
        it.alpha = alpha
        it.rotation = rotation
    }
}

data class Position(val row: Int, val col: Int) {
    fun isAdjacent(other: Position): Boolean {
        val rowDiff = kotlin.math.abs(row - other.row)
        val colDiff = kotlin.math.abs(col - other.col)
        return (rowDiff == 1 && colDiff == 0) || (rowDiff == 0 && colDiff == 1)
    }
}

data class Match(
    val candies: List<Position>,
    val type: CandyType
) {
    val size: Int get() = candies.size
    val score: Int get() = when {
        size >= 5 -> 100
        size == 4 -> 60
        else -> 30
    }
}

package com.crush.game.game

import android.content.Context
import android.content.SharedPreferences

class LevelManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val levels: List<Level> = Level.createLevels()
    private val progress: MutableList<LevelProgress> = mutableListOf()

    init {
        loadProgress()
    }

    private fun loadProgress() {
        progress.clear()
        levels.forEachIndexed { index, level ->
            val isUnlocked = if (index == 0) true else prefs.getBoolean("level_${level.number}_unlocked", false)
            val bestScore = prefs.getInt("level_${level.number}_score", 0)
            val stars = prefs.getInt("level_${level.number}_stars", 0)
            progress.add(LevelProgress(level.number, isUnlocked, bestScore, stars))
        }
    }

    fun getLevel(number: Int): Level? = levels.find { it.number == number }

    fun getLevelProgress(number: Int): LevelProgress? = progress.find { it.levelNumber == number }

    fun getAllProgress(): List<LevelProgress> = progress.toList()

    fun getTotalLevels(): Int = levels.size

    fun completeLevel(levelNumber: Int, score: Int): LevelCompletionResult {
        val level = getLevel(levelNumber) ?: return LevelCompletionResult(false, 0, false)
        val levelProgress = getLevelProgress(levelNumber) ?: return LevelCompletionResult(false, 0, false)

        val stars = level.getStars(score)
        val isNewHighScore = score > levelProgress.bestScore
        val isNewStarRecord = stars > levelProgress.stars

        if (isNewHighScore) {
            levelProgress.bestScore = score
            prefs.edit().putInt("level_${levelNumber}_score", score).apply()
        }

        if (isNewStarRecord) {
            levelProgress.stars = stars
            prefs.edit().putInt("level_${levelNumber}_stars", stars).apply()
        }

        // Unlock next level
        var unlockedNewLevel = false
        if (stars > 0 && levelNumber < levels.size) {
            val nextProgress = progress.find { it.levelNumber == levelNumber + 1 }
            if (nextProgress != null && !nextProgress.isUnlocked) {
                nextProgress.isUnlocked = true
                prefs.edit().putBoolean("level_${levelNumber + 1}_unlocked", true).apply()
                unlockedNewLevel = true
            }
        }

        return LevelCompletionResult(
            completed = stars > 0,
            stars = stars,
            unlockedNewLevel = unlockedNewLevel,
            isNewHighScore = isNewHighScore
        )
    }

    fun getTotalStars(): Int = progress.sumOf { it.stars }

    fun getMaxPossibleStars(): Int = levels.size * 3

    fun getCompletedLevelsCount(): Int = progress.count { it.isCompleted }

    fun getUnlockedLevelsCount(): Int = progress.count { it.isUnlocked }

    fun resetProgress() {
        prefs.edit().clear().apply()
        loadProgress()
    }

    companion object {
        private const val PREFS_NAME = "crush_game_progress"
    }
}

data class LevelCompletionResult(
    val completed: Boolean,
    val stars: Int,
    val unlockedNewLevel: Boolean,
    val isNewHighScore: Boolean = false
)

package com.crush.game.game

data class Level(
    val number: Int,
    val targetScore: Int,
    val maxMoves: Int,
    val starThresholds: StarThresholds
) {
    data class StarThresholds(
        val oneStar: Int,
        val twoStars: Int,
        val threeStars: Int
    )

    fun getStars(score: Int): Int = when {
        score >= starThresholds.threeStars -> 3
        score >= starThresholds.twoStars -> 2
        score >= starThresholds.oneStar -> 1
        else -> 0
    }

    companion object {
        fun createLevels(): List<Level> {
            return listOf(
                // Easy levels (1-5)
                Level(1, 500, 30, StarThresholds(500, 800, 1200)),
                Level(2, 600, 28, StarThresholds(600, 1000, 1500)),
                Level(3, 750, 28, StarThresholds(750, 1200, 1800)),
                Level(4, 900, 26, StarThresholds(900, 1400, 2100)),
                Level(5, 1000, 26, StarThresholds(1000, 1600, 2400)),

                // Medium levels (6-10)
                Level(6, 1200, 25, StarThresholds(1200, 1900, 2800)),
                Level(7, 1400, 25, StarThresholds(1400, 2200, 3200)),
                Level(8, 1600, 24, StarThresholds(1600, 2500, 3600)),
                Level(9, 1800, 24, StarThresholds(1800, 2800, 4000)),
                Level(10, 2000, 23, StarThresholds(2000, 3100, 4500)),

                // Hard levels (11-15)
                Level(11, 2300, 23, StarThresholds(2300, 3500, 5000)),
                Level(12, 2600, 22, StarThresholds(2600, 3900, 5500)),
                Level(13, 3000, 22, StarThresholds(3000, 4400, 6200)),
                Level(14, 3400, 21, StarThresholds(3400, 5000, 7000)),
                Level(15, 3800, 21, StarThresholds(3800, 5600, 7800)),

                // Expert levels (16-20)
                Level(16, 4200, 20, StarThresholds(4200, 6200, 8600)),
                Level(17, 4700, 20, StarThresholds(4700, 6900, 9500)),
                Level(18, 5200, 19, StarThresholds(5200, 7600, 10400)),
                Level(19, 5800, 19, StarThresholds(5800, 8400, 11500)),
                Level(20, 6500, 18, StarThresholds(6500, 9500, 13000)),

                // Master levels (21-25)
                Level(21, 7200, 18, StarThresholds(7200, 10500, 14500)),
                Level(22, 8000, 17, StarThresholds(8000, 11600, 16000)),
                Level(23, 9000, 17, StarThresholds(9000, 13000, 18000)),
                Level(24, 10000, 16, StarThresholds(10000, 14500, 20000)),
                Level(25, 12000, 15, StarThresholds(12000, 17000, 24000))
            )
        }
    }
}

data class LevelProgress(
    val levelNumber: Int,
    var isUnlocked: Boolean,
    var bestScore: Int = 0,
    var stars: Int = 0
) {
    val isCompleted: Boolean get() = stars > 0
}

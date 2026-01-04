package com.crush.game.animation

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.view.animation.AccelerateInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import com.crush.game.game.Candy
import com.crush.game.game.GameView
import com.crush.game.game.Position

class AnimationManager(private val gameView: GameView) {

    private var cellSize: Float = 0f

    fun setCellSize(size: Float) {
        cellSize = size
    }

    fun animateSwap(
        candy1: Candy?,
        candy2: Candy?,
        pos1: Position,
        pos2: Position,
        onComplete: () -> Unit
    ) {
        if (candy1 == null || candy2 == null) {
            onComplete()
            return
        }

        val animatorSet = AnimatorSet()

        // Calculate offsets
        val dx = (pos1.col - pos2.col) * cellSize
        val dy = (pos1.row - pos2.row) * cellSize

        // Candy 1 animation (currently at pos2, animating from pos1)
        val anim1X = ValueAnimator.ofFloat(dx, 0f).apply {
            addUpdateListener { candy1.animationOffsetX = it.animatedValue as Float }
        }
        val anim1Y = ValueAnimator.ofFloat(dy, 0f).apply {
            addUpdateListener { candy1.animationOffsetY = it.animatedValue as Float }
        }
        val anim1Scale = ValueAnimator.ofFloat(1f, 1.15f, 1f).apply {
            addUpdateListener { candy1.scale = it.animatedValue as Float }
        }

        // Candy 2 animation (currently at pos1, animating from pos2)
        val anim2X = ValueAnimator.ofFloat(-dx, 0f).apply {
            addUpdateListener { candy2.animationOffsetX = it.animatedValue as Float }
        }
        val anim2Y = ValueAnimator.ofFloat(-dy, 0f).apply {
            addUpdateListener { candy2.animationOffsetY = it.animatedValue as Float }
        }
        val anim2Scale = ValueAnimator.ofFloat(1f, 1.15f, 1f).apply {
            addUpdateListener { candy2.scale = it.animatedValue as Float }
        }

        animatorSet.playTogether(anim1X, anim1Y, anim1Scale, anim2X, anim2Y, anim2Scale)
        animatorSet.duration = SWAP_DURATION
        animatorSet.interpolator = DecelerateInterpolator()

        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                candy1.resetAnimation()
                candy2.resetAnimation()
                gameView.refresh()
                onComplete()
            }

            override fun onAnimationCancel(animation: Animator) {
                candy1.resetAnimation()
                candy2.resetAnimation()
            }
        })

        animatorSet.addUpdateListener { gameView.refresh() }
        animatorSet.start()
    }

    fun animateInvalidSwap(
        candy1: Candy?,
        candy2: Candy?,
        pos1: Position,
        pos2: Position,
        onComplete: () -> Unit
    ) {
        if (candy1 == null || candy2 == null) {
            onComplete()
            return
        }

        val animatorSet = AnimatorSet()

        val dx = (pos1.col - pos2.col) * cellSize
        val dy = (pos1.row - pos2.row) * cellSize

        // Animate to swapped position then back with shake
        val shakeAmount = cellSize * 0.1f

        val anim1X = ValueAnimator.ofFloat(dx, 0f, -shakeAmount, shakeAmount, -shakeAmount * 0.5f, 0f).apply {
            addUpdateListener { candy1.animationOffsetX = it.animatedValue as Float }
        }
        val anim1Y = ValueAnimator.ofFloat(dy, 0f).apply {
            addUpdateListener { candy1.animationOffsetY = it.animatedValue as Float }
        }

        val anim2X = ValueAnimator.ofFloat(-dx, 0f, shakeAmount, -shakeAmount, shakeAmount * 0.5f, 0f).apply {
            addUpdateListener { candy2.animationOffsetX = it.animatedValue as Float }
        }
        val anim2Y = ValueAnimator.ofFloat(-dy, 0f).apply {
            addUpdateListener { candy2.animationOffsetY = it.animatedValue as Float }
        }

        animatorSet.playTogether(anim1X, anim1Y, anim2X, anim2Y)
        animatorSet.duration = INVALID_SWAP_DURATION

        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                candy1.resetAnimation()
                candy2.resetAnimation()
                gameView.refresh()
                onComplete()
            }

            override fun onAnimationCancel(animation: Animator) {
                candy1.resetAnimation()
                candy2.resetAnimation()
            }
        })

        animatorSet.addUpdateListener { gameView.refresh() }
        animatorSet.start()
    }

    fun animateMatch(candies: List<Candy>, onComplete: () -> Unit) {
        if (candies.isEmpty()) {
            onComplete()
            return
        }

        val animatorSet = AnimatorSet()
        val animators = mutableListOf<Animator>()

        for (candy in candies) {
            // Scale up and fade out
            val scaleAnim = ValueAnimator.ofFloat(1f, 1.3f, 0f).apply {
                addUpdateListener { candy.scale = it.animatedValue as Float }
            }
            val alphaAnim = ValueAnimator.ofFloat(1f, 1f, 0f).apply {
                addUpdateListener { candy.alpha = it.animatedValue as Float }
            }
            val rotateAnim = ValueAnimator.ofFloat(0f, 15f).apply {
                addUpdateListener { candy.rotation = it.animatedValue as Float }
            }

            animators.add(scaleAnim)
            animators.add(alphaAnim)
            animators.add(rotateAnim)

            // Add particles
            val (centerX, centerY) = gameView.getCandyCenter(candy.row, candy.col)
            gameView.addParticles(centerX, centerY, candy.type.primaryColor, 8)
        }

        animatorSet.playTogether(animators)
        animatorSet.duration = MATCH_DURATION
        animatorSet.interpolator = AccelerateInterpolator()

        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                for (candy in candies) {
                    candy.resetAnimation()
                }
                gameView.refresh()
                onComplete()
            }
        })

        animatorSet.addUpdateListener { gameView.refresh() }
        animatorSet.start()
    }

    fun animateFall(
        movingCandies: List<Pair<Candy, Int>>,
        newCandies: List<Pair<Candy, Int>>,
        onComplete: () -> Unit
    ) {
        val animatorSet = AnimatorSet()
        val animators = mutableListOf<Animator>()

        // Animate existing candies falling
        for ((candy, distance) in movingCandies) {
            val startOffset = -distance * cellSize
            val fallAnim = ValueAnimator.ofFloat(startOffset, 0f).apply {
                duration = FALL_DURATION_PER_CELL * distance
                interpolator = BounceInterpolator()
                addUpdateListener { candy.animationOffsetY = it.animatedValue as Float }
            }
            animators.add(fallAnim)
        }

        // Animate new candies falling in from above
        for ((candy, distance) in newCandies) {
            val startOffset = -(distance + 1) * cellSize
            candy.animationOffsetY = startOffset
            candy.alpha = 0f

            val fallAnim = ValueAnimator.ofFloat(startOffset, 0f).apply {
                duration = FALL_DURATION_PER_CELL * (distance + 1)
                interpolator = BounceInterpolator()
                addUpdateListener { candy.animationOffsetY = it.animatedValue as Float }
            }

            val fadeAnim = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 100
                addUpdateListener { candy.alpha = it.animatedValue as Float }
            }

            val scaleAnim = ValueAnimator.ofFloat(0.5f, 1f).apply {
                duration = FALL_DURATION_PER_CELL * (distance + 1)
                interpolator = OvershootInterpolator(1.5f)
                addUpdateListener { candy.scale = it.animatedValue as Float }
            }

            animators.add(fallAnim)
            animators.add(fadeAnim)
            animators.add(scaleAnim)
        }

        if (animators.isEmpty()) {
            onComplete()
            return
        }

        animatorSet.playTogether(animators)

        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                for ((candy, _) in movingCandies) {
                    candy.resetAnimation()
                }
                for ((candy, _) in newCandies) {
                    candy.resetAnimation()
                }
                gameView.refresh()
                onComplete()
            }
        })

        animatorSet.addUpdateListener { gameView.refresh() }
        animatorSet.start()
    }

    fun animateScore(x: Float, y: Float, score: Int, onComplete: () -> Unit) {
        // Score popup animation is handled by the activity
        onComplete()
    }

    companion object {
        private const val SWAP_DURATION = 200L
        private const val INVALID_SWAP_DURATION = 350L
        private const val MATCH_DURATION = 300L
        private const val FALL_DURATION_PER_CELL = 80L
    }
}

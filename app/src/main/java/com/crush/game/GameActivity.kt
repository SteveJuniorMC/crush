package com.crush.game

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.crush.game.animation.AnimationManager
import com.crush.game.game.*

class GameActivity : AppCompatActivity(), GameListener {

    companion object {
        const val EXTRA_LEVEL_NUMBER = "level_number"
    }

    private lateinit var gameView: GameView
    private lateinit var scoreValue: TextView
    private lateinit var movesValue: TextView
    private lateinit var levelNumber: TextView
    private lateinit var targetValue: TextView

    private lateinit var levelManager: LevelManager
    private lateinit var gameEngine: GameEngine
    private lateinit var animationManager: AnimationManager

    private var currentLevelNumber: Int = 1

    private val handler = Handler(Looper.getMainLooper())
    private var hintRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        currentLevelNumber = intent.getIntExtra(EXTRA_LEVEL_NUMBER, 1)

        initViews()
        initGame()
    }

    private fun initViews() {
        gameView = findViewById(R.id.gameView)
        scoreValue = findViewById(R.id.scoreValue)
        movesValue = findViewById(R.id.movesValue)
        levelNumber = findViewById(R.id.levelNumber)
        targetValue = findViewById(R.id.targetValue)
    }

    private fun initGame() {
        levelManager = LevelManager(this)
        val level = levelManager.getLevel(currentLevelNumber) ?: run {
            finish()
            return
        }

        animationManager = AnimationManager(gameView)
        gameEngine = GameEngine(level, animationManager)
        gameEngine.listener = this

        gameView.setGameEngine(gameEngine)

        // Update UI
        levelNumber.text = currentLevelNumber.toString()
        targetValue.text = level.targetScore.toString()
        updateScore(0)
        updateMoves(level.maxMoves)

        // Set cell size after layout
        gameView.post {
            val cellSize = gameView.width.toFloat() / gameEngine.board.cols
            animationManager.setCellSize(cellSize)
        }

        // Start hint timer
        scheduleHint()
    }

    private fun scheduleHint() {
        hintRunnable?.let { handler.removeCallbacks(it) }
        hintRunnable = Runnable {
            if (gameEngine.isIdle()) {
                gameEngine.board.findHint()?.let { (pos1, pos2) ->
                    gameView.startHintAnimation(pos1, pos2)
                }
            }
        }
        handler.postDelayed(hintRunnable!!, 5000)
    }

    private fun updateScore(score: Int) {
        scoreValue.text = score.toString()
    }

    private fun updateMoves(moves: Int) {
        movesValue.text = moves.toString()

        // Animate when moves are low
        if (moves <= 5) {
            movesValue.setTextColor(ContextCompat.getColor(this, R.color.candy_red))
            if (moves <= 3) {
                pulseView(movesValue)
            }
        }
    }

    private fun pulseView(view: View) {
        ObjectAnimator.ofFloat(view, View.SCALE_X, 1f, 1.3f, 1f).apply {
            duration = 300
            start()
        }
        ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f, 1.3f, 1f).apply {
            duration = 300
            start()
        }
    }

    override fun onScoreChanged(score: Int, delta: Int) {
        // Animate score change
        val startScore = score - delta
        ValueAnimator.ofInt(startScore, score).apply {
            duration = 300
            addUpdateListener {
                scoreValue.text = (it.animatedValue as Int).toString()
            }
            start()
        }

        // Show floating score
        showFloatingScore(delta)

        // Check if target reached
        if (score >= gameEngine.getTargetScore()) {
            targetValue.setTextColor(ContextCompat.getColor(this, R.color.candy_green))
            pulseView(targetValue)
        }

        scheduleHint()
    }

    private fun showFloatingScore(score: Int) {
        val container = findViewById<FrameLayout>(android.R.id.content)

        val textView = TextView(this).apply {
            text = "+$score"
            setTextColor(ContextCompat.getColor(context, R.color.gold))
            textSize = 24f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setShadowLayer(4f, 0f, 2f, Color.BLACK)
        }

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
            topMargin = 200
        }

        container.addView(textView, params)

        textView.animate()
            .translationY(-150f)
            .alpha(0f)
            .scaleX(1.5f)
            .scaleY(1.5f)
            .setDuration(800)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    container.removeView(textView)
                }
            })
            .start()
    }

    override fun onMovesChanged(moves: Int) {
        updateMoves(moves)
        scheduleHint()
    }

    override fun onLevelComplete(stars: Int) {
        gameView.stopHintAnimation()
        hintRunnable?.let { handler.removeCallbacks(it) }

        // Save progress
        levelManager.completeLevel(currentLevelNumber, gameEngine.score)

        // Show level complete dialog
        handler.postDelayed({
            showLevelCompleteDialog(stars)
        }, 500)
    }

    override fun onGameOver() {
        gameView.stopHintAnimation()
        hintRunnable?.let { handler.removeCallbacks(it) }

        handler.postDelayed({
            showGameOverDialog()
        }, 500)
    }

    override fun onNoMoves() {
        gameView.stopHintAnimation()
        hintRunnable?.let { handler.removeCallbacks(it) }

        handler.postDelayed({
            showNoMovesDialog()
        }, 500)
    }

    override fun onStateChanged(state: GameState) {
        // Can be used for additional state-based UI updates
    }

    private fun showLevelCompleteDialog(stars: Int) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_level_complete, null)

        val starsContainer = dialogView.findViewById<LinearLayout>(R.id.starsContainer)
        val scoreText = dialogView.findViewById<TextView>(R.id.scoreText)
        val nextButton = dialogView.findViewById<View>(R.id.nextButton)
        val retryButton = dialogView.findViewById<View>(R.id.retryButton)
        val menuButton = dialogView.findViewById<View>(R.id.menuButton)

        scoreText.text = gameEngine.score.toString()

        // Animate stars
        for (i in 0 until starsContainer.childCount) {
            val star = starsContainer.getChildAt(i) as ImageView
            val delay = (i * 200).toLong()

            star.scaleX = 0f
            star.scaleY = 0f

            if (i < stars) {
                star.setImageResource(R.drawable.ic_star_filled)
                star.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(300)
                    .setStartDelay(delay)
                    .setInterpolator(OvershootInterpolator(2f))
                    .start()
            } else {
                star.setImageResource(R.drawable.ic_star)
                star.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .setStartDelay(delay)
                    .start()
            }
        }

        val dialog = AlertDialog.Builder(this, R.style.Theme_Crush_Dialog)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        nextButton.setOnClickListener {
            dialog.dismiss()
            if (currentLevelNumber < levelManager.getTotalLevels()) {
                currentLevelNumber++
                initGame()
            } else {
                finish()
            }
        }

        retryButton.setOnClickListener {
            dialog.dismiss()
            initGame()
        }

        menuButton.setOnClickListener {
            dialog.dismiss()
            finish()
        }

        dialog.show()
    }

    private fun showGameOverDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_game_over, null)

        val scoreText = dialogView.findViewById<TextView>(R.id.scoreText)
        val targetText = dialogView.findViewById<TextView>(R.id.targetText)
        val retryButton = dialogView.findViewById<View>(R.id.retryButton)
        val menuButton = dialogView.findViewById<View>(R.id.menuButton)

        scoreText.text = gameEngine.score.toString()
        targetText.text = gameEngine.getTargetScore().toString()

        val dialog = AlertDialog.Builder(this, R.style.Theme_Crush_Dialog)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        retryButton.setOnClickListener {
            dialog.dismiss()
            initGame()
        }

        menuButton.setOnClickListener {
            dialog.dismiss()
            finish()
        }

        dialog.show()
    }

    private fun showNoMovesDialog() {
        showGameOverDialog() // Same as game over for now
    }

    override fun onPause() {
        super.onPause()
        hintRunnable?.let { handler.removeCallbacks(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        hintRunnable?.let { handler.removeCallbacks(it) }
    }
}

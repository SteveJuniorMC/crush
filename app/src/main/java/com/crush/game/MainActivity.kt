package com.crush.game

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private lateinit var titleText: TextView
    private lateinit var subtitleText: TextView
    private lateinit var playButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        titleText = findViewById(R.id.titleText)
        subtitleText = findViewById(R.id.subtitleText)
        playButton = findViewById(R.id.playButton)

        playButton.setOnClickListener {
            animateButtonPress {
                startActivity(Intent(this, LevelSelectActivity::class.java))
            }
        }

        // Start entrance animations
        startEntranceAnimation()
    }

    private fun startEntranceAnimation() {
        // Initially hide views
        titleText.alpha = 0f
        titleText.translationY = -100f

        subtitleText.alpha = 0f
        subtitleText.translationY = -50f

        playButton.alpha = 0f
        playButton.scaleX = 0f
        playButton.scaleY = 0f

        // Animate title
        titleText.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(600)
            .setInterpolator(OvershootInterpolator(1.2f))
            .setStartDelay(300)
            .start()

        // Animate subtitle
        subtitleText.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(500)
            .setStartDelay(500)
            .start()

        // Animate play button
        playButton.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(500)
            .setInterpolator(OvershootInterpolator(2f))
            .setStartDelay(700)
            .start()

        // Add subtle pulse animation to play button
        playButton.postDelayed({
            startButtonPulse()
        }, 1500)
    }

    private fun startButtonPulse() {
        val scaleX = ObjectAnimator.ofFloat(playButton, View.SCALE_X, 1f, 1.05f, 1f)
        val scaleY = ObjectAnimator.ofFloat(playButton, View.SCALE_Y, 1f, 1.05f, 1f)

        AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = 1500
            startDelay = 2000
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    if (!isFinishing) {
                        start()
                    }
                }
            })
            start()
        }
    }

    private fun animateButtonPress(onEnd: () -> Unit) {
        playButton.animate()
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(100)
            .withEndAction {
                playButton.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .withEndAction(onEnd)
                    .start()
            }
            .start()
    }
}

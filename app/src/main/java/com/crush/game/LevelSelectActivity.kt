package com.crush.game

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.crush.game.game.LevelManager
import com.crush.game.game.LevelProgress

class LevelSelectActivity : AppCompatActivity() {

    private lateinit var levelGrid: RecyclerView
    private lateinit var levelManager: LevelManager
    private lateinit var adapter: LevelAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_level_select)

        levelManager = LevelManager(this)
        levelGrid = findViewById(R.id.levelGrid)

        setupRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        // Refresh progress when returning from game
        adapter.updateProgress(levelManager.getAllProgress())
    }

    private fun setupRecyclerView() {
        adapter = LevelAdapter(
            levelManager.getAllProgress(),
            levelManager.getTotalLevels()
        ) { levelNumber ->
            val intent = Intent(this, GameActivity::class.java)
            intent.putExtra(GameActivity.EXTRA_LEVEL_NUMBER, levelNumber)
            startActivity(intent)
        }

        levelGrid.layoutManager = GridLayoutManager(this, 4)
        levelGrid.adapter = adapter

        // Add item decoration for spacing
        levelGrid.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: android.graphics.Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                val spacing = resources.getDimensionPixelSize(R.dimen.level_grid_spacing)
                outRect.set(spacing, spacing, spacing, spacing)
            }
        })
    }

    inner class LevelAdapter(
        private var progress: List<LevelProgress>,
        private val totalLevels: Int,
        private val onLevelClick: (Int) -> Unit
    ) : RecyclerView.Adapter<LevelAdapter.LevelViewHolder>() {

        fun updateProgress(newProgress: List<LevelProgress>) {
            progress = newProgress
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LevelViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_level, parent, false)
            return LevelViewHolder(view)
        }

        override fun onBindViewHolder(holder: LevelViewHolder, position: Int) {
            val levelNumber = position + 1
            val levelProgress = progress.find { it.levelNumber == levelNumber }

            holder.bind(levelNumber, levelProgress)
        }

        override fun getItemCount(): Int = totalLevels

        inner class LevelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val levelButton: View = itemView.findViewById(R.id.levelButton)
            private val levelNumberText: TextView = itemView.findViewById(R.id.levelNumber)
            private val lockIcon: ImageView = itemView.findViewById(R.id.lockIcon)
            private val starsContainer: View = itemView.findViewById(R.id.starsContainer)
            private val star1: ImageView = itemView.findViewById(R.id.star1)
            private val star2: ImageView = itemView.findViewById(R.id.star2)
            private val star3: ImageView = itemView.findViewById(R.id.star3)

            fun bind(levelNumber: Int, progress: LevelProgress?) {
                val isUnlocked = progress?.isUnlocked ?: false
                val stars = progress?.stars ?: 0

                levelNumberText.text = levelNumber.toString()

                if (isUnlocked) {
                    levelButton.setBackgroundResource(
                        if (progress?.isCompleted == true)
                            R.drawable.level_button_completed
                        else
                            R.drawable.level_button_background
                    )
                    levelNumberText.visibility = View.VISIBLE
                    lockIcon.visibility = View.GONE
                    starsContainer.visibility = View.VISIBLE

                    // Set star states
                    star1.setImageResource(if (stars >= 1) R.drawable.ic_star_filled else R.drawable.ic_star)
                    star2.setImageResource(if (stars >= 2) R.drawable.ic_star_filled else R.drawable.ic_star)
                    star3.setImageResource(if (stars >= 3) R.drawable.ic_star_filled else R.drawable.ic_star)

                    itemView.setOnClickListener { onLevelClick(levelNumber) }
                    itemView.isClickable = true
                } else {
                    levelButton.setBackgroundResource(R.drawable.level_button_locked)
                    levelNumberText.visibility = View.GONE
                    lockIcon.visibility = View.VISIBLE
                    starsContainer.visibility = View.INVISIBLE

                    itemView.setOnClickListener(null)
                    itemView.isClickable = false
                }
            }
        }
    }
}

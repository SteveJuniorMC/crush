package com.crush.game.game

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import com.crush.game.animation.AnimationManager
import kotlin.math.min

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var gameEngine: GameEngine? = null
    private var cellSize: Float = 0f
    private var boardLeft: Float = 0f
    private var boardTop: Float = 0f
    private var boardPadding: Float = 0f

    private val candyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val selectedPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val boardPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val boardBackgroundColor = Color.parseColor("#0F0F23")
    private val gridLineColor = Color.parseColor("#1A1A2E")
    private val selectionColor = Color.parseColor("#FFD700")

    private var hintAnimator: ValueAnimator? = null
    private var hintAlpha: Float = 0f
    private var hintPositions: Pair<Position, Position>? = null

    private var idleAnimator: ValueAnimator? = null
    private var idleScale: Float = 1f

    // Particle system for effects
    private val particles = mutableListOf<Particle>()
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        setupPaints()
        startIdleAnimation()
    }

    private fun setupPaints() {
        boardPaint.color = boardBackgroundColor
        boardPaint.style = Paint.Style.FILL

        gridPaint.color = gridLineColor
        gridPaint.style = Paint.Style.STROKE
        gridPaint.strokeWidth = 2f

        selectedPaint.color = selectionColor
        selectedPaint.style = Paint.Style.STROKE
        selectedPaint.strokeWidth = 6f

        shadowPaint.color = Color.parseColor("#40000000")
        shadowPaint.maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)

        glowPaint.maskFilter = BlurMaskFilter(16f, BlurMaskFilter.Blur.OUTER)

        particlePaint.color = Color.WHITE
    }

    fun setGameEngine(engine: GameEngine) {
        gameEngine = engine
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateDimensions()
    }

    private fun calculateDimensions() {
        val board = gameEngine?.board ?: return
        boardPadding = width * 0.02f
        val availableSize = min(width.toFloat(), height.toFloat()) - (boardPadding * 2)
        cellSize = availableSize / board.cols
        boardLeft = (width - (cellSize * board.cols)) / 2
        boardTop = (height - (cellSize * board.rows)) / 2
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val engine = gameEngine ?: return
        val board = engine.board

        if (cellSize == 0f) calculateDimensions()

        // Draw board background with rounded corners
        val boardRect = RectF(
            boardLeft - boardPadding,
            boardTop - boardPadding,
            boardLeft + (cellSize * board.cols) + boardPadding,
            boardTop + (cellSize * board.rows) + boardPadding
        )
        canvas.drawRoundRect(boardRect, 24f, 24f, boardPaint)

        // Draw grid lines
        for (row in 0..board.rows) {
            val y = boardTop + (row * cellSize)
            canvas.drawLine(boardLeft, y, boardLeft + (cellSize * board.cols), y, gridPaint)
        }
        for (col in 0..board.cols) {
            val x = boardLeft + (col * cellSize)
            canvas.drawLine(x, boardTop, x, boardTop + (cellSize * board.rows), gridPaint)
        }

        // Draw hint if available
        hintPositions?.let { (pos1, pos2) ->
            if (hintAlpha > 0) {
                selectedPaint.alpha = (hintAlpha * 255).toInt()
                drawCellHighlight(canvas, pos1, selectedPaint)
                drawCellHighlight(canvas, pos2, selectedPaint)
                selectedPaint.alpha = 255
            }
        }

        // Draw selected cell
        engine.getSelectedPosition()?.let { pos ->
            drawCellHighlight(canvas, pos, selectedPaint)
        }

        // Draw candies
        for (candy in board.getAllCandies()) {
            drawCandy(canvas, candy)
        }

        // Draw particles
        drawParticles(canvas)
    }

    private fun drawCellHighlight(canvas: Canvas, pos: Position, paint: Paint) {
        val x = boardLeft + (pos.col * cellSize)
        val y = boardTop + (pos.row * cellSize)
        val padding = cellSize * 0.05f
        val rect = RectF(x + padding, y + padding, x + cellSize - padding, y + cellSize - padding)
        canvas.drawRoundRect(rect, 12f, 12f, paint)
    }

    private fun drawCandy(canvas: Canvas, candy: Candy) {
        val baseX = boardLeft + (candy.col * cellSize) + (cellSize / 2) + candy.animationOffsetX
        val baseY = boardTop + (candy.row * cellSize) + (cellSize / 2) + candy.animationOffsetY
        val radius = (cellSize * 0.38f) * candy.scale * idleScale

        if (candy.alpha <= 0) return

        canvas.save()
        canvas.translate(baseX, baseY)
        canvas.rotate(candy.rotation)

        // Draw shadow
        shadowPaint.alpha = (candy.alpha * 100).toInt()
        canvas.drawCircle(4f, 4f, radius, shadowPaint)

        // Draw candy based on type
        candyPaint.alpha = (candy.alpha * 255).toInt()
        drawCandyShape(canvas, candy, radius)

        canvas.restore()
    }

    private fun drawCandyShape(canvas: Canvas, candy: Candy, radius: Float) {
        val type = candy.type

        // Create gradient for 3D effect
        val gradient = RadialGradient(
            -radius * 0.3f, -radius * 0.3f, radius * 2f,
            intArrayOf(type.accentColor, type.primaryColor, type.darkColor),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        candyPaint.shader = gradient

        when (type) {
            CandyType.RED -> drawCircle(canvas, radius)
            CandyType.ORANGE -> drawRoundedSquare(canvas, radius)
            CandyType.YELLOW -> drawDiamond(canvas, radius)
            CandyType.GREEN -> drawTriangle(canvas, radius)
            CandyType.BLUE -> drawPentagon(canvas, radius)
            CandyType.PURPLE -> drawHexagon(canvas, radius)
        }

        candyPaint.shader = null

        // Draw highlight/shine
        drawShine(canvas, radius, type.accentColor)
    }

    private fun drawCircle(canvas: Canvas, radius: Float) {
        canvas.drawCircle(0f, 0f, radius, candyPaint)
    }

    private fun drawRoundedSquare(canvas: Canvas, radius: Float) {
        val size = radius * 0.85f
        val rect = RectF(-size, -size, size, size)
        canvas.drawRoundRect(rect, radius * 0.3f, radius * 0.3f, candyPaint)
    }

    private fun drawDiamond(canvas: Canvas, radius: Float) {
        val path = Path()
        path.moveTo(0f, -radius)
        path.lineTo(radius, 0f)
        path.lineTo(0f, radius)
        path.lineTo(-radius, 0f)
        path.close()
        canvas.drawPath(path, candyPaint)
    }

    private fun drawTriangle(canvas: Canvas, radius: Float) {
        val path = Path()
        val angle = Math.PI * 2 / 3
        path.moveTo(0f, -radius)
        for (i in 1..2) {
            val x = (Math.sin(angle * i) * radius).toFloat()
            val y = (-Math.cos(angle * i) * radius).toFloat()
            path.lineTo(x, y)
        }
        path.close()
        canvas.drawPath(path, candyPaint)
    }

    private fun drawPentagon(canvas: Canvas, radius: Float) {
        val path = Path()
        val angle = Math.PI * 2 / 5
        path.moveTo(0f, -radius)
        for (i in 1..4) {
            val x = (Math.sin(angle * i) * radius).toFloat()
            val y = (-Math.cos(angle * i) * radius).toFloat()
            path.lineTo(x, y)
        }
        path.close()
        canvas.drawPath(path, candyPaint)
    }

    private fun drawHexagon(canvas: Canvas, radius: Float) {
        val path = Path()
        val angle = Math.PI * 2 / 6
        path.moveTo(0f, -radius)
        for (i in 1..5) {
            val x = (Math.sin(angle * i) * radius).toFloat()
            val y = (-Math.cos(angle * i) * radius).toFloat()
            path.lineTo(x, y)
        }
        path.close()
        canvas.drawPath(path, candyPaint)
    }

    private fun drawShine(canvas: Canvas, radius: Float, color: Int) {
        val shinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        shinePaint.color = Color.WHITE
        shinePaint.alpha = 80

        val shineRadius = radius * 0.25f
        canvas.drawCircle(-radius * 0.35f, -radius * 0.35f, shineRadius, shinePaint)
    }

    private fun drawParticles(canvas: Canvas) {
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val particle = iterator.next()
            if (particle.isDead()) {
                iterator.remove()
            } else {
                particle.update()
                particlePaint.color = particle.color
                particlePaint.alpha = (particle.alpha * 255).toInt()
                canvas.drawCircle(particle.x, particle.y, particle.size, particlePaint)
            }
        }

        if (particles.isNotEmpty()) {
            invalidate()
        }
    }

    fun addParticles(centerX: Float, centerY: Float, color: Int, count: Int = 12) {
        repeat(count) {
            particles.add(Particle(centerX, centerY, color))
        }
        invalidate()
    }

    fun getCandyCenter(row: Int, col: Int): Pair<Float, Float> {
        val x = boardLeft + (col * cellSize) + (cellSize / 2)
        val y = boardTop + (row * cellSize) + (cellSize / 2)
        return x to y
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN) return true

        val engine = gameEngine ?: return true
        if (!engine.isIdle()) return true

        val touchX = event.x
        val touchY = event.y

        val col = ((touchX - boardLeft) / cellSize).toInt()
        val row = ((touchY - boardTop) / cellSize).toInt()

        if (row in 0 until engine.board.rows && col in 0 until engine.board.cols) {
            engine.selectCandy(row, col)
            stopHintAnimation()
            invalidate()
        }

        return true
    }

    private fun startIdleAnimation() {
        idleAnimator = ValueAnimator.ofFloat(1f, 1.02f, 1f).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                idleScale = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun startHintAnimation(pos1: Position, pos2: Position) {
        hintPositions = pos1 to pos2
        hintAnimator?.cancel()
        hintAnimator = ValueAnimator.ofFloat(0f, 1f, 0f).apply {
            duration = 1500
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                hintAlpha = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun stopHintAnimation() {
        hintAnimator?.cancel()
        hintPositions = null
        hintAlpha = 0f
    }

    fun refresh() {
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        hintAnimator?.cancel()
        idleAnimator?.cancel()
    }
}

class Particle(
    startX: Float,
    startY: Float,
    val color: Int
) {
    var x: Float = startX
    var y: Float = startY
    var vx: Float = (Math.random() * 8 - 4).toFloat()
    var vy: Float = (Math.random() * -8 - 2).toFloat()
    var size: Float = (Math.random() * 6 + 3).toFloat()
    var alpha: Float = 1f
    private var life: Int = 30 + (Math.random() * 20).toInt()

    fun update() {
        x += vx
        y += vy
        vy += 0.3f // gravity
        alpha -= 1f / life
        size *= 0.96f
    }

    fun isDead(): Boolean = alpha <= 0 || size < 1
}

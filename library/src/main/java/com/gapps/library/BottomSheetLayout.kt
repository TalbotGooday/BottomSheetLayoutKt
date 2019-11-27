package com.gapps.library

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import kotlin.math.abs

class BottomSheetLayout : FrameLayout {
	companion object {
		private const val CLICK_ACTION_THRESHOLD = 200
		private const val DEFAULT_CORNER_RADIUS = 30.0f
	}

	private lateinit var valueAnimator: ValueAnimator
	private var collapsedHeight: Int = 0

	private var indentTop: Int = 0

	private var progress = 0f
	private var startsCollapsed = true

	private var scrollTranslationY = 0f
	private var userTranslationY = 0f

	private var isScrollingUp: Boolean = false

	private var clickListener: OnClickListener? = null

	private var animationDuration: Long = 300
	private var mCornerRadius = DEFAULT_CORNER_RADIUS

	var isSwipeEnabled = true

	private var mTopLeftEnabled = true
	private var mTopRightEnabled = true

	override fun setOnClickListener(l: OnClickListener?) {
		clickListener = l
	}

	private var progressListener: OnProgressListener? = null

	fun setOnProgressListener(l: OnProgressListener?) {
		progressListener = l
	}

	private val touchToDragListener = TouchToDragListener(true)

	fun setOnProgressListener(l: (progress: Float) -> Unit) {
		progressListener = object : OnProgressListener {
			override fun onProgress(progress: Float) {
				l(progress)
			}
		}
	}

	fun isExpended(): Boolean {
		return progress == 1f
	}

	constructor(context: Context) : super(context) {
		initView(null)
	}

	constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
		initView(attrs)
	}

	constructor(context: Context, attrs: AttributeSet?, @AttrRes defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
		initView(attrs)
	}

	override fun setTranslationY(translationY: Float) {
		userTranslationY = translationY

		super.setTranslationY(scrollTranslationY + userTranslationY)
	}

	override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
		super.onLayout(changed, left, top, right, bottom)

		minimumHeight = minimumHeight.coerceAtLeast(collapsedHeight)

		collapsedHeight = minimumHeight

		if (indentTop != 0) {
			translationY = indentTop.toFloat()
		}

		addOnLayoutChangeListener(object : OnLayoutChangeListener {
			override fun onLayoutChange(view: View, i: Int, i1: Int, i2: Int, i3: Int, i4: Int, i5: Int, i6: Int, i7: Int) {
				removeOnLayoutChangeListener(this)
				animate(1f)
			}
		})
	}

	private fun initView(attrs: AttributeSet?) {
		val a = context.obtainStyledAttributes(attrs, R.styleable.BottomSheetLayout)
		isSwipeEnabled = a.getBoolean(R.styleable.BottomSheetLayout_isSwipable, true)

		collapsedHeight = a.getDimensionPixelSize(R.styleable.BottomSheetLayout_collapsedHeight, -1)

		mCornerRadius = a.getDimension(R.styleable.BottomSheetLayout_cornersRadius, DEFAULT_CORNER_RADIUS)

		indentTop = a.getDimensionPixelSize(R.styleable.BottomSheetLayout_indentTop, 0)

		a.recycle()

		valueAnimator = ValueAnimator.ofFloat(0f, 1f)

		setOnTouchListener(touchToDragListener)
	}

	override fun dispatchDraw(canvas: Canvas) {
		val count = canvas.save()

		val path = Path()
		val rect = RectF(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat())
		val arrayRadius = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)

		if (mTopLeftEnabled) {
			arrayRadius[0] = mCornerRadius
			arrayRadius[1] = mCornerRadius
		}

		if (mTopRightEnabled) {
			arrayRadius[2] = mCornerRadius
			arrayRadius[3] = mCornerRadius
		}

		path.addRoundRect(rect, arrayRadius, Path.Direction.CW)
		canvas.clipPath(path)

		super.dispatchDraw(canvas)

		canvas.restoreToCount(count)
	}

	fun toggle() {
		if (valueAnimator.isRunning) {
			valueAnimator.cancel()
		}

		val duration: Long

		if (progress > 0.5f) {
			duration = (animationDuration * progress).toLong()
			valueAnimator = ValueAnimator.ofFloat(progress, 0f)
		} else {
			duration = (animationDuration * (1 - progress)).toLong()
			valueAnimator = ValueAnimator.ofFloat(progress, 1f)
		}

		valueAnimator.addUpdateListener { animation ->
			val progress = animation.animatedValue as Float
			animate(progress)
		}

		valueAnimator.duration = duration

		valueAnimator.start()
	}

	fun collapse() {
		if (valueAnimator.isRunning) {
			valueAnimator.cancel()
		}

		valueAnimator = ValueAnimator.ofFloat(progress, 0f).apply {
			duration = (animationDuration * progress).toLong()

			addUpdateListener { animation ->
				val progress = animation.animatedValue as Float
				animate(progress)
			}

			start()
		}
	}

	fun expand() {
		if (valueAnimator.isRunning) {
			valueAnimator.cancel()
		}

		valueAnimator = ValueAnimator.ofFloat(progress, 1f).apply {
			duration = (animationDuration * (1 - progress)).toLong()

			addUpdateListener { animation ->
				val progress = animation.animatedValue as Float
				animate(progress)
			}

			start()
		}
	}

	//1 is expanded, 0 is collapsed
	private fun animate(progress: Float) {
		this.progress = progress
		val height = height - indentTop
		val distance = height - collapsedHeight
		scrollTranslationY = distance * (1 - progress)

		progressListener?.onProgress(progress)

		super.setTranslationY(scrollTranslationY + userTranslationY)
	}

	private fun animateScroll(firstPos: Float, touchPos: Float) {
		val distance = touchPos - firstPos
		val height = height
		val totalDistance = height - collapsedHeight
		var progress: Float

		if (startsCollapsed) {
			isScrollingUp = true
			progress = 1f.coerceAtMost(-distance / totalDistance)
		} else {
			isScrollingUp = false
			progress = 0f.coerceAtLeast(1 - distance / totalDistance)
		}

		progress = 0f.coerceAtLeast(1f.coerceAtMost(progress))

		animate(progress)
	}

	private fun animateScrollEnd() {
		if (valueAnimator.isRunning) {
			valueAnimator.cancel()
		}

		val duration: Long
		val progressLimit = if (isScrollingUp) 0.2f else 0.8f

		if (progress > progressLimit) {
			duration = (animationDuration * (1 - progress)).toLong()
			valueAnimator = ValueAnimator.ofFloat(progress, 1f)
		} else {
			duration = (animationDuration * progress).toLong()
			valueAnimator = ValueAnimator.ofFloat(progress, 0f)
		}

		valueAnimator.addUpdateListener { animation ->
			val progress = animation.animatedValue as Float
			animate(progress)
		}

		valueAnimator.duration = duration

		valueAnimator.start()
	}

	override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
		ev ?: return false

		return touchToDragListener.onTouch(this, ev)
	}

	private fun performChildClick(eventX: Float, eventY: Float): Boolean {
		return performChildClick(eventX, eventY, this, 0)
	}

	private fun performChildClick(eventX: Float, eventY: Float, viewGroup: ViewGroup, nest: Int): Boolean {

		for (i in (viewGroup.childCount - 1) downTo 0) {
			val view = viewGroup.getChildAt(i)

			if (isViewAtLocation(eventX, eventY, view)) {
				if (view is ViewGroup) {
					val performChildClick = performChildClick(eventX - view.left, eventY - view.top, view, nest + 1)

					if (performChildClick) {
						return true
					}
				}

				if (view.performClick()) {
					return true
				}
			}
		}
		return performClick()
	}

	private fun isViewAtLocation(rawX: Float, rawY: Float, view: View): Boolean {
		if (view.left <= rawX && view.right >= rawX) {
			if (view.top <= rawY && view.bottom >= rawY) {
				return true
			}
		}

		return false
	}

	private fun onClick() {
		clickListener?.onClick(this)
	}

	private inner class TouchToDragListener(private val touchToDrag: Boolean) : OnTouchListener {
		private var startX: Float = 0.toFloat()
		private var startY: Float = 0.toFloat()
		private var startTime: Double = 0.toDouble()

		@SuppressLint("ClickableViewAccessibility")
		override fun onTouch(v: View, ev: MotionEvent): Boolean {
			if (isSwipeEnabled.not()) return false

			when (ev.action) {
				MotionEvent.ACTION_DOWN -> {
					if (ev.pointerCount == 1) {
						startX = ev.rawX
						startY = ev.rawY
						startTime = System.currentTimeMillis().toDouble()
						startsCollapsed = progress < 0.5
					}
				}

				MotionEvent.ACTION_MOVE -> {
					val y = ev.rawY

					animateScroll(startY, y)

					invalidate()
				}

				MotionEvent.ACTION_UP -> {

					val endX = ev.rawX
					val endY = ev.rawY
					if (isAClick(startX, endX, startY, endY, System.currentTimeMillis())) {
						if (performChildClick(ev.x, ev.y)) {
							return true
						}

						if (touchToDrag && clickListener != null) {
							onClick()
							return true
						}
					}

					animateScrollEnd()
				}
			}
			return true
		}

		private fun isAClick(startX: Float, endX: Float, startY: Float, endY: Float, endTime: Long): Boolean {
			val differenceX = abs(startX - endX)
			val differenceY = abs(startY - endY)
			val differenceTime = abs(startTime - endTime)

			return !(differenceX > CLICK_ACTION_THRESHOLD || differenceY > CLICK_ACTION_THRESHOLD || differenceTime > 400)
		}
	}

	interface OnProgressListener {
		fun onProgress(progress: Float)
	}
}
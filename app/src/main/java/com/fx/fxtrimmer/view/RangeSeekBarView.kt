package com.fx.fxtrimmer.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.NonNull
import androidx.core.content.ContextCompat
import com.fx.fxtrimmer.R
import com.fx.fxtrimmer.interfaces.OnRangeSeekBarListener
import java.util.*

class RangeSeekBarView @JvmOverloads constructor(
    @NonNull context: Context, attrs: AttributeSet,
    defStyleAttr: Int = 0
) :
    View(context, attrs, defStyleAttr) {

    private var mHeightTimeLine: Int = 0
    var thumbs: List<Thumb>? = null
        private set
    private var mListeners: MutableList<OnRangeSeekBarListener>? = null
    private var mMaxWidth: Float = 0.toFloat()
    private var mThumbWidth: Float = 0.toFloat()
    private var mThumbHeight: Float = 0.toFloat()
    private var mViewWidth: Int = 0
    private var mPixelRangeMin: Float = 0.toFloat()
    private var mPixelRangeMax: Float = 0.toFloat()
    private var mScaleRangeMax: Float = 0.toFloat()
    private var mFirstRun: Boolean = false

    private val mShadow = Paint()
    private var currentThumb = 0

    init {
        init()
    }

    fun init() {
        thumbs = Thumb.initThumbs(resources)
        mThumbWidth = Thumb.getWidthBitmap(thumbs!!).toFloat()
        mThumbHeight = Thumb.getHeightBitmap(thumbs!!).toFloat()

        mScaleRangeMax = 100f
        mHeightTimeLine = context.resources.getDimensionPixelOffset(R.dimen.frames_video_height)

        isFocusable = true
        isFocusableInTouchMode = true

        mFirstRun = true

        val shadowColor = ContextCompat.getColor(context, R.color.shadow_color)
        mShadow.isAntiAlias = true
        mShadow.color = shadowColor
        mShadow.alpha = 177

    }

    fun initMaxWidth() {
        mMaxWidth = thumbs!![1].getPos() - thumbs!![0].getPos()

        onSeekStop(this, 0, thumbs!![0].getVal())
        onSeekStop(this, 1, thumbs!![1].getVal())
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val minW = paddingLeft + paddingRight + suggestedMinimumWidth
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mViewWidth = View.resolveSizeAndState(minW, widthMeasureSpec, 1)
        }

        val minH = paddingBottom + paddingTop + mThumbHeight.toInt() + mHeightTimeLine
        var viewHeight = 0
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            viewHeight = View.resolveSizeAndState(minH, heightMeasureSpec, 1)
        }

        setMeasuredDimension(mViewWidth, viewHeight)

        mPixelRangeMin = 0f
        mPixelRangeMax = mViewWidth - mThumbWidth

        if (mFirstRun) {
            for (i in thumbs!!.indices) {
                val th = thumbs!![i]
                th.setVal(mScaleRangeMax * i)
                th.setPos(mPixelRangeMax * i)
            }
            onCreate(this, currentThumb, getThumbValue(currentThumb))
            mFirstRun = false
        }
    }

    override fun onDraw(@NonNull canvas: Canvas) {
        super.onDraw(canvas)

        drawShadow(canvas)
        drawThumbs(canvas)
    }

    override fun onTouchEvent(@NonNull ev: MotionEvent): Boolean {
        val mThumb: Thumb
        val mThumb2: Thumb
        val coordinate = ev.x
        val action = ev.action

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                currentThumb = getClosestThumb(coordinate)

                if (currentThumb == -1) {
                    return false
                }

                mThumb = thumbs!![currentThumb]
                mThumb.set_LastTouchX(coordinate)
                onSeekStart(this, currentThumb, mThumb.getVal())
                return true
            }
            MotionEvent.ACTION_UP -> {

                if (currentThumb == -1) {
                    return false
                }

                mThumb = thumbs!![currentThumb]
                onSeekStop(this, currentThumb, mThumb.getVal())
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                mThumb = thumbs!![currentThumb]
                mThumb2 = thumbs!![if (currentThumb == 0) 1 else 0]
                // Calculate the distance moved
                val dx = coordinate - mThumb.get_LastTouchX()
                val newX = mThumb.getPos() + dx
                if (currentThumb == 0) {

                    if (newX + mThumb.widthBitmap >= mThumb2.getPos()) {
                        mThumb.setPos(mThumb2.getPos() - mThumb.widthBitmap)
                    } else if (newX <= mPixelRangeMin) {
                        mThumb.setPos(mPixelRangeMin)
                    } else {
                        checkPositionThumb(mThumb, mThumb2, dx, true)
                        mThumb.setPos(mThumb.getPos() + dx)
                        mThumb.set_LastTouchX(coordinate)
                    }

                } else {
                    if (newX <= mThumb2.getPos() + mThumb2.widthBitmap) {
                        mThumb.setPos(mThumb2.getPos() + mThumb.widthBitmap)
                    } else if (newX >= mPixelRangeMax) {
                        mThumb.setPos(mPixelRangeMax)
                    } else {
                        checkPositionThumb(mThumb2, mThumb, dx, false)
                        mThumb.setPos(mThumb.getPos() + dx)
                        mThumb.set_LastTouchX(coordinate)
                    }
                }

                setThumbPos(currentThumb, mThumb.getPos())

                invalidate()
                return true
            }
        }
        return false
    }

    private fun checkPositionThumb(
        @NonNull mThumbLeft: Thumb, @NonNull mThumbRight: Thumb, dx: Float,
        isLeftMove: Boolean
    ) {
        if (isLeftMove && dx < 0) {
            if (mThumbRight.getPos() - (mThumbLeft.getPos() + dx) > mMaxWidth) {
                mThumbRight.setPos(mThumbLeft.getPos() + dx + mMaxWidth)
                setThumbPos(1, mThumbRight.getPos())
            }
        } else if (!isLeftMove && dx > 0) {
            if (mThumbRight.getPos() + dx - mThumbLeft.getPos() > mMaxWidth) {
                mThumbLeft.setPos(mThumbRight.getPos() + dx - mMaxWidth)
                setThumbPos(0, mThumbLeft.getPos())
            }
        }
    }


    private fun pixelToScale(index: Int, pixelValue: Float): Float {
        val scale = pixelValue * 100 / mPixelRangeMax
        if (index == 0) {
            val pxThumb = scale * mThumbWidth / 100
            return scale + pxThumb * 100 / mPixelRangeMax
        } else {
            val pxThumb = (100 - scale) * mThumbWidth / 100
            return scale - pxThumb * 100 / mPixelRangeMax
        }
    }

    private fun scaleToPixel(index: Int, scaleValue: Float): Float {
        val px = scaleValue * mPixelRangeMax / 100
        if (index == 0) {
            val pxThumb = scaleValue * mThumbWidth / 100
            return px - pxThumb
        } else {
            val pxThumb = (100 - scaleValue) * mThumbWidth / 100
            return px + pxThumb
        }
    }

    private fun calculateThumbValue(index: Int) {
        if (index < thumbs!!.size && !thumbs!!.isEmpty()) {
            val th = thumbs!![index]
            th.setVal(pixelToScale(index, th.getPos()))
            onSeek(this, index, th.getVal())
        }
    }

    private fun calculateThumbPos(index: Int) {
        if (index < thumbs!!.size && !thumbs!!.isEmpty()) {
            val th = thumbs!![index]
            th.setPos(scaleToPixel(index, th.getVal()))
        }
    }

    private fun getThumbValue(index: Int): Float {
        return thumbs!![index].getVal()
    }

    fun setThumbValue(index: Int, value: Float) {
        thumbs!![index].setVal(value)
        calculateThumbPos(index)
        invalidate()
    }

    private fun setThumbPos(index: Int, pos: Float) {
        thumbs!![index].setPos(pos)
        calculateThumbValue(index)
        invalidate()
    }

    private fun getClosestThumb(coordinate: Float): Int {
        var closest = -1
        if (!thumbs!!.isEmpty()) {
            for (i in thumbs!!.indices) {
                val tcoordinate = thumbs!![i].getPos() + mThumbWidth
                if (coordinate >= thumbs!![i].getPos() && coordinate <= tcoordinate) {
                    closest = thumbs!![i].getIndex()
                }
            }
        }
        return closest
    }

    private fun drawShadow(@NonNull canvas: Canvas) {
        if (!thumbs!!.isEmpty()) {

            for (th in thumbs!!) {
                if (th.getIndex() === 0) {
                    val x = th.getPos() + paddingLeft
                    if (x > mPixelRangeMin) {
                        val mRect =
                            Rect(
                                mThumbWidth.toInt() - 50,
                                0,
                                (x + mThumbWidth).toInt() - 50,
                                mHeightTimeLine
                            )
                        canvas.drawRect(mRect, mShadow)
                    }
                } else {
                    val x = th.getPos() - paddingRight
                    if (x < mPixelRangeMax) {
                        val mRect =
                            Rect(
                                x.toInt() + 50,
                                0,
                                (mViewWidth - mThumbWidth).toInt() + 50,
                                mHeightTimeLine
                            )
                        canvas.drawRect(mRect, mShadow)
                    }
                }
            }
        }
    }

    private fun drawThumbs(@NonNull canvas: Canvas) {

        if (!thumbs!!.isEmpty()) {
            for (th in thumbs!!) {
                if (th.getIndex() === 0) {
                    canvas.drawBitmap(
                        th.bitmap,
                        th.getPos() + paddingLeft,
                        paddingTop.toFloat(),
                        Paint()
                    )
                } else {
                    canvas.drawBitmap(
                        th.bitmap,
                        th.getPos() - paddingRight,
                        paddingTop.toFloat(),
                        Paint()
                    )
                }
            }
        }
    }

    fun addOnRangeSeekBarListener(listener: OnRangeSeekBarListener) {

        if (mListeners == null) {
            mListeners = ArrayList<OnRangeSeekBarListener>()
        }

        mListeners!!.add(listener)
    }

    private fun onCreate(rangeSeekBarView: RangeSeekBarView, index: Int, value: Float) {
        if (mListeners == null)
            return

        for (item in mListeners!!) {
            item.onCreate(rangeSeekBarView, index, value)
        }
    }

    private fun onSeek(rangeSeekBarView: RangeSeekBarView, index: Int, value: Float) {
        if (mListeners == null)
            return

        for (item in mListeners!!) {
            item.onSeek(rangeSeekBarView, index, value)
        }
    }

    private fun onSeekStart(rangeSeekBarView: RangeSeekBarView, index: Int, value: Float) {
        if (mListeners == null)
            return

        for (item in mListeners!!) {
            item.onSeekStart(rangeSeekBarView, index, value)
        }
    }

    private fun onSeekStop(rangeSeekBarView: RangeSeekBarView, index: Int, value: Float) {
        if (mListeners == null)
            return

        for (item in mListeners!!) {
            item.onSeekStop(rangeSeekBarView, index, value)
        }
    }

}

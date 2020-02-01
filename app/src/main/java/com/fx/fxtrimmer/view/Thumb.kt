package com.fx.fxtrimmer.view

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.NonNull
import com.fx.fxtrimmer.R
import java.util.*

class Thumb private constructor() {

    var mIndex: Int = 0
        private set
    var mVal: Float = 0.toFloat()
    var mPos: Float = 0.toFloat()
    var bitmap: Bitmap? = null
        private set(@NonNull bitmap) {
            field = bitmap
            widthBitmap = bitmap!!.getWidth()
            heightBitmap = bitmap!!.getHeight()
//            widthBitmap = 50
//            heightBitmap = 100
        }
    var widthBitmap: Int = 0
        private set
    private var heightBitmap: Int = 0

    var mLastTouchX: Float = 0.toFloat()
    init {
        mVal = 0f
        mPos = 0f
    }

    fun getIndex(): Int {
        return mIndex
    }


    fun getVal(): Float {
        return mVal
    }

    fun setVal(`val`: Float) {
        mVal = `val`
    }

    fun getPos(): Float {
        return mPos
    }

    fun setPos(pos: Float) {
        mPos = pos
    }
    fun get_LastTouchX(): Float {
        return mLastTouchX
    }

    fun set_LastTouchX(lastTouchX: Float) {
        mLastTouchX = lastTouchX
    }

    companion object {

        val LEFT = 0
        val RIGHT = 1

        @NonNull
        fun initThumbs(resources: Resources): List<Thumb> {

            val thumbs = Vector<Thumb>()

            for (i in 0..1) {
                val th = Thumb()
                th.mIndex = i
//                th.widthBitmap = 50
//                th.heightBitmap = 100
                if (i == 0) {
                    val resImageLeft = R.drawable.ic_trimmer_witbar_left
                    th.bitmap = BitmapFactory.decodeResource(resources, resImageLeft)
                } else {
                    val resImageRight = R.drawable.ic_trimmer_witbar_left
                    th.bitmap = BitmapFactory.decodeResource(resources, resImageRight)
                }

                thumbs.add(th)
            }

            return thumbs
        }

        fun getWidthBitmap(@NonNull thumbs: List<Thumb>): Int {
            return thumbs[0].widthBitmap
        }

        fun getHeightBitmap(@NonNull thumbs: List<Thumb>): Int {
            return thumbs[0].heightBitmap
        }
    }
}

package com.fx.fxtrimmer

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Message
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import com.fx.fxtrimmer.interfaces.OnProgressVideoListener
import com.fx.fxtrimmer.interfaces.OnRangeSeekBarListener
import com.fx.fxtrimmer.interfaces.OnTrimVideoListener
import com.fx.fxtrimmer.interfaces.OnVideoListener
import com.fx.fxtrimmer.utils.BackgroundExecutor
import com.fx.fxtrimmer.utils.TrimVideoUtils.stringForTime
import com.fx.fxtrimmer.utils.UiThreadExecutor
import com.fx.fxtrimmer.view.ProgressBarView
import com.fx.fxtrimmer.view.RangeSeekBarView
import com.fx.fxtrimmer.view.Thumb
import com.fx.fxtrimmer.view.TimeLineView
import java.lang.ref.WeakReference
import java.util.*

class VideoTrimmer @JvmOverloads constructor(
    @NonNull context: Context, attrs: AttributeSet,
    defStyleAttr: Int = 0
) :
    FrameLayout(context, attrs, defStyleAttr) {

    private var mHolderTopView: SeekBar? = null
    private var mRangeSeekBarView: RangeSeekBarView? = null
    private var mLinearVideo: RelativeLayout? = null
    private var mTimeInfoContainer: View? = null
    private var mVideoView: VideoView? = null
    private var mPlayView: ImageView? = null
    private var mTextTimeFrame: TextView? = null
    private var mTextTime: TextView? = null
    private var mTimeLineView: TimeLineView? = null

    private var mVideoProgressIndicator: ProgressBarView? = null
    private var mVideoProgressIndicator_top: ProgressBarView? = null

    private var mSrc: Uri? = null
//    private var mFinalPath: String? = null

    private var mMaxDuration: Int = 0
    private var mListeners: MutableList<OnProgressVideoListener>? = null

    private var mOnTrimVideoListener: OnTrimVideoListener? = null
    private var mOnVideoListener: OnVideoListener? = null

    private var mDuration = 0
    private var mStartPosition = 0
    private var mEndPosition = 0

    private var mResetSeekBar = true
    private val mMessageHandler = MessageHandler(this)

    init {
        init(context)
    }

    private fun init(context: Context) {
        LayoutInflater.from(context).inflate(R.layout.view_time_line, this, true)

        mHolderTopView = findViewById(R.id.handlerTop) as SeekBar
        mVideoProgressIndicator = findViewById(R.id.timeVideoView) as ProgressBarView
        mVideoProgressIndicator_top = findViewById(R.id.timeVideoView_top) as ProgressBarView

        mRangeSeekBarView = findViewById(R.id.timeLineBar) as RangeSeekBarView
        mLinearVideo = findViewById(R.id.layout_surface_view) as RelativeLayout
        mVideoView = findViewById(R.id.video_loader) as VideoView
        mPlayView = findViewById(R.id.icon_video_play) as ImageView
        mTimeInfoContainer = findViewById(R.id.timeText)
        mTextTimeFrame = findViewById(R.id.textTimeSelection) as TextView
        mTextTime = findViewById(R.id.textTime) as TextView
        mTimeLineView = findViewById(R.id.timeLineView) as TimeLineView

        setUpListeners()
        setUpMargins()
    }

    private fun setUpListeners() {
        mListeners = ArrayList<OnProgressVideoListener>()
        mListeners!!.add(object : OnProgressVideoListener {
            override fun updateProgress(time: Int, max: Int, scale: Float) {
                updateVideoProgress(time)
            }
        })
        mListeners!!.add(mVideoProgressIndicator!!)
        mListeners!!.add(mVideoProgressIndicator_top!!)


//        findViewById(R.id.btCancel)
//            .setOnClickListener(
//                OnClickListener { onCancelClicked() }
//            )


        val gestureDetector = GestureDetector(
            context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    onClickVideoPlayPause()
                    return true
                }
            }
        )

        mVideoView!!.setOnErrorListener { mediaPlayer, what, extra ->
            if (mOnTrimVideoListener != null)
                mOnTrimVideoListener!!.onError("Something went wrong reason : $what")
            false
        }

        mVideoView!!.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            true
        }

        mRangeSeekBarView!!.addOnRangeSeekBarListener(mVideoProgressIndicator!!)
        mRangeSeekBarView!!.addOnRangeSeekBarListener(mVideoProgressIndicator_top!!)

        mRangeSeekBarView!!.addOnRangeSeekBarListener(object : OnRangeSeekBarListener {
            override fun onCreate(rangeSeekBarView: RangeSeekBarView, index: Int, value: Float) {

            }

            override fun onSeek(rangeSeekBarView: RangeSeekBarView, index: Int, value: Float) {
                onSeekThumbs(index, value)
            }

            override fun onSeekStart(rangeSeekBarView: RangeSeekBarView, index: Int, value: Float) {

            }

            override fun onSeekStop(rangeSeekBarView: RangeSeekBarView, index: Int, value: Float) {
                onStopSeekThumbs()
            }
        })

        mHolderTopView!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                onPlayerIndicatorSeekChanged(progress, fromUser)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                onPlayerIndicatorSeekStart()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                onPlayerIndicatorSeekStop(seekBar)
            }
        })

        mVideoView!!.setOnPreparedListener { mp -> onVideoPrepared(mp) }

        mVideoView!!.setOnCompletionListener { onVideoCompleted() }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private fun setUpMargins() {
        val marge = mRangeSeekBarView!!.thumbs!!.get(0).widthBitmap
//        val marge = 5

        val widthSeek = mHolderTopView!!.thumb.minimumWidth / 2

//        var lp = mHolderTopView!!.layoutParams as RelativeLayout.LayoutParams
//        lp.setMargins(marge - widthSeek, 0, marge - widthSeek, 0)
//        mHolderTopView!!.layoutParams = lp
//
//        lp = mTimeLineView!!.getLayoutParams() as RelativeLayout.LayoutParams
//        lp.setMargins(marge, 0, marge, 0)
//        mTimeLineView!!.setLayoutParams(lp)
//
//        lp = mVideoProgressIndicator!!.getLayoutParams() as RelativeLayout.LayoutParams
//        lp.setMargins(marge, 0, marge, 0)
//        mVideoProgressIndicator!!.setLayoutParams(lp)
//
//        lp = mVideoProgressIndicator_top!!.getLayoutParams() as RelativeLayout.LayoutParams
//        lp.setMargins(marge, 0, marge, 0)
//        mVideoProgressIndicator_top!!.setLayoutParams(lp)

        var lp = mHolderTopView!!.layoutParams as FrameLayout.LayoutParams
        lp.setMargins(marge - widthSeek-20, 170, marge - widthSeek, 0)
        mHolderTopView!!.layoutParams = lp

        lp = mTimeLineView!!.getLayoutParams() as FrameLayout.LayoutParams
        lp.setMargins(marge, 0, marge, 0)
        mTimeLineView!!.setLayoutParams(lp)

        lp = mVideoProgressIndicator!!.getLayoutParams() as FrameLayout.LayoutParams
        lp.setMargins(marge, 0, marge, 0)
        mVideoProgressIndicator!!.setLayoutParams(lp)

        lp = mVideoProgressIndicator_top!!.getLayoutParams() as FrameLayout.LayoutParams
        lp.setMargins(marge, 153, marge, 0)
        mVideoProgressIndicator_top!!.setLayoutParams(lp)
    }

    private fun onClickVideoPlayPause() {
        if (mVideoView!!.isPlaying) {
            mPlayView!!.visibility = View.VISIBLE
            mMessageHandler.removeMessages(SHOW_PROGRESS)
            mVideoView!!.pause()
        } else {
            mPlayView!!.visibility = View.GONE

            if (mResetSeekBar) {
                mResetSeekBar = false
                mVideoView!!.seekTo(mStartPosition)
            }

            mMessageHandler.sendEmptyMessage(SHOW_PROGRESS)
            mVideoView!!.start()
        }
    }

    private fun onCancelClicked() {
        mVideoView!!.stopPlayback()
        if (mOnTrimVideoListener != null) {
            mOnTrimVideoListener!!.cancelAction()
        }
    }

    private fun onPlayerIndicatorSeekChanged(progress: Int, fromUser: Boolean) {

        var duration = (mDuration * progress / 1000L).toInt()

        if (fromUser) {
            if (duration < mStartPosition) {
                setProgressBarPosition(mStartPosition)
                duration = mStartPosition
            } else if (duration > mEndPosition) {
                setProgressBarPosition(mEndPosition)
                duration = mEndPosition
            }
            setTimeVideo(duration)
        }
    }

    private fun onPlayerIndicatorSeekStart() {
        mMessageHandler.removeMessages(SHOW_PROGRESS)
        mVideoView!!.pause()
        mPlayView!!.visibility = View.VISIBLE
        notifyProgressUpdate(false)
    }

    private fun onPlayerIndicatorSeekStop(@NonNull seekBar: SeekBar) {
        mMessageHandler.removeMessages(SHOW_PROGRESS)
        mVideoView!!.pause()
        mPlayView!!.visibility = View.VISIBLE

        val duration = (mDuration * seekBar.progress / 1000L).toInt()
        mVideoView!!.seekTo(duration)
        setTimeVideo(duration)
        notifyProgressUpdate(false)
    }

    private fun onVideoPrepared(@NonNull mp: MediaPlayer) {
        val videoWidth = mp.videoWidth
        val videoHeight = mp.videoHeight
        val videoProportion = videoWidth.toFloat() / videoHeight.toFloat()
        val screenWidth = mLinearVideo!!.width
        val screenHeight = mLinearVideo!!.height
        val screenProportion = screenWidth.toFloat() / screenHeight.toFloat()
        val lp = mVideoView!!.layoutParams

        if (videoProportion > screenProportion) {
            lp.width = screenWidth
            lp.height = (screenWidth.toFloat() / videoProportion).toInt()
        } else {
            lp.width = (videoProportion * screenHeight.toFloat()).toInt()
            lp.height = screenHeight
        }
        mVideoView!!.layoutParams = lp

        mPlayView!!.visibility = View.VISIBLE

        mDuration = mVideoView!!.duration
        setSeekBarPosition()

        setTimeFrames()
        setTimeVideo(0)

        if (mOnVideoListener != null) {
            mOnVideoListener!!.onVideoPrepared()
        }
    }

    private fun setSeekBarPosition() {

        if (mDuration >= mMaxDuration) {
            mStartPosition = mDuration / 2 - mMaxDuration / 2
            mEndPosition = mDuration / 2 + mMaxDuration / 2

            mRangeSeekBarView!!.setThumbValue(0, mStartPosition * 100f / mDuration)
            mRangeSeekBarView!!.setThumbValue(1, mEndPosition * 100f / mDuration)

        } else {
            mStartPosition = 0
            mEndPosition = mDuration
        }

        setProgressBarPosition(mStartPosition)
        mVideoView!!.seekTo(mStartPosition)

        mRangeSeekBarView!!.initMaxWidth()
    }

    private fun setTimeFrames() {
        val seconds = context.getString(R.string.short_seconds)
        mTextTimeFrame!!.text = String.format(
            "%s %s - %s %s",
            stringForTime(mStartPosition),
            seconds,
            stringForTime(mEndPosition),
            seconds
        )
    }

    private fun setTimeVideo(position: Int) {
        val seconds = context.getString(R.string.short_seconds)
        mTextTime!!.text = String.format("%s %s", stringForTime(position), seconds)
    }

    private fun onSeekThumbs(index: Int, value: Float) {
        when (index) {
            Thumb.LEFT -> {
                mStartPosition = (mDuration * value / 100L).toInt()
                mVideoView!!.seekTo(mStartPosition)
            }
            Thumb.RIGHT -> {
                mEndPosition = (mDuration * value / 100L).toInt()
            }
        }
        setProgressBarPosition(mStartPosition)

        setTimeFrames()
    }

    private fun onStopSeekThumbs() {
        mMessageHandler.removeMessages(SHOW_PROGRESS)
        mVideoView!!.pause()
        mPlayView!!.visibility = View.VISIBLE
    }

    private fun onVideoCompleted() {
        mVideoView!!.seekTo(mStartPosition)
    }

    private fun notifyProgressUpdate(all: Boolean) {
        if (mDuration == 0) return

        val position = mVideoView!!.currentPosition
        if (all) {
            for (item in mListeners!!) {
                item.updateProgress(position, mDuration, position * 100f / mDuration)
            }
        } else {
            mListeners!![1].updateProgress(position, mDuration, position * 100f / mDuration)
        }
    }

    private fun updateVideoProgress(time: Int) {
        if (mVideoView == null) {
            return
        }

        if (time >= mEndPosition) {
            mMessageHandler.removeMessages(SHOW_PROGRESS)
            mVideoView!!.pause()
            mPlayView!!.visibility = View.VISIBLE
            mResetSeekBar = true
            return
        }

        if (mHolderTopView != null) {
            setProgressBarPosition(time)
        }
        setTimeVideo(time)
    }

    private fun setProgressBarPosition(position: Int) {
        if (mDuration > 0) {
            val pos = 1000L * position / mDuration
            mHolderTopView!!.progress = pos.toInt()
        }
    }

    fun setVideoInformationVisibility(visible: Boolean) {
        mTimeInfoContainer!!.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun setOnTrimVideoListener(onTrimVideoListener: OnTrimVideoListener) {
        mOnTrimVideoListener = onTrimVideoListener
    }

    fun setOnHgLVideoListener(onHgLVideoListener: OnVideoListener) {
        mOnVideoListener = onHgLVideoListener
    }


    fun destroy() {
        BackgroundExecutor.cancelAll("", true)
        UiThreadExecutor.cancelAll("")
    }

    fun setMaxDuration(maxDuration: Int) {
        // mMaxDuration = maxDuration * 1000;
        mMaxDuration = maxDuration
    }

    fun setVideoURI(videoURI: Uri) {
        mSrc = videoURI

        mVideoView!!.setVideoURI(mSrc)
        mVideoView!!.requestFocus()

        mTimeLineView!!.setVideo(mSrc!!)
    }

    private class MessageHandler internal constructor(view: VideoTrimmer) : Handler() {

        @NonNull
        private val mView: WeakReference<VideoTrimmer>

        init {
            mView = WeakReference(view)
        }

        override fun handleMessage(msg: Message) {
            val view = mView.get()
            if (view == null || view.mVideoView == null) {
                return
            }

            view.notifyProgressUpdate(true)
            if (view.mVideoView!!.isPlaying) {
                sendEmptyMessageDelayed(0, 10)
            }
        }
    }

    companion object {

        private val TAG = VideoTrimmer::class.java.simpleName
        private val SHOW_PROGRESS = 2
    }
}

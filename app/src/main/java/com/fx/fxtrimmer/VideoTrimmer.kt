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
import androidx.core.view.isVisible
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
import kotlinx.android.synthetic.main.layout_trimmer.view.*
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
    private var mVideoView: VideoView? = null
    private var mPlayView: ImageView? = null
    private var mStartTime: TextView? = null
    private var mEndTime: TextView? = null
    private var mCurrentTime: TextView? = null
    private var mTimeLineView: TimeLineView? = null
    private var seekbar_bar: View? = null
    private var layout_convert_right: RelativeLayout? = null
    private var layout_default_trim: RelativeLayout? = null
    private var layout_default: LinearLayout? = null
    private var img_convert_left: ImageView? = null
    private var img_convert_right: ImageView? = null
    private var text_video_name: TextView? = null
    private var text_convert_left: TextView? = null
    private var text_convert_right: TextView? = null
    private var whole_convert = true
    private var r_default: RadioButton? = null
    private var r_video_only: RadioButton? = null
    private var r_audio_only: RadioButton? = null
    private var radioGroup: RadioGroup? = null
    private var layout_video_setting: FrameLayout? = null
    private var layout_audio_setting: FrameLayout? = null
    private var spinner_video_list: MutableList<Spinner>? = null
    private var spinner_audio_list: MutableList<Spinner>? = null
    private var mVideoProgressIndicator_top: ProgressBarView? = null
    private var mVideoProgressIndicator_bottom: ProgressBarView? = null

    private var mSrc: Uri? = null

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
        mVideoProgressIndicator_top = findViewById(R.id.timeVideoView_bottom) as ProgressBarView
        mVideoProgressIndicator_bottom = findViewById(R.id.timeVideoView_top) as ProgressBarView

        mRangeSeekBarView = findViewById(R.id.timeLineBar) as RangeSeekBarView
        mLinearVideo = findViewById(R.id.layout_surface_view) as RelativeLayout

        mVideoView = findViewById(R.id.video_loader) as VideoView
        mPlayView = findViewById(R.id.icon_video_play) as ImageView
        mStartTime = findViewById(R.id.text_start_time) as TextView
        mEndTime = findViewById(R.id.text_end_time) as TextView
        mCurrentTime = findViewById(R.id.text_current_time) as TextView
        mTimeLineView = findViewById(R.id.timeLineView) as TimeLineView

        layout_convert_right = findViewById(R.id.layout_convert_right) as RelativeLayout
        layout_default_trim = findViewById(R.id.layout_trimmer) as RelativeLayout
        img_convert_left = findViewById(R.id.img_convert_left) as ImageView
        img_convert_right = findViewById(R.id.img_convert_right) as ImageView
        text_convert_left = findViewById(R.id.text_convert_left) as TextView
        text_convert_right = findViewById(R.id.text_convert_right) as TextView
        text_video_name = findViewById(R.id.text_video_name) as TextView
        radioGroup = findViewById(R.id.radioGroup) as RadioGroup
        r_default = findViewById(R.id.radio_default) as RadioButton
        r_video_only = findViewById(R.id.radio_video_only) as RadioButton
        r_audio_only = findViewById(R.id.radio_audio_only) as RadioButton
        seekbar_bar = findViewById(R.id.seekbar_bar) as View
        layout_audio_setting = findViewById(R.id.layout_audio_setting) as FrameLayout
        layout_video_setting = findViewById(R.id.layout_video_setting) as FrameLayout

        setUpListeners()
        setUpMargins()
        setUpSpinners(context)

        setUpRadioGroup()
    }

    private fun setUpListeners() {
        mListeners = ArrayList<OnProgressVideoListener>()
        mListeners!!.add(object : OnProgressVideoListener {
            override fun updateProgress(time: Int, max: Int, scale: Float) {
                updateVideoProgress(time)
            }
        })
        mListeners!!.add(mVideoProgressIndicator_top!!)
        mListeners!!.add(mVideoProgressIndicator_bottom!!)


//        findViewById(R.id.btCancel)
//            .setOnClickListener(
//                OnClickListener { onCancelClicked() }
//            )

        layout_convert_right?.setOnClickListener {
            if (whole_convert) {
                img_convert_left?.setBackgroundResource(R.drawable.ic_convert_whole)
                text_convert_left?.setText("구간선택 변환")
                img_convert_right?.setBackgroundResource(R.drawable.ic_convert_section)
                text_convert_right?.setText("전체 변환")

            } else {
                img_convert_left?.setBackgroundResource(R.drawable.ic_convert_whole)
                text_convert_left?.setText("전체 변환")
                img_convert_right?.setBackgroundResource(R.drawable.ic_convert_section)
                text_convert_right?.setText("구간선택 변환")
//                seekBarVideo!!.setProgress(0)
//                    seekBarVideo!!.max = mTimeVideo * 1000
//
//                mRangeSeekBarView?.init()
//                mDuration = 0
//                setSeekBarPosition()
//
//                setTimeFrames()
//                setTimeVideo(0)
//                setPosVideo()
            }
            whole_convert = !whole_convert
            mRangeSeekBarView!!.isVisible = !(mRangeSeekBarView!!.isVisible)
            mVideoProgressIndicator_bottom!!.isVisible =
                !(mVideoProgressIndicator_bottom!!.isVisible)
            mVideoProgressIndicator_top!!.isVisible = !(mVideoProgressIndicator_top!!.isVisible)
        }
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
        mLinearVideo!!.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            true
        }

        mRangeSeekBarView!!.addOnRangeSeekBarListener(mVideoProgressIndicator_top!!)
        mRangeSeekBarView!!.addOnRangeSeekBarListener(mVideoProgressIndicator_bottom!!)

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

    fun getSeekbarPosition(): Int {
        val padding = mHolderTopView!!.getPaddingLeft() + mHolderTopView!!.getPaddingRight()
        val sPos = mHolderTopView!!.getLeft() + mHolderTopView!!.getPaddingLeft()
        val xPos =
            (mHolderTopView!!.getWidth() - padding) * mHolderTopView!!.getProgress() / mHolderTopView!!.getMax() + sPos - text_current_time!!.getWidth() / 2
        return xPos
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private fun setUpMargins() {
        val marge = mRangeSeekBarView!!.thumbs!!.get(0).widthBitmap - 50

        val widthSeek = mHolderTopView!!.thumb.minimumWidth / 2


        var lp = mHolderTopView!!.layoutParams as FrameLayout.LayoutParams
        lp.setMargins(marge - widthSeek - 20, 170, marge - widthSeek, 0)

        mHolderTopView!!.layoutParams = lp

        lp = mTimeLineView!!.getLayoutParams() as FrameLayout.LayoutParams
        lp.setMargins(marge, 0, marge, 0)
        mTimeLineView!!.setLayoutParams(lp)

        lp = mVideoProgressIndicator_top!!.getLayoutParams() as FrameLayout.LayoutParams
        lp.setMargins(marge, 0, marge, 0)
        mVideoProgressIndicator_top!!.setLayoutParams(lp)

        lp = mVideoProgressIndicator_bottom!!.getLayoutParams() as FrameLayout.LayoutParams
        lp.setMargins(marge, 153, marge, 0)
        mVideoProgressIndicator_bottom!!.setLayoutParams(lp)
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
            setPosVideo()
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
        setPosVideo()
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
        setPosVideo()

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
        mStartTime!!.text = stringForTime(mStartPosition)
        mEndTime!!.text = stringForTime(mEndPosition)

        mStartTime!!.text = stringForTime(mStartPosition)
        setPosVideo()

    }

    private fun setTimeVideo(position: Int) {
        mCurrentTime!!.text = stringForTime(position)

    }

    private fun setPosVideo() {
        val xPos = getSeekbarPosition()
        mCurrentTime!!.setX(xPos.toFloat())
        seekbar_bar!!.setX(xPos.toFloat() + 44)
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
        setPosVideo()
    }

    private fun setProgressBarPosition(position: Int) {
        if (mDuration > 0) {
            val pos = 1000L * position / mDuration
            mHolderTopView!!.progress = pos.toInt()
        }
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

    fun setUpSpinners(context: Context) {
        spinner_video_list = mutableListOf()
        spinner_audio_list = mutableListOf()
        for (i in 1..5) {
            var spinner_id =
                resources.getIdentifier("spinner_video_$i", "id", "com.fx.fxtrimmer")
            spinner_video_list!!.add(findViewById(spinner_id))
        }
        for (i in 1..5) {
            var spinner_array =
                resources.getIdentifier("video_arr_$i", "array", "com.fx.fxtrimmer")
            var m_adtType =
                ArrayAdapter.createFromResource(context, spinner_array, R.layout.row_spinner)
            m_adtType.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner_video_list!![i - 1].adapter = m_adtType
        }

        for (i in 1..6) {
            var spinner_id =
                resources.getIdentifier("spinner_audio_$i", "id", "com.fx.fxtrimmer")
            spinner_audio_list!!.add(findViewById(spinner_id))
        }
        for (i in 1..6) {
            var spinner_array =
                resources.getIdentifier("audio_arr_$i", "array", "com.fx.fxtrimmer")
            var m_adtType =
                ArrayAdapter.createFromResource(context, spinner_array, R.layout.row_spinner)
            m_adtType.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner_audio_list!![i - 1].adapter = m_adtType
        }
    }

    fun setUpRadioGroup() {
        var mRadioCheck: RadioGroup.OnCheckedChangeListener =
            RadioGroup.OnCheckedChangeListener { group, checkedId ->
                if (group.id == R.id.radioGroup) {
                    when (checkedId) {
                        R.id.radio_default -> {
                            r_default?.setButtonDrawable(R.drawable.radiobtn_selected)
                            r_video_only?.setButtonDrawable(R.drawable.radiobtn_unselected)
                            r_audio_only?.setButtonDrawable(R.drawable.radiobtn_unselected)
                            layout_video_setting!!.visibility = View.VISIBLE
                            layout_audio_setting!!.visibility = View.VISIBLE

                        }
                        R.id.radio_video_only -> {
                            r_default?.setButtonDrawable(R.drawable.radiobtn_unselected)
                            r_video_only?.setButtonDrawable(R.drawable.radiobtn_selected)
                            r_audio_only?.setButtonDrawable(R.drawable.radiobtn_unselected)
                            layout_video_setting!!.visibility = View.VISIBLE
                            layout_audio_setting!!.visibility = View.GONE
                        }
                        R.id.radio_audio_only -> {
                            r_default?.setButtonDrawable(R.drawable.radiobtn_unselected)
                            r_video_only?.setButtonDrawable(R.drawable.radiobtn_unselected)
                            r_audio_only?.setButtonDrawable(R.drawable.radiobtn_selected)
                            layout_video_setting!!.visibility = View.GONE
                            layout_audio_setting!!.visibility = View.VISIBLE
                        }
                    }
                }
            }
        radioGroup!!.setOnCheckedChangeListener(mRadioCheck)
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
        private val SHOW_PROGRESS = 2
    }
}

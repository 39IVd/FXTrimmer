package com.fx.fxtrimmer.activity

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.fx.fxtrimmer.R
import com.fx.fxtrimmer.VideoTrimmer
import com.fx.fxtrimmer.interfaces.OnTrimVideoListener
import com.fx.fxtrimmer.interfaces.OnVideoListener

class VideoTrimmerActivity : AppCompatActivity(), OnTrimVideoListener, OnVideoListener {
    private var mVideoTrimmer: VideoTrimmer? = null
    private var mProgressDialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_trimmer)

        val extraIntent = intent
        var path = ""
        var maxDuration = 10

        if (extraIntent != null) {
            path = extraIntent.getStringExtra(MainActivity.EXTRA_VIDEO_PATH)
            maxDuration = extraIntent.getIntExtra(MainActivity.VIDEO_TOTAL_DURATION, 10)
        }

        //setting progressbar
        mProgressDialog = ProgressDialog(this)
        mProgressDialog!!.setCancelable(false)
        mProgressDialog!!.setMessage(getString(R.string.trimming_progress))

        mVideoTrimmer = findViewById(R.id.timeLine) as VideoTrimmer
        if (mVideoTrimmer != null) {


            /**
             * get total duration of video file
             */
            Log.e("tg", "maxDuration = $maxDuration")
            //mVideoTrimmer.setMaxDuration(maxDuration);
            mVideoTrimmer!!.setMaxDuration(maxDuration)
            mVideoTrimmer!!.setOnTrimVideoListener(this)
            mVideoTrimmer!!.setOnHgLVideoListener(this)
            //mVideoTrimmer.setDestinationPath("/storage/emulated/0/DCIM/CameraCustom/");
            mVideoTrimmer!!.setVideoURI(Uri.parse(path))
            mVideoTrimmer!!.setVideoInformationVisibility(true)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.e("tg", "resultCode = $resultCode data $data")
    }

    override fun cancelAction() {
        mProgressDialog!!.cancel()
        mVideoTrimmer!!.destroy()
        finish()
    }

    override fun onError(message: String) {
        mProgressDialog!!.cancel()

    }

    override fun onVideoPrepared() {}
}

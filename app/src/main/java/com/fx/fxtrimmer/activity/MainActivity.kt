package com.fx.fxtrimmer.activity

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.fx.fxtrimmer.R
import com.fx.fxtrimmer.utils.FileUtils

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private var buttonTrimVideo: Button? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        buttonTrimVideo = findViewById(R.id.btnTrimVideo) as Button
        buttonTrimVideo!!.setOnClickListener(this)

    }

    private fun pickFromGallery() {
        Log.v("pickfromgal", "dd")
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) !== PackageManager.PERMISSION_GRANTED
        ) {
            requestPermission(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                getString(R.string.permission_read_storage_rationale),
                REQUEST_STORAGE_READ_ACCESS_PERMISSION
            )
        } else {
            val intent = Intent()
            intent.setTypeAndNormalize("video/*")
            intent.action = Intent.ACTION_GET_CONTENT
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            startActivityForResult(
                Intent.createChooser(
                    intent,
                    getString(R.string.label_select_video)
                ), REQUEST_VIDEO_TRIMMER
            )
        }
    }

    override fun onClick(v: View) {
        if(v==buttonTrimVideo) {
            pickFromGallery()
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_VIDEO_TRIMMER) {
                val selectedUri = data?.data
                if (selectedUri != null) {
                    startTrimActivity(selectedUri)
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        R.string.toast_cannot_retrieve_selected_video,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun startTrimActivity(@NonNull uri: Uri) {
        val intent = Intent(this, VideoTrimmerActivity::class.java)
        intent.putExtra(EXTRA_VIDEO_PATH, FileUtils.getPath(this, uri))
        intent.putExtra(VIDEO_TOTAL_DURATION, getMediaDuration(uri))
        startActivity(intent)
    }

    private fun getMediaDuration(uriOfFile: Uri): Int {
        val mp = MediaPlayer.create(this, uriOfFile)
        return mp.duration
    }

    private fun requestPermission(permission: String, rationale: String, requestCode: Int) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.permission_title_rationale))
            builder.setMessage(rationale)
            builder.setPositiveButton(getString(R.string.Ok),
                DialogInterface.OnClickListener { dialog, which ->
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(permission),
                        requestCode
                    )
                })
            builder.setNegativeButton(getString(R.string.cancel), null)
            builder.show()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, @NonNull permissions: Array<String>, @NonNull grantResults: IntArray) {
        when (requestCode) {
            REQUEST_STORAGE_READ_ACCESS_PERMISSION -> if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickFromGallery()
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    companion object {

        private val REQUEST_VIDEO_TRIMMER = 0x01
        private val REQUEST_STORAGE_READ_ACCESS_PERMISSION = 101
        internal val EXTRA_VIDEO_PATH = "EXTRA_VIDEO_PATH"
        internal val VIDEO_TOTAL_DURATION = "VIDEO_TOTAL_DURATION"
    }
}

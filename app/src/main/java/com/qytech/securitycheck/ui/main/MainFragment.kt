package com.qytech.securitycheck.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.hardware.Camera
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.view.CameraView
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.qytech.securitycheck.FileUtils
import com.qytech.securitycheck.R
import kotlinx.android.synthetic.main.main_fragment.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

@SuppressLint("RestrictedApi")
class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private val BRIGHTNESS_LIST = arrayOf(
            "/sys/class/leds/365nm-led/brightness",
            "/sys/class/leds/410nm-led/brightness",
            "/sys/class/leds/940nm-led/brightness",
            "/sys/class/leds/White-led/brightness",
        )
        private const val BRIGHTNESS_HRLAMP = "/sys/class/leds/HRlamp-led/brightness"
        private const val BRIGHTNESS_ON = "1"
        private const val BRIGHTNESS_OFF = "0"
    }

    private lateinit var viewModel: MainViewModel

    private lateinit var outputDirectory: File


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Timber.d("onCreateView message:  ")
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    @SuppressLint("MissingPermission")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        btn_take_pictures.setOnClickListener {
            //开灯拍照,然后关灯
            GlobalScope.launch(Dispatchers.Main) {
                BRIGHTNESS_LIST.forEach {
                    FileUtils.write2File(File(it), BRIGHTNESS_ON)
                    Timber.d("$it BRIGHTNESS_ON  ")
                    takePhoto()
                    delay(500L)
                    FileUtils.write2File(File(it), BRIGHTNESS_OFF)
                    Timber.d("$it BRIGHTNESS_OFF ")
                    delay(500L)
                }
                delay(1000L)
                FileUtils.write2File(File(BRIGHTNESS_HRLAMP), BRIGHTNESS_ON)
                recordVideo()
                delay(15_000L)
                camera_view.stopRecording()
                FileUtils.write2File(File(BRIGHTNESS_HRLAMP), BRIGHTNESS_OFF)
        }
        }
        camera_view.cameraLensFacing = CameraSelector.LENS_FACING_FRONT
        camera_view.bindToLifecycle(viewLifecycleOwner)
        outputDirectory = getOutputDirectory()
        Timber.d("onActivityCreated message:  ")
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun recordVideo() {
        if (camera_view.isRecording) {
            return
        }
        try {

            camera_view?.let {
                it.captureMode = CameraView.CaptureMode.VIDEO
                val dateFormat = SimpleDateFormat(FILENAME_FORMAT, Locale.getDefault())
                    .format(System.currentTimeMillis())
                val videoFile = File(outputDirectory, "${dateFormat}.mp4")
                val outputOptions = VideoCapture.OutputFileOptions.Builder(videoFile).build()
                it.startRecording(
                    outputOptions,
//                    Executors.newSingleThreadExecutor(),
                    ContextCompat.getMainExecutor(requireContext()),
                    object : VideoCapture.OnVideoSavedCallback {
                        override fun onVideoSaved(outputFileResults: VideoCapture.OutputFileResults) {
                            val savedUri = outputFileResults.savedUri
                            val msg = "Video capture succeeded: $savedUri"
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                            Timber.d("onImageSaved message: $msg ")
                            val mimeType = MimeTypeMap.getSingleton()
                                .getMimeTypeFromExtension(savedUri?.toFile()?.extension)
                            MediaScannerConnection.scanFile(
                                context,
                                arrayOf(savedUri?.toFile()?.absolutePath),
                                arrayOf(mimeType)
                            ) { _, uri ->
                                Timber.d("Video capture scanned into media store: $uri")
                            }
                        }

                        override fun onError(
                            videoCaptureError: Int,
                            message: String,
                            cause: Throwable?
                        ) {
                            Timber.e("onError message:  $message cause $cause")
                        }

                    }
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun takePhoto() {
        try {
            camera_view?.let {
                it.captureMode = CameraView.CaptureMode.IMAGE
                val dateFormat = SimpleDateFormat(FILENAME_FORMAT, Locale.getDefault())
                    .format(System.currentTimeMillis())
                val photoFile = File(outputDirectory, "${dateFormat}.jpg")
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                it.takePicture(
                    outputOptions,
//                    Executors.newSingleThreadExecutor(),
                    ContextCompat.getMainExecutor(requireContext()),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            val savedUri = Uri.fromFile(photoFile)
                            val msg = "Photo capture succeeded: $savedUri"
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                            Timber.d("onImageSaved message: $msg ")

//                            // We can only change the foreground Drawable using API level 23+ API
//                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                                // Update the gallery thumbnail with latest picture taken
//                                setGalleryThumbnail(savedUri)
//                            }

                            // Implicit broadcasts will be ignored for devices running API level >= 24
                            // so if you only target API level 24+ you can remove this statement
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                                requireActivity().sendBroadcast(
                                    Intent(Camera.ACTION_NEW_PICTURE, savedUri)
                                )
                            }

                            // If the folder selected is an external media directory, this is
                            // unnecessary but otherwise other apps will not be able to access our
                            // images unless we scan them using [MediaScannerConnection]
                            val mimeType = MimeTypeMap.getSingleton()
                                .getMimeTypeFromExtension(savedUri.toFile().extension)
                            MediaScannerConnection.scanFile(
                                context,
                                arrayOf(savedUri.toFile().absolutePath),
                                arrayOf(mimeType)
                            ) { _, uri ->
                                Timber.d("Image capture scanned into media store: $uri")
                            }
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Timber.e("onError message:  $exception")
                        }

                    })
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }


    }

    private fun getOutputDirectory(): File {
        val mediaDir = requireActivity().externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else requireActivity().filesDir
    }


}
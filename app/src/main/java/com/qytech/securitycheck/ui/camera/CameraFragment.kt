package com.qytech.securitycheck.ui.camera

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.CompoundButton
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.CameraView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.qytech.securitycheck.FileUtils
import com.qytech.securitycheck.LuminosityAnalyzer
import com.qytech.securitycheck.R
import com.qytech.securitycheck.showToast
import kotlinx.android.synthetic.main.camera_fragment.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


class CameraFragment : Fragment() {
    companion object {
        fun newInstance() = CameraFragment()

        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
        private const val FILENAME = "yyyy-MM-dd-HH-mm-ss"
        private const val PHOTO_EXTENSION = ".jpg"
        private const val VIDEO_EXTENSION = ".mp4"

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


    private lateinit var viewModel: CameraViewModel

    private lateinit var preview: Preview
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture? = null
    private var imageAnalysis: ImageAnalysis? = null
    private lateinit var cameraExecutor: ExecutorService
    private var captureMode: CameraView.CaptureMode? = null
    private lateinit var outputDirectory: File
    private var isRecording = false

    private var takePictureIndex = 0

    //    private lateinit var cameraSelector: CameraSelector
    private var cameraProvider: ProcessCameraProvider? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.camera_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(CameraViewModel::class.java)
        cameraExecutor = Executors.newSingleThreadExecutor()
        viewFinder.post {
            startCamera()
        }
        btn_take_pictures.setOnClickListener {
            lifecycleScope.launch(Dispatchers.Main) {
                outputDirectory = File(getOutputDirectory(), formatDate()).apply { mkdirs() }
                BRIGHTNESS_LIST.forEach {
                    takePicture(it)
                    delay(500L)
                }
                delay(500L)
                recordVideo(BRIGHTNESS_HRLAMP)
            }
        }
        btn_picture_365nm.setOnClickListener(onClickListener)
        btn_picture_410nm.setOnClickListener(onClickListener)
        btn_picture_960nm.setOnClickListener(onClickListener)
        btn_picture_White.setOnClickListener(onClickListener)
        btn_record_video.setOnClickListener(onClickListener)
        switch_brightness_365nm.setOnCheckedChangeListener(onCheckedChangeListener)
        switch_brightness_410nm.setOnCheckedChangeListener(onCheckedChangeListener)
        switch_brightness_960nm.setOnCheckedChangeListener(onCheckedChangeListener)
        switch_brightness_HRlamp.setOnCheckedChangeListener(onCheckedChangeListener)
        switch_brightness_whitenm.setOnCheckedChangeListener(onCheckedChangeListener)
        btn_picture_viewer.setOnClickListener {
            openAlbum()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        toggleBrightness(false)
    }

    private fun toggleBrightness(toggle: Boolean) {
        BRIGHTNESS_LIST.forEach {
            FileUtils.write2File(File(it), if (toggle) BRIGHTNESS_ON else BRIGHTNESS_OFF)
        }
        FileUtils.write2File(
            File(BRIGHTNESS_HRLAMP),
            if (toggle) BRIGHTNESS_ON else BRIGHTNESS_OFF
        )
    }

    private fun toggleBrightness(path: String, toggle: Boolean) {
        FileUtils.write2File(File(path), if (toggle) BRIGHTNESS_ON else BRIGHTNESS_OFF)
    }

    private fun startCamera() {
        ProcessCameraProvider.getInstance(requireContext()).apply {
            addListener({

                cameraProvider = this.get()

                bindCameraUserCases()

            }, ContextCompat.getMainExecutor(requireContext()))
        }
    }

    @SuppressLint("RestrictedApi")
    private fun bindCameraUserCases(mode: CameraView.CaptureMode = CameraView.CaptureMode.IMAGE) {
        if (captureMode == mode) {
            return
        }
        captureMode = mode
        // Get screen metrics used to setup camera for full screen resolution
////        val metrics = DisplayMetrics().also { viewFinder.display.getRealMetrics(it) }
////        Timber.d("Screen metrics: ${metrics.widthPixels} x ${metrics.heightPixels}")
////
////        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
////        Timber.d("Preview aspect ratio: $screenAspectRatio")
//
//        val rotation = viewFinder.display.rotation
//        Timber.d("bindCameraUserCases message: rotation $rotation ")

        val cameraSelector = if (hasFrontCamera()) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        preview = Preview.Builder()
//            .setTargetAspectRatio(screenAspectRatio)
//            .setTargetRotation(rotation)
            .build()
            .also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
//            .setTargetAspectRatio(screenAspectRatio)
//            .setTargetRotation(rotation)
            .build()

        imageAnalysis = ImageAnalysis.Builder()
//            .setTargetAspectRatio(screenAspectRatio)
//            .setTargetRotation(rotation)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->
                    Timber.d("bindCameraUserCases message:  Average luminosity $luma")
                })
            }

        videoCapture = VideoCapture.Builder()
//            .setTargetAspectRatio(screenAspectRatio)
//            .setTargetRotation(rotation)
            .build()

        try {
            cameraProvider?.unbindAll()
            if (mode == CameraView.CaptureMode.IMAGE) {
                cameraProvider?.bindToLifecycle(
                    viewLifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture,
                    imageAnalysis,
                )
            } else if (mode == CameraView.CaptureMode.VIDEO) {
                cameraProvider?.bindToLifecycle(
                    viewLifecycleOwner,
                    cameraSelector,
                    preview,
                    videoCapture
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private suspend fun takePicture(brightness: String) {
        if (isRecording) {
            showToast(R.string.recording)
            return
        }
        bindCameraUserCases(CameraView.CaptureMode.IMAGE)
        imageCapture?.let {
            val photoFile = createFile(outputDirectory, PHOTO_EXTENSION)

            // Create output options object which contains file + metadata
            val outputOptions = ImageCapture.OutputFileOptions
                .Builder(photoFile)
                .build()
            FileUtils.write2File(File(brightness), BRIGHTNESS_ON)
            delay(1500L)
            it.takePicture(
                outputOptions,
                cameraExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        Timber.d("onImageSaved message:  ${outputFileResults.savedUri}")
                        notifyScanFile(outputFileResults.savedUri)
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                            requireActivity().sendBroadcast(
                                Intent(
                                    android.hardware.Camera.ACTION_NEW_PICTURE,
                                    outputFileResults.savedUri
                                )
                            )
                        }
                        lifecycleScope.launch(Dispatchers.Main) {
                            requireContext().showToast(R.string.image_saved)
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Timber.e("OnImageSavedCallback error $exception ")
                        lifecycleScope.launch(Dispatchers.Main) {
                            requireContext().showToast(R.string.image_saved_error)
                        }

                    }
                })
            delay(500L)
            FileUtils.write2File(File(brightness), BRIGHTNESS_OFF)

        }
    }

    @SuppressLint("RestrictedApi")
    private suspend fun recordVideo(brightness: String) {
        if (isRecording) {
            showToast(R.string.recording)
            return
        }
        bindCameraUserCases(CameraView.CaptureMode.VIDEO)
        try {
            videoCapture?.let {
                val videoFile = createFile(outputDirectory, VIDEO_EXTENSION)

                // Create output options object which contains file + metadata
                val outputOptions = VideoCapture.OutputFileOptions
                    .Builder(videoFile)
                    .build()
                FileUtils.write2File(File(brightness), BRIGHTNESS_ON)
                delay(1500L)
                it.startRecording(
                    outputOptions,
                    cameraExecutor,
                    object : VideoCapture.OnVideoSavedCallback {
                        override fun onVideoSaved(outputFileResults: VideoCapture.OutputFileResults) {
                            Timber.d("onVideoSaved message:  ${outputFileResults.savedUri}")
                            notifyScanFile(outputFileResults.savedUri)
                            lifecycleScope.launch(Dispatchers.Main) {
                                requireContext().showToast(R.string.video_saved)
                            }
                        }

                        override fun onError(
                            videoCaptureError: Int,
                            message: String,
                            cause: Throwable?
                        ) {
                            Timber.e("OnVideoSavedCallback error message: $message cause: $cause")
                            lifecycleScope.launch(Dispatchers.Main) {
                                requireContext().showToast(R.string.video_saved_error)
                            }
                        }

                    })
                isRecording = true
                delay(15_000L)
                it.stopRecording()
                isRecording = false
                FileUtils.write2File(File(brightness), BRIGHTNESS_OFF)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     *  [androidx.camera.core.ImageAnalysisConfig] requires enum value of
     *  [androidx.camera.core.AspectRatio]. Currently it has values of 4:3 & 16:9.
     *
     *  Detecting the most suitable ratio for dimensions provided in @params by counting absolute
     *  of preview ratio to one of the provided values.
     *
     *  @param width - preview width
     *  @param height - preview height
     *  @return suitable aspect ratio
     */
    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    private fun hasBackCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) == true
    }

    private fun hasFrontCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) == true
    }

    private fun getOutputDirectory(): File {
        val mediaDir = requireActivity().externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else requireActivity().filesDir
    }


    /** Helper function used to create a timestamped file */
    private fun createFile(baseFolder: File, extension: String) =
        File(baseFolder, "${formatDate()}${extension}")

    private fun formatDate() = SimpleDateFormat(FILENAME, Locale.getDefault())
        .format(System.currentTimeMillis())

    private fun notifyScanFile(uri: Uri?) {
        val mimeType = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(uri?.toFile()?.extension)
        MediaScannerConnection.scanFile(
            context,
            arrayOf(uri?.toFile()?.absolutePath),
            arrayOf(mimeType)
        ) { _, uri ->
            Timber.d("notifyScanFile scanned into media store: $uri")
        }
    }

    private fun openAssignFolder(file: File) {
        if (!file.exists()) {
            return
        }
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            val contentUri: Uri = FileProvider.getUriForFile(
                requireContext(),
                "com.qytech.securitycheck.provider",
                file
            )
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.setDataAndType(contentUri, "file/*")
        } else {
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.setDataAndType(Uri.fromFile(file), "file/*")
        }
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
        }
    }

    private fun openAlbum() {
        val intent = Intent(
            Intent.ACTION_VIEW, Uri.parse(
                "content://media/internal/images/media"
            )
        )
        startActivity(intent)
    }

    private val onCheckedChangeListener =
        CompoundButton.OnCheckedChangeListener { compoundButton, checked ->
            when (compoundButton.id) {
                R.id.switch_brightness_365nm -> {
                    toggleBrightness(BRIGHTNESS_LIST[0], checked)
                }
                R.id.switch_brightness_410nm -> {
                    toggleBrightness(BRIGHTNESS_LIST[1], checked)
                }
                R.id.switch_brightness_960nm -> {
                    toggleBrightness(BRIGHTNESS_LIST[2], checked)
                }
                R.id.switch_brightness_whitenm -> {
                    toggleBrightness(BRIGHTNESS_LIST[3], checked)
                }
                R.id.switch_brightness_HRlamp -> {
                    toggleBrightness(BRIGHTNESS_HRLAMP, checked)
                }
            }
        }

    private val onClickListener = View.OnClickListener {
        lifecycleScope.launch(Dispatchers.Main) {
            outputDirectory = File(getOutputDirectory(), formatDate()).apply { mkdirs() }
            when (it.id) {
                R.id.btn_picture_365nm -> {
                    takePicture(BRIGHTNESS_LIST[0])
                }
                R.id.btn_picture_410nm -> {
                    takePicture(BRIGHTNESS_LIST[1])
                }
                R.id.btn_picture_960nm -> {
                    takePicture(BRIGHTNESS_LIST[2])
                }
                R.id.btn_picture_White -> {
                    takePicture(BRIGHTNESS_LIST[3])
                }
                R.id.btn_record_video -> {
                    recordVideo(BRIGHTNESS_HRLAMP)
                }
            }
        }
    }


}
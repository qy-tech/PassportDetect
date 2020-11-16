package com.qytech.securitycheck.ui.camera

import android.annotation.SuppressLint
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.CameraView
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.qytech.securitycheck.PreviewActivity
import com.qytech.securitycheck.R
import com.qytech.securitycheck.databinding.CameraFragmentBinding
import com.qytech.securitycheck.extensions.showToast
import com.qytech.securitycheck.utils.FileUtils
import com.qytech.securitycheck.utils.LuminosityAnalyzer
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
        private const val VIDEO_DURATION = 5_000L

    }


    private lateinit var viewModel: CameraViewModel
    private lateinit var dataBinding: CameraFragmentBinding

    private lateinit var preview: Preview
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture? = null
    private var imageAnalysis: ImageAnalysis? = null
    private lateinit var cameraExecutor: ExecutorService
    private var captureMode: CameraView.CaptureMode? = null
    private var isRecording = false

    private var cameraProvider: ProcessCameraProvider? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dataBinding = CameraFragmentBinding.inflate(inflater, container, false)
        return dataBinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(CameraViewModel::class.java)
        dataBinding.lifecycleOwner = viewLifecycleOwner
        dataBinding.viewModel = viewModel
        cameraExecutor = Executors.newSingleThreadExecutor()
        viewFinder.post {
            startCamera()
        }
        btn_take_pictures.setOnClickListener {
            if (viewModel.currentBrightness.value == "HRlamp") {
                recordVideo()
            } else {
                takePicture()
            }
        }
        btn_picture_viewer.setOnClickListener {
            PreviewActivity.start(requireContext(), getOutputDirectory().absolutePath)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
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

    private fun takePicture() {
        if (isRecording) {
            showToast(R.string.recording)
            return
        }
        bindCameraUserCases(CameraView.CaptureMode.IMAGE)
        imageCapture?.let {
            val photoFile =
                createFile(getOutputDirectory(), viewModel.currentBrightness.value, PHOTO_EXTENSION)

            // Create output options object which contains file + metadata
            val outputOptions = ImageCapture.OutputFileOptions
                .Builder(photoFile)
                .build()
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
        }
    }

    @SuppressLint("RestrictedApi")
    private fun recordVideo() {
        if (isRecording) {
            showToast(R.string.recording)
            return
        }
        lifecycleScope.launch {
            bindCameraUserCases(CameraView.CaptureMode.VIDEO)
            try {
                videoCapture?.let {
                    val videoFile = createFile(
                        getOutputDirectory(),
                        viewModel.currentBrightness.value,
                        VIDEO_EXTENSION
                    )

                    // Create output options object which contains file + metadata
                    val outputOptions = VideoCapture.OutputFileOptions
                        .Builder(videoFile)
                        .build()
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
                    delay(VIDEO_DURATION)
                    it.stopRecording()
                    isRecording = false
                    viewModel.toggleBrightness(
                        CameraViewModel.BRIGHTNESS_MAP.getValue("HRlamp"),
                        false
                    )
                    dataBinding.switchBrightnessHRlamp.isChecked = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
        val storagePath = FileUtils.getStoragePath(requireContext())
        val outputPath =
            if (storagePath?.isNotBlank() == true) storagePath else Environment.getExternalStorageDirectory().absolutePath
        return File(outputPath, getString(R.string.app_name)).also {
            if (!it.exists()) {
                it.mkdirs()
            }
        }
    }


    /** Helper function used to create a timestamped file */
    private fun createFile(baseFolder: File, brightness: String?, extension: String) =
        File(baseFolder, "${formatDate()}_${brightness}${extension}")

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
}
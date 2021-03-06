package com.qytech.securitycheck.ui.camera

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.CameraView
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.qytech.securitycheck.GlobalApplication
import com.qytech.securitycheck.PreviewActivity
import com.qytech.securitycheck.R
import com.qytech.securitycheck.databinding.CameraFragmentBinding
import com.qytech.securitycheck.extensions.showToast
import com.qytech.securitycheck.ui.fingerprint.FingerprintActivity
import com.qytech.securitycheck.ui.fingerprint.LoginActivity
import com.qytech.securitycheck.utils.FileUtils
import com.qytech.securitycheck.utils.LuminosityAnalyzer
import com.qytech.securitycheck.utils.PreferenceUtils
import com.qytech.securitycheck.utils.PreferenceUtils.clear
import com.qytech.securitycheck.utils.PreferenceUtils.findPreference
import com.qytech.securitycheck.utils.PreferenceUtils.putPreference
import com.qytech.securitycheck.utils.SpUtil
import com.szadst.szoemhost_lib.DevComm
import com.szadst.szoemhost_lib.DevComm.NEED_FIRST_SWEEP
import com.szadst.szoemhost_lib.DevComm.NEED_RELEASE_FINGER
import com.szadst.szoemhost_lib.HostLib
import com.szadst.szoemhost_lib.IFPListener.FPCommandListener
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


@RequiresApi(Build.VERSION_CODES.M)
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
    var m_szDevice: String? = "/dev/ttyS4"
    var m_nBaudrate = 115200
    var popupWindow: PopupWindow? = null
    private var m_txtStatus: TextView? = null
    val enrollCount = findPreference("TEMPLATE_NO", 1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HostLib.getInstance(activity).FPCmdProc().OpenDevice(m_szDevice, m_nBaudrate)
    }

    override fun onResume() {
        super.onResume()
        initLogin()
        val data = SpUtil.getData(GlobalApplication.instance, "loginusername")
        val isLogins = findPreference("isLogins", 13)
        if (!data.isEmpty() && isLogins == 13) {
            btn_fingerprint.visibility = VISIBLE
        } else if (data.isEmpty()) {
            btn_fingerprint.visibility = GONE
            agin_btn.visibility = INVISIBLE
        }
        if (btn_fingerprint != null) {
            btn_fingerprint.setOnClickListener {
                viewModel.currentSwitch.value?.isChecked = false
                val intent = Intent(activity, FingerprintActivity::class.java)
                startActivity(intent)
            }
        }
        HostLib.getInstance(activity).FPCmdProc().SetListener(object : FPCommandListener {
            override fun cmdProcReturn(
                p_nCmdCode: Int,
                p_nRetCode: Int,
                p_nParam1: Int,
                p_nParam2: Int
            ) {
                procResponsePacketfragment(p_nCmdCode, p_nRetCode, p_nParam1, p_nParam2)
            }

            override fun cmdProcReturnData(op_pData: ByteArray, p_nSize: Int) {}

            override fun cmdProcShowText(p_szInfo: String) {}
        }) {}
    }

    private fun initLogin() {
        btn_login.setOnClickListener {
            val intent = Intent(activity, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    private fun procResponsePacketfragment(
        p_nCode: Int,
        p_nRet: Int,
        p_nParam1: Int,
        p_nParam2: Int
    ) {
        var m_strPost = ""
        m_txtStatus?.text = m_strPost
        when (p_nCode) {
            DevComm.CMD_IDENTIFY_CODE ->
                if (p_nRet == DevComm.ERR_SUCCESS) {
                    when (p_nParam1) {
                        NEED_RELEASE_FINGER.toShort().toInt() -> m_strPost = "收起手指"
                        NEED_FIRST_SWEEP.toShort().toInt() -> m_strPost = "放置手指"
                        else -> {
                            agin_btn.visibility = GONE
                            putPreference("isident", 0)
                            putPreference("popupWindowisShowing", false)
                            val toast: Toast = Toast.makeText(
                                activity,
                                "验证成功",
                                Toast.LENGTH_SHORT
                            )
                            val layout =
                                toast.view as LinearLayout?
                            val tv =
                                layout!!.getChildAt(0) as TextView
                            tv.textSize = 30f
                            tv.setTextColor(Color.WHITE)
                            toast.view!!.setBackgroundColor(Color.DKGRAY)
                            toast.show()
                            if (popupWindow != null && popupWindow!!.isShowing) {
                                popupWindow!!.dismiss()
                            }
                        }
                    }
                } else {
                    agin_btn.visibility = VISIBLE
                    putPreference("isident", 50)
                    putPreference("popupWindowisShowing", false)
                    val toast: Toast = Toast.makeText(activity, "验证失败", Toast.LENGTH_SHORT)
                    val layout =
                        toast.view as LinearLayout?
                    val tv = layout!!.getChildAt(0) as TextView
                    tv.textSize = 30f
                    tv.setTextColor(Color.WHITE)
                    toast.view!!.setBackgroundColor(Color.DKGRAY)
                    toast.show()
                    if (popupWindow != null && popupWindow!!.isShowing) {
                        popupWindow!!.dismiss()
                    }
                }
        }
        m_txtStatus?.text = m_strPost
    }

    override fun onPause() {
        super.onPause()
        putPreference("isident", 0)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dataBinding = CameraFragmentBinding.inflate(inflater, container, false)
        return dataBinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(CameraViewModel::class.java)
        viewModel.currentSwitch.value?.isChecked = false
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
            val data = SpUtil.getData(GlobalApplication.instance, "loginusername")  //当前用户是否已经登陆
            val findPreference = PreferenceUtils.findPreference("isident", 50)  //验证指纹是否正确
            val enrollCount = PreferenceUtils.findPreference("TEMPLATE_NO", 1) //判断当前有没有录制指纹
            val popupWindowisShowing = PreferenceUtils.findPreference("popupWindowisShowing", true)
            if (popupWindowisShowing) {
                return@setOnClickListener
            }
            if (findPreference == 50) {
                val toast = Toast.makeText(GlobalApplication.instance, "重新验证指纹", Toast.LENGTH_SHORT)
                val layout =
                    toast.view as LinearLayout?
                val tv = layout!!.getChildAt(0) as TextView
                tv.textSize = 30f
                tv.setTextColor(Color.WHITE)
                toast.view!!.setBackgroundColor(Color.DKGRAY)
                toast.show()
                return@setOnClickListener
            } else
                if (data.isEmpty()) {
                    val toast =
                        Toast.makeText(GlobalApplication.instance, "请先登录用户", Toast.LENGTH_SHORT)
                    val layout =
                        toast.view as LinearLayout?
                    val tv = layout!!.getChildAt(0) as TextView
                    tv.textSize = 30f
                    tv.setTextColor(Color.WHITE)
                    toast.view!!.setBackgroundColor(Color.DKGRAY)
                    toast.show()
                    return@setOnClickListener
                } else if (enrollCount < 2) {
                    val toast = Toast.makeText(
                        GlobalApplication.instance,
                        "请录制指纹",
                        Toast.LENGTH_SHORT
                    )
                    val layout =
                        toast.view as LinearLayout?
                    val tv = layout!!.getChildAt(0) as TextView
                    tv.textSize = 30f
                    tv.setTextColor(Color.WHITE)
                    toast.view!!.setBackgroundColor(Color.DKGRAY)
                    toast.show()
                    return@setOnClickListener
                } else {
                    PreviewActivity.start(requireContext(), getOutputDirectory().absolutePath)
                }
        }

        isEnrollTrue()
    }

    private fun isEnrollTrue() {
        val isLogins = findPreference("isLogins", 13)
        val data = SpUtil.getData(GlobalApplication.instance, "loginusername")
        Timber.d("data is $data")
        if (data.isEmpty()) {
            val toast = Toast.makeText(
                GlobalApplication.instance,
                "请先登录用户",
                Toast.LENGTH_SHORT
            )
            val layout =
                toast.view as LinearLayout?
            val tv = layout!!.getChildAt(0) as TextView
            tv.textSize = 30f
            tv.setTextColor(Color.WHITE)
            toast.view!!.setBackgroundColor(Color.DKGRAY)
            toast.show()
        } else if (enrollCount < 2) {
            val toast = Toast.makeText(
                GlobalApplication.instance,
                "请录制指纹",
                Toast.LENGTH_SHORT
            )
            val layout =
                toast.view as LinearLayout?
            val tv = layout!!.getChildAt(0) as TextView
            tv.textSize = 30f
            tv.setTextColor(Color.WHITE)
            toast.view!!.setBackgroundColor(Color.DKGRAY)
            toast.show()
        } else
            dataBinding.root.post {
                if (data.isNotEmpty() && enrollCount >= 2 && isLogins == 13) {
                    if (HostLib.getInstance(GlobalApplication.instance).FPCmdProc()
                            .Run_CmdIdentify(false) <= 0) {
                        putPreference("popupWindowisShowing", true)
                        val popupView = LayoutInflater.from(requireContext())
                            .inflate(R.layout.pop_fingerprint, null)
                        m_txtStatus = popupView.findViewById<TextView>(R.id.txtStatus)
                        popupWindow = PopupWindow(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        popupWindow!!.contentView = popupView
                        popupWindow!!.isClippingEnabled = true
                        popupWindow!!.isOutsideTouchable = false
                        popupWindow!!.isFocusable = false
                        Timber.d("root view is ${requireView().rootView}")
                        popupWindow!!.showAtLocation(
                            requireView().rootView,
                            Gravity.CENTER or Gravity.CENTER_HORIZONTAL,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        val toast = Toast.makeText(
                            GlobalApplication.instance,
                            "请验证指纹",
                            Toast.LENGTH_SHORT
                        )
                        val layout =
                            toast.view as LinearLayout?
                        val tv = layout!!.getChildAt(0) as TextView
                        tv.textSize = 30f
                        tv.setTextColor(Color.WHITE)
                        toast.view!!.setBackgroundColor(Color.DKGRAY)
                        toast.show()
                    }
                }
            }
        agin_btn.setOnClickListener {
            dataBinding.root.post {
                if (data.isNotEmpty() && enrollCount >= 2) {
                    if (HostLib.getInstance(GlobalApplication.instance).FPCmdProc()
                            .Run_CmdIdentify(false) <= 0
                    ) {
                        val popupView = LayoutInflater.from(requireContext())
                            .inflate(R.layout.pop_fingerprint, null)
                        m_txtStatus = popupView.findViewById<TextView>(R.id.txtStatus)
                        Timber.d("popupView  is $popupView")
                        popupWindow = PopupWindow(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        popupWindow!!.contentView = popupView
                        popupWindow!!.isClippingEnabled = true
                        popupWindow!!.isOutsideTouchable = false
                        popupWindow!!.isFocusable = false
                        popupWindow!!.showAtLocation(
                            requireView().rootView,
                            Gravity.CENTER or Gravity.CENTER_HORIZONTAL,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        val toast = Toast.makeText(
                            GlobalApplication.instance,
                            "请验证指纹",
                            Toast.LENGTH_SHORT
                        )
                        val layout =
                            toast.view as LinearLayout?
                        val tv = layout!!.getChildAt(0) as TextView
                        tv.textSize = 30f
                        tv.setTextColor(Color.WHITE)
                        toast.view!!.setBackgroundColor(Color.DKGRAY)
                        toast.show()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        HostLib.getInstance(activity).FPCmdProc().CloseDevice()
        cameraExecutor.shutdown()
    }


    private fun startCamera() {
        ProcessCameraProvider.getInstance(requireContext()).apply {
            addListener(Runnable {
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
//        Timber.d("Screen metrics: ${metrics.widthPixels} x ${metrics.heightPixels}")
////
////        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
////        Timber.d("Preview aspect ratio: $screenAspectRatio")
//
//        val rotation = viewFinder.display.rotation
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
//                    Timber.d("bindCameraUserCases message:  Average luminosity $luma")
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
                createFile(
                    getOutputDirectory(),
                    viewModel.currentBrightness.value,
                    PHOTO_EXTENSION
                )

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
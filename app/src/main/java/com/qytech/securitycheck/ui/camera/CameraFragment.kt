package com.qytech.securitycheck.ui.camera

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.MimeTypeMap
import android.widget.AdapterView
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.CameraView
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import cn.jzvd.JZUtils.getWindow
import com.qytech.securitycheck.PreviewActivity
import com.qytech.securitycheck.R
import com.qytech.securitycheck.databinding.CameraFragmentBinding
import com.qytech.securitycheck.extensions.showToast
import com.qytech.securitycheck.utils.FileUtils
import com.qytech.securitycheck.utils.LuminosityAnalyzer
import com.szadst.szoemhost_lib.DevComm
import com.szadst.szoemhost_lib.HostLib
import com.szadst.szoemhost_lib.IFPListener.FPCommandListener
import kotlinx.android.synthetic.main.camera_fragment.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.experimental.and
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
    var m_szDevice: String? = "/dev/ttyS4"
    var m_nBaudrate = 115200
    var m_nUserID = 0
    var m_strPost: String? = null
    var m_nTemplateSize = 0
    var m_bForce = false
    lateinit var m_TemplateData: ByteArray

    private fun GetInputTemplateNo(): Int {
        val str: String
        str = editUserID.getText().toString()
        if (str.isEmpty()) {
            showToast("请输入用户ID")
            return -1
        }
        m_nUserID = try {
            str.toInt()
        } catch (e: NumberFormatException) {
            showToast(String.format("请输入正确的用户ID(1~%d)", DevComm.MAX_RECORD_COUNT.toShort()))
            return -1
        }
        return m_nUserID
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HostLib.getInstance(activity).FPCmdProc().OpenDevice(m_szDevice, m_nBaudrate)

    }

    fun WriteTemplateFile(p_nUserID: Int, pTemplate: ByteArray?, p_nSize: Int): Boolean {
        val w_szSaveDirPath =
            Environment.getExternalStorageDirectory().absolutePath + "/sz_template"
        val w_fpDir = File(w_szSaveDirPath)
        if (!w_fpDir.exists()) w_fpDir.mkdirs()
        val w_fpTemplate =
            File("$w_szSaveDirPath/$p_nUserID.fpt")
        if (!w_fpTemplate.exists()) {
            try {
                w_fpTemplate.createNewFile()
            } catch (e: IOException) {
                e.printStackTrace()
                return false
            }
        }
        var w_foTemplate: FileOutputStream? = null
        try {
            w_foTemplate = FileOutputStream(w_fpTemplate)
            w_foTemplate.write(pTemplate, 0, p_nSize)
            w_foTemplate.close()
            m_strPost += "\nSaved file path = $w_szSaveDirPath/$p_nUserID.fpt"
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }


    private fun ProcResponsePacket(p_nCode: Int, p_nRet: Int, p_nParam1: Int, p_nParam2: Int) {
        m_strPost = ""
        when (p_nCode) {
            DevComm.CMD_ENROLL_CODE,
            DevComm.CMD_VERIFY_CODE,
            DevComm.CMD_IDENTIFY_CODE,
            DevComm.CMD_IDENTIFY_FREE_CODE,
            DevComm.CMD_ENROLL_ONETIME_CODE,
            DevComm.CMD_CHANGE_TEMPLATE_CODE,
            DevComm.CMD_VERIFY_WITH_DOWN_TMPL_CODE,
            DevComm.CMD_IDENTIFY_WITH_DOWN_TMPL_CODE,
            DevComm.CMD_VERIFY_WITH_IMAGE_CODE,
            DevComm.CMD_IDENTIFY_WITH_IMAGE_CODE ->
                if (p_nRet == DevComm.ERR_SUCCESS) {
                    when (p_nParam1) {
                        DevComm.NEED_RELEASE_FINGER -> m_strPost = ("收起手指")
                        DevComm.NEED_FIRST_SWEEP -> m_strPost = ("放置手指")
                        DevComm.NEED_SECOND_SWEEP -> m_strPost = ("再来二次")
                        DevComm.NEED_THIRD_SWEEP -> m_strPost = ("再来一次")
                        else -> {
                            m_strPost = String.format("成功\r\n模板为 : %d", p_nParam1)
                            viewModel.currentSwitch.value?.isChecked = true
                            btn_picture_viewer.setOnClickListener {
                                PreviewActivity.start(requireContext(), getOutputDirectory().absolutePath)
                            }
                        }
                    }
                    showToast(m_strPost!!)
                } else {
                    viewModel.currentSwitch.value?.isChecked = false
                    m_strPost = String.format("失败\r\n")
                    m_strPost += getErrorMsg(p_nParam1.toShort())
                    showToast(m_strPost!!)
                }
            DevComm.CMD_GET_EMPTY_ID_CODE -> if (p_nRet == DevComm.ERR_SUCCESS) {
                m_strPost = String.format("成功\r\n空ID : %d", p_nParam1)
                showToast(m_strPost!!)
            } else {
                m_strPost = String.format("失败\r\n")
                m_strPost += getErrorMsg(p_nParam1.toShort())
                showToast(m_strPost!!)
            }
            DevComm.CMD_GET_ENROLL_COUNT_CODE -> if (p_nRet == DevComm.ERR_SUCCESS) {
                m_strPost = String.format("成功\r\n注册数 : %d", p_nParam1)
                showToast(m_strPost!!)
            } else {
                m_strPost = String.format("失败\r\n")
                m_strPost += getErrorMsg(p_nParam1.toShort())
                showToast(m_strPost!!)
            }
            DevComm.CMD_CLEAR_TEMPLATE_CODE -> if (p_nRet == DevComm.ERR_SUCCESS) {
                m_strPost = String.format("成功\r\n模板为 : %d", p_nParam1)
                showToast(m_strPost!!)
            } else {
                m_strPost = String.format("失败\r\n")
                m_strPost += getErrorMsg(p_nParam1.toShort())
                showToast(m_strPost!!)
            }
            DevComm.CMD_CLEAR_ALLTEMPLATE_CODE -> if (p_nRet == DevComm.ERR_SUCCESS) {
                m_strPost = String.format(
                    "成功\r\n清除模板 : %d",
                    p_nParam1
                )
                showToast(m_strPost!!)
            } else {
                m_strPost = String.format("失败\r\n")
                m_strPost += getErrorMsg(p_nParam1.toShort())
                showToast(m_strPost!!)
            }
            DevComm.CMD_READ_TEMPLATE_CODE -> if (p_nRet == DevComm.ERR_SUCCESS) {
                m_strPost = String.format("成功\r\n模板为 : %d", p_nParam1)
                WriteTemplateFile(p_nParam1, m_TemplateData, m_nTemplateSize)
                showToast(m_strPost!!)
            } else {
                m_strPost = String.format("失败\r\n")
                m_strPost += getErrorMsg(p_nParam1.toShort())
                showToast(m_strPost!!)
            }
            DevComm.CMD_WRITE_TEMPLATE_CODE -> if (p_nRet == DevComm.ERR_SUCCESS) {
                m_strPost = String.format("成功\r\n模板 : %d", p_nParam1)
            } else {
                m_strPost = String.format("失败\r\n")
                m_strPost += getErrorMsg(p_nParam1.toShort())
                if (p_nParam1 == DevComm.ERR_DUPLICATION_ID) {
                    m_strPost += String.format(" %d.", p_nParam2)
                    showToast(m_strPost!!)
                }
                showToast(m_strPost!!)
            }
            DevComm.CMD_UP_IMAGE_CODE -> {
                if (p_nRet == DevComm.ERR_SUCCESS) {
                    m_strPost = String.format("Result : Receive Image Success")
                } else {
                    m_strPost = String.format("Result : Fail\r\n")
                    m_strPost += getErrorMsg(p_nParam1.toShort())
                    showToast(m_strPost!!)
                }
            }
            DevComm.CMD_VERIFY_DEVPASS_CODE, DevComm.CMD_SET_DEVPASS_CODE, DevComm.CMD_EXIT_DEVPASS_CODE -> if (p_nRet == DevComm.ERR_SUCCESS) {
                m_strPost = String.format("成功")
            } else {
                m_strPost = String.format("失败\r\n")
                m_strPost += getErrorMsg(p_nParam1.toShort())
                showToast(m_strPost!!)
            }
        }
    }

    fun getErrorMsg(p_wErrorCode: Short): String? {
        return when (p_wErrorCode and 0xFF) {
            DevComm.ERR_VERIFY.toShort() -> "证实指纹"
            DevComm.ERR_IDENTIFY.toShort() -> "认出指纹"
            DevComm.ERR_EMPTY_ID_NOEXIST.toShort() -> "空模板不存在"
            DevComm.ERR_BROKEN_ID_NOEXIST.toShort() -> "模板破损不存在"
            DevComm.ERR_TMPL_NOT_EMPTY.toShort() -> "此ID的模板已经存在"
            DevComm.ERR_TMPL_EMPTY.toShort() -> "此模板已经为空"
            DevComm.ERR_INVALID_TMPL_NO.toShort() -> "无效的模板"
            DevComm.ERR_ALL_TMPL_EMPTY.toShort() -> "所有模板为空"
            DevComm.ERR_INVALID_TMPL_DATA.toShort() -> "无效的模板数据"
            DevComm.ERR_DUPLICATION_ID.toShort() -> "ID重复 : "
            DevComm.ERR_TIME_OUT.toShort() -> "超时"
            DevComm.ERR_NOT_AUTHORIZED.toShort() -> "设备未授权"
            DevComm.ERR_EXCEPTION.toShort() -> "异常程序错误 "
            DevComm.ERR_MEMORY.toShort() -> "内存错误 "
            DevComm.ERR_INVALID_PARAM.toShort() -> "无效的参数"
            DevComm.ERR_NO_RELEASE.toShort() -> "手指松开失败"
            DevComm.ERR_INTERNAL.toShort() -> "内部错误"
            DevComm.ERR_INVALID_OPERATION_MODE.toShort() -> "无效的操作模式"
            DevComm.ERR_FP_NOT_DETECTED.toShort() -> "未检测到手指"
            DevComm.ERR_ADJUST_SENSOR.toShort() -> "传感器调整失败"
            else -> "失败"
        }
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
            showToast("请验证指纹")
            if (HostLib.getInstance(activity).FPCmdProc().Run_CmdIdentify(m_bForce)>0) {
                PreviewActivity.start(requireContext(), getOutputDirectory().absolutePath)
            }else{
                return@setOnClickListener
            }
        }

        btnEnroll.setOnClickListener {
            val w_nTemplateNo: Int
            w_nTemplateNo = GetInputTemplateNo()
            if (w_nTemplateNo < 0) return@setOnClickListener
            HostLib.getInstance(activity).FPCmdProc().Run_CmdEnroll(w_nTemplateNo, m_bForce)
            viewModel.currentSwitch.value?.isChecked = false
        }

        btnIdentify.setOnClickListener {
            val runCmdidentify = HostLib.getInstance(activity).FPCmdProc().Run_CmdIdentify(m_bForce)
            if (runCmdidentify > 1) {
                viewModel.currentSwitch.value?.isChecked = false
            }
        }

        btnVerify.setOnClickListener {
            val w_nTemplateNo: Int
            w_nTemplateNo = GetInputTemplateNo()
            if (w_nTemplateNo < 0) return@setOnClickListener
            HostLib.getInstance(activity).FPCmdProc().Run_CmdVerify(w_nTemplateNo, m_bForce)
            viewModel.currentSwitch.value?.isChecked = false
        }

        btnGetEnrollCount.setOnClickListener {
            HostLib.getInstance(activity).FPCmdProc().Run_CmdGetUserCount(m_bForce)
            viewModel.currentSwitch.value?.isChecked = false
        }

        btnRemoveAll.setOnClickListener {
            HostLib.getInstance(activity).FPCmdProc().Run_CmdDeleteAll(m_bForce)
        }

        getWindow(activity).addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        spnBaudrate.setSelection(4)
        m_TemplateData = ByteArray(DevComm.SZ_MAX_RECORD_SIZE)
        HostLib.getInstance(activity).FPCmdProc().GetDeviceList(spnDevice)
        HostLib.getInstance(activity).FPCmdProc().SetListener(object : FPCommandListener {
            override fun cmdProcReturn(
                p_nCmdCode: Int,
                p_nRetCode: Int,
                p_nParam1: Int,
                p_nParam2: Int
            ) {
                ProcResponsePacket(p_nCmdCode, p_nRetCode, p_nParam1, p_nParam2)
            }

            override fun cmdProcReturnData(p_pData: ByteArray, p_nSize: Int) {
                var i: Int
                if (p_nSize > DevComm.SZ_MAX_RECORD_SIZE) {
                } else {
                    System.arraycopy(p_pData, 0, m_TemplateData, 0, p_nSize)
                    m_nTemplateSize = p_nSize
                }
            }

            override fun cmdProcShowText(p_szInfo: String) { // show information
                showToast(p_szInfo)
            }
        }) {

        }

        spnBaudrate.setOnItemSelectedListener(object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View,
                position: Int,
                id: Long
            ) {
                m_nBaudrate =
                    if (position == 0) 9600 else if (position == 1) 19200 else if (position == 2) 38400 else if (position == 3) 57600 else 115200
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        })

        spnDevice.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View,
                position: Int,
                id: Long
            ) {
                m_szDevice = spnDevice.getItemAtPosition(position).toString()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        })
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
            switch_brightness_365nm
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
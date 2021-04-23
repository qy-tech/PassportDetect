package com.qytech.securitycheck.ui.opengl

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.qytech.securitycheck.databinding.FragmentCameraGlBinding
import com.qytech.securitycheck.utils.LuminosityAnalyzer
import kotlinx.android.synthetic.main.preview_fragment.*
import timber.log.Timber
import java.util.concurrent.Executors

/**
 * Created by Jax on 2021/4/23.
 * Description :
 * Version : V1.0.0
 */
class GlCameraFragment : Fragment() {

    companion object {
        fun newInstance(): GlCameraFragment {
            val args = Bundle()

            val fragment = GlCameraFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private val executor = Executors.newSingleThreadExecutor()

    lateinit var dataBinding: FragmentCameraGlBinding
    lateinit var cameraProvider: ProcessCameraProvider
    lateinit var renderer: GlRenderer

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dataBinding = FragmentCameraGlBinding.inflate(inflater, container, false)
        return dataBinding.root//super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        renderer = GlRenderer(dataBinding.viewFinder)
        dataBinding.viewFinder.preserveEGLContextOnPause = true
        dataBinding.viewFinder.setEGLContextClientVersion(2)
        dataBinding.viewFinder.setRenderer(renderer)
        dataBinding.viewFinder.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        dataBinding.viewFinder.post {
            ProcessCameraProvider.getInstance(requireContext()).apply {
                addListener({
                    cameraProvider = this.get()
                    startCamera()
                }, ContextCompat.getMainExecutor(requireContext()))
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        cameraProvider.unbindAll()
    }

    private fun startCamera() {
        val imageAnalysis = ImageAnalysis.Builder()
            //.setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .setTargetResolution(Size(1920, 1080))
            .build().apply {
                setAnalyzer(
                    executor,
                    renderer
                )
            }

        val preview = Preview.Builder()
            .build()
            .apply {

            }

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            viewLifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            imageAnalysis,
        )
    }

}
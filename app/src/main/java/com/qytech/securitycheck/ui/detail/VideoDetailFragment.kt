package com.qytech.securitycheck.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.qytech.securitycheck.consts.ExtraConst
import com.qytech.securitycheck.databinding.VideoDetailFragmentBinding

class VideoDetailFragment : Fragment() {

    companion object {
        fun newInstance(videoPath: String) = VideoDetailFragment().apply {
            val args = Bundle()
            args.putString(ExtraConst.FILE_PATH, videoPath)
            arguments = args
        }
    }

    private lateinit var viewModel: VideoDetailViewModel
    private lateinit var dataBinding: VideoDetailFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dataBinding = VideoDetailFragmentBinding.inflate(inflater, container, false)
        return dataBinding.root
//        return inflater.inflate(R.layout.video_detail_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(VideoDetailViewModel::class.java)
        dataBinding.lifecycleOwner = viewLifecycleOwner
        dataBinding.viewModel = viewModel
        val videoPath = arguments?.getString(ExtraConst.FILE_PATH) ?: ""
        viewModel.setImagePath(videoPath)
    }

}
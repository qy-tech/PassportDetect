package com.qytech.securitycheck.ui.preview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.qytech.securitycheck.DetailActivity
import com.qytech.securitycheck.consts.ExtraConst
import com.qytech.securitycheck.databinding.PreviewFragmentBinding
import java.io.File

class PreviewFragment : Fragment() {

    companion object {
        fun newInstance(path: String) = PreviewFragment().apply {
            val bundle = Bundle()
            bundle.putString(ExtraConst.FILE_PATH, path)
            arguments = bundle
        }
    }

    private lateinit var viewModel: PreviewViewModel
    private lateinit var dataBinding: PreviewFragmentBinding
    private lateinit var adapter: PreviewAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dataBinding = PreviewFragmentBinding.inflate(inflater, container, false)
        return dataBinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(PreviewViewModel::class.java)
        dataBinding.lifecycleOwner = viewLifecycleOwner
        dataBinding.viewModel = viewModel
        viewModel.fetchData(arguments?.getString(ExtraConst.FILE_PATH, "") ?: "")
        adapter = PreviewAdapter()
        adapter.onItemClickListener = object : PreviewAdapter.OnItemClickListener {
            override fun onItemClick(item: File?) {
                DetailActivity.start(requireContext(), item?.absolutePath?:"")
            }
        }
        dataBinding.adapter = adapter
        viewModel.data.observe(viewLifecycleOwner, {
            adapter.submitList(it)
        })
    }

}
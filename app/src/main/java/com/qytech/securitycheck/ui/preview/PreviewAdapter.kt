package com.qytech.securitycheck.ui.preview

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.qytech.securitycheck.databinding.ItemPreviewBinding
import java.io.File

/**
 * Created by Jax on 2020/11/10.
 * Description :
 * Version : V1.0.0
 */
class PreviewAdapter : ListAdapter<File, PreviewAdapter.FileViewHolder>(Companion) {
    class FileViewHolder(val binding: ItemPreviewBinding) : RecyclerView.ViewHolder(binding.root)

     var onItemClickListener: OnItemClickListener? = null

    companion object : DiffUtil.ItemCallback<File>() {
        override fun areItemsTheSame(oldItem: File, newItem: File): Boolean = oldItem === newItem
        override fun areContentsTheSame(oldItem: File, newItem: File): Boolean =
            oldItem.absolutePath == newItem.absolutePath
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemPreviewBinding.inflate(layoutInflater)
        return FileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val item = getItem(position)
        holder.itemView.setOnClickListener {
            onItemClickListener?.onItemClick(item)
        }
        holder.binding.item = item
        holder.binding.executePendingBindings()
    }

    interface OnItemClickListener {
        fun onItemClick(item: File?)
    }

}
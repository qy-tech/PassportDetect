package com.qytech.securitycheck.utils

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import cn.jzvd.JzvdStd
import com.bumptech.glide.Glide
import timber.log.Timber

/**
 * Created by Jax on 2020/11/10.
 * Description :
 * Version : V1.0.0
 */
@BindingAdapter(value = ["setAdapter"])
fun RecyclerView.bindRecyclerViewAdapter(adapter: RecyclerView.Adapter<*>) {
    this.run {
        this.setHasFixedSize(true)
        this.adapter = adapter
    }
}

@BindingAdapter(value = ["setImageUrl"])
fun ImageView.bindImageUrl(url: String?) {
    if (url != null && url.isNotBlank()) {
        Glide.with(context).load(url).into(this)
    }
}

@BindingAdapter(value = ["setVideoUrl", "setTitle"])
fun JzvdStd.bindVideoUrl(url: String?, title: String?) {
    if (url != null && url.isNotBlank()) {
        Timber.d("bindVideoUrl message:  $url")
        setUp(url, title)
    }
}
package com.qytech.securitycheck.ui.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import timber.log.Timber

class VideoDetailViewModel : ViewModel() {
    private val _videoPath = MutableLiveData<String>()
    val videoPath: LiveData<String> = _videoPath

    fun setImagePath(path: String) {
        _videoPath.value = path
        Timber.d("setImagePath message:  $path")
    }
}
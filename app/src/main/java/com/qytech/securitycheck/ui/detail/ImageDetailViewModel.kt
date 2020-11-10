package com.qytech.securitycheck.ui.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * Created by Jax on 2020/11/10.
 * Description :
 * Version : V1.0.0
 */
class ImageDetailViewModel : ViewModel() {
    private val _imagePath = MutableLiveData<String>()
    val imagePath: LiveData<String> = _imagePath

    fun setImagePath(path: String) {
        _imagePath.value = path
    }
}
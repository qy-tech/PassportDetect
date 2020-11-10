package com.qytech.securitycheck.ui.preview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.io.File

class PreviewViewModel : ViewModel() {
    private val _data = MutableLiveData<List<File>>()
    val data: LiveData<List<File>>
        get() = _data

    fun fetchData(path: String) {
        val file = File(path)
        if (file.exists() && file.isDirectory) {
            _data.value = file.listFiles()?.toMutableList()?.reversed()
        }
    }

}
package com.qytech.securitycheck.ui.camera

import android.view.View
import android.widget.CompoundButton
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qytech.securitycheck.GlobalApplication
import com.qytech.securitycheck.R.id
import com.qytech.securitycheck.utils.FileUtils
import com.szadst.szoemhost_lib.HostLib
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

class CameraViewModel : ViewModel() {

    companion object {
        const val LED_365NM = "365nm"
        const val LED_410NM = "410nm"
        const val LED_940NM = "940nm"
        const val LED_WHITE = "White"
        const val LED_HRLAMP = "HRlamp"
        const val LED_OFF = "default"

        val BRIGHTNESS_MAP = mapOf<String, String>(
            LED_365NM to "/sys/class/leds/365nm-led/brightness",
            LED_410NM to "/sys/class/leds/410nm-led/brightness",
            LED_940NM to "/sys/class/leds/940nm-led/brightness",
            LED_WHITE to "/sys/class/leds/White-led/brightness",
            LED_HRLAMP to "/sys/class/leds/HRlamp-led/brightness"
        )

        val HRLAMP_LIST_LEFT = 1..15
        val HRLAMP_LIST_RIGHT = 16..30
        const val HRLAMP_PATH_FORMAT = "/sys/class/leds/led%d/brightness"
        private const val BRIGHTNESS_ON = "1"
        private var BRIGHTNESS_OFF = "0"
        private const val BRIGHTNESS_VALUE_MAX = "255"
    }

    private val _currentBrightness = MutableLiveData("default")
    private val _currentSwitch = MutableLiveData<CompoundButton>()
    val currentSwitch: LiveData<CompoundButton>
        get() = _currentSwitch
    val currentBrightness: LiveData<String>
        get() = _currentBrightness
    private val _interval = MutableLiveData(100L)
    val interval: LiveData<Long>
        get() = _interval

    private var hrlampJob: Job? = null

    private fun closeAllBrightness() {
        BRIGHTNESS_MAP.forEach {
            toggleBrightness(it.value, false)
        }
    }

    fun toggleBrightness(path: String, toggle: Boolean) {
        FileUtils.write2File(File(path), if (toggle) BRIGHTNESS_ON else BRIGHTNESS_OFF)
    }

    fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        Timber.d("onCheckedChanged message:  ${buttonView?.id}")
        if (isChecked && HostLib.getInstance(GlobalApplication.instance).FPCmdProc().Run_CmdIdentify(false) <= 0) {
            _currentSwitch.value = buttonView
            buttonView?.isChecked = false
            return
        }
        when (buttonView?.id) {
            id.switch_brightness_365nm -> {
                _currentBrightness.value = if (isChecked) LED_365NM else LED_OFF
                toggleBrightness(BRIGHTNESS_MAP.getValue(LED_365NM), isChecked)
            }
            id.switch_brightness_410nm -> {
                _currentBrightness.value = if (isChecked) LED_410NM else LED_OFF
                toggleBrightness(BRIGHTNESS_MAP.getValue(LED_410NM), isChecked)
            }
            id.switch_brightness_960nm -> {
                _currentBrightness.value = if (isChecked) LED_940NM else LED_OFF
                toggleBrightness(BRIGHTNESS_MAP.getValue(LED_940NM), isChecked)
            }
            id.switch_brightness_white -> {
                _currentBrightness.value = if (isChecked) LED_WHITE else LED_OFF
                toggleBrightness(BRIGHTNESS_MAP.getValue(LED_WHITE), isChecked)
            }
            id.switch_brightness_HRlamp -> {
                _currentBrightness.value = if (isChecked) LED_HRLAMP else LED_OFF
                toggleBrightness(BRIGHTNESS_MAP.getValue(LED_HRLAMP), isChecked)
                hrlampToggle(isChecked)
            }
        }
    }

    private fun hrlampToggle(toggle: Boolean) {
        if (!toggle) {
            cancelHrlampJob()
            return
        }
        hrlampJob = viewModelScope.launch {
            while (isActive) {
                val list = (HRLAMP_LIST_LEFT zip HRLAMP_LIST_RIGHT)
                list.forEach {
                    toggleHrlampGroup(it)
                }
                list.reversed().forEach {
                    toggleHrlampGroup(it)
                }
            }
        }
    }

    private suspend fun toggleHrlampGroup(ledNumbers: Pair<Int, Int>) {
        val first = File(HRLAMP_PATH_FORMAT.format(ledNumbers.first))
        val second = File(HRLAMP_PATH_FORMAT.format(ledNumbers.second))
        Timber.d("hrlampToggle open :  let${ledNumbers.first} and led${ledNumbers.second}")
        FileUtils.write2File(first, BRIGHTNESS_VALUE_MAX)
        FileUtils.write2File(second, BRIGHTNESS_VALUE_MAX)
        delay(_interval.value ?: 100L)
        Timber.d("hrlampToggle close :  let${ledNumbers.first} and led${ledNumbers.second}")
        FileUtils.write2File(first, BRIGHTNESS_OFF)
        FileUtils.write2File(second, BRIGHTNESS_OFF)
    }


    private fun cancelHrlampJob() {
        Timber.d("cancelHrlampJob message close all Hrlamp led")
        (HRLAMP_LIST_LEFT zip HRLAMP_LIST_RIGHT).forEach {
            FileUtils.write2File(File(HRLAMP_PATH_FORMAT.format(it.first)), BRIGHTNESS_OFF)
            FileUtils.write2File(File(HRLAMP_PATH_FORMAT.format(it.second)), BRIGHTNESS_OFF)
        }
        hrlampJob?.cancel()
    }

    fun onClick(view: View) {
        _interval.value?.let {
            if (it < 1000L) {
                _interval.value = it + 100L
            } else {
                _interval.value = 100L
            }
            cancelHrlampJob()
            hrlampToggle(_currentBrightness.value == LED_HRLAMP)
        }
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("onCleared message:  ")
        closeAllBrightness()
        cancelHrlampJob()
    }
}
package com.qytech.securitycheck.ui.camera

import android.widget.CompoundButton
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.qytech.securitycheck.utils.FileUtils
import com.qytech.securitycheck.R
import timber.log.Timber
import java.io.File

class CameraViewModel : ViewModel() {

    companion object {
        val BRIGHTNESS_MAP = mapOf<String, String>(
            "365nm" to "/sys/class/leds/365nm-led/brightness",
            "410nm" to "/sys/class/leds/410nm-led/brightness",
            "940nm" to "/sys/class/leds/940nm-led/brightness",
            "white" to "/sys/class/leds/White-led/brightness",
            "HRlamp" to "/sys/class/leds/HRlamp-led/brightness"
        )
        private const val BRIGHTNESS_ON = "1"
        private const val BRIGHTNESS_OFF = "0"
    }

    private val _currentBrightness = MutableLiveData("default")
    val currentBrightness: LiveData<String>
        get() = _currentBrightness

    fun toggleAllBrightness(toggle: Boolean = false) {
        BRIGHTNESS_MAP.forEach {
            toggleBrightness(it.value, toggle)
        }
    }

    fun toggleBrightness(path: String, toggle: Boolean) {
        FileUtils.write2File(File(path), if (toggle) BRIGHTNESS_ON else BRIGHTNESS_OFF)
    }

    fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        Timber.d("onCheckedChanged message:  ${buttonView?.id}")
        when (buttonView?.id) {
            R.id.switch_brightness_365nm -> {
                _currentBrightness.value = "365nm"
                toggleBrightness(BRIGHTNESS_MAP.getValue("365nm"), isChecked)
            }
            R.id.switch_brightness_410nm -> {
                _currentBrightness.value = "410nm"
                toggleBrightness(BRIGHTNESS_MAP.getValue("410nm"), isChecked)
            }
            R.id.switch_brightness_960nm -> {
                _currentBrightness.value = "940nm"
                toggleBrightness(BRIGHTNESS_MAP.getValue("940nm"), isChecked)
            }
            R.id.switch_brightness_white -> {
                _currentBrightness.value = "white"
                toggleBrightness(BRIGHTNESS_MAP.getValue("white"), isChecked)
            }
            R.id.switch_brightness_HRlamp -> {
                _currentBrightness.value = "HRlamp"
                toggleBrightness(BRIGHTNESS_MAP.getValue("HRlamp"), isChecked)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("onCleared message:  ")
        toggleAllBrightness(false)
    }


}
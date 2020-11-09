package com.qytech.securitycheck.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qytech.securitycheck.FileUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel : ViewModel() {
    //sys/class/leds/365nm-led/brightness
    //sys/class/leds/410nm-led/brightness
    //sys/class/leds/940nm-led/brightness
    companion object {
        const val LED_BRIGHTNESS_365NM = "/sys/class/leds/365nm-led/brightness"
        const val LED_BRIGHTNESS_410NM = "/sys/class/leds/410nm-led/brightness"
        const val LED_BRIGHTNESS_940NM = "/sys/class/leds/940nm-led/brightness"
        const val LED_BRIGHTNESS_OPEN = "1"
        const val LED_BRIGHTNESS_CLOSE = "0"
        val ledScrollLists = arrayOf(
            LED_BRIGHTNESS_365NM,
            LED_BRIGHTNESS_410NM,
            LED_BRIGHTNESS_940NM
        )

    }

    fun ledScroll() {
        viewModelScope.launch {
            while (isActive) {
                ledScrollLists.forEach {
                    FileUtils.write2File(File(it), LED_BRIGHTNESS_OPEN)
                    delay(1000L)
                    FileUtils.write2File(File(it), LED_BRIGHTNESS_CLOSE)
                    delay(1000L)
                }
            }
        }
    }


}
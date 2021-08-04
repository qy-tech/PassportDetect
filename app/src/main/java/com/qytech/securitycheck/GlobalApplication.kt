package com.qytech.securitycheck

import android.app.Application
import timber.log.Timber

/**
 * Created by Jax on 2020/10/24.
 * Description :
 * Version : V1.0.0
 */
class GlobalApplication : Application() {
    companion object {
        const val TAG = "MultipleCamera"
    }

    override fun onCreate() {
        super.onCreate()
        Timber.plant(debugTree)
    }

    private val debugTree = object : Timber.DebugTree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            super.log(priority, "${TAG}_${tag}", message, t)
        }
    }
}
package com.qytech.securitycheck

import android.app.Application
import timber.log.Timber

/**
 * Created by Jax on 2020/10/24.
 * Description :
 * Version : V1.0.0
 */
class GlobalApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}
package com.qytech.securitycheck

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment

/**
 * Created by Jax on 2020/10/27.
 * Description :
 * Version : V1.0.0
 */
fun Context.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Context.showToast(@StringRes message: Int) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Fragment.showToast(message: String) {
    requireContext().showToast(message)
}

fun Fragment.showToast(@StringRes message: Int) {
    requireContext().showToast(message)
}

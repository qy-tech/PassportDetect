package com.qytech.securitycheck.extension

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.DisplayMetrics
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.qytech.securitycheck.R

/**
 * Created by Jax on 2019-12-05.
 * Description :
 * Version : V1.0.0
 */
val Activity.screenSize: Pair<Int, Int>
    get() {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        return Pair(displayMetrics.widthPixels, displayMetrics.heightPixels)
    }

fun Context.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Context.showToast(@StringRes resId: Int) {
    Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
}

fun Context.executeOrShowToast(flag: Boolean, execute: () -> Unit, message: String) {
    if (flag) {
        execute()
    } else {
        showToast(message)
    }
}

fun Context.executeOrShowToast(flag: Boolean, execute: () -> Unit, @StringRes message: Int) {
    if (flag) {
        execute()
    } else {
        showToast(message)
    }
}

fun Context.executeOrShowToast(flag: () -> Boolean, execute: () -> Unit, message: String) {
    if (flag()) {
        execute()
    } else {
        showToast(message)
    }
}

fun Context.executeOrShowToast(flag: () -> Boolean, execute: () -> Unit, @StringRes message: Int) {
    if (flag()) {
        execute()
    } else {
        showToast(message)
    }
}

fun Context.alert(
    message: CharSequence,
    title: CharSequence? = null,
    init: (AlertDialog.Builder.() -> Unit)? = null
): AlertDialog.Builder {
    return AlertDialog.Builder(this).apply {
        if (!title.isNullOrBlank()) {
            setTitle(title)
        }
        setMessage(message)
        init?.invoke(this)
    }
}

fun Context.alert(
    @StringRes message: Int,
    @StringRes title: Int? = null,
    init: (AlertDialog.Builder.() -> Unit)? = null
): AlertDialog {
    val create = AlertDialog.Builder(this, R.style.ThemeOverlay_AppCompat_Dialog_Alert)
            .apply {
                setMessage(message)
                if (title != null && title != 0) {
                    setTitle(title)
                }
                init?.invoke(this)
            }.create()
    create.setCanceledOnTouchOutside(false)
    return create
}


fun Context.alert(
    message: String,
    title: String? = null,
    init: (AlertDialog.Builder.() -> Unit)? = null
) =
    AlertDialog.Builder(this, R.style.ThemeOverlay_AppCompat_Dialog_Alert)
        .apply {
            setMessage(message)
            if (title.isNullOrBlank()) {
                setTitle(title)
            }
            init?.invoke(this)
        }.create()

fun createBroadcastReceiver(receive: (Context?, Intent?) -> Unit) =
    object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            receive(context, intent)
        }
    }

fun FragmentActivity.replaceFragment(
    @IdRes containerViewId: Int,
    fragment: Fragment,
    tag: String? = null
) {
    supportFragmentManager.beginTransaction().apply {
        replace(containerViewId, fragment, tag)
        setMaxLifecycle(fragment, lifecycle.currentState)
    }.commit()
}

fun FragmentActivity.showHideFragment(showFragment: Fragment, vararg hideFragment: Fragment) {
    supportFragmentManager.beginTransaction().apply {
        hideFragment.map {
            if (it.isAdded && it.isVisible) {
                hide(it)
            }
        }
        if (showFragment.isAdded && showFragment.isHidden) {
            show(showFragment)
            setMaxLifecycle(showFragment, lifecycle.currentState)
        }
    }.commit()
}

fun FragmentActivity.addFragment(
    @IdRes containerViewId: Int, fragment: Fragment,
    tag: String? = null
) {
    supportFragmentManager.beginTransaction().apply {
        if (!fragment.isAdded) {
            add(containerViewId, fragment, tag)
        }
    }.commit()
}

fun FragmentActivity.addFragment(fragment: Fragment, tag: String?) {
    supportFragmentManager.beginTransaction().apply {
        if (!fragment.isAdded) {
            add(fragment, tag)
        }
    }.commit()
}

fun FragmentActivity.removeFragment(fragment: Fragment) {
    supportFragmentManager.beginTransaction().apply {
        if (fragment.isAdded) {
            remove(fragment)
        }
    }.commit()
}

fun Context.checkAppInstalled(packageName: String) =
    packageManager.getInstalledPackages(PackageManager.GET_ACTIVITIES).any {
        it.packageName == packageName
    }

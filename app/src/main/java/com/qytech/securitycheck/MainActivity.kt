package com.qytech.securitycheck

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.qytech.securitycheck.ui.camera.CameraFragment
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    companion object {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
        )
        const val REQUEST_CODE_PERMISSION = 0x1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        permissions.any { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED
        }.let { result ->
            if (result) {
                ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_PERMISSION)
            } else {
                if (savedInstanceState == null) {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container, CameraFragment.newInstance())
                        .commitNow()
                }
            }
        }

        Timber.d("onCreate: ")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (REQUEST_CODE_PERMISSION == requestCode) {
            grantResults.all {
                it == PackageManager.PERMISSION_GRANTED
            }.let {
                if (it) {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container, CameraFragment.newInstance())
                        .commitNow()
                }
            }
        }
    }
}
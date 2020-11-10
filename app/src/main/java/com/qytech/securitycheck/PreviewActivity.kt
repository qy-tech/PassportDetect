package com.qytech.securitycheck

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.qytech.securitycheck.consts.ExtraConst
import com.qytech.securitycheck.ui.preview.PreviewFragment
import kotlinx.android.synthetic.main.preview_activity.*

class PreviewActivity : AppCompatActivity() {

    companion object {
        @JvmStatic
        fun start(context: Context, path: String) {
            val starter = Intent(context, PreviewActivity::class.java)
                .putExtra(ExtraConst.FILE_PATH, path)
            context.startActivity(starter)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.preview_activity)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)//左侧添加一个默认的返回图标
        supportActionBar?.setHomeButtonEnabled(true) //设置返回键可用
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.container,
                    PreviewFragment.newInstance(intent.getStringExtra(ExtraConst.FILE_PATH) ?: "")
                )
                .commitNow()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }
}
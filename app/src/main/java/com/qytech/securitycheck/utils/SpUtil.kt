package com.qytech.securitycheck.utils

import android.app.Activity
import android.content.Context
import com.qytech.securitycheck.GlobalApplication

object SpUtil {
    //存储key对应的数据
    fun saveData(context: Context, key: String, info: String) {
        val sharedPreferences = context.getSharedPreferences(key, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(key, info)
        editor.apply()
    }

    fun saveData(context: Context, key: String, info: Int) {
        val sharedPreferences = context.getSharedPreferences(key, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt(key, info)
        editor.apply()
    }

    fun saveData(context: Context, key: String, info: Boolean) {
        val sharedPreferences = context.getSharedPreferences(key, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean(key, info)
        editor.apply()
    }

    fun saveDataPos(context: Context, key: String, info: Int) {
        val sharedPreferences = context.getSharedPreferences(key, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt(key, info)
        editor.apply()
    }


    //取key对应的数据
    fun getData(context: Context, key: String): String {
        val result = context.getSharedPreferences(key, Context.MODE_PRIVATE).getString(key, "")
        if (result != null) {
            return if (result.isEmpty()) {
                ""
            } else {
                result
            }
        }
        return ""
    }


    //清空缓存对应key的数据
    fun clearData(context: Activity, key: String) {
        context.getSharedPreferences(key, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
package com.qytech.securitycheck.utils
import android.preference.PreferenceManager
import com.qytech.securitycheck.GlobalApplication

/**
 * Created by Jax on 2018/8/28.
 * Description :
 * Version : V1.0.0
 */
object PreferenceUtils {

    private val preferences =
            PreferenceManager.getDefaultSharedPreferences(GlobalApplication.instance)


    @Suppress("UNCHECKED_CAST")
    fun <U> findPreference(name: String, default: U): U =
            preferences.let {
                when (default) {
                    is Long -> it.getLong(name, default)
                    is String -> it.getString(name, default) ?: ""
                    is Int -> it.getInt(name, default)
                    is Boolean -> it.getBoolean(name, default)
                    is Float -> it.getFloat(name, default)
                    else -> default
                }
            } as U

    fun putPreference(name: String, default: Any) =
            preferences.edit().let {
                when (default) {
                    is Long -> it.putLong(name, default)
                    is String -> it.putString(name, default)
                    is Int -> it.putInt(name, default)
                    is Boolean -> it.putBoolean(name, default)
                    is Float -> it.putFloat(name, default)
                    else -> it.putString(name, default.toString())
                }
            }.apply()

    fun clear() = preferences.edit().clear().apply()

}
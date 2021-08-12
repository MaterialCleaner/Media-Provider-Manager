package me.gm.cleaner.plugin.util

import android.util.Log
import me.gm.cleaner.plugin.BuildConfig

object LogUtils {
    fun handleThrowable(tr: Throwable) {
        Log.e(BuildConfig.APPLICATION_ID, tr.getStackTraceString)
    }

    fun handleThrowable(msg: Any, tr: Throwable) {
        Log.e(BuildConfig.APPLICATION_ID, msg.toString())
        handleThrowable(tr)
    }

    private val Throwable.getStackTraceString: String
        get() = Log.getStackTraceString(this)

    val getStackTraceString: String
        get() = Log.getStackTraceString(Exception())

    @JvmStatic
    fun e(msg: Any) {
        Log.e(BuildConfig.APPLICATION_ID, msg.toString())
    }

    @JvmStatic
    fun e(tag: Any, msg: Any) {
        Log.e(BuildConfig.APPLICATION_ID, "$tag: $msg")
    }

    @JvmStatic
    fun i(msg: Any) {
        Log.i(BuildConfig.APPLICATION_ID, msg.toString())
    }
}
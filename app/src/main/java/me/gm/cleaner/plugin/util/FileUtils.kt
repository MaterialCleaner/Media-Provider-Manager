package me.gm.cleaner.plugin.util

import android.annotation.SuppressLint
import android.os.Environment
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

object FileUtils {
    fun startsWith(child: String, parent: File): Boolean {
        return startsWith(child, parent.path)
    }

    fun startsWith(child: String, parent: String): Boolean {
        val lowerChild = child.lowercase(Locale.getDefault())
        val lowerParent = parent.lowercase(Locale.getDefault())
        return lowerChild == lowerParent || lowerChild.startsWith(lowerParent + File.separator)
    }

    val androidDir: File
        get() = File(Environment.getExternalStorageDirectory(), "Android")

    val standardDirs: MutableList<File>
        @SuppressLint("SoonBlockedPrivateApi")
        get() {
            val paths = Class.forName("android.os.Environment")
                .getDeclaredField("STANDARD_DIRECTORIES")
                .apply { isAccessible = true }[null] as Array<String>
            return ArrayList<File>().apply {
                paths.forEach {
                    add(Environment.getExternalStoragePublicDirectory(it))
                }
            }
        }
}

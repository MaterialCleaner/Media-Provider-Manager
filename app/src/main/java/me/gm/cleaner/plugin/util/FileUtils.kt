package me.gm.cleaner.plugin.util

import android.annotation.SuppressLint
import android.os.Environment
import java.io.File

object FileUtils {
    fun startsWith(parent: File, child: String): Boolean {
        return startsWith(parent.path, child)
    }

    fun startsWith(parent: String, child: String): Boolean {
        val lowerParent = parent.lowercase()
        val lowerChild = child.lowercase()
        return lowerChild == lowerParent || lowerChild.startsWith(lowerParent + File.separator)
    }

    val androidDir: File = File(Environment.getExternalStorageDirectory(), "Android")

    val standardDirs: List<File>
        @SuppressLint("SoonBlockedPrivateApi")
        get() {
            val paths = Class.forName("android.os.Environment")
                .getDeclaredField("STANDARD_DIRECTORIES")
                .apply { isAccessible = true }[null] as Array<String>
            return paths.map { Environment.getExternalStoragePublicDirectory(it) }
        }
}

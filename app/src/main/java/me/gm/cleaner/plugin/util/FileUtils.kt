/*
 * Copyright 2021 Green Mushroom
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

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

package me.gm.cleaner.plugin.xposed.util

import android.annotation.SuppressLint
import android.os.Environment
import de.robv.android.xposed.XposedHelpers
import java.io.File

object FileUtils {
    fun startsWith(parent: File, child: File) = startsWith(parent.path, child.path)
    fun startsWith(parent: String, child: File) = startsWith(parent, child.path)
    fun startsWith(parent: File, child: String) = startsWith(parent.path, child)
    fun startsWith(parent: String, child: String): Boolean {
        val lowerParent = parent.lowercase()
        val lowerChild = child.lowercase()
        return lowerChild == lowerParent || lowerChild.startsWith(lowerParent + File.separator)
    }

    val externalStorageDirPath = Environment.getExternalStorageDirectory().path
    val androidDir = File(externalStorageDirPath, "Android")
    val standardDirs: List<File>
        @SuppressLint("SoonBlockedPrivateApi")
        get() {
            val paths = XposedHelpers.getStaticObjectField(
                Class.forName("android.os.Environment"), "STANDARD_DIRECTORIES"
            ) as Array<String>
            return paths.map { Environment.getExternalStoragePublicDirectory(it) }
        }
}

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

package me.gm.cleaner.plugin.xposed.hooker

import android.content.Context
import android.os.Environment
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import me.gm.cleaner.plugin.BuildConfig
import me.gm.cleaner.plugin.util.FileUtils

class FileHooker(private val context: Context) : XC_MethodHook() {
    private val niceParents =
        FileUtils.standardDirs.toMutableList().apply { add(FileUtils.androidDir) }
    private val redirectDir = context.getExternalFilesDir(null)!!.path
    private val externalStorageDirectory = Environment.getExternalStorageDirectory().path

    @Throws(Throwable::class)
    override fun beforeHookedMethod(param: MethodHookParam) {
        val path = XposedHelpers.getObjectField(param.thisObject, "path") as String
        // record
        // TODO
        // redirect
        if (niceParents.none { FileUtils.startsWith(it, path) }) {
            val redirect = redirectDir + path.substring(externalStorageDirectory.length)
            XposedHelpers.setObjectField(param.thisObject, "path", redirect)
            if (BuildConfig.DEBUG) {
                XposedBridge.log("redirected a dir: $redirect")
            }
        }
    }
}

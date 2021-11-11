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

import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import me.gm.cleaner.plugin.xposed.ManagerService
import java.io.File

class InsertHooker(private val service: ManagerService) : XC_MethodHook(), MediaProviderHooker {
    @Throws(Throwable::class)
    override fun beforeHookedMethod(param: MethodHookParam) {
        val uri = param.args[0] as Uri
        val initialValues = param.args[1] as? ContentValues
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val extras = param.args[2] as? Bundle
        }

        if (param.matchUri(uri, param.isCallingPackageAllowedHidden) == MEDIA_SCANNER) {
            return
        }

        val path = if (initialValues?.containsKey(MediaStore.MediaColumns.RELATIVE_PATH) == true &&
            initialValues.containsKey(MediaStore.MediaColumns.DISPLAY_NAME)
        ) {
            Environment.getExternalStorageDirectory().path + File.separator +
                    initialValues.getAsString(MediaStore.MediaColumns.RELATIVE_PATH) + File.separator +
                    initialValues.getAsString(MediaStore.MediaColumns.DISPLAY_NAME)
        } else {
            initialValues?.getAsString(MediaStore.MediaColumns.DATA)
        }

        XposedBridge.log("packageName: " + param.callingPackage)
        XposedBridge.log("path: $path")
        XposedBridge.log("MIME_TYPE: ${initialValues?.getAsString(MediaStore.MediaColumns.MIME_TYPE)}")
    }

    companion object {
        private const val MEDIA_SCANNER = 500
    }
}

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

package me.gm.cleaner.plugin.xposed

import android.content.ContentValues
import android.net.Uri
import android.os.Bundle
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

class XposedInit : ManagerService(), IXposedHookLoadPackage {
    @Throws(Throwable::class)
    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        if (lpparam.packageName != "com.android.providers.media.module") return
        classLoader = lpparam.classLoader

        XposedHelpers.findAndHookMethod("com.android.providers.media.MediaProvider", classLoader,
            "insertInternal", Uri::class.java, ContentValues::class.java, Bundle::class.java,
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val contentValues = param.args[1] as ContentValues
                    // contentValues.get("_display_name")
                    XposedBridge.log("_display_name: " + contentValues.get("_display_name"))
                    // contentValues.get("relative_path")
                    XposedBridge.log("relative_path: " + contentValues.get("relative_path"))

                    // "mime_type" = image/png

                    val threadLocal = XposedHelpers.getObjectField(
                        param.thisObject, "mCallingIdentity"
                    ) as ThreadLocal<*>
                    val packageName = XposedHelpers.callMethod(
                        threadLocal.get()!!, "getPackageName"
                    ) as String
                    XposedBridge.log("packageName: " + packageName)

                    if (contentValues.get("_display_name") == "jpush_uid.png") param.result = null
                }
            }
        )
    }
}

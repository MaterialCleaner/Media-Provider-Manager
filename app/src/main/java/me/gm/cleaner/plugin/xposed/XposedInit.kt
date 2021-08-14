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

import android.content.ContentProvider
import android.content.ContentValues
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import de.robv.android.xposed.*
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import me.gm.cleaner.plugin.util.DevUtils
import java.io.File

class XposedInit : ManagerService(), IXposedHookLoadPackage {
    @Throws(Throwable::class)
    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        classLoader = lpparam.classLoader
        DevUtils.init(classLoader)
        when (lpparam.packageName) {
            "com.android.providers.media", "com.android.providers.media.module" -> {
                XposedBridge.hookAllMethods(XposedHelpers.findClass(
                    "com.android.providers.media.MediaProvider", classLoader
                ), "onCreate", object : XC_MethodHook() {
                    @Throws(Throwable::class)
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        context = (param.thisObject as ContentProvider).context!!
                    }
                })

                XposedBridge.hookAllConstructors(File::class.java, object : XC_MethodHook() {
                    @Throws(Throwable::class)
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val path = XposedHelpers.callMethod(param.thisObject, "getPath") as String
//                        XposedBridge.log(path)
                    }
                })

                XposedBridge.hookAllMethods(XposedHelpers.findClass(
                    "com.android.providers.media.MediaProvider", classLoader
                ), "insertInternal", object : XC_MethodHook() {
                    @Throws(Throwable::class)
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val uri = param.args[0] as Uri
                        val contentValues = param.args[1] as ContentValues

//                        contentValues.get("_display_name")
//                        XposedBridge.log("_display_name: " + contentValues.get("_display_name"))
//                        contentValues.get("relative_path")
//                        XposedBridge.log("relative_path: " + contentValues.get("relative_path"))
//
//                        // "mime_type" = image / png
//                        XposedBridge.log("packageName: " + param.thisObject.callingPackage)
                    }
                })

                XposedHelpers.findAndHookMethod("com.android.providers.media.MediaProvider",
                    classLoader, "queryInternal", Uri::class.java, Array<String>::class.java,
                    Bundle::class.java, CancellationSignal::class.java, object : XC_MethodHook() {
                        @Throws(Throwable::class)
                        override fun afterHookedMethod(param: MethodHookParam) {
                            if (param.thisObject.callingPackage == "me.gm.cleaner.plugin") {
                                val c = MatrixCursor(listOf("binder").toTypedArray())
                                c.extras = Bundle().apply {
                                    putBinder(
                                        "me.gm.cleaner.plugin.intent.extra.BINDER", this@XposedInit
                                    )
                                }
                                param.result = c
                                return
                            }

                            if (param.args[2] == null) return
                            val queryArgs = param.args[2] as Bundle

                            XposedBridge.log(
                                param.thisObject.callingPackage + ": " + queryArgs.toString()
                            )
                        }
                    }
                )
            }
        }
    }

    private val Any.callingPackage: String
        get() {
            require(javaClass.name == "com.android.providers.media.MediaProvider")
            val threadLocal =
                XposedHelpers.getObjectField(this, "mCallingIdentity") as ThreadLocal<*>
            return XposedHelpers.callMethod(threadLocal.get()!!, "getPackageName") as String
        }
}

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
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import me.gm.cleaner.plugin.BuildConfig
import me.gm.cleaner.plugin.xposed.hooker.DeleteHooker
import me.gm.cleaner.plugin.xposed.hooker.FileHooker
import me.gm.cleaner.plugin.xposed.hooker.InsertHooker
import me.gm.cleaner.plugin.xposed.hooker.QueryHooker
import me.gm.cleaner.plugin.xposed.util.DevUtils
import java.io.File

class XposedInit : ManagerService(), IXposedHookLoadPackage {
    @Throws(Throwable::class)
    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        classLoader = lpparam.classLoader
        if (BuildConfig.DEBUG) {
            DevUtils.init(classLoader)
        }
        when (lpparam.packageName) {
            "com.android.providers.media", "com.android.providers.media.module" -> {
                val mediaProvider = try {
                    XposedHelpers.findClass(
                        "com.android.providers.media.MediaProvider", classLoader
                    )
                } catch (e: XposedHelpers.ClassNotFoundError) {
                    return
                }

                XposedHelpers.findAndHookMethod(
                    mediaProvider, "onCreate", object : XC_MethodHook() {
                        @Throws(Throwable::class)
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            onCreate((param.thisObject as ContentProvider).context!!)

                            XposedBridge.hookAllMethods(
                                mediaProvider, "queryInternal", QueryHooker(this@XposedInit)
                            )

                            XposedBridge.hookAllMethods(
                                mediaProvider, "insertInternal", InsertHooker(this@XposedInit)
                            )

                            XposedBridge.hookAllMethods(
                                mediaProvider, "deleteInternal", DeleteHooker(this@XposedInit)
                            )
                        }
                    }
                )
            }
            "com.android.providers.downloads" -> {
                XposedHelpers.findAndHookMethod(File::class.java, "mkdir", FileHooker())
            }
        }
    }
}

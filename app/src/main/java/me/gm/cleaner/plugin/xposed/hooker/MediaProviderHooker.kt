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

import android.net.Uri
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

interface MediaProviderHooker {
    val XC_MethodHook.MethodHookParam.callingPackage: String
        get() {
            ensureMediaProvider()
            val threadLocal =
                XposedHelpers.getObjectField(thisObject, "mCallingIdentity") as ThreadLocal<*>
            return XposedHelpers.callMethod(threadLocal.get(), "getPackageName") as String
        }

    val XC_MethodHook.MethodHookParam.isCallingPackageAllowedHidden: Boolean
        get() {
            ensureMediaProvider()
            return XposedHelpers.callMethod(thisObject, "isCallingPackageAllowedHidden") as Boolean
        }

    fun XC_MethodHook.MethodHookParam.matchUri(uri: Uri, allowHidden: Boolean): Int {
        ensureMediaProvider()
        return XposedHelpers.callMethod(thisObject, "matchUri", uri, allowHidden) as Int
    }

    fun XC_MethodHook.MethodHookParam.ensureMediaProvider() {
        require(method.declaringClass.name == "com.android.providers.media.MediaProvider")
    }
}

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

import android.content.ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.util.ArraySet
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import me.gm.cleaner.plugin.BuildConfig
import me.gm.cleaner.plugin.xposed.ManagerService
import java.util.*
import java.util.function.Consumer
import java.util.function.Function

class QueryHooker(private val service: ManagerService) : XC_MethodHook(), MediaProviderHooker {
    @Throws(Throwable::class)
    override fun beforeHookedMethod(param: MethodHookParam) {
        val uri = param.args[0] as Uri
        val projection = param.args[1] as? Array<String>
        val queryArgs = param.args[2] as? Bundle ?: Bundle()
        val signal = param.args[3] as? CancellationSignal

        XposedBridge.log("packageName: " + param.callingPackage)
        XposedBridge.log("uri: $uri")
        XposedBridge.log("projection: ${Arrays.toString(projection)}")

        queryArgs.remove(INCLUDED_DEFAULT_DIRECTORIES)
        val honoredArgs: ArraySet<String> = ArraySet()
        XposedHelpers.callStaticMethod(
            XposedHelpers.findClass(
                "com.android.providers.media.util.DatabaseUtils", service.classLoader
            ), "resolveQueryArgs", queryArgs, object : Consumer<String> {
                override fun accept(t: String) {
                    honoredArgs.add(t)
                }
            }, object : Function<String, String> {
                override fun apply(t: String) =
                    XposedHelpers.callMethod(param.thisObject, "ensureCustomCollator", t) as String
            }
        )

        XposedBridge.log(
            "queryArgs: ${
                Arrays.toString(
                    queryArgs.getStringArray(
                        QUERY_ARG_SQL_SELECTION_ARGS
                    )
                )
            }"
        )
    }

    // for interaction
    @Throws(Throwable::class)
    override fun afterHookedMethod(param: MethodHookParam) {
        if (param.callingPackage == BuildConfig.APPLICATION_ID) {
            val c = param.result as? Cursor ?: MatrixCursor(arrayOf("binder"))
            c.extras = c.extras.apply {
                putBinder("me.gm.cleaner.plugin.intent.extra.BINDER", service)
            }
            param.result = c
        }
    }

    companion object {
        private const val INCLUDED_DEFAULT_DIRECTORIES = "android:included-default-directories"
    }
}

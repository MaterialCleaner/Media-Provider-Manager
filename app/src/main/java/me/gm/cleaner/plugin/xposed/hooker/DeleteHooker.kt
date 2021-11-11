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
import android.os.Build
import android.os.Bundle
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import me.gm.cleaner.plugin.xposed.ManagerService

class DeleteHooker(private val service: ManagerService) : XC_MethodHook(), MediaProviderHooker {
    @Throws(Throwable::class)
    override fun beforeHookedMethod(param: MethodHookParam) {
        val uri = param.args[0] as Uri
        when {
            Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> {
                val userWhere = param.args[1] as? String
                val userWhereArgs = param.args[2] as? Array<String>
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                val extras = param.args[1] as? Bundle
            }
        }

        XposedBridge.log("packageName: " + param.callingPackage)
        XposedBridge.log("delete: $uri")
    }
}

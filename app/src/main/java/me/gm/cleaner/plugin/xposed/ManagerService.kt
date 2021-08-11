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

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import me.gm.cleaner.plugin.BuildConfig
import me.gm.cleaner.plugin.IManagerService
import me.gm.cleaner.plugin.ParceledListSlice
import kotlin.system.exitProcess

class ManagerService constructor(val context: Context) : IManagerService.Stub() {
    override fun getServerVersion(): Int {
        return BuildConfig.VERSION_CODE
    }

    @SuppressLint("QueryPermissionsNeeded")
    override fun getInstalledPackages(): ParceledListSlice<PackageInfo> {
        // TODO: use system api
        return ParceledListSlice(context.packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS))
    }

    // FIXME
    @SuppressLint("SoonBlockedPrivateApi")
    override fun notifyPreferencesChanged() {
        try {
            val context: Context = context.createDeviceProtectedStorageContext()
            context.javaClass.getDeclaredMethod("reloadSharedPreferences").invoke(context)
        } catch (tr: Throwable) {
            tr.printStackTrace()
            exitProcess(1)
        }
    }
}

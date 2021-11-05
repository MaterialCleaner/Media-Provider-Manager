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
import android.os.IBinder
import android.os.IInterface
import de.robv.android.xposed.XposedHelpers
import me.gm.cleaner.plugin.BuildConfig
import me.gm.cleaner.plugin.IManagerService
import me.gm.cleaner.plugin.model.ParceledListSlice
import kotlin.system.exitProcess

abstract class ManagerService : IManagerService.Stub() {
    lateinit var classLoader: ClassLoader
    lateinit var context: Context

    val packageManager: IInterface by lazy {
        val binder = XposedHelpers.callStaticMethod(
            XposedHelpers.findClass("android.os.ServiceManager", classLoader),
            "getService", "package"
        ) as IBinder
        XposedHelpers.callStaticMethod(
            XposedHelpers.findClass(
                "android.content.pm.IPackageManager\$Stub", classLoader
            ), "asInterface", binder
        ) as IInterface
    }

    override fun getModuleVersion(): Int = BuildConfig.VERSION_CODE

    override fun getInstalledPackages(userId: Int): ParceledListSlice<PackageInfo> {
        val parceledListSlice = XposedHelpers.callMethod(
            packageManager, "getInstalledPackages", PackageManager.GET_PERMISSIONS, userId
        )
        val list = XposedHelpers.callMethod(parceledListSlice, "getList") as List<PackageInfo>
        return ParceledListSlice(list)
    }

    // FIXME
    @SuppressLint("SoonBlockedPrivateApi")
    override fun notifyPreferencesChanged() {
        try {
            context.javaClass.getDeclaredMethod("reloadSharedPreferences").invoke(context)
        } catch (tr: Throwable) {
            tr.printStackTrace()
            exitProcess(1)
        }
    }
}

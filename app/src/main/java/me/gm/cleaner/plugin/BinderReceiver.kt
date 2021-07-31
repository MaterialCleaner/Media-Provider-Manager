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

package me.gm.cleaner.plugin

import android.content.pm.PackageInfo
import android.os.IBinder
import android.os.RemoteException
import androidx.lifecycle.MutableLiveData
import me.gm.cleaner.plugin.settings.PreferencesPackageInfo
import java.util.*
import java.util.stream.Collectors

object BinderReceiver {
    val MODULE_VER = MutableLiveData(-1)
    private val binder: IBinder? = null
    private var service: IManagerService? = null

    fun pingBinder(): Boolean {
        val ping = binder != null && binder.pingBinder()
        if (!ping) MODULE_VER.postValue(-1)
        return ping
    }

    private fun getBinder(): IBinder {
        checkNotNull(binder) { "module not active" }
        if (service == null) {
            synchronized(BinderReceiver::class.java) {
                service = IManagerService.Stub.asInterface(
                    binder
                )
            }
        }
        return binder
    }

    val serverVersion: Int
        get() {
            val value = MODULE_VER.value
            return value ?: -1
        }

    val installedPackages: List<PackageInfo>
        get() = try {
            service!!.installedPackages.list
        } catch (e: RemoteException) {
            e.printStackTrace()
            ArrayList()
        }

    val installedPackagesForServicePreferences: List<PreferencesPackageInfo>
        get() {
            val installedPackages = installedPackages.stream()
                .map { pi: PackageInfo? ->
                    PreferencesPackageInfo.newInstance(
                        pi!!
                    )
                }.collect(Collectors.toList())
            installedPackages.removeIf { packageInfo: PreferencesPackageInfo -> !packageInfo.applicationInfo.enabled }
            return installedPackages
        }
}

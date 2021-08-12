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
import android.os.Binder
import android.os.IBinder
import android.os.RemoteException
import androidx.annotation.Keep
import androidx.lifecycle.MutableLiveData
import java.util.*

object BinderReceiver {
    val MODULE_VER = MutableLiveData(-1)
    private var binder: IBinder? = null
    private var service: IManagerService? = null
    private val DEATH_RECIPIENT = IBinder.DeathRecipient {
        binder = null
        service = null
    }

    fun pingBinder(): Boolean {
        val ping = binder != null && binder!!.pingBinder()
        if (!ping) MODULE_VER.postValue(-1)
        return ping
    }

    @Keep
    @JvmStatic
    private fun onBinderReceived(newBinder: Binder) {
        if (binder == newBinder) return
        binder?.unlinkToDeath(DEATH_RECIPIENT, 0)
        binder = newBinder
        service = IManagerService.Stub.asInterface(newBinder)
        binder?.linkToDeath(DEATH_RECIPIENT, 0)
        MODULE_VER.postValue(service!!.serverVersion)
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

    fun notifyPreferencesChanged() {
        try {
            service!!.notifyPreferencesChanged()
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }
}
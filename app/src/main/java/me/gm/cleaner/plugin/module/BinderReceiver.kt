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

package me.gm.cleaner.plugin.module

import android.content.pm.PackageInfo
import android.os.IBinder
import android.os.Process
import android.os.RemoteException
import kotlinx.coroutines.flow.MutableStateFlow
import me.gm.cleaner.plugin.IManagerService

object BinderReceiver {
    private val _moduleVersionFlow = MutableStateFlow(-1)
    var moduleVersion: Int
        get() = _moduleVersionFlow.value
        set(value) {
            _moduleVersionFlow.tryEmit(value)
        }

    private var binder: IBinder? = null
    private var service: IManagerService? = null
    private val DEATH_RECIPIENT = IBinder.DeathRecipient {
        binder = null
        service = null
        moduleVersion = -1
    }

    fun pingBinder() = binder != null && binder!!.pingBinder()

    fun onBinderReceived(newBinder: IBinder) {
        if (binder == newBinder) return
        binder?.unlinkToDeath(DEATH_RECIPIENT, 0)
        binder = newBinder
        service = IManagerService.Stub.asInterface(newBinder)
        binder?.linkToDeath(DEATH_RECIPIENT, 0)
        moduleVersion = service!!.moduleVersion
    }

    val installedPackages: List<PackageInfo>
        get() = try {
            service!!.getInstalledPackages(Process.myUid() / 100000).list
        } catch (e: RemoteException) {
            e.printStackTrace()
            emptyList()
        }

    fun notifyPreferencesChanged() {
        service!!.notifyPreferencesChanged()
    }
}

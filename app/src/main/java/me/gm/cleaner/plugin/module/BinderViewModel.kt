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
import android.util.SparseArray
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import me.gm.cleaner.plugin.IManagerService
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.model.Template
import javax.inject.Inject

@HiltViewModel
class BinderViewModel @Inject constructor(private val binder: IBinder?) : ViewModel() {
    private var service: IManagerService? = IManagerService.Stub.asInterface(binder)
    private val _remoteSpCacheLiveData = MutableLiveData(SparseArray<String>())
    val remoteSpCacheLiveData: LiveData<SparseArray<String>>
        get() = _remoteSpCacheLiveData
    val remoteSpCache: SparseArray<String>
        get() = _remoteSpCacheLiveData.value!!

    fun notifyRemoteSpChanged() {
        _remoteSpCacheLiveData.postValue(remoteSpCache)
    }

    fun pingBinder() = binder != null && binder.pingBinder()

    val moduleVersion: Int
        get() = service!!.moduleVersion

    val installedPackages: List<PackageInfo>
        get() = try {
            service!!.getInstalledPackages(Process.myUid() / AID_USER_OFFSET).list
        } catch (e: RemoteException) {
            e.printStackTrace()
            emptyList()
        }

    fun getPackageInfo(packageName: String): PackageInfo? =
        service!!.getPackageInfo(packageName, 0, Process.myUid() / AID_USER_OFFSET)

    fun readSp(who: Int): String? =
        remoteSpCache[who, service!!.readSp(who).also { remoteSpCache.put(who, it) }]

    fun readTemplates(): Array<Template> {
        val sp = readSp(R.xml.template_preferences)
        return if (sp.isNullOrEmpty()) {
            emptyArray()
        } else {
            Gson().fromJson(sp, Array<Template>::class.java)
        }
    }

    fun writeSp(who: Int, what: String) {
        if (remoteSpCache[who] != what) {
            service!!.writeSp(who, what)
            remoteSpCache.put(who, what)
            notifyRemoteSpChanged()
        }
    }

    fun clearAllTables() {
        service!!.clearAllTables()
    }

    fun packageUsageTimes(table: String, packageNames: List<String>): Int =
        service!!.packageUsageTimes(table, packageNames)

    companion object {
        const val AID_USER_OFFSET = 100000
    }
}

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

package me.gm.cleaner.plugin.ui.module

import android.content.Context
import android.content.pm.PackageInfo
import android.os.IBinder
import android.os.Process
import android.util.SparseArray
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import me.gm.cleaner.plugin.IManagerService
import me.gm.cleaner.plugin.IMediaChangeObserver
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.provider.xSpUtils
import javax.inject.Inject

@HiltViewModel
class BinderViewModel @Inject constructor(private val binder: IBinder?) : ViewModel() {
    public var service: IManagerService? = IManagerService.Stub.asInterface(binder)
    private val _remoteSpCacheLiveData = MutableLiveData(SparseArray<String>())
    val remoteSpCacheLiveData: LiveData<SparseArray<String>>
        get() = _remoteSpCacheLiveData
    val remoteSpCache: SparseArray<String>
        get() = _remoteSpCacheLiveData.value!!

    fun notifyRemoteSpChanged() {
        _remoteSpCacheLiveData.postValue(remoteSpCache)
    }

    fun pingBinder() = binder?.pingBinder() == true

    val moduleVersion: Int
        get() = service!!.moduleVersion

    fun getInstalledPackages(flags: Int): List<PackageInfo> =
        service!!.getInstalledPackages(Process.myUid() / AID_USER_OFFSET, flags).list

    fun getPackageInfo(packageName: String): PackageInfo? =
        service!!.getPackageInfo(packageName, 0, Process.myUid() / AID_USER_OFFSET)

    fun readSp(who: Int): String? =
        remoteSpCache[who, service!!.readSp(who).also { remoteSpCache.put(who, it) }]

    fun writeSp(ctx: Context, who: Int, what: String) {
        if (remoteSpCache[who] != what) {
            writeSpInApp(ctx, who, what);
            remoteSpCache.put(who, what)
            notifyRemoteSpChanged()
            service!!.refreshSp();
        }
    }

    fun refreshSp() {
        service!!.refreshSp();
    }

    fun writeSpInApp(ctx: Context, who: Int, what: String) {
        var r = xSpUtils.getxSp(ctx).edit();
        when (who) {
            R.xml.root_preferences -> r.putString("root", what)
            R.xml.template_preferences -> r.putString("rule", what)
        }
        r.commit();
    }

    fun clearAllTables() {
        service!!.clearAllTables()
    }

    fun packageUsageTimes(operation: Int, packageNames: List<String>): Int =
        service!!.packageUsageTimes(operation, packageNames)

    fun registerMediaChangeObserver(observer: IMediaChangeObserver) {
        service!!.registerMediaChangeObserver(observer)
    }

    fun unregisterMediaChangeObserver(observer: IMediaChangeObserver) {
        service!!.unregisterMediaChangeObserver(observer)
    }

    companion object {
        const val AID_USER_OFFSET = 100000
    }
}

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

package me.gm.cleaner.plugin.settings

import android.content.pm.PackageManager
import androidx.lifecycle.LiveData
import me.gm.cleaner.plugin.BinderReceiver
import me.gm.cleaner.plugin.util.PreferencesPackageInfo
import me.gm.cleaner.plugin.util.PreferencesPackageInfo.Companion.copy
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Collectors

class AppListLiveData : LiveData<List<PreferencesPackageInfo>>() {
    init {
        value = mutableListOf()
    }

    fun load(pm: PackageManager, l: ProgressListener?) {
        val installedPackages = BinderReceiver.installedPackages.filter {
            it.applicationInfo.enabled
        }
        val size = installedPackages.size
        val count = AtomicInteger(0)
        postValue(installedPackages.stream()
            .map {
                l?.onProgress(100 * count.incrementAndGet() / size)
                PreferencesPackageInfo.newInstance(it, pm)
            }
            .collect(Collectors.toList())
            .apply { l?.onProgress(0) }
        )
    }

    fun refreshPreferencesCount() {
        val list = ArrayList<PreferencesPackageInfo>()
        value?.forEach {
            list.add(it.copy())
        }
        postValue(list)
    }

    interface ProgressListener {
        fun onProgress(progress: Int)
    }
}

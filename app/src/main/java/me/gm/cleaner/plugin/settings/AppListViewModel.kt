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

import android.Manifest
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import me.gm.cleaner.plugin.BinderReceiver
import me.gm.cleaner.plugin.dao.ModulePreferences
import me.gm.cleaner.plugin.util.PreferencesPackageInfo
import me.gm.cleaner.plugin.util.PreferencesPackageInfo.Companion.copy
import java.text.Collator
import java.util.concurrent.atomic.AtomicInteger

class AppListViewModel : ViewModel() {
    private val _apps = MutableStateFlow<SourceState>(SourceState.Load(0))
    val apps: StateFlow<SourceState> = _apps
    suspend fun loadApps(pm: PackageManager) {
        val installedPackages = BinderReceiver.installedPackages.filter {
            it.applicationInfo.enabled
        }
        val size = installedPackages.size
        val count = AtomicInteger(0)
        _apps.emit(SourceState.Success(
            installedPackages
                .map {
                    _apps.emit(SourceState.Load(100 * count.incrementAndGet() / size))
                    PreferencesPackageInfo.newInstance(it, pm)
                }
                .apply { _apps.emit(SourceState.Load(0)) }
        ))
    }

    suspend fun updateApps() {
        val list = mutableListOf<PreferencesPackageInfo>()
        if (_apps.value is SourceState.Success) {
            (_apps.value as SourceState.Success).source.forEach {
                list.add(it.copy())
            }
        }
        _apps.emit(SourceState.Success(list))
    }

    private val _isSearching = MutableStateFlow(false)
    var isSearching: Boolean
        get() = _isSearching.value
        set(value) {
            if (isSearching == value) {
                return
            }
            _isSearching.value = value
        }
    private val _queryText = MutableStateFlow("")
    var queryText: String
        get() = _queryText.value
        set(value) {
            if (queryText == value) {
                return
            }
            _queryText.value = value
        }
    val showingList = combine(apps, _isSearching, _queryText) { apps, isSearching, queryText ->
        if (apps is SourceState.Load) {
            return@combine
        }
        (apps as SourceState.Success).source.toMutableList().apply {
            if (ModulePreferences.isHideSystemApp) {
                removeIf {
                    it.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
                }
            }
            if (ModulePreferences.isHideNoStoragePermissionApp) {
                removeIf {
                    val requestedPermissions = it.requestedPermissions
                    requestedPermissions == null || !requestedPermissions.run {
                        contains(Manifest.permission.READ_EXTERNAL_STORAGE)
                                || contains(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                || Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                                && contains(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
                    }
                }
            }
            when (ModulePreferences.sortBy) {
                ModulePreferences.SORT_BY_NAME ->
                    sortWith { o1: PreferencesPackageInfo?, o2: PreferencesPackageInfo? ->
                        Collator.getInstance().compare(o1?.label, o2?.label)
                    }
                ModulePreferences.SORT_BY_UPDATE_TIME ->
                    sortWith(Comparator.comparingLong {
                        -it.lastUpdateTime
                    })
            }
            if (ModulePreferences.ruleCount) {
//                    sortWith { o1: PreferencesPackageInfo?, o2: PreferencesPackageInfo? ->
//                        when (mTitle) {
//                            R.string.storage_redirect_title -> return@sortWith o2!!.srCount - o1!!.srCount
//                            R.string.foreground_activity_observer_title -> return@sortWith o2!!.faInfo.size - o1!!.faInfo.size
//                            else -> return@sortWith 0
//                        }
//                    }
            }
            if (isSearching) {
                val lowerQuery = queryText.lowercase()
                removeIf {
                    !it.label.lowercase().contains(lowerQuery) &&
                            !it.applicationInfo.packageName.lowercase().contains(lowerQuery)
                }
            }
        }
    }
}

sealed class SourceState {
    data class Load(var progress: Int) : SourceState()
    data class Success(val source: List<PreferencesPackageInfo>) : SourceState()
}

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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import me.gm.cleaner.plugin.dao.ModulePreferences
import me.gm.cleaner.plugin.util.PreferencesPackageInfo
import java.text.Collator

class AppListViewModel : ViewModel() {
    private val searchState = MutableLiveData(Pair(false, ""))
    var isSearching: Boolean
        get() = searchState.value!!.first
        set(value) {
            if (isSearching == value) {
                return
            }
            searchState.postValue(Pair(value, queryText))
        }
    var queryText: String
        get() = searchState.value!!.second
        set(value) {
            if (queryText == value) {
                return
            }
            searchState.postValue(Pair(isSearching, value))
        }
    val installedPackages = AppListLiveData()
    private val _showingList = SearchableAppListLiveData(installedPackages, searchState)
    val showingList: LiveData<List<PreferencesPackageInfo>>
        get() = _showingList

    private class SearchableAppListLiveData(
        private val source: AppListLiveData,
        private val searchState: LiveData<Pair<Boolean, String>>
    ) : MediatorLiveData<List<PreferencesPackageInfo>>() {
        init {
            addSource(source) { updateSource() }
            addSource(searchState) { updateSource() }
        }

        fun updateSource() {
            val list = source.value!!.toMutableList().apply {
                if (searchState.value!!.first) {
                    if (ModulePreferences.isHideSystemApp) {
                        removeIf {
                            it.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
                        }
                    }
                    if (ModulePreferences.isHideNoStoragePermissionApp) {
                        removeIf {
                            val requestedPermissions = it.requestedPermissions
                            requestedPermissions == null || !listOf(*requestedPermissions)
                                .contains(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                    }
                    when (ModulePreferences.sortBy) {
                        ModulePreferences.SORT_BY_NAME ->
                            sortWith { o1: PreferencesPackageInfo?, o2: PreferencesPackageInfo? ->
                                Collator.getInstance().compare(o1?.label, o2?.label)
                            }
                        ModulePreferences.SORT_BY_UPDATE_TIME ->
                            sortWith(Comparator.comparingLong {
                                -it!!.lastUpdateTime
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
                } else {
                    val lowerQuery = searchState.value!!.second.lowercase()
                    removeIf {
                        !it.label.lowercase().contains(lowerQuery)
                                && !it.applicationInfo.packageName.lowercase()
                            .contains(lowerQuery)
                    }
                }
            }
            postValue(list)
        }
    }
}

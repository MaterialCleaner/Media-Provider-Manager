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
import android.os.Build
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.gm.cleaner.plugin.dao.ModulePreferences
import me.gm.cleaner.plugin.util.PreferencesPackageInfo
import java.text.Collator

class AppListViewModel : ViewModel() {
    private val state = MutableLiveData(false to "")
    var isSearching: Boolean
        get() = state.value!!.first
        set(value) {
            if (isSearching == value) {
                return
            }
            state.value = value to queryText
        }
    var queryText: String
        get() = state.value!!.second
        set(value) {
            if (queryText == value) {
                return
            }
            state.value = isSearching to value
        }
    val installedPackages = AppListLiveData()
    private val _showingList = SearchableAppListLiveData(installedPackages, state)
    val showingList: LiveData<List<PreferencesPackageInfo>>
        get() = _showingList

    inner class SearchableAppListLiveData(
        private val source: AppListLiveData, state: LiveData<Pair<Boolean, String>>
    ) : MediatorLiveData<List<PreferencesPackageInfo>>() {
        init {
            addSource(source) { updateSource() }
            addSource(state) { updateSource() }
        }

        private fun updateSource() {
            viewModelScope.launch(Dispatchers.Default) {
                val list = source.value!!.toMutableList().apply {
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
                postValue(list)
            }
        }
    }
}

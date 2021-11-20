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

import android.Manifest
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import me.gm.cleaner.plugin.dao.ModulePreferences
import java.text.Collator

class AppListViewModel : ViewModel() {
    private val _isSearchingFlow = MutableStateFlow(false)
    var isSearching: Boolean
        get() = _isSearchingFlow.value
        set(value) {
            _isSearchingFlow.value = value
        }
    private val _queryTextFlow = MutableStateFlow("")
    var queryText: String
        get() = _queryTextFlow.value
        set(value) {
            _queryTextFlow.value = value
        }
    private val collator = Collator.getInstance()
    private val _appsFlow = MutableStateFlow<SourceState>(SourceState.Loading(0))
    val appsFlow =
        combine(_appsFlow, _isSearchingFlow, _queryTextFlow) { source, isSearching, queryText ->
            when (source) {
                is SourceState.Loading -> SourceState.Loading(source.progress)
                is SourceState.Done -> {
                    var sequence = source.list.asSequence()
                    if (ModulePreferences.isHideSystemApp) {
                        sequence = sequence.filter {
                            it.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0
                        }
                    }
                    if (ModulePreferences.isHideNoStoragePermissionApp) {
                        sequence = sequence.filter {
                            it.requestedPermissions?.run {
                                contains(Manifest.permission.READ_EXTERNAL_STORAGE)
                                        || contains(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                        || Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                                        && contains(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
                            } == true
                        }
                    }
                    sequence = when (ModulePreferences.sortBy) {
                        ModulePreferences.SORT_BY_NAME -> sequence.sortedWith { o1, o2 ->
                            collator.compare(o1?.label, o2?.label)
                        }
                        ModulePreferences.SORT_BY_UPDATE_TIME -> sequence.sortedWith(Comparator.comparingLong {
                            -it.lastUpdateTime
                        })
                        else -> throw IllegalArgumentException()
                    }
                    if (ModulePreferences.ruleCount) {
//                    sequence = sortWith { o1, o2 ->
//                        when (mTitle) {
//                            R.string.storage_redirect_title -> return@sortWith o2!!.srCount - o1!!.srCount
//                            R.string.foreground_activity_observer_title -> return@sortWith o2!!.faInfo.size - o1!!.faInfo.size
//                            else -> return@sortWith 0
//                        }
//                    }
                    }
                    if (isSearching) {
                        val lowerQuery = queryText.lowercase()
                        sequence = sequence.filter {
                            it.label.lowercase().contains(lowerQuery) ||
                                    it.applicationInfo.packageName.lowercase().contains(lowerQuery)
                        }
                    }
                    SourceState.Done(sequence.toList())
                }
            }
        }

    fun loadApps(
        binderViewModel: BinderViewModel, pm: PackageManager,
        l: AppListLoader.ProgressListener? = object : AppListLoader.ProgressListener {
            override fun onProgress(progress: Int) {
                _appsFlow.value = SourceState.Loading(progress)
            }
        }
    ) {
        viewModelScope.launch {
            val list = AppListLoader().load(binderViewModel, pm, l)
            _appsFlow.value = SourceState.Done(list)
        }
    }

    fun updateApps() {
        viewModelScope.launch {
            if (_appsFlow.value is SourceState.Done) {
                val list = AppListLoader().update((_appsFlow.value as SourceState.Done).list)
                _appsFlow.value = SourceState.Done(list)
            }
        }
    }
}

sealed class SourceState {
    data class Loading(val progress: Int) : SourceState()
    data class Done(val list: List<PreferencesPackageInfo>) : SourceState()
}

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

package me.gm.cleaner.plugin.ui.module.appmanagement

import android.app.Application
import android.content.pm.ApplicationInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import me.gm.cleaner.plugin.dao.RootPreferences
import me.gm.cleaner.plugin.ui.module.BinderViewModel
import me.gm.cleaner.plugin.util.collatorComparator

class AppListViewModel(application: Application) : AndroidViewModel(application) {
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
    val isLoading: Boolean
        get() = _appsFlow.value is AppListState.Loading
    private val _appsFlow = MutableStateFlow<AppListState>(AppListState.Loading(0))
    val appsFlow =
        combine(_appsFlow, _isSearchingFlow, _queryTextFlow) { source, isSearching, queryText ->
            when (source) {
                is AppListState.Loading -> AppListState.Loading(source.progress)
                is AppListState.Done -> {
                    var sequence = source.list.asSequence()
                    if (RootPreferences.isHideSystemApp) {
                        sequence = sequence.filter {
                            it.packageInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0
                        }
                    }
                    if (isSearching) {
                        sequence = sequence.filter {
                            it.label.contains(queryText, true) ||
                                    it.packageInfo.packageName.contains(queryText, true)
                        }
                    }
                    sequence = when (RootPreferences.sortBy) {
                        RootPreferences.SORT_BY_APP_NAME -> {
                            sequence.sortedWith(collatorComparator { it.label })
                        }

                        RootPreferences.SORT_BY_UPDATE_TIME -> sequence.sortedBy {
                            -it.packageInfo.lastUpdateTime
                        }

                        else -> throw IllegalArgumentException()
                    }
                    if (RootPreferences.ruleCount) {
                        sequence = sequence.sortedBy { -it.ruleCount }
                    }
                    AppListState.Done(sequence.toList())
                }
            }
        }

    fun loadApps(
        binderViewModel: BinderViewModel,
        l: AppListLoader.ProgressListener? = object : AppListLoader.ProgressListener {
            override fun onProgress(progress: Int) {
                _appsFlow.value = AppListState.Loading(progress)
            }
        }
    ) {
        viewModelScope.launch {
            _appsFlow.value = AppListState.Loading(0)
            val list = AppListLoader().load(
                binderViewModel, getApplication<Application>().packageManager, l
            )
            _appsFlow.value = AppListState.Done(list)
        }
    }

    fun updateApps(binderViewModel: BinderViewModel) {
        viewModelScope.launch {
            if (!isLoading) {
                val list = AppListLoader().update(
                    (_appsFlow.value as AppListState.Done).list, binderViewModel
                )
                _appsFlow.value = AppListState.Loading(0)
                _appsFlow.value = AppListState.Done(list)
            }
        }
    }
}

sealed class AppListState {
    data class Loading(val progress: Int) : AppListState()
    data class Done(val list: List<AppListModel>) : AppListState()
}

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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import me.gm.cleaner.plugin.dao.RootPreferences
import me.gm.cleaner.plugin.dao.RootPreferences.SORT_BY_APP_NAME
import me.gm.cleaner.plugin.dao.RootPreferences.SORT_BY_UPDATE_TIME
import me.gm.cleaner.plugin.ktx.getValue
import me.gm.cleaner.plugin.ktx.setValue
import me.gm.cleaner.plugin.ui.module.BinderViewModel
import me.gm.cleaner.plugin.util.collatorComparator

class AppListViewModel(
    application: Application,
    private val binderViewModel: BinderViewModel
) : AndroidViewModel(application) {
    private val _isSearchingFlow = MutableStateFlow(false)
    var isSearching: Boolean by _isSearchingFlow
    private val _queryTextFlow = MutableStateFlow("")
    var queryText: String by _queryTextFlow
    val isLoading: Boolean
        get() = _appsFlow.value is AppListState.Loading
    private val _appsFlow = MutableStateFlow<AppListState>(AppListState.Loading(0))
    val appsFlow =
        combine(
            _appsFlow,
            _isSearchingFlow,
            _queryTextFlow,
            RootPreferences.isHideSystemApp.asFlow(),
            RootPreferences.sortBy.asFlow(),
            RootPreferences.ruleCount.asFlow(),
        ) { apps, isSearching, queryText, isHideSystemApp, sortBy, ruleCount ->
            when (apps) {
                is AppListState.Loading -> AppListState.Loading(apps.progress)
                is AppListState.Done -> {
                    var sequence = apps.list.asSequence()
                    if (isHideSystemApp) {
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
                    sequence = when (sortBy) {
                        SORT_BY_APP_NAME -> {
                            sequence.sortedWith(collatorComparator { it.label })
                        }

                        SORT_BY_UPDATE_TIME -> sequence.sortedBy {
                            -it.packageInfo.lastUpdateTime
                        }

                        else -> throw IllegalArgumentException()
                    }
                    if (ruleCount) {
                        sequence = sequence.sortedBy { -it.ruleCount }
                    }
                    AppListState.Done(sequence.toList())
                }
            }
        }

    fun load(
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

    fun update() {
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

    init {
        load()
    }

    companion object {
        fun provideFactory(
            application: Application, binderViewModel: BinderViewModel
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AppListViewModel(application, binderViewModel) as T
            }
        }
    }
}

sealed class AppListState {
    data class Loading(val progress: Int) : AppListState()
    data class Done(val list: List<AppListModel>) : AppListState()
}

fun <T1, T2, T3, T4, T5, T6, R> combine(
    flow: Flow<T1>, flow2: Flow<T2>, flow3: Flow<T3>, flow4: Flow<T4>, flow5: Flow<T5>,
    flow6: Flow<T6>, transform: suspend (T1, T2, T3, T4, T5, T6) -> R
): Flow<R> = combine(flow, flow2, flow3, flow4, flow5, flow6) { args: Array<*> ->
    transform(
        args[0] as T1, args[1] as T2, args[2] as T3, args[3] as T4, args[4] as T5, args[5] as T6
    )
}

fun <T1, T2, T3, T4, T5, T6, T7, R> combine(
    flow: Flow<T1>, flow2: Flow<T2>, flow3: Flow<T3>, flow4: Flow<T4>, flow5: Flow<T5>,
    flow6: Flow<T6>, flow7: Flow<T7>, transform: suspend (T1, T2, T3, T4, T5, T6, T7) -> R
): Flow<R> = combine(flow, flow2, flow3, flow4, flow5, flow6, flow7) { args: Array<*> ->
    transform(
        args[0] as T1, args[1] as T2, args[2] as T3, args[3] as T4, args[4] as T5, args[5] as T6,
        args[6] as T7
    )
}

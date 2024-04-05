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

package me.gm.cleaner.plugin.ui.module.usagerecord

import android.app.Application
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.gm.cleaner.plugin.dao.MediaProviderOperation.Companion.OP_DELETE
import me.gm.cleaner.plugin.dao.MediaProviderOperation.Companion.OP_INSERT
import me.gm.cleaner.plugin.dao.MediaProviderOperation.Companion.OP_QUERY
import me.gm.cleaner.plugin.dao.MediaProviderRecord
import me.gm.cleaner.plugin.dao.RootPreferences
import me.gm.cleaner.plugin.ui.module.BinderViewModel
import java.util.Calendar

class UsageRecordViewModel(
    application: Application,
    private val binderViewModel: BinderViewModel,
) : AndroidViewModel(application) {
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
    val calendar: Calendar = Calendar.getInstance()

    private val _recordsFlow = MutableStateFlow<SourceState>(SourceState.Loading)
    val recordsFlow =
        combine(_recordsFlow, _isSearchingFlow, _queryTextFlow) { source, isSearching, queryText ->
            when (source) {
                is SourceState.Loading -> SourceState.Loading
                is SourceState.Done -> withContext(Dispatchers.Default) {
                    var sequence = source.list.asSequence()
                    if (isSearching) {
                        sequence = sequence.filter {
                            it.data.any { data -> data.contains(queryText, true) } ||
                                    it.label?.contains(queryText, true) == true ||
                                    it.packageName.contains(queryText, true)
                        }
                    }
                    SourceState.Done(sequence.toList())
                }
            }
        }

    fun reloadRecords(): Job = loadRecords(calendar.timeInMillis)

    /**
     * Find the start and the end time millis of a day.
     * @param timeMillis any time millis in that day
     */
    fun loadRecords(timeMillis: Long): Job = calendar.run {
        timeInMillis = timeMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        val start = timeInMillis
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
        val end = timeInMillis
        loadRecords(
            start, end,
            RootPreferences.isHideQuery,
            RootPreferences.isHideInsert,
            RootPreferences.isHideDelete
        )
    }

    fun loadRecords(
        start: Long, end: Long,
        isHideQuery: Boolean, isHideInsert: Boolean, isHideDelete: Boolean
    ): Job = viewModelScope.launch {
        _recordsFlow.value = SourceState.Loading
        val packageManager = getApplication<Application>().packageManager
        val operations = mutableListOf<Int>()
        if (!isHideQuery) {
            operations += OP_QUERY
        }
        if (!isHideInsert) {
            operations += OP_INSERT
        }
        if (!isHideDelete) {
            operations += OP_DELETE
        }
        val records = mutableListOf<MediaProviderRecord>().also {
            it += queryRecord(start, end, operations)
        }.onEach {
            val pi = binderViewModel.getPackageInfo(it.packageName) ?: return@onEach
            it.packageInfo = pi
            it.label = packageManager.getApplicationLabel(pi.applicationInfo).toString()
        }.takeWhile {
            it.packageInfo != null
        }
        _recordsFlow.value = SourceState.Done(records)
    }

    private suspend inline fun queryRecord(
        start: Long, end: Long, operations: List<Int>
    ): List<MediaProviderRecord> = withContext(Dispatchers.IO) {
        val projection = operations.map { it.toString() }.toTypedArray()
        val selection = start.toString()
        val sortOrder = end.toString()

        getApplication<Application>().contentResolver.query(
            MediaStore.Images.Media.INTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            return@withContext MediaProviderRecord.convert(cursor)
        }
        return@withContext emptyList()
    }

    init {
        reloadRecords()
    }

    companion object {
        fun provideFactory(
            application: Application, binderViewModel: BinderViewModel
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return UsageRecordViewModel(application, binderViewModel) as T
            }
        }
    }
}

sealed class SourceState {
    data object Loading : SourceState()
    data class Done(val list: List<MediaProviderRecord>) : SourceState()
}

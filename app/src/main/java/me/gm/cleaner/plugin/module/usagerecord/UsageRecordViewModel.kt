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

package me.gm.cleaner.plugin.module.usagerecord

import android.app.Application
import android.database.ContentObserver
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.gm.cleaner.plugin.dao.ModulePreferences
import me.gm.cleaner.plugin.dao.mediaprovider.MediaProviderDeleteRecord
import me.gm.cleaner.plugin.dao.mediaprovider.MediaProviderInsertRecord
import me.gm.cleaner.plugin.dao.mediaprovider.MediaProviderQueryRecord
import me.gm.cleaner.plugin.dao.mediaprovider.MediaProviderRecord
import me.gm.cleaner.plugin.module.BinderViewModel
import java.util.*

class UsageRecordViewModel(application: Application) : AndroidViewModel(application) {
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
    private val calendar = Calendar.getInstance()
    private val packageNameToLabel = mutableMapOf<String, String>()

    private val _recordsFlow = MutableStateFlow<List<MediaProviderRecord>>(emptyList())
    val recordsFlow =
        combine(_recordsFlow, _isSearchingFlow, _queryTextFlow) { source, isSearching, queryText ->
            var sequence = source.asSequence()
            sequence = sequence.sortedWith { o1, o2 ->
                (o2.timeMillis - o1.timeMillis).toInt()
            }
            if (isSearching) {
                val lowerQuery = queryText.lowercase()
                sequence = sequence.filter {
                    it.dataList.any { data -> data.lowercase().contains(lowerQuery) } ||
                            it.label!!.lowercase().contains(lowerQuery) ||
                            it.packageName.lowercase().contains(lowerQuery)
                }
            }
            sequence.toList()
        }
    val records: List<MediaProviderRecord>
        get() = _recordsFlow.value

    private var contentObserver: ContentObserver? = null

    /**
     * Find the start and the end time millis of a day.
     * @param timeMillis any time millis in that day
     */
    fun loadRecords(binderViewModel: BinderViewModel, timeMillis: Long) {
        calendar.run {
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
                binderViewModel, start, end,
                ModulePreferences.isHideQuery,
                ModulePreferences.isHideInsert,
                ModulePreferences.isHideDelete
            )
        }
    }

    fun reloadRecords(binderViewModel: BinderViewModel) {
        loadRecords(binderViewModel, calendar.timeInMillis)
    }

    fun loadRecords(
        binderViewModel: BinderViewModel, start: Long, end: Long,
        isHideQuery: Boolean, isHideInsert: Boolean, isHideDelete: Boolean
    ) {
        viewModelScope.launch {
            val records = mutableListOf<MediaProviderRecord>().also {
                if (!isHideQuery) {
                    it += queryRecord<MediaProviderQueryRecord>(start, end)
                }
                if (!isHideInsert) {
                    it += queryRecord<MediaProviderInsertRecord>(start, end)
                }
                if (!isHideDelete) {
                    it += queryRecord<MediaProviderDeleteRecord>(start, end)
                }
            }
            records.forEach {
                it.label = packageNameToLabel[it.packageName]
                if (it.label == null) {
                    it.label = binderViewModel.getLabel(it.packageName)
                    packageNameToLabel[it.packageName] = it.label!!
                }
            }
            _recordsFlow.value = records

//           TODO: register support
//            if (contentObserver == null) {
//                contentObserver = getApplication<Application>().contentResolver.registerObserver(
//                    MediaStore.Images.Media.INTERNAL_CONTENT_URI
//                ) {
//                    loadImages()
//                }
//            }
        }
    }

    private suspend inline fun <reified E : MediaProviderRecord> queryRecord(start: Long, end: Long)
            : List<MediaProviderRecord> = withContext(Dispatchers.IO) {
        val projection = arrayOf(E::class.simpleName)
        val selection = start.toString()
        val sortOrder = end.toString()

        getApplication<Application>().contentResolver.query(
            MediaStore.Images.Media.INTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val constructor = E::class.java.declaredConstructors.first()
            val mockParameters = constructor.parameterTypes.map { type ->
                when (type) {
                    Int::class.java -> 0
                    String::class.java -> ""
                    Long::class.java -> 0L
                    List::class.java -> emptyList<String>()
                    Boolean::class.java -> true
                    else -> throw IllegalArgumentException()
                }
            }.toTypedArray()
            return@withContext (constructor.newInstance(*mockParameters) as E).convert(cursor)
        }
        return@withContext emptyList()
    }
}

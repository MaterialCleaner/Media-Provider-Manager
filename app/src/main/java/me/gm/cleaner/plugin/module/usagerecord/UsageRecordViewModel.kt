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
import android.database.Cursor
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import me.gm.cleaner.plugin.dao.ModulePreferences
import me.gm.cleaner.plugin.dao.usagerecord.MediaProviderDeleteRecord
import me.gm.cleaner.plugin.dao.usagerecord.MediaProviderInsertRecord
import me.gm.cleaner.plugin.dao.usagerecord.MediaProviderQueryRecord
import me.gm.cleaner.plugin.dao.usagerecord.MediaProviderRecord
import me.gm.cleaner.plugin.module.BinderViewModel
import me.gm.cleaner.plugin.module.PreferencesPackageInfo
import java.util.*
import kotlin.collections.set

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
    val calendar: Calendar = Calendar.getInstance()
    private val packageManager
        get() = getApplication<Application>().packageManager
    private val packageNameToPackageInfo = mutableMapOf<String, PreferencesPackageInfo>()

    private val _recordsFlow = MutableStateFlow<List<MediaProviderRecord>>(emptyList())
    val recordsFlow =
        combine(_recordsFlow, _isSearchingFlow, _queryTextFlow) { source, isSearching, queryText ->
            var sequence = source.asSequence()
            if (isSearching) {
                val lowerQuery = queryText.lowercase()
                sequence = sequence.filter {
                    it.dataList.any { data -> data.lowercase().contains(lowerQuery) } ||
                            it.packageInfo?.label?.lowercase()?.contains(lowerQuery) == true ||
                            it.packageName.lowercase().contains(lowerQuery)
                }
            }
            sequence = sequence.sortedWith(Comparator.comparingLong {
                -it.timeMillis
            })
            sequence.toList()
        }
    val records: List<MediaProviderRecord>
        get() = _recordsFlow.value

    private var contentObserver: ContentObserver? = null
    private val cursors = mutableListOf<Cursor>()

    /**
     * Find the start and the end time millis of a day.
     * @param timeMillis any time millis in that day
     */
    fun loadRecords(binderViewModel: BinderViewModel, timeMillis: Long) = calendar.run {
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

    fun loadRecords(
        binderViewModel: BinderViewModel, start: Long, end: Long,
        isHideQuery: Boolean, isHideInsert: Boolean, isHideDelete: Boolean
    ): Deferred<Unit> = viewModelScope.async {
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
        }.onEach {
            it.packageInfo = packageNameToPackageInfo[it.packageName]
            if (it.packageInfo == null) {
                val pi = binderViewModel.getPackageInfo(it.packageName) ?: return@onEach
                it.packageInfo = PreferencesPackageInfo.newInstance(pi, packageManager)
                packageNameToPackageInfo[it.packageName] = it.packageInfo!!
            }
        }.takeWhile {
            it.packageInfo != null
        }
        _recordsFlow.value = records

        if (contentObserver == null) {
            contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    reloadRecords(binderViewModel)
                }
            }
            cursors.forEach {
                it.registerContentObserver(contentObserver)
                it.setNotificationUri(
                    getApplication<Application>().contentResolver,
                    MediaStore.Images.Media.INTERNAL_CONTENT_URI
                )
            }
        }
    }

    fun reloadRecords(binderViewModel: BinderViewModel) =
        loadRecords(binderViewModel, calendar.timeInMillis)

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
            if (contentObserver == null) {
                cursors.add(cursor)
            }
            val constructor = E::class.java.declaredConstructors.first()
            val mockParameters = constructor.parameterTypes.map { type ->
                when (type) {
                    Int::class.java -> 0
                    String::class.java -> ""
                    Long::class.java -> 0L
                    List::class.java -> emptyList<String>()
                    Boolean::class.java -> false
                    else -> throw IllegalArgumentException()
                }
            }.toTypedArray()
            return@withContext (constructor.newInstance(*mockParameters) as E).convert(cursor)
        }
        return@withContext emptyList()
    }

    override fun onCleared() {
        val contentObserver = contentObserver
        if (contentObserver != null) {
            cursors.forEach { it.unregisterContentObserver(contentObserver) }
            cursors.clear()
        }
    }
}

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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.gm.cleaner.plugin.dao.mediaprovider.MediaProviderDeleteRecord
import me.gm.cleaner.plugin.dao.mediaprovider.MediaProviderInsertRecord
import me.gm.cleaner.plugin.dao.mediaprovider.MediaProviderQueryRecord
import me.gm.cleaner.plugin.dao.mediaprovider.MediaProviderRecord
import java.text.Collator

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
    private val collator = Collator.getInstance()

    private val _recordsFlow = MutableStateFlow<List<MediaProviderRecord>>(emptyList())
    val recordsFlow = _recordsFlow.asStateFlow()
    val records: List<MediaProviderRecord>
        get() = _recordsFlow.value

    private var contentObserver: ContentObserver? = null

    fun loadRecords(date: Long, vararg packageNames: String) {

    }

    fun loadRecords(start: Long, end: Long, vararg packageNames: String) {
        viewModelScope.launch {
            val records = queryRecord<MediaProviderQueryRecord>(start, end, *packageNames) +
                    queryRecord<MediaProviderInsertRecord>(start, end, *packageNames) +
                    queryRecord<MediaProviderDeleteRecord>(start, end, *packageNames)
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

    private suspend inline fun <reified E : MediaProviderRecord> queryRecord(
        start: Long, end: Long, vararg packageNames: String
    ): List<MediaProviderRecord> = withContext(Dispatchers.IO) {
        val projection = arrayOf(E::class.simpleName)
        val selection = start.toString()
        val selectionArgs = packageNames
        val sortOrder = end.toString()

        getApplication<Application>().contentResolver.query(
            MediaStore.Images.Media.INTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            return@withContext E::class.java.newInstance().convert(cursor)
        }
        return@withContext emptyList()
    }
}

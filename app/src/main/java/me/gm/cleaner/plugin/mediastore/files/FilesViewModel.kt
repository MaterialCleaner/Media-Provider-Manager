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

package me.gm.cleaner.plugin.mediastore.files

import android.app.Application
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import me.gm.cleaner.plugin.dao.ModulePreferences
import me.gm.cleaner.plugin.mediastore.MediaStoreViewModel
import me.gm.cleaner.plugin.xposed.util.MimeUtils

class FilesViewModel(application: Application) : MediaStoreViewModel<MediaStoreFiles>(application) {
    override val uri: Uri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
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
    val requeryFlow = combine(_isSearchingFlow, _queryTextFlow) { isSearching, _ ->
        if (isSearching) delay(500L)
    }

    override suspend fun queryMedias(): List<MediaStoreFiles> {
        val files = mutableListOf<MediaStoreFiles>()

        withContext(Dispatchers.IO) {

            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Files.FileColumns.DATE_TAKEN,
                MediaStore.Files.FileColumns.SIZE,
            )

            val selection = "${MediaStore.Files.FileColumns.DATE_TAKEN} >= ?"

            val selectionArgs = arrayOf(
                dateToTimestamp(day = 1, month = 1, year = 1970).toString()
            )

            val sortOrder = when (ModulePreferences.sortMediaBy) {
                ModulePreferences.SORT_BY_FILE_NAME -> MediaStore.Files.FileColumns.DATA
                ModulePreferences.SORT_BY_DATE_TAKEN -> "${MediaStore.Files.FileColumns.DATE_TAKEN} DESC"
                else -> throw IllegalArgumentException()
            }

            val resolver = getApplication<Application>().contentResolver
            resolver.query(
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->

                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                val mimeTypeColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                val dateTakenColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_TAKEN)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)

                Log.i(TAG, "Found ${cursor.count} files")
                while (cursor.moveToNext()) {

                    val id = cursor.getLong(idColumn)
                    val data = cursor.getString(dataColumn)
                    val mimeType = cursor.getString(mimeTypeColumn)
                    val timeMillis = cursor.getLong(dateTakenColumn)
                    val size = cursor.getLong(sizeColumn)

                    val contentUri = ContentUris.withAppendedId(
                        when (MimeUtils.resolveMediaType(mimeType)) {
                            MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            // Unsupported type
                            else -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                        },
                        id
                    )

                    val file = MediaStoreFiles(id, contentUri, data, mimeType, timeMillis, size)
                    if (isSearching) {
                        val lowerQuery = queryText.lowercase()
                        if (!file.data.lowercase().contains(lowerQuery)) {
                            continue
                        }
                    }
                    files += file

                    Log.v(TAG, "Added file: $file")
                }
            }
        }

        Log.v(TAG, "Found ${files.size} files")
        return files
    }
}

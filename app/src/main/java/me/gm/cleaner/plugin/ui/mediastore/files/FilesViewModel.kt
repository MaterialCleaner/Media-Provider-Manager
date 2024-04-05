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

package me.gm.cleaner.plugin.ui.mediastore.files

import android.app.Application
import android.content.ContentUris
import android.media.MediaScannerConnection
import android.mtp.MtpConstants
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.gm.cleaner.plugin.dao.RootPreferences
import me.gm.cleaner.plugin.dao.RootPreferences.SORT_BY_DATE_TAKEN
import me.gm.cleaner.plugin.dao.RootPreferences.SORT_BY_PATH
import me.gm.cleaner.plugin.dao.RootPreferences.SORT_BY_SIZE
import me.gm.cleaner.plugin.ktx.getValue
import me.gm.cleaner.plugin.ktx.setValue
import me.gm.cleaner.plugin.ui.mediastore.MediaStoreModel
import me.gm.cleaner.plugin.ui.mediastore.MediaStoreViewModel
import me.gm.cleaner.plugin.util.fileNameComparator
import me.gm.cleaner.plugin.xposed.util.MimeUtils

open class FilesViewModel(application: Application) :
    MediaStoreViewModel<MediaStoreFiles>(application) {
    private val uri: Uri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)

    protected val _isSearchingFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)
    var isSearching: Boolean by _isSearchingFlow
    protected val _queryTextFlow: MutableStateFlow<String> = MutableStateFlow("")
    var queryText: String by _queryTextFlow

    override suspend fun queryMedias(uri: Uri, sortMediaBy: Int): List<MediaStoreFiles> {
        uriForLoad = uri
        val files = mutableListOf<MediaStoreFiles>()

        withContext(Dispatchers.IO) {

            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DATA,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.RELATIVE_PATH,
                MediaStore.MediaColumns.DATE_TAKEN,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.SIZE,
            )

            val selection = "${MediaStore.MediaColumns.DATE_TAKEN} >= ?"

            val selectionArgs = arrayOf(
                dateToTimestamp(day = 1, month = 1, year = 1970).toString()
            )

            val sortOrder = when (sortMediaBy) {
                SORT_BY_PATH -> MediaStore.MediaColumns.RELATIVE_PATH + ", " +
                        MediaStore.MediaColumns.DISPLAY_NAME

                SORT_BY_DATE_TAKEN -> "${MediaStore.MediaColumns.DATE_TAKEN} DESC"
                SORT_BY_SIZE -> "${MediaStore.MediaColumns.SIZE} DESC"
                else -> throw IllegalArgumentException()
            }

            getApplication<Application>().contentResolver.query(
                uri,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->

                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val displayNameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val relativePathColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                val dateTakenColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)

                Log.i(TAG, "Found ${cursor.count} files")
                while (cursor.moveToNext()) {

                    val id = cursor.getLong(idColumn)
                    val displayName = cursor.getString(displayNameColumn)
                    val relativePath = cursor.getString(relativePathColumn)
                    val data = cursor.getString(dataColumn)
                    val dateTaken = cursor.getLong(dateTakenColumn)
                    val mimeType = cursor.getString(mimeTypeColumn)
                    val size = cursor.getLong(sizeColumn)
                    val contentUri = ContentUris.withAppendedId(
                        when (MimeUtils.resolveMediaType(mimeType)) {
                            MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            // Unsupported type
                            else -> uri
                        },
                        id
                    )

                    if (isSearching) {
                        if (!data.contains(queryText, true)) {
                            continue
                        }
                    }
                    val file = MediaStoreFiles(
                        id, contentUri, displayName, relativePath, data, dateTaken, mimeType, size
                    )
                    files += file

                    Log.v(TAG, "Added file: $file")
                }
            }
        }

        if (sortMediaBy == SORT_BY_PATH) {
            files.sortWith(fileNameComparator { it.displayName })
            files.sortWith(fileNameComparator { it.relativePath })
        }

        Log.v(TAG, "Found ${files.size} files")
        return files
    }

    override fun deleteMedia(media: MediaStoreModel) {
        if (media is MediaStoreFiles && MimeUtils.resolveFormatCode(media.mimeType) == MtpConstants.FORMAT_UNDEFINED) {
            MediaScannerConnection.scanFile(getApplication(), arrayOf(media.data), null, null)
        } else {
            super.deleteMedia(media)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun deleteMedias(medias: Array<out MediaStoreModel>) {
        val partition = medias.partition {
            it is MediaStoreFiles && MimeUtils.resolveFormatCode(it.mimeType) == MtpConstants.FORMAT_UNDEFINED
        }
        MediaScannerConnection.scanFile(
            getApplication(), partition.first.map { (it as MediaStoreFiles).data }.toTypedArray(),
            null, null
        )

        super.deleteMedias(partition.second.toTypedArray())
    }

    init {
        viewModelScope.launch {
            combine(
                _isSearchingFlow, _queryTextFlow, RootPreferences.sortMediaByFlowable.asFlow()
            ) { isSearching, queryText, sortMediaBy ->
                queryMedias(uri, sortMediaBy)
            }.collect {
                _mediasFlow.value = it
            }
        }
        application.contentResolver.registerContentObserver(
            uri, true, contentObserver
        )
    }
}

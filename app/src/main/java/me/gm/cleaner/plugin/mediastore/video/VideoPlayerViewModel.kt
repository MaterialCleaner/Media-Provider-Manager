/*
 * Copyright 2022 Green Mushroom
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

package me.gm.cleaner.plugin.mediastore.video

import android.app.Application
import android.net.Uri
import android.provider.MediaStore
import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import me.gm.cleaner.plugin.R
import java.io.FileNotFoundException

class VideoPlayerViewModel(application: Application) : AndroidViewModel(application) {
    fun queryVideoInfoAsync(uri: Uri) = viewModelScope.async {
        queryVideoInfo(uri)
    }

    private suspend fun queryVideoInfo(uri: Uri) = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DATE_TAKEN,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DURATION,
        )

        val context = getApplication<Application>()
        context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            null
        )?.use { cursor ->

            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val dateTakenColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

            if (cursor.moveToNext()) {
                val data = cursor.getString(dataColumn)
                val dateTaken = cursor.getLong(dateTakenColumn)
                val size = cursor.getLong(sizeColumn)
                val width = cursor.getLong(widthColumn)
                val height = cursor.getLong(heightColumn)
                val resolution = "$width x $height"
                val duration = cursor.getLong(durationColumn)

                val infos = listOf(
                    context.getString(
                        R.string.info_item, context.getString(R.string.menu_sort_by_path_title),
                        data
                    ),
                    context.getString(
                        R.string.info_item,
                        context.getString(R.string.menu_sort_by_date_taken_title),
                        DateUtils.formatDateTime(
                            context, dateTaken,
                            DateUtils.FORMAT_NO_NOON or DateUtils.FORMAT_NO_MIDNIGHT or
                                    DateUtils.FORMAT_ABBREV_ALL or DateUtils.FORMAT_SHOW_YEAR or
                                    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME
                        )
                    ),
                    context.getString(
                        R.string.info_item, context.getString(R.string.menu_sort_by_size_title),
                        Formatter.formatFileSize(context, size)
                    ),
                    context.getString(
                        R.string.info_item, context.getString(R.string.resolution),
                        resolution
                    ),
                    // TODO DURATION
                )
                return@withContext Result.success(infos.joinToString("\n"))
            }
        }
        Result.failure(FileNotFoundException())
    }
}

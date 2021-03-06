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

package me.gm.cleaner.plugin.mediastore.imagepager

import android.app.Application
import android.graphics.PointF
import android.net.Uri
import android.provider.MediaStore
import android.text.format.DateUtils
import android.text.format.Formatter
import android.util.Size
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import me.gm.cleaner.plugin.R
import java.io.FileNotFoundException

class ImagePagerViewModel(application: Application) : AndroidViewModel(application) {
    var isFirstEntrance = true

    val size by lazy {
        val displayMetrics = getApplication<Application>().resources.displayMetrics
        Size(displayMetrics.widthPixels, displayMetrics.heightPixels)
    }
    private val top by lazy {
        val res = getApplication<Application>().resources
        val actionBarSize =
            res.getDimensionPixelSize(com.google.android.material.R.dimen.m3_appbar_size_compact)
        val resourceId = res.getIdentifier("status_bar_height", "dimen", "android")
        res.getDimensionPixelSize(resourceId) + actionBarSize
    }
    private val vTarget = PointF()
    fun isOverlay(subsamplingScaleImageView: SubsamplingScaleImageView): Boolean {
        if (!subsamplingScaleImageView.isReady) {
            return false
        }
        subsamplingScaleImageView.sourceToViewCoord(0F, 0F, vTarget)
        return vTarget.y - top < 0
    }

    fun queryImageInfoAsync(uri: Uri) = viewModelScope.async {
        queryImageInfo(uri)
    }

    private suspend fun queryImageInfo(uri: Uri) = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
        )

        val context = getApplication<Application>()
        context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            null
        )?.use { cursor ->

            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val dateTakenColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)

            if (cursor.moveToNext()) {
                val data = cursor.getString(dataColumn)
                val dateTaken = cursor.getLong(dateTakenColumn)
                val size = cursor.getLong(sizeColumn)
                val width = cursor.getLong(widthColumn)
                val height = cursor.getLong(heightColumn)
                val resolution = "$width x $height"

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
                )
                return@withContext Result.success(infos.joinToString("\n"))
            }
        }
        Result.failure(FileNotFoundException())
    }
}

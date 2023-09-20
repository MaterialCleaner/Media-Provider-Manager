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
import android.app.RecoverableSecurityException
import android.content.IntentSender
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.core.os.bundleOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import me.gm.cleaner.plugin.R
import java.io.FileNotFoundException

class ImagePagerViewModel(application: Application, state: SavedStateHandle) :
    AndroidViewModel(application) {

    init {
        state.setSavedStateProvider(::currentItemId.name) {
            bundleOf(::currentItemId.name to currentItemId)
        }
    }

    private val _isOverlayingLiveData: MutableLiveData<Boolean> = MutableLiveData(false)
    val isOverlayingLiveData: LiveData<Boolean>
        get() = _isOverlayingLiveData
    private val top: Int by lazy {
        val res = getApplication<Application>().resources
        val actionBarSize =
            res.getDimensionPixelSize(com.google.android.material.R.dimen.m3_appbar_size_compact)
        val resourceId = res.getIdentifier("status_bar_height", "dimen", "android")
        res.getDimensionPixelSize(resourceId) + actionBarSize
    }

    var currentItemId: Long = state.get<Bundle>(::currentItemId.name).let { bundle ->
        bundle?.getLong(::currentItemId.name) ?: 0L
    }

    fun isOverlaying(displayRect: RectF?): Boolean {
        displayRect ?: return false
        val isOverlaying = displayRect.top < top
        _isOverlayingLiveData.postValue(isOverlaying)
        return isOverlaying
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

            val dataColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
            val dateTakenColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
            val sizeColumn = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)
            val widthColumn = cursor.getColumnIndex(MediaStore.Images.Media.WIDTH)
            val heightColumn = cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT)

            if (cursor.moveToNext()) {
                val nameStrIdToValue = mutableListOf<Pair<Int, String>>()
                if (dataColumn != -1) {
                    val data = cursor.getString(dataColumn)
                    nameStrIdToValue += R.string.menu_sort_by_path_title to data
                }
                if (dateTakenColumn != -1) {
                    val dateTaken = cursor.getLong(dateTakenColumn)
                    nameStrIdToValue += R.string.menu_sort_by_date_taken_title to
                            DateUtils.formatDateTime(
                                context, dateTaken,
                                DateUtils.FORMAT_NO_NOON or DateUtils.FORMAT_NO_MIDNIGHT or
                                        DateUtils.FORMAT_ABBREV_ALL or DateUtils.FORMAT_SHOW_YEAR or
                                        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME
                            )
                }
                if (sizeColumn != -1) {
                    val size = cursor.getLong(sizeColumn)
                    nameStrIdToValue += R.string.menu_sort_by_size_title to
                            Formatter.formatFileSize(context, size)
                }
                if (widthColumn != -1 && heightColumn != -1) {
                    val width = cursor.getLong(widthColumn)
                    val height = cursor.getLong(heightColumn)
                    val resolution = "$width x $height"
                    nameStrIdToValue += R.string.resolution to resolution
                }

                val info = nameStrIdToValue.map { (nameStrId, value) ->
                    context.getString(
                        R.string.info_item, context.getString(nameStrId), value
                    )
                }
                return@withContext Result.success(info.joinToString("\n"))
            }
        }
        Result.failure(FileNotFoundException())
    }

    fun queryImageTitleAsync(uri: Uri) = viewModelScope.async {
        queryImageTitle(uri)
    }

    private suspend fun queryImageTitle(uri: Uri) = withContext(Dispatchers.IO) {
        val projection = arrayOf(MediaStore.Images.Media.DISPLAY_NAME)
        val context = getApplication<Application>()
        context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            null
        )?.use { cursor ->

            val displayNameColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)

            if (cursor.moveToNext()) {
                val displayName = cursor.getString(displayNameColumn)

                return@withContext Result.success(displayName)
            }
        }
        Result.failure(FileNotFoundException())
    }

    private val _permissionNeededForDelete: MutableLiveData<IntentSender?> = MutableLiveData()
    internal val permissionNeededForDelete: LiveData<IntentSender?> = _permissionNeededForDelete

    fun deleteImageAsync(uri: Uri) = viewModelScope.async {
        return@async performDeleteImage(uri)
    }

    private suspend fun performDeleteImage(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            getApplication<Application>().contentResolver.delete(uri, null, null)
            return@withContext true
        } catch (securityException: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val recoverableSecurityException =
                    securityException as? RecoverableSecurityException
                        ?: throw securityException
                _permissionNeededForDelete.postValue(
                    recoverableSecurityException.userAction.actionIntent.intentSender
                )
                return@withContext false
            } else {
                throw securityException
            }
        }
    }
}

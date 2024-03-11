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

package me.gm.cleaner.plugin.ui.mediastore.video

import android.app.Application
import android.content.pm.ActivityInfo
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException

class VideoPlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val _screenOrientationFlow: MutableStateFlow<Int> =
        MutableStateFlow(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
    val screenOrientationLiveData: LiveData<Int>
        get() = _screenOrientationFlow.asLiveData()
    var screenOrientation: Int
        get() = _screenOrientationFlow.value
        set(value) {
            _screenOrientationFlow.tryEmit(value)
        }

    fun queryVideoTitleAsync(uri: Uri) = viewModelScope.async {
        queryVideoTitle(uri)
    }

    private suspend fun queryVideoTitle(uri: Uri) = withContext(Dispatchers.IO) {
        val projection = arrayOf(MediaStore.Video.Media.DISPLAY_NAME)

        val context = getApplication<Application>()
        context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            null
        )?.use { cursor ->

            val displayNameColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)

            if (cursor.moveToNext()) {
                val displayName = cursor.getString(displayNameColumn)

                return@withContext Result.success(displayName)
            }
        }
        Result.failure(FileNotFoundException())
    }
}

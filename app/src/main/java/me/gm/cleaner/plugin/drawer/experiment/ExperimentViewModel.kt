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

package me.gm.cleaner.plugin.drawer.experiment

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.os.FileUtils
import android.provider.MediaStore
import android.util.SparseArray
import androidx.appcompat.view.menu.MenuBuilder
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ensureActive
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.data.unsplash.UnsplashPhoto
import me.gm.cleaner.plugin.data.unsplash.UnsplashRepository
import me.gm.cleaner.plugin.drawer.experiment.ExperimentContentItems.findIndexById
import me.gm.cleaner.plugin.drawer.experiment.ExperimentContentItems.findItemById
import java.net.URL
import javax.inject.Inject

@HiltViewModel
class ExperimentViewModel @Inject constructor(private val repository: UnsplashRepository) :
    ViewModel() {
    val dismissedCard = mutableListOf<Int>()

    @SuppressLint("RestrictedApi")
    fun prepareContentItems(activity: Activity, adapter: ExperimentAdapter) {
        val menu = MenuBuilder(activity)
        activity.menuInflater.inflate(R.menu.experiment_content, menu)
        val items = ExperimentContentItems.forMenuBuilder(menu)
        dismissedCard.asSequence()
            .map { id -> items.findIndexById(id) }
            .sortedDescending()
            .forEach { indexOfSubHeader ->
                if (indexOfSubHeader + 1 <= items.size && items[indexOfSubHeader + 1] is ExperimentContentSeparatorItem) {
                    items.removeAt(indexOfSubHeader + 1)
                }
                items.removeAt(indexOfSubHeader)
            }

        items.findItemById<ExperimentContentActionItem>(R.id.unsplash_download_manager).action =
            unsplashDownloadManager(activity)
        items.findItemById<ExperimentContentActionItem>(R.id.unsplash_insert).action =
            unsplashInsert(activity)
        adapter.submitList(items)
    }

    val actions = SparseArray<Deferred<*>>()

    private val _unsplashPhotosLiveData: MutableLiveData<Result<List<UnsplashPhoto>>> =
        MutableLiveData(Result.failure(UninitializedPropertyAccessException()))
    val unsplashPhotosLiveData: LiveData<Result<List<UnsplashPhoto>>> = _unsplashPhotosLiveData
    private var unsplashPhotos: Result<List<UnsplashPhoto>>
        get() = _unsplashPhotosLiveData.value!!
        set(value) {
            _unsplashPhotosLiveData.postValue(value)
        }

    private var width = 0
    private lateinit var downloadManager: DownloadManager
    private fun unsplashDownloadManager(context: Context): suspend CoroutineScope.() -> Unit {
        if (!::downloadManager.isInitialized) {
            width = context.resources.displayMetrics.widthPixels
            downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        }
        return {
            val unsplashPhotoListResult = if (unsplashPhotos.isSuccess) unsplashPhotos
            else repository.fetchUnsplashPhotoList()
            ensureActive()
            unsplashPhotoListResult.onSuccess { unsplashPhotos ->
                repeat(10) {
                    val unsplashPhoto = unsplashPhotos.random()
                    val request = DownloadManager
                        .Request(unsplashPhoto.getPhotoUrl(width).toUri())
                        .setDestinationInExternalPublicDir(
                            Environment.DIRECTORY_PICTURES, unsplashPhoto.filename
                        )
                    val id = downloadManager.enqueue(request)
                }
            }.onFailure { e ->
                e.printStackTrace()
                // TODO
            }
            unsplashPhotos = unsplashPhotoListResult
        }
    }

    private fun unsplashInsert(context: Context): suspend CoroutineScope.() -> Unit {
        if (!::downloadManager.isInitialized) {
            width = context.resources.displayMetrics.widthPixels
        }
        return {
            val unsplashPhotoListResult = if (unsplashPhotos.isSuccess) unsplashPhotos
            else repository.fetchUnsplashPhotoList()
            ensureActive()
            val resolver = context.contentResolver
            unsplashPhotoListResult.onSuccess { unsplashPhotos ->
                repeat(10) {
                    ensureActive()
                    val unsplashPhoto = unsplashPhotos.random()
                    val newImageDetails = ContentValues().apply {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                        put(MediaStore.MediaColumns.DISPLAY_NAME, unsplashPhoto.filename)
                        put(
                            MediaStore.MediaColumns.MIME_TYPE,
                            "image/${unsplashPhoto.filename.substringAfterLast('.')}"
                        )
                    }
                    val imageUri = resolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, newImageDetails
                    ) ?: return@repeat
                    runCatching {
                        val `is` = URL(unsplashPhoto.getPhotoUrl(width)).openStream()
                        val os = resolver.openOutputStream(imageUri, "w") ?: return@runCatching
                        FileUtils.copy(`is`, os)
                    }
                }
            }.onFailure { e ->
                e.printStackTrace()
                // TODO
            }
            unsplashPhotos = unsplashPhotoListResult
        }
    }
}

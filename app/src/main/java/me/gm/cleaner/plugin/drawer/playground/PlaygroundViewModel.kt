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

package me.gm.cleaner.plugin.drawer.playground

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.os.FileUtils
import android.provider.MediaStore
import android.util.SparseArray
import android.widget.Toast
import androidx.appcompat.view.menu.MenuBuilder
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import me.gm.cleaner.plugin.BuildConfig
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.data.unsplash.UnsplashPhoto
import me.gm.cleaner.plugin.data.unsplash.UnsplashRepository
import me.gm.cleaner.plugin.drawer.playground.PlaygroundContentItems.findIndexById
import me.gm.cleaner.plugin.drawer.playground.PlaygroundContentItems.findItemById
import java.io.File
import java.net.URL
import javax.inject.Inject

@HiltViewModel
class PlaygroundViewModel @Inject constructor(private val repository: UnsplashRepository) :
    ViewModel() {
    val dismissedCards = mutableListOf<Int>()

    @SuppressLint("RestrictedApi")
    fun prepareContentItems(fragment: PlaygroundFragment, adapter: PlaygroundAdapter) {
        val activity = fragment.requireActivity()
        val menu = MenuBuilder(activity)
        activity.menuInflater.inflate(R.menu.playground_content, menu)
        val items = PlaygroundContentItems.forMenuBuilder(menu)
        dismissedCards.asSequence()
            .map { id -> items.findIndexById(id) }
            .sortedDescending()
            .forEach { indexOfSubHeader ->
                if (indexOfSubHeader + 1 <= items.size &&
                    items[indexOfSubHeader + 1] is PlaygroundContentSeparatorItem
                ) {
                    items.removeAt(indexOfSubHeader + 1)
                }
                items.removeAt(indexOfSubHeader)
            }

        items.findItemById<PlaygroundContentActionItem>(R.id.unsplash_download_manager).action =
            unsplashDownloadManager(activity)
        items.findItemById<PlaygroundContentActionItem>(R.id.unsplash_insert).action =
            unsplashInsert(activity)
        items.findItemById<PlaygroundContentActionItem>(R.id.intercept_insert).action =
            interceptInsert(fragment)
        items.findItemById<PlaygroundContentActionItem>(R.id.intercept_download_manager).action =
            interceptDownloadManager(fragment)
        items.findItemById<PlaygroundContentActionItem>(R.id.intercept_query).action =
            interceptQuery(fragment)

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
            val unsplashPhotoListResult = withContext(Dispatchers.IO) {
                if (unsplashPhotos.isSuccess) unsplashPhotos
                else repository.fetchUnsplashPhotoList()
            }
            unsplashPhotoListResult.onSuccess { unsplashPhotos ->
                withContext(Dispatchers.IO) {
                    repeat(10) {
                        ensureActive()
                        val unsplashPhoto = unsplashPhotos.random()
                        val request = DownloadManager
                            .Request(unsplashPhoto.getPhotoUrl(width).toUri())
                            .setDestinationInExternalPublicDir(
                                Environment.DIRECTORY_PICTURES,
                                File.separator + "MPM" + File.separator + unsplashPhoto.filename
                            )
                        val id = downloadManager.enqueue(request)
                    }
                }
            }.onFailure { e ->
                e.printStackTrace()
                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
            }
            unsplashPhotos = unsplashPhotoListResult
        }
    }

    private fun unsplashInsert(context: Context): suspend CoroutineScope.() -> Unit {
        if (!::downloadManager.isInitialized) {
            width = context.resources.displayMetrics.widthPixels
        }
        return {
            val unsplashPhotoListResult = withContext(Dispatchers.IO) {
                if (unsplashPhotos.isSuccess) unsplashPhotos
                else repository.fetchUnsplashPhotoList()
            }
            unsplashPhotoListResult.onSuccess { unsplashPhotos ->
                withContext(Dispatchers.IO) {
                    val resolver = context.contentResolver
                    repeat(10) {
                        ensureActive()
                        val unsplashPhoto = unsplashPhotos.random()
                        val imageDetails = ContentValues().apply {
                            put(
                                MediaStore.MediaColumns.RELATIVE_PATH,
                                Environment.DIRECTORY_PICTURES + File.separator + "MPM"
                            )
                            put(MediaStore.MediaColumns.DISPLAY_NAME, unsplashPhoto.filename)
                            put(
                                MediaStore.MediaColumns.MIME_TYPE,
                                "image/${unsplashPhoto.filename.substringAfterLast('.')}"
                            )
                            put(MediaStore.Audio.Media.IS_PENDING, 1)
                        }
                        val imageUri = resolver.insert(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageDetails
                        ) ?: return@repeat
                        runCatching {
                            val `is` = URL(unsplashPhoto.getPhotoUrl(width)).openStream()
                            val os = resolver.openOutputStream(imageUri, "w") ?: return@runCatching
                            FileUtils.copy(`is`, os)
                        }
                        imageDetails.clear()
                        imageDetails.put(MediaStore.Audio.Media.IS_PENDING, 0)
                        resolver.update(imageUri, imageDetails, null, null)
                    }
                }
            }.onFailure { e ->
                e.printStackTrace()
                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
            }
            unsplashPhotos = unsplashPhotoListResult
        }
    }

    private fun interceptInsert(fragment: PlaygroundFragment): suspend CoroutineScope.() -> Unit {
        return {
            val direction = PlaygroundFragmentDirections.actionPlaygroundToCreateTemplate(
                templateName = fragment.getString(R.string.intercept_insert_title),
                hookOperation = fragment.resources.getStringArray(R.array.hook_operation_entryValues) -
                        fragment.getString(R.string.hook_operation_query),
                packageNames = arrayOf(BuildConfig.APPLICATION_ID),
                permittedMediaTypes = fragment.resources.getStringArray(R.array.media_types_entryValues) -
                        fragment.getString(R.string.media_type_image),
            )
            fragment.findNavController().navigate(direction)
        }
    }

    private fun interceptDownloadManager(fragment: PlaygroundFragment): suspend CoroutineScope.() -> Unit {
        return {
            val direction = PlaygroundFragmentDirections.actionPlaygroundToCreateTemplate(
                templateName = fragment.getString(R.string.intercept_download_manager_title),
                hookOperation = fragment.resources.getStringArray(R.array.hook_operation_entryValues) -
                        fragment.getString(R.string.hook_operation_query),
                packageNames = fragment.resources.getStringArray(R.array.recommend_package),
                permittedMediaTypes = fragment.resources.getStringArray(R.array.media_types_entryValues) -
                        fragment.getString(R.string.media_type_image),
            )
            fragment.findNavController().navigate(direction)
        }
    }

    private fun interceptQuery(fragment: PlaygroundFragment): suspend CoroutineScope.() -> Unit {
        return {
            val direction = PlaygroundFragmentDirections.actionPlaygroundToCreateTemplate(
                templateName = fragment.getString(R.string.intercept_query_title),
                hookOperation = fragment.resources.getStringArray(R.array.hook_operation_entryValues) -
                        fragment.getString(R.string.hook_operation_insert),
                packageNames = arrayOf(BuildConfig.APPLICATION_ID),
                permittedMediaTypes = fragment.resources.getStringArray(R.array.media_types_entryValues) -
                        fragment.getString(R.string.media_type_image),
            )
            fragment.findNavController().navigate(direction)
        }
    }
}

private inline operator fun <reified T> Array<T>.minus(string: T): Array<T> {
    return (toMutableSet() - string).toTypedArray()
}

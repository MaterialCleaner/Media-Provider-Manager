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

package me.gm.cleaner.plugin.experiment

import android.app.DownloadManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import me.gm.cleaner.plugin.data.unsplash.UnsplashPhoto
import me.gm.cleaner.plugin.data.unsplash.UnsplashRepository
import me.gm.cleaner.plugin.util.LogUtils
import javax.inject.Inject

@HiltViewModel
class ExperimentViewModel @Inject constructor(private val repository: UnsplashRepository) :
    ViewModel() {
    private var width = 0
    private lateinit var downloadManager: DownloadManager

    private val _unsplashPhotosFlow: MutableStateFlow<Result<List<UnsplashPhoto>>> =
        MutableStateFlow(Result.failure(UninitializedPropertyAccessException()))
    val unsplashPhotosLiveData = _unsplashPhotosFlow.asLiveData()

    fun unsplashDownloadManager(context: Context): suspend CoroutineScope.() -> Unit {
        if (!::downloadManager.isInitialized) {
            width = context.resources.displayMetrics.widthPixels
            downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        }
        return {
            val unsplashPhotoListResult = repository.fetchUnsplashPhotoList()
                .also { _unsplashPhotosFlow.emit(it) }
            ensureActive()
            unsplashPhotoListResult.onSuccess { unsplashPhotos ->
                repeat(10) {
                    val unsplashPhoto = unsplashPhotos.random()
                    val request = DownloadManager
                        .Request(Uri.parse(unsplashPhoto.getPhotoUrl(width)))
                        .setDestinationInExternalPublicDir(
                            Environment.DIRECTORY_PICTURES, unsplashPhoto.filename
                        )
                    val id = downloadManager.enqueue(request)
                }
            }.onFailure { e ->
                LogUtils.e(e)
                // TODO
            }
        }
    }

    companion object {
        val Context.hasWifiTransport: Boolean
            get() {
                val connManager =
                    getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val capabilities = connManager.getNetworkCapabilities(connManager.activeNetwork)
                return capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            }
    }
}

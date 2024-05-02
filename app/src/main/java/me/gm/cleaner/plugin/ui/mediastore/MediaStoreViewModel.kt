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

package me.gm.cleaner.plugin.ui.mediastore

import android.annotation.SuppressLint
import android.app.Application
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.gm.cleaner.plugin.dao.RootPreferences
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

abstract class MediaStoreViewModel<M : MediaStoreModel>(application: Application) :
    AndroidViewModel(application) {
    protected val _mediasFlow: MutableStateFlow<List<M>> = MutableStateFlow(emptyList())
    val mediasFlow: StateFlow<List<M>> = _mediasFlow.asStateFlow()
    val medias: List<M>
        get() = _mediasFlow.value

    protected lateinit var uriToLoad: Uri

    /**
     * Performs a one shot load of medias from [uri] [Uri] into
     * the [_mediasFlow] [MutableStateFlow] above.
     */
    fun load() {
        if (::uriToLoad.isInitialized) {
            viewModelScope.launch {
                _mediasFlow.value =
                    queryMedias(uriToLoad, RootPreferences.sortMediaByFlowable.value)
            }
        }
    }

    protected abstract suspend fun queryMedias(uri: Uri, sortMediaBy: Int): List<M>

    /**
     * Convenience method to convert a day/month/year date into a UNIX timestamp.
     *
     * We're suppressing the lint warning because we're not actually using the date formatter
     * to format the date to display, just to specify a format to use to parse it, and so the
     * locale warning doesn't apply.
     */
    @Suppress("SameParameterValue")
    @SuppressLint("SimpleDateFormat")
    protected fun dateToTimestamp(day: Int, month: Int, year: Int): Long =
        SimpleDateFormat("dd.MM.yyyy").let { formatter ->
            TimeUnit.MICROSECONDS.toSeconds(formatter.parse("$day.$month.$year")?.time ?: 0)
        }

    protected var contentObserver: ContentObserver =
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                load()
            }
        }

    /**
     * Since we register a [ContentObserver], we want to unregister this when the `ViewModel`
     * is being released.
     */
    override fun onCleared() {
        contentObserver.let { contentObserver ->
            getApplication<Application>().contentResolver.unregisterContentObserver(contentObserver)
        }
    }

    companion object {
        const val TAG = "MediaStoreVM"
    }
}

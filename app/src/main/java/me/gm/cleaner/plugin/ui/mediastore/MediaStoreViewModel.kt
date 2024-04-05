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
import android.app.RecoverableSecurityException
import android.content.IntentSender
import android.database.ContentObserver
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.BaseColumns
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.gm.cleaner.plugin.dao.RootPreferences
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

abstract class MediaStoreViewModel<M : MediaStoreModel>(application: Application) :
    AndroidViewModel(application) {
    protected val _mediasFlow: MutableStateFlow<List<M>> = MutableStateFlow(emptyList())
    val mediasFlow: StateFlow<List<M>> = _mediasFlow.asStateFlow()
    val medias: List<M>
        get() = _mediasFlow.value

    private var pendingDeleteMedia: MediaStoreModel? = null
    private val _permissionNeededForDelete: MutableLiveData<IntentSender?> = MutableLiveData()
    internal val permissionNeededForDelete: LiveData<IntentSender?> = _permissionNeededForDelete

    protected lateinit var uriForLoad: Uri

    /**
     * Performs a one shot load of medias from [uri] [Uri] into
     * the [_mediasFlow] [MutableStateFlow] above.
     */
    fun load() {
        if (::uriForLoad.isInitialized) {
            viewModelScope.launch {
                _mediasFlow.value =
                    queryMedias(uriForLoad, RootPreferences.sortMediaByFlowable.value)
            }
        }
    }

    open fun deleteMedia(media: MediaStoreModel) {
        viewModelScope.launch {
            performDeleteMedia(media)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    open fun deleteMedias(medias: Array<out MediaStoreModel>) {
        viewModelScope.launch {
            performDeleteMedias(*medias)
        }
    }

    internal fun deletePendingMedia() {
        pendingDeleteMedia?.let { media ->
            pendingDeleteMedia = null
            deleteMedia(media)
        }
    }

    protected abstract suspend fun queryMedias(uri: Uri, sortMediaBy: Int): List<M>

    // @see https://developer.android.com/training/data-storage/shared/media#remove-item
    private suspend fun performDeleteMedia(media: MediaStoreModel) {
        withContext(Dispatchers.IO) {
            try {
                /**
                 * In [Build.VERSION_CODES.Q] and above, it isn't possible to modify
                 * or delete items in MediaStore directly, and explicit permission
                 * must usually be obtained to do this.
                 *
                 * The way it works is the OS will throw a [RecoverableSecurityException],
                 * which we can catch here. Inside there's an [IntentSender] which the
                 * activity can use to prompt the user to grant permission to the item
                 * so it can be either updated or deleted.
                 */
                getApplication<Application>().contentResolver.delete(
                    media.contentUri,
                    "${BaseColumns._ID} = ?",
                    arrayOf(media.id.toString())
                )
            } catch (securityException: SecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val recoverableSecurityException =
                        securityException as? RecoverableSecurityException
                            ?: return@withContext // from MediaStore.createDeleteRequest()

                    // Signal to the Activity that it needs to request permission and
                    // try the delete again if it succeeds.
                    pendingDeleteMedia = media
                    _permissionNeededForDelete.postValue(
                        recoverableSecurityException.userAction.actionIntent.intentSender
                    )
                } else {
                    throw securityException
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun performDeleteMedias(vararg medias: MediaStoreModel) {
        if (medias.isEmpty()) {
            // This check is important because media store iterates to the first element without check.
            // Pass an empty collection to createRequest results in NoSuchElementException.
            return
        }
        withContext(Dispatchers.IO) {
            val pendingIntent = MediaStore.createDeleteRequest(
                getApplication<Application>().contentResolver, medias.map { it.contentUri }
            )
            _permissionNeededForDelete.postValue(pendingIntent.intentSender)
        }
    }

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

    fun rescanFiles() {
        val paths = medias.map { it.data }.toTypedArray()
        MediaScannerConnection.scanFile(getApplication(), paths, null, null)
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

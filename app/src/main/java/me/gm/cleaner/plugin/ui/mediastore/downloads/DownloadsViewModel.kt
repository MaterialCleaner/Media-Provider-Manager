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

package me.gm.cleaner.plugin.ui.mediastore.downloads

import android.app.Application
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import me.gm.cleaner.plugin.dao.RootPreferences
import me.gm.cleaner.plugin.ui.mediastore.files.FilesViewModel

class DownloadsViewModel(application: Application) : FilesViewModel(application) {
    private val uri: Uri = MediaStore.Downloads.EXTERNAL_CONTENT_URI

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

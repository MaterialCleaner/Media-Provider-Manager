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

package me.gm.cleaner.plugin.ui.drawer.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import me.gm.cleaner.plugin.data.github.ReadmeRepository
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class AboutViewModel @Inject constructor(private val repository: ReadmeRepository) : ViewModel() {
    private var rawReadme: Result<String> = Result.failure(UninitializedPropertyAccessException())

    fun getRawReadmeAsync() = viewModelScope.async {
        if (rawReadme.isFailure) {
            withContext(Dispatchers.IO) {
                rawReadme = repository.getRawReadme(Locale.getDefault().toLanguageTag())
            }
        }
        rawReadme
    }
}

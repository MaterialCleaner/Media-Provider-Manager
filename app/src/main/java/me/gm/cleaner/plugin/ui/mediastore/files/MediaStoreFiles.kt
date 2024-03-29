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

package me.gm.cleaner.plugin.ui.mediastore.files

import android.net.Uri
import me.gm.cleaner.plugin.ui.mediastore.MediaStoreModel

data class MediaStoreFiles(
    override val id: Long,
    override val contentUri: Uri,
    override val displayName: String,
    override val relativePath: String,
    override val data: String,
    override val dateTaken: Long,
    val mimeType: String?,
    val size: Long,
) : MediaStoreModel(id, contentUri, displayName, relativePath, data, dateTaken)

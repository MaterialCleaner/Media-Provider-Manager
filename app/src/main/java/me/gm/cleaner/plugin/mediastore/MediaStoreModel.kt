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

package me.gm.cleaner.plugin.mediastore

import android.net.Uri
import androidx.recyclerview.widget.DiffUtil

abstract class MediaStoreModel(
    open val id: Long,
    open val contentUri: Uri,
    open val displayName: String,
    open val relativePath: String,
    open val data: String,
    open val dateTaken: Long,
) {
    companion object {
        fun <M : MediaStoreModel> createCallback() = object : DiffUtil.ItemCallback<M>() {
            override fun areItemsTheSame(oldItem: M, newItem: M): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: M, newItem: M): Boolean = oldItem == newItem
        }
    }
}

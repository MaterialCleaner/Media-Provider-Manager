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

package me.gm.cleaner.plugin.data.unsplash

import com.google.gson.annotations.SerializedName
import java.util.*

/**
 * Data class that represents a photo from Unsplash.
 */
data class UnsplashPhoto(
    @field:SerializedName("format") val format: String,
    @field:SerializedName("width") val width: Int,
    @field:SerializedName("height") val height: Int,
    @field:SerializedName("filename") val filename: String,
    @field:SerializedName("id") val id: Int,
    @field:SerializedName("author") val author: String,
    @field:SerializedName("author_url") val authorUrl: String,
    @field:SerializedName("post_url") val postUrl: String,
) {

    fun getPhotoUrl(requestWidth: Int) =
        String.format(Locale.getDefault(), PHOTO_URL_BASE, requestWidth, id)

    companion object {
        private const val PHOTO_URL_BASE = "https://unsplash.it/%d?image=%d"
    }
}

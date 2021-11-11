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

package me.gm.cleaner.plugin.xposed.hooker

import android.net.Uri
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

interface MediaProviderHooker {
    fun XC_MethodHook.MethodHookParam.ensureMediaProvider() {
        require(method.declaringClass.name == "com.android.providers.media.MediaProvider")
    }

    val XC_MethodHook.MethodHookParam.callingPackage: String
        get() {
            ensureMediaProvider()
            val threadLocal =
                XposedHelpers.getObjectField(thisObject, "mCallingIdentity") as ThreadLocal<*>
            return XposedHelpers.callMethod(threadLocal.get(), "getPackageName") as String
        }

    val XC_MethodHook.MethodHookParam.isCallingPackageAllowedHidden: Boolean
        get() {
            ensureMediaProvider()
            return XposedHelpers.callMethod(thisObject, "isCallingPackageAllowedHidden") as Boolean
        }

    fun XC_MethodHook.MethodHookParam.matchUri(uri: Uri, allowHidden: Boolean): Int {
        ensureMediaProvider()
        return XposedHelpers.callMethod(thisObject, "matchUri", uri, allowHidden) as Int
    }

    val IMAGES_MEDIA: Int
        get() = 1
    val IMAGES_MEDIA_ID: Int
        get() = 2
    val IMAGES_MEDIA_ID_THUMBNAIL: Int
        get() = 3
    val IMAGES_THUMBNAILS: Int
        get() = 4
    val IMAGES_THUMBNAILS_ID: Int
        get() = 5

    val AUDIO_MEDIA: Int
        get() = 100
    val AUDIO_MEDIA_ID: Int
        get() = 101
    val AUDIO_MEDIA_ID_GENRES: Int
        get() = 102
    val AUDIO_MEDIA_ID_GENRES_ID: Int
        get() = 103
    val AUDIO_GENRES: Int
        get() = 106
    val AUDIO_GENRES_ID: Int
        get() = 107
    val AUDIO_GENRES_ID_MEMBERS: Int
        get() = 108
    val AUDIO_GENRES_ALL_MEMBERS: Int
        get() = 109
    val AUDIO_PLAYLISTS: Int
        get() = 110
    val AUDIO_PLAYLISTS_ID: Int
        get() = 111
    val AUDIO_PLAYLISTS_ID_MEMBERS: Int
        get() = 112
    val AUDIO_PLAYLISTS_ID_MEMBERS_ID: Int
        get() = 113
    val AUDIO_ARTISTS: Int
        get() = 114
    val AUDIO_ARTISTS_ID: Int
        get() = 115
    val AUDIO_ALBUMS: Int
        get() = 116
    val AUDIO_ALBUMS_ID: Int
        get() = 117
    val AUDIO_ARTISTS_ID_ALBUMS: Int
        get() = 118
    val AUDIO_ALBUMART: Int
        get() = 119
    val AUDIO_ALBUMART_ID: Int
        get() = 120
    val AUDIO_ALBUMART_FILE_ID: Int
        get() = 121

    val VIDEO_MEDIA: Int
        get() = 200
    val VIDEO_MEDIA_ID: Int
        get() = 201
    val VIDEO_MEDIA_ID_THUMBNAIL: Int
        get() = 202
    val VIDEO_THUMBNAILS: Int
        get() = 203
    val VIDEO_THUMBNAILS_ID: Int
        get() = 204

    val VOLUMES: Int
        get() = 300
    val VOLUMES_ID: Int
        get() = 301

    val MEDIA_SCANNER: Int
        get() = 500

    val FS_ID: Int
        get() = 600
    val VERSION: Int
        get() = 601

    val FILES: Int
        get() = 700
    val FILES_ID: Int
        get() = 701

    val DOWNLOADS: Int
        get() = 800
    val DOWNLOADS_ID: Int
        get() = 801
}

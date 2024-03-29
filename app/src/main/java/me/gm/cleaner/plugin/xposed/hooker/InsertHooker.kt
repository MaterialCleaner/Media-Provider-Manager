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

import android.content.ClipDescription
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.FileUtils
import android.provider.MediaStore
import android.text.TextUtils
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.dao.MediaProviderOperation.Companion.OP_INSERT
import me.gm.cleaner.plugin.dao.MediaProviderRecord
import me.gm.cleaner.plugin.xposed.ManagerService
import me.gm.cleaner.plugin.xposed.util.MimeUtils
import java.io.File
import java.util.*

class InsertHooker(private val service: ManagerService) : XC_MethodHook(), MediaProviderHooker {
    @Throws(Throwable::class)
    override fun beforeHookedMethod(param: MethodHookParam) {
        if (param.isFuseThread) {
            return
        }
        /** ARGUMENTS */
        val match = (
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) param.args[2] else param.args[1]
                ) as Int
        val uri = (
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) param.args[3] else param.args[2]
                ) as Uri
        val extras = (
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) param.args[4] else Bundle.EMPTY
                ) as Bundle
        val values = (
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) param.args[5] else param.args[3]
                ) as ContentValues
        val mediaType = (
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) param.args[6] else param.args[4]
                ) as Int

        /** PARSE */
        var mimeType = values.getAsString(MediaStore.MediaColumns.MIME_TYPE)
        val wasPathEmpty = wasPathEmpty(values)
        if (wasPathEmpty) {
            // Generate path when undefined
            ensureUniqueFileColumns(param.thisObject, match, uri, values, mimeType)
        }
        val data = values.getAsString(MediaStore.MediaColumns.DATA)
        if (mimeType.isNullOrEmpty()) {
            mimeType = values.getAsString(MediaStore.MediaColumns.MIME_TYPE)
            // Restore to support apps not targeting sdk R or higher
            values.remove(MediaStore.MediaColumns.MIME_TYPE)
        }
        if (wasPathEmpty) {
            // Restore to allow mkdir
            values.remove(MediaStore.MediaColumns.DATA)
        }

        /** INTERCEPT */
        val shouldIntercept = service.ruleSp.templates
            .filterTemplate(javaClass, param.callingPackage)
            .applyTemplates(listOf(data), listOf(mimeType)).first()
        if (shouldIntercept) {
            param.result = null
        }

        /** RECORD */
        if (service.rootSp.getBoolean(
                service.resources.getString(R.string.usage_record_key), true
            )
        ) {
            service.dao.insert(
                MediaProviderRecord(
                    0,
                    System.currentTimeMillis(),
                    param.callingPackage,
                    match,
                    OP_INSERT,
                    listOf(data),
                    listOf(mimeType),
                    listOf(shouldIntercept)
                )
            )
            service.dispatchMediaChange()
        }
    }

    private fun wasPathEmpty(values: ContentValues) =
        !values.containsKey(MediaStore.MediaColumns.DATA)
                || values.getAsString(MediaStore.MediaColumns.DATA).isEmpty()

    private fun ensureUniqueFileColumns(
        thisObject: Any, match: Int, uri: Uri, values: ContentValues, mimeType: String?
    ) {
        var defaultMimeType = ClipDescription.MIMETYPE_UNKNOWN
        var defaultPrimary = Environment.DIRECTORY_DOWNLOADS
        var defaultSecondary: String? = null
        when (match) {
            AUDIO_MEDIA, AUDIO_MEDIA_ID -> {
                defaultMimeType = "audio/mpeg"
                defaultPrimary = Environment.DIRECTORY_MUSIC
            }

            VIDEO_MEDIA, VIDEO_MEDIA_ID -> {
                defaultMimeType = "video/mp4"
                defaultPrimary = Environment.DIRECTORY_MOVIES
            }

            IMAGES_MEDIA, IMAGES_MEDIA_ID -> {
                defaultMimeType = "image/jpeg"
                defaultPrimary = Environment.DIRECTORY_PICTURES
            }

            AUDIO_ALBUMART, AUDIO_ALBUMART_ID -> {
                defaultMimeType = "image/jpeg"
                defaultPrimary = Environment.DIRECTORY_MUSIC
                defaultSecondary = DIRECTORY_THUMBNAILS
            }

            VIDEO_THUMBNAILS, VIDEO_THUMBNAILS_ID -> {
                defaultMimeType = "image/jpeg"
                defaultPrimary = Environment.DIRECTORY_MOVIES
                defaultSecondary = DIRECTORY_THUMBNAILS
            }

            IMAGES_THUMBNAILS, IMAGES_THUMBNAILS_ID -> {
                defaultMimeType = "image/jpeg"
                defaultPrimary = Environment.DIRECTORY_PICTURES
                defaultSecondary = DIRECTORY_THUMBNAILS
            }

            AUDIO_PLAYLISTS, AUDIO_PLAYLISTS_ID -> {
                defaultMimeType = "audio/mpegurl"
                defaultPrimary = Environment.DIRECTORY_MUSIC
            }

            DOWNLOADS, DOWNLOADS_ID -> {
                defaultPrimary = Environment.DIRECTORY_DOWNLOADS
            }
        }
        // Give ourselves reasonable defaults when missing
        if (TextUtils.isEmpty(values.getAsString(MediaStore.MediaColumns.DISPLAY_NAME))) {
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, System.currentTimeMillis().toString())
        }
        // Use default directories when missing
        if (TextUtils.isEmpty(values.getAsString(MediaStore.MediaColumns.RELATIVE_PATH))) {
            if (defaultSecondary != null) {
                values.put(
                    MediaStore.MediaColumns.RELATIVE_PATH, "$defaultPrimary/$defaultSecondary/"
                )
            } else {
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, "$defaultPrimary/")
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val resolvedVolumeName = XposedHelpers.callMethod(
                thisObject, "resolveVolumeName", uri
            ) as String
            val volumePath = XposedHelpers.callMethod(
                thisObject, "getVolumePath", resolvedVolumeName
            ) as File

            val fileUtilsClass = XposedHelpers.findClass(
                "com.android.providers.media.util.FileUtils", service.classLoader
            )
            val isFuseThread = XposedHelpers.callMethod(thisObject, "isFuseThread")
                    as Boolean
            XposedHelpers.callStaticMethod(
                fileUtilsClass, "sanitizeValues", values, !isFuseThread
            )
            XposedHelpers.callStaticMethod(
                fileUtilsClass, "computeDataFromValues", values, volumePath, isFuseThread
            )

            var res = File(values.getAsString(MediaStore.MediaColumns.DATA))
            res = XposedHelpers.callStaticMethod(
                fileUtilsClass, "buildUniqueFile", res.parentFile, mimeType, res.name
            ) as File

            values.put(MediaStore.MediaColumns.DATA, res.absolutePath)
        } else {
            val resolvedVolumeName = XposedHelpers.callMethod(
                thisObject, "resolveVolumeName", uri
            ) as String

            val relativePath = XposedHelpers.callMethod(
                thisObject, "sanitizePath",
                values.getAsString(MediaStore.MediaColumns.RELATIVE_PATH)
            )
            val displayName = XposedHelpers.callMethod(
                thisObject, "sanitizeDisplayName",
                values.getAsString(MediaStore.MediaColumns.DISPLAY_NAME)
            )

            var res = XposedHelpers.callMethod(
                thisObject, "getVolumePath", resolvedVolumeName
            ) as File
            res = XposedHelpers.callStaticMethod(
                Environment::class.java, "buildPath", res, relativePath
            ) as File
            res = XposedHelpers.callStaticMethod(
                FileUtils::class.java, "buildUniqueFile", res, mimeType, displayName
            ) as File

            values.put(MediaStore.MediaColumns.DATA, res.absolutePath)
        }

        val displayName = values.getAsString(MediaStore.MediaColumns.DISPLAY_NAME)
        val mimeTypeFromExt = if (TextUtils.isEmpty(displayName)) null
        else MimeUtils.resolveMimeType(File(displayName))
        if (TextUtils.isEmpty(values.getAsString(MediaStore.MediaColumns.MIME_TYPE))) {
            // Extract the MIME type from the display name if we couldn't resolve it from the
            // raw path
            if (mimeTypeFromExt != null) {
                values.put(MediaStore.MediaColumns.MIME_TYPE, mimeTypeFromExt)
            } else {
                // We couldn't resolve mimeType, it means that both display name and MIME type
                // were missing in values, so we use defaultMimeType.
                values.put(MediaStore.MediaColumns.MIME_TYPE, defaultMimeType)
            }
        }
    }

    companion object {
        private const val DIRECTORY_THUMBNAILS = ".thumbnails"
    }
}

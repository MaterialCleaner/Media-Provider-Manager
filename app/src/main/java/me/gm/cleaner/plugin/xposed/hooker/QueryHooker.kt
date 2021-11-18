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

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Process
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns
import android.util.ArraySet
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import me.gm.cleaner.plugin.BuildConfig
import me.gm.cleaner.plugin.xposed.ManagerService
import java.util.function.Consumer
import java.util.function.Function

class QueryHooker(private val service: ManagerService) : XC_MethodHook(), MediaProviderHooker {
    @Throws(Throwable::class)
    override fun beforeHookedMethod(param: MethodHookParam) {
        /** ARGUMENTS */
        val uri = param.args[0] as Uri
        val projection = param.args[1] as? Array<String>?
        val queryArgs = param.args[2] as? Bundle ?: Bundle.EMPTY
        val signal = param.args[3] as? CancellationSignal

        if (param.callingPackage in
            setOf("com.android.providers.media", "com.android.providers.media.module")
        ) {
            // Scanning files and internal queries.
            return
        }
        if (isBinderQuery(param.callingPackage, uri)) {
            return
        }

        /** PARSE */
        val query = Bundle(queryArgs)
        query.remove(INCLUDED_DEFAULT_DIRECTORIES)
        val honoredArgs = ArraySet<String>()
        val databaseUtils = try {
            XposedHelpers.findClass(
                "com.android.providers.media.util.DatabaseUtils", service.classLoader
            )
        } catch (e: XposedHelpers.ClassNotFoundError) {
            XposedBridge.log(Process.myPid().toString())
            return
        }
        XposedHelpers.callStaticMethod(
            databaseUtils, "resolveQueryArgs", query, object : Consumer<String> {
                override fun accept(t: String) {
                    honoredArgs.add(t)
                }
            }, object : Function<String, String> {
                override fun apply(t: String) =
                    XposedHelpers.callMethod(param.thisObject, "ensureCustomCollator", t) as String
            }
        )
        val table = param.matchUri(uri, param.isCallingPackageAllowedHidden)
        val dataProjection = when {
            projection == null -> null
            table in setOf(IMAGES_THUMBNAILS, VIDEO_THUMBNAILS) -> projection + FileColumns.DATA
            else -> projection + arrayOf(FileColumns.DATA, FileColumns.MIME_TYPE)
        }
        val helper = XposedHelpers.callMethod(param.thisObject, "getDatabaseForUri", uri)
        val qb = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> XposedHelpers.callMethod(
                param.thisObject, "getQueryBuilder", TYPE_QUERY, table, uri, query,
                object : Consumer<String> {
                    override fun accept(t: String) {
                        honoredArgs.add(t)
                    }
                }
            )
            Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> XposedHelpers.callMethod(
                param.thisObject, "getQueryBuilder", TYPE_QUERY, uri, table, query
            )
            else -> throw UnsupportedOperationException()
        }

        val c = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> XposedHelpers.callMethod(
                qb, "query", helper, dataProjection, query, signal
            )
            Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> {
                val selection = query.getString(ContentResolver.QUERY_ARG_SQL_SELECTION)
                val selectionArgs =
                    query.getStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS)
                val sortOrder = query.getString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER) ?: let {
                    if (query.containsKey(ContentResolver.QUERY_ARG_SORT_COLUMNS)) {
                        XposedHelpers.callStaticMethod(
                            ContentResolver::class.java, "createSqlSortClause", query
                        ) as String?
                    } else {
                        null
                    }
                }
                val groupBy = if (table == AUDIO_ARTISTS_ID_ALBUMS) "audio.album_id"
                else null
                val having = null
                val limit = uri.getQueryParameter("limit")

                XposedHelpers.callMethod(
                    qb, "query", XposedHelpers.callMethod(helper, "getWritableDatabase"),
                    dataProjection, selection, selectionArgs, groupBy, having, sortOrder, limit,
                    signal
                )
            }
            else -> throw UnsupportedOperationException()
        } as Cursor

        val dataColumn = c.getColumnIndexOrThrow(FileColumns.DATA)
        val mimeTypeColumn = c.getColumnIndex(FileColumns.MIME_TYPE)

        /** RECORD */
        XposedBridge.log("packageName: " + param.callingPackage)
        if (c.isAfterLast) {
            XposedBridge.log("isAfterLast")
        }
        while (c.moveToNext()) {
            val data = c.getString(dataColumn)
            val mimeType = when {
                mimeTypeColumn != -1 -> c.getString(mimeTypeColumn)
                else -> DIRECTORY_THUMBNAILS
            }
            XposedBridge.log("path: $data")
            XposedBridge.log("mimeType: $mimeType")
        }

        /** INTERCEPT */
    }

    @Throws(Throwable::class)
    override fun afterHookedMethod(param: MethodHookParam) {
        val uri = param.args[0] as Uri
        if (isBinderQuery(param.callingPackage, uri)) {
            val c = param.result as? Cursor
            c?.extras?.putBinder("me.gm.cleaner.plugin.cursor.extra.BINDER", service)
        }
    }

    private fun isBinderQuery(callingPackage: String, uri: Uri) =
        callingPackage == BuildConfig.APPLICATION_ID && uri == MediaStore.Images.Media.INTERNAL_CONTENT_URI

    companion object {
        private const val INCLUDED_DEFAULT_DIRECTORIES = "android:included-default-directories"
        private const val TYPE_QUERY = 0
        private const val DIRECTORY_THUMBNAILS = ".thumbnails"
    }
}

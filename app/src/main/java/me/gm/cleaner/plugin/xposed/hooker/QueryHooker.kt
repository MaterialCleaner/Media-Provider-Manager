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
import android.database.MatrixCursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns
import android.util.ArraySet
import androidx.core.os.bundleOf
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import me.gm.cleaner.plugin.BuildConfig
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.dao.MediaProviderOperation.Companion.OP_QUERY
import me.gm.cleaner.plugin.dao.MediaProviderRecord
import me.gm.cleaner.plugin.xposed.ManagerService
import me.gm.cleaner.plugin.xposed.util.FilteredCursor
import java.util.function.Consumer
import java.util.function.Function

class QueryHooker(private val service: ManagerService) : XC_MethodHook(), MediaProviderHooker {
    @Throws(Throwable::class)
    override fun beforeHookedMethod(param: MethodHookParam) {
        if (param.isFuseThread) {
            return
        }
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

        /** PARSE */
        val query = Bundle(queryArgs)
        query.remove(INCLUDED_DEFAULT_DIRECTORIES)
        val honoredArgs = ArraySet<String>()
        val databaseUtilsClass = XposedHelpers.findClass(
            "com.android.providers.media.util.DatabaseUtils", service.classLoader
        )
        XposedHelpers.callStaticMethod(
            databaseUtilsClass, "resolveQueryArgs", query, object : Consumer<String> {
                override fun accept(t: String) {
                    honoredArgs.add(t)
                }
            }, object : Function<String, String> {
                override fun apply(t: String) =
                    XposedHelpers.callMethod(param.thisObject, "ensureCustomCollator", t) as String
            }
        )
        if (isClientQuery(param.callingPackage, uri)) {
            param.result = handleClientQuery(projection, query)
            return
        }
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

        val c = try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> XposedHelpers.callMethod(
                    qb, "query", helper, dataProjection, query, signal
                )
                Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> {
                    val selection = query.getString(ContentResolver.QUERY_ARG_SQL_SELECTION)
                    val selectionArgs =
                        query.getStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS)
                    val sortOrder =
                        query.getString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER) ?: let {
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
        } catch (e: XposedHelpers.InvocationTargetError) {
            // IllegalArgumentException that thrown from the media provider. Nothing I can do.
            return
        }
        if (c.isAfterLast) {
            // querying nothing.
            c.close()
            return
        }
        val dataColumn = c.getColumnIndexOrThrow(FileColumns.DATA)
        val mimeTypeColumn = c.getColumnIndex(FileColumns.MIME_TYPE)

        val data = mutableListOf<String>()
        val mimeType = mutableListOf<String>()
        while (c.moveToNext()) {
            data += c.getString(dataColumn)
            mimeType += c.getString(mimeTypeColumn)
        }

        /** INTERCEPT */
        val shouldIntercept = service.ruleSp.templates
            .filterTemplate(javaClass, param.callingPackage)
            .applyTemplates(data, mimeType)
        if (shouldIntercept.isEmpty()) {
            c.close()
        } else {
            c.moveToFirst()
            val filter = shouldIntercept.mapIndexedNotNull { index, b ->
                if (!b) index else null
            }.toIntArray()
            param.result = FilteredCursor.createUsingFilter(c, filter)
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
                    table,
                    OP_QUERY,
                    if (data.size < MAX_SIZE) data else data.subList(0, MAX_SIZE),
                    mimeType,
                    shouldIntercept
                )
            )
            service.dispatchMediaChange()
        }
    }

    private fun isClientQuery(callingPackage: String, uri: Uri) =
        callingPackage == BuildConfig.APPLICATION_ID && uri == MediaStore.Images.Media.INTERNAL_CONTENT_URI

    /**
     * This function handles queries from the client. It takes effect when calling package is
     * [BuildConfig.APPLICATION_ID] and query Uri is [MediaStore.Images.Media.INTERNAL_CONTENT_URI].
     * @param table We regard projection as table name.
     * @param queryArgs We regard selection as start time millis, sort order as end time millis,
     * selection args as package names.
     * @return Returns an empty [Cursor] with [ManagerService]'s [android.os.IBinder] in its extras
     * when queryArgs is empty. Returns a [Cursor] queried from the [MediaProviderRecordDatabase]
     * when at least table name, start time millis and end time millis are declared.
     * @throws [NullPointerException] or [IllegalArgumentException] when we don't know how to
     * handle the query.
     */
    private fun handleClientQuery(table: Array<String>?, queryArgs: Bundle): Cursor {
        if (table == null || queryArgs.isEmpty) {
            return MatrixCursor(arrayOf("binder")).apply {
                extras = bundleOf("me.gm.cleaner.plugin.cursor.extra.BINDER" to service)
            }
        }
        val start = queryArgs.getString(ContentResolver.QUERY_ARG_SQL_SELECTION)!!.toLong()
        val end = queryArgs.getString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER)!!.toLong()
        val packageNames = queryArgs.getStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS)
        return service.dao.loadForTimeMillis(start, end, *table.map { it.toInt() }.toIntArray())
    }

    companion object {
        private const val INCLUDED_DEFAULT_DIRECTORIES = "android:included-default-directories"
        private const val TYPE_QUERY = 0

        private const val MAX_SIZE = 1000
    }
}

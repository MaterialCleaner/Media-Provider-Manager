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

import android.content.ContentResolver.QUERY_ARG_SQL_SELECTION
import android.content.ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore.Files.FileColumns
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import me.gm.cleaner.plugin.xposed.ManagerService

class DeleteHooker(private val service: ManagerService) : XC_MethodHook(), MediaProviderHooker {
    @Throws(Throwable::class)
    override fun beforeHookedMethod(param: MethodHookParam) {
        val uri = param.args[0] as Uri
        val extras = param.args[1] as? Bundle
        var userWhere: String? = null
        var userWhereArgs: Array<String>? = null
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                userWhere = extras?.getString(QUERY_ARG_SQL_SELECTION)
                userWhereArgs = extras?.getStringArray(QUERY_ARG_SQL_SELECTION_ARGS)
            }
            Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> {
                userWhere = param.args[1] as? String
                userWhereArgs = param.args[2] as? Array<String>
            }
        }

        val match = param.matchUri(uri, param.isCallingPackageAllowedHidden)
        var c: Cursor? = null
        val path = when (match) {
            AUDIO_MEDIA_ID, VIDEO_MEDIA_ID, IMAGES_MEDIA_ID -> {
                try {
                    when {
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> XposedHelpers.callMethod(
                            param.thisObject, "enforceCallingPermission", uri, extras, true
                        )
                        Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> XposedHelpers.callMethod(
                            param.thisObject, "enforceCallingPermission", uri, true
                        )
                        else -> throw UnsupportedOperationException()
                    }
                } catch (securityException: SecurityException) {
                    // Give callers interacting with a specific media item a chance to
                    // escalate access if they don't already have it
                    return
                }

                val qb = when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> XposedHelpers.callMethod(
                        param.thisObject, "getQueryBuilder", TYPE_DELETE, match, uri,
                        extras ?: Bundle(), null
                    )
                    Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> XposedHelpers.callMethod(
                        param.thisObject, "getQueryBuilder", TYPE_DELETE, uri, match, null
                    )
                    else -> throw UnsupportedOperationException()
                }
                val helper = XposedHelpers.callMethod(param.thisObject, "getDatabaseForUri", uri)
                val projection = arrayOf(
                    FileColumns.MEDIA_TYPE,
                    FileColumns.DATA,
                    FileColumns._ID,
                    FileColumns.IS_DOWNLOAD,
                    FileColumns.MIME_TYPE,
                )

                c = when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> XposedHelpers.callMethod(
                        qb, "query", helper, projection, userWhere, userWhereArgs,
                        null, null, null, null, null
                    )
                    Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> XposedHelpers.callMethod(
                        qb, "query", XposedHelpers.callMethod(helper, "getWritableDatabase"),
                        projection, userWhere, userWhereArgs, null, null, null, null, null
                    )
                    else -> throw UnsupportedOperationException()
                } as Cursor
                val data = if (c.moveToNext()) c.getString(1)
                else null
                data
            }
            FILES -> userWhereArgs?.single()
            else -> return // We don't care about these data, just ignore.
        }
        c?.close()

        XposedBridge.log("packageName: " + param.callingPackage)
        XposedBridge.log("path: $path")
    }

    private val TYPE_DELETE: Int = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> 3
        Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> 2
        else -> throw UnsupportedOperationException()
    }
}

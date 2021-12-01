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

import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.dao.mediaprovider.MediaProviderInsertRecord
import me.gm.cleaner.plugin.ktx.retry
import me.gm.cleaner.plugin.xposed.ManagerService
import me.gm.cleaner.plugin.xposed.util.FileCreationObserver
import me.gm.cleaner.plugin.xposed.util.FileUtils.externalStorageDirPath
import java.io.File
import java.util.*

class InsertHooker(private val service: ManagerService) : XC_MethodHook(), MediaProviderHooker {
    private val dao = service.database.MediaProviderInsertRecordDao()
    private val pendingScan =
        Collections.synchronizedMap(mutableMapOf<String, FileCreationObserver>())

    @Throws(Throwable::class)
    override fun beforeHookedMethod(param: MethodHookParam) {
        /** ARGUMENTS */
        val uri = param.args[0] as Uri
        val initialValues = param.args[1] as? ContentValues
        val extras = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) param.args[2] as? Bundle
        else null

        val match = param.matchUri(uri, param.isCallingPackageAllowedHidden)
        if (match == MEDIA_SCANNER) {
            return
        }

        /** PARSE */
        val modern = initialValues?.containsKey(MediaStore.MediaColumns.RELATIVE_PATH) == true &&
                initialValues.containsKey(MediaStore.MediaColumns.DISPLAY_NAME)
        val data = if (modern) {
            externalStorageDirPath + File.separator +
                    initialValues?.getAsString(MediaStore.MediaColumns.RELATIVE_PATH)
                        ?.trimEnd(File.separatorChar) + File.separator +
                    initialValues?.getAsString(MediaStore.MediaColumns.DISPLAY_NAME)
        } else {
            initialValues?.getAsString(MediaStore.MediaColumns.DATA)
        }
        val mimeType = initialValues?.getAsString(MediaStore.MediaColumns.MIME_TYPE)

        /** INTERCEPT */
        val shouldIntercept = false
        if (shouldIntercept) {
            param.result = null
        }

        /** SCAN */
        if (!shouldIntercept && !modern && data != null && service.rootSp.getBoolean(
                service.resources.getString(R.string.scan_for_obsolete_insert_key), true
            )
        ) {
            val file = File(data)
            if (!file.exists()) {
                XposedBridge.log("scan for obsolete insert: $data")
                val ob = FileCreationObserver(file)
                if (pendingScan.putIfAbsent(data, ob) == null) {
                    ob.setOnMaybeFileCreatedListener {
                        val firstResult = scanFile(param.thisObject, file)
                        XposedBridge.log("scan result: $firstResult")
                        return@setOnMaybeFileCreatedListener if (firstResult != null) {
                            pendingScan.remove(data)
                            true
                        } else {
                            false
                        }
                    }.startWatching()
                }
            }
        }

        /** RECORD */
        if (service.rootSp.getBoolean(
                service.resources.getString(R.string.usage_record_key), true
            )
        ) {
            retry(10) {
                dao.insert(
                    MediaProviderInsertRecord(
                        System.currentTimeMillis() + it,
                        param.callingPackage,
                        match,
                        data ?: "",
                        mimeType ?: "",
                        shouldIntercept
                    )
                )
            }
        }
    }

    private fun scanFile(thisObject: Any, file: File): Uri? = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
            val mediaScanner = XposedHelpers.getObjectField(thisObject, "mMediaScanner")
            XposedHelpers.callMethod(
                mediaScanner, "scanFile", file, MEDIA_PROVIDER_SCAN_OCCURRED__REASON__DEMAND
            )
        }
        Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> {
            val mediaScanner = XposedHelpers.findClass(
                "com.android.providers.media.scan.MediaScanner", service.classLoader
            )
            val instance =
                XposedHelpers.callStaticMethod(mediaScanner, "instance", service.context)
            XposedHelpers.callMethod(instance, "scanFile", file)
        }
        else -> throw UnsupportedOperationException()
    } as Uri?

    companion object {
        const val MEDIA_PROVIDER_SCAN_OCCURRED__REASON__DEMAND = 2
    }
}

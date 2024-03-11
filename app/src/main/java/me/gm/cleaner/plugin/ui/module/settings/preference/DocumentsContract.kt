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

package me.gm.cleaner.plugin.ui.module.settings.preference

import android.content.Context
import android.net.Uri
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import androidx.core.provider.DocumentsContractCompat
import java.io.File

private const val EXTERNAL_STORAGE_PRIMARY_EMULATED_ROOT_ID = "primary"

// @see com.android.externalstorage.ExternalStorageProvider
fun treeUriToFile(result: Uri, context: Context): File? {
    require(DocumentsContractCompat.isTreeUri(result))
    val docId = DocumentsContract.getTreeDocumentId(result)
    val splitIndex = docId.indexOf(':', 1)

    val sm = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
    val roots = sm.storageVolumes.associateBy { volume ->
        val rootId = if (volume.isPrimary) EXTERNAL_STORAGE_PRIMARY_EMULATED_ROOT_ID
        else volume.uuid
        rootId
    }
    val tag = docId.substring(0, splitIndex)
    val root = roots[tag] ?: return null
    val path = docId.substring(splitIndex + 1)
    val target = File(root.javaClass.getMethod("getPathFile").invoke(root) as File, path)
    return target
}

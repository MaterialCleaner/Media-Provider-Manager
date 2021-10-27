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

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.app.BaseFragment

abstract class MediaStoreFragment : BaseFragment() {
    override val requiredPermissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onRequestPermissions(permissions: Array<String>, savedInstanceState: Bundle?) {
        // TODO: show rationale
        Log.i(javaClass.simpleName, "useful overriding method")
        super.onRequestPermissions(permissions, savedInstanceState)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.mediastore_toolbar, menu)
    }

    @Suppress("DEPRECATION")
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.menu_refresh -> {
            dirsToBroadcast.forEach {
                val dirToBroadcast = Environment.getExternalStoragePublicDirectory(it)
                val scanIntent = Intent("android.intent.action.MEDIA_SCANNER_SCAN_DIR").apply {
                    data = Uri.fromFile(dirToBroadcast)
                }
//                requireContext().sendBroadcast(scanIntent)
            }
            onRescanBroadcast()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    open val dirsToBroadcast = emptyArray<String>()
    abstract fun onRescanBroadcast()
}

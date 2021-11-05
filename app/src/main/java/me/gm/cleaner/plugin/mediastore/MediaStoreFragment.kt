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
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import me.gm.cleaner.plugin.BuildConfig
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.app.BaseFragment
import me.gm.cleaner.plugin.dao.ModulePreferences
import me.gm.cleaner.plugin.module.BinderReceiver

abstract class MediaStoreFragment : BaseFragment() {
    override val requiredPermissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun dispatchRequestPermissions(
        permissions: Array<String>, savedInstanceState: Bundle?
    ) {
        if (ModulePreferences.isShowAllMediaFiles) {
            super.dispatchRequestPermissions(permissions, savedInstanceState)
        } else {
            if (requiredPermissions.any {
                    ActivityCompat.checkSelfPermission(requireContext(), it) ==
                            PackageManager.PERMISSION_GRANTED
                }
            ) {
                if (BinderReceiver.pingBinder()) {
                    requiredPermissions.forEach {
                        BinderReceiver.revokeRuntimePermission(BuildConfig.APPLICATION_ID, it)
                    }
                } else {
                    dialog = AlertDialog.Builder(requireContext())
                        .setMessage(R.string.revoke_self_permission)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            val intent =
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data =
                                        Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                                }
                            startActivity(intent)
                        }
                        .show()
                }
            }
            onRequestPermissionsSuccess(requiredPermissions.toSet(), savedInstanceState)
        }
    }

    override fun onRequestPermissionsFailure(
        shouldShowRationale: Set<String>, permanentlyDenied: Set<String>,
        savedInstanceState: Bundle?
    ) {
        if (shouldShowRationale.isNotEmpty()) {
            dialog = AlertDialog.Builder(requireContext())
                .setMessage(R.string.rationale_shouldShowRationale)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    onRequestPermissions(shouldShowRationale.toTypedArray(), savedInstanceState)
                }
                .show()
        } else if (permanentlyDenied.isNotEmpty()) {
            dialog = AlertDialog.Builder(requireContext())
                .setMessage(R.string.rationale_permanentlyDenied)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                    }
                    startActivity(intent)
                }
                .show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.mediastore_toolbar, menu)
        menu.findItem(R.id.menu_show_all).isChecked = ModulePreferences.isShowAllMediaFiles
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.menu_refresh -> {
            dispatchRequestPermissions(requiredPermissions, null)
            true
        }
        R.id.menu_show_all -> {
            val isShowAllMediaFiles = !item.isChecked
            item.isChecked = isShowAllMediaFiles
            ModulePreferences.isShowAllMediaFiles = isShowAllMediaFiles
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}

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

package me.gm.cleaner.plugin.app

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.AppBarLayout
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.ktx.isNightModeActivated

abstract class BaseFragment : Fragment() {
    val supportActionBar: ActionBar?
        get() = (requireActivity() as AppCompatActivity).supportActionBar
    val appBarLayout: AppBarLayout
        get() = requireActivity().findViewById(R.id.toolbar_container)

    // PERMISSIONS
    open val requiredPermissions = emptyArray<String>()
    private lateinit var requestMultiplePermissions: ActivityResultLauncher<Array<out String>>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestMultiplePermissions =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
                // We should dispatch by ourselves rather than call dispatchRequestPermissions(),
                // or we'll stick in infinite recursion because dispatchRequestPermissions()
                // can't distinguish permanently denied permissions and never dispatches them.
                val granted = result.filterValues { it }.keys
                if (granted.isNotEmpty()) {
                    onRequestPermissionsSuccess(granted, savedInstanceState)
                }
                val denied = result.keys - granted
                if (denied.isNotEmpty()) {
                    val shouldShowRationale = denied.asSequence().filter {
                        ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), it)
                    }.toSet()
                    val permanentlyDenied = denied - shouldShowRationale
                    onRequestPermissionsFailure(
                        shouldShowRationale, permanentlyDenied, savedInstanceState
                    )
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dispatchRequestPermissions(requiredPermissions, savedInstanceState)
    }

    open fun dispatchRequestPermissions(permissions: Array<String>, savedInstanceState: Bundle?) {
        val granted = permissions.asSequence().filter {
            ActivityCompat.checkSelfPermission(requireContext(), it) ==
                    PackageManager.PERMISSION_GRANTED
        }.toSet()
        if (granted.isNotEmpty()) {
            onRequestPermissionsSuccess(granted, savedInstanceState)
        }
        if (permissions.size > granted.size) {
            val denied = permissions.toSet() - granted
            val shouldShowRationale = denied.asSequence().filter {
                ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), it)
            }.toSet()
            if (shouldShowRationale.isNotEmpty()) {
                onRequestPermissionsFailure(shouldShowRationale, emptySet(), savedInstanceState)
            } else {
                // Permissions that have never been explicitly denied and that are permanently denied
                // can't be distinguished unless we actually request.
                onRequestPermissions(denied.toTypedArray(), savedInstanceState)
            }
        }
    }

    protected open fun onRequestPermissions(
        permissions: Array<String>, savedInstanceState: Bundle?
    ) {
        requestMultiplePermissions.launch(permissions)
    }

    protected open fun onRequestPermissionsSuccess(
        permissions: Set<String>, savedInstanceState: Bundle?
    ) {
    }

    protected open fun onRequestPermissionsFailure(
        shouldShowRationale: Set<String>, permanentlyDenied: Set<String>,
        savedInstanceState: Bundle?
    ) {
        if (shouldShowRationale.isNotEmpty()) {
            onRequestPermissions(shouldShowRationale.toTypedArray(), savedInstanceState)
        } else if (permanentlyDenied.isNotEmpty()) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", requireContext().packageName, null)
            }
            startActivity(intent)
        }
    }

    // ALERT DIALOG
    lateinit var dialog: AlertDialog
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        savedInstanceState?.run {
            if (::dialog.isInitialized && getBoolean(SAVED_SHOWS_DIALOG, false)) {
                dialog.show()
            }
        }
        return container
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::dialog.isInitialized) {
            outState.putBoolean(SAVED_SHOWS_DIALOG, dialog.isShowing)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requestMultiplePermissions.unregister()
        if (::dialog.isInitialized && dialog.isShowing) {
            dialog.dismiss()
        }
    }

    fun toggleAppBar(isShow: Boolean) {
        val decorView = requireActivity().window.decorView
        if (isShow) {
            supportActionBar?.show()
            var flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            if (!resources.configuration.isNightModeActivated) {
                flags = flags or
                        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                        View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
            decorView.systemUiVisibility = flags
        } else {
            supportActionBar?.hide()
            // Fullscreen is costly in my case, so I come to terms with immersive.
            // If you persist in fullscreen, I'd advise you to display the photos with activity.
            // See also: https://developer.android.com/training/system-ui/immersive
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }
    }

    companion object {
        private const val SAVED_SHOWS_DIALOG = "android:showsDialog"
    }
}

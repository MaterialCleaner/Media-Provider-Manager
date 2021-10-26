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
import me.gm.cleaner.plugin.compat.isNightModeActivated

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
                val granted = result.filterValues { it }.keys
                if (granted.isNotEmpty()) {
                    onRequestPermissionsSuccess(granted, savedInstanceState)
                }
                val denied = requiredPermissions.toSet() - granted
                if (denied.isNotEmpty()) {
                    val canRequestAgain = denied.asSequence().filter {
                        ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), it)
                    }.toSet()
                    val permanentlyDenied = denied - canRequestAgain
                    onRequestPermissionsFailure(
                        canRequestAgain, permanentlyDenied, savedInstanceState
                    )
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onRequestPermissions(requiredPermissions, savedInstanceState)
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
        canRequestAgain: Set<String>, permanentlyDenied: Set<String>, savedInstanceState: Bundle?
    ) {
        if (canRequestAgain.isNotEmpty()) {
            onRequestPermissions(canRequestAgain.toTypedArray(), savedInstanceState)
        }
        if (permanentlyDenied.isNotEmpty()) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.fromParts("package", requireActivity().packageName, null)
            startActivity(intent)
        }
    }

    // ALERT DIALOG
    protected lateinit var dialog: AlertDialog
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

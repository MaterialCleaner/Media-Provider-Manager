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

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.NavDestination
import com.google.android.material.appbar.AppBarLayout
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.ktx.isNightModeActivated

abstract class BaseFragment : Fragment() {
    val supportActionBar: ActionBar?
        get() = (requireActivity() as AppCompatActivity).supportActionBar
    val appBarLayout: AppBarLayout
        get() = requireActivity().findViewById(R.id.toolbar_container)

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
        if (::dialog.isInitialized && dialog.isShowing) {
            dialog.dismiss()
        }
    }

    // @see https://developer.android.com/training/system-ui/immersive#EnableFullscreen
    fun toggleAppBar(isShow: Boolean) {
        val decorView = requireActivity().window.decorView
        if (isShow) {
            supportActionBar?.show()
            // Shows the system bars by removing all the flags
            // except for the ones that make the content appear under the system bars.
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
            // Enables regular immersive mode.
            // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
            // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                    // Set the content to appear under the system bars so that the
                    // content doesn't resize when the system bars hide and show.
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    // Hide the nav bar and status bar
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
        }
    }

    fun toDefaultAppBarState(currentDestination: NavDestination) {
        supportActionBar?.apply {
            title = currentDestination.label
            subtitle = null
        }
        toggleAppBar(true)
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    companion object {
        private const val SAVED_SHOWS_DIALOG = "android:showsDialog"
    }
}

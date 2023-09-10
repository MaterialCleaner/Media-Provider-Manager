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
import android.view.View
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavDestination
import com.google.android.material.appbar.AppBarLayout
import me.gm.cleaner.plugin.R

abstract class BaseFragment : Fragment() {
    val supportActionBar: ActionBar?
        get() = (requireActivity() as AppCompatActivity).supportActionBar
    val appBarLayout: AppBarLayout
        get() = requireActivity().findViewById(R.id.toolbar_container)
    protected lateinit var liftOnScrollTargetView: View

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (::liftOnScrollTargetView.isInitialized) {
            appBarLayout.setLiftOnScrollTargetView(liftOnScrollTargetView)
        }
    }

    // @see https://developer.android.com/training/system-ui/immersive#EnableFullscreen
    open fun toggleAppBar(show: Boolean) {
        val window = requireActivity().window
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (show) {
            supportActionBar?.show()
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        } else {
            supportActionBar?.hide()
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
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
}

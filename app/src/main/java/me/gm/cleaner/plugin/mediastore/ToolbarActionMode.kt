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

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView
import androidx.annotation.MenuRes
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.appcompat.view.ActionMode
import androidx.appcompat.view.SupportMenuInflater
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentController
import androidx.fragment.app.FragmentHostCallback
import me.gm.cleaner.plugin.BuildConfig
import me.gm.cleaner.plugin.R

// TODO: Finish ActionMod on back pressed and on navigation click
@SuppressLint("RestrictedApi")
class ToolbarActionMode(fragment: Fragment, private val toolbar: Toolbar) {
    private val menu: MenuBuilder
        get() = toolbar.menu as MenuBuilder

    // FIXME: ClassCastException
    private val fragmentController by lazy { FragmentController.createController(fragment.requireHost() as FragmentHostCallback<*>) }
    private val arrowDrawable = DrawerArrowDrawable(fragment.requireContext())
    private var animator: ValueAnimator? = null

    private var actionMode: ToolbarActionModeImpl? = null
    private var mCallback: ActionMode.Callback? = null
    fun startActionMode(@MenuRes menuRes: Int, callback: ActionMode.Callback): ActionMode? {
        actionMode?.finish()

        val mode = object : ToolbarActionModeImpl(toolbar, callback) {
            override fun finish() {
                super.finish()
                mCallback = null
                restore()
            }
        }
        if (mode.dispatchOnCreate()) {
            // This needs to be set before invalidate() so that it calls
            // onPrepareActionMode()
            mCallback = callback
            mode.invalidate()
            menu.close()
            menu.clear()
            toolbar.inflateMenu(menuRes)
            toolbar.setNavigationOnClickListener {
                // TODO
            }
            toolbar.setOnMenuItemClickListener { item ->
                mode.onMenuItemSelected(menu, item)
            }
            animateToMode(true)
            toolbar.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
            return mode
        }
        return null
    }

    fun restore() {
        menu.close()
        menu.clear()
        fragmentController.dispatchCreateOptionsMenu(menu, SupportMenuInflater(toolbar.context))
        toolbar.setOnMenuItemClickListener { item ->
            fragmentController.dispatchOptionsItemSelected(item)
        }
        animateToMode(false)
    }

    fun animateToMode(toActionMode: Boolean) {
        setActionBarUpIndicator(!toActionMode)
    }

    @SuppressLint("PrivateResource")
    private fun setActionBarUpIndicator(showAsDrawerIndicator: Boolean) {
        toolbar.navigationIcon = arrowDrawable
        toolbar.setNavigationContentDescription(
            if (showAsDrawerIndicator) androidx.navigation.ui.R.string.nav_app_bar_open_drawer_description
            else androidx.navigation.ui.R.string.nav_app_bar_navigate_up_description
        )

        val endValue = if (showAsDrawerIndicator) 0f else 1f
        val startValue = arrowDrawable.progress
        animator?.cancel()
        animator = ObjectAnimator.ofFloat(arrowDrawable, "progress", startValue, endValue)
        animator?.start()
    }

    // TODO: Check if it's helpful to put here.
    val toolbarTitle by lazy {
        Toolbar::class.java.run {
            if (BuildConfig.DEBUG) getDeclaredField("mTitleTextView")
            else declaredFields.first { it.type == TextView::class.java }
        }.let {
            it.isAccessible = true
            it[toolbar] as TextView
        }
    }
}

fun Fragment.startToolbarActionMode(@MenuRes menuRes: Int, callback: ActionMode.Callback) =
    ToolbarActionMode(this, requireActivity().findViewById(R.id.toolbar))
        .startActionMode(menuRes, callback)

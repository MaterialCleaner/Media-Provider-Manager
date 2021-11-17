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
import android.view.Window
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.appcompat.view.ActionMode
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.Toolbar
import me.gm.cleaner.plugin.BuildConfig
import me.gm.cleaner.plugin.R

@SuppressLint("RestrictedApi")
class ToolbarActionMode(private val activity: AppCompatActivity, private val toolbar: Toolbar) {
    private val menu: MenuBuilder
        get() = toolbar.menu as MenuBuilder
    private val arrowDrawable = DrawerArrowDrawable(activity)
    private var animator: ValueAnimator? = null

    private val originalToolbarTitle = toolbar.title to toolbar.subtitle
    private var actionMode: ToolbarActionModeImpl? = null
    private var mCallback: ActionMode.Callback? = null
    private var cancellable: OnBackPressedCallback? = null

    fun startActionMode(callback: ActionMode.Callback): ActionMode? {
        actionMode?.finish()

        val mode = object : ToolbarActionModeImpl(toolbar, callback) {
            override fun finish() {
                super.finish()
                mCallback = null
                closeMode()
            }
        }
        initForMode(mode)
        if (mode.dispatchOnCreate()) {
            // This needs to be set before invalidate() so that it calls
            // onPrepareActionMode()
            mCallback = callback
            mode.invalidate()
            animateToMode(true)
            toolbar.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
            return mode
        }
        closeMode()
        return null
    }

    private fun initForMode(mode: ToolbarActionModeImpl) {
        menu.close()
        menu.clear()
        toolbar.setNavigationOnClickListener {
            mode.finish()
        }
        toolbar.setOnMenuItemClickListener { item ->
            mode.onMenuItemSelected(menu, item)
        }
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                mode.finish()
            }
        }
        activity.onBackPressedDispatcher.addCallback(activity, onBackPressedCallback)
        cancellable = onBackPressedCallback
    }

    private fun closeMode() {
        toolbar.title = originalToolbarTitle.first
        toolbar.subtitle = originalToolbarTitle.second
        menu.close()
        menu.clear()
        activity.onCreatePanelMenu(Window.FEATURE_OPTIONS_PANEL, menu)
        toolbar.setNavigationOnClickListener {
            activity.onSupportNavigateUp()
        }
        toolbar.setOnMenuItemClickListener { item ->
            activity.onMenuItemSelected(Window.FEATURE_OPTIONS_PANEL, item)
        }
        cancellable?.remove()
        cancellable = null
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

fun AppCompatActivity.startToolbarActionMode(callback: ActionMode.Callback) =
    ToolbarActionMode(this, findViewById(R.id.toolbar)).startActionMode(callback)

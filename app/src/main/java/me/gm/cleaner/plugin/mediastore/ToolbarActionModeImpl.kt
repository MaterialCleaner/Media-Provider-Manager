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

import android.annotation.SuppressLint
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.StringRes
import androidx.appcompat.view.ActionMode
import androidx.appcompat.view.SupportMenuInflater
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.Toolbar

@SuppressLint("RestrictedApi")
open class ToolbarActionModeImpl(private val toolbar: Toolbar, private val callback: Callback) :
    ActionMode(), MenuBuilder.Callback {
    private val menu: MenuBuilder
        get() = toolbar.menu as MenuBuilder

    override fun setTitle(title: CharSequence?) {
        toolbar.title = title
    }

    override fun setTitle(@StringRes resId: Int) {
        toolbar.setTitle(resId)
    }

    override fun setSubtitle(subtitle: CharSequence?) {
        toolbar.subtitle = subtitle
    }

    override fun setSubtitle(@StringRes resId: Int) {
        toolbar.setSubtitle(resId)
    }

    override fun invalidate() {
        menu.stopDispatchingItemsChanged()
        callback.onPrepareActionMode(this, menu)
        menu.startDispatchingItemsChanged()
    }

    fun dispatchOnCreate(): Boolean {
        menu.stopDispatchingItemsChanged()
        try {
            return callback.onCreateActionMode(this, menu)
        } finally {
            menu.startDispatchingItemsChanged()
        }
    }

    override fun finish() {
        callback.onDestroyActionMode(this)
        toolbar.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
    }

    override fun getMenu(): Menu = toolbar.menu
    override fun getTitle(): CharSequence = toolbar.title
    override fun getSubtitle(): CharSequence = toolbar.subtitle
    override fun getMenuInflater() = SupportMenuInflater(toolbar.context)

    override fun onMenuItemSelected(menu: MenuBuilder, item: MenuItem) =
        callback.onActionItemClicked(this, item)

    override fun onMenuModeChange(menu: MenuBuilder) {
        invalidate()
    }

    override fun setCustomView(view: View?) {
        throw UnsupportedOperationException()
    }

    override fun getCustomView(): View {
        throw UnsupportedOperationException()
    }
}

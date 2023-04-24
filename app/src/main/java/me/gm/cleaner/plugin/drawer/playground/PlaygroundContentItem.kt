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

package me.gm.cleaner.plugin.drawer.playground

import android.annotation.SuppressLint
import android.view.View
import androidx.appcompat.view.menu.MenuBuilder
import kotlinx.coroutines.CoroutineScope

/** Unified data model for all sorts of playground content items. */
abstract class PlaygroundContentItem(
    open val id: Int
)

/** Separator items. */
data class PlaygroundContentSeparatorItem(
    override val id: Int = View.generateViewId()
) : PlaygroundContentItem(id)

/** Normal or header items. */
data class PlaygroundContentHeaderItem(
    override val id: Int,
    var title: CharSequence?
) : PlaygroundContentItem(id)

/** Normal or subheader items. */
data class PlaygroundContentSubHeaderItem(
    override val id: Int,
    var content: CharSequence?,
    var dismissed: Boolean
) : PlaygroundContentItem(id)

/** Action items. */
data class PlaygroundContentActionItem(
    override val id: Int,
    var title: CharSequence?,
    var summary: CharSequence?,
    var action: (suspend CoroutineScope.() -> Unit)? = null,
    var needsNetwork: Boolean
) : PlaygroundContentItem(id)

object PlaygroundContentItems {

    /** Convert MenuItemImpl to PlaygroundMenuItem. */
    fun forMenuBuilder(menu: MenuBuilder): MutableList<PlaygroundContentItem> {
        val items = mutableListOf<PlaygroundContentItem>()
        convertTo(items, menu)
        return items
    }

    @SuppressLint("RestrictedApi")
    private fun convertTo(items: MutableList<PlaygroundContentItem>, menu: MenuBuilder) {
        menu.visibleItems.forEach { menuItemImpl ->
            when {
                menuItemImpl.hasSubMenu() -> {
                    if (items.isNotEmpty()) {
                        items.add(PlaygroundContentSeparatorItem())
                    }
                    items.add(PlaygroundContentHeaderItem(menuItemImpl.itemId, menuItemImpl.title))
                    convertTo(items, menuItemImpl.subMenu as MenuBuilder)
                }

                menuItemImpl.isCheckable -> items.add(
                    PlaygroundContentSubHeaderItem(
                        menuItemImpl.itemId, menuItemImpl.title, menuItemImpl.isChecked
                    )
                )

                else -> items.add(
                    PlaygroundContentActionItem(
                        menuItemImpl.itemId,
                        menuItemImpl.title,
                        menuItemImpl.titleCondensed,
                        needsNetwork = menuItemImpl.isChecked
                    )
                )
            }
        }
    }

    inline fun <reified T : PlaygroundContentItem> Collection<PlaygroundContentItem>.findItemById(id: Int) =
        first { id == it.id } as T

    fun Collection<PlaygroundContentItem>.findIndexById(id: Int) = indexOfFirst { id == it.id }
}

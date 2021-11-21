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

package me.gm.cleaner.plugin.experiment

import android.annotation.SuppressLint
import android.view.View
import androidx.appcompat.view.menu.MenuBuilder
import kotlinx.coroutines.CoroutineScope

/** Unified data model for all sorts of experiment content items. */
abstract class ExperimentContentItem(
    open val id: Int
)

/** Separator items. */
data class ExperimentContentSeparatorItem(
    override val id: Int = View.generateViewId()
) : ExperimentContentItem(id)

/** Normal or header items. */
data class ExperimentContentHeaderItem(
    override val id: Int,
    var title: CharSequence?
) : ExperimentContentItem(id)

/** Normal or subheader items. */
data class ExperimentContentSubHeaderItem(
    override val id: Int,
    var content: CharSequence?,
    var dismissed: Boolean
) : ExperimentContentItem(id)

/** Action items. */
data class ExperimentContentActionItem(
    override val id: Int,
    var title: CharSequence?,
    var summary: CharSequence?,
    var action: (suspend CoroutineScope.() -> Unit)? = null,
    var needsNetwork: Boolean
) : ExperimentContentItem(id)

object ExperimentContentItems {

    /** Convert MenuItemImpl to ExperimentMenuItem. */
    fun forMenuBuilder(menu: MenuBuilder): MutableList<ExperimentContentItem> {
        val items = mutableListOf<ExperimentContentItem>()
        convertTo(items, menu)
        return items
    }

    @SuppressLint("RestrictedApi")
    private fun convertTo(items: MutableList<ExperimentContentItem>, menu: MenuBuilder) {
        menu.visibleItems.forEach { menuItemImpl ->
            when {
                menuItemImpl.hasSubMenu() -> {
                    if (items.isNotEmpty()) {
                        items.add(ExperimentContentSeparatorItem())
                    }
                    items.add(ExperimentContentHeaderItem(menuItemImpl.itemId, menuItemImpl.title))
                    convertTo(items, menuItemImpl.subMenu as MenuBuilder)
                }
                menuItemImpl.isCheckable -> {
                    items.add(
                        ExperimentContentSubHeaderItem(
                            menuItemImpl.itemId, menuItemImpl.title, menuItemImpl.isChecked
                        )
                    )
                }
                else -> {
                    items.add(
                        ExperimentContentActionItem(
                            menuItemImpl.itemId,
                            menuItemImpl.title,
                            menuItemImpl.titleCondensed,
                            needsNetwork = menuItemImpl.isChecked
                        )
                    )
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : ExperimentContentItem> Collection<ExperimentContentItem>.findItemById(id: Int) =
        first { id == it.id } as T

    fun Collection<ExperimentContentItem>.findIndexById(id: Int) = indexOfFirst { id == it.id }
}

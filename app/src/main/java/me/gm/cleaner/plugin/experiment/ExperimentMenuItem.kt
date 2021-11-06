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

/** Unified data model for all sorts of navigation menu items. */
interface ExperimentMenuItem

/** Separator items. */
data class ExperimentMenuSeparatorItem(
    val id: Int = View.generateViewId()
) : ExperimentMenuItem

/** Normal or header items. */
data class ExperimentMenuHeaderItem(
    val id: Int,
    var title: CharSequence?
) : ExperimentMenuItem

/** Normal or subheader items. */
data class ExperimentMenuSubHeaderItem(
    val id: Int,
    var content: CharSequence?,
    var checked: Boolean
) : ExperimentMenuItem

/** Action items. */
data class ExperimentMenuActionItem(
    val id: Int,
    var title: CharSequence?,
    var summary: CharSequence?,
    var listener: View.OnClickListener? = null
) : ExperimentMenuItem

object ExperimentMenuItems {

    /** Convert MenuItemImpl to ExperimentMenuItem. */
    @SuppressLint("RestrictedApi")
    fun forMenuBuilder(menu: MenuBuilder): List<ExperimentMenuItem> {
        val items = mutableListOf<ExperimentMenuItem>()
        menu.visibleItems.forEach { menuItemImpl ->
            if (items.isNotEmpty()) {
                items.add(ExperimentMenuSeparatorItem())
            }
            items.add(ExperimentMenuHeaderItem(menuItemImpl.itemId, menuItemImpl.title))
            if (menuItemImpl.hasSubMenu()) {
                (menuItemImpl.subMenu as MenuBuilder).visibleItems.forEach { subMenu ->
                    when {
                        subMenu.isCheckable -> items.add(
                            ExperimentMenuSubHeaderItem(
                                subMenu.itemId, subMenu.title, subMenu.isChecked
                            )
                        )
                        !subMenu.isCheckable -> items.add(
                            ExperimentMenuActionItem(subMenu.itemId, subMenu.title, null)
                        )
                    }
                }
            }
        }
        return items
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : ExperimentMenuItem> List<ExperimentMenuItem>.findItemById(id: Int): T = first {
        id == when (it) {
            is ExperimentMenuSeparatorItem -> it.id
            is ExperimentMenuHeaderItem -> it.id
            is ExperimentMenuSubHeaderItem -> it.id
            is ExperimentMenuActionItem -> it.id
            else -> throw IndexOutOfBoundsException()
        }
    } as T
}

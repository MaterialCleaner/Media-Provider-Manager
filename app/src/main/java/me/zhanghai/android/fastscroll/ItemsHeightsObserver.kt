/*
 * Copyright 2022 Green Mushroom
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

package me.zhanghai.android.fastscroll

import android.graphics.Canvas
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.gm.cleaner.plugin.ktx.getObjectField

open class ItemsHeightsObserver(list: RecyclerView, measureAllItemsOnStart: Boolean = true) :
    RecyclerView.AdapterDataObserver() {
    val itemsHeights: PrefixSumArrayList = PrefixSumArrayList()
    private val heightUndeterminedPositions: MutableList<Int> = mutableListOf()

    protected val adapter: RecyclerView.Adapter<RecyclerView.ViewHolder> = list.adapter!!
    protected val layoutManager: RecyclerView.LayoutManager = list.layoutManager!!

    /**
     * You should override it if you are not using [LinearLayoutManager].
     * @return Returns the adapter position of all visible view.
     */
    open val visibleItemPositions: Iterable<Int>
        get() {
            val layoutManager = layoutManager as LinearLayoutManager
            return layoutManager.findFirstVisibleItemPosition()..
                    layoutManager.findLastVisibleItemPosition()
        }

    private val recycler: RecyclerView.Recycler
            by lazy { list.getObjectField<RecyclerView.Recycler>() }

    private fun enforceItemOffset(position: Int): Int {
        var itemView = layoutManager.findViewByPosition(position)
        var newHolderCreated = false
        if (itemView == null) {
            itemView = recycler.getViewForPosition(position)
            newHolderCreated = true
            layoutManager.measureChildWithMargins(itemView, 0, 0)
        }
        try {
            return itemView.measuredHeight
        } finally {
            if (newHolderCreated) {
                recycler.recycleView(itemView)
            }
        }
    }

    /**
     * You should override it if you are not using [LinearLayoutManager].
     * @return Returns one item's height obtained in the easiest way.
     */
    open fun getOneItemOffset(): Int = itemsHeights.firstOrNull { it > 0 }
        ?: getVisibleItemOffset((layoutManager as LinearLayoutManager).findFirstVisibleItemPosition())

    /**
     * You can override it to get a better user experience if you know how to estimate the height
     * of the view at the specific position.
     * @param position The adapter position of the view we need to estimate its height.
     * @return Returns the height of the view at the specific position. Return `null` if you don't
     * know how to estimate the height of the view at the specific position.
     */
    open fun guessItemOffsetAt(position: Int): Int? = null

    protected fun getVisibleItemOffset(position: Int): Int {
        val itemView = layoutManager.findViewByPosition(position) ?: return -1
        return itemView.measuredHeight
    }

    private fun enforceAllItemsOffset(measureAllItems: Boolean) {
        val oneItemOffset = if (measureAllItems) {
            0
        } else {
            getOneItemOffset()
        }
        itemsHeights.clear()
        heightUndeterminedPositions.clear()
        val visibleItemPositions = visibleItemPositions
        for (i in 0 until adapter.itemCount) {
            itemsHeights += if (measureAllItems) {
                enforceItemOffset(i)
            } else {
                if (i in visibleItemPositions) {
                    getVisibleItemOffset(i)
                } else {
                    heightUndeterminedPositions += i
                    guessItemOffsetAt(i) ?: oneItemOffset
                }
            }
        }
    }

    override fun onChanged() {
        enforceAllItemsOffset(false)
    }

    override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
        for (i in positionStart until positionStart + itemCount) {
            heightUndeterminedPositions += i
        }
    }

    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
        for (i in 0 until heightUndeterminedPositions.size) {
            if (heightUndeterminedPositions[i] >= positionStart) {
                heightUndeterminedPositions[i] += itemCount
            }
        }
        val oneItemOffset = itemsHeights.firstOrNull { it > 0 } ?: 0
        for (i in positionStart until positionStart + itemCount) {
            // getVisibleItemOffset(i) can't get accurate result here, so we can only guess.
            val guessItemOffset = guessItemOffsetAt(i) ?: oneItemOffset
            itemsHeights.add(i, guessItemOffset)
            heightUndeterminedPositions += i
        }
    }

    override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
        repeat(itemCount) {
            itemsHeights.removeAt(positionStart)
        }
        for (i in positionStart until positionStart + itemCount) {
            heightUndeterminedPositions -= i
        }
        for (i in 0 until heightUndeterminedPositions.size) {
            if (heightUndeterminedPositions[i] > positionStart) {
                heightUndeterminedPositions[i] -= itemCount
            }
        }
    }

    override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
        // itemCount is always 1
        itemsHeights.add(toPosition, itemsHeights.removeAt(fromPosition))
        if (fromPosition > toPosition) {
            for (i in 0 until heightUndeterminedPositions.size) {
                when (heightUndeterminedPositions[i]) {
                    fromPosition -> heightUndeterminedPositions[i] = toPosition
                    in toPosition until fromPosition -> heightUndeterminedPositions[i]++
                }
            }
        } else if (fromPosition < toPosition) {
            for (i in 0 until heightUndeterminedPositions.size) {
                when (heightUndeterminedPositions[i]) {
                    fromPosition -> heightUndeterminedPositions[i] = toPosition
                    in fromPosition..toPosition -> heightUndeterminedPositions[i]--
                }
            }
        }
    }

    private fun updateAllVisibleItemsOffset() {
        if (heightUndeterminedPositions.isEmpty()) {
            return
        }
        for (position in visibleItemPositions) {
            if (heightUndeterminedPositions.remove(position)) {
                val offset = getVisibleItemOffset(position)
                itemsHeights[position] = offset
            }
        }
    }

    private fun itemHeightsInitialized(): Boolean = itemsHeights.any { it > 0 }

    init {
        list.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
                // added by onItemRangeInserted()
                if (!itemHeightsInitialized()) {
                    enforceAllItemsOffset(measureAllItemsOnStart)
                } else {
                    updateAllVisibleItemsOffset()
                }
            }
        })
    }
}

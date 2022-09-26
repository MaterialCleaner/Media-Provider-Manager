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

class ItemHeightsObserver(list: RecyclerView, private val measureAllItemsOnStart: Boolean = true) :
    RecyclerView.AdapterDataObserver() {
    private val _itemHeights = mutableListOf<Int>()
    private val heightUndeterminedPositions = mutableListOf<Int>()
    val itemHeights: List<Int>
        get() = _itemHeights
    var itemHeightsSum = 0
        private set
    private val adapter = list.adapter!!
    private val layoutManager = list.layoutManager as LinearLayoutManager
    private val visibleItemPositions: IntRange
        get() = layoutManager.findFirstVisibleItemPosition()..
                layoutManager.findLastVisibleItemPosition()

    private val recycler by lazy { list.getObjectField<RecyclerView.Recycler>() }
    private fun enforceItemOffset(position: Int): Int {
        var itemView = layoutManager.findViewByPosition(position)
        var newHolderCreated = false
        if (itemView == null) {
            itemView = recycler.getViewForPosition(position)
            newHolderCreated = true
            layoutManager.measureChildWithMargins(itemView, 0, 0)
        }
        try {
            return if (layoutManager.orientation == RecyclerView.VERTICAL) {
                itemView.measuredHeight
            } else {
                itemView.measuredWidth
            }
        } finally {
            if (newHolderCreated) {
                recycler.recycleView(itemView)
            }
        }
    }

    private fun guessItemOffset(): Int? = _itemHeights.firstOrNull { it > 0 }

    private fun getVisibleItemOffset(position: Int): Int {
        val itemView = layoutManager.findViewByPosition(position) ?: return -1
        return if (layoutManager.orientation == RecyclerView.VERTICAL) {
            itemView.measuredHeight
        } else {
            itemView.measuredWidth
        }
    }

    private fun enforceAllItemsOffset(measureAllItems: Boolean) {
        val guessItemOffset = if (measureAllItems) {
            0
        } else {
            guessItemOffset() ?: getVisibleItemOffset(layoutManager.findFirstVisibleItemPosition())
        }
        _itemHeights.clear()
        heightUndeterminedPositions.clear()
        val visibleItemPositions = visibleItemPositions
        for (i in 0 until adapter.itemCount) {
            _itemHeights += if (measureAllItems) {
                enforceItemOffset(i)
            } else {
                if (i in visibleItemPositions) {
                    getVisibleItemOffset(i)
                } else {
                    heightUndeterminedPositions += i
                    guessItemOffset
                }
            }
        }
        itemHeightsSum = _itemHeights.sum()
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
        // getVisibleItemOffset(i) can't get accurate result here, so we always use guessItemOffset().
        val guessItemOffset = guessItemOffset() ?: 0
        for (i in positionStart until positionStart + itemCount) {
            _itemHeights.add(i, guessItemOffset)
            itemHeightsSum += guessItemOffset
            heightUndeterminedPositions += i
        }
    }

    override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
        repeat(itemCount) {
            itemHeightsSum -= _itemHeights.removeAt(positionStart)
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
        _itemHeights.add(toPosition, _itemHeights.removeAt(fromPosition))
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
                itemHeightsSum = itemHeightsSum - _itemHeights[position] + offset
                _itemHeights[position] = offset
            }
        }
    }

    private fun itemHeightsInitialized(): Boolean = _itemHeights.any { it > 0 }

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

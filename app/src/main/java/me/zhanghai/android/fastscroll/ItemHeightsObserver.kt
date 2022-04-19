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
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import me.gm.cleaner.plugin.ktx.getObjectField

class ItemHeightsObserver(list: RecyclerView) : RecyclerView.AdapterDataObserver() {
    val itemHeights = mutableListOf<Int>()
    var itemHeightsSum = 0
        private set
    private val recycler = list.getObjectField<RecyclerView.Recycler>()
    private val adapter = list.adapter!!
    private val layoutManager = list.layoutManager as LinearLayoutManager

    private fun getItemOffset(itemView: View): Int {
        layoutManager.measureChildWithMargins(itemView, 0, 0)
        return if (layoutManager.orientation == RecyclerView.VERTICAL) {
            itemView.measuredHeight
        } else {
            itemView.measuredWidth
        }
    }

    override fun onChanged() {
        itemHeights.clear()
        for (i in 0 until adapter.itemCount) {
            val itemView = recycler.getViewForPosition(i)
            itemHeights += getItemOffset(itemView)
            recycler.recycleView(itemView)
        }
        itemHeightsSum = itemHeights.sum()
    }

    override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
        for (i in positionStart until positionStart + itemCount) {
            val itemView = recycler.getViewForPosition(i)
            val itemOffset = getItemOffset(itemView)
            itemHeightsSum = itemHeightsSum - itemHeights[i] + itemOffset
            itemHeights[i] = itemOffset
            recycler.recycleView(itemView)
        }
    }

    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
        try {
            for (i in positionStart until positionStart + itemCount) {
                val itemView = recycler.getViewForPosition(i)
                val itemOffset = getItemOffset(itemView)
                itemHeights.add(i, itemOffset)
                itemHeightsSum += itemOffset
                recycler.recycleView(itemView)
            }
        } catch (e: IndexOutOfBoundsException) {
            // will fallback to onChanged()
        }
    }

    override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
        for (i in 0 until itemCount) {
            itemHeightsSum -= itemHeights.removeAt(positionStart)
        }
    }

    override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
        // itemCount is always 1
        itemHeights.add(toPosition, itemHeights.removeAt(fromPosition))
    }

    init {
        list.addItemDecoration(object : ItemDecoration() {
            override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
                if (itemHeights.size != adapter.itemCount) {
                    onChanged()
                }
            }
        })
    }
}

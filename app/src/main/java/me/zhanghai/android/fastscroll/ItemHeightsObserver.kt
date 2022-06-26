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

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.gm.cleaner.plugin.ktx.LayoutCompleteAwareGridLayoutManager
import me.gm.cleaner.plugin.ktx.getObjectField
import java.util.concurrent.LinkedBlockingQueue

class ItemHeightsObserver(list: RecyclerView) : RecyclerView.AdapterDataObserver() {
    private val _itemHeights = mutableListOf<Int>()
    val itemHeights: List<Int>
        get() = _itemHeights
    var itemHeightsSum = 0
        private set
    private val recycler = list.getObjectField<RecyclerView.Recycler>()
    private val adapter = list.adapter!!
    private val layoutManager = list.layoutManager as LinearLayoutManager
    private val queue = LinkedBlockingQueue<Runnable>()

    private fun getItemOffset(position: Int): Int {
        if (position >= adapter.itemCount) {
            return 0
        }
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

    override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
        queue.add {
            for (i in positionStart until positionStart + itemCount) {
                val itemOffset = getItemOffset(i)
                itemHeightsSum = itemHeightsSum - _itemHeights[i] + itemOffset
                _itemHeights[i] = itemOffset
            }
        }
    }

    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
        queue.add {
            for (i in positionStart until positionStart + itemCount) {
                val itemOffset = getItemOffset(i)
                _itemHeights.add(i, itemOffset)
                itemHeightsSum += itemOffset
            }
        }
    }

    override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
        queue.add {
            for (i in 0 until itemCount) {
                itemHeightsSum -= _itemHeights.removeAt(positionStart)
            }
        }
    }

    override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
        queue.add {
            // itemCount is always 1
            _itemHeights.add(toPosition, _itemHeights.removeAt(fromPosition))
        }
    }

    init {
        (layoutManager as? LayoutCompleteAwareGridLayoutManager)?.addOnLayoutCompletedListener {
            if (queue.isNotEmpty()) {
                val iterator = queue.iterator()
                while (iterator.hasNext()) {
                    iterator.next().run()
                    iterator.remove()
                }
            }
        }
    }
}

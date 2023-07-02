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

import android.graphics.Rect
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

open class PreciseRecyclerViewHelper(
    private val list: RecyclerView,
    popupTextProvider: PopupTextProvider? = null,
    measureAllItemsOnStart: Boolean = true,
    val observer: ItemsHeightsObserver = ItemsHeightsObserver(list, measureAllItemsOnStart)
) : NoInterceptionRecyclerViewHelper(list, popupTextProvider) {
    private val layoutManager: LinearLayoutManager = list.layoutManager as LinearLayoutManager
    private val mTempRect: Rect = Rect()
    private val isGridLayoutManager: Boolean =
        layoutManager is GridLayoutManager && layoutManager.spanCount > 1

    init {
        list.adapter!!.registerAdapterDataObserver(observer)
    }

    private fun calcPrefixSumForGridLayoutManager(
        requestSize: Int, prefixSum: MutableList<Int> = mutableListOf()
    ): MutableList<Int> {
        val spanSizeLookup = (layoutManager as GridLayoutManager).spanSizeLookup
        val spanCount = layoutManager.spanCount
        val chunkHeights = mutableListOf<Int>()
        while (prefixSum.size < requestSize) {
            chunkHeights.clear()
            var remainingSpan = spanCount
            while (true) {
                val position = prefixSum.size + chunkHeights.size
                if (position >= observer.itemsHeights.size) {
                    break
                }
                val spanSize = spanSizeLookup.getSpanSize(position)
                remainingSpan -= spanSize
                if (remainingSpan < 0) {
                    break // item did not fit into this row or column
                }
                val itemHeight = observer.itemsHeights[position]
                chunkHeights.add(itemHeight)
            }
            val newSum = (prefixSum.lastOrNull() ?: 0) + chunkHeights.max()
            repeat(chunkHeights.size) { // possibly more than requestSize
                prefixSum.add(newSum)
            }
        }
        return prefixSum
    }

    override fun getScrollRange(): Int = list.paddingTop + list.paddingBottom +
            if (!isGridLayoutManager) {
                observer.itemsHeights.sum()
            } else {
                calcPrefixSumForGridLayoutManager(observer.itemsHeights.size).lastOrNull() ?: 0
            }

    override fun getScrollOffset(): Int {
        val firstItemPosition = layoutManager.getPosition(list.getChildAt(0))
        if (firstItemPosition == RecyclerView.NO_POSITION ||
            firstItemPosition >= observer.itemsHeights.size
        ) {
            return 0
        }
        val itemsHeightsSum = if (!isGridLayoutManager) {
            observer.itemsHeights.query(0, firstItemPosition)
        } else {
            calcPrefixSumForGridLayoutManager(firstItemPosition).lastOrNull() ?: 0
        }
        return list.paddingTop - getFirstItemOffset() + itemsHeightsSum
    }

    private fun getFirstItemOffset(): Int {
        if (list.childCount == 0) {
            return 0
        }
        val itemView = list.getChildAt(0)
        list.getDecoratedBoundsWithMargins(itemView, mTempRect)
        return mTempRect.top
    }

    override fun scrollTo(offset: Int) {
        // Stop any scroll in progress for RecyclerView.
        list.stopScroll()
        val offset = offset - list.paddingTop
        var sum = 0
        var firstItemPosition = 0
        if (!isGridLayoutManager) {
            for (i in 0 until observer.itemsHeights.size) {
                val next = sum + observer.itemsHeights[i]
                if (next > offset) break
                sum = next
                firstItemPosition++
            }
        } else {
            val prefixSum = mutableListOf<Int>()
            for (i in 0 until observer.itemsHeights.size) {
                val next = calcPrefixSumForGridLayoutManager(firstItemPosition + 1, prefixSum)
                    .lastOrNull() ?: 0
                if (next > offset) break
                sum = next
                firstItemPosition++
            }
        }
        val firstItemTop = sum - offset
        scrollToPositionWithOffset(firstItemPosition, firstItemTop)
    }

    private fun scrollToPositionWithOffset(position: Int, offset: Int) {
        // LinearLayoutManager actually takes offset from paddingTop instead of top of RecyclerView.
        val offset = offset - list.paddingTop
        layoutManager.scrollToPositionWithOffset(position, offset)
    }
}

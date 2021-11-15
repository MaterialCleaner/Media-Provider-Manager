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

package me.gm.cleaner.plugin.util

import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.view.doOnPreDraw
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

fun <T, VH : RecyclerView.ViewHolder> ListAdapter<T, VH>.submitListKeepPosition(
    list: List<T>, recyclerView: RecyclerView, commitCallback: Runnable? = null
) {
    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
    val position = layoutManager.findFirstVisibleItemPosition()
    if (position == RecyclerView.NO_POSITION) {
        submitList(list)
    } else {
        val rect = Rect()
        recyclerView.getDecoratedBoundsWithMargins(
            layoutManager.findViewByPosition(position)!!, rect
        )
        submitList(list) {
            layoutManager.scrollToPositionWithOffset(position, rect.top - recyclerView.paddingTop)
            commitCallback?.run()
        }
    }
}

class DividerDecoration(private val list: RecyclerView) : RecyclerView.ItemDecoration() {
    private lateinit var divider: Drawable
    private var dividerHeight = 0
    private var allowDividerAfterLastItem = true

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (!::divider.isInitialized) {
            return
        }
        val childCount = parent.childCount
        val width = parent.width
        for (childViewIndex in 0 until childCount) {
            val view = parent.getChildAt(childViewIndex)
            if (shouldDrawDividerBelow(view, parent)) {
                val top = view.y.toInt() + view.height
                divider.setBounds(0, top, width, top + dividerHeight)
                divider.setTint(parent.context.colorControlHighlight)
                divider.draw(c)
            }
        }
    }

    override fun getItemOffsets(
        outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
    ) {
        if (shouldDrawDividerBelow(view, parent)) {
            outRect.bottom = dividerHeight
        }
    }

    private fun shouldDrawDividerBelow(view: View, parent: RecyclerView): Boolean {
        val holder = parent.getChildViewHolder(view)
        val dividerAllowedBelow = holder is DividerViewHolder && holder.isDividerAllowedBelow
        if (dividerAllowedBelow) {
            return true
        }
        var nextAllowed = allowDividerAfterLastItem
        val index = parent.indexOfChild(view)
        if (index < parent.childCount - 1) {
            val nextView = parent.getChildAt(index + 1)
            val nextHolder = parent.getChildViewHolder(nextView)
            nextAllowed = nextHolder is DividerViewHolder && nextHolder.isDividerAllowedAbove
        }
        return nextAllowed
    }

    fun setDivider(divider: Drawable) {
        dividerHeight = divider.intrinsicHeight
        this.divider = divider
        list.invalidateItemDecorations()
    }

    fun setDividerHeight(dividerHeight: Int) {
        this.dividerHeight = dividerHeight
        list.invalidateItemDecorations()
    }

    fun setAllowDividerAfterLastItem(allowDividerAfterLastItem: Boolean) {
        this.allowDividerAfterLastItem = allowDividerAfterLastItem
    }
}

abstract class DividerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    /**
     * Dividers are only drawn between items if both items allow it, or above the first and below
     * the last item if that item allows it.
     *
     * @return `true` if dividers are allowed above this item
     */
    var isDividerAllowedAbove = false

    /**
     * Dividers are only drawn between items if both items allow it, or above the first and below
     * the last item if that item allows it.
     *
     * @return `true` if dividers are allowed below this item
     */
    var isDividerAllowedBelow = false
}

fun RecyclerView.addLiftOnScrollListener(callback: (isLifted: Boolean) -> Unit) {
    addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            val layoutManager = layoutManager
            require(layoutManager is LinearLayoutManager)
            val firstViewHolder = findViewHolderForAdapterPosition(0)
            callback(
                adapter?.itemCount != 0 && !layoutManager.isItemCompletelyVisible(firstViewHolder)
            )
        }
    })
}

fun RecyclerView.overScrollIfContentScrollsPersistent(supportsChangeAnimations: Boolean = true) {
    doOnPreDraw {
        overScrollIfContentScrolls()
    }
    addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> overScrollIfContentScrolls() }
    itemAnimator = object : DefaultItemAnimator() {
        init {
            this.supportsChangeAnimations = supportsChangeAnimations
        }

        override fun onAnimationFinished(viewHolder: RecyclerView.ViewHolder) {
            super.onAnimationFinished(viewHolder)
            overScrollIfContentScrolls()
        }
    }
}

fun RecyclerView.overScrollIfContentScrolls() {
    overScrollMode = if (isContentScrolls(this)) {
        View.OVER_SCROLL_IF_CONTENT_SCROLLS
    } else {
        View.OVER_SCROLL_NEVER
    }
}

private fun isContentScrolls(list: RecyclerView): Boolean {
    val layoutManager = list.layoutManager
    if (layoutManager == null || list.adapter == null || list.adapter?.itemCount == 0) {
        return false
    }
    if (layoutManager !is LinearLayoutManager) {
        return true
    }
    val firstViewHolder = list.findViewHolderForAdapterPosition(0)
    if (!layoutManager.isItemCompletelyVisible(firstViewHolder)) {
        return true
    }
    val lastViewHolder = list.findViewHolderForAdapterPosition(layoutManager.itemCount - 1)
    return !layoutManager.isItemCompletelyVisible(lastViewHolder)
}

fun LinearLayoutManager.isItemCompletelyVisible(viewHolder: RecyclerView.ViewHolder?): Boolean {
    viewHolder ?: return false
    val viewBoundsCheck = ViewBoundsCheck(if (orientation == RecyclerView.HORIZONTAL) {
        object : ViewBoundsCheck.Callback {
            override fun getChildAt(index: Int): View = getChildAt(index)
            override fun getParentStart() = paddingLeft
            override fun getParentEnd() = width - paddingRight

            override fun getChildStart(view: View): Int {
                val params = view.layoutParams as RecyclerView.LayoutParams
                return getDecoratedLeft(view) - params.leftMargin
            }

            override fun getChildEnd(view: View): Int {
                val params = view.layoutParams as RecyclerView.LayoutParams
                return getDecoratedRight(view) + params.rightMargin
            }
        }
    } else {
        object : ViewBoundsCheck.Callback {
            override fun getChildAt(index: Int): View = getChildAt(index)
            override fun getParentStart() = paddingTop
            override fun getParentEnd() = height - paddingBottom

            override fun getChildStart(view: View): Int {
                val params = view.layoutParams as RecyclerView.LayoutParams
                return getDecoratedTop(view) - params.topMargin
            }

            override fun getChildEnd(view: View): Int {
                val params = view.layoutParams as RecyclerView.LayoutParams
                return getDecoratedBottom(view) + params.bottomMargin
            }
        }
    })
    val completelyVisiblePreferredBoundsFlag = ViewBoundsCheck.FLAG_CVS_GT_PVS or
            ViewBoundsCheck.FLAG_CVS_EQ_PVS or ViewBoundsCheck.FLAG_CVE_LT_PVE or
            ViewBoundsCheck.FLAG_CVE_EQ_PVE
    return viewBoundsCheck.isItemViewWithinBoundFlags(
        viewHolder.itemView, completelyVisiblePreferredBoundsFlag
    )
}

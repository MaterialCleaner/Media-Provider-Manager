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

package me.gm.cleaner.plugin.ktx

import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.view.doOnPreDraw
import androidx.core.view.forEach
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import me.zhanghai.android.fastscroll.FastScroller

private val rect = Rect()

fun <T, VH : RecyclerView.ViewHolder> ListAdapter<T, VH>.submitListKeepPosition(
    list: List<T>, recyclerView: RecyclerView, commitCallback: Runnable? = null
) {
    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
    val position = layoutManager.findFirstVisibleItemPosition()
    if (position == RecyclerView.NO_POSITION) {
        submitList(list, commitCallback)
    } else {
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
        val width = parent.width
        parent.forEach { view ->
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
    if (!list.isItemCompletelyVisible(0)) {
        return true
    }
    return !list.isItemCompletelyVisible(layoutManager.itemCount - 1)
}

fun RecyclerView.isItemCompletelyVisible(position: Int): Boolean {
    val vh = findViewHolderForAdapterPosition(position)
    vh ?: return false
    val layoutManager = layoutManager!!
    return layoutManager.isViewPartiallyVisible(vh.itemView, true, true)
}

fun RecyclerView.isItemCompletelyInvisible(position: Int): Boolean {
    val vh = findViewHolderForAdapterPosition(position)
    vh ?: return true
    val layoutManager = layoutManager!!
    return !layoutManager.isViewPartiallyVisible(vh.itemView, true, true) &&
            !layoutManager.isViewPartiallyVisible(vh.itemView, false, false)
}

fun View.fitsSystemWindowInsets(fastScroller: FastScroller? = null) {
    val paddingLeft = paddingLeft
    val paddingTop = paddingTop
    val paddingRight = paddingRight
    val paddingBottom = paddingBottom
    setOnApplyWindowInsetsListener { view, insets ->
        view.setPadding(
            paddingLeft, paddingTop + insets.systemWindowInsetTop,
            paddingRight, paddingBottom + insets.systemWindowInsetBottom
        )
        fastScroller?.setPadding(
            0, paddingTop + insets.systemWindowInsetTop,
            0, paddingBottom + insets.systemWindowInsetBottom
        )
        insets
    }
}

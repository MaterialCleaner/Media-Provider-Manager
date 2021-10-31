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
import android.view.MotionEvent
import android.view.View
import androidx.core.view.doOnPreDraw
import androidx.recyclerview.widget.*
import me.zhanghai.android.fastscroll.FastScroller
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import me.zhanghai.android.fastscroll.PopupTextProvider
import me.zhanghai.android.fastscroll.Predicate
import kotlin.math.absoluteValue

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

fun RecyclerView.initFastScroller() {
    FastScrollerBuilder(this)
        .setViewHelper(PiecewiseRecyclerViewHelper(this))
        .useMd2Style()
        .build()
}

class PiecewiseRecyclerViewHelper(private val list: RecyclerView) : FastScroller.ViewHelper {
    private val mTempRect = Rect()

    override fun addOnPreDrawListener(onPreDraw: Runnable) {
        list.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
                onPreDraw.run()
            }
        })
    }

    override fun addOnScrollChangedListener(onScrollChanged: Runnable) {
        list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                onScrollChanged.run()
            }
        })
    }

    override fun addOnTouchEventListener(onTouchEvent: Predicate<MotionEvent>) {
        list.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(
                recyclerView: RecyclerView, event: MotionEvent
            ) = onTouchEvent.test(event)

            override fun onTouchEvent(recyclerView: RecyclerView, event: MotionEvent) {
                onTouchEvent.test(event)
            }
        })
    }

    override fun getScrollRange() =
        list.computeVerticalScrollRange() + list.paddingTop + list.paddingBottom

    override fun getScrollOffset() = list.computeVerticalScrollOffset()

    override fun scrollTo(offset: Int) {
        // Stop any scroll in progress for RecyclerView.
        list.stopScroll()
        val delta = offset - scrollOffset
        if (delta.absoluteValue <= getItemHeight() || getItemCount() < CUTOFF) {
            list.scrollBy(0, delta)
            return
        }
        val scrolledOffset = offset - list.paddingTop
        val itemHeight = getItemHeight()
        // firstItemPosition should be non-negative even if paddingTop is greater than item height.
        val firstItemPosition = 0.coerceAtLeast(scrolledOffset / itemHeight)
        val firstItemTop = firstItemPosition * itemHeight - scrolledOffset
        scrollToPositionWithOffset(firstItemPosition, firstItemTop)
    }

    private fun getItemCount(): Int {
        val linearLayoutManager = getVerticalLinearLayoutManager() ?: return 0
        var itemCount = linearLayoutManager.itemCount
        if (itemCount == 0) {
            return 0
        }
        if (linearLayoutManager is GridLayoutManager) {
            itemCount = (itemCount - 1) / linearLayoutManager.spanCount + 1
        }
        return itemCount
    }

    private fun getItemHeight(): Int {
        if (list.childCount == 0) {
            return 0
        }
        // Minus one to ensure we can drag to the bottom of the recycler view.
        return list.computeVerticalScrollRange() / getItemCount() - 1
    }

    private fun getFirstItemPosition(): Int {
        var position = getFirstItemAdapterPosition()
        val linearLayoutManager =
            getVerticalLinearLayoutManager() ?: return RecyclerView.NO_POSITION
        if (linearLayoutManager is GridLayoutManager) {
            position /= linearLayoutManager.spanCount
        }
        return position
    }

    private fun getFirstItemAdapterPosition(): Int {
        if (list.childCount == 0) {
            return RecyclerView.NO_POSITION
        }
        val itelist: View = list.getChildAt(0)
        val linearLayoutManager =
            getVerticalLinearLayoutManager() ?: return RecyclerView.NO_POSITION
        return linearLayoutManager.getPosition(itelist)
    }

    private fun getFirstItemOffset(): Int {
        if (list.childCount == 0) {
            return RecyclerView.NO_POSITION
        }
        val itelist: View = list.getChildAt(0)
        list.getDecoratedBoundsWithMargins(itelist, mTempRect)
        return mTempRect.top
    }

    private fun scrollToPositionWithOffset(position: Int, offset: Int) {
        var position = position
        var offset = offset
        val linearLayoutManager = getVerticalLinearLayoutManager() ?: return
        if (linearLayoutManager is GridLayoutManager) {
            position *= linearLayoutManager.spanCount
        }
        // LinearLayoutManager actually takes offset from paddingTop instead of top of RecyclerView.
        offset -= list.paddingTop
        linearLayoutManager.scrollToPositionWithOffset(position, offset)
    }

    @JvmName("getVerticalLinearLayoutManager1")
    private fun getVerticalLinearLayoutManager(): LinearLayoutManager? {
        val linearLayoutManager = list.layoutManager as? LinearLayoutManager ?: return null
        return if (linearLayoutManager.orientation != RecyclerView.VERTICAL) null
        else linearLayoutManager
    }

    override fun getPopupText(): String? {
        val popupTextProvider = list.adapter as? PopupTextProvider ?: return null
        val position = getItemAdapterPositionForPopup()
        return if (position == RecyclerView.NO_POSITION) null
        else popupTextProvider.getPopupText(position)
    }

    private fun getItemAdapterPositionForPopup(): Int {
        if (list.childCount == 0) {
            return RecyclerView.NO_POSITION
        }
        return verticalLinearLayoutManager?.findFirstCompletelyVisibleItemPosition()
            ?: RecyclerView.NO_POSITION
    }

    private val verticalLinearLayoutManager: LinearLayoutManager?
        get() {
            val layoutManager = list.layoutManager as? LinearLayoutManager ?: return null
            return if (layoutManager.orientation != RecyclerView.VERTICAL) null
            else layoutManager
        }

    companion object {
        // As for RecyclerView, there are several ways to scroll programmatically.
        // The scrollBy way is accurate, but the recycler view needs to render all the items sliding by.
        // The scrollToPositionWithOffset way, however, is exactly on the contrary.
        // Therefore, I set up a cut-off point to let each way make use of its strengths.
        private const val CUTOFF = 300
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

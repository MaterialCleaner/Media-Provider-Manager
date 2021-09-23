package me.gm.cleaner.plugin.util

import android.graphics.Canvas
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.zhanghai.android.fastscroll.FastScroller
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import me.zhanghai.android.fastscroll.PopupTextProvider
import me.zhanghai.android.fastscroll.Predicate

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
            ): Boolean = onTouchEvent.test(event)

            override fun onTouchEvent(recyclerView: RecyclerView, event: MotionEvent) {
                onTouchEvent.test(event)
            }
        })
    }

    override fun getScrollRange(): Int =
        list.computeVerticalScrollRange() + list.paddingTop + list.paddingBottom

    override fun getScrollOffset(): Int = list.computeVerticalScrollOffset()

    override fun scrollTo(offset: Int) {
        if (getItemCount() < CUTOFF) {
            list.stopScroll()

            val oldOffset = scrollOffset
            list.scrollBy(0, offset - oldOffset)
            return
        }
        // Stop any scroll in progress for RecyclerView.
        var offset = offset
        list.stopScroll()
        offset -= list.paddingTop
        val itemHeight: Int = getItemHeight()
        // firstItemPosition should be non-negative even if paddingTop is greater than item height.
        val firstItemPosition = 0.coerceAtLeast(offset / itemHeight)
        val firstItemTop = firstItemPosition * itemHeight - offset
        scrollToPositionWithOffset(firstItemPosition, firstItemTop)
    }

    private fun getItemCount(): Int {
        val linearLayoutManager: LinearLayoutManager = getVerticalLinearLayoutManager() ?: return 0
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
        val linearLayoutManager: LinearLayoutManager =
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
        val linearLayoutManager: LinearLayoutManager =
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
        val linearLayoutManager: LinearLayoutManager = getVerticalLinearLayoutManager() ?: return
        if (linearLayoutManager is GridLayoutManager) {
            position *= linearLayoutManager.spanCount
        }
        // LinearLayoutManager actually takes offset from paddingTop instead of top of RecyclerView.
        offset -= list.paddingTop
        linearLayoutManager.scrollToPositionWithOffset(position, offset)
    }

    @JvmName("getVerticalLinearLayoutManager1")
    private fun getVerticalLinearLayoutManager(): LinearLayoutManager? {
        val layoutManager: RecyclerView.LayoutManager =
            list.layoutManager as? LinearLayoutManager ?: return null
        val linearLayoutManager = layoutManager as LinearLayoutManager
        return if (linearLayoutManager.orientation != RecyclerView.VERTICAL) {
            null
        } else linearLayoutManager
    }

    override fun getPopupText(): String? {
        val adapter = list.adapter
        if (adapter !is PopupTextProvider) {
            return null
        }
        val popupTextProvider = adapter as PopupTextProvider
        val position = getItemAdapterPositionForPopup()
        return if (position == RecyclerView.NO_POSITION) {
            null
        } else popupTextProvider.getPopupText(position)
    }

    private fun getItemAdapterPositionForPopup(): Int {
        if (list.childCount == 0) {
            return RecyclerView.NO_POSITION
        }
        return verticalLinearLayoutManager?.findFirstCompletelyVisibleItemPosition()
            ?: return RecyclerView.NO_POSITION
    }

    private val verticalLinearLayoutManager: LinearLayoutManager?
        get() {
            val layoutManager = list.layoutManager as? LinearLayoutManager ?: return null
            return if (layoutManager.orientation != RecyclerView.VERTICAL) {
                null
            } else layoutManager
        }

    companion object {
        // As for RecyclerView, there are several ways to scroll programmatically.
        // The scrollBy way is accurate, but the recycler view needs to render all the items sliding by.
        // The scrollToPositionWithOffset way, however, is exactly on the contrary.
        // Therefore, I set up a cut-off point to let each way make use of its strengths.
        private const val CUTOFF = 300
    }
}

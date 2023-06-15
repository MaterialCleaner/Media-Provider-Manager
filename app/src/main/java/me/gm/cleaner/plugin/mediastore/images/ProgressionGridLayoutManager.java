/*
 * Copyright 2023 Green Mushroom
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.gm.cleaner.plugin.mediastore.images;

import android.animation.TimeInterpolator;
import android.content.Context;
import android.graphics.Rect;
import android.util.SparseArray;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.function.Consumer;

/**
 * A {@link RecyclerView.LayoutManager} implementation that lays out items in a grid,
 * and supports animate between different spans.
 */
public class ProgressionGridLayoutManager extends GridLayoutManager {

    private static final boolean DEBUG = false;
    private static final String TAG = "ProgressionGridLayoutManager";

    int mLastSpanCount;
    boolean mLayoutInfoStale = false;
    SparseArray<Rect> mLastLayoutInfo = new SparseArray<>();
    SparseArray<Rect> mCurLayoutInfo = new SparseArray<>();

    @FloatRange(from = 0F, to = 1F)
    float mProgress = 1F;
    @NonNull
    TimeInterpolator mInterpolator = new FastOutSlowInInterpolator();

    public ProgressionGridLayoutManager(Context context, int spanCount) {
        super(context, spanCount);
    }

    public ProgressionGridLayoutManager(Context context, int spanCount,
                                        @RecyclerView.Orientation int orientation,
                                        boolean reverseLayout) {
        super(context, spanCount, orientation, reverseLayout);
    }

    private static class VisibleChild {
        final int index;
        final View child;

        private VisibleChild(int index, View child) {
            this.index = index;
            this.child = child;
        }
    }

    private void forEachIndexed(Consumer<VisibleChild> action) {
        final int fromIndex = findFirstVisibleItemPosition();
        if (fromIndex == RecyclerView.NO_POSITION) {
            return;
        }
        final int toIndex = findLastVisibleItemPosition();
        for (int i = fromIndex; i <= toIndex; i++) {
            final View child = findViewByPosition(i);
            assert child != null;
            action.accept(new VisibleChild(i, child));
        }
    }

    void layoutInfoSnapshot(SparseArray<Rect> layoutInfo) {
        layoutInfo.clear();
        forEachIndexed(visibleChild -> {
            final int i = visibleChild.index;
            final View child = visibleChild.child;

            final Rect rect = new Rect();
            child.getHitRect(rect);
            layoutInfo.put(i, rect);
        });
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        // Note that this method is not just called when spanCount change.
        // We shall check the state carefully.
        if (mLayoutInfoStale) {
            layoutInfoSnapshot(mLastLayoutInfo);
        }
        super.onLayoutChildren(recycler, state);
    }

    @Override
    public void onLayoutCompleted(RecyclerView.State state) {
        super.onLayoutCompleted(state);
        // Note that this method is not just called when spanCount change.
        // We shall check the state carefully.
        if (mLayoutInfoStale) {
            layoutInfoSnapshot(mCurLayoutInfo);
            mLayoutInfoStale = false;
            if (mLastSpanCount != DEFAULT_SPAN_COUNT) {
                setProgress(0F);
            }
        }
    }

    @Override
    public boolean canScrollVertically() {
        return getProgress() == 1F && super.canScrollVertically();
    }

    private float getInterpolatedProgress() {
        return mInterpolator.getInterpolation(getProgress());
    }

    void mockLayout(@NonNull View child, int left, int top, int right, int bottom,
                    int lastLeft, int lastTop, int lastRight, int lastBottom) {
        final float progress = getInterpolatedProgress();

        final int lastWidth = lastRight - lastLeft;
        final int lastHeight = lastBottom - lastTop;
        final int width = right - left;
        final int height = bottom - top;
        child.setScaleX((lastWidth + (width - lastWidth) * progress) / width);
        child.setScaleY((lastHeight + (height - lastHeight) * progress) / height);

        final float lastHorizontalCenter = (lastRight + lastLeft) >> 1;
        final float lastVerticalCenter = (lastBottom + lastTop) >> 1;
        final float horizontalCenter = (right + left) >> 1;
        final float verticalCenter = (bottom + top) >> 1;
        child.setTranslationX((lastHorizontalCenter - horizontalCenter) * (1 - progress));
        child.setTranslationY((lastVerticalCenter - verticalCenter) * (1 - progress));
    }

    @Override
    public void setSpanCount(int spanCount) {
        if (spanCount == getSpanCount()) {
            return;
        }
        if (getProgress() != 1F &&
                // ensure not called by the constructor
                getSpanCount() != DEFAULT_SPAN_COUNT) {
            throw new IllegalStateException(
                    "Must finish the previous animation first before setting a new span count");
        }
        mLastSpanCount = getSpanCount();
        mLayoutInfoStale = true;
        super.setSpanCount(spanCount);
    }

    public float getProgress() {
        return mProgress;
    }

    public void setProgress(@FloatRange(from = 0F, to = 1F) float progress) {
        if (progress == mProgress) {
            return;
        }
        if (mLayoutInfoStale) {
            return;
        }
        mProgress = progress;
        final boolean setAlphaNeeded = mLastSpanCount < getSpanCount();

        forEachIndexed(visibleChild -> {
            final int i = visibleChild.index;
            final View child = visibleChild.child;

            final Rect last = mLastLayoutInfo.get(i);
            if (last != null) {
                final Rect cur = mCurLayoutInfo.get(i);
                if (cur == null) {
                    throw new IllegalStateException("Are you scrolling?");
                }
                mockLayout(child, cur.left, cur.top, cur.right, cur.bottom,
                        last.left, last.top, last.right, last.bottom
                );
            } else if (setAlphaNeeded) {
                final float interpolatedProgress = getInterpolatedProgress();
                child.setAlpha(interpolatedProgress);
            }
        });
    }

    public TimeInterpolator getInterpolator() {
        return mInterpolator;
    }

    public void setInterpolator(@Nullable TimeInterpolator i) {
        mInterpolator = i != null ? i : new LinearInterpolator();
    }
}

/*
 * Copyright 2021 Green Mushroom
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

package androidx.recyclerview.widget;

import android.animation.TimeInterpolator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

/**
 * A {@link RecyclerView.LayoutManager} implementation that lays out items in a grid,
 * and supports animate between different spans.
 * <p/>
 * Note that its robustness is poor and it is only designed for this project,
 * you probably can't use it directly in your project.
 */
public class ProgressionGridLayoutManager extends OverridableGridLayoutManager {

    private static final boolean DEBUG = false;
    private static final String TAG = "ProgressionGridLayoutManager";
    int mLastSpanCount;
    int[] mLastCachedBorders;
    public static final int INVALID_ANCHOR_LINE = -1;
    int mPrevAnchorLine = INVALID_ANCHOR_LINE;
    @FloatRange(from = 0F, to = 1F)
    float mProgress = 1F;
    @NonNull
    TimeInterpolator mInterpolator = new FastOutSlowInInterpolator();

    @SuppressLint("WrongConstant")
    public ProgressionGridLayoutManager(Context context, int spanCount) {
        this(context, spanCount, RecyclerView.DEFAULT_ORIENTATION, false);
    }

    public ProgressionGridLayoutManager(Context context, int spanCount,
                                        @RecyclerView.Orientation int orientation,
                                        boolean reverseLayout) {
        super(context, spanCount, orientation, reverseLayout);
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        if (mOrientation == HORIZONTAL) {
            return new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
        } else {
            return new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Override
    public RecyclerView.LayoutParams generateLayoutParams(Context c, AttributeSet attrs) {
        return new LayoutParams(c, attrs);
    }

    @Override
    public RecyclerView.LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            return new LayoutParams((ViewGroup.MarginLayoutParams) lp);
        } else {
            return new LayoutParams(lp);
        }
    }

    @Override
    public boolean checkLayoutParams(RecyclerView.LayoutParams lp) {
        return lp instanceof LayoutParams;
    }

    @Override
    public void setSpanCount(int spanCount) {
        if (spanCount == mSpanCount) {
            return;
        } else {
            mLastSpanCount = mSpanCount;
            mLastCachedBorders = mCachedBorders;
            mProgress = 0F;
        }
        mPendingSpanCountChange = true;
        if (spanCount < 1) {
            throw new IllegalArgumentException("Span count should be at least 1. Provided "
                    + spanCount);
        }
        mSpanCount = spanCount;
        mSpanSizeLookup.invalidateSpanIndexCache();
        mPrevAnchorLine = INVALID_ANCHOR_LINE;
        // Remove requestLayout.
        // requestLayout();
    }

    public float getProgress() {
        return mProgress;
    }

    public void setProgress(@FloatRange(from = 0F, to = 1F) float progress) {
        if (progress == mProgress) {
            return;
        }
        mProgress = progress;
        requestLayout();
    }

    public TimeInterpolator getInterpolator() {
        return mInterpolator;
    }

    public void setInterpolator(@Nullable TimeInterpolator i) {
        mInterpolator = i != null ? i : new LinearInterpolator();
    }

    private float getInterpolatedProgress() {
        return mInterpolator.getInterpolation(mProgress);
    }

    @Override
    protected void measureChildWithDecorationsAndMargin(View child, int widthSpec, int heightSpec,
                                                        boolean alreadyMeasured) {
        super.measureChildWithDecorationsAndMargin(child, widthSpec, heightSpec, alreadyMeasured);
        final var width = child.getMeasuredWidth();
        final var height = child.getMeasuredHeight();
        final var lastWidth = width * mSpanCount / mLastSpanCount;
        final var lastHeight = height * mSpanCount / mLastSpanCount;
        final var interpolatedWidth = lastWidth + (width - lastWidth) * getInterpolatedProgress();
        final var interpolatedHeight = lastHeight + (height - lastHeight) * getInterpolatedProgress();
        child.measure(
                MeasureSpec.makeMeasureSpec(Math.round(interpolatedWidth), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(Math.round(interpolatedHeight), MeasureSpec.EXACTLY)
        );
    }

    protected void updateLastSpans(int count, boolean layingOutInPrimaryDirection) {
        // spans are always assigned from 0 to N no matter if it is RTL or not.
        // RTL is used only when positioning the view.
        int start, end, diff;
        // make sure we traverse from min position to max position
        if (layingOutInPrimaryDirection) {
            start = 0;
            end = count;
            diff = 1;
        } else {
            start = count - 1;
            end = -1;
            diff = -1;
        }
        for (int i = start; i != end; i += diff) {
            View view = mSet[i];
            ProgressionGridLayoutManager.LayoutParams params = (ProgressionGridLayoutManager.LayoutParams) view.getLayoutParams();
            params.mLastSpanSize = params.mSpanSize;
            params.mLastSpanIndex = params.mSpanIndex;
        }
    }

    @Override
    protected void assignSpans(RecyclerView.Recycler recycler, RecyclerView.State state, int count,
                               boolean layingOutInPrimaryDirection) {
        if (mPendingSpanCountChange) {
            updateLastSpans(count, layingOutInPrimaryDirection);
        }
        super.assignSpans(recycler, state, count, layingOutInPrimaryDirection);
    }

    /**
     * Assume previous width == height.
     */
    private int guessOffset(int prevLine) {
        if (mPrevAnchorLine == INVALID_ANCHOR_LINE) {
            // align first prevLine
            mPrevAnchorLine = prevLine;
        }
        if (mAnchorInfo.mCoordinate == INVALID_OFFSET) {
            mAnchorInfo.assignCoordinateFromPadding();
        }
        return mAnchorInfo.mCoordinate + mLastCachedBorders[1] * (prevLine - mPrevAnchorLine);
    }

    @Override
    protected void calculateMargins(LayoutState layoutState, LayoutChunkResult result, int count,
                                    int maxSize) {
        if (mLastCachedBorders == null) {
            mLastCachedBorders = mCachedBorders;
        }
        int lastLeft = 0, lastRight = 0, lastTop = 0, lastBottom = 0;
        int left = 0, right = 0, top = 0, bottom = 0;
        if (mOrientation == VERTICAL) {
            if (layoutState.mLayoutDirection == LayoutState.LAYOUT_START) {
                bottom = layoutState.mOffset;
                top = bottom - maxSize;
            } else {
                top = layoutState.mOffset;
                bottom = top + maxSize;
            }
        } else {
            if (layoutState.mLayoutDirection == LayoutState.LAYOUT_START) {
                right = layoutState.mOffset;
                left = right - maxSize;
            } else {
                left = layoutState.mOffset;
                right = left + maxSize;
            }
        }
        for (int i = 0; i < count; i++) {
            View view = mSet[i];
            LayoutParams params = (LayoutParams) view.getLayoutParams();
            if (mOrientation == VERTICAL) {
                if (isLayoutRTL()) {
                    right = getPaddingLeft() + mCachedBorders[mSpanCount - params.mSpanIndex];
                    left = right - mOrientationHelper.getDecoratedMeasurementInOther(view);
                    if (params.mLastSpanIndex != GridLayoutManager.LayoutParams.INVALID_SPAN_ID &&
                            mLastSpanCount - params.mLastSpanIndex < mLastCachedBorders.length) {
                        lastRight = getPaddingLeft() + mLastCachedBorders[mLastSpanCount - params.mLastSpanIndex];
                        lastLeft = lastRight - mOrientationHelper.getDecoratedMeasurementInOther(view);
                    }
                } else {
                    left = getPaddingLeft() + mCachedBorders[params.mSpanIndex];
                    right = left + mOrientationHelper.getDecoratedMeasurementInOther(view);
                    if (params.mLastSpanIndex != GridLayoutManager.LayoutParams.INVALID_SPAN_ID &&
                            params.mLastSpanIndex < mLastCachedBorders.length) {
                        lastLeft = getPaddingLeft() + mLastCachedBorders[params.mLastSpanIndex];
                        lastRight = lastLeft + mOrientationHelper.getDecoratedMeasurementInOther(view);
                    }
                }
            } else {
                top = getPaddingTop() + mCachedBorders[params.mSpanIndex];
                bottom = top + mOrientationHelper.getDecoratedMeasurementInOther(view);
                if (params.mLastSpanIndex != GridLayoutManager.LayoutParams.INVALID_SPAN_ID &&
                        params.mLastSpanIndex < mLastCachedBorders.length) {
                    lastTop = getPaddingTop() + mLastCachedBorders[params.mLastSpanIndex];
                    lastBottom = lastTop + mOrientationHelper.getDecoratedMeasurementInOther(view);
                }
            }
            // Find the previous line for view.
            final var prevLine = getPosition(view) / mLastSpanCount;
            if (mOrientation == VERTICAL) {
                if (layoutState.mLayoutDirection == LayoutState.LAYOUT_START) {
                    lastBottom = guessOffset(prevLine);
                    lastTop = guessOffset(prevLine - 1);
                } else {
                    lastTop = guessOffset(prevLine);
                    lastBottom = guessOffset(prevLine + 1);
                }
            } else {
                if (layoutState.mLayoutDirection == LayoutState.LAYOUT_START) {
                    lastRight = guessOffset(prevLine);
                    lastLeft = guessOffset(prevLine - 1);
                } else {
                    lastLeft = guessOffset(prevLine);
                    lastRight = guessOffset(prevLine + 1);
                }
            }
            // We calculate everything with View's bounding box (which includes decor and margins)
            // To calculate correct layout position, we subtract margins.
            layoutDecoratedWithMargins(view, left, top, right, bottom,
                    lastLeft, lastTop, lastRight, lastBottom);
            if (DEBUG) {
                Log.d(TAG, "laid out child at position " + getPosition(view) + ", with l:"
                        + left + ", t:" + top + ", r:" + right + ", b:" + bottom
                        + ", span:" + params.mSpanIndex + ", spanSize:" + params.mSpanSize);
            }
            // Consume the available space if the view is not removed OR changed
            if (params.isItemRemoved() || params.isItemChanged()) {
                result.mIgnoreConsumed = true;
            }
            result.mFocusable |= view.hasFocusable();
        }
    }

    void layoutDecoratedWithMargins(@NonNull View child, int left, int top, int right, int bottom,
                                    int lastLeft, int lastTop, int lastRight, int lastBottom) {
        child.layout(
                Math.round(lastLeft + (left - lastLeft) * getInterpolatedProgress()),
                Math.round(lastTop + (top - lastTop) * getInterpolatedProgress()),
                Math.round(lastRight + (right - lastRight) * getInterpolatedProgress()),
                Math.round(lastBottom + (bottom - lastBottom) * getInterpolatedProgress())
        );
    }

    public static class LayoutParams extends GridLayoutManager.LayoutParams {

        int mLastSpanIndex = INVALID_SPAN_ID;

        int mLastSpanSize = 0;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(RecyclerView.LayoutParams source) {
            super(source);
        }
    }
}

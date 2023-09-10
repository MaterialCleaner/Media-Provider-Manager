/*
 * Copyright 2023 Green Mushroom
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

package me.gm.cleaner.plugin.mediastore.video.customexo;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;

public class VideoGestureDetector {
    private final OnVideoGestureListener mListener;
    private final GestureDetector mDetector;

    private boolean mIsHorizontallyScrubbing;
    private boolean mIsVerticallyScrubbing;
    private final float mTouchSlop;
    /**
     * Position of the last motion event.
     */
    private float mLastMotionX;
    private float mLastMotionY;
    private float mInitialMotionX;
    private float mInitialMotionY;
    /**
     * ID of the active pointer. This is used to retain consistency during
     * drags/flings if multiple pointers are used.
     */
    private int mActivePointerId = INVALID_POINTER;
    /**
     * Sentinel value for no current active pointer.
     * Used by {@link #mActivePointerId}.
     */
    private static final int INVALID_POINTER = -1;

    public VideoGestureDetector(@NonNull Context context,
                                @NonNull OnVideoGestureListener listener) {
        final ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
        mListener = listener;
        final GestureDetector.SimpleOnGestureListener onDoubleTapListener
                = new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
                return mListener.onSingleTapConfirmed(e);
            }

            @Override
            public boolean onDoubleTap(@NonNull MotionEvent ev) {
                return mListener.onDoubleTap(ev);
            }
        };
        mDetector = new GestureDetector(context, onDoubleTapListener);
    }

    private void resetTouch() {
        mActivePointerId = INVALID_POINTER;
        mIsHorizontallyScrubbing = false;
        mIsVerticallyScrubbing = false;
    }

    private boolean onTouchEventInternal(@NonNull MotionEvent ev) {
        boolean handled = false;
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                // Remember where the motion event started
                mLastMotionX = mInitialMotionX = ev.getX();
                mLastMotionY = mInitialMotionY = ev.getY();
                mActivePointerId = ev.getPointerId(0);
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if (!mIsHorizontallyScrubbing && !mIsVerticallyScrubbing) {
                    final int pointerIndex = ev.findPointerIndex(mActivePointerId);
                    if (pointerIndex == -1) {
                        // A child has consumed some touch events and put us into an inconsistent
                        // state.
                        resetTouch();
                        break;
                    }
                    final float x = ev.getX(pointerIndex);
                    final float xDiff = Math.abs(x - mLastMotionX);
                    final float y = ev.getY(pointerIndex);
                    final float yDiff = Math.abs(y - mLastMotionY);
                    if (xDiff > mTouchSlop && xDiff > yDiff) {
                        mLastMotionX = x - mInitialMotionX > 0 ? mInitialMotionX + mTouchSlop :
                                mInitialMotionX - mTouchSlop;
                        mIsHorizontallyScrubbing = true;
                        mListener.onHorizontalScrubStart(mInitialMotionX, mInitialMotionY);
                    } else if (yDiff > mTouchSlop && yDiff > xDiff) {
                        mLastMotionY = y - mInitialMotionY > 0 ? mInitialMotionY + mTouchSlop :
                                mInitialMotionY - mTouchSlop;
                        mIsVerticallyScrubbing = true;
                        mListener.onVerticalScrubStart(mInitialMotionX, mInitialMotionY);
                    }
                }
                // Not else! Note that mIsBeingDragged can be set above.
                if (mIsHorizontallyScrubbing) {
                    final int activePointerIndex = ev.findPointerIndex(mActivePointerId);
                    final float x = ev.getX(activePointerIndex);
                    final float y = ev.getY(activePointerIndex);
                    handled |= mListener.onHorizontalScrubMove(x - mLastMotionX);
                    mLastMotionX = x;
                    mLastMotionY = y;
                }
                if (mIsVerticallyScrubbing) {
                    final int activePointerIndex = ev.findPointerIndex(mActivePointerId);
                    final float x = ev.getX(activePointerIndex);
                    final float y = ev.getY(activePointerIndex);
                    handled |= mListener.onVerticalScrubMove(y - mLastMotionY);
                    mLastMotionX = x;
                    mLastMotionY = y;
                }
                break;
            }
            case MotionEvent.ACTION_POINTER_DOWN: {
                final int index = ev.getActionIndex();
                final float x = ev.getX(index);
                mLastMotionX = x;
                final float y = ev.getY(index);
                mLastMotionY = y;
                mActivePointerId = ev.getPointerId(index);
                break;
            }
            case MotionEvent.ACTION_POINTER_UP: {
                final int pointerIndex = ev.getActionIndex();
                final int pointerId = ev.getPointerId(pointerIndex);
                if (pointerId == mActivePointerId) {
                    // This was our active pointer going up. Choose a new
                    // active pointer and adjust accordingly.
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    mLastMotionX = ev.getX(newPointerIndex);
                    mLastMotionY = ev.getY(newPointerIndex);
                    mActivePointerId = ev.getPointerId(newPointerIndex);
                }
                break;
            }
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                if (mIsHorizontallyScrubbing) {
                    handled = true;
                    mListener.onHorizontalScrubEnd();
                }
                if (mIsVerticallyScrubbing) {
                    handled = true;
                    mListener.onVerticalScrubEnd();
                }
                resetTouch();
                break;
            }
        }
        return handled;
    }

    public boolean onTouchEvent(@NonNull MotionEvent ev) {
        mDetector.onTouchEvent(ev);
        return onTouchEventInternal(ev);
    }

    public interface OnVideoGestureListener {

        void onHorizontalScrubStart(float initialMotionX, float initialMotionY);

        boolean onHorizontalScrubMove(float dx);

        void onHorizontalScrubEnd();

        void onVerticalScrubStart(float initialMotionX, float initialMotionY);

        boolean onVerticalScrubMove(float dy);

        void onVerticalScrubEnd();

        boolean onSingleTapConfirmed(@NonNull MotionEvent ev);

        boolean onDoubleTap(@NonNull MotionEvent ev);
    }
}

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

package me.gm.cleaner.plugin.mediastore.images

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.core.animation.doOnEnd
import androidx.core.math.MathUtils.clamp
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.animation.AnimationUtils
import me.gm.cleaner.plugin.dao.RootPreferences
import me.gm.cleaner.plugin.ktx.mediumAnimTime
import me.gm.cleaner.plugin.mediastore.MediaStoreFragment
import kotlin.math.abs

class ScaleGestureListener(
    private val context: Context,
    private val layoutManager: ProgressionGridLayoutManager,
    private val viewHelper: MediaStoreFragment.MediaStoreRecyclerViewHelper
) : RecyclerView.SimpleOnItemTouchListener() {
    private var scaleEndAnimator: ValueAnimator? = null
    private val gestureDetector: ScaleGestureDetector = ScaleGestureDetector(
        context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            private var prevProgress = 0F
            private var isNewSpanCountSet = false

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val scaleFactor = detector.scaleFactor

                if (!isNewSpanCountSet && layoutManager.progress == 1F) {
                    layoutManager.spanCount = when {
                        scaleFactor > 1F -> {
                            // zoom in
                            if (layoutManager.spanCount - spanCountInterval < minSpanCount ||
                                layoutManager.spanCount == minSpanCount && layoutManager.progress == 1F
                            ) {
                                return true
                            }
                            layoutManager.spanCount - spanCountInterval
                        }

                        scaleFactor < 1F -> {
                            // zoom out
                            if (layoutManager.spanCount + spanCountInterval > maxSpanCount ||
                                layoutManager.spanCount == maxSpanCount && layoutManager.progress == 1F
                            ) {
                                return true
                            }
                            layoutManager.spanCount + spanCountInterval
                        }

                        else -> return false
                    }
                    isNewSpanCountSet = true
                }

                val newProgress = when {
                    scaleFactor > 1F -> abs(1F - scaleFactor) / SCALE_FACTOR
                    scaleFactor < 1F -> abs(1F - 1 / scaleFactor) / SCALE_FACTOR
                    else -> 0F
                }
                if (layoutManager.progress == 1F) {
                    prevProgress = newProgress
                    isNewSpanCountSet = false
                }
                layoutManager.progress = clamp(newProgress - prevProgress, 0F, 1F)
                return false
            }

            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                if (scaleEndAnimator?.isRunning == true) {
                    return false
                }
                prevProgress = 0F
                isNewSpanCountSet = false
                return true
            }

            override fun onScaleEnd(detector: ScaleGestureDetector) {
                animateProgress(layoutManager.progress, 1F) {
                    RootPreferences.spanCount = layoutManager.spanCount
                    // manually invalidate itemsHeights
                    viewHelper.observer.onChanged()
                }
            }
        }
    )

    @SuppressLint("RestrictedApi")
    private fun animateProgress(
        from: Float, to: Float, doOnEnd: ((animator: Animator) -> Unit)? = null
    ) {
        scaleEndAnimator?.cancel()
        scaleEndAnimator = ValueAnimator.ofFloat(from, to).apply {
            duration = (context.mediumAnimTime * abs(to - from)).toLong()
            interpolator = AnimationUtils.LINEAR_INTERPOLATOR
            addUpdateListener { valueAnimator ->
                layoutManager.progress = valueAnimator.animatedValue as Float
            }
            doOnEnd {
                doOnEnd?.invoke(it)
            }
            start()
        }
    }

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        if (scaleEndAnimator?.isRunning == true) {
            // Disable fastScroller.
            return true
        } else {
            scaleEndAnimator = null
        }
        return e.pointerCount >= 2
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
        val scaleEndAnimator = scaleEndAnimator
        when {
            scaleEndAnimator == null -> {
                gestureDetector.onTouchEvent(e)
            }

            scaleEndAnimator.isRunning -> {
                // Request intercept SelectionTracker's OnItemTouchListener.
                rv.requestDisallowInterceptTouchEvent(true)
            }

            !scaleEndAnimator.isRunning -> {
                // TODO: I don't know what to do.
                //  rv.requestDisallowInterceptTouchEvent(false)
            }
        }
    }

    companion object {
        const val SCALE_FACTOR: Float = 1F
        const val minSpanCount: Int = 1
        const val maxSpanCount: Int = 5
        const val spanCountInterval: Int = 1
    }
}

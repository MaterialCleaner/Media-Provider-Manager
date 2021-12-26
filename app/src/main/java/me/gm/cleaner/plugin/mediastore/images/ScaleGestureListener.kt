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

import android.content.Context
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.core.math.MathUtils.clamp
import androidx.recyclerview.widget.ProgressionGridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.gm.cleaner.plugin.dao.ModulePreferences
import kotlin.math.abs

class ScaleGestureListener(
    context: Context, private val layoutManager: ProgressionGridLayoutManager
) :
    RecyclerView.SimpleOnItemTouchListener() {
    private val gestureDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            val scaleThreshold = 0.5F
            var isScaleBegun = false
            var prevSpanCount = layoutManager.spanCount

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val currSpan = detector.currentSpan
                val prevSpan = detector.previousSpan
                if (currSpan == prevSpan) {
                    return false
                }
                if (isScaleBegun) {
                    isScaleBegun = false
                    // TODO reversed gesture support
                    //  maybe overflow gesture support?
                    if (currSpan > prevSpan) {
                        // zoom in
                        if (layoutManager.spanCount == 1) {
                            return true
                        }
                        layoutManager.spanCount -= 1
                    } else if (currSpan < prevSpan) {
                        // zoom out
                        if (layoutManager.spanCount == 5) {
                            return true
                        }
                        layoutManager.spanCount += 1
                    }
                }

                val rawProgress = when {
                    currSpan > prevSpan -> (currSpan / prevSpan - 1F) / scaleThreshold
                    currSpan < prevSpan -> (prevSpan / currSpan - 1F) / scaleThreshold
                    else -> 0F
                }
                layoutManager.progress = clamp(abs(rawProgress), 0F, 1F)
                return false
            }

            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                prevSpanCount = layoutManager.spanCount
                isScaleBegun = true
                return isScaleBegun
            }

            override fun onScaleEnd(detector: ScaleGestureDetector) {
                if (layoutManager.progress >= 0.6F) {
                    ModulePreferences.spanCount = layoutManager.spanCount
                } else {
                    layoutManager.spanCount = prevSpanCount
                }
                // TODO use animator to animate progress to 1F
                layoutManager.progress = 1F
            }
        }
    )

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(e)
        return gestureDetector.isInProgress
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
        gestureDetector.onTouchEvent(e)
    }
}

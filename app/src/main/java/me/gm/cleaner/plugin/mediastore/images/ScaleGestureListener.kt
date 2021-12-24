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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.gm.cleaner.plugin.dao.ModulePreferences

class ScaleGestureListener(context: Context, private val layoutManager: GridLayoutManager) :
    RecyclerView.SimpleOnItemTouchListener() {
    private val gestureDetector = ScaleGestureDetector(context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            val scaleThreshold = 0.2F
            fun isZoomIn(currSpan: Float, prevSpan: Float) =
                currSpan > (1F + scaleThreshold) * prevSpan

            fun isZoomOut(currSpan: Float, prevSpan: Float) =
                currSpan < (1F - scaleThreshold) * prevSpan

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val currSpan = detector.currentSpan
                // TODO
                return false
            }

            override fun onScaleEnd(detector: ScaleGestureDetector) {
                val currSpan = detector.currentSpan
                val prevSpan = detector.previousSpan
                if (isZoomIn(currSpan, prevSpan) && ModulePreferences.spanCount > 1) {
                    ModulePreferences.spanCount -= 1
                } else if (isZoomOut(currSpan, prevSpan) && ModulePreferences.spanCount < 5) {
                    ModulePreferences.spanCount += 1
                }
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

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

package me.gm.cleaner.plugin.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import com.drakeet.drawer.FullDraggableContainer

class FullyDraggableContainer @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FullDraggableContainer(context, attrs, defStyleAttr) {
    private val interceptTouchEventListeners by lazy { mutableListOf<OnGenericMotionListener>() }

    override fun onInterceptTouchEvent(event: MotionEvent) =
        if (interceptTouchEventListeners.any { it.onGenericMotion(this, event) }) {
            false
        } else {
            super.onInterceptTouchEvent(event)
        }

    fun addInterceptTouchEventListener(l: OnGenericMotionListener) {
        interceptTouchEventListeners.add(l)
    }

    fun removeInterceptTouchEventListener(l: OnGenericMotionListener) {
        interceptTouchEventListeners.remove(l)
    }
}

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

package me.zhanghai.android.fastscroll

import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SimpleOnItemTouchListener

internal class SelectionTrackerRecyclerViewHelper(
    private val list: RecyclerView,
    private val shouldInterceptTouchEvent: Predicate<MotionEvent?>,
    popupTextProvider: PopupTextProvider? = null
) : RecyclerViewHelper(list, popupTextProvider) {

    override fun addOnTouchEventListener(onTouchEvent: Predicate<MotionEvent?>) {
        list.addOnItemTouchListener(object : SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(recyclerView: RecyclerView, event: MotionEvent) =
                if (!shouldInterceptTouchEvent.test(null)) {
                    onTouchEvent.test(event)
                } else {
                    false
                }

            override fun onTouchEvent(recyclerView: RecyclerView, event: MotionEvent) {
                onTouchEvent.test(event)
            }
        })
    }
}

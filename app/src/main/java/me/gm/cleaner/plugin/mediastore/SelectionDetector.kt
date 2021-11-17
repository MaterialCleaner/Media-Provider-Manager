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

package me.gm.cleaner.plugin.mediastore

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent

class LongPressingListener : GestureDetector.SimpleOnGestureListener() {

    @Volatile
    var isSelecting = false

    override fun onLongPress(e: MotionEvent?) {
        isSelecting = true
    }
}

class SelectionDetector(context: Context, private val l: LongPressingListener) :
    GestureDetector(context, l) {
    val isSelecting
        get() = l.isSelecting

    private val onUp = setOf(MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL)

    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action in onUp) {
            l.isSelecting = false
        }
        return super.onTouchEvent(ev)
    }
}

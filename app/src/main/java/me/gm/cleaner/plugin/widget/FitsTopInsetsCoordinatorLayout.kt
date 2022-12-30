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
import android.graphics.Rect
import android.util.AttributeSet
import android.view.WindowInsets
import androidx.annotation.AttrRes
import androidx.coordinatorlayout.widget.CoordinatorLayout

class FitsTopInsetsCoordinatorLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, @AttrRes defStyleAttr: Int = 0
) : CoordinatorLayout(context, attrs, defStyleAttr) {
    private val mPaddingStart = paddingStart
    private val mPaddingTop = paddingTop
    private val mPaddingEnd = paddingEnd
    private val mPaddingBottom = paddingBottom

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        val localInsets = Rect()
        val result = computeSystemWindowInsets(insets, localInsets)
        applyInsets(localInsets)
        // Return "result" will consume the insets.
        return insets
    }

    private fun applyInsets(insets: Rect) {
        setPaddingRelative(mPaddingStart, mPaddingTop + insets.top, mPaddingEnd, mPaddingBottom)
    }
}

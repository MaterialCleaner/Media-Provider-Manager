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
import android.view.View
import androidx.annotation.AttrRes
import androidx.appcompat.R
import androidx.appcompat.widget.SearchView
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePaddingRelative
import me.gm.cleaner.plugin.ktx.dpToPx
import me.gm.cleaner.plugin.ktx.getDrawableByAttr

open class FixLayoutSearchView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = R.attr.searchViewStyle
) : SearchView(context, attrs, defStyleAttr) {

    // abc_search_view.xml
    init {
        // A negative value won't work here because SearchView will use its preferred width as max
        // width instead.
        maxWidth = Int.MAX_VALUE
        val searchEditFrame = findViewById<View>(R.id.search_edit_frame)
        // 72 - 60 + 0 = 12
        searchEditFrame.updateLayoutParams<MarginLayoutParams> {
            leftMargin = 12
            rightMargin = 12
        }
        val searchSrcText = findViewById<View>(R.id.search_src_text)
        searchSrcText.setPaddingRelative(0, searchSrcText.top, 0, searchSrcText.paddingBottom)
        val searchCloseBtn = findViewById<View>(R.id.search_close_btn)
        val searchCloseBtnPaddingHorizontal = searchCloseBtn.context.dpToPx(12)
        searchCloseBtn.updatePaddingRelative(
            start = searchCloseBtnPaddingHorizontal, end = searchCloseBtnPaddingHorizontal
        )
        searchCloseBtn.background =
            searchCloseBtn.context.getDrawableByAttr(R.attr.actionBarItemBackground)
    }
}

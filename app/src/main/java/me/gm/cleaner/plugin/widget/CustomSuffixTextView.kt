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

import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class CustomSuffixTextView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = android.R.attr.textViewStyle
) : AppCompatTextView(context, attrs, defStyleAttr) {
    var suffix: CharSequence? = null
        private set

    @Deprecated("Use setTextAndSuffix() for better performance.")
    fun setSuffix(suffix: CharSequence?) {
        this.suffix = suffix
        requestLayout()
    }

    fun setTextAndSuffix(text: CharSequence?, suffix: CharSequence?) {
        this.suffix = suffix
        this.text = text
    }

    @SuppressLint("SetTextI18n")
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (suffix.isNullOrEmpty()) {
            return
        }
        val ellipsisWidth = measuredWidth - compoundPaddingLeft - compoundPaddingRight
        val totalEllipsisWidth = ellipsisWidth * maxLines
        // Create a synthetic text for measuring ellipsize.
        val tmpText = "$suffix $text"
        var ellipsizedText =
            TextUtils.ellipsize(tmpText, paint, totalEllipsisWidth.toFloat(), ellipsize)
        if (tmpText == ellipsizedText) {
            ellipsizedText = "$ellipsizedText\u2026"
        }
        ellipsizedText = ellipsizedText.subSequence("$suffix ".length, ellipsizedText.length)
        text = "$ellipsizedText $suffix"
    }
}

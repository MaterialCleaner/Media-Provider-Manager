/*
 * Copyright 2022 Green Mushroom
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

package androidx.swiperefreshlayout.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.ShapeDrawable
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.google.android.material.R
import me.gm.cleaner.plugin.ktx.colorPrimary
import me.gm.cleaner.plugin.ktx.colorSurface

@SuppressLint("PrivateResource")
class ThemedSwipeRefreshLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : SwipeRefreshLayout(context, attrs) {

    init {
        val overlayColor = ContextCompat
            .getColorStateList(context, R.color.m3_popupmenu_overlay_color)!!
            .defaultColor
        val backgroundColor = ColorUtils.compositeColors(overlayColor, context.colorSurface)
        (mCircleView.background as ShapeDrawable).paint.color = backgroundColor

        setColorSchemeColors(context.colorPrimary)
    }
}

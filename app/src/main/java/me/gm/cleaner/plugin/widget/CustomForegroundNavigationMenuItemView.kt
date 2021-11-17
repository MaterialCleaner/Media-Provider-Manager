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
import android.content.res.ColorStateList
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.RippleDrawable
import android.util.AttributeSet
import androidx.annotation.AttrRes
import androidx.appcompat.view.menu.MenuView.ItemView
import com.google.android.material.R
import com.google.android.material.internal.NavigationMenuItemView
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import me.gm.cleaner.plugin.ktx.colorControlHighlight
import me.gm.cleaner.plugin.ktx.dipToPx

@SuppressLint("RestrictedApi", "PrivateResource")
class CustomForegroundNavigationMenuItemView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, @AttrRes defStyleAttr: Int = 0
) : NavigationMenuItemView(context, attrs, defStyleAttr), ItemView {

    init {
        val materialShapeDrawable = MaterialShapeDrawable(
            ShapeAppearanceModel
                .builder(context, R.style.ShapeAppearanceOverlay_Material3_NavigationView_Item, 0)
                .build()
        )
        val inset = context.dipToPx(12F)
        foreground = RippleDrawable(
            ColorStateList.valueOf(context.colorControlHighlight), null,
            InsetDrawable(materialShapeDrawable, inset, 0, inset, 0)
        )
    }
}

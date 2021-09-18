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

package me.gm.cleaner.plugin.home

import android.content.res.Resources
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.EdgeEffect
import androidx.recyclerview.widget.RecyclerView
import com.drakeet.about.AbsAboutActivity
import me.gm.cleaner.plugin.util.DisplayUtils.getColorByAttr
import rikka.core.res.resolveColor
import rikka.core.util.ResourceUtils

abstract class ThemedAboutActivity : AbsAboutActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ResourceUtils.isNightMode(resources.configuration)) {
            val aboutPageBackground = getColor(com.drakeet.about.R.color.about_page_background)
            val backgroundDrawable = ColorDrawable(aboutPageBackground)
            setHeaderBackground(backgroundDrawable)
            setHeaderContentScrim(backgroundDrawable)
        }
    }

    override fun onCreateEdgeEffect(view: RecyclerView, direction: Int, rect: Rect?): EdgeEffect =
        super.onCreateEdgeEffect(view, direction, rect).apply {
            if (ResourceUtils.isNightMode(resources.configuration)) {
                color =
                    getColor(com.drakeet.about.R.color.about_page_background) and 0xffffff or 0x33000000
            }
        }

    override fun onApplyTranslucentSystemBars() {
        super.onApplyTranslucentSystemBars()
        window.statusBarColor = if (ResourceUtils.isNightMode(resources.configuration)) {
            getColor(com.drakeet.about.R.color.about_page_background)
        } else {
            getColorByAttr(android.R.attr.colorPrimary)
        }
        window.decorView.post {
            if (window.decorView.rootWindowInsets.systemWindowInsetBottom >=
                Resources.getSystem().displayMetrics.density * 40
            ) {
                window.navigationBarColor =
                    theme.resolveColor(android.R.attr.navigationBarColor) and 0x00ffffff or -0x20000000
                window.isNavigationBarContrastEnforced = false
            } else {
                window.navigationBarColor = Color.TRANSPARENT
                window.isNavigationBarContrastEnforced = true
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        if (!super.onSupportNavigateUp()) {
            finish()
        }
        return true
    }
}

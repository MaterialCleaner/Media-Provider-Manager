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

package me.gm.cleaner.plugin.app

import android.content.res.Resources
import android.graphics.Color
import android.view.View
import androidx.appcompat.widget.Toolbar
import me.gm.cleaner.plugin.R
import rikka.core.res.resolveColor
import rikka.material.app.MaterialActivity
import rikka.material.widget.AppBarLayout

abstract class BaseActivity : MaterialActivity() {
    var appBarLayout: AppBarLayout? = null

    override fun setContentView(view: View) {
        super.setContentView(view)
        appBarLayout = findViewById(R.id.toolbar_container)
        appBarLayout?.apply {
            val toolbar: Toolbar = findViewById(R.id.toolbar)
            setAppBar(this, toolbar)
        }
    }

    override fun onApplyTranslucentSystemBars() {
        super.onApplyTranslucentSystemBars()
        window.statusBarColor = Color.TRANSPARENT
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

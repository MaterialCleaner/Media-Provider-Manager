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

package me.gm.cleaner.plugin.mediastore.images

import android.app.Application
import android.graphics.PointF
import android.view.View
import androidx.lifecycle.AndroidViewModel
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import me.gm.cleaner.plugin.app.BaseFragment
import me.gm.cleaner.plugin.compat.isNightModeActivated

class ImageViewModel(application: Application) : AndroidViewModel(application) {
    private val top by lazy {
        val res = getApplication<Application>().resources
        val actionBarSize =
            res.getDimensionPixelSize(com.google.android.material.R.dimen.m3_appbar_size_compact)
        val resourceId = res.getIdentifier("status_bar_height", "dimen", "android")
        res.getDimensionPixelSize(resourceId) + actionBarSize
    }
    private val vTarget by lazy { PointF() }

    fun isOverlay(photoView: SubsamplingScaleImageView): Boolean {
        if (!photoView.isReady) {
            return false
        }
        photoView.sourceToViewCoord(0f, 0f, vTarget)
        return vTarget.y - top < 0
    }
}

fun BaseFragment.toggleAppBar(isShow: Boolean) {
    val decorView = requireActivity().window.decorView
    if (isShow) {
        supportActionBar?.show()
        var flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        if (!resources.configuration.isNightModeActivated) {
            flags = flags or
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                    View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
        decorView.systemUiVisibility = flags
    } else {
        supportActionBar?.hide()
        // Fullscreen is costly in my case, so I come to terms with immersive.
        // If you persist in fullscreen, I'd advise you to display the photos with activity.
        // See also: https://developer.android.com/training/system-ui/immersive
        decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }
}

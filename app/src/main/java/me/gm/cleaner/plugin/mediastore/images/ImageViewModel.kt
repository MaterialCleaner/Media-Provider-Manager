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
import android.util.Size
import androidx.appcompat.app.ActionBar
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import kotlinx.coroutines.flow.MutableStateFlow

class ImageViewModel(application: Application) : AndroidViewModel(application) {
    var currentPosition = 0
    var isAppBarUpToDate = true
    private val _isPostponedFlow = MutableStateFlow(true)
    val isPostponedLiveData = _isPostponedFlow.asLiveData()
    var isPostponed: Boolean
        get() = _isPostponedFlow.value
        set(value) {
            _isPostponedFlow.value = value
        }

    fun updateAppBar(supportActionBar: ActionBar?, images: List<MediaStoreImage>) {
        supportActionBar?.apply {
            title = images[currentPosition].displayName
            subtitle = "${currentPosition + 1} / ${images.size}"
        }
    }

    val size by lazy {
        val displayMetrics = getApplication<Application>().resources.displayMetrics
        Size(displayMetrics.widthPixels, displayMetrics.heightPixels)
    }
    private val top by lazy {
        val res = getApplication<Application>().resources
        val actionBarSize =
            res.getDimensionPixelSize(com.google.android.material.R.dimen.m3_appbar_size_compact)
        val resourceId = res.getIdentifier("status_bar_height", "dimen", "android")
        res.getDimensionPixelSize(resourceId) + actionBarSize
    }
    private val vTarget by lazy { PointF() }
    fun isOverlay(subsamplingScaleImageView: SubsamplingScaleImageView): Boolean {
        if (!subsamplingScaleImageView.isReady) {
            return false
        }
        subsamplingScaleImageView.sourceToViewCoord(0f, 0f, vTarget)
        return vTarget.y - top < 0
    }
}

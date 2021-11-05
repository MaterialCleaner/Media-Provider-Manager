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
import androidx.lifecycle.AndroidViewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import me.gm.cleaner.plugin.R

class PagerViewModel(application: Application) : AndroidViewModel(application) {
    private val _currentPositionFlow = MutableStateFlow(0)
    var currentPosition: Int
        get() = _currentPositionFlow.value
        set(value) {
            _currentPositionFlow.value = value
        }
    private val _currentDestinationFlow: MutableStateFlow<NavDestination?> = MutableStateFlow(null)
    val currentDestination: NavDestination?
        get() = _currentDestinationFlow.value
    private val destinationChangedListener =
        NavController.OnDestinationChangedListener { controller, destination, _ ->
            when {
                _currentDestinationFlow.value?.id == R.id.images_fragment &&
                        destination.id == R.id.pager_fragment -> isFirstEntrance = true
                _currentDestinationFlow.value?.id == R.id.pager_fragment &&
                        destination.id == R.id.images_fragment -> isFromPager = true
                _currentDestinationFlow.value?.id !in setOf(
                    R.id.images_fragment, R.id.pager_fragment
                ) && destination.id == R.id.pager_fragment
                -> {
                    isFromPager = false
                    controller.navigate(R.id.images_fragment)
                }
            }
            _currentDestinationFlow.value = destination
        }
    var isFirstEntrance = false
    var isFromPager = false

    fun setDestinationChangedListener(navController: NavController) {
        navController.removeOnDestinationChangedListener(destinationChangedListener)
        navController.addOnDestinationChangedListener(destinationChangedListener)
    }

    val currentAppBarTitleSourceFlow = combine(
        _currentPositionFlow, _currentDestinationFlow
    ) { currentPosition, currentDestination -> currentPosition to currentDestination }

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
    private val vTarget = PointF()
    fun isOverlay(subsamplingScaleImageView: SubsamplingScaleImageView): Boolean {
        if (!subsamplingScaleImageView.isReady) {
            return false
        }
        subsamplingScaleImageView.sourceToViewCoord(0F, 0F, vTarget)
        return vTarget.y - top < 0
    }
}

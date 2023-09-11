/*
 * Copyright 2023 Green Mushroom
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

package me.gm.cleaner.plugin.mediastore.video.customexo

import android.provider.Settings
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import androidx.core.math.MathUtils.clamp
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.media3.common.DeviceInfo
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerControlView
import androidx.media3.ui.PlayerControlViewLayoutManagerAccessor
import androidx.media3.ui.PlayerView
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.ktx.isRtl
import kotlin.math.roundToInt

@UnstableApi
open class CustomOnVerticalScrubListener(
    private val window: Window,
    private val playerView: PlayerView,
    private val controller: PlayerControlView,
    private val controlViewLayoutManager: PlayerControlViewLayoutManagerAccessor
) {
    private lateinit var seekDelta: TextView
    private lateinit var deviceInfo: DeviceInfo

    private val density: Float = playerView.resources.displayMetrics.density * 2
    private val isRtl: Boolean = playerView.resources.configuration.isRtl
    private var atLeftHalfScreen: Boolean = true
    private var screenBrightness: Float = window.attributes.screenBrightness
    private var currentVolume: Float = 0F

    private fun prepare() {
        if (::seekDelta.isInitialized) {
            return
        }
        seekDelta = playerView.findViewById(R.id.seek_delta)
        deviceInfo = playerView.player!!.deviceInfo
    }

    private fun getBrightnessString(brightness: Float): String = "${(100 * brightness).toInt()} %"

    private fun getVolumeString(deviceVolume: Int): String {
        val fraction =
            100 * (deviceVolume - deviceInfo.minVolume) / (deviceInfo.maxVolume - deviceInfo.minVolume)
        return "$fraction %"
    }

    fun onScrubStart(initialMotionX: Float, initialMotionY: Float) {
        val player = playerView.player ?: return
        prepare()
        controller.children.forEach { child ->
            child.isVisible = child === seekDelta
        }
        controlViewLayoutManager.showImmediately()
        controlViewLayoutManager.removeHideCallbacks()

        atLeftHalfScreen = initialMotionX < playerView.width / 2
        if (screenBrightness == WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE) {
            val SCREEN_BRIGHTNESS_FLOAT = "screen_brightness_float"
            screenBrightness = Settings.System.getFloat(
                playerView.context.contentResolver, SCREEN_BRIGHTNESS_FLOAT
            )
        }
        currentVolume = player.deviceVolume.toFloat()
    }

    fun onScrubMove(dy: Float) {
        val player = playerView.player ?: return
        if (!isRtl && atLeftHalfScreen || isRtl && !atLeftHalfScreen) {
            screenBrightness = clamp(
                screenBrightness - dy / density / (deviceInfo.maxVolume - deviceInfo.minVolume),
                0F,
                1F
            )
            seekDelta.text = getBrightnessString(screenBrightness)
            val attributes = window.attributes
            attributes.screenBrightness = screenBrightness
            window.attributes = attributes
        } else {
            currentVolume = clamp(
                currentVolume - dy / density,
                deviceInfo.minVolume.toFloat(),
                deviceInfo.maxVolume.toFloat()
            )
            val currentVolumeInt = currentVolume.roundToInt()
            seekDelta.text = getVolumeString(currentVolumeInt)
            player.setDeviceVolume(currentVolumeInt, 0)
        }
    }

    fun onScrubStop() {
        controller.children.forEach { child ->
            child.isVisible = child !== seekDelta
        }
        controlViewLayoutManager.hideImmediately()
    }
}

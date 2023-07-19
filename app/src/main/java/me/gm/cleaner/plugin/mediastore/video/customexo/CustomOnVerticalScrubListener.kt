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

import android.widget.TextView
import androidx.core.math.MathUtils.clamp
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.media3.common.DeviceInfo
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerControlView
import androidx.media3.ui.PlayerView
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.ktx.isRtl
import kotlin.math.roundToInt

@UnstableApi
open class CustomOnVerticalScrubListener(private val playerView: PlayerView) {
    private lateinit var controller: PlayerControlView
    private lateinit var seekDelta: TextView
    private lateinit var deviceInfo: DeviceInfo

    private val density: Float = playerView.resources.displayMetrics.density
    private val isRtl: Boolean = playerView.resources.configuration.isRtl
    private var atLeftHalfScreen: Boolean = true
    private var currentVolume: Float = 0F

    private fun prepare() {
        if (::seekDelta.isInitialized) {
            return
        }
        controller = playerView.findViewById(androidx.media3.ui.R.id.exo_controller)
        seekDelta = playerView.findViewById(R.id.seek_delta)
        deviceInfo = playerView.player!!.deviceInfo
    }

    private fun setOnlyTextVisible() {
        controller.show()
        controller.children.forEach { child ->
            child.isVisible = child === seekDelta
        }
    }

    private fun hideController() {
        controller.children.forEach { child ->
            child.isVisible = child !== seekDelta
        }
        controller.hideImmediately()
    }

    private fun getVolumeString(deviceVolume: Int): String {
        val fraction =
            100 * (deviceVolume - deviceInfo.minVolume) / (deviceInfo.maxVolume - deviceInfo.minVolume)
        return "$fraction %"
    }

    fun onScrubStart(initialMotionX: Float) {
        val player = playerView.player ?: return
        prepare()
        setOnlyTextVisible()
        atLeftHalfScreen = initialMotionX < playerView.width / 2
        currentVolume = player.deviceVolume.toFloat()
    }

    fun onScrubMove(dy: Float) {
        val player = playerView.player ?: return
        if (!isRtl && atLeftHalfScreen || isRtl && !atLeftHalfScreen) {

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
        hideController()
    }
}

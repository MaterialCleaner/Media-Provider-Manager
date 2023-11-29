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

package me.gm.cleaner.plugin.mediastore.video.customexo

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.ui.PlayerControlView
import androidx.media3.ui.PlayerControlViewLayoutManagerAccessor
import androidx.media3.ui.PlayerView
import androidx.media3.ui.TimeBar
import me.gm.cleaner.plugin.R
import java.lang.reflect.Field
import java.util.Formatter
import java.util.Locale

@UnstableApi
open class CustomOnHorizontalScrubListener(
    private val playerView: PlayerView,
    private val controller: PlayerControlView,
    private val controlViewLayoutManager: PlayerControlViewLayoutManagerAccessor
) : TimeBar.OnScrubListener {
    private val scrubbingField: Field = PlayerControlView::class.java
        .getDeclaredField("scrubbing")
        .apply { isAccessible = true }
    private lateinit var controlsBackground: View
    private lateinit var centerControls: LinearLayout
    private lateinit var centerText: TextView
    private lateinit var topBar: Toolbar
    private lateinit var bottomBar: ViewGroup
    private lateinit var timeBar: View

    private var playingOnScrubStart: Boolean = true
    private var controllerVisibleOnScrubStart: Boolean = false
    private var startingPosition: Long = 0L
    private val formatBuilder: StringBuilder = StringBuilder()
    private val formatter: Formatter = Formatter(formatBuilder, Locale.getDefault())

    private fun prepareViews() {
        if (::controlsBackground.isInitialized) {
            return
        }
        controlsBackground =
            controller.findViewById(androidx.media3.ui.R.id.exo_controls_background)
        centerControls = controller.findViewById(androidx.media3.ui.R.id.exo_center_controls)
        centerText = controller.findViewById(R.id.center_text)
        topBar = controller.findViewById(R.id.top_bar)
        bottomBar = controller.findViewById(androidx.media3.ui.R.id.exo_bottom_bar)
        timeBar = controller.findViewById(androidx.media3.ui.R.id.exo_progress)
    }

    private fun getDeltaString(timeMs: Long): String {
        val prefix = if (timeMs > 0) "+" else ""
        return prefix + Util.getStringForTime(formatBuilder, formatter, timeMs)
    }

    override fun onScrubStart(timeBar: TimeBar, position: Long) {
        prepareViews()
        scrubbingField[controller] = true
        playingOnScrubStart = playerView.player?.playWhenReady == true
        controllerVisibleOnScrubStart = controller.isFullyVisible
        controlsBackground.isVisible = false
        centerControls.isVisible = false
        centerText.isVisible = true
        topBar.isVisible = false
        bottomBar.translationY = 0F
        this.timeBar.translationY = 0F
        controlViewLayoutManager.showImmediately()
        controlViewLayoutManager.removeHideCallbacks()

        playerView.player?.pause()
        startingPosition = playerView.player?.currentPosition ?: 0L
        playerView.player?.seekTo(position)
        centerText.text = getDeltaString(position - startingPosition)
    }

    override fun onScrubMove(timeBar: TimeBar, position: Long) {
        playerView.player?.seekTo(position)
        centerText.text = getDeltaString(position - startingPosition)
    }

    override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
        scrubbingField[controller] = false
        controlsBackground.isVisible = true
        centerControls.isVisible = true
        centerText.isVisible = false
        topBar.isVisible = true
        if (controllerVisibleOnScrubStart) {
            controlViewLayoutManager.resetHideCallbacks()
        } else {
            controlViewLayoutManager.hideImmediately()
        }

        playerView.player?.seekTo(position)
        if (playingOnScrubStart) {
            playerView.player?.play()
        }
    }

    val isScrubbing: Boolean
        get() = scrubbingField[controller] as Boolean
}

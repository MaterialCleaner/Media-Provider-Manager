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
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.android.exoplayer2.ui.R
import com.google.android.exoplayer2.ui.StyledPlayerControlView
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.ui.TimeBar
import com.google.android.exoplayer2.util.Util
import java.util.*

open class CustomOnScrubListener(private val playerView: StyledPlayerView) :
    TimeBar.OnScrubListener {
    private val scrubbingField = StyledPlayerControlView::class.java.getDeclaredField("scrubbing")
        .apply { isAccessible = true }
    private lateinit var controller: StyledPlayerControlView
    private lateinit var controlsBackground: View
    private lateinit var centerControls: LinearLayout
    private lateinit var seekDelta: TextView

    private var startingPosition = 0L
    private val formatBuilder = StringBuilder()
    private val formatter = Formatter(formatBuilder, Locale.getDefault())

    private fun prepareViews() {
        if (::controller.isInitialized) {
            return
        }
        controller = playerView.findViewById(R.id.exo_controller)
        controlsBackground = playerView.findViewById(R.id.exo_controls_background)
        centerControls = playerView.findViewById(R.id.exo_center_controls)
        seekDelta = playerView.findViewById(me.gm.cleaner.plugin.R.id.seek_delta)
    }

    private fun getDeltaString(timeMs: Long): String {
        val prefix = if (timeMs > 0) "+" else ""
        return prefix + Util.getStringForTime(formatBuilder, formatter, timeMs)
    }

    override fun onScrubStart(timeBar: TimeBar, position: Long) {
        prepareViews()
        scrubbingField[controller] = true
        playerView.player?.pause()
        startingPosition = playerView.player?.currentPosition ?: 0L
        controlsBackground.isVisible = false
        centerControls.isVisible = false
        seekDelta.isVisible = true
        playerView.player?.seekTo(position)
        seekDelta.text = getDeltaString(position - startingPosition)
    }

    override fun onScrubMove(timeBar: TimeBar, position: Long) {
        playerView.player?.seekTo(position)
        seekDelta.text = getDeltaString(position - startingPosition)
    }

    override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
        scrubbingField[controller] = false
        controlsBackground.isVisible = true
        centerControls.isVisible = true
        seekDelta.isVisible = false
        playerView.player?.seekTo(position)
        playerView.player?.play()
    }

    val isScrubbing: Boolean
        get() = ::controller.isInitialized && scrubbingField[controller] as Boolean
}

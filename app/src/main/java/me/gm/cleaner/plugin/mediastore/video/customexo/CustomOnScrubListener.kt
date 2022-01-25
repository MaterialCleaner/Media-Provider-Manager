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
import androidx.core.view.isVisible
import com.google.android.exoplayer2.ui.R
import com.google.android.exoplayer2.ui.StyledPlayerControlView
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.ui.TimeBar

open class CustomOnScrubListener(private val playerView: StyledPlayerView) :
    TimeBar.OnScrubListener {
    private val scrubbingField = StyledPlayerControlView::class.java.getDeclaredField("scrubbing")
        .apply { isAccessible = true }
    private lateinit var controller: StyledPlayerControlView
    private lateinit var controlsBackground: View
    private lateinit var centerControls: LinearLayout

    private fun prepareViews() {
        if (::controller.isInitialized) {
            return
        }
        controller = playerView.findViewById(R.id.exo_controller)
        controlsBackground = playerView.findViewById(R.id.exo_controls_background)
        centerControls = playerView.findViewById(R.id.exo_center_controls)
    }

    override fun onScrubStart(timeBar: TimeBar, position: Long) {
        prepareViews()
        scrubbingField.set(controller, true)
        playerView.player?.pause()
        controlsBackground.isVisible = false
        centerControls.isVisible = false
    }

    override fun onScrubMove(timeBar: TimeBar, position: Long) {
        playerView.player?.seekTo(position)
    }

    override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
        scrubbingField.set(controller, false)
        playerView.player?.play()
        controlsBackground.isVisible = true
        centerControls.isVisible = true
    }
}

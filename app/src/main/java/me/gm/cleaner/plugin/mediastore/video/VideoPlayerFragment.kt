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

package me.gm.cleaner.plugin.mediastore.video

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerControlView
import androidx.media3.ui.PlayerControlViewLayoutManagerAccessor
import androidx.media3.ui.PlayerView
import androidx.media3.ui.TimeBar
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.app.BaseFragment
import me.gm.cleaner.plugin.databinding.VideoPlayerFragmentBinding
import me.gm.cleaner.plugin.ktx.addOnExitListener
import me.gm.cleaner.plugin.ktx.getObjectField
import me.gm.cleaner.plugin.mediastore.video.customexo.CustomOnScrubListener
import me.gm.cleaner.plugin.mediastore.video.customexo.CustomTimeBar
import me.gm.cleaner.plugin.mediastore.video.customexo.DefaultTimeBar
import me.gm.cleaner.plugin.widget.FullyDraggableContainer
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.math.max

@UnstableApi
class VideoPlayerFragment : BaseFragment() {
    private val viewModel: VideoPlayerViewModel by viewModels()
    private val args: VideoPlayerFragmentArgs by navArgs()
    private lateinit var trackSelectionParameters: DefaultTrackSelector.Parameters
    private var startItemIndex: Int = 0
    private var startPosition: Long = 0L
    private var isPlaying: Boolean = true
    private var playbackSpeed: Float = 1F
    private lateinit var trackSelector: DefaultTrackSelector
    private var player: ExoPlayer? = null
    private var playerView: PlayerView? = null
    private val forbidDrawerGestureListener = View.OnGenericMotionListener { _, _ ->
        true
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = VideoPlayerFragmentBinding.inflate(inflater)
        playerView = binding.playerView
        customizePlayerViewBehavior(playerView!!)

        if (savedInstanceState != null) {
            // Restore as DefaultTrackSelector.Parameters in case ExoPlayer specific parameters were set.
            trackSelectionParameters = DefaultTrackSelector.Parameters.CREATOR.fromBundle(
                savedInstanceState.getBundle(KEY_TRACK_SELECTION_PARAMETERS)!!
            )
            startItemIndex = savedInstanceState.getInt(KEY_ITEM_INDEX)
            startPosition = savedInstanceState.getLong(KEY_POSITION)
            isPlaying = savedInstanceState.getBoolean(KEY_IS_PLAYING, isPlaying)
            playbackSpeed = savedInstanceState.getFloat(KEY_SPEED, playbackSpeed)
        } else {
            trackSelectionParameters = DefaultTrackSelector
                .ParametersBuilder(requireContext())
                .build()
        }

        findNavController().addOnExitListener { _, destination, _ ->
            toDefaultAppBarState(destination)
            requireActivity().findViewById<FullyDraggableContainer>(R.id.fully_draggable_container)
                .removeInterceptTouchEventListener(forbidDrawerGestureListener)
        }
        return binding.root
    }

    private fun customizePlayerViewBehavior(playerView: PlayerView) {
        val controller =
            playerView.findViewById<PlayerControlView>(androidx.media3.ui.R.id.exo_controller)!!
        val timeBar = controller.getObjectField<TimeBar>() as CustomTimeBar
        val listeners =
            timeBar.getObjectField<CopyOnWriteArraySet<TimeBar.OnScrubListener>>(DefaultTimeBar::class.java)
        listeners.clear()
        timeBar.addListener(timeBar)

        val controlViewLayoutManager = PlayerControlViewLayoutManagerAccessor(controller)
        timeBar.addListener(object : CustomOnScrubListener(playerView) {
            override fun onScrubStart(timeBar: TimeBar, position: Long) {
                (playerView.player as? ExoPlayer)?.setSeekParameters(SeekParameters.CLOSEST_SYNC)
                super.onScrubStart(timeBar, position)
                controlViewLayoutManager.removeHideCallbacks()
            }

            override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
                (playerView.player as? ExoPlayer)?.setSeekParameters(null)
                super.onScrubStop(timeBar, position, canceled)
                controlViewLayoutManager.resetHideCallbacks()
            }
        })
    }

    inner class PlayerEventListener : Player.Listener {
        override fun onVideoSizeChanged(videoSize: VideoSize) {
            super.onVideoSizeChanged(videoSize)
            if (videoSize != VideoSize.UNKNOWN) {
                viewModel.isFirstTimeEntry = false
                requireActivity().requestedOrientation = if (videoSize.width > videoSize.height) {
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
            }
        }
    }

    private fun initializePlayer() {
        if (player != null) {
            return
        }
        val context = requireContext().applicationContext
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
        trackSelector = DefaultTrackSelector(context)

        player = ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .setTrackSelector(trackSelector)
            .build().also { player ->
                player.trackSelectionParameters = trackSelectionParameters
                if (viewModel.isFirstTimeEntry) {
                    player.addListener(PlayerEventListener())
                }
                player.setAudioAttributes(AudioAttributes.DEFAULT, true)
                player.seekTo(startItemIndex, startPosition)
                player.playWhenReady = isPlaying
                player.setPlaybackSpeed(playbackSpeed)
                val mediaItems = args.uris.map { MediaItem.fromUri(it) }
                player.setMediaItems(mediaItems, false)
                player.prepare()
            }
        playerView?.player = player
    }

    private fun updatePlayerState() {
        player?.let { player ->
            trackSelectionParameters =
                player.trackSelectionParameters as DefaultTrackSelector.Parameters
            startItemIndex = player.currentMediaItemIndex
            startPosition = max(0, player.contentPosition)
            isPlaying = player.playWhenReady
            playbackSpeed = player.playbackParameters.speed
        }
    }

    private fun releasePlayer() {
        updatePlayerState()
        player?.release()
        player = null
    }

    override fun onResume() {
        super.onResume()
        initializePlayer()
        playerView?.onResume()
    }

    override fun onStart() {
        super.onStart()
        initializePlayer()
        playerView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        playerView?.onPause()
        releasePlayer()
    }

    override fun onStop() {
        super.onStop()
        playerView?.onPause()
        releasePlayer()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        toggleAppBar(false)
        requireActivity().findViewById<FullyDraggableContainer>(R.id.fully_draggable_container)
            .addInterceptTouchEventListener(forbidDrawerGestureListener)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        updatePlayerState()
        outState.putBundle(KEY_TRACK_SELECTION_PARAMETERS, trackSelectionParameters.toBundle())
        outState.putInt(KEY_ITEM_INDEX, startItemIndex)
        outState.putLong(KEY_POSITION, startPosition)
        outState.putBoolean(KEY_IS_PLAYING, isPlaying)
        outState.putFloat(KEY_SPEED, playbackSpeed)
    }

    companion object {
        // Saved instance state keys.
        private const val KEY_TRACK_SELECTION_PARAMETERS = "track_selection_parameters"
        private const val KEY_ITEM_INDEX = "item_index"
        private const val KEY_POSITION = "position"
        private const val KEY_IS_PLAYING = "is_playing"
        private const val KEY_SPEED = "speed"
    }
}

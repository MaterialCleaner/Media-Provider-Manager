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
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.drawerlayout.widget.DrawerLayout
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
import me.gm.cleaner.plugin.dao.RootPreferences
import me.gm.cleaner.plugin.databinding.VideoPlayerFragmentBinding
import me.gm.cleaner.plugin.ktx.addOnExitListener
import me.gm.cleaner.plugin.mediastore.video.customexo.CustomOnHorizontalScrubListener
import me.gm.cleaner.plugin.mediastore.video.customexo.CustomOnVerticalScrubListener
import me.gm.cleaner.plugin.mediastore.video.customexo.CustomTimeBar
import me.gm.cleaner.plugin.mediastore.video.customexo.VideoGestureDetector
import me.gm.cleaner.plugin.widget.FullyDraggableContainer
import kotlin.math.max

@UnstableApi
class VideoPlayerFragment : BaseFragment() {
    private val viewModel: VideoPlayerViewModel by viewModels()
    private val args: VideoPlayerFragmentArgs by navArgs()
    private lateinit var trackSelectionParameters: DefaultTrackSelector.Parameters
    private var startItemIndex: Int = 0
    private var startPosition: Long = 0L
    private var isPlaying: Boolean = true
    private var playbackSpeed: Float = RootPreferences.playbackSpeed
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
        customizePlayerViewBehavior(playerView!!, binding.gestureView)

        viewModel.screenOrientationLiveData.observe(viewLifecycleOwner) { orientation ->
            requireActivity().requestedOrientation = orientation
        }

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
            requireActivity().findViewById<DrawerLayout>(R.id.drawer_layout)
                .setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        }
        return binding.root
    }

    private fun customizePlayerViewBehavior(playerView: PlayerView, gestureView: View) {
        playerView.controllerAutoShow = false
        playerView.isClickable = false

        val controller =
            playerView.findViewById<PlayerControlView>(androidx.media3.ui.R.id.exo_controller)!!
        val timeBar = controller.findViewById<CustomTimeBar>(androidx.media3.ui.R.id.exo_progress)
        timeBar.addListener(timeBar)

        val controlViewLayoutManager = PlayerControlViewLayoutManagerAccessor(controller)
        timeBar.addListener(object : CustomOnHorizontalScrubListener(
            playerView, controller, controlViewLayoutManager
        ) {
            override fun onScrubStart(timeBar: TimeBar, position: Long) {
                (playerView.player as? ExoPlayer)?.setSeekParameters(SeekParameters.CLOSEST_SYNC)
                super.onScrubStart(timeBar, position)
            }

            override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
                (playerView.player as? ExoPlayer)?.setSeekParameters(null)
                super.onScrubStop(timeBar, position, canceled)
            }
        })

        val listeners = arrayOf(timeBar, object : CustomOnHorizontalScrubListener(
            playerView, controller, controlViewLayoutManager
        ) {
            // TODO: Maybe we can implement a scheme to seek fast and exact. Please refer to
            //  https://github.com/google/ExoPlayer/issues/7025

        })
        val customOnVerticalScrubListener = CustomOnVerticalScrubListener(
            requireActivity().window, playerView, controller, controlViewLayoutManager
        )
        val detector = VideoGestureDetector(
            requireContext(), object : VideoGestureDetector.OnVideoGestureListener {
                private val density: Float = resources.displayMetrics.density

                override fun onHorizontalScrubStart(
                    initialMotionX: Float, initialMotionY: Float
                ) {
                    val player = player ?: return
                    for (listener in listeners) {
                        listener.onScrubStart(timeBar, player.currentPosition)
                    }
                }

                override fun onHorizontalScrubMove(dx: Float): Boolean {
                    val player = player ?: return false
                    val newPositionMs =
                        player.currentPosition + (SCRUB_FACTOR * dx / density).toLong()
                    for (listener in listeners) {
                        listener.onScrubMove(timeBar, newPositionMs)
                    }
                    return true
                }

                override fun onHorizontalScrubEnd() {
                    val player = player ?: return
                    for (listener in listeners) {
                        listener.onScrubStop(timeBar, player.currentPosition, false)
                    }
                }

                override fun onVerticalScrubStart(initialMotionX: Float, initialMotionY: Float) {
                    customOnVerticalScrubListener.onScrubStart(initialMotionX, initialMotionY)
                }

                override fun onVerticalScrubMove(dy: Float): Boolean {
                    customOnVerticalScrubListener.onScrubMove(dy)
                    return true
                }

                override fun onVerticalScrubEnd() {
                    customOnVerticalScrubListener.onScrubStop()
                }

                override fun onSingleTapConfirmed(ev: MotionEvent): Boolean {
                    if (controller.isFullyVisible) {
                        controlViewLayoutManager.hide()
                    } else {
                        controlViewLayoutManager.show()
                    }
                    return true
                }

                override fun onDoubleTap(ev: MotionEvent): Boolean {
                    val player = player ?: return false
                    player.playWhenReady = !player.playWhenReady
                    if (player.playWhenReady) {
                        controlViewLayoutManager.hide()
                    }
                    return true
                }
            }
        )
        //noinspection ClickableViewAccessibility
        gestureView.setOnTouchListener { _, event ->
            detector.onTouchEvent(event)
            true
        }
    }

    inner class PlayerEventListener : Player.Listener {
        override fun onVideoSizeChanged(videoSize: VideoSize) {
            super.onVideoSizeChanged(videoSize)
            if (videoSize != VideoSize.UNKNOWN) {
                viewModel.screenOrientation = if (videoSize.width > videoSize.height) {
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
            .setDeviceVolumeControlEnabled(true)
            .build().also { player ->
                player.trackSelectionParameters = trackSelectionParameters
                if (viewModel.screenOrientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
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
        requireActivity().findViewById<DrawerLayout>(R.id.drawer_layout)
            .setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
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

    override fun onDestroy() {
        super.onDestroy()
        RootPreferences.playbackSpeed = playbackSpeed
    }

    companion object {
        private const val SCRUB_FACTOR: Int = 125

        // Saved instance state keys.
        private const val KEY_TRACK_SELECTION_PARAMETERS: String = "track_selection_parameters"
        private const val KEY_ITEM_INDEX: String = "item_index"
        private const val KEY_POSITION: String = "position"
        private const val KEY_IS_PLAYING: String = "is_playing"
        private const val KEY_SPEED: String = "speed"
    }
}

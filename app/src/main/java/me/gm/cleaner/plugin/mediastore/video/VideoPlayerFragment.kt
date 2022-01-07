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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector.ParametersBuilder
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.util.EventLogger
import me.gm.cleaner.plugin.app.BaseFragment
import me.gm.cleaner.plugin.databinding.VideoPlayerFragmentBinding
import me.gm.cleaner.plugin.ktx.addOnExitListener
import kotlin.math.max

class VideoPlayerFragment : BaseFragment() {
    private val viewModel: VideoPlayerViewModel by viewModels()
    private val args: VideoPlayerFragmentArgs by navArgs()
    private val navController by lazy { findNavController() }
    private lateinit var trackSelectionParameters: DefaultTrackSelector.Parameters
    private var startItemIndex = 0
    private var startPosition = 0L
    private lateinit var trackSelector: DefaultTrackSelector
    private var player: ExoPlayer? = null
    private var playerView: StyledPlayerView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        if (savedInstanceState != null) {
            // Restore as DefaultTrackSelector.Parameters in case ExoPlayer specific parameters were set.
            trackSelectionParameters = DefaultTrackSelector.Parameters.CREATOR.fromBundle(
                savedInstanceState.getBundle(KEY_TRACK_SELECTION_PARAMETERS)!!
            )
            startItemIndex = savedInstanceState.getInt(KEY_ITEM_INDEX)
            startPosition = savedInstanceState.getLong(KEY_POSITION)
        } else {
            trackSelectionParameters = ParametersBuilder(requireContext()).build()
        }

        val binding = VideoPlayerFragmentBinding.inflate(inflater)
        playerView = binding.playerView

        navController.addOnExitListener { _, destination, _ ->
            toDefaultAppBarState(destination)
        }
        return binding.root
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
//        player.addListener(PlayerEventListener())
                player.addAnalyticsListener(EventLogger(trackSelector))
                player.setAudioAttributes(AudioAttributes.DEFAULT, true)
                player.playWhenReady = true
                player.seekTo(startItemIndex, startPosition)
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
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        updatePlayerState()
        outState.putBundle(KEY_TRACK_SELECTION_PARAMETERS, trackSelectionParameters.toBundle())
        outState.putInt(KEY_ITEM_INDEX, startItemIndex)
        outState.putLong(KEY_POSITION, startPosition)
    }

    companion object {
        // Saved instance state keys.
        private const val KEY_TRACK_SELECTION_PARAMETERS = "track_selection_parameters"
        private const val KEY_ITEM_INDEX = "item_index"
        private const val KEY_POSITION = "position"
    }
}

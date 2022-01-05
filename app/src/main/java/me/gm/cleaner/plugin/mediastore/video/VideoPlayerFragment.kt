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
import com.google.android.exoplayer2.util.EventLogger
import me.gm.cleaner.plugin.app.BaseFragment
import me.gm.cleaner.plugin.databinding.VideoPlayerFragmentBinding
import me.gm.cleaner.plugin.ktx.addOnExitListener

class VideoPlayerFragment : BaseFragment() {
    private val viewModel: VideoPlayerViewModel by viewModels()
    private val args: VideoPlayerFragmentArgs by navArgs()
    private val navController by lazy { findNavController() }
    private lateinit var trackSelector: DefaultTrackSelector
    private lateinit var player: ExoPlayer

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = VideoPlayerFragmentBinding.inflate(inflater)

        toggleAppBar(false)
        initializePlayer()
        binding.playerView.player = player

        navController.addOnExitListener { _, destination, _ ->
            toDefaultAppBarState(destination)
        }

        return binding.root
    }

    private fun initializePlayer() {
        val context = requireContext().applicationContext
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
        trackSelector = DefaultTrackSelector(context)

        player = ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
//            .setMediaSourceFactory(mediaSourceFactory)
            .setTrackSelector(trackSelector)
            .build()
//        player.setTrackSelectionParameters(trackSelectionParameters)
//        player.addListener(com.google.android.exoplayer2.demo.PlayerActivity.PlayerEventListener())
        player.addAnalyticsListener(EventLogger(trackSelector))
        player.setAudioAttributes(AudioAttributes.DEFAULT,  /* handleAudioFocus= */true)
        player.playWhenReady = true
//        player.seekTo(startItemIndex, startPosition)
        val mediaItems = args.uris.map { MediaItem.fromUri(it) }
        player.setMediaItems(mediaItems, false)
        player.prepare()
    }
}

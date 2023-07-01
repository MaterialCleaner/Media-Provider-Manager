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

import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.mediastore.files.FilesAdapter
import me.gm.cleaner.plugin.mediastore.files.MediaStoreFiles

class VideoAdapter(private val fragment: VideoFragment) : FilesAdapter(fragment) {
    private val viewModel: VideoViewModel by fragment.viewModels()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        when (holder) {
            is ItemViewHolder -> {
                val binding = holder.binding
                val item = getItem(position) as MediaStoreFiles
                binding.card.setOnClickListener {
                    val navController = fragment.findNavController()
                    if (navController.currentDestination?.id != R.id.video_fragment) {
                        return@setOnClickListener
                    }

                    // TODO
//                    val videos = viewModel.medias
//                    val direction = VideoFragmentDirections.actionVideoToVideoPlayer(
//                        initialPosition = holder.bindingAdapterPosition,
//                        isMediaStoreUri = true,
//                        uris = videos.map { it.contentUri }.toTypedArray(),
//                        displayNames = videos.map { it.displayName }.toTypedArray()
//                    )
                    val direction = VideoFragmentDirections.actionVideoToVideoPlayer(
                        uris = arrayOf(item.contentUri),
                        displayNames = arrayOf(item.displayName),
                    )
                    navController.navigate(direction)
                }
            }
        }
    }
}

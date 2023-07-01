/*
 * Copyright 2021 Green Mushroom
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

package me.gm.cleaner.plugin.mediastore.images

import android.graphics.drawable.Drawable
import android.transition.TransitionSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.selection.ItemDetailsLookup.ItemDetails
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.dao.RootPreferences
import me.gm.cleaner.plugin.databinding.ImagesItemBinding
import me.gm.cleaner.plugin.mediastore.MediaStoreAdapter
import me.gm.cleaner.plugin.mediastore.MediaStoreHeader
import me.gm.cleaner.plugin.mediastore.MediaStoreModel

class ImagesAdapter(private val fragment: ImagesFragment) : MediaStoreAdapter(fragment) {
    private val viewModel: ImagesViewModel by fragment.viewModels()
    private val uriPositionMap: MutableList<Int> = mutableListOf()

    fun getHolderPositionForUriPosition(position: Int): Int = uriPositionMap[position]

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is MediaStoreImage -> R.layout.images_item
        else -> super.getItemViewType(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        when (viewType) {
            R.layout.images_item -> ItemViewHolder(
                ImagesItemBinding.inflate(LayoutInflater.from(parent.context)), 0
            )

            else -> super.onCreateViewHolder(parent, viewType)
        }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (holder) {
            is ItemViewHolder -> {
                val binding = holder.binding
                holder.uriPosition = uriPositionMap.binarySearch(position)
                val item = getItem(position)
                // Load the image with Glide to prevent OOM error when the image drawables are very large.
                Glide.with(fragment)
                    .load(item.contentUri)
                    .listener(object : RequestListener<Drawable?> {
                        override fun onLoadFailed(
                            e: GlideException?, model: Any?, target: Target<Drawable?>?,
                            isFirstResource: Boolean
                        ): Boolean {
                            if (fragment.lastPosition == holder.uriPosition) {
                                fragment.startPostponedEnterTransition()
                            }
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable?, model: Any?, target: Target<Drawable?>?,
                            dataSource: DataSource?, isFirstResource: Boolean
                        ): Boolean {
                            if (fragment.lastPosition == holder.uriPosition) {
                                fragment.startPostponedEnterTransition()
                            }
                            return false
                        }
                    })
                    .centerCrop()
                    .into(binding.image)
                binding.image.transitionName = item.contentUri.toString()
                binding.card.setOnClickListener {
                    val navController = fragment.findNavController()
                    if (fragment.isInActionMode() ||
                        navController.currentDestination?.id != R.id.images_fragment
                    ) {
                        return@setOnClickListener
                    }
                    fragment.lastPosition = holder.uriPosition

                    // Exclude the clicked card from the exit transition (e.g. the card will disappear immediately
                    // instead of fading out with the rest to prevent an overlapping animation of fade and move).
                    (fragment.exitTransition as TransitionSet).excludeTarget(binding.card, true)

                    val images = viewModel.medias
                    val direction = ImagesFragmentDirections.actionImagesToImagePager(
                        initialPosition = holder.uriPosition,
                        isMediaStoreUri = true,
                        uris = images.map { it.contentUri }.toTypedArray(),
                        displayNames = images.map { it.displayName }.toTypedArray()
                    )
                    val extras = FragmentNavigatorExtras(
                        binding.image to binding.image.transitionName
                    )
                    navController.navigate(direction, extras)
                }

                holder.details = object : ItemDetails<Long>() {
                    override fun getPosition(): Int = holder.bindingAdapterPosition
                    override fun getSelectionKey(): Long = item.id
                    override fun inSelectionHotspot(e: MotionEvent): Boolean = false
                    override fun inDragRegion(e: MotionEvent): Boolean = true
                }
                if (selectionTrackerInitialized) {
                    binding.card.isChecked = selectionTracker.isSelected(item.id)
                }
            }

            else -> super.onBindViewHolder(holder, position)
        }
    }

    override fun onPreSubmitList(list: List<MediaStoreModel>): List<MediaStoreModel> {
        uriPositionMap.clear()
        return when (RootPreferences.sortMediaBy) {
            RootPreferences.SORT_BY_PATH -> {
                val groupedList = mutableListOf<MediaStoreModel>()
                var lastHeader = ""
                list.forEach { model ->
                    val header = model.relativePath
                    if (lastHeader != header) {
                        lastHeader = header
                        groupedList += MediaStoreHeader(header)
                    }
                    uriPositionMap += groupedList.size
                    groupedList += model
                }
                groupedList
            }

            RootPreferences.SORT_BY_DATE_TAKEN -> {
                val groupedList = mutableListOf<MediaStoreModel>()
                var lastHeader = ""
                list.forEach { model ->
                    val header = formatDateTime(model.dateTaken)
                    if (lastHeader != header) {
                        lastHeader = header
                        groupedList += MediaStoreHeader(header)
                    }
                    uriPositionMap += groupedList.size
                    groupedList += model
                }
                groupedList
            }

            else -> {
                list
            }
        }
    }

    class ItemViewHolder(val binding: ImagesItemBinding, var uriPosition: Int) :
        ViewHolder(binding.root)
}

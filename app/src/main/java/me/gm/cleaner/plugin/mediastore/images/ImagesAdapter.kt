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
import android.text.format.DateUtils
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
import com.google.android.material.transition.platform.MaterialFadeOutIn
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.dao.ModulePreferences
import me.gm.cleaner.plugin.databinding.ImagesItemBinding
import me.gm.cleaner.plugin.mediastore.MediaStoreAdapter
import me.zhanghai.android.fastscroll.PopupTextProvider

class ImagesAdapter(private val fragment: ImagesFragment) :
    MediaStoreAdapter<MediaStoreImage, ImagesAdapter.ViewHolder>(), PopupTextProvider {
    private val context = fragment.requireContext()
    private val viewModel: ImagesViewModel by fragment.viewModels()
    private val navController by lazy { fragment.findNavController() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ImagesItemBinding.inflate(LayoutInflater.from(parent.context)))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val item = getItem(position)
        // Load the image with Glide to prevent OOM error when the image drawables are very large.
        Glide.with(fragment)
            .load(item.contentUri)
            .listener(object : RequestListener<Drawable?> {
                override fun onLoadFailed(
                    e: GlideException?, model: Any?, target: Target<Drawable?>?,
                    isFirstResource: Boolean
                ): Boolean {
                    if (fragment.lastPosition == holder.bindingAdapterPosition) {
                        fragment.startPostponedEnterTransition()
                    }
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?, model: Any?, target: Target<Drawable?>?,
                    dataSource: DataSource?, isFirstResource: Boolean
                ): Boolean {
                    if (fragment.lastPosition == holder.bindingAdapterPosition) {
                        fragment.startPostponedEnterTransition()
                    }
                    return false
                }
            })
            .centerCrop()
            .into(binding.image)
        binding.image.transitionName = item.contentUri.toString()
        binding.card.setOnClickListener {
            if (navController.currentDestination?.id != R.id.images_fragment || fragment.isInActionMode()) {
                return@setOnClickListener
            }
            fragment.lastPosition = holder.bindingAdapterPosition
            fragment.exitTransition = MaterialFadeOutIn()

            val images = viewModel.medias
            val direction = ImagesFragmentDirections.actionImagesToImagePager(
                initialPosition = holder.bindingAdapterPosition,
                isMediaStoreUri = true,
                uris = images.map { it.contentUri }.toTypedArray(),
                displayNames = images.map { it.displayName }.toTypedArray()
            )
            val extras = FragmentNavigatorExtras(binding.image to binding.image.transitionName)
            navController.navigate(direction, extras)
        }

        holder.details = object : ItemDetails<Long>() {
            override fun getPosition() = holder.bindingAdapterPosition
            override fun getSelectionKey() = item.id
            override fun inSelectionHotspot(e: MotionEvent) = false
            override fun inDragRegion(e: MotionEvent) = true
        }
        if (selectionTrackerInitialized) {
            binding.card.isChecked = selectionTracker.isSelected(item.id)
        }
    }

    private fun formatDateTime(timeMillis: Long): String {
        val flags = DateUtils.FORMAT_NO_NOON or DateUtils.FORMAT_NO_MIDNIGHT or
                DateUtils.FORMAT_ABBREV_ALL or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_SHOW_DATE
        return DateUtils.formatDateTime(context, timeMillis, flags)
    }

    override fun getPopupText(position: Int) =
        if (ModulePreferences.sortMediaBy != ModulePreferences.SORT_BY_PATH) {
            formatDateTime(getItem(position).dateTaken)
        } else {
            ""
        }

    class ViewHolder(val binding: ImagesItemBinding) : MediaStoreAdapter.ViewHolder(binding.root)
}

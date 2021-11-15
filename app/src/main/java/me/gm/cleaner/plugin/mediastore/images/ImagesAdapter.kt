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
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.ItemDetailsLookup.ItemDetails
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.databinding.ImagesItemBinding

class ImagesAdapter(private val fragment: ImagesFragment) :
    ListAdapter<MediaStoreImage, ImagesAdapter.ViewHolder>(MediaStoreImage.DiffCallback) {
    private val imagesViewModel: ImagesViewModel by fragment.viewModels()
    private val navController by lazy { fragment.findNavController() }
    lateinit var selectionTracker: SelectionTracker<Long>

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ImagesItemBinding.inflate(LayoutInflater.from(parent.context)))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val uri = getItem(position).contentUri
        // Load the image with Glide to prevent OOM error when the image drawables are very large.
        Glide.with(fragment)
            .load(uri)
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
            .thumbnail(1F / 3F)
            .centerCrop()
            .into(binding.image)
        binding.image.transitionName = uri.toString()
        binding.card.setOnClickListener {
            if (navController.currentDestination?.id != R.id.images_fragment || fragment.actionMode != null) {
                return@setOnClickListener
            }
            fragment.lastPosition = holder.bindingAdapterPosition
            val images = imagesViewModel.images
            val direction = ImagesFragmentDirections.actionImagesToImagePager(
                initialPosition = holder.bindingAdapterPosition,
                hasOptionsMenu = true,
                uris = images.map { it.contentUri }.toTypedArray(),
                displayNames = images.map { it.displayName }.toTypedArray()
            )
            val extras = FragmentNavigatorExtras(binding.image to binding.image.transitionName)
            navController.navigate(direction, extras)
        }

        holder.details = object : ItemDetails<Long>() {
            override fun getPosition() = holder.bindingAdapterPosition
            override fun getSelectionKey() = getItemId(holder.bindingAdapterPosition)
            override fun inSelectionHotspot(e: MotionEvent) = false
            override fun inDragRegion(e: MotionEvent) = true
        }
        if (::selectionTracker.isInitialized) {
            binding.card.isChecked = selectionTracker.isSelected(getItemId(position))
        }
    }

    override fun getItemId(position: Int) = getItem(position).hashCode().toLong()

    class ViewHolder(val binding: ImagesItemBinding) : RecyclerView.ViewHolder(binding.root) {
        lateinit var details: ItemDetails<Long>
    }
}

class DetailsLookup(private val list: RecyclerView) : ItemDetailsLookup<Long>() {
    override fun getItemDetails(e: MotionEvent): ItemDetails<Long>? {
        val view = list.findChildViewUnder(e.x, e.y)
        if (view != null) {
            val viewHolder = list.getChildViewHolder(view)
            if (viewHolder is ImagesAdapter.ViewHolder) {
                return viewHolder.details
            }
        }
        return null
    }
}

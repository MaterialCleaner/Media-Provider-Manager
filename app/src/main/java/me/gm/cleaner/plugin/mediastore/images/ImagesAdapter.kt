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
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.Navigation
import androidx.navigation.fragment.FragmentNavigatorExtras
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
    private val viewModel: ImagesViewModel by fragment.activityViewModels()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ImagesItemBinding.inflate(LayoutInflater.from(parent.context)))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val uri = getItem(position).contentUri
        // Load the image with Glide to prevent OOM error when the image drawables are very large.
        Glide.with(binding.image)
            .load(uri)
            .listener(object : RequestListener<Drawable?> {
                override fun onLoadFailed(
                    e: GlideException?, model: Any?, target: Target<Drawable?>?,
                    isFirstResource: Boolean
                ): Boolean {
                    if (viewModel.currentPosition == holder.bindingAdapterPosition) {
                        fragment.startPostponedEnterTransition()
                    }
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?, model: Any?, target: Target<Drawable?>?,
                    dataSource: DataSource?, isFirstResource: Boolean
                ): Boolean {
                    if (viewModel.currentPosition == holder.bindingAdapterPosition) {
                        fragment.startPostponedEnterTransition()
                    }
                    return false
                }
            })
            .thumbnail(0.33F)
            .centerCrop()
            .into(binding.image)
        binding.image.transitionName = uri.toString()
        binding.root.setOnClickListener {
            viewModel.currentPosition = holder.bindingAdapterPosition
            val extras = FragmentNavigatorExtras(binding.image to binding.image.transitionName)
            Navigation
                .findNavController(fragment.requireActivity(), R.id.nav_host)
                .navigate(R.id.action_query_to_gallery, null, null, extras)
        }
    }

    class ViewHolder(val binding: ImagesItemBinding) : RecyclerView.ViewHolder(binding.root)
}

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

package me.gm.cleaner.plugin.test

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.databinding.GalleryItemBinding

class QueryAdapter(fragment: QueryFragment) :
    ListAdapter<MediaStoreImage, QueryAdapter.ImageViewHolder>(MediaStoreImage.DiffCallback) {
    private val activity: TestActivity = fragment.requireActivity() as TestActivity

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder =
        ImageViewHolder(GalleryItemBinding.inflate(LayoutInflater.from(parent.context)))

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val binding = holder.binding
        val mediaStoreImage = getItem(position)
        Glide.with(binding.image)
            .load(mediaStoreImage.contentUri)
            .thumbnail(0.33f)
            .centerCrop()
            .into(binding.image)
        binding.root.setOnClickListener {
            Navigation
                .findNavController(activity, R.id.home)
                .navigate(R.id.action_query_to_gallery)
        }
    }

    class ImageViewHolder(val binding: GalleryItemBinding) : RecyclerView.ViewHolder(binding.root)
}

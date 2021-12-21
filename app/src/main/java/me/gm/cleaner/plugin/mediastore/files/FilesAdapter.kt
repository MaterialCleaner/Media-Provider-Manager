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

package me.gm.cleaner.plugin.mediastore.files

import android.annotation.SuppressLint
import android.text.format.DateUtils
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.selection.ItemDetailsLookup.ItemDetails
import com.bumptech.glide.Glide
import me.gm.cleaner.plugin.databinding.FilesItemBinding
import me.gm.cleaner.plugin.mediastore.MediaStoreAdapter
import java.util.*

class FilesAdapter(private val fragment: FilesFragment) :
    MediaStoreAdapter<MediaStoreFiles, FilesAdapter.ViewHolder>() {
    private val then: Calendar = Calendar.getInstance()
    private val now: Calendar = Calendar.getInstance()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(FilesItemBinding.inflate(LayoutInflater.from(parent.context)))

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val item = getItem(position)
        Glide.with(fragment)
            .load(item.contentUri)
            .centerCrop()
            .into(binding.icon)
        binding.title.text = item.data
        binding.title.isSelected = true
        binding.summary.text = formatDateTime(item.timeMillis) + "\u0020\u0020\u0020\u0020" +
                Formatter.formatFileSize(fragment.requireContext(), item.size)

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
        then.timeInMillis = timeMillis
        now.timeInMillis = System.currentTimeMillis()
        val flags = DateUtils.FORMAT_NO_NOON or DateUtils.FORMAT_NO_MIDNIGHT or
                DateUtils.FORMAT_ABBREV_ALL or when {
            then[Calendar.YEAR] != now[Calendar.YEAR] -> DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_SHOW_DATE
            then[Calendar.DAY_OF_YEAR] != now[Calendar.DAY_OF_YEAR] -> DateUtils.FORMAT_SHOW_DATE
            else -> DateUtils.FORMAT_SHOW_TIME
        }
        return DateUtils.formatDateTime(fragment.requireContext(), timeMillis, flags)
    }

    class ViewHolder(val binding: FilesItemBinding) : MediaStoreAdapter.ViewHolder(binding.root)
}

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
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.selection.ItemDetailsLookup.ItemDetails
import com.bumptech.glide.Glide
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.dao.ModulePreferences
import me.gm.cleaner.plugin.databinding.FilesHeaderBinding
import me.gm.cleaner.plugin.databinding.FilesItemBinding
import me.gm.cleaner.plugin.mediastore.MediaStoreAdapter
import me.gm.cleaner.plugin.mediastore.MediaStoreModel
import java.util.*

open class FilesAdapter(private val fragment: Fragment) :
    MediaStoreAdapter<MediaStoreModel, MediaStoreAdapter.ViewHolder>() {
    private val then: Calendar = Calendar.getInstance()
    private val now: Calendar = Calendar.getInstance()

    override fun getItemViewType(position: Int) = when (getItem(position)) {
        is MediaStoreFilesHeader -> R.layout.files_header
        is MediaStoreFiles -> R.layout.files_item
        else -> throw IndexOutOfBoundsException()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
        R.layout.files_header -> HeaderViewHolder(
            FilesHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
        R.layout.files_item -> ItemViewHolder(FilesItemBinding.inflate(LayoutInflater.from(parent.context)))
        else -> throw IndexOutOfBoundsException()
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> {
                val binding = holder.binding
                val item = getItem(position) as MediaStoreFilesHeader
                binding.title.text = item.displayName
            }
            is ItemViewHolder -> {
                val binding = holder.binding
                val item = getItem(position) as MediaStoreFiles
                Glide.with(fragment)
                    .load(item.contentUri)
                    .centerCrop()
                    .into(binding.icon)
                binding.title.text = item.displayName
                binding.summary.text =
                    formatDateTime(item.timeMillis) + "\u0020\u0020\u0020\u0020" +
                            Formatter.formatFileSize(fragment.requireContext(), item.size)
                binding.card.setOnClickListener {
                    val viewIntent = Intent(Intent.ACTION_VIEW)
                        .setDataAndType(item.contentUri, item.mimeType)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    with(fragment) {
                        try {
                            startActivity(Intent.createChooser(viewIntent, null))
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
                        }
                    }
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

    open fun onPreSubmitList(list: List<MediaStoreModel>): List<MediaStoreModel>? =
        if (ModulePreferences.sortMediaBy == ModulePreferences.SORT_BY_PATH) {
            val groupedList = mutableListOf<MediaStoreModel>()
            var lastRelativePath = ""
            list.forEach {
                val relativePath = (it as MediaStoreFiles).relativePath
                if (lastRelativePath != relativePath) {
                    lastRelativePath = relativePath
                    groupedList += MediaStoreFilesHeader(relativePath)
                }
                groupedList += it
            }
            groupedList
        } else {
            list
        }

    override fun submitList(list: List<MediaStoreModel>?) {
        submitList(list, null)
    }

    override fun submitList(list: List<MediaStoreModel>?, commitCallback: Runnable?) {
        if (list != null) {
            super.submitList(onPreSubmitList(list), commitCallback)
        } else {
            super.submitList(list, commitCallback)
        }
    }

    class HeaderViewHolder(val binding: FilesHeaderBinding) :
        MediaStoreAdapter.ViewHolder(binding.root)

    class ItemViewHolder(val binding: FilesItemBinding) : MediaStoreAdapter.ViewHolder(binding.root)
}

class MediaStoreFilesHeader(override val displayName: String) :
    MediaStoreModel(displayName.hashCode().toLong(), Uri.EMPTY, displayName)

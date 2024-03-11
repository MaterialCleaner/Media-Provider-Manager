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

package me.gm.cleaner.plugin.ui.mediastore.files

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
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
import me.gm.cleaner.plugin.dao.RootPreferences
import me.gm.cleaner.plugin.databinding.FilesItemBinding
import me.gm.cleaner.plugin.ui.mediastore.MediaStoreAdapter
import java.util.*

open class FilesAdapter(private val fragment: Fragment) : MediaStoreAdapter(fragment) {

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is MediaStoreFiles -> R.layout.files_item
        else -> super.getItemViewType(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        when (viewType) {
            R.layout.files_item -> ItemViewHolder(
                FilesItemBinding.inflate(LayoutInflater.from(parent.context))
            )

            else -> super.onCreateViewHolder(parent, viewType)
        }

    private fun formatDateTimeForItem(timeMillis: Long): String {
        then.timeInMillis = timeMillis
        now.timeInMillis = System.currentTimeMillis()
        val flags = DateUtils.FORMAT_NO_NOON or DateUtils.FORMAT_NO_MIDNIGHT or
                DateUtils.FORMAT_ABBREV_ALL or when {
            RootPreferences.sortMediaBy == RootPreferences.SORT_BY_DATE_TAKEN -> {
                DateUtils.FORMAT_SHOW_TIME
            }

            then[Calendar.YEAR] != now[Calendar.YEAR] -> {
                DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_SHOW_DATE
            }

            then[Calendar.DAY_OF_YEAR] != now[Calendar.DAY_OF_YEAR] -> {
                DateUtils.FORMAT_SHOW_DATE
            }

            else -> {
                DateUtils.FORMAT_SHOW_TIME
            }
        }
        return DateUtils.formatDateTime(fragment.requireContext(), timeMillis, flags)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (holder) {
            is ItemViewHolder -> {
                val binding = holder.binding
                val item = getItem(position) as MediaStoreFiles
                Glide.with(fragment)
                    .load(item.contentUri)
                    .centerCrop()
                    .into(binding.icon)
                binding.title.text = item.displayName
                binding.summary.text = formatDateTimeForItem(item.dateTaken) +
                        "\u0020\u0020\u0020\u0020" +
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

    class ItemViewHolder(val binding: FilesItemBinding) : ViewHolder(binding.root)
}

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

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.fragment.app.Fragment
import me.gm.cleaner.plugin.mediastore.files.FilesAdapter
import me.gm.cleaner.plugin.mediastore.files.MediaStoreFiles

class VideoAdapter(private val fragment: Fragment) : FilesAdapter(fragment) {
    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        when (holder) {
            is ItemViewHolder -> {
                val binding = holder.binding
                val item = getItem(position) as MediaStoreFiles
                binding.card.setOnClickListener {
                    // TODO
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
            }
        }
    }
}

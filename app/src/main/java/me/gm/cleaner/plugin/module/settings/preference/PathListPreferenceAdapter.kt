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

package me.gm.cleaner.plugin.module.settings.preference

import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.core.view.forEach
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.databinding.PathListItemBinding
import java.io.File

class PathListPreferenceAdapter(
    private val fragment: PathListPreferenceFragmentCompat
) : ListAdapter<String, PathListPreferenceAdapter.ViewHolder>(CALLBACK) {
    private lateinit var selectedHolder: ViewHolder

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(PathListItemBinding.inflate(LayoutInflater.from(parent.context)))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val path = getItem(position)!!
        binding.title.text = path
        binding.root.setOnLongClickListener {
            selectedHolder = holder
            false
        }
        binding.root.setOnCreateContextMenuListener { menu, _, _ ->
            fragment.requireActivity().menuInflater.inflate(R.menu.item_delete, menu)
            menu.setHeaderTitle(path.substring(path.lastIndexOf(File.separator) + 1))
            menu.forEach { it.setOnMenuItemClickListener(::onContextItemSelected) }
        }
    }

    private fun onContextItemSelected(item: MenuItem): Boolean {
        if (!::selectedHolder.isInitialized) return false
        val position = selectedHolder.bindingAdapterPosition
        val path = getItem(position)!!
        if (item.itemId == R.id.menu_delete) {
            fragment.newValues -= path
            return true
        }
        return false
    }

    class ViewHolder(val binding: PathListItemBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {
        private val CALLBACK: DiffUtil.ItemCallback<String> =
            object : DiffUtil.ItemCallback<String>() {
                override fun areItemsTheSame(oldItem: String, newItem: String) = oldItem == newItem
                override fun areContentsTheSame(oldItem: String, newItem: String) =
                    oldItem == newItem
            }
    }
}

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

package me.gm.cleaner.plugin.experiment

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.view.menu.MenuItemImpl
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.databinding.ExperimentCardActionBinding
import me.gm.cleaner.plugin.databinding.ExperimentCardHeaderBinding

@SuppressLint("RestrictedApi")
class ExperimentAdapter(private val fragment: ExperimentFragment) :
    ListAdapter<MenuItemImpl, RecyclerView.ViewHolder>(CALLBACK) {

    override fun getItemViewType(position: Int) = when (position) {
        0 -> R.layout.experiment_card_header
        1 -> R.layout.experiment_card_action
        else -> throw IndexOutOfBoundsException()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
        R.layout.experiment_card_header -> HeaderCardViewHolder(
            ExperimentCardHeaderBinding.inflate(LayoutInflater.from(parent.context))
        )
        R.layout.experiment_card_action -> ActionCardViewHolder(
            ExperimentCardActionBinding.inflate(LayoutInflater.from(parent.context))
        )
        else -> throw IndexOutOfBoundsException()
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

    }

    override fun getItemCount(): Int {
        return 2
    }

    class HeaderCardViewHolder(val binding: ExperimentCardHeaderBinding) :
        RecyclerView.ViewHolder(binding.root)

    class ActionCardViewHolder(val binding: ExperimentCardActionBinding) :
        RecyclerView.ViewHolder(binding.root)

    companion object {
        private val CALLBACK: DiffUtil.ItemCallback<MenuItemImpl> =
            object : DiffUtil.ItemCallback<MenuItemImpl>() {
                override fun areItemsTheSame(oldItem: MenuItemImpl, newItem: MenuItemImpl) =
                    oldItem.itemId == newItem.itemId

                override fun areContentsTheSame(oldItem: MenuItemImpl, newItem: MenuItemImpl) =
                    oldItem.title == newItem.title
            }
    }
}

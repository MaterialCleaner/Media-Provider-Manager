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
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.databinding.ExperimentCardActionBinding
import me.gm.cleaner.plugin.databinding.ExperimentCardHeaderBinding
import me.gm.cleaner.plugin.databinding.ExperimentCardSubheaderBinding

@SuppressLint("PrivateResource")
class ExperimentAdapter : ListAdapter<ExperimentContentItem, RecyclerView.ViewHolder>(CALLBACK) {

    override fun getItemViewType(position: Int) = when (getItem(position)) {
        is ExperimentContentSeparatorItem -> com.google.android.material.R.layout.design_navigation_item_separator
        is ExperimentContentHeaderItem -> R.layout.experiment_card_header
        is ExperimentContentSubHeaderItem -> R.layout.experiment_card_subheader
        is ExperimentContentActionItem -> R.layout.experiment_card_action
        else -> throw IndexOutOfBoundsException()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
        com.google.android.material.R.layout.design_navigation_item_separator ->
            SeparatorViewHolder(LayoutInflater.from(parent.context), parent)
        R.layout.experiment_card_header -> HeaderCardViewHolder(
            ExperimentCardHeaderBinding.inflate(LayoutInflater.from(parent.context))
        )
        R.layout.experiment_card_subheader -> SubHeaderCardViewHolder(
            ExperimentCardSubheaderBinding.inflate(LayoutInflater.from(parent.context))
        )
        R.layout.experiment_card_action -> ActionCardViewHolder(
            ExperimentCardActionBinding.inflate(LayoutInflater.from(parent.context))
        )
        else -> throw IndexOutOfBoundsException()
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (position == 0) {
            val itemView = holder.itemView
            itemView.setPaddingRelative(
                itemView.paddingStart, itemView.paddingTop +
                        itemView.resources.getDimensionPixelOffset(R.dimen.card_margin),
                itemView.paddingEnd, itemView.paddingBottom
            )
        }
        when (holder) {
            is SeparatorViewHolder -> {
                holder.itemView.setPaddingRelative(
                    0, holder.itemView.resources.getDimensionPixelOffset(
                        com.google.android.material.R.dimen.design_navigation_separator_vertical_padding
                    ), 0, 0
                )
            }
            is HeaderCardViewHolder -> {
                val binding = holder.binding
                val item = getItem(position) as ExperimentContentHeaderItem
                binding.title.text = item.title
            }
            is SubHeaderCardViewHolder -> {
                val binding = holder.binding
                val item = getItem(position) as ExperimentContentSubHeaderItem
                binding.cardContextText.text = item.content
            }
            is ActionCardViewHolder -> {
                val binding = holder.binding
                val item = getItem(position) as ExperimentContentActionItem
                binding.title.text = item.title
                binding.summary.text = item.summary
                binding.button.setOnClickListener(item.listener)
            }
        }
    }

    class SeparatorViewHolder(inflater: LayoutInflater, parent: ViewGroup?) :
        RecyclerView.ViewHolder(
            inflater.inflate(
                com.google.android.material.R.layout.design_navigation_item_separator, parent, false
            )
        )

    class HeaderCardViewHolder(val binding: ExperimentCardHeaderBinding) :
        RecyclerView.ViewHolder(binding.root)

    class SubHeaderCardViewHolder(val binding: ExperimentCardSubheaderBinding) :
        RecyclerView.ViewHolder(binding.root)

    class ActionCardViewHolder(val binding: ExperimentCardActionBinding) :
        RecyclerView.ViewHolder(binding.root)

    companion object {
        private val CALLBACK: DiffUtil.ItemCallback<ExperimentContentItem> =
            object : DiffUtil.ItemCallback<ExperimentContentItem>() {
                override fun areItemsTheSame(
                    oldItem: ExperimentContentItem, newItem: ExperimentContentItem
                ) = oldItem == newItem

                @SuppressLint("DiffUtilEquals")
                override fun areContentsTheSame(
                    oldItem: ExperimentContentItem, newItem: ExperimentContentItem
                ) = oldItem == newItem
            }
    }
}

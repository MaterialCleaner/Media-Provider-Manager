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

package me.gm.cleaner.plugin.drawer.playground

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.behavior.SwipeDismissBehavior
import com.google.android.material.divider.MaterialDivider
import kotlinx.coroutines.*
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.app.ConfirmationDialog
import me.gm.cleaner.plugin.databinding.PlaygroundCardActionBinding
import me.gm.cleaner.plugin.databinding.PlaygroundCardHeaderBinding
import me.gm.cleaner.plugin.databinding.PlaygroundCardSubheaderBinding
import me.gm.cleaner.plugin.ktx.hasWifiTransport

@SuppressLint("PrivateResource")
class PlaygroundAdapter(
    private val fragment: PlaygroundFragment, private val viewModel: PlaygroundViewModel
) : ListAdapter<PlaygroundContentItem, RecyclerView.ViewHolder>(CALLBACK) {

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is PlaygroundContentSeparatorItem -> com.google.android.material.R.layout.design_navigation_item_separator
        is PlaygroundContentHeaderItem -> R.layout.playground_card_header
        is PlaygroundContentSubHeaderItem -> R.layout.playground_card_subheader
        is PlaygroundContentActionItem -> R.layout.playground_card_action
        else -> throw IndexOutOfBoundsException()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            com.google.android.material.R.layout.design_navigation_item_separator ->
                SeparatorViewHolder(parent.context)

            R.layout.playground_card_header -> HeaderCardViewHolder(
                PlaygroundCardHeaderBinding.inflate(LayoutInflater.from(parent.context))
            )

            R.layout.playground_card_subheader -> SubHeaderCardViewHolder(
                PlaygroundCardSubheaderBinding.inflate(LayoutInflater.from(parent.context))
            )

            R.layout.playground_card_action -> ActionCardViewHolder(
                PlaygroundCardActionBinding.inflate(LayoutInflater.from(parent.context))
            )

            else -> throw IndexOutOfBoundsException()
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderCardViewHolder -> {
                val binding = holder.binding
                val item = getItem(position) as PlaygroundContentHeaderItem
                binding.title.text = item.title
            }

            is SubHeaderCardViewHolder -> {
                val binding = holder.binding
                val item = getItem(position) as PlaygroundContentSubHeaderItem
                binding.cardContextText.text = item.content
                val card = binding.card
                val swipeDismissBehavior = SwipeDismissBehavior<View>().apply {
                    setSwipeDirection(SwipeDismissBehavior.SWIPE_DIRECTION_ANY)
                    setListener(object : SwipeDismissBehavior.OnDismissListener {
                        override fun onDragStateChanged(state: Int) {
                            when (state) {
                                SwipeDismissBehavior.STATE_DRAGGING,
                                SwipeDismissBehavior.STATE_SETTLING -> card.isDragged = true

                                SwipeDismissBehavior.STATE_IDLE -> card.isDragged = false
                            }
                        }

                        override fun onDismiss(view: View) {
                            viewModel.dismissedCards.add(item.id)
                            viewModel.prepareContentItems(fragment, this@PlaygroundAdapter)
                        }
                    })
                }
                val coordinatorParams = card.layoutParams as CoordinatorLayout.LayoutParams
                coordinatorParams.behavior = swipeDismissBehavior
            }

            is ActionCardViewHolder -> {
                val binding = holder.binding
                val item = getItem(position) as PlaygroundContentActionItem
                binding.title.text = item.title
                binding.summary.text = item.summary
                val button = binding.button
                button.addOnCheckedChangeListener { _, isChecked ->
                    button.setText(
                        if (isChecked) android.R.string.cancel
                        else R.string.start
                    )
                }
                val deferred = viewModel.actions[item.id]
                button.isChecked = deferred != null && deferred.isActive

                button.setOnClickListener {
                    var deferred = viewModel.actions[item.id] as? Deferred<Unit>
                    if (deferred == null || !deferred.isActive) {
                        deferred = viewModel.viewModelScope
                            .async(Dispatchers.Main.immediate, CoroutineStart.LAZY) {
                                item.action!!()
                            }
                        viewModel.actions.put(item.id, deferred)
                        if (item.needsNetwork && !fragment.requireContext().hasWifiTransport) {
                            ConfirmationDialog
                                .newInstance(fragment.getString(R.string.no_wifi))
                                .apply {
                                    addOnPositiveButtonClickListener {
                                        startAction(deferred)
                                    }
                                }
                                .show(fragment.childFragmentManager, null)
                        } else {
                            startAction(deferred)
                        }
                    } else {
                        deferred.cancel()
                    }
                }
            }
        }
    }

    private fun startAction(deferred: Deferred<Unit>): Job = viewModel.viewModelScope.launch {
        deferred.await()
    }

    override fun getItemId(position: Int): Long = getItem(position).id.toLong()

    class SeparatorViewHolder(context: Context) :
        RecyclerView.ViewHolder(FrameLayout(context).apply {
            val cardMargin = context.resources.getDimensionPixelSize(R.dimen.card_margin)
            addView(MaterialDivider(context).apply {
                dividerInsetStart = cardMargin
                dividerInsetEnd = cardMargin
            })
            setPaddingRelative(0, cardMargin, 0, 0)
        })

    class HeaderCardViewHolder(val binding: PlaygroundCardHeaderBinding) :
        RecyclerView.ViewHolder(binding.root)

    class SubHeaderCardViewHolder(val binding: PlaygroundCardSubheaderBinding) :
        RecyclerView.ViewHolder(binding.root)

    class ActionCardViewHolder(val binding: PlaygroundCardActionBinding) :
        RecyclerView.ViewHolder(binding.root)

    companion object {
        private val CALLBACK = object : DiffUtil.ItemCallback<PlaygroundContentItem>() {
            override fun areItemsTheSame(
                oldItem: PlaygroundContentItem, newItem: PlaygroundContentItem
            ): Boolean = oldItem.id == newItem.id

            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(
                oldItem: PlaygroundContentItem, newItem: PlaygroundContentItem
            ): Boolean = oldItem == newItem
        }
    }
}

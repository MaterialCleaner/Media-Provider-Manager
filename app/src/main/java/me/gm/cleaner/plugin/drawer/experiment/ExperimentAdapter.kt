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

package me.gm.cleaner.plugin.drawer.experiment

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.behavior.SwipeDismissBehavior
import com.google.android.material.divider.MaterialDivider
import kotlinx.coroutines.*
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.databinding.ExperimentCardActionBinding
import me.gm.cleaner.plugin.databinding.ExperimentCardHeaderBinding
import me.gm.cleaner.plugin.databinding.ExperimentCardSubheaderBinding

@SuppressLint("PrivateResource")
class ExperimentAdapter(private val fragment: ExperimentFragment) :
    ListAdapter<ExperimentContentItem, RecyclerView.ViewHolder>(CALLBACK) {
    private val viewModel: ExperimentViewModel by fragment.viewModels()

    override fun getItemViewType(position: Int) = when (getItem(position)) {
        is ExperimentContentSeparatorItem -> com.google.android.material.R.layout.design_navigation_item_separator
        is ExperimentContentHeaderItem -> R.layout.experiment_card_header
        is ExperimentContentSubHeaderItem -> R.layout.experiment_card_subheader
        is ExperimentContentActionItem -> R.layout.experiment_card_action
        else -> throw IndexOutOfBoundsException()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
        com.google.android.material.R.layout.design_navigation_item_separator -> SeparatorViewHolder(
            parent.context
        )
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
            is HeaderCardViewHolder -> {
                val binding = holder.binding
                val item = getItem(position) as ExperimentContentHeaderItem
                binding.title.text = item.title
            }
            is SubHeaderCardViewHolder -> {
                val binding = holder.binding
                val item = getItem(position) as ExperimentContentSubHeaderItem
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
                            viewModel.dismissedCard.add(item.id)
                            viewModel.prepareContentItems(
                                fragment.requireActivity(), this@ExperimentAdapter
                            )
                        }
                    })
                }
                val coordinatorParams = card.layoutParams as CoordinatorLayout.LayoutParams
                coordinatorParams.behavior = swipeDismissBehavior
            }
            is ActionCardViewHolder -> {
                val binding = holder.binding
                val item = getItem(position) as ExperimentContentActionItem
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
                        deferred = viewModel.viewModelScope.async(
                            Dispatchers.IO, CoroutineStart.LAZY, item.action!!
                        )
                        viewModel.actions.put(item.id, deferred)
                        if (!fragment.requireContext().hasWifiTransport) {
                            button.toggle()
                            fragment.dialog = AlertDialog.Builder(fragment.requireContext())
                                .setMessage(R.string.no_wifi)
                                .setNegativeButton(android.R.string.cancel, null)
                                .setPositiveButton(android.R.string.ok) { _, _ ->
                                    button.toggle()
                                    startAction(deferred)
                                }
                                .show()
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

    private fun startAction(deferred: Deferred<Unit>) = viewModel.viewModelScope.launch {
        deferred.await()
    }

    class SeparatorViewHolder(context: Context) :
        RecyclerView.ViewHolder(FrameLayout(context).apply {
            val cardMargin = context.resources.getDimensionPixelSize(R.dimen.card_margin)
            addView(MaterialDivider(context).apply {
                dividerInsetStart = cardMargin
                dividerInsetEnd = cardMargin
            })
            setPaddingRelative(0, cardMargin, 0, 0)
        })

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
                ) = oldItem.id == newItem.id

                @SuppressLint("DiffUtilEquals")
                override fun areContentsTheSame(
                    oldItem: ExperimentContentItem, newItem: ExperimentContentItem
                ) = oldItem == newItem
            }

        val Context.hasWifiTransport: Boolean
            get() {
                val connManager =
                    getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val capabilities = connManager.getNetworkCapabilities(connManager.activeNetwork)
                return capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            }
    }
}

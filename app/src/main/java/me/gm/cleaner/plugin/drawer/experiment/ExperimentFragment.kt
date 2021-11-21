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

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.util.keyIterator
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.app.BaseFragment
import me.gm.cleaner.plugin.databinding.ExperimentFragmentBinding
import me.gm.cleaner.plugin.drawer.experiment.ExperimentContentItems.findIndexById
import me.gm.cleaner.plugin.ktx.addLiftOnScrollListener
import me.gm.cleaner.plugin.ktx.overScrollIfContentScrollsPersistent
import rikka.recyclerview.fixEdgeEffect

@AndroidEntryPoint
class ExperimentFragment : BaseFragment() {
    private val viewModel: ExperimentViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = ExperimentFragmentBinding.inflate(layoutInflater)

        val adapter = ExperimentAdapter(this)
        val list = binding.list
        list.adapter = adapter
        list.layoutManager = GridLayoutManager(requireContext(), 1)
        list.setHasFixedSize(true)
        list.fixEdgeEffect(false)
        list.overScrollIfContentScrollsPersistent()
        list.addLiftOnScrollListener { appBarLayout.isLifted = it }
        list.addItemDecoration(object : RecyclerView.ItemDecoration() {
            private var divider = ColorDrawable(Color.TRANSPARENT)
            private var dividerHeight = resources.getDimensionPixelSize(R.dimen.card_margin)

            override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
                val childCount = parent.childCount
                val width = parent.width
                for (childViewIndex in 0 until childCount) {
                    val view = parent.getChildAt(childViewIndex)
                    val top = view.y.toInt() + view.height
                    divider.setBounds(0, top, width, top + dividerHeight)
                    divider.draw(c)
                }
            }

            override fun getItemOffsets(
                outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
            ) {
                outRect.bottom = dividerHeight
            }
        })

        viewModel.prepareContentItems(requireActivity(), adapter)
        viewModel.unsplashPhotosLiveData.observe(viewLifecycleOwner) {
            synchronized(viewModel.actions) {
                val changedItemIds = mutableListOf<Int>()
                viewModel.actions.keyIterator().forEach { id ->
                    if (!viewModel.actions[id].isActive) {
                        changedItemIds.add(id)
                        val position = adapter.currentList.findIndexById(id)
                        adapter.notifyItemChanged(position)
                    }
                }
                changedItemIds.forEach { id ->
                    viewModel.actions.remove(id)
                }
            }
        }
        return binding.root
    }
}

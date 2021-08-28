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

package me.gm.cleaner.plugin.test

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.app.BaseFragment
import me.gm.cleaner.plugin.databinding.HomeActivityBinding
import rikka.recyclerview.addFastScroller
import rikka.recyclerview.fixEdgeEffect
import rikka.widget.borderview.BorderView.OnBorderVisibilityChangedListener

class QueryFragment : BaseFragment() {
    private val viewModel by viewModels<QueryViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = HomeActivityBinding.inflate(inflater)
        setAppBar(binding.root).apply {
            setNavigationOnClickListener { navigateUp() }
            setNavigationIcon(R.drawable.ic_outline_arrow_back_24)
        }

        val adapter = QueryAdapter(this)
        binding.list.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.list.setHasFixedSize(true)
        binding.list.fixEdgeEffect()
        binding.list.addFastScroller()
        binding.list.isVerticalScrollBarEnabled = false
        binding.list.borderViewDelegate.borderVisibilityChangedListener =
            OnBorderVisibilityChangedListener { top: Boolean, _: Boolean, _: Boolean, _: Boolean ->
                appBarLayout.isRaised = !top
            }
        binding.list.adapter = adapter

        viewModel.images.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }
        if (viewModel.images.value == null) {
            viewModel.loadImages()
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    navigateUp()
                }
            })
        return binding.root
    }
}

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

package me.gm.cleaner.plugin.module.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.SharedElementCallback
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.transition.platform.MaterialContainerTransform
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.app.BaseFragment
import me.gm.cleaner.plugin.databinding.AppFragmentBinding
import me.gm.cleaner.plugin.databinding.TemplatesFragmentBinding
import me.gm.cleaner.plugin.ktx.addLiftOnScrollListener
import me.gm.cleaner.plugin.ktx.colorBackground
import me.gm.cleaner.plugin.ktx.mediumAnimTime
import me.gm.cleaner.plugin.ktx.overScrollIfContentScrollsPersistent
import rikka.recyclerview.fixEdgeEffect

class TemplatesFragment : BaseFragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = TemplatesFragmentBinding.inflate(layoutInflater)

        val adapter = TemplatesHeaderAdapter(this)
        val list = binding.list
        list.adapter = adapter
        list.layoutManager = GridLayoutManager(requireContext(), 1)
        list.setHasFixedSize(true)
        list.fixEdgeEffect(false)
        list.overScrollIfContentScrollsPersistent()
        list.addLiftOnScrollListener { appBarLayout.isLifted = it }
        val paddingStart = list.paddingStart
        val paddingTop = list.paddingTop
        val paddingEnd = list.paddingEnd
        val paddingBottom = list.paddingBottom
        list.setOnApplyWindowInsetsListener { view, insets ->
            view.setPaddingRelative(
                paddingStart, paddingTop, paddingEnd, paddingBottom + insets.systemWindowInsetBottom
            )
            insets
        }

        prepareSharedElementTransition(list)
        return binding.root
    }

    private fun prepareSharedElementTransition(list: RecyclerView) {
//        list.transitionName = args.pi.packageName

        sharedElementEnterTransition = MaterialContainerTransform(requireContext(), true).apply {
            endViewId = R.id.root
            drawingViewId = R.id.root
            setAllContainerColors(requireContext().colorBackground)
            interpolator = FastOutSlowInInterpolator()
            fadeMode = MaterialContainerTransform.FADE_MODE_CROSS
            duration = requireContext().mediumAnimTime
        }

        sharedElementReturnTransition = MaterialContainerTransform(requireContext(), false).apply {
            drawingViewId = android.R.id.content
            setAllContainerColors(requireContext().colorBackground)
            interpolator = FastOutSlowInInterpolator()
            fadeMode = MaterialContainerTransform.FADE_MODE_CROSS
            duration = requireContext().mediumAnimTime
        }

        setEnterSharedElementCallback(object : SharedElementCallback() {
            override fun onMapSharedElements(
                names: List<String>, sharedElements: MutableMap<String, View>
            ) {
                if (names.isNotEmpty()) {
                    sharedElements[names[0]] = list
                }
            }
        })
    }

    companion object {
        const val KEY_PACKAGENAME = "me.gm.cleaner.plugin.key.position"
    }
}

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

package me.gm.cleaner.plugin.ui.module.appmanagement

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.SharedElementCallback
import androidx.core.os.bundleOf
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.transition.platform.MaterialContainerTransform
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.databinding.AppFragmentBinding
import me.gm.cleaner.plugin.ktx.DividerDecoration
import me.gm.cleaner.plugin.ktx.colorSurface
import me.gm.cleaner.plugin.ktx.fitsSystemWindowInsets
import me.gm.cleaner.plugin.ktx.isItemCompletelyInvisible
import me.gm.cleaner.plugin.ktx.mediumAnimTime
import me.gm.cleaner.plugin.ktx.overScrollIfContentScrollsPersistent
import me.gm.cleaner.plugin.model.Template
import me.gm.cleaner.plugin.model.Templates
import me.gm.cleaner.plugin.ui.module.ModuleFragment
import me.gm.cleaner.plugin.ui.module.settings.CreateTemplateFragment
import rikka.recyclerview.fixEdgeEffect
import java.text.Collator

class AppFragment : ModuleFragment() {
    val args: AppFragmentArgs by navArgs()
    var lastTemplateName: String? = null

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = AppFragmentBinding.inflate(layoutInflater)

        val templatesAdapter = TemplatesAdapter(this)
        val createTemplateAdapter = CreateTemplateAdapter(this)
        val addToExistingTemplateAdapter = AddToExistingTemplateAdapter(this)
        val adapters = ConcatAdapter(
            AppHeaderAdapter(this),
            templatesAdapter,
            createTemplateAdapter,
            addToExistingTemplateAdapter
        )
        val list = binding.list
        liftOnScrollTargetView = list
        list.adapter = adapters
        list.layoutManager = GridLayoutManager(requireContext(), 1)
        list.setHasFixedSize(true)
        list.fixEdgeEffect(false)
        list.overScrollIfContentScrollsPersistent()
        list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                supportActionBar?.title = if (recyclerView.isItemCompletelyInvisible(0)) {
                    args.label
                } else {
                    findNavController().currentDestination?.label
                }
            }
        })
        binding.root.fitsSystemWindowInsets()
        // Don't add systemWindowInsetTop to RecyclerView for a better SharedElementTransition.
        list.setOnApplyWindowInsetsListener { view, insets ->
            view.setPadding(
                view.paddingLeft, view.paddingTop,
                view.paddingRight, view.paddingBottom + insets.systemWindowInsetBottom
            )
            insets
        }
        list.addItemDecoration(DividerDecoration(list).apply {
            setDivider(resources.getDrawable(R.drawable.list_divider_material, null))
            setAllowDividerAfterLastItem(false)
        })

        binderViewModel.remoteSpCacheLiveData.observe(viewLifecycleOwner) {
            val currentList = prepareCurrentList()
            templatesAdapter.submitList(currentList.first)
            addToExistingTemplateAdapter.submitList(currentList.second)
            if (currentList.first.any { it.templateName == args.label }) {
                adapters.removeAdapter(createTemplateAdapter)
            } else {
                adapters.addAdapter(
                    adapters.adapters.indexOfFirst { it is TemplatesAdapter } + 1,
                    createTemplateAdapter
                )
            }
        }

        prepareSharedElementTransition(list)
        setFragmentResultListener(CreateTemplateFragment::class.java.name) { _, bundle ->
            lastTemplateName = bundle.getString(CreateTemplateFragment.KEY_TEMPLATE_NAME)
            val currentList = prepareCurrentList()
            var position = 1 +
                    currentList.first.indexOfFirst { it.templateName == lastTemplateName }
            if (position == 0) {
                position = 1 + currentList.first.size + 1 +
                        currentList.second.indexOfFirst { it.templateName == lastTemplateName }
            }
            prepareTransitions(list, position)
            postponeEnterTransition()
            scrollToPosition(list, position)
        }
        return binding.root
    }

    private fun prepareCurrentList(): Pair<List<Template>, List<Template>> {
        val collator = Collator.getInstance()
        return Templates(binderViewModel.readSp(R.xml.template_preferences)).values
            .sortedWith { o1, o2 -> collator.compare(o1?.templateName, o2?.templateName) }
            .partition { it.applyToApp?.contains(args.packageInfo.packageName) == true }
    }

    private fun prepareTransitions(list: RecyclerView, position: Int) {
        setExitSharedElementCallback(object : SharedElementCallback() {
            override fun onMapSharedElements(
                names: List<String>, sharedElements: MutableMap<String, View>
            ) {
                val selectedViewHolder = list.findViewHolderForAdapterPosition(position) ?: return
                sharedElements[names[0]] = selectedViewHolder.itemView
            }
        })
    }

    private fun prepareSharedElementTransition(list: RecyclerView) {
        setFragmentResult(
            AppFragment::class.java.name, bundleOf(KEY_PACKAGENAME to args.packageInfo.packageName)
        )
        list.transitionName = args.packageInfo.packageName

        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.nav_host
            setAllContainerColors(requireContext().colorSurface)
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

    private fun scrollToPosition(list: RecyclerView, position: Int) {
        list.doOnPreDraw {
            val layoutManager = list.layoutManager as? LinearLayoutManager ?: return@doOnPreDraw
            val viewAtPosition = layoutManager.findViewByPosition(position)
            // Scroll to position if the view for the current position is null (not currently part of
            // layout manager children), or it's not completely visible.
            if (viewAtPosition == null ||
                layoutManager.isViewPartiallyVisible(viewAtPosition, false, true)
            ) {
                val lastPosition = layoutManager.findLastCompletelyVisibleItemPosition()
                if (position >= lastPosition && lastPosition - layoutManager.findFirstCompletelyVisibleItemPosition() > 0) {
                    layoutManager.scrollToPosition(position)
                } else {
                    layoutManager.scrollToPositionWithOffset(position, list.paddingTop)
                }
            }
        }
    }

    companion object {
        const val KEY_PACKAGENAME = "me.gm.cleaner.plugin.key.packageName"
    }
}

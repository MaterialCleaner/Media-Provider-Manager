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

package me.gm.cleaner.plugin.test.query

import android.os.Bundle
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.annotation.Px
import androidx.core.app.SharedElementCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.app.BaseFragment
import me.gm.cleaner.plugin.databinding.GalleryFragmentBinding
import me.gm.cleaner.plugin.test.TestActivity

class GalleryFragment : BaseFragment() {
    private val viewModel by activityViewModels<QueryViewModel>()
    private lateinit var viewPager: ViewPager2

    private class PagerAdapter(fragment: Fragment, private val imageCount: Int) :
        FragmentStateAdapter(fragment) {
        override fun createFragment(position: Int): Fragment = GalleryItem.newInstance(position)

        override fun getItemCount(): Int = imageCount
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = GalleryFragmentBinding.inflate(inflater)
        setAppBar(binding.root).apply {
            setNavigationOnClickListener { navigateUp() }
            setNavigationIcon(R.drawable.ic_outline_arrow_back_24)
        }

        viewPager = binding.viewPager
        viewPager.adapter = PagerAdapter(this, viewModel.images.value!!.size)
        // Set the current position and add a listener that will update the selection coordinator when
        // paging the images.
        viewPager.currentItem = viewModel.currentPosition
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(
                position: Int, positionOffset: Float, @Px positionOffsetPixels: Int
            ) {
                viewModel.currentPosition = position
                (requireActivity() as TestActivity).supportActionBar?.title =
                    viewModel.images.value!![position].displayName
            }
        })

        appBarLayout.isRaised = true

        prepareSharedElementTransition()
        // Avoid a postponeEnterTransition on orientation change, and postpone only of first creation.
        if (savedInstanceState == null) {
            postponeEnterTransition()
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    navigateUp()
                }
            }
        )
        return binding.root
    }

    private fun prepareSharedElementTransition() {
        sharedElementEnterTransition = TransitionInflater.from(context)
            .inflateTransition(R.transition.image_shared_element_transition)

        // A similar mapping is set at the GridFragment with a setExitSharedElementCallback.
        setEnterSharedElementCallback(object : SharedElementCallback() {
            override fun onMapSharedElements(
                names: List<String>, sharedElements: MutableMap<String, View>
            ) {
                // Locate the image view at the primary fragment (the ImageFragment that is currently
                // visible). To locate the fragment, call instantiateItem with the selection position.
                // At this stage, the method will simply return the fragment at the position and will
                // not create a new one.
                // https://stackoverflow.com/questions/55728719/get-current-fragment-with-viewpager2
                val currentFragment =
                    parentFragmentManager.findFragmentByTag("f" + viewModel.currentPosition)
                val view = currentFragment?.view ?: return

                // Map the first shared element name to the child ImageView.
                sharedElements[names[0]] = view.findViewById(R.id.photo_view)
            }
        })
    }
}
